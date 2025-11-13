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

    print_info "Verifying images in minikube:"
    docker images | grep "^${IMAGE_PREFIX}" | grep "${TAG}" || print_warning "No images found with prefix [${IMAGE_PREFIX}]"

    # Reset docker environment
#    eval $(minikube docker-env -u)
    print_info "Images loaded into minikube successfully"
}

# Deploy application using Helm
deploy_app() {
    print_step "Deploying application to minikube..."

    # Create namespace if it doesn't exist
    kubectl create namespace ${NAMESPACE} 2>/dev/null || print_info "Namespace ${NAMESPACE} already exists"

    # Check if release exists
    if helm list -n ${NAMESPACE} | grep -q ${HELM_RELEASE}; then
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
    print_info "If something is not Ready, inspect with:"
    echo "  kubectl describe pod <pod> -n ${NAMESPACE}"
    echo "  kubectl logs <pod> -n ${NAMESPACE} --all-containers"

    echo ""
    print_info "To access the application, run:"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/${HELM_RELEASE}-front-ui 8090:8090"
    echo "  kubectl port-forward -n ${NAMESPACE} svc/${HELM_RELEASE}-gateway-service 8100:8100"

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

    if helm list -n ${NAMESPACE} | grep -q ${HELM_RELEASE}; then
        print_info "Uninstalling Helm release..."
        helm uninstall ${HELM_RELEASE} -n ${NAMESPACE}
        print_info "Helm release uninstalled"
    else
        print_warning "Helm release ${HELM_RELEASE} not found"
    fi

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
    echo "  deploy        - Deploy application using Helm"
    echo "  redeploy      - Clean and deploy again"
    echo "  clean         - Remove deployment and namespace"
    echo "  stop          - Stop minikube cluster"
    echo "  delete        - Delete minikube cluster completely"
    echo "  status        - Show cluster and deployment status"
    echo ""
    echo "Environment variables:"
    echo "  IMAGE_TAG     - Docker image tag (default: latest)"
    echo ""
    echo "Examples:"
    echo "  $0 all                    # Complete setup"
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
