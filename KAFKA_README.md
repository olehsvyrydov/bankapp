# Kafka Configuration for Bank App

## Current Status: Disabled for Minikube

Kafka is currently **disabled** in the minikube deployment (`helm/bank-app/values.yaml`) due to image availability issues.

## Issue Description

The Bitnami Kafka Helm chart (v32.4.3) specifies a Docker image tag that is not available:
- **Specified image**: `docker.io/bitnami/kafka:4.0.0-debian-12-r10`
- **Status**: Image not found in Docker registries

### Why We Can't Use Apache Kafka Images

The Bitnami Kafka chart is tightly coupled to Bitnami-specific images and requires:
- Bitnami directory structure (`/opt/bitnami/`)
- Bitnami initialization scripts (`/opt/bitnami/scripts/libkafka.sh`)
- Bitnami configuration management tools

Apache Kafka official images have a completely different structure and cause init container failures when used with the Bitnami chart.

## Solutions

### Option 1: Use Cloud-Managed Kafka (Recommended for Production)
For production deployments, use a managed Kafka service:
- **AWS**: Amazon MSK
- **Azure**: Azure Event Hubs for Kafka
- **GCP**: Confluent Cloud on GCP
- **Confluent Cloud**: Fully managed Kafka

Configure connection in `helm/bank-app/values.yaml`:
```yaml
global:
  kafka:
    bootstrapServers: "your-kafka-cluster:9092"

kafka:
  enabled: false  # Disable embedded Kafka
```

### Option 2: Update to Working Bitnami Image (For Minikube/Development)

If you find a working Bitnami Kafka image tag, update `helm/bank-app/values.yaml`:

```yaml
kafka:
  enabled: true
  image:
    registry: docker.io
    repository: bitnami/kafka
    tag: "3.x.x-debian-12-rXX"  # Replace with working tag
    pullPolicy: IfNotPresent
```

And update `minikube-setup.sh` to pull and load the image:
```bash
# In load_images() function
KAFKA_IMAGE="docker.io/bitnami/kafka:3.x.x-debian-12-rXX"
docker pull "${KAFKA_IMAGE}"
minikube image load "${KAFKA_IMAGE}"
```

### Option 3: Use Different Kafka Chart

Switch to the Strimzi Kafka operator or Confluent Platform charts which have better image availability.

## Impact of Disabled Kafka

With Kafka disabled, the following features are affected:

### Affected Services:
- **notifications-service**: Cannot send Kafka-based notifications
  - Fallback: Service will log errors but continue running
  - HTTP endpoints remain functional

### Working Services (Not Affected):
- ✅ All other banking services (accounts, transfer, cash, exchange, etc.)
- ✅ Authentication and authorization
- ✅ Database operations
- ✅ Gateway and routing
- ✅ Front-end UI

## Testing

Helm tests include Kafka validation which will be skipped when Kafka is disabled:
- `test-kafka.yaml` - Conditional based on `.Values.kafka.enabled`
- Other tests verify Kafka services only when enabled

Run tests:
```bash
./minikube-setup.sh test
```

## Enabling Kafka

To re-enable Kafka when a working image is available:

1. **Update values.yaml**:
   ```yaml
   kafka:
     enabled: true
   ```

2. **Update minikube-setup.sh**: Uncomment Kafka image loading section

3. **Update Chart.yaml** if needed: Consider downgrading to an older Kafka chart version with available images

4. **Redeploy**:
   ```bash
   ./minikube-setup.sh redeploy
   ```

## For More Information

- Bitnami Kafka Chart: https://github.com/bitnami/charts/tree/main/bitnami/kafka
- Known Issues: https://github.com/bitnami/charts/issues/30850
- Kafka Documentation: https://kafka.apache.org/documentation/
