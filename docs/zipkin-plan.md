# План реализации мониторинга и логирования для микросервисного приложения "Банк"

## Общая информация

**Версия проекта**: v4.0
**Подход**: TDD (Test-Driven Development) где применимо
**Стратегия коммитов**: Микрокоммиты

---

## Фаза 1: Подготовка инфраструктуры

### 1.1 Создание структуры Helm charts для мониторинга

**TDD подход**: Сначала создаем тесты для Helm charts

**Шаги**:

1. **Создать структуру директорий** (микрокоммит)
   ```
   helm/
   ├── zipkin/
   ├── prometheus/
   ├── grafana/
   └── elk/
       ├── elasticsearch/
       ├── logstash/
       └── kibana/
   ```

2. **Zipkin Helm chart** (микрокоммит на каждый файл)
   - Создать `Chart.yaml` с зависимостями
   - Использовать официальный Helm chart: `openzipkin/zipkin`
   - Создать `values.yaml` с конфигурацией:
     - In-memory storage или PostgreSQL
     - 1 реплика для dev
     - Service type: ClusterIP
     - Порт: 9411
   - Создать шаблоны если нужна кастомизация
   - **Тест**: Создать `templates/tests/test-connection.yaml` для проверки доступности Zipkin

3. **Prometheus Helm chart** (микрокоммит на каждый файл)
   - Использовать `prometheus-community/prometheus` или `prometheus-community/kube-prometheus-stack`
   - Создать `values.yaml`:
     - ServiceMonitor для автообнаружения микросервисов
     - Retention: 15 дней
     - Storage: emptyDir или PVC
     - Scrape interval: 15s
   - Настроить правила алертов (`prometheusrules.yaml`):
     - High error rate (5xx > 5%)
     - High response time (p95 > 1s)
     - Failed login attempts (> 10 в минуту)
     - Failed transfers (> 5 в минуту)
     - Exchange rates not updated (> 1 час)
   - **Тест**: Helm test для проверки доступности Prometheus

4. **Grafana Helm chart** (микрокоммит на каждый файл)
   - Использовать `grafana/grafana`
   - Создать `values.yaml`:
     - Admin credentials
     - Datasources: Prometheus
     - Dashboard providers (ConfigMaps)
   - Создать ConfigMaps для дашбордов:
     - `dashboards/http-metrics.json` - HTTP метрики (RPS, 4xx, 5xx, latency)
     - `dashboards/jvm-metrics.json` - JVM метрики (память, GC, threads)
     - `dashboards/business-metrics.json` - Бизнес метрики
   - Настроить алерты в Grafana (если не используются Prometheus алерты)
   - **Тест**: Helm test для проверки доступности Grafana

5. **ELK Stack Helm charts** (микрокоммит на каждый компонент)

   **Elasticsearch**:
   - Использовать `elastic/elasticsearch`
   - `values.yaml`:
     - Single node для dev
     - Индексы с retention 7 дней
     - Heap size: 512m
   - **Тест**: Проверка health endpoint

   **Logstash**:
   - Использовать `elastic/logstash`
   - `values.yaml`:
     - Input: Kafka consumer (topic: `bank.logs`)
     - Filters:
       - Grok pattern для парсинга логов
       - JSON parser
       - Mutate для маскировки паролей/счетов
     - Output: Elasticsearch
   - Создать `logstash.conf` как ConfigMap
   - **Тест**: Проверка pipeline

   **Kibana**:
   - Использовать `elastic/kibana`
   - `values.yaml`:
     - Connection к Elasticsearch
     - Index patterns (автоматическое создание)
   - **Тест**: Проверка доступности UI

### 1.2 Создание Kafka топика для логов

**Шаги**:
1. Обновить `helm/bank-app/values.yaml`:
   ```yaml
   customKafka:
     topics:
       - name: bank.logs
         partitions: 5
         replicationFactor: 1
   ```
2. **Тест**: Проверить создание топика после развертывания

---

## Фаза 2: Интеграция Zipkin (Tracing)

### 2.1 TDD: Написание тестов для трейсинга

**RED фаза** (микрокоммит):

1. **Создать интеграционный тест** в `common-lib/src/test/java/.../tracing/`:
   ```java
   @SpringBootTest
   @AutoConfigureObservability
   class TracingIntegrationTest {
       @Test
       void shouldPropagateTraceId() {
           // Тест проверяет, что trace ID передается между сервисами
       }

       @Test
       void shouldCreateSpanForDatabaseQuery() {
           // Тест проверяет создание span для БД запросов
       }

       @Test
       void shouldCreateSpanForKafkaProducer() {
           // Тест проверяет создание span для Kafka
       }
   }
   ```

2. **Запустить тесты** - они должны упасть (RED)

### 2.2 GREEN фаза: Реализация трейсинга

**Шаги** (микрокоммит на каждый):

1. **Обновить parent POM** (`pom.xml`):
   ```xml
   <properties>
       <micrometer-tracing.version>1.4.1</micrometer-tracing.version>
       <brave.version>6.0.3</brave.version>
       <zipkin-reporter.version>3.4.2</zipkin-reporter.version>
   </properties>

   <dependencyManagement>
       <dependencies>
           <!-- Micrometer Tracing -->
           <dependency>
               <groupId>io.micrometer</groupId>
               <artifactId>micrometer-tracing-bom</artifactId>
               <version>${micrometer-tracing.version}</version>
               <type>pom</type>
               <scope>import</scope>
           </dependency>

           <dependency>
               <groupId>io.micrometer</groupId>
               <artifactId>micrometer-tracing-bridge-brave</artifactId>
           </dependency>

           <dependency>
               <groupId>io.zipkin.reporter2</groupId>
               <artifactId>zipkin-reporter-brave</artifactId>
               <version>${zipkin-reporter.version}</version>
           </dependency>
       </dependencies>
   </dependencyManagement>
   ```

2. **Добавить зависимости в каждый микросервис** (pom.xml):
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-tracing-bridge-brave</artifactId>
   </dependency>
   <dependency>
       <groupId>io.zipkin.reporter2</groupId>
       <artifactId>zipkin-reporter-brave</artifactId>
   </dependency>
   ```

3. **Создать общую конфигурацию в common-lib** (`src/main/resources/application-tracing.yml`):
   ```yaml
   management:
     tracing:
       sampling:
         probability: 1.0  # 100% для dev, 0.1 для prod
       enabled: true
     zipkin:
       tracing:
         endpoint: ${ZIPKIN_URL:http://bank-app-zipkin:9411}/api/v2/spans

   spring:
     application:
       name: ${spring.application.name}
   ```

4. **Включить tracing профиль** в каждом микросервисе (`application.yml`):
   ```yaml
   spring:
     profiles:
       include: tracing
   ```

5. **Настроить трейсинг для HTTP** (автоматически через Spring Boot)

6. **Настроить трейсинг для JPA** (автоматически через Hibernate integration)

7. **Настроить трейсинг для Kafka** - создать конфигурацию:
   ```java
   @Configuration
   public class KafkaTracingConfiguration {
       @Bean
       public KafkaTemplate<String, Object> kafkaTemplate(
               ProducerFactory<String, Object> producerFactory,
               ObservationRegistry observationRegistry) {
           KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
           template.setObservationEnabled(true);
           return template;
       }
   }
   ```

8. **Запустить тесты** - они должны пройти (GREEN)

### 2.3 REFACTOR фаза

**Шаги**:
1. Создать базовый класс `TracingConfiguration` в common-lib
2. Оптимизировать sampling для production (0.1)
3. Добавить custom tags для важных операций
4. Микрокоммит после рефакторинга

---

## Фаза 3: Интеграция Prometheus (Metrics)

### 3.1 TDD: Написание тестов для метрик

**RED фаза** (микрокоммит):

1. **Создать тесты для кастомных метрик**:
   ```java
   @SpringBootTest
   class MetricsIntegrationTest {
       @Autowired
       private MeterRegistry meterRegistry;

       @Test
       void shouldRecordLoginAttempts() {
           // Проверка метрики login_attempts_total
       }

       @Test
       void shouldRecordFailedTransfers() {
           // Проверка метрики transfer_failed_total
       }

       @Test
       void shouldRecordBlockedOperations() {
           // Проверка метрики blocked_operations_total
       }
   }
   ```

2. **Запустить тесты** - RED

### 3.2 GREEN фаза: Реализация метрик

**Шаги** (микрокоммит на каждый):

1. **Обновить parent POM**:
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

2. **Добавить зависимости в микросервисы** (уже есть `spring-boot-starter-actuator`)

3. **Создать общую конфигурацию метрик** (`common-lib/src/main/resources/application-metrics.yml`):
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
     metrics:
       tags:
         application: ${spring.application.name}
         environment: ${ENVIRONMENT:dev}
       distribution:
         percentiles-histogram:
           http.server.requests: true
         slo:
           http.server.requests: 50ms,100ms,200ms,500ms,1s,2s
     prometheus:
       metrics:
         export:
           enabled: true
   ```

4. **Включить metrics профиль** в каждом микросервисе

5. **Создать сервис для кастомных метрик** в common-lib:
   ```java
   @Service
   public class CustomMetricsService {
       private final Counter loginSuccessCounter;
       private final Counter loginFailureCounter;
       private final Counter transferFailureCounter;
       private final Counter blockedOperationsCounter;
       // ... другие метрики

       public CustomMetricsService(MeterRegistry registry) {
           this.loginSuccessCounter = Counter.builder("login_attempts_total")
               .tag("status", "success")
               .description("Total successful login attempts")
               .register(registry);
           // ... инициализация других метрик
       }

       public void recordLoginSuccess(String username) {
           loginSuccessCounter.increment();
       }
       // ... другие методы
   }
   ```

6. **Интегрировать метрики в микросервисы**:

   **auth-server** - логин метрики:
   ```java
   @Service
   public class AuthService {
       @Autowired
       private CustomMetricsService metricsService;

       public void login(String username, String password) {
           try {
               // логика логина
               metricsService.recordLoginSuccess(username);
           } catch (Exception e) {
               metricsService.recordLoginFailure(username);
               throw e;
           }
       }
   }
   ```

   **transfer-service** - метрики переводов:
   ```java
   @Service
   public class TransferService {
       @Autowired
       private CustomMetricsService metricsService;

       public void transfer(...) {
           try {
               // логика перевода
           } catch (InsufficientFundsException e) {
               metricsService.recordFailedTransfer(fromAccount, toAccount, "insufficient_funds");
               throw e;
           }
       }
   }
   ```

   **blocker-service** - метрики блокировок:
   ```java
   @Service
   public class BlockerService {
       @Autowired
       private CustomMetricsService metricsService;

       public boolean checkOperation(...) {
           if (isSuspicious) {
               metricsService.recordBlockedOperation(fromAccount, toAccount);
               return false;
           }
           return true;
       }
   }
   ```

   **notifications-service** - метрики уведомлений:
   ```java
   @Service
   public class NotificationService {
       @Autowired
       private CustomMetricsService metricsService;

       public void sendNotification(...) {
           try {
               // отправка
           } catch (Exception e) {
               metricsService.recordFailedNotification(username);
               throw e;
           }
       }
   }
   ```

   **exchange-generator-service** - метрики курсов:
   ```java
   @Scheduled(fixedRate = 3600000)
   public void updateRates() {
       try {
           // обновление курсов
           metricsService.recordExchangeRateUpdate();
       } catch (Exception e) {
           metricsService.recordExchangeRateUpdateFailure();
           throw e;
       }
   }
   ```

7. **Настроить ServiceMonitor** в Helm chart каждого микросервиса:
   ```yaml
   # templates/servicemonitor.yaml
   apiVersion: monitoring.coreos.com/v1
   kind: ServiceMonitor
   metadata:
     name: {{ include "service.fullname" . }}
   spec:
     selector:
       matchLabels:
         {{- include "service.selectorLabels" . | nindent 6 }}
     endpoints:
       - port: http
         path: /actuator/prometheus
   ```

8. **Запустить тесты** - GREEN

### 3.3 REFACTOR фаза

**Шаги**:
1. Создать аннотацию `@Measured` для автоматического измерения методов
2. Добавить AOP для автоматического измерения критических операций
3. Оптимизировать tags для метрик
4. Микрокоммит

---

## Фаза 4: Интеграция Grafana

### 4.1 Создание дашбордов

**Шаги** (микрокоммит на каждый дашборд):

1. **HTTP Metrics Dashboard**:
   - RPS (requests per second)
   - Error rates (4xx, 5xx)
   - Response time percentiles (p50, p95, p99)
   - Request duration histogram

2. **JVM Metrics Dashboard**:
   - Heap memory usage
   - Non-heap memory usage
   - GC pause time
   - Thread count
   - CPU usage

3. **Business Metrics Dashboard**:
   - Login успешность/неуспешность (rate, total)
   - Transfer failures by reason
   - Blocked operations rate
   - Notification failures
   - Exchange rate update status
   - Графики по времени для всех метрик

4. **Spring Boot Dashboard**:
   - Использовать готовый дашборд (ID: 4701)
   - Настроить переменные для выбора сервиса

### 4.2 Настройка алертов

**Шаги**:
1. Создать Contact Point (email/Slack)
2. Создать Notification Policy
3. Создать Alert Rules:
   - High 5xx rate (> 5% за 5 минут)
   - High latency (p95 > 1s за 5 минут)
   - Failed logins spike (> 10 за минуту)
   - Failed transfers (> 5 за минуту)
   - Exchange rates not updated (> 1 час)
   - Notification failures (> 3 за минуту)

---

## Фаза 5: Интеграция ELK (Logging)

### 5.1 TDD: Написание тестов для логирования

**RED фаза** (микрокоммит):

1. **Создать тесты**:
   ```java
   @SpringBootTest
   class LoggingIntegrationTest {
       @Test
       void shouldLogWithTraceId() {
           // Проверка наличия traceId в логах
       }

       @Test
       void shouldSendLogsToKafka() {
           // Проверка отправки логов в Kafka
       }

       @Test
       void shouldMaskSensitiveData() {
           // Проверка маскировки паролей и счетов
       }
   }
   ```

2. **Запустить тесты** - RED

### 5.2 GREEN фаза: Реализация логирования

**Шаги** (микрокоммит на каждый):

1. **Выбрать логгер**: Logback (уже включен в Spring Boot)

2. **Добавить зависимости** в parent POM:
   ```xml
   <dependency>
       <groupId>com.github.danielwegener</groupId>
       <artifactId>logback-kafka-appender</artifactId>
       <version>0.2.0-RC2</version>
   </dependency>
   ```

3. **Создать общую конфигурацию Logback** (`common-lib/src/main/resources/logback-spring.xml`):
   ```xml
   <configuration>
       <!-- Console appender для локальной разработки -->
       <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
           <encoder>
               <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n</pattern>
           </encoder>
       </appender>

       <!-- Kafka appender для отправки в ELK -->
       <appender name="KAFKA" class="com.github.danielwegener.logback.kafka.KafkaAppender">
           <encoder class="net.logstash.logback.encoder.LogstashEncoder">
               <includeContext>true</includeContext>
               <includeMdc>true</includeMdc>
               <customFields>{"application":"${spring.application.name}"}</customFields>
           </encoder>
           <topic>bank.logs</topic>
           <keyingStrategy class="com.github.danielwegener.logback.kafka.keying.RoundRobinKeyingStrategy" />
           <deliveryStrategy class="com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy" />
           <producerConfig>bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS:bank-app-kafka:9092}</producerConfig>
       </appender>

       <!-- Root logger -->
       <root level="INFO">
           <appender-ref ref="CONSOLE" />
           <appender-ref ref="KAFKA" />
       </root>

       <!-- Специфичные логгеры -->
       <logger name="com.bank" level="DEBUG" />
       <logger name="org.springframework.web" level="INFO" />
       <logger name="org.hibernate" level="WARN" />
   </configuration>
   ```

4. **Добавить логирование в критические места**:

   **auth-server**:
   ```java
   @Slf4j
   @Service
   public class AuthService {
       public void login(String username, String password) {
           log.info("Login attempt for user: {}", username);
           try {
               // логика
               log.info("Login successful for user: {}", username);
           } catch (Exception e) {
               log.error("Login failed for user: {}", username, e);
               throw e;
           }
       }
   }
   ```

   **transfer-service**:
   ```java
   @Slf4j
   @Service
   public class TransferService {
       public void transfer(String from, String to, BigDecimal amount) {
           log.info("Transfer request: from={}, to={}, amount={}",
                    maskAccount(from), maskAccount(to), amount);
           try {
               // логика
               log.info("Transfer completed successfully");
           } catch (Exception e) {
               log.error("Transfer failed", e);
               throw e;
           }
       }

       private String maskAccount(String account) {
           // Маскировка номера счета: 1234****5678
           return account.substring(0, 4) + "****" + account.substring(account.length() - 4);
       }
   }
   ```

   **Аналогично для других сервисов**

5. **Настроить Logstash** (`helm/elk/logstash/templates/configmap.yaml`):
   ```ruby
   input {
     kafka {
       bootstrap_servers => "bank-app-kafka:9092"
       topics => ["bank.logs"]
       codec => json
       group_id => "logstash-consumer-group"
     }
   }

   filter {
     # Парсинг JSON
     json {
       source => "message"
     }

     # Добавление timestamp
     date {
       match => ["timestamp", "ISO8601"]
     }

     # Маскировка паролей (если они попали в логи)
     mutate {
       gsub => [
         "message", "password=\S+", "password=***",
         "message", "token=\S+", "token=***"
       ]
     }
   }

   output {
     elasticsearch {
       hosts => ["bank-app-elasticsearch:9200"]
       index => "bank-logs-%{+YYYY.MM.dd}"
     }
   }
   ```

6. **Настроить Kibana index patterns** (автоматически через init container)

7. **Запустить тесты** - GREEN

### 5.3 REFACTOR фаза

**Шаги**:
1. Создать утилиты для маскировки sensitive data
2. Добавить MDC для дополнительного контекста (user, session)
3. Оптимизировать логирование для production
4. Микрокоммит

---

## Фаза 6: Доработка Jenkinsfile

### 6.1 Создать отдельный Jenkinsfile для мониторинга

**Файл**: `monitoring/Jenkinsfile`

```groovy
pipeline {
    agent any

    environment {
        NAMESPACE = 'bank-app-dev'
    }

    stages {
        stage('Deploy Zipkin') {
            steps {
                sh """
                    helm upgrade --install bank-app-zipkin helm/zipkin \
                        --namespace ${NAMESPACE} \
                        --create-namespace \
                        --wait
                """
            }
        }

        stage('Deploy Prometheus') {
            steps {
                sh """
                    helm upgrade --install bank-app-prometheus helm/prometheus \
                        --namespace ${NAMESPACE} \
                        --wait
                """
            }
        }

        stage('Deploy Grafana') {
            steps {
                sh """
                    helm upgrade --install bank-app-grafana helm/grafana \
                        --namespace ${NAMESPACE} \
                        --wait
                """
            }
        }

        stage('Deploy ELK Stack') {
            parallel {
                stage('Deploy Elasticsearch') {
                    steps {
                        sh """
                            helm upgrade --install bank-app-elasticsearch helm/elk/elasticsearch \
                                --namespace ${NAMESPACE} \
                                --wait
                        """
                    }
                }
                stage('Deploy Logstash') {
                    steps {
                        sh """
                            helm upgrade --install bank-app-logstash helm/elk/logstash \
                                --namespace ${NAMESPACE} \
                                --wait
                        """
                    }
                }
                stage('Deploy Kibana') {
                    steps {
                        sh """
                            helm upgrade --install bank-app-kibana helm/elk/kibana \
                                --namespace ${NAMESPACE} \
                                --wait
                        """
                    }
                }
            }
        }

        stage('Create Kafka Topics') {
            steps {
                sh """
                    kubectl exec -n ${NAMESPACE} bank-app-kafka-0 -- \
                        /opt/kafka/bin/kafka-topics.sh \
                        --create --if-not-exists \
                        --bootstrap-server localhost:9092 \
                        --topic bank.logs \
                        --partitions 5 \
                        --replication-factor 1
                """
            }
        }
    }
}
```

### 6.2 Обновить основной Jenkinsfile

**Добавить стадию** (после Deploy to Dev):

```groovy
stage('Deploy Monitoring Stack') {
    steps {
        echo "Deploying monitoring and logging infrastructure"
        build job: 'monitoring-deployment', wait: true
    }
}
```

---

## Фаза 7: Финальное тестирование и документация

### 7.1 Интеграционные тесты

**Шаги**:

1. **Развернуть всё в Minikube**:
   ```bash
   ./minikube-setup.sh all
   ```

2. **Проверить Zipkin**:
   - Port-forward: `kubectl port-forward -n bank-app-dev svc/bank-app-zipkin 9411:9411`
   - Открыть http://localhost:9411
   - Выполнить несколько операций в приложении
   - Проверить наличие traces

3. **Проверить Prometheus**:
   - Port-forward: `kubectl port-forward -n bank-app-dev svc/bank-app-prometheus 9090:9090`
   - Открыть http://localhost:9090
   - Проверить targets (все микросервисы должны быть UP)
   - Проверить наличие кастомных метрик

4. **Проверить Grafana**:
   - Port-forward: `kubectl port-forward -n bank-app-dev svc/bank-app-grafana 3000:3000`
   - Открыть http://localhost:3000
   - Логин: admin/admin
   - Проверить дашборды
   - Проверить алерты

5. **Проверить Kibana**:
   - Port-forward: `kubectl port-forward -n bank-app-dev svc/bank-app-kibana 5601:5601`
   - Открыть http://localhost:5601
   - Проверить index patterns
   - Выполнить поиск логов
   - Проверить наличие traceId в логах

### 7.2 Обновить документацию

**Обновить README.md** (микрокоммит):

```markdown
# Bank Application - Microservices Architecture

## Мониторинг и логирование

### Zipkin (Distributed Tracing)
- URL: http://bank-app-zipkin:9411
- Просмотр распределённых трейсов
- Анализ производительности

### Prometheus (Metrics)
- URL: http://bank-app-prometheus:9090
- Сбор метрик со всех микросервисов
- Алерты

### Grafana (Visualization)
- URL: http://bank-app-grafana:3000
- Логин: admin/admin
- Дашборды:
  - HTTP Metrics
  - JVM Metrics
  - Business Metrics

### ELK Stack (Logging)
- Elasticsearch: http://bank-app-elasticsearch:9200
- Kibana: http://bank-app-kibana:5601
- Логи со всех микросервисов

## Развёртывание

### Локально (Minikube)
```bash
./minikube-setup.sh all
```

### CI/CD (Jenkins)
1. Запустить pipeline для мониторинга
2. Запустить основной pipeline

## Доступ к сервисам
```bash
# Zipkin
kubectl port-forward -n bank-app-dev svc/bank-app-zipkin 9411:9411

# Prometheus
kubectl port-forward -n bank-app-dev svc/bank-app-prometheus 9090:9090

# Grafana
kubectl port-forward -n bank-app-dev svc/bank-app-grafana 3000:3000

# Kibana
kubectl port-forward -n bank-app-dev svc/bank-app-kibana 5601:5601
```

## Метрики приложения

### Кастомные метрики
- `login_attempts_total{status="success|failure",username="..."}` - попытки логина
- `transfer_failed_total{from="...",to="...",reason="..."}` - неуспешные переводы
- `blocked_operations_total{from="...",to="..."}` - заблокированные операции
- `notification_failed_total{username="..."}` - неудачные уведомления
- `exchange_rate_update{status="success|failure"}` - обновление курсов

### Алерты
- High error rate (5xx > 5%)
- High latency (p95 > 1s)
- Failed login spike (> 10/min)
- Failed transfers (> 5/min)
- Exchange rates not updated (> 1h)
- Notification failures (> 3/min)

## Логирование

Формат логов (JSON):
```json
{
  "timestamp": "2025-11-17T10:00:00.000Z",
  "level": "INFO",
  "application": "accounts-service",
  "traceId": "abc123",
  "spanId": "xyz789",
  "message": "Transfer completed",
  "logger": "com.bank.accounts.service.TransferService"
}
```

## Версии
- v4.0 - Добавлен мониторинг и логирование (Zipkin, Prometheus, Grafana, ELK)
```

### 7.3 Создать финальный коммит с тегом

```bash
git add .
git commit -m "Add monitoring and logging infrastructure

- Zipkin for distributed tracing
- Prometheus for metrics collection
- Grafana dashboards and alerts
- ELK stack for centralized logging
- Custom business metrics
- Comprehensive logging with trace correlation

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"

git tag v4.0
git push origin bankapp-kafka-V2
git push origin v4.0
```

---

## Чек-лист финальной проверки

### Zipkin
- [ ] Helm chart развёрнут
- [ ] Сервис доступен
- [ ] Traces от всех микросервисов приходят
- [ ] Spans для HTTP, DB, Kafka видны
- [ ] TraceId передаётся между сервисами

### Prometheus
- [ ] Helm chart развёрнут
- [ ] Все микросервисы в targets
- [ ] Стандартные метрики работают
- [ ] Кастомные метрики работают
- [ ] Алерты настроены

### Grafana
- [ ] Helm chart развёрнут
- [ ] Datasource Prometheus подключен
- [ ] HTTP Metrics dashboard работает
- [ ] JVM Metrics dashboard работает
- [ ] Business Metrics dashboard работает
- [ ] Алерты настроены

### ELK
- [ ] Elasticsearch развёрнут
- [ ] Logstash развёрнут и читает из Kafka
- [ ] Kibana развёрнут
- [ ] Index patterns созданы
- [ ] Логи видны в Kibana
- [ ] TraceId присутствует в логах
- [ ] Sensitive data замаскирована

### Kafka
- [ ] Топик `bank.logs` создан
- [ ] Логи попадают в топик
- [ ] Logstash читает из топика

### Jenkinsfile
- [ ] Мониторинг deployment работает
- [ ] Интегрирован в основной pipeline
- [ ] Применён в Jenkins

### Тесты
- [ ] Все юнит-тесты проходят
- [ ] Все интеграционные тесты проходят
- [ ] Helm tests проходят

### Документация
- [ ] README.md обновлён
- [ ] Инструкции по развёртыванию
- [ ] Инструкции по доступу к сервисам
- [ ] Описание метрик
- [ ] Описание алертов

### Git
- [ ] Микрокоммиты сделаны
- [ ] История чистая (rebase если нужно)
- [ ] Тег v4.0 проставлен
- [ ] Запушено на GitHub

---

## Примерное распределение времени

| Фаза                 | Время          | Коммиты    |
|----------------------|----------------|------------|
| Фаза 1: Helm charts  | 8-10 часов     | ~15-20     |
| Фаза 2: Zipkin       | 6-8 часов      | ~10-15     |
| Фаза 3: Prometheus   | 8-10 часов     | ~15-20     |
| Фаза 4: Grafana      | 6-8 часов      | ~8-12      |
| Фаза 5: ELK          | 10-12 часов    | ~15-20     |
| Фаза 6: Jenkinsfile  | 4-6 часов      | ~5-8       |
| Фаза 7: Тесты и Docs | 6-8 часов      | ~5-8       |
| **Итого**            | **48-62 часа** | **73-103** |

---

## Важные замечания

1. **TDD подход**: Для инфраструктурных компонентов TDD применяется через Helm tests. Для кода микросервисов - классический TDD. Проверять как позитивные, так и негативные сценарии. Максимально использовать юнит тестирование.

2. **Микрокоммиты**: Каждое логическое изменение - отдельный коммит. Не объединять несвязанные изменения.

3. **Версионирование**: Используйте семантическое версионирование для Helm charts.

4. **Безопасность**: Не коммитить credentials в Git. Использовать Kubernetes Secrets.

5. **Production ready**: Для production нужно:
   - Персистентное хранилище для Prometheus, Elasticsearch
   - Реплики для высокой доступности
   - Retention policies
   - Backup стратегия
   - Security (authentication, TLS)

6. **Monitoring**: Мониторить сами системы мониторинга (meta-monitoring).
7. После завершения каждой фазы проводить код-ревью и тестирование. Убедиться, что все тесты проходят и все требования выполнены. 
   Сделай рефакторинг кода при необходимости и снова проверь тесты. Используй моки или стабы в юнит тестах. 
8. Не добавлять ненужные комментарии в код. Код должен быть чистым и понятным. Давай имена функциям и переменным, которые отражают их назначение. Комментарии можно использовать только в javadoc для описания публичных API.
9. Избегать дублирования кода. Вынеси общую логику в утилиты или базовые классы. Использовать полиморфизм и композицию вместо дублирования. Использовать шаблоны проектирования, если это уместно. Использовать AOP для кросс-срезной функциональности (логирование, метрики, транзакции). Использовать или писать свои аннотации для декларативного программирования, где это возможно и логически целесообразно.
10. Мзбегать использования System.out.println или System.err.println для логирования. Всегда использовать логгер (SLF4J с Logback). Настроить уровни логирования (DEBUG, INFO, WARN, ERROR) и использовать их соответственно. Логировать структурированные логи в формате JSON для удобства парсинга и анализа.
--- 

Этот план следует итеративному подходу и обеспечивает полное покрытие всех требований задания. Каждая фаза независима и может быть выполнена отдельно с тестированием.