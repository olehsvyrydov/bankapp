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
- `config-server` + `eureka-server` – конфигурации и сервис-дискавери.
- `front-ui` – пользовательский веб-интерфейс.

## Запуск

Требования: Docker и Docker Compose, Java 21, Maven. Из корня репозитория:
```bash
cd bankapp-common/bankapp
mvn -DskipTests package
docker compose up --build
```
Стек поднимается несколько минут. UI доступен на http://localhost:8090, Gateway на http://localhost:8080, Eureka – http://localhost:8761.

## Тесты

```bash
mvn test
```
Большинство модулей имеют unit/integration тесты (фронт, transfer, auth и др.).

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
