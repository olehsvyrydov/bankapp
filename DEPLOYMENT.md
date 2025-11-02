# Развёртывание в Minikube через Jenkins

Этот сценарий показывает, как развернуть банковское приложение в локальном кластере Minikube с помощью Jenkins-пайплайнов из репозитория.

## 1. Требования

- **ОС**: Linux или WSL2 (поддержка `--network host` для контейнера Jenkins упрощает доступ к `localhost`).
- **Пакеты**: Docker 24+, Minikube 1.33+, kubectl 1.30+, Helm 3.15+, Git, Maven.
- **Ресурсы**: не менее 4 CPU и 8 ГБ RAM, свободные порты 8080 (Jenkins) и 5000 (локальный Registry/порт-forward).

## 2. Запуск Minikube

```bash
minikube start \
  --driver=docker \
  --cpus=4 \
  --memory=8192

minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable registry
```
> При необходимости указать `--insecure-registry`, сначала определите подсеть (обычно `192.168.49.0/24` для драйвера Docker) или запустите кластер, получите `minikube ip`, а затем перезапустите с нужным CIDR.


Проверьте состояние:

```bash
minikube status
kubectl get nodes -o wide
```

## 3. Доступ к встраиваемому Registry

Minikube поднимает Registry `registry.kube-system.svc.cluster.local:80`. Пробросьте его на localhost и держите процесс в отдельном окне:

```bash
kubectl port-forward -n kube-system service/registry 5000:80
```

Docker и Jenkins будут обращаться к registry как `localhost:5000`. Добавьте его в список небезопасных реестров Docker (если требуется):

```bash
cat <<JSON | sudo tee /etc/docker/daemon.json
{
  "insecure-registries": ["localhost:5000"]
}
JSON
sudo systemctl restart docker
```

## 4. Получение исходников

```bash
git clone https://github.com/<your-account>/bankapp.git
cd bankapp
```

## 5. Запуск Jenkins в контейнере

```bash
docker run -d \
  --name jenkins \
  --network host \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts-jdk17
```

> На macOS/Windows замените `--network host` на дефолтную сеть и используйте `host.docker.internal` вместо `localhost` при обращении к Registry.

## 6. Установка kubectl и Helm внутри Jenkins

```bash
docker exec -it jenkins bash -c "apt-get update && apt-get install -y curl gnupg lsb-release"
docker exec -it jenkins bash -c "curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && chmod +x kubectl && mv kubectl /usr/local/bin/"
docker exec -it jenkins bash -c "curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash"
```

## 7. Подготовка kubeconfig для Jenkins

Создайте файл kubeconfig с правами на весь кластер:

```bash
kubectl config view --minify --flatten > kubeconfig-minikube.yaml
```

В Jenkins перейдите в **Manage Jenkins → Credentials → Global → Add Credentials** и добавьте два `Secret file`:

- ID `kubeconfig-test` → файл `kubeconfig-minikube.yaml`
- ID `kubeconfig-prod` → тот же файл (для демонстрации можно использовать один и тот же контекст)

## 8. Docker-реквизиты (опционально)

Если Registry не требует авторизации, оставляйте параметр `DOCKER_CREDENTIALS_ID` в пайплайнах пустым. При необходимости создайте учётку типа `Username with password` и используйте её ID.

## 9. Настройка Jenkins-пайплайна

1. Зайдите на `http://localhost:8080`, завершите первоначальную настройку (админ-пароль находится в `docker logs jenkins | grep -i password`).
2. Установите рекомендованные плагины + **Docker Pipeline**, **Kubernetes**, **Git**, **Pipeline Utility Steps**.
3. Создайте новый **Pipeline** job, укажите Git-репозиторий проекта.
4. Выберите опцию «Pipeline script from SCM», Jenkinsfile по умолчанию: `jenkins/Jenkinsfile`.
5. При первом запуске задайте параметры:
   - `IMAGE_TAG` — произвольная метка (например, `local-$(date +%s)`).
  - `DOCKER_REGISTRY` — `localhost:5000/bank` (совокупность проброшенного registry и нужного namespace).
  - `DOCKER_CREDENTIALS_ID` — оставьте пустым, если registry без авторизации.
  - `KUBE_CONFIG_TEST` / `KUBE_CONFIG_PROD` — используйте созданные secret file.
  - `DEPLOY_TO_PROD` — включите флаг, если хотите повторно деплоить в namespace `prod` после ручного подтверждения.

## 10. Запуск и контроль пайплайна

1. Нажмите **Build with Parameters** и следите за стадиями:
   - Maven-сборка (`mvn clean verify`).
   - Сборка/публикация образов в `localhost:5000`.
   - `helm upgrade --install` в namespace `test` и запуск `helm test`.
   - При включённом `DEPLOY_TO_PROD` — ручной input и релиз в namespace `prod`.
2. Проверяйте ресурсы:

```bash
kubectl get ns
kubectl get pods -n test
kubectl get svc -n test
kubectl get ingress -n test
```

3. Для доступа к Ingress выполните `minikube tunnel` (требуется sudo). После этого UI будет доступен по `http://bank.local` (см. host в `helm/environments/dev.yaml`). Добавьте запись в `/etc/hosts`:

```
127.0.0.1 bank.local
```

## 11. Использование пайплайнов на отдельный сервис

Для точечных деплоев запускайте `jenkins/pipelines/<service>.Jenkinsfile` (создайте отдельные jobs). Параметры идентичны общему пайплайну, а деплой выполняется только для выбранного микросервиса.


## 12. Траблшутинг запуска Minikube

Если `minikube start` падает с сообщениями вроде
```
Unable to get control-plane node minikube host status: No such container: minikube
Failing to connect to https://registry.k8s.io/
error validating "...storageclass.yaml": connect: connection refused
```
предпримите следующие шаги:

### Стереть сломанный профиль

```bash
minikube delete --all --purge
```

### Запуск без `--insecure-registry=$(minikube ip)/24`

Флаг обращается к `minikube ip` до старта кластера; если контейнер удалён, команда завершается с ошибкой. Запускайте без него:

```bash
minikube start --driver=docker --cpus=4 --memory=8192
```

После успешного старта можно пробросить встроенный реестр и настроить Docker на доверие к `localhost:5000` (см. секцию 3).

### Проблемы с доступом к `registry.k8s.io`

### Ошибка `waiting for app.kubernetes.io/name=ingress-nginx pods`

При `minikube addons enable ingress` или другом аддоне процесс ждёт готовности подов и по таймауту завершает команду. Чаще всего это означает, что образ контроллера не смог скачаться или pod застрял в Pending/ContainerCreating.

1. Посмотрите на состояние:
   ```bash
   kubectl -n ingress-nginx get pods
   kubectl -n ingress-nginx describe pod ingress-nginx-controller
   ```
   Неуспешные pulls будут видны в секции Events.

2. Используйте зеркало или локальный registry (см. выше) и повторно примените манифесты. Поменять образ можно командой:
   ```bash
   kubectl -n ingress-nginx set image deployment/ingress-nginx-controller      controller=registry.aliyuncs.com/google_containers/ingress-nginx/controller:v1.12.2
   kubectl -n ingress-nginx set image job/ingress-nginx-admission-create      create=registry.aliyuncs.com/google_containers/ingress-nginx/kube-webhook-certgen:v1.5.3
   kubectl -n ingress-nginx set image job/ingress-nginx-admission-patch      patch=registry.aliyuncs.com/google_containers/ingress-nginx/kube-webhook-certgen:v1.5.3
   ```
   Затем дождитесь развёртывания:
   ```bash
   kubectl rollout status -n ingress-nginx deployment/ingress-nginx-controller
   ```

3. Если поды Pending из‑за нехватки ресурсов, увеличьте лимиты кластера (`minikube delete`, затем `minikube start --memory=8192 --cpus=4`).

После того как контроллер Ready, можно снова активировать аддон либо оставить как есть (он уже развернут).


Если контроль plane не может скачивать образы:
1. **Проверьте сетевой доступ**: убедитесь, что хост видит `registry.k8s.io`.
2. **Работа через прокси** – задайте переменные окружения или настройте прокси для Docker, чтобы Minikube контейнер наследовал параметры. Пример:
   ```bash
   export HTTPS_PROXY=http://user:pass@proxy:port
   export HTTP_PROXY=$HTTPS_PROXY
   export NO_PROXY=localhost,127.0.0.1
   minikube start --driver=docker --cpus=4 --memory=8192
   ```
3. **Используйте зеркало**: при отсутствии прямого доступа
   ```bash
   minikube start --driver=docker --cpus=4 --memory=8192      --image-repository registry.aliyuncs.com/google_containers
   ```
4. **Обновите версию**: начиная с 1.37.0 улучшена логика переподключения к репозиториям.

После успешного старта повторно включите аддоны:

```bash
minikube addons enable registry
minikube addons enable ingress
minikube addons enable metrics-server
```

Если включение аддона завершилось ошибкой, подождите готовности API сервера (`kubectl get nodes`/`kubectl get pods -A`) и запустите команду ещё раз.

## 13. Очистка окружения

```bash
minikube delete
docker stop jenkins && docker rm jenkins
rm -rf jenkins_home
```

Не забудьте остановить `kubectl port-forward` и `minikube tunnel`, а также удалить запись из `/etc/hosts`, если она больше не нужна.


```bash
minikube delete
docker stop jenkins && docker rm jenkins
rm -rf jenkins_home
```

Не забудьте остановить `kubectl port-forward` и `minikube tunnel`, а также удалить запись из `/etc/hosts`, если она больше не нужна.
