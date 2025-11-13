# Банковское приложение (Yandex Study Project)

Много сервисов, которые вместе предоставляют функционал банковского приложения: регистрация пользователей, управление счетами, денежные операции, обмен валют, уведомления и т.д. В проекте использованы Spring Boot, Spring Cloud, PostgreSQL, Docker Compose/Kubernetes и фронт на Thymeleaf.

## Требования

- Java 21 и Maven 3.9+
- Docker (для сборки образов, загрузки в Minikube)
- kubectl 1.27+, Helm 3.12+
- Minikube 1.33+ (для локального Kubernetes)

> Docker Compose ранее использовался для локального запуска, но модули `eureka-server` и `config-server` больше не поставляются с Dockerfile'ами, поэтому `docker compose up --build` не работает. Используйте Minikube/Helm варианты ниже.

## 🚀 Развертывание и использование

### 1. Minikube (через helper-скрипт)
1. Выполните `./minikube-setup.sh all`.
   - Скрипт стартует Minikube (8 ГБ RAM, 4 CPU, драйвер docker), переключает docker-env, собирает и загружает образы, затем делает `helm upgrade --install bank-app`.
2. После окончания добавьте `$(minikube ip) bank-app-dev.local` в `/etc/hosts`.
3. При необходимости запустите `minikube tunnel`, чтобы ingress IP был доступен с хоста.
4. Откройте `http://bank-app-dev.local/`.
5. Повторный деплой:
   - `./minikube-setup.sh deploy` — только `helm upgrade`.
   - `./minikube-setup.sh redeploy` — `helm uninstall` + новое развёртывание.
   - `./minikube-setup.sh status` — быстрая проверка Minikube, pods и сервисов.
   - `./minikube-setup.sh clean` — удаление релиза и namespace.

### 2. Kubernetes + Helm (ручное развертывание)
1. Создайте namespace:
   ```bash
   kubectl create namespace bank-app-dev
   ```
2. Установите чарт:
   ```bash
   helm install bank-app ./helm/bank-app \
     -f ./helm/bank-app/values-dev.yaml \
     --namespace bank-app-dev \
     --create-namespace \
     --wait --timeout 10m
   ```
3. Для обновлений используйте `helm upgrade bank-app ./helm/bank-app -f helm/bank-app/values-dev.yaml --namespace bank-app-dev --wait`.
4. Пропишите `INGRESS_IP bank-app-dev.local` в `/etc/hosts` (для Minikube возьмите `minikube ip`).
5. Проверьте состояние:
   ```bash
   kubectl get pods -n bank-app-dev
   kubectl logs -n bank-app-dev <pod>
   ```
6. Удаление:
   ```bash
   helm uninstall bank-app -n bank-app-dev
   kubectl delete namespace bank-app-dev
   ```
7. **Важно:** оставляйте `spring.cloud.kubernetes.loadbalancer.mode=SERVICE` (см. `global.loadBalancer.mode`), чтобы первые запросы не зависали на ожидании Endpoints.

## Проверка состояния

```bash
kubectl get pods -n bank-app-dev
kubectl get svc -n bank-app-dev
kubectl logs -n bank-app-dev <pod>
```

## Пользователи по умолчанию

В `auth-server` при старте создаются учетные данные:
- **admin** / `password` – администратор.
- **tester** / `password` – тестовый пользователь.

У каждого пользователя есть набор банковских счетов (создаются в `accounts-service` миграциями). Для входа в UI используйте логин/пароль и затем работайте с личными счетами, переводами и пр.
