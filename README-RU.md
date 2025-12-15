# Банковское Приложение - Микросервисная Архитектура

Банковское приложение на основе микросервисов с комплексным мониторингом и логированием.

## Быстрый Старт

### Развертывание Локально (Minikube)

```bash
# Полная установка (сборка + развертывание + мониторинг + ELK)
./minikube-setup.sh all

# Развертывание только стека мониторинга
./minikube-setup.sh deploy-monitoring

# Развертывание только ELK стека
./minikube-setup.sh deploy-elk

# Запуск тестов
./minikube-setup.sh test
./minikube-setup.sh test-monitoring
./minikube-setup.sh test-elk
```

### Развертывание через Jenkins

1. **Основной Pipeline Приложения**: Jenkinsfile (корневой)
   - Собирает все сервисы
   - Развертывает в dev/test/prod
   - Запускает развертывание мониторинга

2. **Pipeline Стека Мониторинга**: monitoring/Jenkinsfile
   - Развертывает Zipkin, Prometheus, Grafana
   - Развертывает ELK Stack (Elasticsearch, Logstash, Kibana)

## Доступ к Сервисам

```bash
# Приложение
kubectl port-forward -n bank-app-dev svc/bank-app-front-ui 8090:8090
kubectl port-forward -n bank-app-dev svc/bank-app-gateway-service 8100:8100

# Мониторинг
kubectl port-forward -n bank-app-dev svc/bank-app-zipkin 9411:9411
kubectl port-forward -n bank-app-dev svc/bank-app-prometheus-server 9090:9090
kubectl port-forward -n bank-app-dev svc/bank-app-grafana 3000:3000

# Логирование
kubectl port-forward -n bank-app-dev svc/bank-app-kibana-kibana 5601:5601
```

Затем откройте:
- **UI Приложения**: http://localhost:8090
- **API Gateway**: http://localhost:8100
- **Zipkin (Трассировка)**: http://localhost:9411
- **Prometheus (Метрики)**: http://localhost:9090
- **Grafana (Дашборды)**: http://localhost:3000
  - Учетные данные для dev: `admin` / `admin123`
  - Для production используйте auto-generated пароль (см. ниже)
- **Kibana (Логи)**: http://localhost:5601

### Получение Автосгенерированного Пароля Grafana

Для production окружений пароль генерируется автоматически:

```bash
kubectl get secret bank-app-grafana -n bank-app-dev -o jsonpath='{.data.admin-password}' | base64 -d
```

## Микросервисы

- **auth-server** - OAuth2 аутентификация
- **gateway-service** - API Gateway
- **accounts-service** - Управление счетами
- **cash-service** - Кассовые операции
- **transfer-service** - Денежные переводы
- **exchange-service** - Обмен валют
- **exchange-generator-service** - Генератор курсов валют
- **blocker-service** - Обнаружение мошенничества
- **notifications-service** - Email/SMS уведомления
- **front-ui** - Веб-интерфейс

## Документация

- **[Руководство по Развертыванию](docs/DEPLOYMENT.md)** - Детальные инструкции по развертыванию для разных окружений
- **[Настройка Jenkins](docs/JENKINS_SETUP.md)** - Конфигурация и настройка CI/CD pipeline
- **[Устранение Проблем Grafana](docs/GRAFANA_TROUBLESHOOTING.md)** - Исправление проблемы "No Data" и проверка сбора метрик
- **[Устранение Проблем Minikube](docs/MINIKUBE_TROUBLESHOOTING.md)** - Исправление TLS timeouts и проблем подключения к кластеру
- **[Исправление Перезапусков Подов](docs/POD_RESTART_FIX.md)** - Решение проблемы постоянных перезапусков из-за лимитов ресурсов и проверок работоспособности

## Мониторинг и Логирование

### Распределенная Трассировка (Zipkin)
- Трассировка запросов между микросервисами
- Анализ производительности

### Метрики (Prometheus + Grafana)
- HTTP метрики (RPS, ошибки, задержки)
- JVM метрики (память, GC, потоки)
- Бизнес-метрики (логины, переводы, блокировки)

### Централизованное Логирование (ELK)
- Elasticsearch - Хранение логов
- Logstash - Обработка логов
- Kibana - Визуализация логов
- Логи включают trace ID для корреляции

## Структура Проекта

```
.
├── helm/
│   ├── bank-app/           # Основной Helm chart приложения
│   ├── zipkin/             # Helm chart Zipkin
│   ├── prometheus/         # Helm chart Prometheus
│   ├── grafana/            # Helm chart Grafana
│   └── elk/                # Helm charts ELK Stack
├── monitoring/
│   └── Jenkinsfile         # Pipeline развертывания мониторинга
├── Jenkinsfile             # Основной pipeline приложения
├── minikube-setup.sh       # Скрипт локального развертывания
└── [microservices]/        # Директории отдельных сервисов
```

## Справочник Команд

```bash
# Minikube
./minikube-setup.sh all          # Полная установка
./minikube-setup.sh deploy       # Развертывание app + monitoring + ELK
./minikube-setup.sh status       # Проверка статуса развертывания
./minikube-setup.sh clean        # Удаление всех развертываний

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

## Требования Безопасности

### Grafana
- **Разработка**: Используется фиксированный пароль `admin123` (только для локальной разработки)
- **Production**: Пароль должен быть установлен через переменные окружения или Kubernetes Secrets
  ```bash
  # Создание secret для production
  kubectl create secret generic grafana-admin-credentials \
    --from-literal=admin-user=admin \
    --from-literal=admin-password=$(openssl rand -base64 32) \
    -n bank-app-prod
  
  # Обновите values.yaml для использования secret
  # Раскомментируйте секцию admin.existingSecret
  ```

### OAuth2
- Измените секреты клиентов перед развертыванием в production
- Используйте внешний провайдер идентификации (Keycloak, Auth0, etc.) для production

## Архитектура

```
┌─────────────┐
│  Front UI   │
└──────┬──────┘
       │
┌──────▼──────────┐
│  API Gateway    │
└──────┬──────────┘
       │
       ├─────────────┬──────────────┬─────────────┬──────────────┐
       │             │              │             │              │
┌──────▼─────┐ ┌────▼────┐  ┌──────▼──────┐ ┌───▼────┐  ┌─────▼──────┐
│  Accounts  │ │  Cash   │  │  Transfer   │ │Exchange│  │   Blocker  │
│  Service   │ │ Service │  │   Service   │ │Service │  │   Service  │
└────────────┘ └─────────┘  └─────────────┘ └────────┘  └────────────┘
       │             │              │             │              │
       └─────────────┴──────────────┴─────────────┴──────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
            ┌───────▼──────┐ ┌─────▼─────┐  ┌─────▼──────┐
            │   Zipkin     │ │Prometheus │  │    ELK     │
            │ (Трассировка)│ │ (Метрики) │  │  (Логи)    │
            └──────────────┘ └───────────┘  └────────────┘
```

## Конфигурация Окружений

### Development (minikube)
- Одна реплика каждого сервиса
- Встроенная база данных PostgreSQL
- Фиксированные пароли для упрощения разработки
- Все сервисы мониторинга включены

### Test
- Множественные реплики для некоторых сервисов
- Выделенная база данных PostgreSQL
- Интеграционные тесты через Helm

### Production
- Горизонтальное автомасштабирование (HPA)
- Внешняя управляемая база данных
- Secrets из Vault/External Secrets
- Строгие network policies
- Resource limits и requests настроены

## Устранение Неполадок

### Проверка Логов
```bash
# Логи конкретного сервиса
kubectl logs -f deployment/bank-app-accounts-service -n bank-app-dev

# Логи всех контейнеров в поде
kubectl logs <pod-name> -n bank-app-dev --all-containers

# Предыдущие логи (если под перезапустился)
kubectl logs <pod-name> -n bank-app-dev --previous
```

### Проверка Событий
```bash
kubectl get events -n bank-app-dev --sort-by='.lastTimestamp'
```

### Проверка Состояния Подов
```bash
kubectl describe pod <pod-name> -n bank-app-dev
```

### Доступ к Базе Данных
```bash
kubectl port-forward -n bank-app-dev svc/bank-app-postgresql 5432:5432
psql -h localhost -U postgres -d bankdb
```

### Тесты Helm
```bash
# Запуск всех тестов
./minikube-setup.sh test

# Проверка логов тестовых подов
kubectl logs -n bank-app-dev <test-pod-name>

# Список всех тестовых подов
kubectl get pods -n bank-app-dev | grep test
```

## Вклад в Проект

1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/amazing-feature`)
3. Commit изменений (`git commit -m 'Add some amazing feature'`)
4. Push в branch (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

## Лицензия

Этот проект разработан для образовательных целей.

