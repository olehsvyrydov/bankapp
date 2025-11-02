{{- define "front-ui.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "front-ui.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "front-ui.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" -}}
{{- end -}}

{{- define "front-ui.labels" -}}
app.kubernetes.io/name: {{ include "front-ui.name" . }}
helm.sh/chart: {{ include "front-ui.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.podLabels }}
{{ toYaml . }}
{{- end }}
{{- end -}}

{{- define "front-ui.selectorLabels" -}}
app.kubernetes.io/name: {{ include "front-ui.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "front-ui.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "front-ui.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{- define "front-ui.image" -}}
{{- $global := .Values.global | default (dict) -}}
{{- $registry := (or .Values.image.registry $global.imageRegistry) | default "" | trimSuffix "/" -}}
{{- $repository := .Values.image.repository -}}
{{- $tag := .Values.image.tag | default .Chart.AppVersion -}}
{{- if $registry -}}
{{- printf "%s/%s:%s" $registry $repository $tag -}}
{{- else -}}
{{- printf "%s:%s" $repository $tag -}}
{{- end -}}
{{- end -}}

{{- define "front-ui.databaseImage" -}}
{{- $global := .Values.global | default (dict) -}}
{{- $image := .Values.database.image -}}
{{- if not $image -}}
postgres:16
{{- else -}}
{{- $registry := (or $image.registry $global.imageRegistry) | default "" | trimSuffix "/" -}}
{{- $repository := $image.repository | default "postgres" -}}
{{- $tag := $image.tag | default "16" -}}
{{- if $registry -}}
{{- printf "%s/%s:%s" $registry $repository $tag -}}
{{- else -}}
{{- printf "%s:%s" $repository $tag -}}
{{- end -}}
{{- end -}}
{{- end -}}
