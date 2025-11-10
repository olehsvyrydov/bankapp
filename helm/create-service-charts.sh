#!/bin/bash

# Script to generate Helm subcharts for all microservices

# Define services and their ports
declare -A services
services[accounts-service]=8081
services[transfer-service]=8083
services[cash-service]=8082
services[exchange-service]=8084
services[exchange-generator-service]=8085
services[notifications-service]=8087
services[blocker-service]=8086
services[gateway-service]=8100
services[front-ui]=8090

# Base directory
BASE_DIR="helm/bank-app/charts"

# Function to create service subchart
create_service_chart() {
    local service=$1
    local port=$2
    local chart_dir="$BASE_DIR/$service"

    echo "Creating subchart for $service..."

    # Create Chart.yaml
    cat > "$chart_dir/Chart.yaml" << EOF
apiVersion: v2
name: $service
description: $service for Bank application
type: application
version: 1.0.0
appVersion: "2.0.0"
keywords:
  - bank
  - microservice
  - spring-boot
maintainers:
  - name: Bank App Team
EOF

    # Create values.yaml
    cat > "$chart_dir/values.yaml" << EOF
## $service subchart default values

image:
  repository: $service
  tag: "latest"
  pullPolicy: IfNotPresent

replicaCount: 1

service:
  type: ClusterIP
  port: $port

resources:
  limits:
    memory: 512Mi
    cpu: 500m
  requests:
    memory: 256Mi
    cpu: 250m

# Database configuration (from global/parent)
postgresql:
  host: postgresql
  port: 5432
  database: bankdb
  username: bank_user
  # password from secret

# Spring profiles
springProfiles: kubernetes

# Spring Cloud Kubernetes
springCloudKubernetes:
  enabled: true
  discovery:
    enabled: true

# OAuth2 client configuration
oauth2:
  clientId: $service
  # clientSecret from secret
  providerTokenUri: http://auth-server:9100/oauth2/token
  resourceServerIssuerUri: http://auth-server:9100

# Liveness and readiness probe configuration
livenessProbe:
  enabled: true
  initialDelaySeconds: 90
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 6

readinessProbe:
  enabled: true
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3
EOF

    # Create _helpers.tpl
    local service_camel=$(echo $service | sed 's/-\([a-z]\)/\U\1/g')
    cat > "$chart_dir/templates/_helpers.tpl" << EOF
{{/*
Expand the name of the chart.
*/}}
{{- define "$service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "$service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- \$name := default .Chart.Name .Values.nameOverride }}
{{- if contains \$name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name \$name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "$service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "$service.labels" -}}
helm.sh/chart: {{ include "$service.chart" . }}
{{ include "$service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "$service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "$service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
EOF

    echo "Subchart $service created successfully!"
}

# Create subcharts for all services
for service in "${!services[@]}"; do
    create_service_chart "$service" "${services[$service]}"
done

echo "All service subcharts created successfully!"
