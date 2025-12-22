# Fix remaining Linux HDF4 pattern (build-linux-app)
/echo "Downloading HDF4: \${{ inputs.use_hdf }}"/,/tar -zxvf "\${{ inputs.use_hdf }}-ubuntu-2404_gcc.tar.gz"/ {
    s/echo "Downloading HDF4: \${{ inputs.use_hdf }}"/echo "Downloading HDF4 from release tag: ${{ inputs.use_hdf }}"\n\n        # Determine file pattern based on whether base name is provided\n        if [ -n "${{ inputs.use_hdf_name }}" ]; then\n          PATTERN="${{ inputs.use_hdf_name }}-ubuntu-2404_gcc.tar.gz"\n        else\n          PATTERN="*-ubuntu-2404_gcc.tar.gz"\n        fi\n        echo "Using pattern: $PATTERN"/
    s|gh release download "\${{ inputs.use_hdf }}" \\|gh release download "${{ inputs.use_hdf }}" \\|
    s|--pattern "\${{ inputs.use_hdf }}-ubuntu-2404_gcc.tar.gz"|--pattern "$PATTERN" \\\n          --clobber|
    s|tar -zxvf "\${{ inputs.use_hdf }}-ubuntu-2404_gcc.tar.gz"|tar -zxvf *-ubuntu-2404_gcc.tar.gz\n        [ -d hdf4 ] || mv hdf4-* hdf4|
}
