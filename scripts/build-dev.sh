#!/bin/bash
#
# build-dev.sh - Build HDFView to ready-to-run development state
#
# This script compiles the project and prepares it for immediate execution
# with ./run-hdfview.sh, skipping tests and quality checks for speed.
#
# Usage: ./scripts/build-dev.sh [options]
#
# Options:
#   --with-tests     Run tests after compiling (slower)
#   --with-quality   Run PMD/Checkstyle quality checks (much slower)
#   --clean          Run clean before compile
#   --help           Show this help message

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default options
RUN_TESTS=false
RUN_QUALITY=false
DO_CLEAN=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --with-tests)
            RUN_TESTS=true
            shift
            ;;
        --with-quality)
            RUN_QUALITY=true
            shift
            ;;
        --clean)
            DO_CLEAN=true
            shift
            ;;
        --help)
            grep '^#' "$0" | grep -v '#!/bin/bash' | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

cd "$PROJECT_ROOT"

echo "================================================"
echo "HDFView Development Build"
echo "================================================"
echo ""
echo "Build configuration:"
echo "  Run tests: $RUN_TESTS"
echo "  Quality checks: $RUN_QUALITY"
echo "  Clean first: $DO_CLEAN"
echo ""

# Step 1: Clean if requested
if [ "$DO_CLEAN" = true ]; then
    echo "Step 1: Cleaning..."
    mvn clean -q
    echo "  ✓ Cleaned"
    echo ""
fi

# Step 2: Compile object module
echo "Step 2: Compiling object module..."
OBJECT_OPTS="-DskipTests"
if [ "$RUN_QUALITY" = false ]; then
    OBJECT_OPTS="$OBJECT_OPTS -Dpmd.skip=true -Dcheckstyle.skip=true -Ddependency-check.skip=true"
fi

mvn compile $OBJECT_OPTS -pl object -q
echo "  ✓ Object module compiled"
echo ""

# Step 3: Package object module to libs/
echo "Step 3: Packaging object module..."
mvn package $OBJECT_OPTS -pl object -q
echo "  ✓ Object JAR created in libs/"
echo ""

# Step 4: Install object module to local repository
echo "Step 4: Installing object module to ~/.m2/repository..."
mvn install:install-file \
    -Dfile=libs/object-3.4-SNAPSHOT.jar \
    -DgroupId=org.hdfgroup.hdfview \
    -DartifactId=object \
    -Dversion=3.4-SNAPSHOT \
    -Dpackaging=jar \
    -q
echo "  ✓ Object module installed"
echo ""

# Step 5: Compile hdfview module
echo "Step 5: Compiling hdfview module..."
HDFVIEW_OPTS="-DskipTests"
if [ "$RUN_QUALITY" = false ]; then
    HDFVIEW_OPTS="$HDFVIEW_OPTS -Dpmd.skip=true -Dcheckstyle.skip=true -Ddependency-check.skip=true"
fi

mvn compile $HDFVIEW_OPTS -pl hdfview -q
echo "  ✓ HDFView module compiled"
echo ""

# Step 6: Compile tests
echo "Step 6: Compiling test classes..."
mvn test-compile $HDFVIEW_OPTS -pl hdfview -q
echo "  ✓ Test classes compiled"
echo ""

# Step 7: Run tests if requested
if [ "$RUN_TESTS" = true ]; then
    echo "Step 7: Running tests..."

    # Run object module tests
    echo "  Running object module tests..."
    mvn test -pl object -q

    # Run hdfview module tests
    echo "  Running hdfview module tests..."
    mvn test -pl hdfview -q

    echo "  ✓ All tests passed"
    echo ""
fi

echo "================================================"
echo "Build complete!"
echo "================================================"
echo ""
echo "Project is ready for development."
echo ""
echo "Next steps:"
echo "  • Run HDFView:"
echo "      ./run-hdfview.sh"
echo ""
echo "  • Run tests:"
echo "      mvn test -pl hdfview -Dtest=TestClassName"
echo ""
echo "  • Run specific UI test:"
echo "      mvn test -pl hdfview -Dtest=TestHDFViewFloat16#checkHDF5BFloat16AttrDS16BITS"
echo ""

# Show build artifacts
echo "Build artifacts:"
echo "  object JAR:  libs/object-3.4-SNAPSHOT.jar"
if [ -f "libs/hdfview-3.4-SNAPSHOT.jar" ]; then
    echo "  hdfview JAR: libs/hdfview-3.4-SNAPSHOT.jar"
fi
echo ""
