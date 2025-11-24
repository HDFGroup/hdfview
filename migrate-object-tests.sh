#!/bin/bash
# Batch migrate all object module JUnit 4 tests to JUnit 5

cd /home/byrn/HDF_Projects/hdfview/dev

# List of test files to migrate (excluding the 2 already working)
TEST_FILES=(
    "object/src/test/java/object/AllH5ObjectTests.java"
    "object/src/test/java/object/AttributeTest.java"
    "object/src/test/java/object/CompoundDSTest.java"
    "object/src/test/java/object/DataFormatTest.java"
    "object/src/test/java/object/DatatypeTest.java"
    "object/src/test/java/object/FileFormatTest.java"
    "object/src/test/java/object/H5BugFixTest.java"
    "object/src/test/java/object/H5CompoundDSTest.java"
    "object/src/test/java/object/H5DatatypeTest.java"
    "object/src/test/java/object/H5FileTest.java"
    "object/src/test/java/object/H5GroupTest.java"
    "object/src/test/java/object/H5ScalarDSTest.java"
    "object/src/test/java/object/HObjectTest.java"
    "object/src/test/java/object/ScalarDSTest.java"
)

echo "======================================"
echo "Object Module JUnit 5 Migration"
echo "Migrating ${#TEST_FILES[@]} test files"
echo "======================================"
echo ""

for test_file in "${TEST_FILES[@]}"; do
    echo "----------------------------------------"
    echo "Migrating: $test_file"
    echo "----------------------------------------"
    # Run migration script non-interactively
    yes | scripts/migrate-junit5.sh "$test_file" 2>&1 | grep -E "(INFO|OK|WARN|ERROR|Found JUnit|patterns to migrate)"
    echo ""
done

echo "======================================"
echo "Migration Complete!"
echo "======================================"
echo ""
echo "Next steps:"
echo "  1. Check for compilation errors: mvn test-compile -pl object"
echo "  2. Fix assertion parameter order (if needed)"
echo "  3. Run tests: mvn test -pl object"
