# Bank Application - Microservices Architecture

**[Русская версия / Russian version](README-RU.md)**

Microservices-based banking application with comprehensive monitoring and logging.

## Quick Start

### Deploy Locally (Minikube)

```bash
# Complete setup (build + deploy + monitoring + ELK)
./minikube-setup.sh all

# Deploy monitoring stack only
./minikube-setup.sh deploy-monitoring

# Deploy ELK stack only  
./minikube-setup.sh deploy-elk

# Run tests
./minikube-setup.sh test
./minikube-setup.sh test-monitoring
./minikube-setup.sh test-elk
```

### Deploy with Jenkins

1. **Main Application Pipeline**: Jenkinsfile (root)
   - Builds all services
   - Deploys to dev/test/prod
   - Triggers monitoring deployment

2. **Monitoring Stack Pipeline**: monitoring/Jenkinsfile
   - Deploys Zipkin, Prometheus, Grafana
   - Deploys ELK Stack (Elasticsearch, Logstash, Kibana)

## Access Services

```bash
# Application
kubectl port-forward -n bank-app-dev svc/bank-app-front-ui 8090:8090
kubectl port-forward -n bank-app-dev svc/bank-app-gateway-service 8100:8100

# Monitoring
kubectl port-forward -n bank-app-dev svc/bank-app-zipkin 9411:9411
kubectl port-forward -n bank-app-dev svc/bank-app-prometheus-server 9090:9090
kubectl port-forward -n bank-app-dev svc/bank-app-grafana 3000:3000

# Logging
kubectl port-forward -n bank-app-dev svc/bank-app-kibana-kibana 5601:5601
```

Then open:
- **Application UI**: http://localhost:8090
- **API Gateway**: http://localhost:8100
- **Zipkin (Tracing)**: http://localhost:9411
- **Prometheus (Metrics)**: http://localhost:9090
- **Grafana (Dashboards)**: http://localhost:3000
  - Dev credentials: `admin` / `admin123`
  - For production, use auto-generated password (see below)
- **Kibana (Logs)**: http://localhost:5601

### Retrieving Auto-Generated Grafana Password

For production environments, the password is auto-generated:

```bash
kubectl get secret bank-app-grafana -n bank-app-dev -o jsonpath='{.data.admin-password}' | base64 -d
```

## Microservices

- **auth-server** - OAuth2 authentication
- **gateway-service** - API Gateway
- **accounts-service** - Account management
- **cash-service** - Cash operations
- **transfer-service** - Money transfers
- **exchange-service** - Currency exchange
- **exchange-generator-service** - Exchange rates generator
- **blocker-service** - Fraud detection
- **notifications-service** - Email/SMS notifications
- **front-ui** - Web interface

## Documentation

- **[Deployment Guide](docs/DEPLOYMENT.md)** - Detailed deployment instructions for different environments
- **[Jenkins Setup](docs/JENKINS_SETUP.md)** - CI/CD pipeline configuration and setup
- **[Grafana Troubleshooting](docs/GRAFANA_TROUBLESHOOTING.md)** - Fix "No Data" issues and verify metrics collection
- **[Minikube Troubleshooting](docs/MINIKUBE_TROUBLESHOOTING.md)** - Fix TLS timeouts and cluster connectivity issues
- **[Pod Restart Fix](docs/POD_RESTART_FIX.md)** - Resolve constant pod restarts due to resource limits and probe issues

## Monitoring & Logging

### Distributed Tracing (Zipkin)
- Trace requests across microservices
- Performance analysis

### Metrics (Prometheus + Grafana)
- HTTP metrics (RPS, errors, latency)
- JVM metrics (memory, GC, threads)
- Business metrics (logins, transfers, blocks)

### Centralized Logging (ELK)
- Elasticsearch - Log storage
- Logstash - Log processing
- Kibana - Log visualization
- Logs include trace IDs for correlation

## Project Structure

```
.
├── helm/
│   ├── bank-app/           # Main application Helm chart
│   ├── zipkin/             # Zipkin Helm chart
│   ├── prometheus/         # Prometheus Helm chart
│   ├── grafana/            # Grafana Helm chart
│   └── elk/                # ELK Stack Helm charts
├── monitoring/
│   └── Jenkinsfile         # Monitoring deployment pipeline
├── Jenkinsfile             # Main application pipeline
├── minikube-setup.sh       # Local deployment script
└── [microservices]/        # Individual service directories
```

## Commands Reference

```bash
# Minikube
./minikube-setup.sh all          # Complete setup
./minikube-setup.sh deploy       # Deploy app + monitoring + ELK
./minikube-setup.sh status       # Check deployment status
./minikube-setup.sh clean        # Remove all deployments

# Kubernetes
kubectl get pods -n bank-app-dev
kubectl logs <pod> -n bank-app-dev
kubectl describe pod <pod> -n bank-app-dev

# Helm
helm list -n bank-app-dev
helm test bank-app -n bank-app-dev
helm test bank-app-zipkin -n bank-app-dev
helm test bank-app-prometheus -n bank-app-dev
helm test bank-app-grafana -n bank-app-dev
helm test bank-app-elasticsearch -n bank-app-dev
helm test bank-app-logstash -n bank-app-dev
helm test bank-app-kibana -n bank-app-dev
```

## Security Requirements

### Grafana
- **Development**: Uses fixed password `admin123` (local development only)
- **Production**: Password must be set via environment variables or Kubernetes Secrets
  ```bash
  # Create secret for production
  kubectl create secret generic grafana-admin-credentials \
    --from-literal=admin-user=admin \
    --from-literal=admin-password=$(openssl rand -base64 32) \
    -n bank-app-prod
  
  # Update values.yaml to use the secret
  # Uncomment the admin.existingSecret section
  ```

### OAuth2
- Change client secrets before deploying to production
- Use external identity provider (Keycloak, Auth0, etc.) for production

