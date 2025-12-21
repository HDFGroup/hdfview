# jpackage Integration for HDFView

This document describes how to create native application packages for HDFView using Java's jpackage tool.

## Overview

HDFView supports two packaging modes:

1. **App Image** - Creates a portable application directory (no installer)
2. **Platform Installer** - Creates native installers (MSI, DMG, DEB, RPM)

## Prerequisites

- Java 21 (includes jpackage)
- Maven 3.x
- HDF4 and HDF5 native libraries configured in `build.properties`
- **IMPORTANT**: Dependencies must be built first: `mvn clean package -DskipTests`

## How It Works

The jpackage build uses a **staging directory assembly** process:

1. **maven-dependency-plugin** copies all dependencies to `target/lib/`
2. **maven-antrun-plugin** creates `target/jpackage-input/` staging directory containing:
   - Main HDFView JAR and object module JAR
   - All dependency JARs (SWT, NatTable, etc.) - 70+ JARs
   - HDF native libraries (.so/.dll/.dylib) from configured paths
   - HDF5 plugins (compression libraries)
   - Documentation and sample files
3. **exec-maven-plugin** runs jpackage with the complete staging directory
4. jpackage bundles everything into a distributable application

**Result**: Complete ~155MB package with all dependencies and native libraries

## Creating an App Image (Portable Application)

The app image creates a self-contained application directory that can be run without installation.

### Command

```bash
# Build everything first (object + hdfview modules)
mvn clean package -DskipTests

# Create app image
mvn package -Pjpackage-app-image -pl hdfview -DskipTests
```

### Output Location

```
hdfview/target/dist/HDFView.app/          # macOS
hdfview/target/dist/HDFView/              # Linux/Windows
```

### Package Contents

The app image includes:
- All application JARs (hdfview, object, dependencies)
- HDF4 and HDF5 native libraries (25+ .so files)
- HDF5 compression plugins (11 plugins)
- Bundled Java 21 runtime
- Documentation and sample files
- Native launcher script

### Running the App Image

**Linux:**
```bash
# Run the launcher
./hdfview/target/dist/HDFView/bin/HDFView

# Or set LD_LIBRARY_PATH and run directly
export LD_LIBRARY_PATH=./hdfview/target/dist/HDFView/lib/app:$LD_LIBRARY_PATH
./hdfview/target/dist/HDFView/bin/HDFView
```

**macOS:**
```bash
# Run as macOS application
open hdfview/target/dist/HDFView.app

# Or run the launcher directly
./hdfview/target/dist/HDFView.app/Contents/MacOS/HDFView
```

**Windows:**
```cmd
# Run the executable
hdfview\target\dist\HDFView\HDFView.exe

# Or run from bin directory
hdfview\target\dist\HDFView\bin\HDFView.exe
```

### Verifying Package Contents

```bash
# Check package size
du -sh hdfview/target/dist/HDFView/

# List native libraries
find hdfview/target/dist/HDFView/lib/app -name "*.so" -o -name "*.dll" -o -name "*.dylib"

# List JARs
ls -lh hdfview/target/dist/HDFView/lib/app/*.jar

# Check HDF5 plugins
ls -la hdfview/target/dist/HDFView/lib/app/plugin/
```

## Creating Platform Installers

Platform installers create native packages that users can install on their systems.

### Linux (DEB/RPM)

**DEB Package (Debian/Ubuntu):**
```bash
# Build dependencies first
mvn clean package -DskipTests

# Create DEB package
mvn package -Pjpackage-deb -pl hdfview -DskipTests
```

Output: `hdfview/target/dist/hdfview_3.4-SNAPSHOT-1_amd64.deb`

**RPM Package (RedHat/Fedora/SUSE):**
```bash
# Build dependencies first
mvn clean package -DskipTests

# Create RPM package
mvn package -Pjpackage-rpm -pl hdfview -DskipTests
```

Output: `hdfview/target/dist/hdfview-3.4-SNAPSHOT-1.x86_64.rpm`

**Features:**
- Desktop menu shortcut in "Science" category
- File associations for .hdf, .h4, .h5, .hdf4, .hdf5
- System-level installation
- Dependency management via package manager

### macOS (DMG)

```bash
mvn package -Pjpackage-app-image -Djpackage.type=dmg -pl hdfview
```

Output: `hdfview/target/dist/HDFView-*.dmg`

**Note:** Code signing and notarization are only available on the canonical HDFGroup/hdfview repository with proper credentials.

### Windows (MSI)

```bash
mvn package -Pjpackage-app-image -Djpackage.type=msi -pl hdfview
```

Output: `hdfview/target/dist/HDFView-*.msi`

**Note:** Code signing requires Azure Trusted Signing credentials (only available on canonical repository).

## Platform-Specific Features

### Linux
- Desktop menu integration
- File associations for .hdf, .h4, .h5, .hdf4, .hdf5
- PNG icon

### macOS
- macOS application bundle (.app)
- File associations
- ICNS icon
- macOS-specific JVM options (-XstartOnFirstThread)
- Optional code signing and notarization

### Windows
- Windows Start Menu integration
- File associations
- ICO icon
- Console window option
- Per-user or system-wide installation
- Optional MSI code signing with Azure Trusted Signing

## Testing Workflow

### Local Testing (No Signing)

1. **Linux** - Test app-image and DEB/RPM locally:
   ```bash
   ./scripts/build-dev.sh
   mvn package -Pjpackage-app-image -pl hdfview
   ./hdfview/target/dist/HDFView/bin/HDFView
   ```

2. **macOS** - Test in fork (without signing):
   ```bash
   mvn package -Pjpackage-app-image -Djpackage.type=dmg -pl hdfview
   ```

3. **Windows** - Test in fork (without signing):
   ```bash
   mvn package -Pjpackage-app-image -Djpackage.type=msi -pl hdfview
   ```

### CI/CD Testing with Signing

Code signing is only available on the canonical `HDFGroup/hdfview` repository when running snapshot or release builds. The GitHub Actions workflows automatically handle signing when credentials are available.

## Troubleshooting

### jpackage not found
- Ensure you're using Java 21 (jpackage included since Java 14)
- Verify: `java -version` and `which jpackage` (or `where jpackage` on Windows)

### Missing dependencies
- Run full build first: `mvn clean install`
- Ensure HDF libraries are configured in `build.properties`

### File associations not working
- Check that `package_files/*.properties` files exist
- Verify file paths in properties files

### Icon not displaying
- Verify icon files exist in `package_files/`
- Check platform-specific icon format (ICO for Windows, ICNS for macOS, PNG for Linux)

## File Associations

HDFView registers file associations for:
- `.hdf` - Generic HDF file
- `.h4`, `.hdf4` - HDF4 files
- `.h5`, `.hdf5` - HDF5 files

These are defined in `package_files/HDFView*.properties` files.

## Architecture

The jpackage integration uses:
- **exec-maven-plugin** to invoke jpackage directly
- **Maven profiles** for platform-specific configuration
- **combine.children="append"** for merging platform-specific arguments

## Future Enhancements

- [ ] Add jpackage steps to GitHub Actions workflows
- [ ] Integrate code signing for Windows (Azure Trusted Signing)
- [ ] Integrate code signing and notarization for macOS (Apple Developer)
- [ ] Add profile for creating both app-image and installer in one command
