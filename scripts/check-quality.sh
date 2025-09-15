#!/bin/bash
# Comprehensive quality check script
# Part of Phase 2C: Quality and Static Analysis Implementation

set -e

echo "🔍 HDFView Comprehensive Quality Analysis"
echo "========================================"

# Parse command line arguments
SKIP_TESTS=false
SKIP_PMD=false
SKIP_CHECKSTYLE=false
SKIP_COVERAGE=false
GENERATE_SITE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-pmd)
            SKIP_PMD=true
            shift
            ;;
        --skip-checkstyle)
            SKIP_CHECKSTYLE=true
            shift
            ;;
        --skip-coverage)
            SKIP_COVERAGE=true
            shift
            ;;
        --generate-site)
            GENERATE_SITE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --skip-tests      Skip test execution"
            echo "  --skip-pmd        Skip PMD analysis"
            echo "  --skip-checkstyle Skip Checkstyle analysis"
            echo "  --skip-coverage   Skip coverage analysis"
            echo "  --generate-site   Generate Maven site with all reports"
            echo "  --help            Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "📋 Analysis Configuration:"
echo "  Skip tests: $SKIP_TESTS"
echo "  Skip PMD: $SKIP_PMD"
echo "  Skip Checkstyle: $SKIP_CHECKSTYLE"
echo "  Skip Coverage: $SKIP_COVERAGE"
echo "  Generate Site: $GENERATE_SITE"
echo ""

# Clean previous reports
echo "🧹 Cleaning previous reports..."
mvn clean -q

# Run tests and generate coverage
if [ "$SKIP_TESTS" = false ] && [ "$SKIP_COVERAGE" = false ]; then
    echo "🧪 Running tests and generating coverage..."
    mvn test jacoco:report -q
elif [ "$SKIP_TESTS" = false ]; then
    echo "🧪 Running tests..."
    mvn test -q
fi

# PMD Analysis
if [ "$SKIP_PMD" = false ]; then
    echo "🔍 Running PMD static analysis..."
    mvn pmd:pmd pmd:cpd -q

    if [ -f "target/site/pmd.html" ]; then
        echo "✅ PMD analysis completed"
        # Count violations
        if [ -f "target/pmd.xml" ]; then
            PMD_VIOLATIONS=$(grep -c '<violation' target/pmd.xml || echo "0")
            echo "   PMD violations found: $PMD_VIOLATIONS"
        fi
    else
        echo "⚠️  PMD analysis may have failed - no report generated"
    fi
fi

# Checkstyle Analysis
if [ "$SKIP_CHECKSTYLE" = false ]; then
    echo "📋 Running Checkstyle analysis..."
    mvn checkstyle:checkstyle -q

    if [ -f "target/site/checkstyle.html" ]; then
        echo "✅ Checkstyle analysis completed"
        # Count violations
        if [ -f "target/checkstyle-result.xml" ]; then
            CHECKSTYLE_ERRORS=$(grep -c 'severity="error"' target/checkstyle-result.xml || echo "0")
            CHECKSTYLE_WARNINGS=$(grep -c 'severity="warning"' target/checkstyle-result.xml || echo "0")
            echo "   Checkstyle errors: $CHECKSTYLE_ERRORS"
            echo "   Checkstyle warnings: $CHECKSTYLE_WARNINGS"
        fi
    else
        echo "⚠️  Checkstyle analysis may have failed - no report generated"
    fi
fi

# Coverage Analysis
if [ "$SKIP_COVERAGE" = false ]; then
    echo "📊 Running coverage analysis..."
    if [ -x "./scripts/analyze-coverage.sh" ]; then
        ./scripts/analyze-coverage.sh
    else
        echo "⚠️  Coverage analysis script not found or not executable"
    fi
fi

# Generate comprehensive site
if [ "$GENERATE_SITE" = true ]; then
    echo "🌐 Generating comprehensive site..."
    mvn site -q
    echo "✅ Site generated at target/site/index.html"
fi

echo ""
echo "📊 Quality Reports Generated:"
echo "============================="

# List available reports
if [ -f "target/site/jacoco/index.html" ]; then
    echo "✅ Code Coverage: target/site/jacoco/index.html"
fi

if [ -f "target/site/pmd.html" ]; then
    echo "✅ PMD Analysis: target/site/pmd.html"
fi

if [ -f "target/site/checkstyle.html" ]; then
    echo "✅ Checkstyle: target/site/checkstyle.html"
fi

if [ -f "target/site/index.html" ]; then
    echo "✅ Unified Site: target/site/index.html"
fi

echo ""

# Summary
echo "🎯 Quality Analysis Summary:"
echo "============================"

if [ "$SKIP_COVERAGE" = false ] && [ -f "quality-history" ]; then
    LATEST_METRICS=$(ls -t quality-history/*.json 2>/dev/null | head -1)
    if [ -n "$LATEST_METRICS" ]; then
        echo "📈 Latest quality metrics stored in: $LATEST_METRICS"
    fi
fi

if [ "$SKIP_PMD" = false ] && [ -n "${PMD_VIOLATIONS:-}" ]; then
    if [ "$PMD_VIOLATIONS" -eq 0 ]; then
        echo "✅ PMD: No violations found"
    elif [ "$PMD_VIOLATIONS" -le 20 ]; then
        echo "⚠️  PMD: $PMD_VIOLATIONS violations (acceptable)"
    else
        echo "❌ PMD: $PMD_VIOLATIONS violations (exceeds recommended limit of 20)"
    fi
fi

if [ "$SKIP_CHECKSTYLE" = false ] && [ -n "${CHECKSTYLE_ERRORS:-}" ]; then
    if [ "$CHECKSTYLE_ERRORS" -eq 0 ]; then
        echo "✅ Checkstyle: No errors found"
    else
        echo "❌ Checkstyle: $CHECKSTYLE_ERRORS errors found"
    fi
fi

echo ""
echo "🎉 Quality analysis complete!"
echo ""
echo "💡 Next steps:"
echo "   1. Review reports in target/site/"
echo "   2. Address any violations found"
echo "   3. Run './scripts/validate-quality.sh' to check thresholds"