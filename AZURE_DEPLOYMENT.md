# SecureVault - Azure Container Deployment Guide

This guide covers deploying SecureVault as a containerized app on Azure with Grafana Cloud for observability.

## Prerequisites

- Azure subscription with Container Registry, App Service, and PostgreSQL
- Grafana Cloud account (free tier available)
- Docker CLI installed locally
- Azure CLI installed locally

---

## Architecture

```
┌─────────────────────────────────────────┐
│   Azure Container Instances / App Service │
│   (SecureVault Native Image)            │
├─────────────────────────────────────────┤
│  ↓ Metrics (Prometheus)                 │
│  ↓ Traces (OpenTelemetry/OTLP)          │
│  ↓ Logs (Loki)                          │
└─────────────────────────────────────────┘
        │
        ↓
┌─────────────────────────────────────────┐
│   Grafana Cloud (Observability Stack)   │
│   - Prometheus (metrics)                │
│   - Tempo (traces)                      │
│   - Loki (logs)                         │
└─────────────────────────────────────────┘
```

---

## Step 1: Set Up Azure Infrastructure

### 1.1 Create Resource Group

```bash
az group create --name rg-securevault --location eastus
```

### 1.2 Create Azure Database for PostgreSQL (Flexible Server)

```bash
az postgres flexible-server create \
  --resource-group rg-securevault \
  --name securevault-db \
  --location eastus \
  --admin-user dbadmin \
  --admin-password '<STRONG_PASSWORD>' \
  --sku-name Standard_B1ms \
  --tier Burstable
```

**Get connection string:**
```bash
az postgres flexible-server connection-string show \
  --resource-group rg-securevault \
  --name securevault-db \
  --client psycopg
```

Output format:
```
postgresql://dbadmin:<password>@securevault-db.postgres.database.azure.com:5432/postgres
```

### 1.3 Create Azure Cache for Redis

```bash
az redis create \
  --resource-group rg-securevault \
  --name securevault-redis \
  --location eastus \
  --sku Basic \
  --vm-size c0
```

**Get connection info:**
```bash
az redis show \
  --resource-group rg-securevault \
  --name securevault-redis \
  --query "hostName,sslPort" -o table
```

Also enable SSL and get access key:
```bash
az redis access-key list \
  --resource-group rg-securevault \
  --name securevault-redis
```

Connection string format:
```
redis://:PASSWORD@HOST:6380?ssl=true
```

### 1.4 Create Key Vault (for secrets)

```bash
az keyvault create \
  --resource-group rg-securevault \
  --name securevault-kv \
  --location eastus
```

### 1.5 Store Secrets in Key Vault

```bash
# Database credentials
az keyvault secret set \
  --vault-name securevault-kv \
  --name DATABASE-URL \
  --value "postgresql://dbadmin:PASSWORD@securevault-db.postgres.database.azure.com:5432/securevault"

az keyvault secret set \
  --vault-name securevault-kv \
  --name DATABASE-USERNAME \
  --value "dbadmin"

az keyvault secret set \
  --vault-name securevault-kv \
  --name DATABASE-PASSWORD \
  --value "<PASSWORD>"

# Redis
az keyvault secret set \
  --vault-name securevault-kv \
  --name REDIS-URL \
  --value "redis://:PASSWORD@host:6380?ssl=true"

# Grafana Cloud (see Step 2)
az keyvault secret set \
  --vault-name securevault-kv \
  --name OTEL-ENDPOINT-TRACES \
  --value "https://otlp-gateway-prod-xxxxx.grafana.net/otlp/v1/traces"

az keyvault secret set \
  --vault-name securevault-kv \
  --name OTEL-AUTH-TOKEN \
  --value "<GRAFANA_CLOUD_TOKEN>"
```

### 1.6 Create Azure Container Registry

```bash
az acr create \
  --resource-group rg-securevault \
  --name securevaultregistry \
  --sku Basic
```

---

## Step 2: Set Up Grafana Cloud

### 2.1 Create Grafana Cloud Account

Go to https://grafana.com/products/cloud/ and create a free account.

### 2.2 Get OTLP Endpoint & Token

In Grafana Cloud:
1. Go to **Connections** → **Your Grafana Stack**
2. Find **OTLP** section
3. Copy the endpoint and create an API token

Or use the API:
```bash
# Get your Grafana instance URL and API key from Grafana Cloud console
GRAFANA_URL="https://YOUR_INSTANCE.grafana.net"
GRAFANA_API_KEY="YOUR_API_KEY"

# Create API token for OTLP
curl -X POST "$GRAFANA_URL/api/v1/tokens" \
  -H "Authorization: Bearer $GRAFANA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "securevault-otlp", "scopes": ["metrics:write", "traces:write", "logs:write"]}'
```

### 2.3 Store Grafana Credentials in Key Vault

```bash
az keyvault secret set \
  --vault-name securevault-kv \
  --name OTEL-EXPORTER-OTLP-ENDPOINT-TRACES \
  --value "https://otlp-gateway-prod-XXXXX.grafana.net/otlp/v1/traces"

az keyvault secret set \
  --vault-name securevault-kv \
  --name OTEL-EXPORTER-OTLP-HEADERS \
  --value "Authorization=Bearer YOUR_TOKEN"
```

---

## Step 3: Build & Push Docker Image

### 3.1 Login to Azure Container Registry

```bash
az acr login --name securevaultregistry
```

### 3.2 Build Native Docker Image

```bash
cd E:\hk\gith\SecureVault

docker build -f Dockerfile.prod -t securevaultregistry.azurecr.io/securevault:latest .
```

### 3.3 Push to Azure Container Registry

```bash
docker push securevaultregistry.azurecr.io/securevault:latest
```

---

## Step 4: Deploy to Azure Container Instances

### 4.1 Create Deployment Environment Variables File

Create `env-prod.txt`:
```
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=<DATABASE_URL_FROM_KEY_VAULT>
SPRING_DATASOURCE_USERNAME=dbadmin
SPRING_DATASOURCE_PASSWORD=<PASSWORD_FROM_KEY_VAULT>
SPRING_DATA_REDIS_URL=<REDIS_URL_FROM_KEY_VAULT>
OTEL_EXPORTER_OTLP_ENDPOINT_TRACES=<GRAFANA_ENDPOINT_FROM_KEY_VAULT>
OTEL_EXPORTER_OTLP_HEADERS=<GRAFANA_AUTH_TOKEN_FROM_KEY_VAULT>
PORT=8080
```

### 4.2 Deploy Container Instance

```bash
az container create \
  --resource-group rg-securevault \
  --name securevault-container \
  --image securevaultregistry.azurecr.io/securevault:latest \
  --registry-login-server securevaultregistry.azurecr.io \
  --registry-username <REGISTRY_USERNAME> \
  --registry-password <REGISTRY_PASSWORD> \
  --environment-variables SPRING_PROFILES_ACTIVE=production \
  --secure-environment-variables \
    SPRING_DATASOURCE_URL="$(az keyvault secret show --vault-name securevault-kv --name DATABASE-URL -o tsv --query value)" \
    SPRING_DATASOURCE_USERNAME="$(az keyvault secret show --vault-name securevault-kv --name DATABASE-USERNAME -o tsv --query value)" \
    SPRING_DATASOURCE_PASSWORD="$(az keyvault secret show --vault-name securevault-kv --name DATABASE-PASSWORD -o tsv --query value)" \
    SPRING_DATA_REDIS_URL="$(az keyvault secret show --vault-name securevault-kv --name REDIS-URL -o tsv --query value)" \
  --cpu 2 \
  --memory 2 \
  --ports 8080 \
  --protocol TCP
```

### 4.3 Verify Deployment

```bash
az container show \
  --resource-group rg-securevault \
  --name securevault-container \
  --query "{Status: containers[0].instanceView.currentState.state, FQDN: ipAddress.fqdn}"
```

Access your app:
```
http://<CONTAINER_FQDN>:8080
```

---

## Step 5: (Optional) Deploy to Azure App Service

For more control and auto-scaling, use App Service:

### 5.1 Create App Service Plan

```bash
az appservice plan create \
  --name ASP-securevault \
  --resource-group rg-securevault \
  --sku B2 \
  --is-linux
```

### 5.2 Create Web App

```bash
az webapp create \
  --resource-group rg-securevault \
  --plan ASP-securevault \
  --name securevault-app \
  --deployment-container-image-name securevaultregistry.azurecr.io/securevault:latest
```

### 5.3 Configure App Settings

```bash
az webapp config appsettings set \
  --resource-group rg-securevault \
  --name securevault-app \
  --settings \
    SPRING_PROFILES_ACTIVE=production \
    SPRING_DATASOURCE_URL=$(az keyvault secret show --vault-name securevault-kv --name DATABASE-URL -o tsv --query value) \
    SPRING_DATASOURCE_USERNAME=dbadmin \
    SPRING_DATASOURCE_PASSWORD=$(az keyvault secret show --vault-name securevault-kv --name DATABASE-PASSWORD -o tsv --query value) \
    SPRING_DATA_REDIS_URL=$(az keyvault secret show --vault-name securevault-kv --name REDIS-URL -o tsv --query value) \
    PORT=8080
```

### 5.4 Configure Health Probe

```bash
az webapp config appsettings set \
  --resource-group rg-securevault \
  --name securevault-app \
  --settings WEBSITES_HEALTHCHECK_PATH=/actuator/health
```

---

## Step 6: Monitor with Grafana Cloud

### 6.1 Verify Metrics in Grafana

1. Log into your Grafana Cloud instance
2. Go to **Dashboards** → **New** → **Import**
3. Search for **Spring Boot** dashboard templates
4. Import and customize

### 6.2 Create Alerts (Optional)

In Grafana:
- Set up alert rules for high error rates, CPU usage, slow queries
- Examples:
  ```
  http_requests_total{status=~"5.."}  > 10
  jvm_memory_usage_percent > 90
  ```

### 6.3 View Logs (Loki)

The logback-spring.xml sends logs via `loki4j` appender:
- Logs appear in Grafana **Explore** → **Loki**
- Filter by labels: `app=password-manager`

---

## Environment Variables Reference

| Variable | Value | Source |
|----------|-------|--------|
| `SPRING_PROFILES_ACTIVE` | `production` | Hardcoded |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...` | Key Vault |
| `SPRING_DATASOURCE_USERNAME` | `dbadmin` | Key Vault |
| `SPRING_DATASOURCE_PASSWORD` | `*****` | Key Vault |
| `SPRING_DATA_REDIS_URL` | `redis://:****@....rediscache.windows.net:6380?ssl=true` | Key Vault |
| `OTEL_EXPORTER_OTLP_ENDPOINT_TRACES` | `https://otlp-gateway-prod-xxxxx.grafana.net/otlp/v1/traces` | Key Vault |
| `OTEL_EXPORTER_OTLP_HEADERS` | `Authorization=Bearer xxxx` | Key Vault |
| `PORT` | `8080` | Hardcoded |

---

## Troubleshooting

### App won't start
- Check logs: `az container logs --resource-group rg-securevault --name securevault-container`
- Verify environment variables are set correctly
- Check database connectivity: `az postgres flexible-server connect --admin-user dbadmin --name securevault-db`

### Health checks failing
- Verify app is responding: `curl http://CONTAINER_FQDN:8080/actuator/health`
- Check logs for startup errors

### No metrics in Grafana
- Verify `OTEL_EXPORTER_OTLP_ENDPOINT_TRACES` and auth token are correct
- Check if Grafana Cloud stack is active
- Wait a few minutes for metrics to populate

### Database connection issues
- Verify PostgreSQL firewall allows Azure Container Instances
- Test connection string locally before deploying

---

## Cost Estimation (Azure + Grafana Cloud)

| Resource | Tier | Monthly Cost |
|----------|------|--------------|
| Container Instances (2 vCPU, 2GB RAM) | Pay-as-you-go | ~$10-30 |
| PostgreSQL Flexible Server (B1ms) | Burstable | ~$20-50 |
| Azure Cache for Redis (Basic, 250MB) | Basic | ~$20 |
| Grafana Cloud Stack | Free tier | Free (limited) |
| Grafana Cloud Stack | Pro tier | ~$80/month |
| **Total (Free Tier)** | | ~$50-100/month |
| **Total (Pro Tier)** | | ~$130-160/month |

---

## Next Steps

1. Run the deployment commands above
2. Verify app is running: `http://CONTAINER_FQDN:8080`
3. Monitor in Grafana Cloud dashboard
4. Set up CI/CD to auto-deploy on code changes (Azure DevOps / GitHub Actions)
5. Configure auto-scaling and backup policies

For CI/CD setup, see `.github/workflows/` or create an Azure Pipeline YAML file.

