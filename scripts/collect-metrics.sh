#!/bin/bash
# Collect and aggregate quality metrics
# Part of Phase 2C: Quality and Static Analysis Implementation

set -e

echo "📊 HDFView Quality Metrics Collection"
echo "===================================="

# Parse command line arguments
GENERATE_SITE=false
STORE_HISTORY=true
OUTPUT_FORMAT="json"

while [[ $# -gt 0 ]]; do
    case $1 in
        --site)
            GENERATE_SITE=true
            shift
            ;;
        --no-history)
            STORE_HISTORY=false
            shift
            ;;
        --format)
            OUTPUT_FORMAT="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --site        Generate comprehensive Maven site"
            echo "  --no-history  Don't store metrics in history"
            echo "  --format      Output format (json, xml, text) [default: json]"
            echo "  --help        Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "📋 Collection Configuration:"
echo "  Generate Site: $GENERATE_SITE"
echo "  Store History: $STORE_HISTORY"
echo "  Output Format: $OUTPUT_FORMAT"
echo ""

# Ensure required tools are available
if ! command -v xmllint &> /dev/null; then
    echo "⚠️  xmllint not found. Installing libxml2-utils..."
    sudo apt-get update && sudo apt-get install -y libxml2-utils
fi

echo "📈 Generating all quality reports..."
mvn clean test site -q

echo "📊 Extracting metrics from reports..."

# Initialize variables
COVERAGE_LINE=0
COVERAGE_BRANCH=0
PMD_VIOLATIONS=0
CHECKSTYLE_ERRORS=0
CHECKSTYLE_WARNINGS=0
HIGH_VULNS=0
MEDIUM_VULNS=0
LOW_VULNS=0
LICENSE_COUNT=0

# Extract JaCoCo Coverage
if [ -f "target/site/jacoco/jacoco.xml" ]; then
    TOTAL_LINES=$(xmllint --xpath "sum(//counter[@type='LINE']/@missed) + sum(//counter[@type='LINE']/@covered)" target/site/jacoco/jacoco.xml 2>/dev/null || echo "0")
    COVERED_LINES=$(xmllint --xpath "sum(//counter[@type='LINE']/@covered)" target/site/jacoco/jacoco.xml 2>/dev/null || echo "0")

    TOTAL_BRANCHES=$(xmllint --xpath "sum(//counter[@type='BRANCH']/@missed) + sum(//counter[@type='BRANCH']/@covered)" target/site/jacoco/jacoco.xml 2>/dev/null || echo "0")
    COVERED_BRANCHES=$(xmllint --xpath "sum(//counter[@type='BRANCH']/@covered)" target/site/jacoco/jacoco.xml 2>/dev/null || echo "0")

    if [ "$TOTAL_LINES" -gt 0 ]; then
        COVERAGE_LINE=$(awk "BEGIN {printf \"%.2f\", $COVERED_LINES * 100 / $TOTAL_LINES}")
    fi
    if [ "$TOTAL_BRANCHES" -gt 0 ]; then
        COVERAGE_BRANCH=$(awk "BEGIN {printf \"%.2f\", $COVERED_BRANCHES * 100 / $TOTAL_BRANCHES}")
    fi

    echo "✅ Coverage metrics extracted: ${COVERAGE_LINE}% line, ${COVERAGE_BRANCH}% branch"
else
    echo "⚠️  No JaCoCo coverage report found"
fi

# Extract PMD Violations
if [ -f "target/site/pmd.xml" ]; then
    PMD_VIOLATIONS=$(grep -c '<violation' target/site/pmd.xml 2>/dev/null || echo "0")
    echo "✅ PMD violations extracted: $PMD_VIOLATIONS"
else
    echo "⚠️  No PMD report found"
fi

# Extract Checkstyle Violations
if [ -f "target/checkstyle-result.xml" ]; then
    CHECKSTYLE_ERRORS=$(grep -c 'severity="error"' target/checkstyle-result.xml 2>/dev/null || echo "0")
    CHECKSTYLE_WARNINGS=$(grep -c 'severity="warning"' target/checkstyle-result.xml 2>/dev/null || echo "0")
    echo "✅ Checkstyle violations extracted: $CHECKSTYLE_ERRORS errors, $CHECKSTYLE_WARNINGS warnings"
else
    echo "⚠️  No Checkstyle report found"
fi

# Extract OWASP Dependency Check Results
if [ -f "target/dependency-check/dependency-check-report.json" ]; then
    if command -v jq &> /dev/null; then
        HIGH_VULNS=$(jq '.dependencies[]?.vulnerabilities[]? | select(.severity == "HIGH")' target/dependency-check/dependency-check-report.json 2>/dev/null | jq -s length)
        MEDIUM_VULNS=$(jq '.dependencies[]?.vulnerabilities[]? | select(.severity == "MEDIUM")' target/dependency-check/dependency-check-report.json 2>/dev/null | jq -s length)
        LOW_VULNS=$(jq '.dependencies[]?.vulnerabilities[]? | select(.severity == "LOW")' target/dependency-check/dependency-check-report.json 2>/dev/null | jq -s length)
        echo "✅ Security vulnerabilities extracted: $HIGH_VULNS high, $MEDIUM_VULNS medium, $LOW_VULNS low"
    else
        echo "⚠️  jq not available for security metrics parsing"
    fi
else
    echo "⚠️  No OWASP dependency check report found"
fi

# Extract License Information
if [ -f "target/generated-sources/license/THIRD-PARTY.txt" ]; then
    LICENSE_COUNT=$(grep -E "^\s*\(" target/generated-sources/license/THIRD-PARTY.txt 2>/dev/null | sort -u | wc -l)
    echo "✅ License information extracted: $LICENSE_COUNT unique licenses"
else
    echo "⚠️  No license report found"
fi

# Calculate quality scores
COVERAGE_SCORE=0
if (( $(echo "$COVERAGE_LINE >= 60" | bc -l 2>/dev/null || echo "0") )); then
    COVERAGE_SCORE=100
elif (( $(echo "$COVERAGE_LINE >= 40" | bc -l 2>/dev/null || echo "0") )); then
    COVERAGE_SCORE=75
elif (( $(echo "$COVERAGE_LINE >= 20" | bc -l 2>/dev/null || echo "0") )); then
    COVERAGE_SCORE=50
else
    COVERAGE_SCORE=25
fi

PMD_SCORE=100
if [ "$PMD_VIOLATIONS" -gt 50 ]; then
    PMD_SCORE=25
elif [ "$PMD_VIOLATIONS" -gt 20 ]; then
    PMD_SCORE=75
fi

CHECKSTYLE_SCORE=100
if [ "$CHECKSTYLE_ERRORS" -gt 0 ]; then
    CHECKSTYLE_SCORE=0
elif [ "$CHECKSTYLE_WARNINGS" -gt 100 ]; then
    CHECKSTYLE_SCORE=50
fi

SECURITY_SCORE=100
if [ "$HIGH_VULNS" -gt 0 ]; then
    SECURITY_SCORE=0
elif [ "$MEDIUM_VULNS" -gt 10 ]; then
    SECURITY_SCORE=50
fi

OVERALL_SCORE=$(((COVERAGE_SCORE + PMD_SCORE + CHECKSTYLE_SCORE + SECURITY_SCORE) / 4))

# Generate timestamp
TIMESTAMP=$(date -Iseconds)

# Create metrics output
case $OUTPUT_FORMAT in
    json)
        METRICS_FILE="target/quality-metrics-${TIMESTAMP}.json"
        cat > "$METRICS_FILE" << EOF
{
    "timestamp": "$TIMESTAMP",
    "coverage": {
        "line": {
            "percentage": $COVERAGE_LINE,
            "covered": $COVERED_LINES,
            "total": $TOTAL_LINES
        },
        "branch": {
            "percentage": $COVERAGE_BRANCH,
            "covered": $COVERED_BRANCHES,
            "total": $TOTAL_BRANCHES
        },
        "score": $COVERAGE_SCORE
    },
    "static_analysis": {
        "pmd": {
            "violations": $PMD_VIOLATIONS,
            "score": $PMD_SCORE
        },
        "checkstyle": {
            "errors": $CHECKSTYLE_ERRORS,
            "warnings": $CHECKSTYLE_WARNINGS,
            "score": $CHECKSTYLE_SCORE
        }
    },
    "security": {
        "vulnerabilities": {
            "high": $HIGH_VULNS,
            "medium": $MEDIUM_VULNS,
            "low": $LOW_VULNS
        },
        "licenses": {
            "unique_count": $LICENSE_COUNT
        },
        "score": $SECURITY_SCORE
    },
    "quality_score": {
        "overall": $OVERALL_SCORE,
        "breakdown": {
            "coverage": $COVERAGE_SCORE,
            "static_analysis": $(((PMD_SCORE + CHECKSTYLE_SCORE) / 2)),
            "security": $SECURITY_SCORE
        }
    },
    "thresholds": {
        "coverage_line_min": 60.0,
        "coverage_branch_min": 50.0,
        "pmd_violations_max": 20,
        "checkstyle_errors_max": 0,
        "high_vulnerabilities_max": 0
    }
}
EOF
        echo "📁 JSON metrics: $METRICS_FILE"
        ;;

    xml)
        METRICS_FILE="target/quality-metrics-${TIMESTAMP}.xml"
        cat > "$METRICS_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<quality-metrics timestamp="$TIMESTAMP">
    <coverage>
        <line percentage="$COVERAGE_LINE" covered="$COVERED_LINES" total="$TOTAL_LINES"/>
        <branch percentage="$COVERAGE_BRANCH" covered="$COVERED_BRANCHES" total="$TOTAL_BRANCHES"/>
        <score>$COVERAGE_SCORE</score>
    </coverage>
    <static-analysis>
        <pmd violations="$PMD_VIOLATIONS" score="$PMD_SCORE"/>
        <checkstyle errors="$CHECKSTYLE_ERRORS" warnings="$CHECKSTYLE_WARNINGS" score="$CHECKSTYLE_SCORE"/>
    </static-analysis>
    <security>
        <vulnerabilities high="$HIGH_VULNS" medium="$MEDIUM_VULNS" low="$LOW_VULNS"/>
        <licenses unique-count="$LICENSE_COUNT"/>
        <score>$SECURITY_SCORE</score>
    </security>
    <quality-score overall="$OVERALL_SCORE">
        <breakdown coverage="$COVERAGE_SCORE" static-analysis="$(((PMD_SCORE + CHECKSTYLE_SCORE) / 2))" security="$SECURITY_SCORE"/>
    </quality-score>
</quality-metrics>
EOF
        echo "📁 XML metrics: $METRICS_FILE"
        ;;

    text)
        METRICS_FILE="target/quality-metrics-${TIMESTAMP}.txt"
        cat > "$METRICS_FILE" << EOF
HDFView Quality Metrics Report
Generated: $TIMESTAMP

COVERAGE METRICS
================
Line Coverage:    ${COVERAGE_LINE}% (${COVERED_LINES}/${TOTAL_LINES})
Branch Coverage:  ${COVERAGE_BRANCH}% (${COVERED_BRANCHES}/${TOTAL_BRANCHES})
Coverage Score:   $COVERAGE_SCORE/100

STATIC ANALYSIS
===============
PMD Violations:         $PMD_VIOLATIONS
PMD Score:              $PMD_SCORE/100
Checkstyle Errors:      $CHECKSTYLE_ERRORS
Checkstyle Warnings:    $CHECKSTYLE_WARNINGS
Checkstyle Score:       $CHECKSTYLE_SCORE/100

SECURITY ANALYSIS
=================
High Vulnerabilities:   $HIGH_VULNS
Medium Vulnerabilities: $MEDIUM_VULNS
Low Vulnerabilities:    $LOW_VULNS
Unique Licenses:        $LICENSE_COUNT
Security Score:         $SECURITY_SCORE/100

OVERALL QUALITY
===============
Quality Score:          $OVERALL_SCORE/100

THRESHOLDS
==========
Line Coverage Min:      60%
Branch Coverage Min:    50%
PMD Violations Max:     20
Checkstyle Errors Max:  0
High Vulns Max:         0
EOF
        echo "📁 Text metrics: $METRICS_FILE"
        ;;
esac

# Store in quality history
if [ "$STORE_HISTORY" = true ]; then
    HISTORY_DIR="quality-history"
    mkdir -p "$HISTORY_DIR"
    cp "$METRICS_FILE" "$HISTORY_DIR/"
    echo "📈 Metrics stored in quality history"
fi

echo ""
echo "📊 Quality Dashboard Summary:"
echo "============================="
echo "🎯 Overall Quality Score: $OVERALL_SCORE/100"
echo ""
echo "📈 Component Scores:"
echo "   Coverage:        $COVERAGE_SCORE/100 (${COVERAGE_LINE}% line coverage)"
echo "   Static Analysis: $(((PMD_SCORE + CHECKSTYLE_SCORE) / 2))/100 ($PMD_VIOLATIONS PMD violations, $CHECKSTYLE_ERRORS Checkstyle errors)"
echo "   Security:        $SECURITY_SCORE/100 ($HIGH_VULNS high vulnerabilities)"
echo ""
echo "📁 Reports Available:"
if [ -f "target/site/index.html" ]; then
    echo "   🌐 Unified Dashboard: target/site/index.html"
fi
echo "   📊 Coverage: target/site/jacoco/index.html"
echo "   🔍 PMD: target/site/pmd.html"
echo "   📋 Checkstyle: target/site/checkstyle.html"
echo "   🔒 Security: target/dependency-check/dependency-check-report.html"
echo "   📜 Licenses: target/generated-sources/license/THIRD-PARTY.txt"
echo ""
echo "🎉 Quality metrics collection complete!"