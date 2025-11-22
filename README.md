# Bank Application - Microservices Architecture

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
kubectl port-forward -n bank-app-dev svc/bank-app-zipkin-zipkin 9411:9411
kubectl port-forward -n bank-app-dev svc/bank-app-prometheus-server 9090:80
kubectl port-forward -n bank-app-dev svc/bank-app-grafana 3000:80

# Logging
kubectl port-forward -n bank-app-dev svc/bank-app-kibana-kibana 5601:5601
```

Then open:
- **Application UI**: http://localhost:8090
- **API Gateway**: http://localhost:8100
- **Zipkin (Tracing)**: http://localhost:9411
- **Prometheus (Metrics)**: http://localhost:9090
- **Grafana (Dashboards)**: http://localhost:3000
- **Kibana (Logs)**: http://localhost:5601

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
helm test bank-app-elasticsearch -n bank-app-dev
```
