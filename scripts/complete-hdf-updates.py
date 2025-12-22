#!/usr/bin/env python3
"""Complete all HDF download pattern updates in maven-build.yml"""

import re

# Read file
with open('.github/workflows/maven-build.yml', 'r') as f:
    content = f.read()

# ===== LINUX PATTERNS =====
# Update remaining Linux HDF4 download (build-linux-app)
linux_hdf4_old = r'''        echo "Downloading HDF4: \$\{\{ inputs\.use_hdf \}\}"

        # Download HDF4 binary from HDF Group GitHub releases
        gh release download "\$\{\{ inputs\.use_hdf \}\}" \\
          --repo HDFGroup/hdf4 \\
          --pattern "\$\{\{ inputs\.use_hdf \}\}-ubuntu-2404_gcc\.tar\.gz"

        # Extract outer tar\.gz \(creates hdf4/ directory\)
        tar -zxvf "\$\{\{ inputs\.use_hdf \}\}-ubuntu-2404_gcc\.tar\.gz"'''

linux_hdf4_new = r'''        echo "Downloading HDF4 from release tag: ${{ inputs.use_hdf }}"

        # Determine file pattern based on whether base name is provided
        if [ -n "${{ inputs.use_hdf_name }}" ]; then
          PATTERN="${{ inputs.use_hdf_name }}-ubuntu-2404_gcc.tar.gz"
        else
          PATTERN="*-ubuntu-2404_gcc.tar.gz"
        fi
        echo "Using pattern: $PATTERN"

        # Download HDF4 binary from HDF Group GitHub releases
        gh release download "${{ inputs.use_hdf }}" \\
          --repo HDFGroup/hdf4 \\
          --pattern "$PATTERN" \\
          --clobber

        # Extract outer tar.gz (creates hdf4/ directory)
        tar -zxvf *-ubuntu-2404_gcc.tar.gz
        [ -d hdf4 ] || mv hdf4-* hdf4'''

content = re.sub(linux_hdf4_old, linux_hdf4_new, content, flags=re.MULTILINE)
print("✓ Updated Linux HDF4")

# ===== MACOS PATTERNS =====
# Update macOS HDF4 downloads
macos_hdf4_old = r'''          --pattern "\$\{\{ inputs\.use_hdf \}\}-macos14_clang\.tar\.gz"

        # Extract outer tar\.gz \(creates hdf4/ directory\)
        tar -zxvf "\$\{\{ inputs\.use_hdf \}\}-macos14_clang\.tar\.gz"'''

macos_hdf4_new = r'''          --pattern "$PATTERN" \\
          --clobber

        # Extract outer tar.gz (creates hdf4/ directory)
        tar -zxvf *-macos14_clang.tar.gz
        [ -d hdf4 ] || mv hdf4-* hdf4'''

# First add the pattern logic before the download
content = re.sub(
    r'''(echo "Downloading HDF4:.*\n\n        # Download HDF4 binary from HDF Group GitHub releases)''',
    r'''echo "Downloading HDF4 from release tag: ${{ inputs.use_hdf }}"\n\n        # Determine file pattern based on whether base name is provided\n        if [ -n "${{ inputs.use_hdf_name }}" ]; then\n          PATTERN="${{ inputs.use_hdf_name }}-macos14_clang.tar.gz"\n        else\n          PATTERN="*-macos14_clang.tar.gz"\n        fi\n        echo "Using pattern: $PATTERN"\n\n        # Download HDF4 binary from HDF Group GitHub releases''',
    content,
    flags=re.MULTILINE
)
content = re.sub(macos_hdf4_old, macos_hdf4_new, content, flags=re.MULTILINE)
print("✓ Updated macOS HDF4")

# Update macOS HDF5 downloads
macos_hdf5_old = r'''          --pattern "hdf5-\$\{\{ inputs\.use_hdf5 \}\}-macos14_clang\.tar\.gz"

        # Extract outer tar\.gz \(creates hdf5/ directory\)
        tar -zxvf "hdf5-\$\{\{ inputs\.use_hdf5 \}\}-macos14_clang\.tar\.gz"'''

macos_hdf5_new = r'''          --pattern "$PATTERN" \\
          --clobber

        # Extract outer tar.gz (creates hdf5/ directory)
        tar -zxvf hdf5-*-macos14_clang.tar.gz
        [ -d hdf5 ] || mv hdf5-* hdf5'''

content = re.sub(
    r'''(echo "Downloading HDF5 from release tag:.*\n\n.*if \[ -n.*use_hdf5_name.*\n.*PATTERN=.*ubuntu.*\n.*else\n.*PATTERN=.*hdf5.*ubuntu.*\n.*fi)''',
    lambda m: m.group(0).replace('ubuntu-2404_gcc', 'macos14_clang') if 'macos' in content[m.start():m.end()+500] else m.group(0),
    content
)
content = re.sub(macos_hdf5_old, macos_hdf5_new, content, flags=re.MULTILINE)
print("✓ Updated macOS HDF5")

# Write file
with open('.github/workflows/maven-build.yml', 'w') as f:
    f.write(content)

print("\n✅ All bash-based (Linux/macOS) patterns updated!")
print("⚠️  Windows PowerShell patterns need manual update")
