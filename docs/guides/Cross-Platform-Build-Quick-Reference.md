# Cross-Platform Build Quick Reference
**Last Updated**: November 7, 2025
**Status**: All platforms operational

---

## Quick Status Check

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

## Platform-Specific Commands

### Linux (Ubuntu)
```bash
# Download HDF libraries
gh release download snapshot --repo HDFGroup/hdf4 --pattern "hdf4-master-*-ubuntu-2404_gcc.tar.gz"
gh release download snapshot --repo HDFGroup/hdf5 --pattern "hdf5-develop-*-ubuntu-2404_gcc.tar.gz"

# Extract (nested structure)
tar -zxvf hdf4-master-*-ubuntu-2404_gcc.tar.gz
cd hdf4 && tar -zxvf HDF-*-Linux.tar.gz --strip-components 1

# Path variable
export LD_LIBRARY_PATH=$HDF5LIB_PATH/lib:$HDF4LIB_PATH/lib

# Build
mvn generate-sources -pl repository
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.gtk.linux.x86_64 \
  -Dversion=3.126.0 -Dpackaging=jar -DgeneratePom=true
mvn package -pl object,hdfview -Dmaven.test.skip=true
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

# Path conversion (IMPORTANT)
$HDF5_PATH = $env:HDF5LIB_PATH -replace '\\', '/'
$HDF4_PATH = $env:HDF4LIB_PATH -replace '\\', '/'

# Path variable
$env:PATH = "$env:HDF5LIB_PATH\bin;$env:HDF4LIB_PATH\bin;$env:PATH"

# Build (QUOTE ALL -D PARAMETERS!)
mvn generate-sources -pl repository -B
mvn install:install-file "-Dfile=repository\lib\swt.jar" `
  "-DgroupId=org.eclipse.platform" `
  "-DartifactId=org.eclipse.swt.win32.win32.amd64" `
  "-Dversion=3.126.0" "-Dpackaging=jar" "-DgeneratePom=true"
mvn package -pl object,hdfview "-Dmaven.test.skip=true" -B
```

### macOS (Bash)
```bash
# Download HDF libraries
gh release download snapshot --repo HDFGroup/hdf4 --pattern "hdf4-master-*-macos14_clang.tar.gz"
gh release download snapshot --repo HDFGroup/hdf5 --pattern "hdf5-develop-*-macos14_clang.tar.gz"

# Extract (nested structure)
tar -zxvf hdf4-master-*-macos14_clang.tar.gz
cd hdf4 && tar -zxvf HDF-*-Darwin.tar.gz --strip-components 1

# Path variable
export DYLD_LIBRARY_PATH=$HDF5LIB_PATH/lib:$HDF4LIB_PATH/lib

# Build
mvn generate-sources -pl repository -B
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.cocoa.macosx.amd64 \
  -Dversion=3.126.0 -Dpackaging=jar -DgeneratePom=true
mvn package -pl object,hdfview -Dmaven.test.skip=true -B
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
**Fix**: Convert backslashes to forward slashes
```powershell
$HDF5_PATH = $env:HDF5LIB_PATH -replace '\\', '/'
```

### HDF Directory Not Found (Windows)
**Error**: `Cannot find path 'hdf4\HDF_Group\HDF'`
**Fix**: Use flat directory pattern
```powershell
$HDF4DIR = Get-ChildItem -Filter "HDF-*-win64" -Directory | Select-Object -First 1
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

## Key Version Numbers

| Dependency | Version | Property | Notes |
|------------|---------|----------|-------|
| HDF4 (jarhdf) | 4.3.1 | `hdf.version` | In parent POM |
| HDF5 (jarhdf5) | 2.0.0 | `hdf5.version` | In parent POM |
| SWT | 3.126.0 | `swt.version` | In parent POM |
| fits | 1.0.0 | N/A | Hardcoded |
| netcdf | 1.0.0 | N/A | Hardcoded |
| Java | 21 | N/A | Target version |
| Maven | 3.9+ | N/A | Build tool |

---

## Maven Lifecycle Phases

```
generate-sources  ← Repository module (copies SWT JAR)
    ↓
compile
    ↓
test
    ↓
package           ← BUILD WORKFLOWS STOP HERE
    ↓
verify            ← PMD/Checkstyle run here (CI only)
    ↓
install
    ↓
deploy
```

**Build Workflows**: Stop at `package`
**CI Workflows**: Run through `verify`

---

## Workflow Files

### Build Workflow
- **File**: `.github/workflows/maven-build.yml`
- **Jobs**: 6 (Linux/Windows/macOS × Binary/App)
- **Trigger**: Daily schedule, manual
- **Purpose**: Generate platform artifacts

### CI Workflows
- **Files**:
  - `.github/workflows/maven-ci.yml` (Linux)
  - `.github/workflows/maven-ci-windows.yml` (Windows)
  - `.github/workflows/maven-ci-macos.yml` (macOS)
- **Trigger**: Push, pull request, manual
- **Purpose**: Testing and quality checks

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

## Native Library Patterns

| Platform | Extension | Location | Path Variable |
|----------|-----------|----------|---------------|
| Linux | `.so` | `lib/` | `LD_LIBRARY_PATH` |
| Windows | `.dll` | `bin/` | `PATH` |
| macOS | `.dylib` | `lib/` | `DYLD_LIBRARY_PATH` |

---

## Build Time Expectations

| Platform | Binary Build | App Build | CI Tests |
|----------|-------------|-----------|----------|
| Linux | ~12 min | ~12 min | ~10-15 min |
| Windows | ~15 min | ~15 min | ~30-35 min |
| macOS | ~13 min | ~13 min | ~25-30 min |

---

## Common Git Commands

```bash
# Check recent commits
git log --oneline -5

# View changes
git diff

# Commit changes
git add <files>
git commit -m "message"
git push origin master

# View status
git status

# View remote
git remote -v
```

---

## Documentation Index

### Current Status
- `.claude/Current-Status-2025-11-07.md` - Overall status

### This Session
- `.claude/Session-Summary-2025-11-07.md` - Detailed summary
- `.claude/Windows-macOS-Build-Troubleshooting-Guide.md` - Technical guide
- `.claude/Next-Session-Tasks-2025-11-07.md` - Task list

### Historical
- `.claude/Final-Status-2025-11-06.md` - Previous session
- `.claude/Cross-Platform-CI-Implementation-2025-11-06.md` - CI details
- `.claude/HDF-Library-Strategy-2025-11-06.md` - HDF strategy

### Reference
- `.claude/Cross-Platform-Build-Quick-Reference.md` - This document
- `CLAUDE.md` - Project overview

---

## Emergency Contacts

### If Builds Fail
1. Check GitHub Actions status: https://www.githubstatus.com/
2. Check HDF Group releases: https://github.com/HDFGroup/hdf4/releases
3. Review workflow logs: `gh run view --log-failed`
4. Consult troubleshooting guide

### If Artifacts Corrupted
1. Re-run workflow: `gh run rerun <run-id>`
2. Check artifact sizes: `gh run view <run-id>`
3. Test locally: Download and extract

### If Tests Fail
1. Check test logs: `gh run view <run-id> --log-failed`
2. Run locally: `mvn test -Dtest=<TestClassName>`
3. Check CI workflow: `.github/workflows/maven-ci*.yml`

---

## Useful Links

- **Repository**: https://github.com/byrnHDF/hdfview
- **Actions**: https://github.com/byrnHDF/hdfview/actions
- **HDF4 Releases**: https://github.com/HDFGroup/hdf4/releases
- **HDF5 Releases**: https://github.com/HDFGroup/hdf5/releases
- **SWT Downloads**: https://download.eclipse.org/eclipse/downloads/

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

---

## Environment Setup

### Linux
```bash
export HDF4LIB_PATH=/path/to/hdf4
export HDF5LIB_PATH=/path/to/hdf5
export LD_LIBRARY_PATH=$HDF5LIB_PATH/lib:$HDF4LIB_PATH/lib
```

### Windows
```powershell
$env:HDF4LIB_PATH = "C:\path\to\hdf4"
$env:HDF5LIB_PATH = "C:\path\to\hdf5"
$env:PATH = "$env:HDF5LIB_PATH\bin;$env:HDF4LIB_PATH\bin;$env:PATH"
```

### macOS
```bash
export HDF4LIB_PATH=/path/to/hdf4
export HDF5LIB_PATH=/path/to/hdf5
export DYLD_LIBRARY_PATH=$HDF5LIB_PATH/lib:$HDF4LIB_PATH/lib
```

---

**Document Type**: Quick Reference
**Created**: November 7, 2025
**Purpose**: Fast lookups and emergency procedures
**Audience**: Developers and maintainers
