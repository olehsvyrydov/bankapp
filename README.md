# Банковское приложение (Yandex Study Project)

Много сервисов, которые вместе предоставляют функционал банковского приложения: регистрация пользователей, управление счетами, денежные операции, обмен валют, уведомления и т.д. В проекте использованы Spring Boot, Spring Cloud, PostgreSQL, Docker Compose и фронт на Thymeleaf.

## Структура

- `auth-server` – OAuth2 сервер авторизации, выдает токены и управляет пользователями.
- `accounts-service` – учетные записи клиентов, банковские счета, балансы.
- `cash-service` – депозиты и снятие средств.
- `transfer-service` – переводы между счетами и пользователями.
- `exchange-service`, `exchange-generator-service` – курсы валют и их генерация.
- `blocker-service` – проверка подозрительных операций.
- `notifications-service` – рассылка уведомлений.
- `gateway-service` – API gateway (Spring Cloud Gateway).
- `front-ui` – пользовательский веб-интерфейс.

> Сервис-дискавери и конфигурация выполняются средствами Kubernetes/Docker DNS и ConfigMap/Secret, поэтому отдельные компоненты вроде Eureka или Spring Cloud Config больше не используются.

## Запуск

Требования: Docker и Docker Compose, Java 21, Maven. Из корня репозитория:
```bash
cd bankapp-common/bankapp
mvn -DskipTests package
docker compose up --build
```
Стек поднимается несколько минут. UI доступен на http://localhost:8090, Gateway на http://localhost:8080.

### Развёртывание в Kubernetes с Helm

В каталоге `helm/` находится зонтичный чарт `bankapp` и сабчарты для каждого сервиса. Они используют ConfigMap/Secret для конфигурации, StatefulSet — для баз данных и Ingress/Gateway API вместо Spring Cloud Gateway/Eureka.

1. Соберите зависимости и проверьте шаблоны:
   ```bash
   helm dependency update helm/bankapp
   helm lint helm/bankapp
   ```

2. Выберите окружение (`helm/environments/dev.yaml`, `test.yaml`, `prod.yaml`) и при необходимости создайте кастомный values-файл.

3. Установите приложение:
   ```bash
   kubectl create namespace dev
   helm upgrade --install bankapp-dev helm/bankapp \
     --namespace dev \
     -f helm/environments/dev.yaml
   ```

4. Для обновления образов укажите репозиторий/тэг:
   ```bash
   helm upgrade bankapp-dev helm/bankapp \
     --namespace dev \
     -f helm/environments/dev.yaml \
     --set accounts.image.repository=registry.example.com/bank/accounts-service \
     --set accounts.image.tag=1.2.3
   ```

Каждый сабчарт можно применять отдельно, например:
```bash
helm upgrade --install accounts helm/services/accounts \
  --namespace dev \
  --set image.repository=registry.example.com/bank/accounts-service \
  --set image.tag=1.2.3 \
  -f helm/environments/dev.yaml
```

Для проверки после релиза выполните:
```bash
helm test bankapp-dev --namespace dev
```

### CI/CD в Jenkins

Пайплайны хранятся в каталоге `jenkins/`:
- `jenkins/Jenkinsfile` — зонтичный конвейер, который последовательно собирает все сервисы, публикует образы и раскатывает `helm/bankapp` в тестовое и (по подтверждению) продуктивное пространство.
- `jenkins/pipelines/<service>.Jenkinsfile` — конвейеры для каждого микросервиса: сборка Maven, сборка/публикация образа, `helm upgrade` в тест/прод, прогон `helm test`.

Ожидается, что в Jenkins настроены креденшелы для Docker Registry (`DOCKER_CREDENTIALS_ID`) и kubeconfig для тестового/продового кластеров (`KUBE_CONFIG_TEST`, `KUBE_CONFIG_PROD`). Параметр `IMAGE_TAG` позволяет управлять версией образов, флаг `DEPLOY_TO_PROD` включает автоматический релиз после подтверждения.

## Тесты

```bash
mvn test
```
Большинство модулей имеют unit/integration тесты (фронт, transfer, auth и др.).

## Документация

- [Локальный деплой в Minikube через Jenkins](DEPLOYMENT.md)

## Пользователи по умолчанию

В `auth-server` при старте создаются учетные данные:
- **admin** / `password` – администратор.
- **tester** / `password` – тестовый пользователь.

У каждого пользователя есть набор банковских счетов (создаются в `accounts-service` миграциями). Для входа в UI используйте логин/пароль и затем работайте с личными счетами, переводами и пр.

## Полезные команды

- Сборка без тестов: `mvn -DskipTests package`
- Локальный запуск отдельных сервисов: `mvn spring-boot:run -pl <module>`
- Просмотр логов docker: `docker compose logs <service>`
- Остановка стека: `docker compose down`

## Использование

После входа в веб-приложение:

1. Просматривать счета и балансы.
2. Открывать новые счета (до 3 валют – RUB, USD, CNY).
3. Вносить средства и снимать деньги (cash-service).
4. Делать переводы между своими счетами и на счета других пользователей (transfer-service) с проверкой и подтверждением получателя.
5. Смотреть курсы валют (exchange-service) и уведомления.

## Примечания

- Профиль `docker` активируется автоматически в контейнерах.
- Для локальной разработки вне docker используйте профиль `default` и свои настройки в `application.yml`.
- Если появляются ошибки доступа к сервисам – убедитесь, что все контейнеры находятся в состоянии `healthy` (`docker compose ps`).
- Модули `config-server` и `eureka-server` удалены: конфигурация и сервис-дискавери выполняются средствами Kubernetes/Docker DNS и ConfigMap/Secret.
