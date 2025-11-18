{{/*
Kafka Bootstrap Servers
*/}}
{{- define "bank-app.kafka.bootstrapServers" -}}
{{- if .Values.kafka.enabled -}}
kafka:9092
{{- else -}}
{{- .Values.global.kafka.bootstrapServers | default "localhost:9092" -}}
{{- end -}}
{{- end -}}
