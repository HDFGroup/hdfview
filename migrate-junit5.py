#!/usr/bin/env python3
"""
JUnit 4 to JUnit 5 Migration Script
Automated migration without prompts
"""

import re
import sys
from pathlib import Path

def migrate_file(filepath):
    """Migrate a single JUnit 4 test file to JUnit 5"""
    print(f"Migrating: {filepath}")

    with open(filepath, 'r') as f:
        content = f.read()

    original_content = content

    # Import replacements
    replacements = {
        'import static org.junit.Assert.': 'import static org.junit.jupiter.api.Assertions.',
        'import org.junit.After;': 'import org.junit.jupiter.api.AfterEach;',
        'import org.junit.AfterClass;': 'import org.junit.jupiter.api.AfterAll;',
        'import org.junit.Before;': 'import org.junit.jupiter.api.BeforeEach;',
        'import org.junit.BeforeClass;': 'import org.junit.jupiter.api.BeforeAll;',
        'import org.junit.Test;': 'import org.junit.jupiter.api.Test;',
        'import org.junit.Ignore;': 'import org.junit.jupiter.api.Disabled;',
        'import org.junit.runner.RunWith;': '// import org.junit.runner.RunWith; // JUnit 5 - not needed',
        'import org.junit.runners.Suite;': '// import org.junit.runners.Suite; // JUnit 5 - use @Suite instead',
    }

    for old, new in replacements.items():
        content = content.replace(old, new)

    # Annotation replacements
    ann_replacements = {
        r'@BeforeClass\b': '@BeforeAll',
        r'@AfterClass\b': '@AfterAll',
        r'@Before\b': '@BeforeEach',
        r'@After\b': '@AfterEach',
        r'@Ignore\b': '@Disabled',
    }

    for old, new in ann_replacements.items():
        content = re.sub(old, new, content)

    # Add Tag import if not present and there are JUnit 5 imports
    if 'org.junit.jupiter.api' in content and 'import org.junit.jupiter.api.Tag;' not in content:
        # Find last JUnit 5 import
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if 'import org.junit.jupiter.api.' in line:
                last_junit_import = i
        # Insert Tag import after last JUnit import
        lines.insert(last_junit_import + 1, 'import org.junit.jupiter.api.Tag;')
        content = '\n'.join(lines)

    # Add @Tag annotations if not present
    if '@Tag(' not in content and 'public class ' in content:
        content = re.sub(
            r'(public class \w+)',
            r'@Tag("unit")\n@Tag("fast")\n\1',
            content
        )

    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"  ✓ Migrated successfully")
        return True
    else:
        print(f"  - No changes needed")
        return False

def main():
    # List of files to migrate
    base_dir = Path('/home/byrn/HDF_Projects/hdfview/dev')
    test_files = [
        'object/src/test/java/object/AttributeTest.java',
        'object/src/test/java/object/CompoundDSTest.java',
        'object/src/test/java/object/DataFormatTest.java',
        'object/src/test/java/object/DatatypeTest.java',
        'object/src/test/java/object/FileFormatTest.java',
        'object/src/test/java/object/H5BugFixTest.java',
        'object/src/test/java/object/H5CompoundDSTest.java',
        'object/src/test/java/object/H5DatatypeTest.java',
        'object/src/test/java/object/H5FileTest.java',
        'object/src/test/java/object/H5GroupTest.java',
        'object/src/test/java/object/H5ScalarDSTest.java',
        'object/src/test/java/object/HObjectTest.java',
        'object/src/test/java/object/ScalarDSTest.java',
    ]

    print("=" * 60)
    print("JUnit 5 Migration - Object Module Tests")
    print(f"Migrating {len(test_files)} files")
    print("=" * 60)
    print()

    migrated_count = 0
    for test_file in test_files:
        filepath = base_dir / test_file
        if filepath.exists():
            if migrate_file(filepath):
                migrated_count += 1
        else:
            print(f"WARNING: File not found: {filepath}")

    print()
    print("=" * 60)
    print(f"Migration Complete: {migrated_count}/{len(test_files)} files migrated")
    print("=" * 60)
    print()
    print("Next steps:")
    print("  1. Compile: mvn test-compile -pl object")
    print("  2. Fix assertion parameter order if needed")
    print("  3. Run tests: mvn test -pl object")

if __name__ == '__main__':
    main()
