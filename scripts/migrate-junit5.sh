#!/bin/bash

# JUnit 5 Migration Script for HDFView
# Automates the basic import and annotation migration from JUnit 4 to JUnit 5

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

usage() {
    echo "Usage: $0 [OPTIONS] <test-file>"
    echo "Options:"
    echo "  -d, --dry-run    Show what would be changed without modifying files"
    echo "  -b, --backup     Create backup files (.bak)"
    echo "  -h, --help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --dry-run GroupTest.java"
    echo "  $0 --backup AttributeTest.java"
    echo "  $0 object/src/test/java/object/DatatypeTest.java"
}

# Default options
DRY_RUN=false
BACKUP=false
TEST_FILE=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -b|--backup)
            BACKUP=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            print_error "Unknown option $1"
            usage
            exit 1
            ;;
        *)
            TEST_FILE="$1"
            shift
            ;;
    esac
done

if [[ -z "$TEST_FILE" ]]; then
    print_error "Test file not specified"
    usage
    exit 1
fi

# Check if file exists
if [[ ! -f "$TEST_FILE" ]]; then
    print_error "File not found: $TEST_FILE"
    exit 1
fi

# Check if file is already migrated
if grep -q "org.junit.jupiter" "$TEST_FILE"; then
    print_warning "File appears to already be migrated to JUnit 5: $TEST_FILE"
    read -p "Continue anyway? [y/N]: " continue_anyway
    if [[ ! "$continue_anyway" =~ ^[Yy]$ ]]; then
        exit 0
    fi
fi

print_status "Migrating JUnit 4 test to JUnit 5: $TEST_FILE"

# Create backup if requested
if [[ "$BACKUP" == true ]]; then
    cp "$TEST_FILE" "$TEST_FILE.bak"
    print_success "Created backup: $TEST_FILE.bak"
fi

# Define migration patterns
declare -A IMPORT_REPLACEMENTS=(
    ["import static org.junit.Assert."]="import static org.junit.jupiter.api.Assertions."
    ["import org.junit.After;"]="import org.junit.jupiter.api.AfterEach;"
    ["import org.junit.AfterClass;"]="import org.junit.jupiter.api.AfterAll;"
    ["import org.junit.Before;"]="import org.junit.jupiter.api.BeforeEach;"
    ["import org.junit.BeforeClass;"]="import org.junit.jupiter.api.BeforeAll;"
    ["import org.junit.Test;"]="import org.junit.jupiter.api.Test;"
    ["import org.junit.runner.RunWith;"]="// import org.junit.runner.RunWith; // JUnit 5 - not needed"
    ["import org.junit.runners.Suite;"]="// import org.junit.runners.Suite; // JUnit 5 - use @Suite instead"
)

declare -A ANNOTATION_REPLACEMENTS=(
    ["@BeforeClass"]="@BeforeAll"
    ["@AfterClass"]="@AfterAll"
    ["@Before"]="@BeforeEach"
    ["@After"]="@AfterEach"
    ["@RunWith(Suite.class)"]="// @RunWith(Suite.class) // JUnit 5 - use @Suite instead"
)

# Function to apply replacements
apply_migration() {
    local temp_file=$(mktemp)
    cp "$TEST_FILE" "$temp_file"

    # Add JUnit 5 Tag import if not present
    if ! grep -q "import org.junit.jupiter.api.Tag;" "$temp_file"; then
        # Find the last import line and add Tag import after it
        sed -i '/^import org\.junit\.jupiter\.api\./a import org.junit.jupiter.api.Tag;' "$temp_file"
    fi

    # Apply import replacements
    for old_import in "${!IMPORT_REPLACEMENTS[@]}"; do
        new_import="${IMPORT_REPLACEMENTS[$old_import]}"
        if grep -q "$old_import" "$temp_file"; then
            print_status "Replacing import: $old_import"
            sed -i "s|$old_import|$new_import|g" "$temp_file"
        fi
    done

    # Apply annotation replacements
    for old_annotation in "${!ANNOTATION_REPLACEMENTS[@]}"; do
        new_annotation="${ANNOTATION_REPLACEMENTS[$old_annotation]}"
        if grep -q "$old_annotation" "$temp_file"; then
            print_status "Replacing annotation: $old_annotation → $new_annotation"
            sed -i "s|$old_annotation|$new_annotation|g" "$temp_file"
        fi
    done

    # Add @Tag annotations to class declaration
    if ! grep -q "@Tag(" "$temp_file"; then
        # Find class declaration and add tags before it
        sed -i '/^public class /i @Tag("unit")\n@Tag("fast")' "$temp_file"
        print_status "Added @Tag annotations"
    fi

    # Remove @SuppressWarnings("deprecation") that's often related to JUnit 4
    sed -i '/@SuppressWarnings("deprecation")/d' "$temp_file"

    if [[ "$DRY_RUN" == true ]]; then
        print_status "Dry run - showing differences:"
        diff "$TEST_FILE" "$temp_file" || true
    else
        cp "$temp_file" "$TEST_FILE"
        print_success "Migration completed: $TEST_FILE"
    fi

    rm "$temp_file"
}

# Show what will be changed
print_status "Analyzing file: $TEST_FILE"

# Check for JUnit 4 patterns
junit4_patterns_found=0

for pattern in "${!IMPORT_REPLACEMENTS[@]}"; do
    if grep -q "$pattern" "$TEST_FILE"; then
        print_status "Found JUnit 4 pattern: $pattern"
        ((junit4_patterns_found++))
    fi
done

for pattern in "${!ANNOTATION_REPLACEMENTS[@]}"; do
    if grep -q "$pattern" "$TEST_FILE"; then
        print_status "Found JUnit 4 annotation: $pattern"
        ((junit4_patterns_found++))
    fi
done

if [[ $junit4_patterns_found -eq 0 ]]; then
    print_warning "No JUnit 4 patterns found in file"
    exit 0
fi

print_status "Found $junit4_patterns_found JUnit 4 patterns to migrate"

if [[ "$DRY_RUN" == false ]]; then
    read -p "Proceed with migration? [y/N]: " proceed
    if [[ ! "$proceed" =~ ^[Yy]$ ]]; then
        print_status "Migration cancelled"
        exit 0
    fi
fi

# Apply the migration
apply_migration

# Final recommendations
echo ""
print_success "Basic migration completed!"
echo ""
print_status "Manual review required for:"
echo "  - Complex assertion patterns"
echo "  - Test suite configurations"
echo "  - Custom test runners"
echo "  - Parameterized test opportunities"
echo "  - Nested test organization"
echo ""
print_status "Next steps:"
echo "  1. Review and compile: mvn test-compile -pl module"
echo "  2. Run tests: mvn test -Dtest=TestClassName"
echo "  3. Consider modern JUnit 5 features (see docs/JUnit5-Migration-Guide.md)"
echo "  4. Add @DisplayName annotations for better test reporting"
echo ""

if [[ "$BACKUP" == true ]]; then
    print_status "Original file backed up as: $TEST_FILE.bak"
fi