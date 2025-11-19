#!/usr/bin/env python3
"""
Fix JUnit 4 to JUnit 5 assertion parameter order
JUnit 4: assert*(message, value)
JUnit 5: assert*(value, message)
"""

import re
from pathlib import Path

def fix_assert_true_false(content):
    """Fix assertTrue and assertFalse parameter order"""
    # Pattern: assertTrue("message", condition) -> assertTrue(condition, "message")
    # Pattern: assertFalse("message", condition) -> assertFalse(condition, "message")

    patterns = [
        # assertTrue/assertFalse with string message first
        (r'(assertTrue|assertFalse)\s*\(\s*"([^"]+)"\s*,\s*([^)]+)\)',
         r'\1(\3, "\2")'),
        # assertTrue/assertFalse with multi-line
        (r'(assertTrue|assertFalse)\s*\(\s*"([^"]+)"\s*,\s*\n\s*([^)]+)\)',
         r'\1(\3,\n            "\2")'),
    ]

    for pattern, replacement in patterns:
        content = re.sub(pattern, replacement, content)

    return content

def fix_assert_null_not_null(content):
    """Fix assertNull and assertNotNull parameter order"""
    # Pattern: assertNull("message", object) -> assertNull(object, "message")
    # Pattern: assertNotNull("message", object) -> assertNotNull(object, "message")

    patterns = [
        (r'(assertNull|assertNotNull)\s*\(\s*"([^"]+)"\s*,\s*([^)]+)\)',
         r'\1(\3, "\2")'),
    ]

    for pattern, replacement in patterns:
        content = re.sub(pattern, replacement, content)

    return content

def fix_assert_equals(content):
    """Fix assertEquals parameter order"""
    # Pattern: assertEquals("message", expected, actual)
    # -> assertEquals(expected, actual, "message")

    # This is tricky because assertEquals has 3 parameters
    # We need to move the first parameter to the end

    # Simple pattern for common cases
    pattern = r'assertEquals\s*\(\s*"([^"]+)"\s*,\s*([^,]+),\s*([^)]+)\)'
    replacement = r'assertEquals(\2, \3, "\1")'
    content = re.sub(pattern, replacement, content)

    return content

def fix_assert_same(content):
    """Fix assertSame parameter order"""
    pattern = r'assertSame\s*\(\s*"([^"]+)"\s*,\s*([^,]+),\s*([^)]+)\)'
    replacement = r'assertSame(\2, \3, "\1")'
    content = re.sub(pattern, replacement, content)

    return content

def fix_file(filepath):
    """Fix assertion parameter order in a file"""
    print(f"Fixing: {filepath}")

    with open(filepath, 'r') as f:
        content = f.read()

    original = content

    # Apply fixes in sequence
    content = fix_assert_true_false(content)
    content = fix_assert_null_not_null(content)
    content = fix_assert_equals(content)
    content = fix_assert_same(content)

    if content != original:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"  ✓ Fixed assertion parameter order")
        return True
    else:
        print(f"  - No changes needed")
        return False

def main():
    base_dir = Path('/home/byrn/HDF_Projects/hdfview/dev')
    test_dir = base_dir / 'object/src/test/java/object'

    # Process all Java test files
    test_files = sorted(test_dir.glob('*Test.java'))

    print("=" * 60)
    print("Fixing JUnit Assertion Parameter Order")
    print(f"Processing {len(test_files)} files")
    print("=" * 60)
    print()

    fixed_count = 0
    for filepath in test_files:
        if fix_file(filepath):
            fixed_count += 1

    print()
    print("=" * 60)
    print(f"Complete: {fixed_count}/{len(test_files)} files fixed")
    print("=" * 60)
    print()
    print("Next: Recompile to verify fixes")
    print("  mvn test-compile -pl object")

if __name__ == '__main__':
    main()
