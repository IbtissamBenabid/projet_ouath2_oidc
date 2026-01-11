# Security Reports

This directory contains security scan reports from various DevSecOps tools.

## Directory Structure

```
security-reports/
├── trivy/           # Docker image vulnerability scans
├── owasp/           # OWASP Dependency-Check reports
└── sonarqube/       # SonarQube analysis reports
```

## Tools Used

### 1. Trivy (Docker Image Scanning)
- **Purpose**: Scans Docker images for vulnerabilities
- **Run locally**: `.\scripts\trivy-scan.ps1` (Windows) or `./scripts/trivy-scan.sh` (Linux/Mac)
- **CI/CD**: Runs automatically in GitHub Actions

### 2. OWASP Dependency-Check
- **Purpose**: Scans Maven dependencies for known vulnerabilities
- **Run locally**: `mvn dependency-check:check`
- **Reports**: Generated in `target/dependency-check-report.html`

### 3. SonarQube
- **Purpose**: Static code analysis for bugs, vulnerabilities, and code smells
- **Run locally**: `mvn sonar:sonar -Dsonar.host.url=<URL> -Dsonar.login=<TOKEN>`

## Running Security Scans

### Local Trivy Scan
```powershell
# Windows PowerShell
cd projet_ouath2_oidc
.\scripts\trivy-scan.ps1

# With custom severity
.\scripts\trivy-scan.ps1 -Severity "CRITICAL"
```

### OWASP Dependency Check
```bash
# Product Service
cd product-service
./mvnw dependency-check:check

# Order Service
cd order-service
./mvnw dependency-check:check
```

## Interpreting Results

### Severity Levels
- **CRITICAL**: Must fix immediately
- **HIGH**: Should fix before deployment
- **MEDIUM**: Plan to fix
- **LOW**: Fix when convenient

### Common Vulnerabilities
1. **Outdated dependencies**: Update to latest versions
2. **Known CVEs**: Check NVD for patches
3. **Configuration issues**: Review security configs
