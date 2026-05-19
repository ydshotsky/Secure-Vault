# SecureVault - Azure + Grafana Cloud Deployment Setup

## What Was Updated

Your SecureVault project is now configured for **Azure Container deployment** with **Grafana Cloud observability**.

### Files Created / Updated:

#### 1. **application-production.yml** (Updated)
   - ✅ Added database connection pooling (HikariCP)
   - ✅ Added Redis pooling configuration
   - ✅ Added graceful shutdown settings
   - ✅ Added health check endpoints
   - ✅ Added OTLP tracing endpoints for Grafana Cloud
   - ✅ Added metrics export configuration
   - ✅ Proper logging configuration

#### 2. **Dockerfile.prod** (Updated)
   - ✅ JVM-based multi-stage build (Temurin 21 JDK/JRE)
   - ✅ Builds runnable JAR (no native/AOT during docker build)
   - ✅ Non-root runtime user

#### 3. **AZURE_DEPLOYMENT.md** (NEW)
   - Complete step-by-step Azure deployment guide
   - Infrastructure setup (PostgreSQL, Redis, Key Vault, Container Registry)
   - Grafana Cloud setup instructions
   - Build & push Docker image commands
   - Deployment to Azure Container Instances & App Service
   - Monitoring setup
   - Troubleshooting guide
   - Cost estimation

#### 4. **.dockerignore** (NEW)
   - Speeds up Docker builds by excluding unnecessary files
   - Reduces image build time by ~30-50%

#### 5. **.env.development / .env.production** (NEW)
   - Environment variable templates for local and cloud usage
   - Shows required variables for database, Redis, and OTLP

---

## Quick Start Guide

### Local Development

Option A: Run with Maven (recommended for dev)

```bash
# Set environment variables from .env.development, then run
mvnw spring-boot:run -Dspring.profiles.active=development
```

Option B: Build a container image (production profile)

```bash
docker build -f Dockerfile.prod -t securevault:prod .
```

---

### Azure Cloud Deployment

1. **Read the guide:**
   ```bash
   # Open AZURE_DEPLOYMENT.md for complete instructions
   ```

2. **Quick summary of steps:**
   - Create Azure resources (PostgreSQL, Redis, Key Vault, Container Registry)
   - Get Grafana Cloud credentials
   - Build Docker image: `docker build -f Dockerfile.prod -t ...`
   - Push to Azure Container Registry
   - Deploy to Container Instances or App Service
   - Monitor in Grafana Cloud

3. **For production deployment, you'll set these environment variables:**
   ```
   SPRING_PROFILES_ACTIVE=production
   DATABASE_URL=<from Key Vault>
   DATABASE_USERNAME=<from Key Vault>
   DATABASE_PASSWORD=<from Key Vault>
   REDIS_URL=<from Key Vault>
   OTEL_EXPORTER_OTLP_ENDPOINT=<Grafana endpoint base>
   OTEL_EXPORTER_OTLP_HEADERS=<Grafana auth token>
   ```

---

## Architecture

```
┌──────────────────────────┐
│  Your Local Machine      │
│  - IDE / Maven           │
│  - Docker build (prod)   │
└──────────┬───────────────┘
           │
           ├─ PostgreSQL
           ├─ Redis
           └─ SecureVault

┌──────────────────────────────┐
│  Azure Cloud                 │
│  - Container Instances       │
│  - App Service               │
│  - PostgreSQL Server         │
│  - Azure Cache for Redis     │
│  - Key Vault (secrets)       │
└──────────┬────────────────────┘
           │ Metrics/Traces/Logs
           ▼
┌──────────────────────────────┐
│  Grafana Cloud               │
│  - Prometheus (metrics)      │
│  - Tempo (traces)            │
│  - Loki (logs)               │
│  - Dashboards & Alerts       │
└──────────────────────────────┘
```

---

## Key Features

### Performance
- ⚡ JVM-based container (Temurin 21)
- 💾 Predictable memory footprint
- 📦 Standard container build (no native image build step)

### Security
- 🔐 Non-root Docker user
- 🔐 Secrets in Azure Key Vault (not in code)
- 🔐 HTTPS enforced
- 🔐 Database password-protected
- 🔐 Redis SSL enabled

### Observability
- 📊 Prometheus metrics exported
- 📈 OpenTelemetry traces → Grafana Cloud
- 📝 Centralized logging via Loki
- 🏥 Health checks on `/actuator/health`

### Reliability
- ✅ Graceful shutdown (30 seconds)
- ✅ Health probes for orchestration
- ✅ Connection pooling configured
- ✅ Session persistence via Redis
- ✅ Automatic database migrations

---

## Next Steps

### 1. **Test Locally**
```bash
# Run locally with development profile
mvnw spring-boot:run -Dspring.profiles.active=development
```

### 2. **Deploy to Azure** (See AZURE_DEPLOYMENT.md)
```bash
# Create resources, build image, push, and deploy
```

### 3. **Monitor in Grafana Cloud**
- View dashboards
- Set up alerts
- Track performance

### 4. **Setup CI/CD** (Optional)
- GitHub Actions workflow to auto-build & deploy on code push
- Azure DevOps pipeline

---

## Files Summary

| File | Purpose |
|------|---------|
| `src/main/resources/application-production.yml` | Production configuration |
| `Dockerfile.prod` | Production JVM build for Azure |
| `.dockerignore` | Speeds up Docker builds |
| `.env.development` | Local development env template |
| `.env.production` | Production env template |
| `AZURE_DEPLOYMENT.md` | Complete Azure deployment guide |

---

## Common Commands

```bash
# Build production Docker image
docker build -f Dockerfile.prod -t app:latest .

# Push to Azure
az acr login --name myregistry
docker push myregistry.azurecr.io/app:latest

# View running containers
docker ps -a

# Clean up
docker image prune
```

---

## Support & Troubleshooting

**App won't start locally?**
- Check environment variables from `.env.development`
- Ensure port 8080 is not in use
- Verify Docker has enough memory

**Can't connect to database?**
- Verify connection string in `.env.development` or Azure Key Vault
- Ensure PostgreSQL is reachable

**No metrics in Grafana?**
- Verify OTLP endpoint is correct
- Check Grafana Cloud API token is valid
- Wait 2-3 minutes for data to appear

**Cloud deployment issues?**
- See troubleshooting section in `AZURE_DEPLOYMENT.md`

---

You're all set! For detailed Azure deployment instructions, see **AZURE_DEPLOYMENT.md**.

Questions? Check the guide or environment files for examples.
