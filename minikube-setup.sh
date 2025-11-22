#!/bin/bash

# Minikube setup script for bankapp
# This script helps set up minikube and load local Docker images
# Usage: ./minikube-setup.sh [command]
# Commands: start, load, deploy, clean, all

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_PREFIX=""
TAG="${IMAGE_TAG:-latest}"
NAMESPACE="bank-app-dev"
HELM_RELEASE="bank-app"
HELM_CHART="./helm/bank-app"

export EUREKA_ENABLED=false

# Array of services
declare -a SERVICES=(
    "auth-server"
    "gateway-service"
    "accounts-service"
    "cash-service"
    "transfer-service"
    "exchange-service"
    "exchange-generator-service"
    "blocker-service"
    "notifications-service"
    "front-ui"
)

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Check if minikube is installed
check_minikube() {
    if ! command -v minikube &> /dev/null; then
        print_error "minikube is not installed. Please install it first."
        echo "Visit: https://minikube.sigs.k8s.io/docs/start/"
        exit 1
    fi
    print_info "minikube is installed"
}

# Check if kubectl is installed
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install it first."
        exit 1
    fi
    print_info "kubectl is installed"
}

# Check if helm is installed
check_helm() {
    if ! command -v helm &> /dev/null; then
        print_error "helm is not installed. Please install it first."
        echo "Visit: https://helm.sh/docs/intro/install/"
        exit 1
    fi
    print_info "helm is installed"
}

# Start minikube
start_minikube() {
    print_step "Starting minikube..."

    if minikube status &> /dev/null; then
        print_info "minikube is already running"
    else
        print_info "Starting minikube cluster..."
        minikube start --memory=8192 --cpus=4 --driver=docker
        print_info "minikube started successfully"
    fi

    print_info "Configuring kubectl context..."
    kubectl config use-context minikube

    print_info "Minikube status:"
    minikube status
}

# Load images into minikube
load_images() {
    print_step "Loading Docker images into minikube..."

    # Set docker environment to use minikube's docker daemon
    print_info "Using minikube's Docker daemon..."
 #   eval $(minikube docker-env)

    print_info "Building images directly in minikube's Docker daemon..."

    for service in "${SERVICES[@]}"; do
        local image_name="${IMAGE_PREFIX:+${IMAGE_PREFIX}-}${service}:${TAG}"
        print_info "Building ${image_name} in minikube..."

        if [ -d "${service}" ] && [ -f "${service}/Dockerfile" ]; then
            docker build -t "${image_name}" "${service}/" || {
                print_error "Failed to build ${image_name}"
                continue
            }
            print_info "Built ${image_name}"
            minikube image load "${image_name}"
        else
            print_warning "Service directory or Dockerfile not found for ${service}"
        fi
    done

    # Load Kafka image from Apache (custom Kafka deployment)
    # Extract Kafka image configuration from Helm values (customKafka section)
    print_info "Loading Apache Kafka image for custom Kafka deployment..."

    KAFKA_REGISTRY=$(helm show values ${HELM_CHART} 2>/dev/null | grep -A 3 "^customKafka:" | grep -A 3 "image:" | grep "registry:" | head -1 | awk '{print $2}' | tr -d '"')
    KAFKA_REPOSITORY=$(helm show values ${HELM_CHART} 2>/dev/null | grep -A 4 "^customKafka:" | grep -A 4 "image:" | grep "repository:" | head -1 | awk '{print $2}' | tr -d '"')
    KAFKA_TAG=$(helm show values ${HELM_CHART} 2>/dev/null | grep -A 5 "^customKafka:" | grep -A 5 "image:" | grep "tag:" | head -1 | awk '{print $2}' | tr -d '"')

    KAFKA_IMAGE="${KAFKA_REGISTRY}/${KAFKA_REPOSITORY}:${KAFKA_TAG}"
    print_info "Kafka image from Helm values (customKafka): ${KAFKA_IMAGE}"

    print_info "Pulling ${KAFKA_IMAGE}..."
    if docker pull "${KAFKA_IMAGE}"; then
        print_info "Loading ${KAFKA_IMAGE} into minikube..."
        minikube image load "${KAFKA_IMAGE}" || {
            print_warning "Failed to load Kafka image into minikube"
        }
        print_info "Kafka image loaded successfully"
    else
        print_error "Failed to pull Kafka image"
        print_warning "Kafka may not start properly without this image"
    fi

    print_info "Verifying images in minikube:"
    docker images | grep "^${IMAGE_PREFIX}" | grep "${TAG}" || print_warning "No images found with prefix [${IMAGE_PREFIX}]"

    # Reset docker environment
#    eval $(minikube docker-env -u)
    print_info "All images loaded into minikube successfully"
}

# Deploy ELK Stack
deploy_elk_stack() {
    print_step "Deploying ELK Stack..."

    # Update Helm dependencies for all ELK charts
    print_info "Updating Helm dependencies for ELK charts..."
    for chart in elasticsearch logstash kibana; do
        helm dependency update "./helm/elk/${chart}" || {
            print_error "Failed to update Helm dependencies for ${chart}"
            exit 1
        }
    done

    # Deploy Elasticsearch first
    print_info "Deploying Elasticsearch..."
    helm upgrade --install bank-app-elasticsearch ./helm/elk/elasticsearch \
        --namespace ${NAMESPACE} \
        --wait \
        --timeout 10m || {
        print_error "Failed to deploy Elasticsearch"
        exit 1
    }
    print_info "✓ Elasticsearch deployed successfully"

    # Deploy Logstash
    print_info "Deploying Logstash..."
    helm upgrade --install bank-app-logstash ./helm/elk/logstash \
        --namespace ${NAMESPACE} \
        --wait \
        --timeout 10m || {
        print_error "Failed to deploy Logstash"
        exit 1
    }
    print_info "✓ Logstash deployed successfully"

    # Create dummy ES token secret for Kibana (required for Kibana 8.5.1 without hooks)
    print_info "Creating dummy Elasticsearch token secret for Kibana..."
    kubectl create secret generic bank-app-kibana-kibana-es-token \
        -n ${NAMESPACE} \
        --from-literal=token=dummy \
        --dry-run=client -o yaml | kubectl apply -f - || {
        print_warning "Secret may already exist, continuing..."
    }

    # Deploy Kibana with --no-hooks flag
    print_info "Deploying Kibana..."
    helm upgrade --install bank-app-kibana ./helm/elk/kibana \
        --namespace ${NAMESPACE} \
        --no-hooks \
        --wait \
        --timeout 10m || {
        print_error "Failed to deploy Kibana"
        exit 1
    }
    print_info "✓ Kibana deployed successfully"

    print_info "✓ ELK Stack deployed successfully"
}

# Test ELK deployment
test_elk_deployment() {
    print_step "Running ELK Stack Helm tests..."

    local all_passed=true

    # Test Elasticsearch
    print_info "Testing Elasticsearch..."
    if helm test bank-app-elasticsearch -n ${NAMESPACE} --logs; then
        print_info "✓ Elasticsearch tests passed"
    else
        print_error "✗ Elasticsearch tests failed"
        all_passed=false
    fi

    # Test Logstash
    print_info "Testing Logstash..."
    if helm test bank-app-logstash -n ${NAMESPACE} --logs; then
        print_info "✓ Logstash tests passed"
    else
        print_error "✗ Logstash tests failed"
        all_passed=false
    fi

    # Test Kibana
    print_info "Testing Kibana..."
    if helm test bank-app-kibana -n ${NAMESPACE} --logs; then
        print_info "✓ Kibana tests passed"
    else
        print_error "✗ Kibana tests failed"
        all_passed=false
    fi

    if [ "$all_passed" = true ]; then
        echo ""
        print_info "✓ All ELK tests passed successfully!"
        return 0
    else
        echo ""
        print_error "✗ Some ELK tests failed!"
        return 1
    fi
}

# Test deployment using Helm tests
test_deployment() {
    print_step "Running Helm tests..."

    # Check if release exists
    if ! helm list -n ${NAMESPACE} | grep -q ${HELM_RELEASE}; then
        print_error "Helm release ${HELM_RELEASE} not found in namespace ${NAMESPACE}"
        print_info "Please deploy the application first using: $0 deploy"
        exit 1
    fi

    # Run Helm tests
    print_info "Executing Helm test suite for ${HELM_RELEASE}..."
    if helm test ${HELM_RELEASE} -n ${NAMESPACE} --logs; then
        echo ""
        print_info "✓ All Helm tests passed successfully!"
        return 0
    else
        echo ""
        print_error "✗ Some Helm tests failed!"
        print_info "To debug failed tests, check test pod logs:"
        echo "  kubectl get pods -n ${NAMESPACE} | grep test"
        echo "  kubectl logs <test-pod-name> -n ${NAMESPACE}"
        return 1
    fi
}

# Deploy application using Helm
deploy_app() {
    print_step "Deploying application to minikube..."

    # Update Helm dependencies (downloads Kafka chart from Bitnami)
    print_info "Updating Helm dependencies..."
    helm dependency update ${HELM_CHART} || {
        print_error "Failed to update Helm dependencies"
        exit 1
    }

    # Create namespace if it doesn't exist
    kubectl create namespace ${NAMESPACE} 2>/dev/null || print_info "Namespace ${NAMESPACE} already exists"

    # Deploy ELK Stack first (before main app)
    deploy_elk_stack

    # Check if release exists and is deployed
    if helm list -n ${NAMESPACE} --deployed -q | grep -q "^${HELM_RELEASE}$"; then
        print_info "Upgrading existing Helm release..."
        helm upgrade ${HELM_RELEASE} ${HELM_CHART} \
            --namespace ${NAMESPACE} \
            --set global.image.registry="" \
            --set global.image.prefix="${IMAGE_PREFIX:+${IMAGE_PREFIX}-}" \
            --set global.image.pullPolicy="Never" \
            --set global.image.tag="${TAG}" \
            --wait \
            --timeout 10m
    else
        print_info "Installing Helm release..."
        # Clean up any failed releases first
        helm uninstall ${HELM_RELEASE} -n ${NAMESPACE} 2>/dev/null || true

        helm install ${HELM_RELEASE} ${HELM_CHART} \
            --namespace ${NAMESPACE} \
            --set global.image.registry="" \
            --set global.image.prefix="${IMAGE_PREFIX:+${IMAGE_PREFIX}-}" \
            --set global.image.pullPolicy="Never" \
            --set global.image.tag="${TAG}" \
            --wait \
            --timeout 10m
    fi

    print_info "Application deployed successfully"

    echo ""
    print_info "Checking pod status..."
    kubectl get pods -n ${NAMESPACE}

    echo ""
    print_info "Running Helm tests to verify deployment..."
    if test_deployment && test_elk_deployment; then
        echo ""
        print_info "Deployment verification completed successfully!"
    else
        echo ""
        print_warning "Deployment completed but some tests failed. Please check the logs above."
    fi

    echo ""
    print_info "If something is not Ready, inspect with:"
    echo "  kubectl describe pod <pod> -n ${NAMESPACE}"
    echo "  kubectl logs <pod> -n ${NAMESPACE} --all-containers"

    echo ""
    print_info "To access the application, run:"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/${HELM_RELEASE}-front-ui 8090:8090"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/${HELM_RELEASE}-gateway-service 8100:8100"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/bank-app-kibana-kibana 5601:5601"

    local minikube_ip
    if minikube_ip=$(minikube ip 2>/dev/null); then
        echo ""
        print_info "Ingress host mapping:"
        echo "  ${minikube_ip} bank-app-dev.local"
        print_info "Add the line above to /etc/hosts (or Windows hosts file) and open http://bank-app-dev.local/"
        print_info "If you change the host value in helm/bank-app/values-dev.yaml, update the hosts entry accordingly."
        print_info "Tip: run 'minikube tunnel' in a separate terminal if you need the ingress IP routable from your OS."
    else
        print_warning "Could not determine minikube IP for ingress instructions."
    fi
}


# Clean up deployment
clean() {
    print_step "Cleaning up deployment..."

    # Uninstall main app
    if helm list -n ${NAMESPACE} | grep -q ${HELM_RELEASE}; then
        print_info "Uninstalling Helm release..."
        helm uninstall ${HELM_RELEASE} -n ${NAMESPACE}
        print_info "Helm release uninstalled"
    else
        print_warning "Helm release ${HELM_RELEASE} not found"
    fi

    # Uninstall ELK Stack
    print_info "Uninstalling ELK Stack..."
    helm uninstall bank-app-kibana -n ${NAMESPACE} 2>/dev/null || print_warning "Kibana release not found"
    helm uninstall bank-app-logstash -n ${NAMESPACE} 2>/dev/null || print_warning "Logstash release not found"
    helm uninstall bank-app-elasticsearch -n ${NAMESPACE} 2>/dev/null || print_warning "Elasticsearch release not found"

    print_info "Deleting namespace ${NAMESPACE}..."
    kubectl delete namespace ${NAMESPACE} --ignore-not-found=true

    print_info "Cleanup completed"
}

# Stop minikube
stop_minikube() {
    print_step "Stopping minikube..."
    minikube stop
    print_info "minikube stopped"
}

# Delete minikube cluster
delete_minikube() {
    print_step "Deleting minikube cluster..."
    minikube delete
    print_info "minikube cluster deleted"
}

# Show usage
usage() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  all           - Run complete setup (start + load + deploy)"
    echo "  start         - Start minikube cluster"
    echo "  load          - Build images directly in minikube's Docker daemon"
    echo "  deploy        - Deploy application using Helm (includes ELK + tests)"
    echo "  deploy-elk    - Deploy only ELK Stack"
    echo "  test          - Run Helm tests on deployed application"
    echo "  test-elk      - Run Helm tests on ELK Stack"
    echo "  redeploy      - Clean and deploy again"
    echo "  clean         - Remove deployment and namespace (includes ELK)"
    echo "  stop          - Stop minikube cluster"
    echo "  delete        - Delete minikube cluster completely"
    echo "  status        - Show cluster and deployment status"
    echo ""
    echo "Environment variables:"
    echo "  IMAGE_TAG     - Docker image tag (default: latest)"
    echo ""
    echo "Examples:"
    echo "  $0 all                    # Complete setup"
    echo "  $0 deploy                 # Deploy app + ELK and run tests"
    echo "  $0 test-elk               # Test ELK stack only"
    echo "  IMAGE_TAG=1.0.0 $0 load   # Load images with specific tag"
}

# Show status
show_status() {
    print_step "Checking status..."

    echo ""
    print_info "Minikube status:"
    minikube status || print_warning "minikube is not running"

    echo ""
    print_info "Kubectl context:"
    kubectl config current-context

    echo ""
    print_info "Helm releases in ${NAMESPACE}:"
    helm list -n ${NAMESPACE} || print_warning "Namespace ${NAMESPACE} not found"

    echo ""
    print_info "Pods in ${NAMESPACE}:"
    kubectl get pods -n ${NAMESPACE} || print_warning "Namespace ${NAMESPACE} not found"

    echo ""
    print_info "Services in ${NAMESPACE}:"
    kubectl get svc -n ${NAMESPACE} || print_warning "Namespace ${NAMESPACE} not found"
}

# Main function
main() {
    local command="${1:-all}"

    check_minikube
    check_kubectl
    check_helm

    echo ""
    print_info "Using image: ${IMAGE_PREFIX:+${IMAGE_PREFIX}-}*:${TAG}"
    echo ""

    case "${command}" in
        all)
            start_minikube
            load_images
            deploy_app
            ;;
        start)
            start_minikube
            ;;
        load)
            start_minikube
            load_images
            ;;
        deploy)
            deploy_app
            ;;
        deploy-elk)
            deploy_elk_stack
            ;;
        test)
            test_deployment
            ;;
        test-elk)
            test_elk_deployment
            ;;
        redeploy)
            clean
            deploy_app
            ;;
        clean)
            clean
            ;;
        stop)
            stop_minikube
            ;;
        delete)
            delete_minikube
            ;;
        status)
            show_status
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            print_error "Unknown command: ${command}"
            usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
