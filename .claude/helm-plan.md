# Bank Application - Kubernetes & Helm Transformation Plan

## Overview
This document outlines the complete transformation plan for migrating the Bank microservices application from Docker Compose to Kubernetes with Helm charts, including removal of Spring Cloud components and implementation of CI/CD with Jenkins.

---

## Phase 1: Project Setup & Infrastructure

### 1.1 Create Helm Chart Structure
**Objective**: Set up umbrella chart and subchart structure

**Tasks**:
- [ ] Create root `helm/` directory
- [ ] Create umbrella chart: `helm/bank-app/`
  - [ ] Chart.yaml with metadata
  - [ ] values.yaml with global configurations
  - [ ] templates/ directory
  - [ ] charts/ directory for subcharts
- [ ] Create subchart structure for each service:
  - [ ] helm/bank-app/charts/auth-server/
  - [ ] helm/bank-app/charts/accounts-service/
  - [ ] helm/bank-app/charts/transfer-service/
  - [ ] helm/bank-app/charts/cash-service/
  - [ ] helm/bank-app/charts/exchange-service/
  - [ ] helm/bank-app/charts/exchange-generator-service/
  - [ ] helm/bank-app/charts/notifications-service/
  - [ ] helm/bank-app/charts/blocker-service/
  - [ ] helm/bank-app/charts/front-ui/
  - [ ] helm/bank-app/charts/gateway-service/
  - [ ] helm/bank-app/charts/postgresql/ (StatefulSet)

**Deliverables**:
- Complete Helm chart directory structure
- Base Chart.yaml and values.yaml files

---

## Phase 2: Database StatefulSets

### 2.1 PostgreSQL StatefulSet
**Objective**: Deploy PostgreSQL as StatefulSet with persistent volumes

**Tasks**:
- [ ] Create PostgreSQL subchart with:
  - [ ] StatefulSet manifest
  - [ ] PersistentVolumeClaim (PVC) template
  - [ ] Service (headless + regular)
  - [ ] ConfigMap for init scripts (create schemas)
  - [ ] Secret for database credentials
- [ ] Configure multiple database schemas (auth, accounts, transfer, cash, exchange, notifications)
- [ ] Add init container to create schemas
- [ ] Configure volume mounts for data persistence
- [ ] Set resource limits and requests
- [ ] Add liveness and readiness probes

**Configuration**:
```yaml
# Example structure
postgresql:
  image: postgres:14-alpine
  storageClass: standard
  storageSize: 10Gi
  replicas: 1
  database: bankdb
  schemas:
    - auth
    - accounts
    - transfer
    - cash
    - exchange
    - notifications
```

### 2.2 Redis StatefulSet
**Note**: Redis will NOT be used in this project. The exchange-service will function without caching.

**Deliverables**:
- PostgreSQL StatefulSet with multi-schema support
- PVCs for data retention
- Kubernetes Services for database access

---

## Phase 3: ConfigMaps and Secrets

### 3.1 Global ConfigMaps
**Objective**: Replace Spring Cloud Config with Kubernetes ConfigMaps

**Tasks**:
- [ ] Create global ConfigMap for shared settings:
  - [ ] Database connection strings (non-sensitive)
  - [ ] Service URLs (Kubernetes DNS names)
  - [ ] OAuth2 issuer URI
  - [ ] Logging configurations
  - [ ] Feature flags
- [ ] Create service-specific ConfigMaps:
  - [ ] accounts-service-config
  - [ ] transfer-service-config
  - [ ] cash-service-config
  - [ ] exchange-service-config
  - [ ] exchange-generator-config
  - [ ] notifications-service-config (SMTP settings)
  - [ ] blocker-service-config (thresholds)
  - [ ] gateway-service-config (routes)
  - [ ] front-ui-config (session settings)
  - [ ] auth-server-config (token settings)

### 3.2 Secrets Management
**Objective**: Secure sensitive configuration data

**Tasks**:
- [ ] Create Secrets for:
  - [ ] Database credentials
  - [ ] OAuth2 client secrets (for each service)
  - [ ] JWT signing keys
  - [ ] SMTP credentials (notifications)
- [ ] Use Helm's `lookup` function for existing secrets
- [ ] Document secret creation in README

**Configuration Structure**:
```yaml
# Example ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: bank-app-global-config
data:
  POSTGRES_HOST: postgresql.bank-app.svc.cluster.local
  POSTGRES_PORT: "5432"
  POSTGRES_DB: bankdb
  AUTH_SERVER_ISSUER_URI: http://auth-server:9100

# Example Secret
apiVersion: v1
kind: Secret
metadata:
  name: bank-app-secrets
type: Opaque
stringData:
  POSTGRES_USERNAME: bank_user
  POSTGRES_PASSWORD: bank_password
  JWT_SECRET: <random-secret>
```

**Deliverables**:
- ConfigMaps for all service configurations
- Secrets for sensitive data
- values.yaml structure for environment-specific overrides

---

## Phase 4: Service Deployments & Services

### 4.1 Auth Server Deployment
**Objective**: Deploy OAuth2 authorization server

**Tasks**:
- [ ] Create Deployment manifest
  - [ ] Container spec with environment variables from ConfigMap/Secret
  - [ ] Volume mounts for ConfigMap/Secret
  - [ ] Resource requests/limits
  - [ ] Liveness probe: /actuator/health
  - [ ] Readiness probe: /actuator/health
  - [ ] Init container to wait for PostgreSQL
- [ ] Create Service (ClusterIP)
- [ ] Update application.yml to use Kubernetes ConfigMaps/Secrets
- [ ] Remove Spring Cloud Config client dependency
- [ ] Set replica count: 1 (or more for HA)

### 4.2 Business Services Deployments
**Objective**: Deploy all business microservices

**Tasks** (repeat for each service):
- [ ] **accounts-service**
  - [ ] Deployment with ConfigMap/Secret injection
  - [ ] Service (ClusterIP)
  - [ ] Update Feign clients to use Kubernetes DNS names
  - [ ] Remove Eureka client dependency
  - [ ] Update OAuth2 configuration
- [ ] **transfer-service**
  - [ ] Same pattern as accounts-service
  - [ ] Update Feign clients (accounts, exchange, notifications)
- [ ] **cash-service**
  - [ ] Deployment and Service
  - [ ] Update Feign clients
- [ ] **exchange-service**
  - [ ] Deployment and Service
- [ ] **exchange-generator-service**
  - [ ] Deployment and Service
  - [ ] Update Feign client to exchange-service
- [ ] **notifications-service**
  - [ ] Include SMTP configuration
  - [ ] Deployment and Service
- [ ] **blocker-service**
  - [ ] Deployment and Service (stateless)

### 4.3 Gateway Service Deployment
**Objective**: Deploy Spring Cloud Gateway as internal routing service

**Tasks**:
- [ ] Create Deployment
- [ ] Update routes to use Kubernetes Service names:
  ```yaml
  routes:
    - id: accounts-service
      uri: http://accounts-service:8081
      predicates:
        - Path=/api/accounts/**
  ```
- [ ] Remove Eureka client dependency
- [ ] Keep gateway as routing layer (will be exposed via Ingress)
- [ ] Create Service (ClusterIP)

### 4.4 Front-UI Deployment
**Objective**: Deploy web interface

**Tasks**:
- [ ] Create Deployment
- [ ] Update service URLs to use Kubernetes DNS
- [ ] Remove Eureka client
- [ ] Create Service (ClusterIP)

**Common Deployment Patterns**:
```yaml
# Standard deployment structure
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "service.fullname" . }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app: {{ include "service.name" . }}
  template:
    metadata:
      labels:
        app: {{ include "service.name" . }}
    spec:
      initContainers:
        - name: wait-for-postgres
          image: busybox
          command: ['sh', '-c', 'until nc -z postgresql 5432; do sleep 2; done']
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          ports:
            - containerPort: {{ .Values.service.port }}
          envFrom:
            - configMapRef:
                name: {{ include "service.fullname" . }}-config
            - secretRef:
                name: bank-app-secrets
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: {{ .Values.service.port }}
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: {{ .Values.service.port }}
            initialDelaySeconds: 30
            periodSeconds: 5
```

**Deliverables**:
- Deployments for all 10 services
- Kubernetes Services (ClusterIP) for each
- Init containers for startup dependencies
- Liveness and readiness probes

---

## Phase 5: Service Discovery Migration

### 5.1 Remove Eureka Dependencies
**Objective**: Eliminate Netflix Eureka from all services

**Tasks**:
- [ ] Remove from pom.xml files:
  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  ```
- [ ] Remove `@EnableEurekaClient` annotations
- [ ] Remove Eureka configuration from application.yml:
  ```yaml
  eureka:
    client:
      service-url:
        defaultZone: ...
  ```
- [ ] Delete eureka-server module entirely

### 5.2 Add Spring Cloud Kubernetes Dependencies
**Objective**: Use Spring Cloud Kubernetes for service discovery via Kubernetes API

**Tasks**:
- [ ] Add to root pom.xml dependency management:
  ```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-kubernetes-dependencies</artifactId>
        <version>3.1.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ```
- [ ] Add to each service pom.xml (except auth-server, postgresql):
  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-kubernetes-fabric8-all</artifactId>
  </dependency>
  ```
- [ ] Enable Kubernetes discovery in application.yml:
  ```yaml
  spring:
    cloud:
      kubernetes:
        enabled: true
        discovery:
          enabled: true
          all-namespaces: false
  ```

### 5.3 Keep Feign Client Declarations Simple
**Objective**: Use Kubernetes service discovery with Feign clients by name only

**Tasks**:
- [ ] Keep Feign client declarations simple (Spring Cloud Kubernetes will resolve service names):
  ```java
  // This works with Spring Cloud Kubernetes - no URL needed!
  @FeignClient(name = "accounts-service")
  public interface AccountsClient {
      @GetMapping("/api/accounts/{id}")
      AccountDto getAccount(@PathVariable Long id);
  }
  ```
- [ ] Spring Cloud Kubernetes automatically resolves:
  - `accounts-service` → `http://accounts-service:8081`
  - `transfer-service` → `http://transfer-service:8083`
  - `cash-service` → `http://cash-service:8082`
  - etc.
- [ ] No ConfigMap URLs needed - Kubernetes Services are discovered automatically!

### 5.4 Update OAuth2 Configuration
**Objective**: Use Kubernetes Service names for OAuth2 endpoints

**Tasks**:
- [ ] Update issuer URI in all services:
  ```yaml
  spring.security.oauth2.resourceserver.jwt.issuer-uri: http://auth-server:9100
  ```
- [ ] Update token URI in OAuth2 clients:
  ```yaml
  spring.security.oauth2.client.provider.bank-client.token-uri: http://auth-server:9100/oauth2/token
  ```
- [ ] Ensure JWT issuer claim matches Kubernetes service name

**Deliverables**:
- Eureka completely removed from project
- Spring Cloud Kubernetes added to all services
- Feign clients using Kubernetes service discovery (by name only)
- OAuth2 endpoints updated
- Services communicating via Kubernetes Services with automatic DNS resolution

---

## Phase 6: Config Server Migration

### 6.1 Remove Spring Cloud Config
**Objective**: Eliminate Config Server and migrate to ConfigMaps

**Tasks**:
- [ ] Remove from all service pom.xml:
  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  ```
- [ ] Remove bootstrap.yml/bootstrap.properties from all services
- [ ] Remove config-server module entirely
- [ ] Update application.yml to use environment variables:
  ```yaml
  spring:
    datasource:
      url: ${POSTGRES_URL}
      username: ${POSTGRES_USERNAME}
      password: ${POSTGRES_PASSWORD}
  ```

### 6.2 Migrate Configuration to ConfigMaps
**Objective**: Move all externalized config to Kubernetes ConfigMaps

**Tasks**:
- [ ] Create ConfigMap templates in each subchart
- [ ] Map all Config Server properties to ConfigMap keys
- [ ] Use environment-specific values files:
  - [ ] values-dev.yaml
  - [ ] values-test.yaml
  - [ ] values-prod.yaml
- [ ] Test configuration injection in Deployments

**Example Migration**:
```yaml
# OLD: config-server/src/main/resources/config/accounts-service.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/bankdb
    username: bank_user

# NEW: helm/bank-app/charts/accounts-service/templates/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: accounts-service-config
data:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgresql:5432/bankdb"
  SPRING_DATASOURCE_USERNAME: "bank_user"
  SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA: "accounts"
```

**Deliverables**:
- Config Server removed
- All configuration in ConfigMaps
- Environment-specific values files
- Documentation of configuration structure

---

## Phase 7: Gateway API / Ingress

### 7.1 Choose Gateway Implementation
**Decision Point**: Gateway API vs. Ingress Controller

**Recommendation**: Use **Nginx Ingress Controller** (simpler, widely supported)

**Alternative**: Kubernetes Gateway API (newer standard)

### 7.2 Deploy Ingress Controller
**Objective**: Set up Nginx Ingress Controller in Minikube/Kind

**Tasks**:
- [ ] Install Nginx Ingress Controller:
  ```bash
  # Minikube
  minikube addons enable ingress

  # Kind
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
  ```
- [ ] Verify Ingress Controller pods running

### 7.3 Create Ingress Resources
**Objective**: Route external traffic to gateway-service

**Tasks**:
- [ ] Create Ingress manifest in umbrella chart:
  ```yaml
  apiVersion: networking.k8s.io/v1
  kind: Ingress
  metadata:
    name: bank-app-ingress
    annotations:
      nginx.ingress.kubernetes.io/rewrite-target: /
  spec:
    ingressClassName: nginx
    rules:
      - host: bank-app.local  # Add to /etc/hosts
        http:
          paths:
            - path: /
              pathType: Prefix
              backend:
                service:
                  name: front-ui
                  port:
                    number: 8090
            - path: /api
              pathType: Prefix
              backend:
                service:
                  name: gateway-service
                  port:
                    number: 8100
            - path: /oauth2
              pathType: Prefix
              backend:
                service:
                  name: auth-server
                  port:
                    number: 9100
  ```
- [ ] Update /etc/hosts:
  ```
  <minikube-ip> bank-app.local
  ```
- [ ] Test routing: http://bank-app.local/

### 7.4 Update Frontend URLs
**Objective**: Update front-ui to use Ingress URLs

**Tasks**:
- [ ] Update front-ui Feign clients to use internal service URLs
- [ ] Update OAuth2 redirect URIs to use Ingress hostname
- [ ] Update CORS settings in gateway-service

**Deliverables**:
- Ingress Controller deployed
- Ingress resource routing to services
- External access via domain name
- Gateway-service handling internal routing

---

## Phase 8: Namespace Strategy

### 8.1 Multi-Environment Setup
**Objective**: Support dev, test, prod namespaces

**Tasks**:
- [ ] Create namespace definitions:
  ```yaml
  # helm/bank-app/templates/namespace.yaml
  apiVersion: v1
  kind: Namespace
  metadata:
    name: {{ .Values.namespace }}
    labels:
      environment: {{ .Values.environment }}
  ```
- [ ] Create environment-specific values files:
  - [ ] values-dev.yaml
    ```yaml
    namespace: bank-app-dev
    environment: dev
    replicaCount: 1
    resources:
      limits:
        memory: 512Mi
    ```
  - [ ] values-test.yaml
    ```yaml
    namespace: bank-app-test
    environment: test
    replicaCount: 1
    resources:
      limits:
        memory: 1Gi
    ```
  - [ ] values-prod.yaml
    ```yaml
    namespace: bank-app-prod
    environment: prod
    replicaCount: 2
    resources:
      limits:
        memory: 2Gi
    ```

### 8.2 Environment-Specific Configurations
**Objective**: Customize settings per environment

**Tasks**:
- [ ] Database settings per environment
- [ ] Replica counts (dev: 1, prod: 2+)
- [ ] Resource limits (dev: low, prod: high)
- [ ] Ingress hostnames (dev.bank-app.local, prod.bank-app.local)
- [ ] Enable/disable features (debug logging in dev)

**Deployment Commands**:
```bash
# Development
helm install bank-app ./helm/bank-app -f values-dev.yaml --namespace bank-app-dev --create-namespace

# Test
helm install bank-app ./helm/bank-app -f values-test.yaml --namespace bank-app-test --create-namespace

# Production
helm install bank-app ./helm/bank-app -f values-prod.yaml --namespace bank-app-prod --create-namespace
```

**Deliverables**:
- Namespace templates
- Environment-specific values files
- Documentation for multi-environment deployment

---

## Phase 9: Helm Chart Testing

### 9.1 Unit Tests for Helm Charts
**Objective**: Validate Helm template rendering

**Tasks**:
- [ ] Install helm-unittest plugin:
  ```bash
  helm plugin install https://github.com/helm-unittest/helm-unittest
  ```
- [ ] Create test files for each subchart:
  - [ ] tests/deployment_test.yaml
  - [ ] tests/service_test.yaml
  - [ ] tests/configmap_test.yaml
  - [ ] tests/secret_test.yaml
- [ ] Write test cases:
  ```yaml
  # Example: helm/bank-app/charts/accounts-service/tests/deployment_test.yaml
  suite: test accounts-service deployment
  templates:
    - deployment.yaml
  tests:
    - it: should create deployment
      asserts:
        - isKind:
            of: Deployment
        - equal:
            path: metadata.name
            value: accounts-service
        - equal:
            path: spec.replicas
            value: 1
  ```
- [ ] Run tests:
  ```bash
  helm unittest helm/bank-app/charts/accounts-service/
  ```

### 9.2 Helm Test Hooks
**Objective**: Integration tests post-deployment

**Tasks**:
- [ ] Create test pods in templates/tests/:
  ```yaml
  # templates/tests/test-connection.yaml
  apiVersion: v1
  kind: Pod
  metadata:
    name: "{{ .Release.Name }}-test-connection"
    annotations:
      "helm.sh/hook": test
  spec:
    containers:
      - name: wget
        image: busybox
        command: ['wget']
        args: ['{{ include "service.fullname" . }}:{{ .Values.service.port }}']
    restartPolicy: Never
  ```
- [ ] Create health check tests
- [ ] Create OAuth2 token acquisition test
- [ ] Run tests:
  ```bash
  helm test bank-app
  ```

### 9.3 Linting and Validation
**Objective**: Ensure chart quality

**Tasks**:
- [ ] Run helm lint:
  ```bash
  helm lint helm/bank-app
  helm lint helm/bank-app/charts/*
  ```
- [ ] Validate with --dry-run:
  ```bash
  helm install --dry-run --debug bank-app ./helm/bank-app
  ```
- [ ] Fix all linting errors and warnings

**Deliverables**:
- Comprehensive unit tests for all charts
- Integration test hooks
- Linting validation passing
- Test documentation

---

## Phase 10: CI/CD with Jenkins

### 10.1 Jenkinsfile Structure
**Objective**: Create pipelines for each service and umbrella

**Pipeline Types**:
1. **Individual Service Pipelines**: Build and deploy single service
2. **Umbrella Pipeline**: Build and deploy entire application

### 10.2 Individual Service Jenkinsfile
**Objective**: CI/CD for single microservice

**Tasks**:
- [ ] Create Jenkinsfile in each service directory
- [ ] Define pipeline stages:
  1. **Checkout**: Clone repository
  2. **Validate**: Helm lint, unit tests
  3. **Build**: Maven package (skip tests initially)
  4. **Test**: Run unit tests with Maven
  5. **Docker Build**: Build Docker image
  6. **Docker Push**: Push to registry (optional)
  7. **Deploy to Test**: Helm upgrade to test namespace
  8. **Integration Tests**: Run Helm tests
  9. **Deploy to Prod**: Manual approval + Helm upgrade

**Example Jenkinsfile**:
```groovy
// accounts-service/Jenkinsfile
pipeline {
    agent any

    environment {
        SERVICE_NAME = 'accounts-service'
        DOCKER_REGISTRY = 'localhost:5000' // or Docker Hub
        HELM_CHART_PATH = "helm/bank-app/charts/${SERVICE_NAME}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Helm Chart') {
            steps {
                sh "helm lint ${HELM_CHART_PATH}"
                sh "helm unittest ${HELM_CHART_PATH}"
            }
        }

        stage('Build Maven') {
            steps {
                dir(SERVICE_NAME) {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Run Tests') {
            steps {
                dir(SERVICE_NAME) {
                    sh 'mvn test'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir(SERVICE_NAME) {
                    sh "docker build -t ${DOCKER_REGISTRY}/${SERVICE_NAME}:${BUILD_NUMBER} ."
                    sh "docker tag ${DOCKER_REGISTRY}/${SERVICE_NAME}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${SERVICE_NAME}:latest"
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                sh "docker push ${DOCKER_REGISTRY}/${SERVICE_NAME}:${BUILD_NUMBER}"
                sh "docker push ${DOCKER_REGISTRY}/${SERVICE_NAME}:latest"
            }
        }

        stage('Deploy to Test') {
            steps {
                sh """
                    helm upgrade --install ${SERVICE_NAME} ${HELM_CHART_PATH} \
                        --namespace bank-app-test \
                        --create-namespace \
                        --set image.tag=${BUILD_NUMBER} \
                        --values helm/bank-app/values-test.yaml \
                        --wait
                """
            }
        }

        stage('Run Integration Tests') {
            steps {
                sh "helm test ${SERVICE_NAME} --namespace bank-app-test"
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                sh """
                    helm upgrade --install ${SERVICE_NAME} ${HELM_CHART_PATH} \
                        --namespace bank-app-prod \
                        --create-namespace \
                        --set image.tag=${BUILD_NUMBER} \
                        --values helm/bank-app/values-prod.yaml \
                        --wait
                """
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            cleanWs()
        }
    }
}
```

### 10.3 Umbrella Jenkinsfile
**Objective**: Deploy entire application

**Tasks**:
- [ ] Create Jenkinsfile in root directory
- [ ] Build all services in parallel
- [ ] Deploy umbrella chart
- [ ] Run end-to-end tests

**Example Umbrella Jenkinsfile**:
```groovy
// Jenkinsfile (root)
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'localhost:5000'
        HELM_CHART_PATH = 'helm/bank-app'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Umbrella Chart') {
            steps {
                sh "helm lint ${HELM_CHART_PATH}"
                sh "helm dependency update ${HELM_CHART_PATH}"
            }
        }

        stage('Build All Services') {
            parallel {
                stage('Build auth-server') {
                    steps {
                        dir('auth-server') {
                            sh 'mvn clean package -DskipTests'
                            sh "docker build -t ${DOCKER_REGISTRY}/auth-server:${BUILD_NUMBER} ."
                        }
                    }
                }
                stage('Build accounts-service') {
                    steps {
                        dir('accounts-service') {
                            sh 'mvn clean package -DskipTests'
                            sh "docker build -t ${DOCKER_REGISTRY}/accounts-service:${BUILD_NUMBER} ."
                        }
                    }
                }
                // Repeat for all services...
            }
        }

        stage('Run All Tests') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Push All Images') {
            steps {
                script {
                    def services = ['auth-server', 'accounts-service', 'transfer-service',
                                    'cash-service', 'exchange-service', 'exchange-generator-service',
                                    'notifications-service', 'blocker-service', 'gateway-service', 'front-ui']
                    services.each { service ->
                        sh "docker push ${DOCKER_REGISTRY}/${service}:${BUILD_NUMBER}"
                    }
                }
            }
        }

        stage('Deploy to Test') {
            steps {
                sh """
                    helm upgrade --install bank-app ${HELM_CHART_PATH} \
                        --namespace bank-app-test \
                        --create-namespace \
                        --set global.image.tag=${BUILD_NUMBER} \
                        --values ${HELM_CHART_PATH}/values-test.yaml \
                        --wait \
                        --timeout 10m
                """
            }
        }

        stage('Run Integration Tests') {
            steps {
                sh "helm test bank-app --namespace bank-app-test"
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy entire app to production?', ok: 'Deploy'
                sh """
                    helm upgrade --install bank-app ${HELM_CHART_PATH} \
                        --namespace bank-app-prod \
                        --create-namespace \
                        --set global.image.tag=${BUILD_NUMBER} \
                        --values ${HELM_CHART_PATH}/values-prod.yaml \
                        --wait \
                        --timeout 10m
                """
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
        }
        failure {
            mail to: 'team@example.com',
                 subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                 body: "Check Jenkins for details: ${env.BUILD_URL}"
        }
    }
}
```

### 10.4 Jenkins Setup
**Objective**: Configure Jenkins for Kubernetes deployment

**Tasks**:
- [ ] Install Jenkins plugins:
  - [ ] Kubernetes CLI Plugin
  - [ ] Docker Pipeline Plugin
  - [ ] Pipeline Plugin
  - [ ] Git Plugin
- [ ] Configure credentials:
  - [ ] Kubernetes config (kubeconfig)
  - [ ] Docker registry credentials
  - [ ] Git credentials
- [ ] Create Jenkins jobs:
  - [ ] Multibranch Pipeline for umbrella
  - [ ] Individual pipelines for each service
- [ ] Configure webhooks for automatic builds

**Deliverables**:
- Jenkinsfile for each service
- Umbrella Jenkinsfile
- Jenkins job configurations
- CI/CD pipeline documentation

---

## Phase 11: Code Refactoring & Cleanup

### 11.1 Remove Spring Cloud Dependencies
**Objective**: Clean up unused dependencies

**Tasks**:
- [ ] Update root pom.xml:
  - [ ] Remove Spring Cloud Netflix dependency management
  - [ ] Remove Spring Cloud Config dependency management
  - [ ] Keep Spring Cloud LoadBalancer (for Feign)
- [ ] Update service pom.xml files:
  - [ ] Remove eureka-client
  - [ ] Remove config-client
  - [ ] Keep feign dependencies
  - [ ] Keep resilience4j
- [ ] Delete modules:
  - [ ] eureka-server/
  - [ ] config-server/

### 11.2 Update Application Properties
**Objective**: Remove legacy configuration

**Tasks**:
- [ ] Remove from all application.yml:
  ```yaml
  # Remove these sections
  eureka:
    client:
      service-url:
        defaultZone: ...

  spring:
    cloud:
      config:
        uri: ...
  ```
- [ ] Add Kubernetes-specific properties:
  ```yaml
  spring:
    application:
      name: ${SERVICE_NAME}
    datasource:
      url: ${SPRING_DATASOURCE_URL}
      username: ${SPRING_DATASOURCE_USERNAME}
      password: ${SPRING_DATASOURCE_PASSWORD}
  ```

### 11.3 Update Feign Clients
**Objective**: Use explicit URLs instead of discovery

**Example Refactoring**:
```java
// OLD
@FeignClient(name = "accounts-service")
public interface AccountsClient {
    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable Long id);
}

// NEW
@FeignClient(
    name = "accounts-service",
    url = "${services.accounts.url}"  // From ConfigMap
)
public interface AccountsClient {
    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable Long id);
}
```

**Tasks**:
- [ ] Update all Feign clients in:
  - [ ] accounts-service (NotificationClient)
  - [ ] transfer-service (AccountsClient, ExchangeClient, NotificationsClient, BlockerClient)
  - [ ] cash-service (AccountsClient, NotificationsClient, BlockerClient)
  - [ ] exchange-generator-service (ExchangeClient)

### 11.4 Update Gateway Routes
**Objective**: Use Kubernetes Service names

**Tasks**:
- [ ] Update gateway-service routes:
  ```yaml
  spring:
    cloud:
      gateway:
        routes:
          - id: accounts-service
            uri: http://accounts-service:8081  # Kubernetes Service
            predicates:
              - Path=/api/accounts/**
  ```

### 11.5 Update OAuth2 Configuration
**Objective**: Use Kubernetes service names in OAuth2 config

**Tasks**:
- [ ] Update issuer URI in all services
- [ ] Update auth-server JWT issuer to use Kubernetes service name
- [ ] Ensure consistency across all services

**Deliverables**:
- Spring Cloud Netflix removed
- Spring Cloud Config removed
- All Feign clients updated
- Gateway routes updated
- Clean, Kubernetes-native configuration

---

## Phase 12: Testing & Validation

### 12.1 Unit Tests
**Objective**: Ensure all unit tests pass

**Tasks**:
- [ ] Run Maven tests for all services:
  ```bash
  mvn test
  ```
- [ ] Fix any broken tests due to configuration changes
- [ ] Update mocked URLs in tests

### 12.2 Integration Tests
**Objective**: Test service-to-service communication

**Tasks**:
- [ ] Deploy to test namespace:
  ```bash
  helm install bank-app ./helm/bank-app -f values-test.yaml --namespace bank-app-test --create-namespace
  ```
- [ ] Verify all pods running:
  ```bash
  kubectl get pods -n bank-app-test
  ```
- [ ] Test OAuth2 token acquisition:
  ```bash
  curl -X POST http://auth-server.bank-app-test:9100/oauth2/token \
       -d "grant_type=client_credentials" \
       -d "client_id=accounts-service" \
       -d "client_secret=accounts-secret"
  ```
- [ ] Test inter-service communication:
  ```bash
  # Port-forward gateway
  kubectl port-forward svc/gateway-service 8100:8100 -n bank-app-test

  # Test accounts API
  curl -H "Authorization: Bearer <token>" http://localhost:8100/api/accounts
  ```

### 12.3 End-to-End Tests
**Objective**: Validate full user workflows

**Tasks**:
- [ ] Access Front-UI via Ingress:
  ```bash
  echo "$(minikube ip) bank-app.local" | sudo tee -a /etc/hosts
  ```
- [ ] Test user registration flow
- [ ] Test login flow
- [ ] Test account creation
- [ ] Test cash deposit/withdrawal
- [ ] Test transfer between own accounts
- [ ] Test transfer to another user
- [ ] Verify notifications sent
- [ ] Check exchange rate display

### 12.4 Performance Testing
**Objective**: Ensure acceptable performance in Kubernetes

**Tasks**:
- [ ] Load test with multiple concurrent users
- [ ] Monitor pod resource usage:
  ```bash
  kubectl top pods -n bank-app-test
  ```
- [ ] Check database connections
- [ ] Verify circuit breakers functioning

**Deliverables**:
- All unit tests passing
- Integration tests successful
- End-to-end workflows verified
- Performance benchmarks documented

---

## Phase 13: Documentation

### 13.1 Update README.md
**Objective**: Comprehensive deployment documentation

**Tasks**:
- [ ] Document prerequisites:
  - Kubernetes cluster (Minikube/Kind/Colima)
  - Helm 3.x
  - kubectl
  - Docker
  - Maven 3.x, Java 21
- [ ] Document local development setup
- [ ] Document Helm chart structure
- [ ] Document deployment commands:
  ```bash
  # Build all services
  mvn clean package -DskipTests

  # Build Docker images
  docker-compose build

  # Deploy to Kubernetes
  helm install bank-app ./helm/bank-app -f values-dev.yaml --namespace bank-app-dev --create-namespace

  # Access application
  kubectl port-forward svc/front-ui 8090:8090 -n bank-app-dev
  # Or via Ingress: http://bank-app.local
  ```
- [ ] Document Jenkins setup
- [ ] Document configuration management (ConfigMaps/Secrets)
- [ ] Document troubleshooting steps

### 13.2 Helm Chart Documentation
**Objective**: Document chart usage

**Tasks**:
- [ ] Create helm/bank-app/README.md
- [ ] Document values.yaml parameters
- [ ] Document subchart structure
- [ ] Provide examples for common scenarios
- [ ] Document upgrade strategies

### 13.3 Architecture Diagrams
**Objective**: Visual representation of new architecture

**Tasks**:
- [ ] Create Kubernetes architecture diagram
- [ ] Create service communication diagram
- [ ] Create CI/CD pipeline diagram
- [ ] Include in main README.md

**Deliverables**:
- Comprehensive README.md
- Helm chart documentation
- Architecture diagrams
- Troubleshooting guide

---

## Phase 14: Final Steps

### 14.1 Version Tagging
**Objective**: Mark v2.0 release

**Tasks**:
- [ ] Review all changes
- [ ] Ensure all tests passing
- [ ] Commit final changes:
  ```bash
  git add .
  git commit -m "Complete Kubernetes & Helm transformation"
  ```
- [ ] Tag release:
  ```bash
  git tag -a v2.0 -m "Version 2.0: Kubernetes deployment with Helm charts"
  ```
- [ ] Push to GitHub:
  ```bash
  git push origin main
  git push origin v2.0
  ```

### 14.2 Final Validation
**Objective**: Comprehensive system check

**Tasks**:
- [ ] Deploy to all three environments (dev, test, prod)
- [ ] Run all Helm tests
- [ ] Verify Jenkins pipelines
- [ ] Check all documentation
- [ ] Perform security scan (if available)

**Deliverables**:
- Git tag v2.0
- All environments deployed successfully
- Documentation complete
- Project ready for review

---

## Implementation Checklist Summary

### Phase 1: Setup
- [ ] Create Helm chart structure (umbrella + subcharts)

### Phase 2: Databases
- [ ] PostgreSQL StatefulSet with PVC

### Phase 3: Configuration
- [ ] Create ConfigMaps for all services
- [ ] Create Secrets for sensitive data

### Phase 4: Deployments
- [ ] Deploy auth-server
- [ ] Deploy business services (accounts, transfer, cash, exchange, etc.)
- [ ] Deploy gateway-service
- [ ] Deploy front-ui

### Phase 5: Service Discovery
- [ ] Remove Eureka server
- [ ] Update Feign clients to use Kubernetes DNS

### Phase 6: Config Management
- [ ] Remove Config Server
- [ ] Migrate all config to ConfigMaps

### Phase 7: Gateway/Ingress
- [ ] Deploy Nginx Ingress Controller
- [ ] Create Ingress resources
- [ ] Update gateway routes

### Phase 8: Multi-Environment
- [ ] Create namespace templates
- [ ] Create values-dev.yaml, values-test.yaml, values-prod.yaml

### Phase 9: Testing
- [ ] Write Helm unit tests
- [ ] Write Helm integration tests
- [ ] Run helm lint

### Phase 10: CI/CD
- [ ] Create Jenkinsfile for each service
- [ ] Create umbrella Jenkinsfile
- [ ] Configure Jenkins

### Phase 11: Cleanup
- [ ] Remove Spring Cloud dependencies
- [ ] Delete eureka-server and config-server modules
- [ ] Update all application.yml files

### Phase 12: Validation
- [ ] Run all unit tests
- [ ] Run integration tests
- [ ] Perform end-to-end testing

### Phase 13: Documentation
- [ ] Update README.md
- [ ] Create Helm chart documentation
- [ ] Create architecture diagrams

### Phase 14: Release
- [ ] Final review
- [ ] Git tag v2.0
- [ ] Push to GitHub

---

## Risk Mitigation

### Potential Issues & Solutions

1. **Service Startup Dependencies**
   - Risk: Services start before database ready
   - Solution: Use init containers to wait for dependencies

2. **OAuth2 Token Propagation**
   - Risk: Services can't authenticate with each other
   - Solution: Verify OAuth2 client credentials configured correctly

3. **Database Schema Isolation**
   - Risk: Schema permissions issues
   - Solution: Use PostgreSQL init scripts to create schemas with proper permissions

4. **Ingress Routing**
   - Risk: Complex routing not working
   - Solution: Start with simple paths, test incrementally

5. **ConfigMap Size Limits**
   - Risk: ConfigMaps too large
   - Solution: Split large configs, use multiple ConfigMaps

6. **StatefulSet Data Loss**
   - Risk: Data lost on pod restart
   - Solution: Proper PVC configuration with retain policy

7. **Jenkins Permissions**
   - Risk: Jenkins can't access Kubernetes
   - Solution: Configure proper RBAC roles and kubeconfig

---

## Success Metrics

- ✅ Zero Eureka dependencies
- ✅ Zero Config Server dependencies
- ✅ All services deployed via Helm
- ✅ All services communicating via Kubernetes DNS
- ✅ Gateway accessible via Ingress
- ✅ OAuth2 authentication working
- ✅ Databases running as StatefulSets
- ✅ Multi-environment support (dev/test/prod)
- ✅ Helm tests passing
- ✅ Jenkins pipelines functional
- ✅ All unit/integration tests passing
- ✅ Documentation complete
- ✅ Git tag v2.0 created

---

## Next Actions

1. Review this plan with the team
2. Begin Phase 1: Create Helm chart structure
3. Work through phases sequentially
4. Commit frequently (micro-commits)
5. Test at each phase
6. Update this plan as needed

---

**Estimated Timeline**: 2-3 weeks (depending on complexity and team size)

**Priority Order**: Phases 1-7 are critical path. Phases 8-14 can be parallelized.
