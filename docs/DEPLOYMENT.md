# Bank Application — Kubernetes Deployment Guide

This file contains only the steps required to install the application in Kubernetes-based environments.

## 1. Requirements
- Kubernetes cluster (Minikube, kind, or managed) with at least 4 vCPU / 8 GB RAM
- kubectl 1.27+, Helm 3.12+
- Docker registry that the cluster can pull from (or Minikube Docker daemon)
- Java 21 + Maven 3.9+ if you need to build artifacts locally

## 2. Build artifacts (optional)
If you build images yourself instead of using published ones:
```bash
mvn -DskipTests package
for svc in auth-server gateway-service accounts-service cash-service transfer-service \
           exchange-service exchange-generator-service blocker-service notifications-service front-ui; do
  docker build -t $svc:latest ./$svc
done
```
Push the images to your registry or load them into Minikube with `minikube image load <image>`.

## 3. Deploy with `./minikube-setup.sh`
1. Run `./minikube-setup.sh all`.
2. After the script finishes, add `$(minikube ip) bank-app-dev.local` to `/etc/hosts`.
3. (Optional) Run `minikube tunnel` in a separate terminal to expose the ingress IP to the host OS.
4. Open `http://bank-app-dev.local/` and log in with the default users (`admin/password`, `tester/password`).
5. Useful commands:
   - `./minikube-setup.sh deploy` – re-run Helm upgrade without rebuilding images.
   - `./minikube-setup.sh redeploy` – uninstall + fresh install of the release.
   - `./minikube-setup.sh status` – show Minikube status, pods, services, and Helm releases.
   - `./minikube-setup.sh clean` – uninstall the release and delete the namespace.

## 4. Manual Helm deployment (any cluster)
1. Create the namespace (idempotent):
   ```bash
   kubectl create namespace bank-app-dev --dry-run=client -o yaml | kubectl apply -f -
   ```
2. Install or upgrade the umbrella chart:
   ```bash
   helm upgrade --install bank-app ./helm/bank-app \
     -f ./helm/bank-app/values-dev.yaml \
     --namespace bank-app-dev \
     --wait --timeout 10m
   ```
3. To override the image tag: add `--set global.image.tag=<tag>`.
4. Keep `global.loadBalancer.mode=SERVICE` (default) so Spring Cloud Kubernetes resolves services through ClusterIP DNS and the first requests do not time out.
5. To delete the release:
   ```bash
   helm uninstall bank-app -n bank-app-dev
   kubectl delete namespace bank-app-dev
   ```

## 5. Configure ingress access
1. Determine the ingress IP (for Minikube use `minikube ip`; for managed clusters use the ingress controller LoadBalancer address).
2. Add a hosts entry, e.g. `192.168.49.2 bank-app-dev.local`.
3. Open `http://bank-app-dev.local/` in a browser; `/login` and `/logout` stay on `front-ui`, `/api/**` and `/oauth2/**` are routed to the gateway and auth server automatically.
4. If you change `.Values.ingress.host`, update `/etc/hosts` accordingly.

## 6. Verify and troubleshoot
```bash
kubectl get pods -n bank-app-dev
kubectl describe pod <pod> -n bank-app-dev
kubectl logs -n bank-app-dev deployment/bank-app-cash-service
kubectl get ingress,svc -n bank-app-dev
```
- Wait until every pod is `Ready` before issuing the first cash deposit to avoid Resilience4j timeouts.
- The ingress also exposes `/actuator/health` for quick checks.

## 7. Updating or rolling back
- `helm upgrade bank-app ./helm/bank-app -f helm/bank-app/values-dev.yaml -n bank-app-dev --wait`
- `helm rollback bank-app <revision> -n bank-app-dev`
- Rebuild and reload images before the upgrade if you changed code.

## 8. Cleanup
```bash
helm uninstall bank-app -n bank-app-dev
kubectl delete namespace bank-app-dev
minikube delete   # optional, removes the whole cluster
```
