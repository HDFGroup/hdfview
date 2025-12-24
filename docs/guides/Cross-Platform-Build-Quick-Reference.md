# Cross-Platform Build Quick Reference

## Platform-Specific Build Commands

### Linux (Ubuntu)
```bash
# Download HDF libraries
gh release download snapshot --repo HDFGroup/hdf4 --pattern "hdf4-master-*-ubuntu-2404_gcc.tar.gz"
gh release download snapshot --repo HDFGroup/hdf5 --pattern "hdf5-develop-*-ubuntu-2404_gcc.tar.gz"

# Extract (nested structure)
tar -zxvf hdf4-master-*-ubuntu-2404_gcc.tar.gz
cd hdf4 && tar -zxvf HDF-*-Linux.tar.gz --strip-components 1

# Modify build.properties to populate hdf5.lib.dir, hdf5.plugin.dir,
# hdf.lib.dir, and platform.hdf.lib with paths to your local installations

# Build and install
mvn clean install -DskipTests -pl object,hdfview -B

# Generate JAR
mvn package -DskipTests=true -pl object,hdfview

# Run HDFView
./run-hdfview.sh
```

### Windows (PowerShell)
```powershell
# Download HDF libraries
gh release download snapshot --repo HDFGroup/hdf4 --pattern "hdf4-master-*-win-vs2022_cl.zip"
gh release download snapshot --repo HDFGroup/hdf5 --pattern "hdf5-develop-*-win-vs2022_cl.zip"

# Extract (flat structure)
7z x hdf4-master-*-win-vs2022_cl.zip
Set-Location hdf4
7z x HDF-*-win64.zip

# Modify build.properties to populate hdf5.lib.dir, hdf5.plugin.dir,
# hdf.lib.dir, and platform.hdf.lib with paths to your local installations

# Build and install
mvn clean install "-DskipTests" -pl object,hdfview -B

# Generate JAR
mvn package -DskipTests=true  -pl object,hdfview -B
```

### macOS (Bash)
```bash
# Download HDF libraries
gh release download snapshot --repo HDFGroup/hdf4 --pattern "hdf4-master-*-macos14_clang.tar.gz"
gh release download snapshot --repo HDFGroup/hdf5 --pattern "hdf5-develop-*-macos14_clang.tar.gz"

# Extract (nested structure)
tar -zxvf hdf4-master-*-macos14_clang.tar.gz
cd hdf4 && tar -zxvf HDF-*-Darwin.tar.gz --strip-components 1

# Modify build.properties to populate hdf5.lib.dir, hdf5.plugin.dir,
# hdf.lib.dir, and platform.hdf.lib with paths to your local installations

# Build and install
mvn clean install -DskipTests -pl object,hdfview -B

# Generate JAR
mvn package -pl object,hdfview -DskipTests=true  -B
```
