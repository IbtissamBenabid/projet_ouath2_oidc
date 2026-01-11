# =============================================================================
# Trivy Docker Image Security Scanner (PowerShell)
# =============================================================================
# This script scans all project Docker images for vulnerabilities using Trivy
# Usage: .\trivy-scan.ps1 [-Severity "HIGH,CRITICAL"] [-Format "table"]
# =============================================================================

param(
    [string]$Severity = "HIGH,CRITICAL",
    [string]$Format = "table",
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: .\trivy-scan.ps1 [-Severity 'HIGH,CRITICAL'] [-Format 'table|json']"
    exit 0
}

# Configuration
$OutputDir = ".\security-reports\trivy"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# Docker images to scan
$Images = @(
    "product-service:latest",
    "order-service:latest",
    "gateway:latest",
    "react-app:latest"
)

# Create output directory
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

Write-Host "=============================================" -ForegroundColor Blue
Write-Host "   Trivy Docker Image Security Scanner" -ForegroundColor Blue
Write-Host "=============================================" -ForegroundColor Blue
Write-Host ""
Write-Host "Severity Filter: " -NoNewline
Write-Host $Severity -ForegroundColor Yellow
Write-Host "Output Format: " -NoNewline
Write-Host $Format -ForegroundColor Yellow
Write-Host "Timestamp: " -NoNewline
Write-Host $Timestamp -ForegroundColor Yellow
Write-Host ""

# Check if Trivy is installed
$trivyPath = Get-Command trivy -ErrorAction SilentlyContinue
if (-not $trivyPath) {
    Write-Host "Trivy is not installed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Install Trivy using one of these methods:"
    Write-Host "  - Chocolatey: choco install trivy"
    Write-Host "  - Scoop: scoop install trivy"
    Write-Host "  - Download: https://github.com/aquasecurity/trivy/releases"
    Write-Host ""
    Write-Host "Or run with Docker:" -ForegroundColor Yellow
    Write-Host "  docker run aquasec/trivy image <image-name>"
    Write-Host ""
    exit 1
}

$trivyVersion = & trivy --version
Write-Host "Trivy version: " -NoNewline -ForegroundColor Green
Write-Host $trivyVersion
Write-Host ""

$script:FailedScans = 0
$ScanResults = @()

function Scan-DockerImage {
    param([string]$ImageName)
    
    $reportFile = Join-Path $OutputDir ($ImageName -replace '[:/]', '_')
    $reportFile = "${reportFile}_$Timestamp"
    
    Write-Host "--------------------------------------------" -ForegroundColor Blue
    Write-Host "Scanning: " -NoNewline -ForegroundColor Blue
    Write-Host $ImageName -ForegroundColor Yellow
    Write-Host "--------------------------------------------" -ForegroundColor Blue
    
    # Check if image exists
    $imageExists = docker image inspect $ImageName 2>$null
    if (-not $imageExists) {
        Write-Host "[!] Image not found locally. Attempting to build..." -ForegroundColor Yellow
        
        switch -Wildcard ($ImageName) {
            "product-service*" { docker build -t $ImageName ./product-service 2>$null }
            "order-service*" { docker build -t $ImageName ./order-service 2>$null }
            "gateway*" { docker build -t $ImageName ./gateway 2>$null }
            "react-app*" { docker build -t $ImageName ./react-app 2>$null }
        }
    }
    
    # Run Trivy scan
    $scanSuccess = $true
    
    if ($Format -eq "json") {
        $jsonResult = & trivy image --severity $Severity --format json --output "${reportFile}.json" $ImageName 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] JSON report saved: ${reportFile}.json" -ForegroundColor Green
        }
    }
    
    # Table output
    $tableResult = & trivy image --severity $Severity --format table $ImageName 2>&1
    if ($LASTEXITCODE -ne 0) {
        $scanSuccess = $false
    }
    
    $tableResult | Tee-Object -FilePath "${reportFile}.txt"
    
    if ($scanSuccess) {
        return @{
            Image = $ImageName
            Status = "Success"
            ReportFile = $reportFile
        }
    }
    else {
        Write-Host "[X] Failed to scan $ImageName" -ForegroundColor Red
        $script:FailedScans++
        return @{
            Image = $ImageName
            Status = "Failed"
            Error = "Scan failed"
        }
    }
}

# Build images first
Write-Host "Building Docker images..." -ForegroundColor Blue
$buildResult = docker-compose build --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] docker-compose build skipped or failed" -ForegroundColor Yellow
}
Write-Host ""

# Scan each image
foreach ($image in $Images) {
    $result = Scan-DockerImage -ImageName $image
    $ScanResults += $result
    Write-Host ""
}

# Generate summary report
$SummaryFile = Join-Path $OutputDir "summary_$Timestamp.txt"

$summaryLines = @()
$summaryLines += "============================================="
$summaryLines += "   TRIVY SCAN SUMMARY REPORT"
$summaryLines += "============================================="
$summaryLines += "Date: $(Get-Date)"
$summaryLines += "Severity Filter: $Severity"
$summaryLines += ""
$summaryLines += "Images Scanned:"
foreach ($img in $Images) {
    $summaryLines += "  - $img"
}
$summaryLines += ""
$summaryLines += "Results:"
foreach ($res in $ScanResults) {
    $summaryLines += "  - $($res.Image): $($res.Status)"
}
$summaryLines += ""
$summaryLines += "Failed Scans: $($script:FailedScans)"
$summaryLines += ""
$summaryLines += "Reports saved in: $OutputDir"
$summaryLines += "============================================="

$summaryContent = $summaryLines -join "`r`n"
Set-Content -Path $SummaryFile -Value $summaryContent

Write-Host "=============================================" -ForegroundColor Blue
Write-Host "   SCAN COMPLETE" -ForegroundColor Blue
Write-Host "=============================================" -ForegroundColor Blue
Write-Host ""
Write-Host "Reports saved in: " -NoNewline
Write-Host $OutputDir -ForegroundColor Green
Write-Host "Summary: " -NoNewline
Write-Host $SummaryFile -ForegroundColor Green

if ($script:FailedScans -gt 0) {
    Write-Host "[!] $($script:FailedScans) image(s) failed to scan" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] All scans completed successfully" -ForegroundColor Green
exit 0
