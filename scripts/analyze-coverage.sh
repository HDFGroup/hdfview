#!/bin/bash
# Generate coverage reports and analyze trends
# Part of Phase 2C: Quality and Static Analysis Implementation

set -e

echo "🔍 JaCoCo Coverage Analysis"
echo "=========================="

# Create scripts directory if it doesn't exist
mkdir -p scripts

# Check if we have the required tools
if ! command -v xmllint &> /dev/null; then
    echo "⚠️  xmllint not found. Installing libxml2-utils..."
    sudo apt-get update && sudo apt-get install -y libxml2-utils
fi

echo "📊 Generating JaCoCo coverage reports..."
mvn clean test jacoco:report

# Check if coverage report was generated
if [ ! -f "target/site/jacoco/jacoco.xml" ]; then
    echo "❌ JaCoCo XML report not found. Please ensure tests run successfully."
    exit 1
fi

echo "📈 Analyzing coverage metrics..."

# Extract coverage percentages using xmllint
TOTAL_LINES=$(xmllint --xpath "sum(//counter[@type='LINE']/@missed) + sum(//counter[@type='LINE']/@covered)" target/site/jacoco/jacoco.xml)
COVERED_LINES=$(xmllint --xpath "sum(//counter[@type='LINE']/@covered)" target/site/jacoco/jacoco.xml)

TOTAL_BRANCHES=$(xmllint --xpath "sum(//counter[@type='BRANCH']/@missed) + sum(//counter[@type='BRANCH']/@covered)" target/site/jacoco/jacoco.xml)
COVERED_BRANCHES=$(xmllint --xpath "sum(//counter[@type='BRANCH']/@covered)" target/site/jacoco/jacoco.xml)

# Calculate percentages using bc if available, otherwise use awk
if command -v bc &> /dev/null; then
    LINE_COVERAGE=$(echo "scale=2; $COVERED_LINES * 100 / $TOTAL_LINES" | bc)
    BRANCH_COVERAGE=$(echo "scale=2; $COVERED_BRANCHES * 100 / $TOTAL_BRANCHES" | bc)
else
    LINE_COVERAGE=$(awk "BEGIN {printf \"%.2f\", $COVERED_LINES * 100 / $TOTAL_LINES}")
    BRANCH_COVERAGE=$(awk "BEGIN {printf \"%.2f\", $COVERED_BRANCHES * 100 / $TOTAL_BRANCHES}")
fi

echo ""
echo "📊 Coverage Summary:"
echo "==================="
echo "Line Coverage:   ${LINE_COVERAGE}% (${COVERED_LINES}/${TOTAL_LINES})"
echo "Branch Coverage: ${BRANCH_COVERAGE}% (${COVERED_BRANCHES}/${TOTAL_BRANCHES})"
echo ""

# Check thresholds
LINE_THRESHOLD=60.0
BRANCH_THRESHOLD=50.0

echo "🎯 Quality Gate Analysis:"
echo "========================"

# Check line coverage threshold
if (( $(echo "$LINE_COVERAGE >= $LINE_THRESHOLD" | bc -l 2>/dev/null || echo "0") )); then
    echo "✅ Line coverage ($LINE_COVERAGE%) meets minimum threshold ($LINE_THRESHOLD%)"
    LINE_PASS=true
else
    echo "❌ Line coverage ($LINE_COVERAGE%) below minimum threshold ($LINE_THRESHOLD%)"
    LINE_PASS=false
fi

# Check branch coverage threshold
if (( $(echo "$BRANCH_COVERAGE >= $BRANCH_THRESHOLD" | bc -l 2>/dev/null || echo "0") )); then
    echo "✅ Branch coverage ($BRANCH_COVERAGE%) meets minimum threshold ($BRANCH_THRESHOLD%)"
    BRANCH_PASS=true
else
    echo "❌ Branch coverage ($BRANCH_COVERAGE%) below minimum threshold ($BRANCH_THRESHOLD%)"
    BRANCH_PASS=false
fi

echo ""
echo "📁 Generated Reports:"
echo "===================="
echo "- HTML Report: target/site/jacoco/index.html"
echo "- XML Report:  target/site/jacoco/jacoco.xml"
echo "- CSV Report:  target/site/jacoco/jacoco.csv"

# Store metrics for trend analysis
METRICS_DIR="quality-history"
mkdir -p "$METRICS_DIR"
TIMESTAMP=$(date -Iseconds)
METRICS_FILE="$METRICS_DIR/coverage-${TIMESTAMP}.json"

cat > "$METRICS_FILE" << EOF
{
    "timestamp": "$TIMESTAMP",
    "coverage": {
        "line": {
            "percentage": $LINE_COVERAGE,
            "covered": $COVERED_LINES,
            "total": $TOTAL_LINES
        },
        "branch": {
            "percentage": $BRANCH_COVERAGE,
            "covered": $COVERED_BRANCHES,
            "total": $TOTAL_BRANCHES
        }
    },
    "thresholds": {
        "line": $LINE_THRESHOLD,
        "branch": $BRANCH_THRESHOLD
    },
    "quality_gate": {
        "line_pass": $LINE_PASS,
        "branch_pass": $BRANCH_PASS,
        "overall_pass": $(if $LINE_PASS && $BRANCH_PASS; then echo "true"; else echo "false"; fi)
    }
}
EOF

echo "- Metrics JSON: $METRICS_FILE"
echo ""

# Summary
if $LINE_PASS && $BRANCH_PASS; then
    echo "🎉 All coverage quality gates passed!"
    exit 0
else
    echo "⚠️  Some coverage quality gates failed. This is expected during Phase 2C implementation."
    echo "    Quality gates are currently set to warning mode (haltOnFailure=false)"
    exit 0  # Don't fail during implementation phase
fi