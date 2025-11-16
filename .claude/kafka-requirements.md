# Using kafka requirements

Tasks:
1. Add Apache Kafka to the project
2. Implement sending notifications to Notifications via Apache Kafka
3. Implement interaction between Exchange and Exchange Generator via Apache Kafka

Requirements:
В проект должна быть добавлена платформа Apache Kafka.
Платформа Apache Kafka должна быть развёрнута в Kubernetes с использованием Helm.
Платформа Apache Kafka может быть развёрнута в каждом из пространств имён Kubernetes (dev, test, prod в зависимости от того, какие окружения использовались при развёртывании микросервисов) или же в единственном экземпляре в своём пространстве имён (например, default), при этом к ней должны иметь доступ микросервисы.
Платформа Apache Kafka должна быть развёрнута на двух нодах, которые должны быть объединены в кластер (при недостаточности ресурсов допускается развёртывание на одной ноде).
При остановке/перезапуске/падении подов содержимое топиков, добавленных в Apache Kafka, не должно удаляться.
Взаимодействие с микросервисом Notifications должно происходить без использования REST, но с использованием платформы Apache Kafka:
Должны быть заведены соответствующие топики на нодах кластера (или на одной ноде, если Apache Kafka развёрнута только на одной).
Должна быть реализована отправка сообщений в соответствии со стратегией At least once.
Допускается не поддерживать очерёдность отправки сообщений (unordered messages).
При перезапуске/падении микросервис Notifications должен начинать обрабатывать сообщения с последнего прочитанного сообщения.
Должны быть написаны соответствующие тесты на интеграцию с Apache Kafka и на взаимодействие между микросервисами через неё.
Взаимодействие между микросервисами Exchange и Exchange Generator должно происходить без использования REST, но с использованием платформы Apache Kafka:
Должны быть заведены соответствующие топики на нодах кластера (или на одной ноде, если Apache Kafka развёрнута только на одной).
Должна быть реализована отправка сообщений в соответствии со стратегией At most once.
Необходимо поддерживать очерёдность отправки сообщений (ordered messages).
При перезапуске/падении микросервис Exchange может обрабатывать сообщения начиная с последнего (то есть пропустить предыдущие, так как они уже неактуальны).
Должны быть написаны соответствующие тесты на интеграцию с Apache Kafka и на взаимодействие между микросервисами через неё.
Развёртывание и обновление платформы Apache Kafka, а также её конфигураций, топиков и т. д. должны осуществляться через отдельный пайплайн и пайплайн umbrella-проекта (Jenkinsfile) в CI/CD Jenkins.
Jenkinsfile должны храниться в Git, и должна быть возможность их применения в CI/CD Jenkins.


Functionality
preflow:
- during the initialization, kafka topic NOTIFICATION should be created.
- creating kafka producer and consumer beans should be added to common-lib. The configuration should be set up globally in onw place to easily maintain.
- All feign clients, which send notifications to notification service should be replaced with kafka producer sending related message.
Updates in services:
1. Accounts service sends notifications (in JSON format) to according Kafka topic, which will be read by Notifications service. You should replace a related feign client with kafka producer
2. Cash service sends notifications (in JSON format) to according Kafka topic, which will be read by Notifications service. You should replace a related feign client with kafka producer
3. Transfer service sends notifications (in JSON format) to according Kafka topic, which will be read by Notifications service. You should replace a related feign client with kafka producer
4. Exchange service Сервис reads information messages about currency exchange (in JSON format) from related kafka topic and saves them inside the service (in db).
5. Exchange Generator service sends message about currency exchange (in JSON format) into related Kafka topic, with reads Exchange service.
6. Notification service reads notification message from related Kafka topic and sends notification to client (email, Alert) with information about money transfer, deposit money, withdraw money, etc.
7. Use retry topics in case messages have not been sent immediately with configurable settings.
8. Use dead letter topic in case message could not achievable after several retries.
postflow:
- writing integration tests for kafka producer and consumer in each service.
- writing integration tests for interaction between services via kafka.
- updating helm charts and umbrella chart to deploy kafka cluster in k8s.
- use embeded kafka for integration tests.

use tdd approach
