# Repository Guidelines

## Project Structure & Module Organization
- Maven multi-module root (`pom.xml`) with services under `*/src/main/java` and tests in `*/src/test/java`.
- Core modules: `common-lib` (shared DTOs/utils), `gateway-service` (API gateway), `auth-server`, `accounts-service`, `cash-service`, `transfer-service`, `exchange-service`, `exchange-generator-service`, `blocker-service`, `notifications-service`, `front-ui`. Supporting infra: `config-server`, `eureka-server`.
- Ops assets: Helm chart in `helm/bank-app`, deployment helper `minikube-setup.sh`, CI config `Jenkinsfile`, docs in `docs/`.
- SQL migrations live in each service’s `src/main/resources/db/migration`; static templates for UI in `front-ui/src/main/resources/templates`.

## Build, Test, and Development Commands
- `mvn clean install` — full build with tests for all modules.
- `mvn -pl <module> -am spring-boot:run` — run a single service locally (e.g., `accounts-service`).
- `mvn -pl <module> test` — scoped test execution.
- `./minikube-setup.sh all` — build images, load to Minikube, install Helm release for end-to-end checks. `./minikube-setup.sh deploy|redeploy` for incremental redeploys.
- Helm direct: `helm upgrade --install bank-app ./helm/bank-app -f ./helm/bank-app/values-dev.yaml --namespace bank-app-dev --create-namespace --wait`.

## Coding Style & Naming Conventions
- Java 21, Spring Boot 3.x; prefer 4-space indentation. Keep packages under `com.bank.<service>`.
- Favor Lombok (`@RequiredArgsConstructor`, `@Slf4j`) to reduce boilerplate; use MapStruct for DTO mapping.
- Classes and DTOs in PascalCase, methods/fields in camelCase, constants UPPER_SNAKE_CASE. Request/response contracts should reuse `common-lib` types.
- Place configuration in `application.yml`/`bootstrap.yml`; keep environment-specific values externalized.

## Testing Guidelines
- JUnit 5 with Spring Boot Test, AssertJ; integration tests use Embedded Kafka/Testcontainers where needed (e.g., `*IntegrationTest` in `accounts-service`).
- Naming: unit specs `*Test`, integrations `*IntegrationTest`. Keep tests deterministic and isolated; prefer Testcontainers/embedded brokers over external deps.
- Run `mvn test` before pushing; for faster feedback, run module-scoped tests for touched services.

## Commit & Pull Request Guidelines
- Follow concise, imperative commit subjects (e.g., `Add kafka for notifications`, `Remove legacy clients`).
- PRs should include: problem statement, summary of changes, affected services/modules, test commands executed, and doc/Helm updates if behavior changes. Add screenshots for UI changes (`front-ui`).
- Link issues/tickets when available and note any config/secret requirements for reviewers.

## Security & Configuration Tips
- Do not commit secrets; rely on environment variables/Kubernetes secrets referenced in `values-*.yaml` and `application.yml`.
- Kafka/PostgreSQL endpoints and credentials should come from Config Server or Helm values; keep defaults suitable for local dev only.
- If adding new external integrations, document required properties under `docs/` and provide sane local fallbacks.
