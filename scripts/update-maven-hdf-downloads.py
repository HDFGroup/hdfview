#!/usr/bin/env python3
"""
Update HDF download patterns in maven-build.yml to use new input parameters.
Replaces hardcoded patterns with conditional logic based on use_hdf_name/use_hdf5_name.
"""

import re
import sys

def update_linux_hdf5(content):
    """Update Linux HDF5 download pattern"""
    old_pattern = r'''    - name: Download and Install HDF5 from GitHub
      run: \|
        echo "Downloading HDF5: \$\{\{ inputs\.use_hdf5 \}\}"

        # Download HDF5 binary from HDF Group GitHub releases
        gh release download "\$\{\{ inputs\.use_hdf5 \}\}" \\
          --repo HDFGroup/hdf5 \\
          --pattern "hdf5-\$\{\{ inputs\.use_hdf5 \}\}-ubuntu-2404_gcc\.tar\.gz"

        # Extract outer tar\.gz \(creates hdf5/ directory\)
        tar -zxvf "hdf5-\$\{\{ inputs\.use_hdf5 \}\}-ubuntu-2404_gcc\.tar\.gz"'''

    new_pattern = r'''    - name: Download and Install HDF5 from GitHub
      run: |
        echo "Downloading HDF5 from release tag: ${{ inputs.use_hdf5 }}"

        # Determine file pattern based on whether base name is provided
        if [ -n "${{ inputs.use_hdf5_name }}" ]; then
          PATTERN="${{ inputs.use_hdf5_name }}-ubuntu-2404_gcc.tar.gz"
        else
          PATTERN="hdf5-*-ubuntu-2404_gcc.tar.gz"
        fi
        echo "Using pattern: $PATTERN"

        # Download HDF5 binary from HDF Group GitHub releases
        gh release download "${{ inputs.use_hdf5 }}" \\
          --repo HDFGroup/hdf5 \\
          --pattern "$PATTERN" \\
          --clobber

        # Extract outer tar.gz (creates hdf5/ directory)
        tar -zxvf hdf5-*-ubuntu-2404_gcc.tar.gz
        [ -d hdf5 ] || mv hdf5-* hdf5'''

    return re.sub(old_pattern, new_pattern, content, count=1)

# Read the file
with open('.github/workflows/maven-build.yml', 'r') as f:
    content = f.read()

# Apply transformations
print("Updating Linux HDF5 download...")
content = update_linux_hdf5(content)

# Write back
with open('.github/workflows/maven-build.yml', 'w') as f:
    f.write(content)

print("✓ Updates applied")
