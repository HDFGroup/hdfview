#!/bin/bash
#
# Local test script for jpackage integration
# Mimics the CI workflow to test jpackage locally
#

set -e  # Exit on error

# Change to project root (script is in scripts/ folder)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo "========================================"
echo "Local jpackage Test Script"
echo "========================================"
echo "Working directory: $(pwd)"
echo

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if HDF libraries are available
if [ ! -f "build.properties" ]; then
    echo -e "${YELLOW}Warning: build.properties not found${NC}"
    echo "Creating minimal build.properties for testing..."
    cat > build.properties << 'EOF'
# Minimal build.properties for local testing
# Update these paths to match your local HDF installation
hdf5.lib.dir=/usr/lib/x86_64-linux-gnu
hdf5.plugin.dir=/usr/lib/x86_64-linux-gnu/hdf5/plugins
hdf.lib.dir=/usr/lib/x86_64-linux-gnu
platform.hdf.lib=/usr/lib/x86_64-linux-gnu
ci.build=false
EOF
    echo -e "${YELLOW}Created build.properties - update paths if needed${NC}"
    echo
fi

echo "Step 1: Clean previous build"
echo "------------------------------"
mvn clean -B
echo

echo "Step 2: Compile everything"
echo "------------------------------"
mvn compile -B \
  -Dmaven.compiler.showDeprecation=false \
  -Dmaven.compiler.showWarnings=false
echo

echo "Step 3: Install modules (skip tests)"
echo "------------------------------"
# Install parent POM
mvn install -B -N -Ddependency-check.skip=true

# Install object and hdfview modules
mvn install -B -pl object,hdfview -DskipTests -Ddependency-check.skip=true
echo

echo "Step 4: Package application"
echo "------------------------------"
mvn package -DskipTests -B
echo

echo "Step 5: Verify build artifacts"
echo "------------------------------"
echo "JARs created:"
find . -name "*.jar" -type f -newer build.properties | head -10
echo

if [ ! -f "hdfview/target/hdfview-3.4-SNAPSHOT.jar" ]; then
    echo -e "${RED}ERROR: hdfview JAR not found!${NC}"
    echo "Expected: hdfview/target/hdfview-3.4-SNAPSHOT.jar"
    echo "Contents of hdfview/target:"
    ls -la hdfview/target/ || echo "Directory doesn't exist"
    exit 1
fi

echo -e "${GREEN}✓ hdfview JAR exists${NC}"
echo

echo "Step 6: Create jpackage App Image"
echo "------------------------------"
echo "Running: mvn verify -Pjpackage-app-image -pl object,hdfview"
mvn verify -Pjpackage-app-image -pl object,hdfview \
  -Dmaven.test.skip=true \
  -Djacoco.skip=true \
  -Dpmd.skip=true \
  -Ddependency-check.skip=true \
  -B
echo

echo "Step 7: Verify jpackage output"
echo "------------------------------"
if [ -d "hdfview/target/dist/HDFView" ]; then
    echo -e "${GREEN}✓ jpackage app-image created successfully${NC}"
    echo
    echo "Package details:"
    echo "  Size: $(du -sh hdfview/target/dist/HDFView | cut -f1)"
    echo "  Native libraries: $(find hdfview/target/dist/HDFView/lib/app -name "*.so" 2>/dev/null | wc -l) .so files"
    echo "  JAR files: $(find hdfview/target/dist/HDFView/lib/app -name "*.jar" 2>/dev/null | wc -l) JARs"
    echo "  HDF5 plugins: $(ls -1 hdfview/target/dist/HDFView/lib/app/plugin/*.so 2>/dev/null | wc -l) plugins"
    echo
    echo "To test the application:"
    echo "  ./hdfview/target/dist/HDFView/bin/HDFView"
else
    echo -e "${RED}✗ jpackage app-image creation failed${NC}"
    echo "Expected directory not found: hdfview/target/dist/HDFView"
    echo
    echo "Contents of hdfview/target/dist:"
    ls -la hdfview/target/dist/ 2>/dev/null || echo "Directory doesn't exist"
    exit 1
fi

echo
echo "========================================"
echo -e "${GREEN}SUCCESS: jpackage test completed${NC}"
echo "========================================"
