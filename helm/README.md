# Helm Charts

This directory hosts the Helm charts used to deploy the Bank microservice platform.

- `bankapp/` – umbrella chart that aggregates all microservices and infrastructure dependencies.  
  Устанавливается целиком (`helm upgrade --install bankapp helm/bankapp -f environments/dev.yaml`).
- `services/` – переиспользуемые сабчарты для микросервисов (accounts, cash, …) и инфраструктуры (postgres). Их можно применить отдельно.
- `environments/` – значения для dev/test/prod. Дополняйте/создавайте свои файлы по необходимости.

## Быстрый старт

```bash
helm dependency update bankapp
helm lint bankapp
helm upgrade --install bankapp-dev bankapp -n dev -f environments/dev.yaml
helm test bankapp-dev -n dev
```

Каждый сабчарт поддерживает стандартные настройки:

- `image.repository` / `image.tag` – контейнер образа.
- `config.configMap` и `config.secret` – внешние настройки (формируют `ConfigMap`/`Secret`, подхватываемые как `envFrom`).
- `database.enabled` – развёртывание PostgreSQL `StatefulSet` для сервиса.
- `tests.enabled` – включает `helm test` (curl/psql probes).

Стандартные override-файлы лежат в `environments/`. Дополнительные параметры передавайте через `--set`/`-f`.

## FAQ

**Почему в каждом сервисном чарте хранится манифест БД, если есть общий `postgres`?**  
`services/postgres` разворачивает сам инстанс PostgreSQL. Манифесты внутри чарта микросервиса описывают _приложенческий слой_: отдельный секрет с паролем, Service/StatefulSet с нужным именем схемы и привязкой к Flyway, параметры readiness. Их можно выключить (`database.enabled=false`), если БД уже управляется извне. Такой подход позволяет хранить конфигурацию продажного сервиса рядом с кодом и не раздувать зонтичный чарт десятками специфических overrides.

**Нужен ли `Chart.lock`?**  
Да. Файл фиксирует версии зависимостей, которые попали в `helm/bankapp/charts/` после `helm dependency update`. Храните его в Git, чтобы CI/CD и локальная разработка использовали одинаковые версии подчартов. Если `Chart.lock` удалить, Helm подтянет самые свежие версии зависимостей, что может незаметно сломать совместимость.
