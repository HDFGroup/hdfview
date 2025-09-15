#!/bin/bash
# Comprehensive security analysis
# Part of Phase 2C: Quality and Static Analysis Implementation

set -e

echo "🔒 HDFView Security Analysis"
echo "==========================="

# Parse command line arguments
SKIP_DEPENDENCY_CHECK=false
SKIP_LICENSE_CHECK=false
UPDATE_DATABASE=true

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-dependency-check)
            SKIP_DEPENDENCY_CHECK=true
            shift
            ;;
        --skip-license-check)
            SKIP_LICENSE_CHECK=true
            shift
            ;;
        --no-update)
            UPDATE_DATABASE=false
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --skip-dependency-check  Skip OWASP dependency vulnerability check"
            echo "  --skip-license-check     Skip license compliance check"
            echo "  --no-update              Skip CVE database update"
            echo "  --help                   Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "📋 Security Analysis Configuration:"
echo "  Dependency Check: $(if [ "$SKIP_DEPENDENCY_CHECK" = false ]; then echo "enabled"; else echo "disabled"; fi)"
echo "  License Check: $(if [ "$SKIP_LICENSE_CHECK" = false ]; then echo "enabled"; else echo "disabled"; fi)"
echo "  Database Update: $(if [ "$UPDATE_DATABASE" = true ]; then echo "enabled"; else echo "disabled"; fi)"
echo ""

# Clean previous reports
echo "🧹 Cleaning previous security reports..."
rm -rf target/dependency-check target/generated-sources/license
mkdir -p target/dependency-check

# OWASP Dependency Check
if [ "$SKIP_DEPENDENCY_CHECK" = false ]; then
    echo "🛡️  Running OWASP Dependency Check..."
    echo "   This may take several minutes on first run..."

    # Set update flag
    UPDATE_FLAG=""
    if [ "$UPDATE_DATABASE" = false ]; then
        UPDATE_FLAG="-Dowasp.dependency.check.autoUpdate=false"
    fi

    # Run dependency check
    mvn org.owasp:dependency-check-maven:check $UPDATE_FLAG -q

    # Analyze results
    if [ -f "target/dependency-check/dependency-check-report.html" ]; then
        echo "✅ OWASP Dependency Check completed"

        # Count vulnerabilities by severity
        if [ -f "target/dependency-check/dependency-check-report.json" ]; then
            if command -v jq &> /dev/null; then
                HIGH_VULNS=$(jq '.dependencies[]?.vulnerabilities[]? | select(.severity == "HIGH")' target/dependency-check/dependency-check-report.json 2>/dev/null | jq -s length)
                MEDIUM_VULNS=$(jq '.dependencies[]?.vulnerabilities[]? | select(.severity == "MEDIUM")' target/dependency-check/dependency-check-report.json 2>/dev/null | jq -s length)
                LOW_VULNS=$(jq '.dependencies[]?.vulnerabilities[]? | select(.severity == "LOW")' target/dependency-check/dependency-check-report.json 2>/dev/null | jq -s length)

                echo "   Vulnerability Summary:"
                echo "   - High: ${HIGH_VULNS:-0}"
                echo "   - Medium: ${MEDIUM_VULNS:-0}"
                echo "   - Low: ${LOW_VULNS:-0}"

                # Check thresholds
                if [ "${HIGH_VULNS:-0}" -gt 0 ]; then
                    echo "   ⚠️  High severity vulnerabilities found!"
                fi
                if [ "${MEDIUM_VULNS:-0}" -gt 10 ]; then
                    echo "   ⚠️  Many medium severity vulnerabilities found"
                fi
            else
                echo "   ℹ️  Install 'jq' for detailed vulnerability analysis"
            fi
        fi
    else
        echo "❌ OWASP Dependency Check failed - no report generated"
    fi
fi

# License compliance check
if [ "$SKIP_LICENSE_CHECK" = false ]; then
    echo "📜 Running license compliance check..."

    # Generate license report
    mvn license:aggregate-third-party-report -q

    if [ -f "target/generated-sources/license/THIRD-PARTY.txt" ]; then
        echo "✅ License report generated"

        # Check for prohibited licenses
        PROHIBITED_LICENSES="GPL-2.0|GPL-3.0|AGPL|SSPL|BUSL"
        if [ -f "target/generated-sources/license/THIRD-PARTY.txt" ]; then
            if grep -iE "$PROHIBITED_LICENSES" target/generated-sources/license/THIRD-PARTY.txt > /dev/null; then
                echo "   ⚠️  Potentially prohibited licenses found:"
                grep -iE "$PROHIBITED_LICENSES" target/generated-sources/license/THIRD-PARTY.txt | head -5
                echo "   Please review the license report for details."
            else
                echo "   ✅ No obviously prohibited licenses detected"
            fi
        fi

        # Count unique licenses
        if command -v awk &> /dev/null; then
            LICENSE_COUNT=$(grep -E "^\s*\(" target/generated-sources/license/THIRD-PARTY.txt | sort -u | wc -l)
            echo "   Total unique licenses found: $LICENSE_COUNT"
        fi
    else
        echo "❌ License report generation failed"
    fi
fi

echo ""
echo "📊 Security Reports Generated:"
echo "============================="

# List available reports
if [ -f "target/dependency-check/dependency-check-report.html" ]; then
    echo "✅ Vulnerability Report: target/dependency-check/dependency-check-report.html"
    echo "   JSON Report: target/dependency-check/dependency-check-report.json"
    echo "   XML Report: target/dependency-check/dependency-check-report.xml"
fi

if [ -f "target/generated-sources/license/THIRD-PARTY.txt" ]; then
    echo "✅ License Report: target/generated-sources/license/THIRD-PARTY.txt"
fi

# Generate security summary
TIMESTAMP=$(date -Iseconds)
SECURITY_SUMMARY="target/security-summary-${TIMESTAMP}.json"

cat > "$SECURITY_SUMMARY" << EOF
{
    "timestamp": "$TIMESTAMP",
    "security_analysis": {
        "dependency_check": {
            "enabled": $(if [ "$SKIP_DEPENDENCY_CHECK" = false ]; then echo "true"; else echo "false"; fi),
            "report_generated": $(if [ -f "target/dependency-check/dependency-check-report.html" ]; then echo "true"; else echo "false"; fi),
            "vulnerabilities": {
                "high": ${HIGH_VULNS:-0},
                "medium": ${MEDIUM_VULNS:-0},
                "low": ${LOW_VULNS:-0}
            }
        },
        "license_check": {
            "enabled": $(if [ "$SKIP_LICENSE_CHECK" = false ]; then echo "true"; else echo "false"; fi),
            "report_generated": $(if [ -f "target/generated-sources/license/THIRD-PARTY.txt" ]; then echo "true"; else echo "false"; fi),
            "unique_licenses": ${LICENSE_COUNT:-0}
        }
    }
}
EOF

echo "✅ Security Summary: $SECURITY_SUMMARY"

echo ""
echo "🎯 Security Analysis Summary:"
echo "============================="

if [ "$SKIP_DEPENDENCY_CHECK" = false ]; then
    if [ "${HIGH_VULNS:-0}" -eq 0 ]; then
        echo "✅ No high-severity vulnerabilities found"
    else
        echo "❌ ${HIGH_VULNS} high-severity vulnerabilities require attention"
    fi
fi

if [ "$SKIP_LICENSE_CHECK" = false ]; then
    echo "📜 License compliance check completed"
    echo "   Review target/generated-sources/license/THIRD-PARTY.txt for details"
fi

echo ""
echo "🎉 Security analysis complete!"
echo ""
echo "💡 Next steps:"
echo "   1. Review reports in target/dependency-check/ and target/generated-sources/license/"
echo "   2. Address any high-severity vulnerabilities"
echo "   3. Review license compliance report"
echo "   4. Update dependency-check-suppressions.xml for false positives"