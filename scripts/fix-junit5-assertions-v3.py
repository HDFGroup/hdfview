#!/usr/bin/env python3
"""
JUnit 5 assertion parameter order fixer v3.
Improved version with better handling of edge cases and single-file mode.

Usage:
  ./fix-junit5-assertions-v3.py <filename>           # Fix single file
  ./fix-junit5-assertions-v3.py --dry-run <filename> # Preview changes
  ./fix-junit5-assertions-v3.py --all                # Process all files
"""

import sys
import re
from pathlib import Path
from typing import List, Tuple, Optional


class AssertionFixer:
    """Fix JUnit 5 assertion parameter order."""

    def __init__(self, debug=False):
        self.debug = debug
        self.fixes_made = 0
        self.lines_processed = 0

    def log(self, msg):
        """Debug logging."""
        if self.debug:
            print(f"  DEBUG: {msg}")

    def process_file(self, filepath: Path, dry_run: bool = False) -> Tuple[int, bool]:
        """
        Process a single file.
        Returns (num_fixes, success)
        """
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        lines = content.split('\n')
        result = []
        i = 0
        fixes_in_file = 0

        while i < len(lines):
            line = lines[i]

            # Check if line contains assertTrue or assertFalse with likely wrong parameter order
            if self._is_assertion_to_fix(line):
                # Collect multi-line assertion if needed
                full_assertion, end_idx = self._collect_assertion(lines, i)

                # Try to fix it
                fixed = self._fix_assertion(full_assertion)

                if fixed:
                    result.extend(fixed)
                    fixes_in_file += 1
                    self.log(f"Fixed assertion at line {i+1}")
                else:
                    result.extend(full_assertion)
                    self.log(f"Could not fix assertion at line {i+1}, keeping original")

                i = end_idx + 1
            else:
                result.append(line)
                i += 1

        if fixes_in_file > 0:
            if not dry_run:
                # Write the fixed content
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write('\n'.join(result))

            return fixes_in_file, True

        return 0, True

    def _is_assertion_to_fix(self, line: str) -> bool:
        """Check if line is an assertion that might need fixing."""
        stripped = line.lstrip()

        # Must start with assertTrue( or assertFalse(
        if not (stripped.startswith('assertTrue(') or stripped.startswith('assertFalse(')):
            return False

        # Quick check: if first parameter looks like a string literal or constructWrongValueMessage,
        # it probably needs swapping
        # Extract first parameter (simple heuristic)
        match = re.match(r'^\s*assert(?:True|False)\(\s*(["\']|constructWrongValueMessage)', line)
        return match is not None

    def _collect_assertion(self, lines: List[str], start_idx: int) -> Tuple[List[str], int]:
        """Collect all lines of a multi-line assertion."""
        assertion_lines = [lines[start_idx]]
        idx = start_idx

        # Count parentheses to find the end
        paren_count = 0
        in_string = False
        escape = False

        for line in assertion_lines:
            for char in line:
                if escape:
                    escape = False
                    continue
                if char == '\\':
                    escape = True
                    continue
                if char == '"' and not escape:
                    in_string = not in_string
                if not in_string:
                    if char == '(':
                        paren_count += 1
                    elif char == ')':
                        paren_count -= 1

        # Keep collecting lines until parentheses are balanced
        while paren_count > 0 and idx + 1 < len(lines):
            idx += 1
            assertion_lines.append(lines[idx])

            for char in lines[idx]:
                if escape:
                    escape = False
                    continue
                if char == '\\':
                    escape = True
                    continue
                if char == '"' and not escape:
                    in_string = not in_string
                if not in_string:
                    if char == '(':
                        paren_count += 1
                    elif char == ')':
                        paren_count -= 1

        return assertion_lines, idx

    def _fix_assertion(self, assertion_lines: List[str]) -> Optional[List[str]]:
        """Fix assertion parameter order."""
        assertion_text = '\n'.join(assertion_lines)

        # Extract indent, method name, and parameters
        match = re.match(r'^(\s*)(assertTrue|assertFalse)\((.*)\);?\s*$', assertion_text, re.DOTALL)
        if not match:
            return None

        indent = match.group(1)
        method = match.group(2)
        params_text = match.group(3).strip()

        # Split parameters by top-level comma
        param1, param2 = self._split_parameters(params_text)
        if not param1 or not param2:
            return None

        # Check if param1 is a message (needs swapping)
        if not self._is_message(param1):
            # Already in correct order
            return None

        # Swap parameters
        # Format: method(condition, message);
        param1_clean = param1.strip()
        param2_clean = param2.strip()

        # Decide on formatting
        total_len = len(indent) + len(method) + len(param1_clean) + len(param2_clean) + 10

        if total_len <= 100 and '\n' not in param1_clean and '\n' not in param2_clean:
            # Single line
            return [f"{indent}{method}({param2_clean}, {param1_clean});"]
        else:
            # Multi-line
            return [
                f"{indent}{method}({param2_clean},",
                f"{indent}    {param1_clean});"
            ]

    def _split_parameters(self, params_text: str) -> Tuple[Optional[str], Optional[str]]:
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

    def _is_message(self, param: str) -> bool:
        """Check if parameter looks like a message (should be second parameter)."""
        param = param.strip()

        # String literal
        if param.startswith('"'):
            return True

        # constructWrongValueMessage call
        if param.startswith('constructWrongValueMessage('):
            return True

        # If it contains comparison operators, it's likely a condition
        if any(op in param for op in ['==', '!=', '.equals(', '.compareTo(', '>', '<', '&&', '||']):
            return False

        return False


def process_single_file(filepath: Path, dry_run: bool = False):
    """Process a single file and report results."""
    print(f"\n{'='*70}")
    print(f"Processing: {filepath.name}")
    print(f"Mode: {'DRY RUN' if dry_run else 'APPLY CHANGES'}")
    print(f"{'='*70}\n")

    fixer = AssertionFixer(debug=False)
    num_fixes, success = fixer.process_file(filepath, dry_run=dry_run)

    if num_fixes == 0:
        print(f"✓ No changes needed")
        return True, 0
    else:
        action = "Would fix" if dry_run else "Fixed"
        print(f"✓ {action} {num_fixes} assertion(s)")
        return True, num_fixes


def process_all_files(test_dir: Path, dry_run: bool = False):
    """Process all test files."""
    test_files = sorted(test_dir.glob("Test*.java"))

    print(f"\nFound {len(test_files)} test files")
    print(f"Mode: {'DRY RUN' if dry_run else 'APPLY CHANGES'}")
    print("="*70)

    total_changed = 0
    total_fixes = 0

    for test_file in test_files:
        fixer = AssertionFixer(debug=False)
        num_fixes, success = fixer.process_file(test_file, dry_run=dry_run)

        if num_fixes > 0:
            total_changed += 1
            total_fixes += num_fixes
            action = "Would fix" if dry_run else "Fixed"
            print(f"✓ {test_file.name}: {action} {num_fixes} assertions")
        else:
            print(f"  {test_file.name}: No changes needed")

    print("="*70)
    print(f"\nSummary:")
    print(f"  Files changed: {total_changed}/{len(test_files)}")
    print(f"  Total fixes: {total_fixes}")

    return total_fixes


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage:")
        print("  fix-junit5-assertions-v3.py <filename>           # Fix single file")
        print("  fix-junit5-assertions-v3.py --dry-run <filename> # Preview changes")
        print("  fix-junit5-assertions-v3.py --all                # Process all files")
        print("  fix-junit5-assertions-v3.py --all --dry-run      # Preview all files")
        sys.exit(1)

    test_dir = Path(__file__).parent.parent / "hdfview" / "src" / "test" / "java" / "uitest"

    if sys.argv[1] == '--all':
        dry_run = '--dry-run' in sys.argv
        process_all_files(test_dir, dry_run=dry_run)
    else:
        dry_run = sys.argv[1] == '--dry-run'
        filename = sys.argv[2] if dry_run else sys.argv[1]

        # Check if it's a full path or just a filename
        if '/' in filename:
            filepath = Path(filename)
        else:
            filepath = test_dir / filename

        if not filepath.exists():
            print(f"Error: File not found: {filepath}", file=sys.stderr)
            sys.exit(1)

        success, num_fixes = process_single_file(filepath, dry_run=dry_run)
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
