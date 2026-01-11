#!/bin/bash

# =============================================================================
# Trivy Docker Image Security Scanner
# =============================================================================
# This script scans all project Docker images for vulnerabilities using Trivy
# Usage: ./trivy-scan.sh [--severity HIGH,CRITICAL] [--format table|json]
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SEVERITY="${SEVERITY:-HIGH,CRITICAL}"
FORMAT="${FORMAT:-table}"
OUTPUT_DIR="./security-reports/trivy"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Docker images to scan
IMAGES=(
    "product-service:latest"
    "order-service:latest"
    "gateway:latest"
    "react-app:latest"
)

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --severity)
            SEVERITY="$2"
            shift 2
            ;;
        --format)
            FORMAT="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [--severity HIGH,CRITICAL] [--format table|json]"
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}   Trivy Docker Image Security Scanner${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""
echo -e "Severity Filter: ${YELLOW}$SEVERITY${NC}"
echo -e "Output Format: ${YELLOW}$FORMAT${NC}"
echo -e "Timestamp: ${YELLOW}$TIMESTAMP${NC}"
echo ""

# Check if Trivy is installed
if ! command -v trivy &> /dev/null; then
    echo -e "${RED}Trivy is not installed!${NC}"
    echo ""
    echo "Install Trivy using one of these methods:"
    echo "  - Windows (Chocolatey): choco install trivy"
    echo "  - Windows (Scoop): scoop install trivy"
    echo "  - Linux: curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin"
    echo "  - Docker: docker run aquasec/trivy image <image-name>"
    echo ""
    exit 1
fi

echo -e "${GREEN}Trivy version:${NC} $(trivy --version)"
echo ""

# Summary counters
TOTAL_CRITICAL=0
TOTAL_HIGH=0
TOTAL_MEDIUM=0
TOTAL_LOW=0
FAILED_SCANS=0

# Function to scan a single image
scan_image() {
    local image=$1
    local report_file="$OUTPUT_DIR/${image//[:\/]/_}_$TIMESTAMP"

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Scanning: ${YELLOW}$image${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # Check if image exists
    if ! docker image inspect "$image" &> /dev/null; then
        echo -e "${YELLOW}⚠ Image not found locally. Building...${NC}"
        
        # Try to determine the build context
        case $image in
            product-service*)
                docker build -t "$image" ./product-service 2>/dev/null || true
                ;;
            order-service*)
                docker build -t "$image" ./order-service 2>/dev/null || true
                ;;
            gateway*)
                docker build -t "$image" ./gateway 2>/dev/null || true
                ;;
            react-app*)
                docker build -t "$image" ./react-app 2>/dev/null || true
                ;;
        esac
    fi

    # Run Trivy scan
    if [ "$FORMAT" == "json" ]; then
        trivy image --severity "$SEVERITY" --format json --output "${report_file}.json" "$image" 2>/dev/null || {
            echo -e "${RED}✗ Failed to scan $image${NC}"
            ((FAILED_SCANS++))
            return
        }
        echo -e "${GREEN}✓ JSON report saved: ${report_file}.json${NC}"
    fi

    # Always output table format to console
    trivy image --severity "$SEVERITY" --format table "$image" 2>/dev/null | tee "${report_file}.txt" || {
        echo -e "${RED}✗ Failed to scan $image${NC}"
        ((FAILED_SCANS++))
        return
    }

    echo ""
}

# Build images first if using docker-compose
echo -e "${BLUE}Building Docker images...${NC}"
docker-compose build --quiet 2>/dev/null || echo -e "${YELLOW}⚠ docker-compose build skipped${NC}"
echo ""

# Scan each image
for image in "${IMAGES[@]}"; do
    scan_image "$image"
done

# Generate summary report
SUMMARY_FILE="$OUTPUT_DIR/summary_$TIMESTAMP.txt"
{
    echo "============================================="
    echo "   TRIVY SCAN SUMMARY REPORT"
    echo "============================================="
    echo "Date: $(date)"
    echo "Severity Filter: $SEVERITY"
    echo ""
    echo "Images Scanned:"
    for image in "${IMAGES[@]}"; do
        echo "  - $image"
    done
    echo ""
    echo "Failed Scans: $FAILED_SCANS"
    echo ""
    echo "Reports saved in: $OUTPUT_DIR"
    echo "============================================="
} > "$SUMMARY_FILE"

echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}   SCAN COMPLETE${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""
echo -e "Reports saved in: ${GREEN}$OUTPUT_DIR${NC}"
echo -e "Summary: ${GREEN}$SUMMARY_FILE${NC}"

if [ $FAILED_SCANS -gt 0 ]; then
    echo -e "${RED}⚠ $FAILED_SCANS image(s) failed to scan${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All scans completed successfully${NC}"
