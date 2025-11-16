#!/usr/bin/env python3
"""
JUnit 5 assertion parameter order fixer v4.
Handles multi-line assertions where assertTrue( is on its own line.
"""

import sys
import re
from pathlib import Path
from typing import List, Tuple, Optional


def process_file(filepath: Path, dry_run: bool = False) -> int:
    """Process a single file and return number of fixes."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    result = []
    i = 0
    fixes = 0

    while i < len(lines):
        line = lines[i]

        # Check if this line has an assertion
        if re.search(r'\bassertTrue\s*\(', line) or re.search(r'\bassertFalse\s*\(', line):
            # Collect the full assertion (may span multiple lines)
            assertion_lines, end_idx = collect_assertion(lines, i)

            # Try to fix it
            fixed_lines = fix_assertion(assertion_lines)

            if fixed_lines:
                result.extend(fixed_lines)
                fixes += 1
            else:
                result.extend(assertion_lines)

            i = end_idx + 1
        else:
            result.append(line)
            i += 1

    if fixes > 0 and not dry_run:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(result))

    return fixes


def collect_assertion(lines: List[str], start_idx: int) -> Tuple[List[str], int]:
    """Collect all lines of an assertion statement."""
    assertion_lines = [lines[start_idx]]
    idx = start_idx

    # Count parentheses to find where assertion ends
    paren_count = 0
    in_string = False
    escape = False

    for line_idx in range(start_idx, len(lines)):
        line = lines[line_idx]

        for char in line:
            if escape:
                escape = False
                continue
            if char == '\\':
                escape = True
                continue
            if char == '"':
                in_string = not in_string
            elif not in_string:
                if char == '(':
                    paren_count += 1
                elif char == ')':
                    paren_count -= 1

        if paren_count == 0 and line_idx > start_idx:
            idx = line_idx
            if line_idx > start_idx:
                assertion_lines = lines[start_idx:line_idx+1]
            break

    return assertion_lines, idx


def fix_assertion(assertion_lines: List[str]) -> Optional[List[str]]:
    """Fix assertion parameter order if needed."""
    # Join all lines to analyze
    full_text = '\n'.join(assertion_lines)

    # Extract the assertion
    match = re.match(r'^(\s*)(assertTrue|assertFalse)\s*\((.*)\);?\s*$', full_text, re.DOTALL)
    if not match:
        return None

    indent = match.group(1)
    method = match.group(2)
    params_text = match.group(3).strip()

    # Split parameters
    param1, param2 = split_parameters(params_text)
    if not param1 or not param2:
        return None

    # Check if we need to swap (param1 is a message/string)
    if not is_message(param1):
        # Already correct order
        return None

    # Swap parameters: method(condition, message);
    param1_clean = ' '.join(param1.split())  # Normalize whitespace
    param2_clean = ' '.join(param2.split())

    # Format based on length
    total_len = len(indent) + len(method) + len(param1_clean) + len(param2_clean)

    if total_len <= 100:
        # Single line
        return [f"{indent}{method}({param2_clean}, {param1_clean});"]
    else:
        # Multi-line with nice formatting
        return [
            f"{indent}{method}({param2_clean},",
            f"{indent}    {param1_clean});"
        ]


def split_parameters(params_text: str) -> Tuple[Optional[str], Optional[str]]:
    """Split parameters by top-level comma."""
    depth = 0
    in_string = False
    escape = False

    for i, char in enumerate(params_text):
        if escape:
            escape = False
            continue
        if char == '\\':
            escape = True
            continue
        if char == '"':
            in_string = not in_string
        elif not in_string:
            if char == '(':
                depth += 1
            elif char == ')':
                depth -= 1
            elif char == ',' and depth == 0:
                return params_text[:i].strip(), params_text[i+1:].strip()

    return None, None


def is_message(param: str) -> bool:
    """Check if parameter is a message (should be second parameter)."""
    param = param.strip()

    # String literal
    if param.startswith('"'):
        return True

    # constructWrongValueMessage call
    if 'constructWrongValueMessage' in param:
        return True

    # Has comparison operators - probably a condition
    if any(op in param for op in ['==', '!=', '.equals(', '.compareTo(', '>=', '<=', '>', '<']):
        return False

    return False


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage: fix-junit5-assertions-v4.py [--dry-run] <filename>")
        sys.exit(1)

    dry_run = '--dry-run' in sys.argv
    filename = sys.argv[2] if dry_run else sys.argv[1]

    test_dir = Path(__file__).parent.parent / "hdfview" / "src" / "test" / "java" / "uitest"

    if '/' in filename:
        filepath = Path(filename)
    else:
        filepath = test_dir / filename

    if not filepath.exists():
        print(f"Error: File not found: {filepath}")
        sys.exit(1)

    fixes = process_file(filepath, dry_run=dry_run)

    if fixes > 0:
        action = "Would fix" if dry_run else "Fixed"
        print(f"{action} {fixes} assertion(s) in {filepath.name}")
    else:
        print(f"No changes needed in {filepath.name}")


if __name__ == "__main__":
    main()
