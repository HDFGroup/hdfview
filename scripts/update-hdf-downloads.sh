#!/bin/bash
# Helper script to update maven-build.yml HDF download patterns
# This creates the updated download logic for all platforms

cat << 'EOF'
LINUX HDF4 DOWNLOAD:
----------------
    - name: Download and Install HDF4 from GitHub
      run: |
        echo "Downloading HDF4 from release tag: ${{ inputs.use_hdf }}"

        # Determine file pattern based on whether base name is provided
        if [ -n "${{ inputs.use_hdf_name }}" ]; then
          PATTERN="${{ inputs.use_hdf_name }}-ubuntu-2404_gcc.tar.gz"
        else
          PATTERN="*-ubuntu-2404_gcc.tar.gz"
        fi
        echo "Using pattern: $PATTERN"

        gh release download "${{ inputs.use_hdf }}" \
          --repo HDFGroup/hdf4 \
          --pattern "$PATTERN" \
          --clobber

        tar -zxvf *-ubuntu-2404_gcc.tar.gz
        [ -d hdf4 ] || mv hdf4-* hdf4
        cd hdf4
        tar -zxvf HDF-*-Linux.tar.gz --strip-components 1

        HDF4DIR=${{ github.workspace }}/hdf4/HDF_Group/HDF/
        FILE_NAME_HDF=$(ls $HDF4DIR)
        echo "HDF4LIB_PATH=$HDF4DIR$FILE_NAME_HDF" >> $GITHUB_ENV
      env:
        GH_TOKEN: ${{ github.token }}

LINUX HDF5 DOWNLOAD:
----------------
    - name: Download and Install HDF5 from GitHub
      run: |
        echo "Downloading HDF5 from release tag: ${{ inputs.use_hdf5 }}"

        if [ -n "${{ inputs.use_hdf5_name }}" ]; then
          PATTERN="${{ inputs.use_hdf5_name }}-ubuntu-2404_gcc.tar.gz"
        else
          PATTERN="hdf5-*-ubuntu-2404_gcc.tar.gz"
        fi
        echo "Using pattern: $PATTERN"

        gh release download "${{ inputs.use_hdf5 }}" \
          --repo HDFGroup/hdf5 \
          --pattern "$PATTERN" \
          --clobber

        tar -zxvf hdf5-*-ubuntu-2404_gcc.tar.gz
        [ -d hdf5 ] || mv hdf5-* hdf5
        cd hdf5
        tar -zxvf HDF5-*-Linux.tar.gz --strip-components 1

        HDF5DIR=${{ github.workspace }}/hdf5/HDF_Group/HDF5/
        FILE_NAME_HDF5=$(ls $HDF5DIR)
        echo "HDF5LIB_PATH=$HDF5DIR$FILE_NAME_HDF5" >> $GITHUB_ENV
      env:
        GH_TOKEN: ${{ github.token }}

WINDOWS HDF4 DOWNLOAD:
----------------
    - name: Download and Install HDF4 from GitHub
      shell: pwsh
      run: |
        Write-Host "Downloading HDF4 from release tag: ${{ inputs.use_hdf }}"

        if ("${{ inputs.use_hdf_name }}") {
          $PATTERN = "${{ inputs.use_hdf_name }}-win-vs2022_cl.zip"
        } else {
          $PATTERN = "*-win-vs2022_cl.zip"
        }
        Write-Host "Using pattern: $PATTERN"

        gh release download "${{ inputs.use_hdf }}" --repo HDFGroup/hdf4 --pattern "$PATTERN" --clobber
        7z x *-win-vs2022_cl.zip
        Set-Location hdf4
        7z x HDF-*-win64.zip

        $HDF4DIR = Get-ChildItem -Path . -Filter "HDF-*-win64" -Directory | Select-Object -First 1
        if (-not $HDF4DIR) {
          Write-Error "HDF-*-win64 directory not found!"
          exit 1
        }
        echo "HDF4LIB_PATH=$($HDF4DIR.FullName)" >> $env:GITHUB_ENV
      env:
        GH_TOKEN: ${{ github.token }}

MACOS HDF4 DOWNLOAD:
----------------
    - name: Download and Install HDF4 from GitHub
      run: |
        echo "Downloading HDF4 from release tag: ${{ inputs.use_hdf }}"

        if [ -n "${{ inputs.use_hdf_name }}" ]; then
          PATTERN="${{ inputs.use_hdf_name }}-macos14_clang.tar.gz"
        else
          PATTERN="*-macos14_clang.tar.gz"
        fi
        echo "Using pattern: $PATTERN"

        gh release download "${{ inputs.use_hdf }}" \
          --repo HDFGroup/hdf4 \
          --pattern "$PATTERN" \
          --clobber

        tar -zxvf *-macos14_clang.tar.gz
        [ -d hdf4 ] || mv hdf4-* hdf4
        cd hdf4
        tar -zxvf HDF-*-Darwin.tar.gz --strip-components 1

        HDF4DIR=${{ github.workspace }}/hdf4/HDF_Group/HDF/
        FILE_NAME=$(ls $HDF4DIR)
        echo "HDF4LIB_PATH=$HDF4DIR$FILE_NAME" >> $GITHUB_ENV
      env:
        GH_TOKEN: ${{ github.token }}
EOF
