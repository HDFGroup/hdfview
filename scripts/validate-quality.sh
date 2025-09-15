#!/bin/bash
# Validate quality gates before merge
# Part of Phase 2C: Quality and Static Analysis Implementation

set -e

echo "🚦 Quality Gate Validation"
echo "========================="

# Parse command line arguments
PROFILE="quality-report"  # Default to report-only mode
FAIL_ON_ERROR=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --enforce)
            FAIL_ON_ERROR=true
            shift
            ;;
        --help)
            echo "Usage: $0 [--profile <profile>] [--enforce]"
            echo ""
            echo "Profiles:"
            echo "  quality-report   - Report only (default, Phase 1)"
            echo "  quality-warn     - Warnings mode (Phase 2)"
            echo "  quality-enforce  - Enforce gates (Phase 3)"
            echo "  quality-dev      - Development mode (relaxed thresholds)"
            echo ""
            echo "Options:"
            echo "  --enforce        - Fail build on quality gate violations"
            echo "  --help           - Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "📋 Configuration:"
echo "  Profile: $PROFILE"
echo "  Fail on error: $FAIL_ON_ERROR"
echo ""

# Run quality gate profile
echo "🔍 Running quality analysis with profile: $PROFILE"
mvn clean verify -P"$PROFILE" -q

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ Quality gate validation completed successfully"
    echo ""

    # Show coverage summary if available
    if [ -f "target/site/jacoco/jacoco.xml" ]; then
        echo "📊 Coverage Summary:"
        ./scripts/analyze-coverage.sh | tail -n 10
    fi

    exit 0
else
    echo ""
    echo "❌ Quality gate validation failed"
    echo ""

    # Show detailed error information
    echo "🔍 Detailed Analysis:"
    echo "====================="

    if [ -f "target/site/jacoco/jacoco.xml" ]; then
        ./scripts/analyze-coverage.sh
    else
        echo "No coverage report found. Please check test execution."
    fi

    if [ "$FAIL_ON_ERROR" = true ]; then
        echo ""
        echo "Build failed due to quality gate violations (--enforce mode)"
        exit 1
    else
        echo ""
        echo "⚠️  Quality gates failed, but continuing due to implementation phase"
        echo "    (use --enforce to fail build on quality gate violations)"
        exit 0
    fi
fi