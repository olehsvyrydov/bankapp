# Kafka Helm Chart

Apache Kafka deployment for Bank App microservices messaging.

## Overview

This chart deploys Apache Kafka cluster with automatic topic provisioning for:
- Notification messages (at least once delivery)
- Exchange rate updates (at most once delivery)
- Retry topics with exponential backoff
- Dead Letter Topics (DLT) for failed messages

## Configuration

### Environments

- **dev**: Single node, minimal resources, replication factor 1
- **test**: 2 nodes, moderate resources, replication factor 2
- **prod**: 3 nodes, high resources, replication factor 3

### Topics

| Topic | Partitions (prod) | Retention | Purpose |
|-------|-------------------|-----------|---------|
| bank.notifications | 6 | 7 days | User notifications |
| bank.notifications-retry | 6 | 1 day | Failed notification retries |
| bank.notifications-dlt | 6 | 30 days | Failed notifications after all retries |
| bank.exchange.rates | 4 | 1 hour | Exchange rate updates |
| bank.exchange.rates-retry | 4 | Default | Failed rate update retries |
| bank.exchange.rates-dlt | 4 | Default | Failed rates after all retries |

## Installation

```bash
# Development
helm install kafka . -f values-dev.yaml -n dev

# Production
helm install kafka . -f values-prod.yaml -n prod
```

## Persistence

All environments use persistent volumes to ensure data survives pod restarts:
- Dev: 4Gi per broker
- Test: 8Gi per broker
- Prod: 16Gi per broker with fast-ssd storage class

## Monitoring

Production environment includes:
- JMX metrics exporter
- Kafka exporter
- ServiceMonitor for Prometheus integration
