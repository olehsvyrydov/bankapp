# Bank Application - Kubernetes & Helm Transformation Context

## Project Overview
Microservices banking application requiring transformation from Docker Compose deployment to Kubernetes with Helm charts, including CI/CD with Jenkins.

## Current Architecture

### Microservices (12 services + 1 shared library)

#### Infrastructure Services
1. **eureka-server** (8761) - Netflix Eureka service discovery
2. **config-server** (8888) - Spring Cloud Config for centralized configuration
3. **gateway-service** (8100) - Spring Cloud Gateway (WebFlux) for API routing
4. **auth-server** (9100) - OAuth2 authorization server with JWT tokens

#### Business Services
5. **accounts-service** (8081) - User and bank account management
6. **transfer-service** (8083) - Money transfers between accounts
7. **cash-service** (8082) - Deposits and withdrawals
8. **exchange-service** (8084) - Currency exchange rates
9. **exchange-generator-service** (8085) - Periodic exchange rate generation
10. **notifications-service** (8087) - Email notifications
11. **blocker-service** (8086) - Fraud detection
12. **front-ui** (8090) - Thymeleaf web interface

#### Shared
13. **common-lib** - Shared DTOs, security configs, utilities

### Current Technology Stack
- **Framework**: Spring Boot 3.5.6, Spring Cloud 2025.0.0
- **Java**: 21
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Config Management**: Spring Cloud Config (file-based)
- **Security**: Spring Authorization Server, OAuth2, JWT
- **Inter-service Communication**: OpenFeign with OAuth2 client credentials
- **Resilience**: Resilience4j circuit breakers
- **Database**: PostgreSQL 14 (schema-per-service pattern)
- **Migrations**: Flyway
- **Caching**: Redis (exchange-service)
- **Containerization**: Docker with Docker Compose

### Current Communication Patterns

#### Service Discovery
- All services register with Eureka server
- Client-side discovery for service location
- DNS: http://eureka-server:8761/eureka/

#### Gateway Routing
```
/api/accounts/**          → accounts-service
/api/cash/**              → cash-service
/api/transfers/**         → transfer-service
/api/exchange/**          → exchange-service
/api/exchange/generate/** → exchange-generator-service
/api/notifications/**     → notifications-service
/api/blocker/**           → blocker-service
```

#### Inter-Service Communication
- REST over HTTP using OpenFeign
- OAuth2 client credentials for service-to-service auth
- Automatic token acquisition via `OAuth2FeignConfig`
- Circuit breakers with Resilience4j

### Database Configuration
- **Single PostgreSQL instance** with multiple schemas
- **Schemas**: auth, accounts, transfer, cash, exchange, notifications
- **Connection**: jdbc:postgresql://postgres:5432/bankdb
- **Credentials**: bank_user / bank_password

### Security Architecture
- **OAuth2 Authorization Server**: auth-server (port 9100)
- **Token Type**: JWT with 1-hour expiration
- **Service Credentials**: Each service has client_id/client_secret
- **User Flow**: Authorization code + refresh tokens
- **Service Flow**: Client credentials
- **Resource Servers**: All business services validate JWTs

### Docker Deployment
- **Network**: bank-network (bridge)
- **Orchestration**: docker-compose.yml
- **Startup Order**:
  1. PostgreSQL
  2. Eureka → Config Server
  3. Auth Server
  4. Business Services
  5. Exchange Generator
  6. Front-UI
- **Health Checks**: Spring Boot Actuator endpoints
- **Volumes**: postgres-data for persistence

## Transformation Requirements

### Primary Goals
1. ✅ Deploy to Kubernetes (Minikube/Kind/Colima)
2. ✅ Create Helm charts (umbrella + subcharts)
3. ✅ Replace Service Discovery: Eureka → Kubernetes Services + DNS
4. ✅ Replace Gateway: Spring Cloud Gateway → Kubernetes Ingress/Gateway API
5. ✅ Replace Config: Spring Cloud Config → ConfigMaps/Secrets
6. ✅ Deploy databases as StatefulSets
7. ✅ Deploy services as Deployments
8. ✅ Deploy OAuth2 server with Helm
9. ✅ Support multiple namespaces (dev, test, prod)
10. ✅ Write Helm chart tests
11. ✅ Create Jenkinsfiles for CI/CD
12. ❌ Remove: Eureka, Config Server, Spring Cloud dependencies

### Services to Remove/Replace
- **eureka-server** - Replace with Kubernetes Services + DNS
- **config-server** - Replace with ConfigMaps/Secrets
- Spring Cloud Netflix Eureka Client dependencies
- Spring Cloud Config dependencies
- Spring Cloud Gateway (keep as service, but route via Ingress)

### Services to Keep
- **gateway-service** - Keep as routing service (behind Ingress)
- **auth-server** - Keep with Helm chart deployment
- All business services - Keep with modified configurations

### Key Technical Challenges
1. **Service Discovery Migration**: Update all Feign clients to use Kubernetes Service DNS names
2. **Configuration Migration**: Move all externalized config from Config Server to ConfigMaps/Secrets
3. **Gateway Routing**: Ensure Ingress correctly routes to gateway-service, which routes to backend services
4. **OAuth2 URLs**: Update issuer URIs and token endpoints for Kubernetes service names
5. **Database StatefulSets**: Ensure data persistence with PVCs
6. **Health Checks**: Adapt Docker health checks to Kubernetes liveness/readiness probes

## Project Structure
```
bankapp/
├── accounts-service/
├── auth-server/
├── blocker-service/
├── cash-service/
├── common-lib/
├── config-server/          # TO BE REMOVED
├── eureka-server/          # TO BE REMOVED
├── exchange-generator-service/
├── exchange-service/
├── front-ui/
├── gateway-service/
├── notifications-service/
├── transfer-service/
├── pom.xml                 # Root POM
└── docker-compose.yml
```

## Success Criteria
1. All services deployable via Helm charts
2. Services communicate via Kubernetes DNS
3. Gateway accessible via Ingress
4. OAuth2 authentication working
5. Database persistence via StatefulSets
6. Multi-environment support (dev/test/prod namespaces)
7. Helm chart tests passing
8. Jenkins pipelines functional
9. No Eureka or Config Server dependencies
10. All integration tests passing

## Next Steps
See `helm-plan.md` for detailed transformation plan.
