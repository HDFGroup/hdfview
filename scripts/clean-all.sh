#!/bin/bash
#
# clean-all.sh - Deep clean HDFView project to pristine state
#
# This script removes all build artifacts, compiled classes, cached dependencies,
# and Maven metadata to return the project to a fresh-from-repo state.
# Useful for resolving classloading issues and stale dependency problems.
#
# Usage: ./scripts/clean-all.sh

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "================================================"
echo "HDFView Deep Clean"
echo "================================================"
echo ""
echo "This will remove:"
echo "  - All Maven target/ directories"
echo "  - All compiled .class files"
echo "  - Maven local repository cache for hdfview artifacts"
echo "  - Maven build metadata"
echo "  - libs/ directory"
echo ""
read -p "Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
fi

echo ""
echo "Step 1: Cleaning Maven target directories..."
cd "$PROJECT_ROOT"
mvn clean -q || echo "Maven clean completed with warnings (expected if modules don't compile)"

echo ""
echo "Step 2: Removing any nested target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true

echo ""
echo "Step 3: Removing libs/ directory..."
rm -rf libs/
mkdir -p libs
echo "  Created empty libs/ directory"

echo ""
echo "Step 4: Cleaning Maven local repository cache..."
if [ -d "$HOME/.m2/repository/org/hdfgroup/hdfview" ]; then
    rm -rf "$HOME/.m2/repository/org/hdfgroup/hdfview"
    echo "  Removed hdfview artifacts from ~/.m2/repository"
else
    echo "  No hdfview artifacts found in ~/.m2/repository"
fi

echo ""
echo "Step 5: Removing Maven resolver status files..."
find "$HOME/.m2/repository" -name "*.lastUpdated" -delete 2>/dev/null || true
find "$HOME/.m2/repository" -name "_remote.repositories" -delete 2>/dev/null || true
echo "  Cleared Maven resolver cache"

echo ""
echo "Step 6: Checking for stale .class files..."
CLASS_FILES=$(find . -name "*.class" -type f 2>/dev/null | wc -l)
if [ "$CLASS_FILES" -gt 0 ]; then
    echo "  Warning: Found $CLASS_FILES .class files outside target/ directories"
    find . -name "*.class" -type f -delete
    echo "  Removed stale .class files"
else
    echo "  No stale .class files found"
fi

echo ""
echo "================================================"
echo "Deep clean complete!"
echo "================================================"
echo ""
echo "Project is now in a pristine state."
echo ""
echo "Next steps:"
echo "  1. Run ./scripts/build-dev.sh to compile and prepare for development"
echo "  2. Or run ./run-hdfview.sh which will auto-build if needed"
echo ""
