#!/bin/bash
# install-swtbot.sh
# Install SWTBot dependencies to local Maven repository
#
# SWTBot is not available on Maven Central, so we download the JARs from
# Eclipse P2 repository and install them to the local Maven repository.
#
# Usage: ./scripts/install-swtbot.sh

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}SWTBot Local Maven Installation${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# Define version
SWTBOT_VERSION="4.3.0"
SWTBOT_FULL_VERSION="4.3.0.202506021445"

# Define paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SWTBOT_LIB_DIR="$PROJECT_ROOT/lib/swtbot"

# Check if JARs exist
if [ ! -f "$SWTBOT_LIB_DIR/org.eclipse.swtbot.swt.finder_${SWTBOT_FULL_VERSION}.jar" ]; then
    echo -e "${RED}Error: SWTBot JARs not found in $SWTBOT_LIB_DIR${NC}"
    echo "Please run the download script first or download manually from:"
    echo "http://download.eclipse.org/technology/swtbot/releases/latest/plugins/"
    exit 1
fi

echo -e "${GREEN}Found SWTBot JARs in $SWTBOT_LIB_DIR${NC}"
echo

# Install SWTBot Core Finder
echo -e "${BLUE}Installing org.eclipse.swtbot.swt.finder ${SWTBOT_VERSION}...${NC}"
mvn install:install-file \
    -Dfile="$SWTBOT_LIB_DIR/org.eclipse.swtbot.swt.finder_${SWTBOT_FULL_VERSION}.jar" \
    -DgroupId=org.eclipse.swtbot \
    -DartifactId=org.eclipse.swtbot.swt.finder \
    -Dversion=${SWTBOT_VERSION} \
    -Dpackaging=jar \
    -DgeneratePom=true

echo

# Install SWTBot NatTable Finder
echo -e "${BLUE}Installing org.eclipse.swtbot.nebula.nattable.finder ${SWTBOT_VERSION}...${NC}"
mvn install:install-file \
    -Dfile="$SWTBOT_LIB_DIR/org.eclipse.swtbot.nebula.nattable.finder_${SWTBOT_FULL_VERSION}.jar" \
    -DgroupId=org.eclipse.swtbot \
    -DartifactId=org.eclipse.swtbot.nebula.nattable.finder \
    -Dversion=${SWTBOT_VERSION} \
    -Dpackaging=jar \
    -DgeneratePom=true

echo
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}SWTBot Installation Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo
echo "Installed artifacts:"
echo "  - org.eclipse.swtbot:org.eclipse.swtbot.swt.finder:${SWTBOT_VERSION}"
echo "  - org.eclipse.swtbot:org.eclipse.swtbot.nebula.nattable.finder:${SWTBOT_VERSION}"
echo
echo "You can now use these dependencies in your pom.xml with:"
echo "  <groupId>org.eclipse.swtbot</groupId>"
echo "  <artifactId>org.eclipse.swtbot.swt.finder</artifactId>"
echo "  <version>${SWTBOT_VERSION}</version>"
echo
echo "Next steps:"
echo "  1. Add dependencies to hdfview/pom.xml"
echo "  2. Run: mvn clean test-compile -pl hdfview"
echo "  3. Run a test: mvn test -pl hdfview -Dtest=TestHDFViewMenu"
echo
