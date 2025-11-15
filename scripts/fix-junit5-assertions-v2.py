#!/usr/bin/env python3
"""
Fixed version: JUnit 5 assertion parameter order fixer.

This version is more conservative and only modifies actual assertTrue/assertFalse calls.
It will NOT touch other method calls like getText(), printStackTrace(), etc.

Key improvements:
1. Only processes lines that are clearly assertion calls
2. Preserves all other lines exactly as-is
3. Better multi-line handling
4. Thorough validation before making changes
"""

import sys
import re
from pathlib import Path
from typing import List, Tuple, Optional


class SafeAssertionFixer:
    """Safely fix assertion parameter order without breaking other code."""

    def __init__(self, debug=False):
        self.debug = debug
        self.fixes_made = 0
        self.lines_skipped = 0

    def log(self, msg):
        """Debug logging."""
        if self.debug:
            print(f"  DEBUG: {msg}")

    def is_assertion_line(self, line: str) -> bool:
        """
        Check if a line is definitely an assertion call.
        Must match: whitespace + (assertTrue|assertFalse) + (
        Must NOT match other methods like getText(, printStackTrace(, etc.
        """
        # Strip leading whitespace for checking
        stripped = line.lstrip()

        # Must start with assertTrue or assertFalse
        if not (stripped.startswith('assertTrue(') or stripped.startswith('assertFalse(')):
            return False

        # Double-check it's not a substring of something else
        # (though this shouldn't happen with our startswith check)
        return True

    def collect_full_assertion(self, lines: List[str], start_idx: int) -> Tuple[List[str], int]:
        """
        Collect all lines that make up a single assertion statement.
        Returns (assertion_lines, end_index)
        """
        assertion_lines = [lines[start_idx]]
        idx = start_idx + 1

        # Keep collecting until we find ");
        while idx < len(lines):
            assertion_lines.append(lines[idx])
            if lines[idx].rstrip().endswith(');'):
                break
            idx += 1

        return assertion_lines, idx

    def split_assertion_parameters(self, assertion_text: str) -> Optional[Tuple[str, str, str, str]]:
        """
        Split assertion into: (method_name, indent, param1, param2)
        Returns None if unable to parse safely.
        """
        # Extract method name and indent
        match = re.match(r'^(\s*)(assertTrue|assertFalse)\((.*)', assertion_text, re.DOTALL)
        if not match:
            return None

        indent = match.group(1)
        method = match.group(2)
        params_and_end = match.group(3)

        # Remove trailing );
        if not params_and_end.rstrip().endswith(');'):
            self.log(f"No closing ); found")
            return None

        params_text = params_and_end.rstrip()[:-2]  # Remove );

        # Split by top-level comma
        param1, param2 = self.split_by_top_level_comma(params_text)
        if param1 is None:
            return None

        return (method, indent, param1.strip(), param2.strip())

    def split_by_top_level_comma(self, text: str) -> Tuple[Optional[str], Optional[str]]:
        """
        Split by comma that's not inside parentheses or quotes.
        Returns (param1, param2) or (None, None) if can't split.
        """
        depth = 0
        in_string = False
        escape_next = False

        for i, char in enumerate(text):
            if escape_next:
                escape_next = False
                continue

            if char == '\\':
                escape_next = True
                continue

            if char == '"' and not in_string:
                in_string = True
            elif char == '"' and in_string:
                in_string = False
            elif not in_string:
                if char == '(':
                    depth += 1
                elif char == ')':
                    depth -= 1
                elif char == ',' and depth == 0:
                    # Found the splitting comma!
                    return text[:i], text[i+1:]

        return None, None

    def needs_parameter_swap(self, param1: str) -> bool:
        """
        Check if param1 looks like a message (needs to be swapped with param2).
        Messages typically start with " or are constructWrongValueMessage calls.
        """
        p1_stripped = param1.strip()

        # Check if it's a string literal
        if p1_stripped.startswith('"'):
            return True

        # Check if it's a constructWrongValueMessage call
        if p1_stripped.startswith('constructWrongValueMessage('):
            return True

        # If it looks like a boolean expression, it's probably already in correct order
        if '==' in p1_stripped or '.equals(' in p1_stripped or 'compareTo(' in p1_stripped:
            return False

        return False

    def fix_assertion(self, assertion_lines: List[str]) -> Optional[List[str]]:
        """
        Fix a single assertion by swapping parameters if needed.
        Returns fixed lines or None if no changes needed.
        """
        assertion_text = '\n'.join(assertion_lines)

        # Parse the assertion
        parsed = self.split_assertion_parameters(assertion_text)
        if parsed is None:
            self.log(f"Could not parse assertion")
            self.lines_skipped += 1
            return None

        method, indent, param1, param2 = parsed

        # Check if we need to swap
        if not self.needs_parameter_swap(param1):
            self.log(f"Parameters already in correct order")
            return None

        # Swap the parameters
        self.fixes_made += 1

        # Format the result - keep it reasonably compact
        # If both params are short, single line
        # Otherwise, multi-line with proper indentation
        if '\n' not in param1 and '\n' not in param2 and len(param1) + len(param2) < 80:
            # Single line
            return [f"{indent}{method}({param2}, {param1});"]
        else:
            # Multi-line - put condition first, message second
            # Clean up any existing multi-line formatting in params
            param2_clean = ' '.join(param2.split())
            param1_clean = ' '.join(param1.split())

            return [
                f"{indent}{method}({param2_clean},",
                f"{indent}           {param1_clean});"
            ]

    def process_file(self, content: str) -> Tuple[str, int]:
        """
        Process file content and return (fixed_content, num_fixes).
        """
        lines = content.split('\n')
        result = []
        i = 0

        while i < len(lines):
            line = lines[i]

            # Check if this is an assertion line
            if self.is_assertion_line(line):
                self.log(f"Found assertion at line {i+1}: {line[:60]}...")

                # Collect the full assertion
                assertion_lines, end_idx = self.collect_full_assertion(lines, i)

                # Try to fix it
                fixed_lines = self.fix_assertion(assertion_lines)

                if fixed_lines is not None:
                    # Use the fixed version
                    result.extend(fixed_lines)
                    self.log(f"Fixed assertion: {len(assertion_lines)} lines -> {len(fixed_lines)} lines")
                else:
                    # Keep original
                    result.extend(assertion_lines)

                # Skip past the assertion we just processed
                i = end_idx + 1
            else:
                # Not an assertion - keep as-is
                result.append(line)
                i += 1

        return '\n'.join(result), self.fixes_made


def test_single_file(test_file: Path, apply: bool = False) -> bool:
    """Test on a single file and optionally apply changes."""
    print(f"\n{'='*70}")
    print(f"Testing on: {test_file.name}")
    print(f"{'='*70}\n")

    with open(test_file, 'r', encoding='utf-8') as f:
        original = f.read()

    fixer = SafeAssertionFixer(debug=False)
    fixed, num_fixes = fixer.process_file(original)

    if num_fixes == 0:
        print(f"✓ No changes needed ({fixer.lines_skipped} assertions already correct)")
        return False
    else:
        print(f"✓ Would fix {num_fixes} assertions")

        # Show first few changes
        orig_lines = original.split('\n')
        fixed_lines = fixed.split('\n')

        print("\nFirst 2 changes:")
        shown = 0
        i = 0
        while i < min(len(orig_lines), len(fixed_lines)) and shown < 2:
            if orig_lines[i] != fixed_lines[i]:
                print(f"\nChange {shown + 1}:")
                # Show context
                for j in range(max(0, i-1), min(len(orig_lines), i+3)):
                    if j < len(fixed_lines):
                        if orig_lines[j] == fixed_lines[j]:
                            print(f"    {orig_lines[j]}")
                        else:
                            print(f"  - {orig_lines[j]}")
                            if j < len(fixed_lines):
                                print(f"  + {fixed_lines[j]}")
                shown += 1
            i += 1

        if apply:
            with open(test_file, 'w', encoding='utf-8') as f:
                f.write(fixed)
            print(f"\n✓ Applied changes to {test_file.name}")

        return True


def process_all_files(test_dir: Path, apply: bool = False):
    """Process all test files."""
    test_files = sorted(test_dir.glob("Test*.java"))

    print(f"\nFound {len(test_files)} test files")
    print(f"Mode: {'APPLY CHANGES' if apply else 'DRY RUN (--dry-run)'}")
    print("="*70)

    total_changed = 0
    total_fixes = 0

    for test_file in test_files:
        with open(test_file, 'r', encoding='utf-8') as f:
            original = f.read()

        fixer = SafeAssertionFixer(debug=False)
        fixed, num_fixes = fixer.process_file(original)

        if num_fixes > 0:
            total_changed += 1
            total_fixes += num_fixes

            if apply:
                with open(test_file, 'w', encoding='utf-8') as f:
                    f.write(fixed)
                print(f"✓ {test_file.name}: Fixed {num_fixes} assertions")
            else:
                print(f"  {test_file.name}: Would fix {num_fixes} assertions")
        else:
            print(f"  {test_file.name}: No changes needed")

    print("="*70)
    print(f"\nSummary:")
    print(f"  Files to change: {total_changed}/{len(test_files)}")
    print(f"  Total fixes: {total_fixes}")

    if not apply and total_fixes > 0:
        print(f"\n  Run with --apply to make changes")

    return total_fixes


def main():
    """Main entry point."""
    test_dir = Path(__file__).parent.parent / "hdfview" / "src" / "test" / "java" / "uitest"

    if not test_dir.exists():
        print(f"Error: Test directory not found: {test_dir}", file=sys.stderr)
        sys.exit(1)

    if len(sys.argv) > 1:
        if sys.argv[1] == '--test':
            # Test on a single file
            test_file = test_dir / "TestHDFViewLibBounds.java"
            test_single_file(test_file, apply=False)
        elif sys.argv[1] == '--test-apply':
            # Test and apply on a single file
            test_file = test_dir / "TestHDFViewLibBounds.java"
            if test_single_file(test_file, apply=False):
                response = input("\nApply changes to this file? (y/n): ")
                if response.lower() == 'y':
                    test_single_file(test_file, apply=True)
        elif sys.argv[1] == '--dry-run':
            # Dry run on all files
            process_all_files(test_dir, apply=False)
        elif sys.argv[1] == '--apply':
            # Apply to all files
            print("⚠️  This will modify all test files.")
            response = input("Continue? (y/n): ")
            if response.lower() == 'y':
                process_all_files(test_dir, apply=True)
        else:
            print("Usage: fix-junit5-assertions-v2.py [--test|--test-apply|--dry-run|--apply]")
            print("  --test: Test on single file (TestHDFViewLibBounds.java)")
            print("  --test-apply: Test and optionally apply on single file")
            print("  --dry-run: Show what would be changed across all files")
            print("  --apply: Apply changes to all files")
            sys.exit(1)
    else:
        # Default: dry run
        process_all_files(test_dir, apply=False)


if __name__ == "__main__":
    main()
