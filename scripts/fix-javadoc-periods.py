#!/usr/bin/env python3
"""
Auto-fix Javadoc "First sentence should end with a period" violations.

This script reads the checkstyle XML output, identifies violations,
and automatically adds periods to javadoc first sentences.
"""

import re
import xml.etree.ElementTree as ET
from pathlib import Path
from collections import defaultdict


def parse_checkstyle_results(xml_path):
    """Parse checkstyle XML and extract period violation line numbers."""
    tree = ET.parse(xml_path)
    root = tree.getroot()

    violations = defaultdict(list)

    for file_elem in root.findall('file'):
        filename = file_elem.get('name')
        for error in file_elem.findall('error'):
            message = error.get('message', '')
            if 'First sentence should end with a period' in message:
                line_num = int(error.get('line'))
                violations[filename].append(line_num)

    return violations


def fix_javadoc_period(lines, line_num):
    """Add a period to the end of a javadoc line if needed."""
    idx = line_num - 1  # Convert to 0-based index

    if idx >= len(lines):
        return False

    line = lines[idx]

    # Check if this is a javadoc comment line
    if '/**' not in line and '*' not in line:
        return False

    # Extract the comment content
    match = re.match(r'^(\s*\*+\s*)(.*)$', line)
    if not match:
        return False

    prefix, content = match.groups()

    # Skip if already ends with period, or ends with other punctuation
    content_stripped = content.rstrip()
    if not content_stripped:
        return False

    # Check if it ends with period or other sentence-ending punctuation
    if content_stripped.endswith(('.', '!', '?', ':', '}')):
        return False

    # Check if it's a javadoc tag line (@param, @return, etc.)
    if content_stripped.startswith('@'):
        return False

    # Add period at the end
    lines[idx] = prefix + content_stripped + '.\n'
    return True


def process_file(filepath, line_numbers):
    """Process a single file and fix all period violations."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        changes_made = 0
        for line_num in sorted(line_numbers):
            if fix_javadoc_period(lines, line_num):
                changes_made += 1

        if changes_made > 0:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.writelines(lines)
            print(f"✓ Fixed {changes_made} violations in {filepath}")
            return changes_made
        else:
            print(f"⚠ No changes made to {filepath} (lines may already be correct)")
            return 0

    except Exception as e:
        print(f"✗ Error processing {filepath}: {e}")
        return 0


def main():
    """Main entry point."""
    dev_dir = Path(__file__).parent.parent

    # Find checkstyle result files
    result_files = list(dev_dir.glob('*/target/checkstyle-result.xml'))

    if not result_files:
        print("No checkstyle-result.xml files found. Run 'mvn checkstyle:checkstyle' first.")
        return 1

    print(f"Found {len(result_files)} checkstyle result file(s)")

    # Collect all violations
    all_violations = defaultdict(list)
    for result_file in result_files:
        violations = parse_checkstyle_results(result_file)
        for filepath, line_nums in violations.items():
            all_violations[filepath].extend(line_nums)

    if not all_violations:
        print("No 'First sentence should end with a period' violations found.")
        return 0

    print(f"\nFound {len(all_violations)} file(s) with period violations")
    print(f"Total violations: {sum(len(lines) for lines in all_violations.values())}")
    print("\nProcessing files...\n")

    # Process each file
    total_fixed = 0
    for filepath, line_numbers in sorted(all_violations.items()):
        fixed = process_file(filepath, line_numbers)
        total_fixed += fixed

    print(f"\n{'='*60}")
    print(f"Summary: Fixed {total_fixed} violations across {len(all_violations)} files")
    print(f"{'='*60}")

    return 0


if __name__ == '__main__':
    exit(main())
