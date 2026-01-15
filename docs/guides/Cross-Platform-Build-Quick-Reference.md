# Cross-Platform Build Quick Reference

## Platform-Specific Build Commands

Before following these steps to build HDFView from source, you will need to have the following installed on your machine:
- JDK 21
- Git + gh
- Maven

### Linux (Ubuntu)
```bash
# Download HDF libraries
gh release download hdf4.3.1 --repo HDFGroup/hdf4 --pattern "hdf4.3.1-ubuntu-2404_gcc.tar.gz"
gh release download 2.0.0 --repo HDFGroup/hdf5 --pattern "hdf5-2.0.0-ubuntu-2404_gcc.tar.gz"

# Extract (nested structure)
tar -zxvf hdf4.3.1-ubuntu-2404_gcc.tar.gz
cd hdf4 && tar -zxvf HDF-*-Linux.tar.gz --strip-components 1
cd ..
tar -zxvf hdf5-2.0.0-ubuntu-2404_gcc.tar.gz
cd hdf5 && tar -zxvf HDF5-*-Linux.tar.gz

# Modify build.properties to populate hdf5.lib.dir, hdf5.plugin.dir,
# hdf.lib.dir, and platform.hdf.lib with paths to your local installations

# Install repository module first to set up hdf4/5 from local directories
mvn clean install -DskipTests -pl repository -B
# Install HDFView
mvn clean install -DskipTests -B

# Generate JAR
mvn package -DskipTests

# Run HDFView
./run-hdfview.sh
```

### Windows (PowerShell)
```powershell
# Download HDF libraries
gh release download hdf4.3.1 --repo HDFGroup/hdf4 --pattern "hdf4.3.1-win-vs2022_cl.zip"
gh release download 2.0.0 --repo HDFGroup/hdf5 --pattern "hdf5-2.0.0-win-vs2022_cl.zip"

# Extract (flat structure)
7z x hdf4.3.1-win-vs2022_cl.zip
cd hdf4
7z x HDF-*-win64.zip
cd ..
7z x hdf5-2.0.0-win-vs2022_cl.zip
cd hdf5
7z x HDF5-*-win64.zip

# Modify build.properties to populate hdf5.lib.dir, hdf5.plugin.dir,
# hdf.lib.dir, and platform.hdf.lib with paths to your local installations

# Build and install
# Install repository module first to set up hdf4/5 from local directories
mvn clean install -DskipTests -pl repository -B
# Install HDFView
mvn clean install -DskipTests -B

# Generate JAR
mvn package -DskipTests

# Run HDFView
run-hdfview.bat
```

### macOS (Bash)
```bash
# Download HDF libraries
gh release download hdf4.3.1 --repo HDFGroup/hdf4 --pattern "hdf4.3.1-macos14_clang.tar.gz"
gh release download 2.0.0 --repo HDFGroup/hdf5 --pattern "hdf5-2.0.0-macos14_clang.tar.gz"

# Extract (nested structure)
tar -zxvf hdf4.3.1-macos14_clang.tar.gz
cd hdf4 && tar -zxvf HDF-*-Darwin.tar.gz --strip-components 1
cd ..
tar -zxvf hdf5-2.0.0-macos14_clang.tar.gz
cd hdf5 && tar -zxvf HDF5-*-Darwin.tar.gz

# Modify build.properties to populate hdf5.lib.dir, hdf5.plugin.dir,
# hdf.lib.dir, and platform.hdf.lib with paths to your local installations

# Build and install
# Install repository module first to set up hdf4/5 from local directories
mvn clean install -DskipTests -pl repository -B
# Install HDFView
mvn clean install -DskipTests -B

# Generate JAR
mvn package -DskipTests

# Run HDFView
./run-hdfview.sh
```
