# Cross-Platform Build Quick Reference

## Quick Github Build Status Check

If a build attempt fails, use the clean script `scripts/clean-all.sh` to remove partial  or corrupted artifacts before attempting another build.

```bash
# Check workflow status
gh run list --workflow="maven-build.yml" --limit 5

# Check CI status
gh run list --workflow="maven-ci.yml" --limit 3
gh run list --workflow="maven-ci-windows.yml" --limit 3
gh run list --workflow="maven-ci-macos.yml" --limit 3

# View latest run
gh run view

# Trigger manual build
gh workflow run "hdfview daily build"
```

---

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

# Build
mvn generate-sources -pl repository
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.gtk.linux.x86_64 \
  -Dversion=3.126.0 -Dpackaging=jar -DgeneratePom=true
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

# Build (QUOTE ALL -D PARAMETERS!)
mvn generate-sources -pl repository -B
mvn install:install-file "-Dfile=repository\lib\swt.jar" `
  "-DgroupId=org.eclipse.platform" `
  "-DartifactId=org.eclipse.swt.win32.win32.amd64" `
  "-Dversion=3.126.0" "-Dpackaging=jar" "-DgeneratePom=true"
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

# Build
mvn generate-sources -pl repository -B
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.cocoa.macosx.amd64 \
  -Dversion=3.126.0 -Dpackaging=jar -DgeneratePom=true
mvn package -pl object,hdfview -DskipTests=true  -B
```

---

## Common Issues & Quick Fixes

### PowerShell Token Splitting
**Error**: `Unknown lifecycle phase ".0.0"`
**Fix**: Quote all -D parameters
```powershell
# WRONG
mvn install:install-file -Dfile=foo.jar -Dversion=2.0.0

# CORRECT
mvn install:install-file "-Dfile=foo.jar" "-Dversion=2.0.0"
```

### Windows Path Issues
**Error**: Corrupted paths in build.properties
**Fix**: Use forward slashes (/) not backslashes (\) in all paths
```properties
# CORRECT
hdf5.lib.dir=C:/path/to/hdf5

# WRONG
hdf5.lib.dir=C:\path\to\hdf5
```

### PMD Execution During Build
**Error**: `Failed to execute goal maven-pmd-plugin:3.21.2:cpd`
**Fix**: Stop at package phase
```bash
# Don't use 'install' or 'verify'
mvn package -pl object,hdfview -Dmaven.test.skip=true
```

### SWT Artifact Not Found
**Error**: `Could not find artifact org.eclipse.platform:org.eclipse.swt.*.*.amd64`
**Fix**: Install SWT JAR with platform-specific coordinates
```bash
# Must run 'mvn generate-sources -pl repository' first!
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DartifactId=org.eclipse.swt.PLATFORM.PLATFORM.amd64 \
  -DgroupId=org.eclipse.platform -Dversion=3.126.0
```

### Architecture Directory Not Found
**Error**: `lib/ext/swt/win/amd64 does not exist`
**Fix**: Architecture alias already created (should not occur)
```bash
# If needed:
cp -r lib/ext/swt/win/x86_64 lib/ext/swt/win/amd64
```

---

## Artifact Patterns

### Binary Archives
- **Linux**: `HDFView-*-Linux-x86_64.tar.gz`
- **Windows**: `HDFView-*-win64.zip`
- **macOS**: `HDFView-*-Darwin.tar.gz`

### App Archives
- **Linux**: `HDFViewApp-*-Linux-x86_64.tar.gz`
- **Windows**: `HDFViewApp-*-win64.zip`
- **macOS**: `HDFViewApp-*-Darwin.tar.gz`

### Contents
- HDFView JAR
- Object module JAR
- Dependency JARs (SWT, HDF, etc.)
- Native libraries (.so/.dll/.dylib)

---

## Quick Test Commands

```bash
# Test on Linux
java -jar hdfview-*.jar

# Test native libraries (Linux)
ldd lib/*.so

# Test native libraries (macOS)
otool -L lib/*.dylib

# Test native libraries (Windows PowerShell)
dumpbin /dependents lib\*.dll
```
