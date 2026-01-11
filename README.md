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

## Démarrage Rapide

### 1. Infrastructure (Docker)
Lancer les conteneurs PostgreSQL et Keycloak :
```bash
docker-compose up -d
```

### 2. Configuration Keycloak
Accédez à `http://localhost:8080` (admin/admin).
1. Créer un realm `microservices-realm`.
2. Créer un client `react-client`:
   - Client authentication: Off (Public)
   - Valid Redirect URIs: `http://localhost:3000/*`
   - Web Origins: `*`
3. Créer des utilisateurs de test :
   - `user1` (Rôles: `CLIENT`)
   - `admin` (Rôles: `ADMIN`)
4. Créer les rôles de realm `ADMIN` et `CLIENT` (ou configurez les Scopes si nécessaire).

### 3. Backend
Lancer les services dans l'ordre :
1. **Gateway** (`gateway`)
2. **Product Service** (`product-service`)
3. **Order Service** (`order-service`)

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

## Sécurité
- L'accès aux APIs passe obligatoirement par la Gateway.
- Le token JWT est validé par chaque service.
- Les rôles `ADMIN` et `CLIENT` contrôlent l'accès aux endpoints.

## DevSecOps
Le projet inclut les plugins Maven pour :
- **SonarQube**: Analyse statique (`mvn sonar:sonar`)
- **OWASP Dependency Check**: Analyse de vulnérabilités (`mvn dependency-check:check`)
