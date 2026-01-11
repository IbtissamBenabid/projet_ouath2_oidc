# Architecture Micro-services Sécurisée (Spring Boot + React + Keycloak)

Ce projet est une application de gestion de produits et commandes basée sur une architecture micro-services.

## Architecture

- **Frontend**: React.js
- **Gateway**: Spring Cloud Gateway (Port 8085)
- **Services**:
  - `product-service` (Port 8081): Gestion du catalogue.
  - `order-service` (Port 8082): Gestion des commandes.
- **Sécurité**: Keycloak (OAuth2 / OpenID Connect).
- **Base de données**: PostgreSQL (une base par service).

## Prérequis

- Java 21
- Node.js & npm
- Docker Desktop
- Trivy (for security scanning)

## Démarrage Rapide

### 1. Infrastructure (Docker)
Lancer les conteneurs PostgreSQL et Keycloak :
```bash
docker-compose up -d
```

### 2. Configuration Keycloak
Accédez à `http://localhost:8088` (admin/admin).
Le realm `microservices-realm` est automatiquement importé avec :
- Client `react-client` (Public)
- Utilisateurs: `admin/admin` (ADMIN) et `user1/user1` (CLIENT)
- Rôles: `ADMIN` et `CLIENT`

### 3. Backend
Lancer les services dans l'ordre :
```bash
cd product-service && ./mvnw spring-boot:run
cd order-service && ./mvnw spring-boot:run
cd gateway && ./mvnw spring-boot:run
```

### 4. Frontend
```bash
cd react-app
npm install
npm start
```
L'application sera accessible sur `http://localhost:3000`.

## 🏥 Health Dashboard

Le projet inclut un dashboard de monitoring pour surveiller l'état des services.

### Accès au Dashboard
- **URL**: http://localhost:8085/static/health-dashboard.html
- **API Endpoints**:
  - Gateway Health: http://localhost:8085/actuator/health
  - Product Service: http://localhost:8081/actuator/health
  - Order Service: http://localhost:8082/actuator/health
  - Dashboard API: http://localhost:8085/dashboard/health

### Fonctionnalités
- Vérification en temps réel de l'état des services
- Rafraîchissement automatique toutes les 30 secondes
- Indicateurs visuels (UP/DOWN/DEGRADED)

## 🔒 Sécurité

### Authentification & Autorisation
- L'accès aux APIs passe obligatoirement par la Gateway
- Le token JWT est validé par chaque service
- Les rôles `ADMIN` et `CLIENT` contrôlent l'accès aux endpoints

### Endpoints par Rôle
| Endpoint | ADMIN | CLIENT |
|----------|-------|--------|
| GET /products | ✅ | ✅ |
| POST /products | ✅ | ❌ |
| PUT /products/{id} | ✅ | ❌ |
| DELETE /products/{id} | ✅ | ❌ |
| POST /orders | ❌ | ✅ |
| GET /orders | ❌ | ✅ (own) |
| GET /orders/all | ✅ | ❌ |

## 🛡️ DevSecOps

### Outils de Sécurité

#### 1. OWASP Dependency Check
Analyse des vulnérabilités dans les dépendances Maven.
```bash
cd product-service && ./mvnw dependency-check:check
cd order-service && ./mvnw dependency-check:check
```

#### 2. SonarQube
Analyse statique du code.
```bash
./mvnw sonar:sonar -Dsonar.host.url=<URL> -Dsonar.login=<TOKEN>
```

#### 3. Trivy (Docker Image Scanning)
Scan des images Docker pour détecter les vulnérabilités.

**Windows (PowerShell):**
```powershell
.\scripts\trivy-scan.ps1
```

**Linux/Mac:**
```bash
./scripts/trivy-scan.sh
```

**Options:**
```powershell
.\scripts\trivy-scan.ps1 -Severity "CRITICAL" -Format "json"
```

### CI/CD Pipeline (GitHub Actions)
Le fichier `.github/workflows/devsecops.yml` inclut:
- Build et test automatiques
- OWASP Dependency Check
- SonarQube Analysis
- Trivy Docker Image Scanning
- Rapports de sécurité en artifacts

### Rapports de Sécurité
Les rapports sont stockés dans `security-reports/`:
- `trivy/` - Scans d'images Docker
- `owasp/` - Rapports Dependency-Check
- `sonarqube/` - Analyses SonarQube

## 📊 Monitoring & Logs

### Actuator Endpoints
Chaque service expose des endpoints de monitoring:
- `/actuator/health` - État de santé
- `/actuator/info` - Informations sur le service
- `/actuator/metrics` - Métriques

### Journalisation
Les logs sont configurés avec:
- Identification de l'utilisateur dans chaque requête
- Logs d'accès aux APIs
- Logs d'erreurs avec stack traces
- Fichiers de logs: `logs/<service-name>.log`

## 📁 Structure du Projet

```
projet_ouath2_oidc/
├── .github/workflows/     # CI/CD pipelines
├── diagrams/              # Architecture & sequence diagrams
├── gateway/               # API Gateway (Spring Cloud Gateway)
├── init-db/               # Database init scripts & Keycloak config
├── order-service/         # Order microservice
├── product-service/       # Product microservice
├── react-app/             # React frontend
├── scripts/               # Utility scripts (Trivy scan)
├── security-reports/      # Security scan reports
└── docker-compose.yml     # Container orchestration
```

## 🐳 Docker

### Build & Run All Services
```bash
docker-compose up --build
```

### Individual Service Build
```bash
docker build -t product-service:latest ./product-service
docker build -t order-service:latest ./order-service
docker build -t gateway:latest ./gateway
docker build -t react-app:latest ./react-app
```

## 📝 Livrables

- ✅ Code source versionné (Git)
- ✅ Diagramme d'architecture (`diagrams/ARCHITECTURE_DIAGRAM.md`)
- ✅ Diagramme de séquence (`diagrams/SEQUENCE_DIAGRAM.md`)
- ✅ Docker Compose fonctionnel
- ✅ Documentation technique (README)
- ✅ Scripts de sécurité (Trivy, OWASP, SonarQube)
- ✅ Health Dashboard
