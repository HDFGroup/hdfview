# Windows and macOS Build Troubleshooting Guide
**Created**: November 7, 2025
**Purpose**: Reference guide for resolving cross-platform build issues

---

## Table of Contents
1. [PowerShell Issues](#powershell-issues)
2. [Path Handling](#path-handling)
3. [Platform-Specific Archives](#platform-specific-archives)
4. [Maven Lifecycle and Plugins](#maven-lifecycle-and-plugins)
5. [Architecture Naming](#architecture-naming)
6. [SWT Platform Dependencies](#swt-platform-dependencies)
7. [Dependency Version Mismatches](#dependency-version-mismatches)

---

## PowerShell Issues

### Problem: Token Splitting on Special Characters

**Symptom**:
```
Error: Unknown lifecycle phase ".0.0"
Error: Unknown lifecycle phase ".test.skip=true"
```

**Root Cause**: PowerShell splits unquoted arguments containing dots, spaces, and other special characters into separate tokens.

**Example of Wrong Code**:
```powershell
# WRONG - PowerShell treats 2.0.0 as three tokens
mvn install:install-file -Dfile=foo.jar -Dversion=2.0.0

# What Maven actually receives:
# -Dversion=2 .0 .0
# Maven then tries to run ".0" as a lifecycle phase
```

**Solution**: Quote all -D parameters:
```powershell
# CORRECT
mvn install:install-file "-Dfile=foo.jar" `
  "-Dversion=2.0.0" `
  "-DgroupId=org.example" `
  "-DartifactId=example" `
  "-Dpackaging=jar"
```

**Pattern to Follow**:
```powershell
# Always quote these:
"-Dkey=value"           # Any property with dots in key or value
"-Dmaven.test.skip=true"
"-Dpmd.skip=true"
"-Dfile=$($var.FullName)"  # When using variables
```

**Applies To**: Any command-line arguments in PowerShell containing special characters.

---

## Path Handling

### Problem: Mixed Path Separators Break Java/Maven

**Symptom**:
```
ERROR: HDF5 plugin directory does not exist: D:ahdfviewhdfviewhdf5HDF5-2.0.0-win64/lib/plugin
```

**Root Cause**: Windows environment variables use backslashes (`\`), but Java/Maven properties expect forward slashes (`/`) or escaped backslashes (`\\`).

**Example of Wrong Code**:
```powershell
# WRONG - Windows paths with backslashes
@"
hdf5.lib.dir=$env:HDF5LIB_PATH/lib
"@ | Out-File -FilePath build.properties

# Results in corrupted paths like:
# hdf5.lib.dir=D:\a\hdfview\hdfview\hdf5\HDF5-2.0.0-win64/lib
```

**Solution**: Convert Windows paths to forward slashes:
```powershell
# CORRECT
$HDF5_PATH = $env:HDF5LIB_PATH -replace '\\', '/'
$HDF4_PATH = $env:HDF4LIB_PATH -replace '\\', '/'

@"
hdf5.lib.dir=$HDF5_PATH/lib
hdf5.plugin.dir=$HDF5_PATH/lib/plugin
hdf.lib.dir=$HDF4_PATH/lib
"@ | Out-File -FilePath build.properties
```

**Pattern to Follow**:
```powershell
# For any Windows path going into Java/Maven:
$CONVERTED_PATH = $env:WINDOWS_PATH -replace '\\', '/'

# This works on Windows, and forward slashes are platform-independent in Java
```

**Why This Works**: Java and Maven accept forward slashes on all platforms, including Windows. This is the safest cross-platform approach.

---

## Platform-Specific Archives

### Problem: Different Archive Structures

**Symptom**:
```
Cannot find path 'D:\a\hdfview\hdfview\hdf4\HDF_Group\HDF' because it does not exist.
```

**Root Cause**: HDF Group packages binaries with different directory structures for different platforms.

**Archive Structures**:

**Linux/macOS** (nested structure):
```
hdf4-master-*-ubuntu-2404_gcc.tar.gz
├── HDF-4.3.1-Linux.tar.gz
    └── HDF_Group/
        └── HDF/
            └── 4.3.1-50c8c59/
                ├── bin/
                ├── lib/
                └── share/
```

**Windows** (flat structure):
```
hdf4-master-*-win-vs2022_cl.zip
├── HDF-4.3.1-win64.zip
    └── HDF-4.3.1-win64/
        ├── bin/
        ├── lib/
        └── share/
```

**Solution**: Use platform-specific directory search patterns.

**Linux/macOS**:
```bash
# Extract outer archive
tar -zxvf hdf4-master-*-ubuntu-2404_gcc.tar.gz
cd hdf4
tar -zxvf HDF-*-Linux.tar.gz --strip-components 1

# Find nested directory
HDF4DIR=${{ github.workspace }}/hdf4/HDF_Group/HDF/
FILE_NAME_HDF=$(ls $HDF4DIR)
echo "HDF4LIB_PATH=$HDF4DIR$FILE_NAME_HDF" >> $GITHUB_ENV
```

**Windows**:
```powershell
# Extract outer archive
7z x hdf4-master-*-win-vs2022_cl.zip
Set-Location hdf4
7z x HDF-*-win64.zip

# Find flat directory
$HDF4DIR = Get-ChildItem -Path . -Filter "HDF-*-win64" -Directory | Select-Object -First 1
if (-not $HDF4DIR) {
  Write-Error "HDF-*-win64 directory not found!"
  exit 1
}
$HDF4LIB_PATH = $HDF4DIR.FullName
echo "HDF4LIB_PATH=$HDF4LIB_PATH" >> $env:GITHUB_ENV
```

**Key Differences**:
| Aspect | Linux/macOS | Windows |
|--------|-------------|---------|
| Archive format | `.tar.gz` | `.zip` |
| Extraction tool | `tar -zxvf` | `7z x` |
| Directory structure | Nested (HDF_Group/HDF/version) | Flat (HDF-version-win64) |
| Search pattern | `HDF_Group/HDF/` + version dir | `HDF-*-win64` pattern |

---

## Maven Lifecycle and Plugins

### Problem: PMD/Checkstyle Running During Builds

**Symptom**:
```
Failed to execute goal org.apache.maven.plugins:maven-pmd-plugin:3.21.2:cpd
java.lang.NoClassDefFoundError: net/sourceforge/pmd/cpd/renderer/CPDRenderer
```

**Root Cause**: PMD plugin has explicit `<execution>` bindings to lifecycle phases. Skip flags don't work when plugins are explicitly bound.

**Maven Lifecycle Phases** (in order):
```
validate
  ↓
initialize
  ↓
generate-sources  ← maven-antrun-plugin (copies SWT JAR)
  ↓
process-sources
  ↓
generate-resources
  ↓
process-resources
  ↓
compile
  ↓
process-classes
  ↓
generate-test-sources
  ↓
process-test-sources
  ↓
generate-test-resources
  ↓
process-test-resources
  ↓
test-compile
  ↓
process-test-classes
  ↓
test
  ↓
prepare-package
  ↓
package  ← BUILD WORKFLOWS SHOULD STOP HERE
  ↓
pre-integration-test
  ↓
integration-test
  ↓
post-integration-test
  ↓
verify  ← PMD/CHECKSTYLE RUN HERE
  ↓
install
  ↓
deploy
```

**Problem with Skip Flags**:
```xml
<!-- In parent POM -->
<plugin>
  <artifactId>maven-pmd-plugin</artifactId>
  <executions>
    <execution>
      <id>pmd-cpd-check</id>
      <phase>verify</phase>  <!-- Explicitly bound to verify -->
      <goals>
        <goal>cpd-check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

When plugins are explicitly bound like this, `-Dpmd.skip=true` may not work reliably.

**Solution**: Run only the phases you need, stopping before `verify`.

**For Build Workflows**:
```bash
# Build repository module - only need generate-sources
mvn generate-sources -pl repository -B

# Build other modules - only need package
mvn package -pl object,hdfview -Dmaven.test.skip=true -B
```

**For CI/Quality Workflows**:
```bash
# Run full lifecycle including verify (PMD/Checkstyle)
mvn verify -B
```

**Key Principle**:
- **Build workflows** = artifact generation = stop at `package`
- **CI/Quality workflows** = quality checks = run through `verify`

---

## Architecture Naming

### Problem: os.arch Reports "amd64" but Directories Named "x86_64"

**Symptom**:
```
An Ant BuildException has occured: lib/ext/swt/win/amd64 does not exist.
```

**Root Cause**: Different tools and conventions use different names for the same 64-bit x86 architecture.

**Architecture Naming Conventions**:
| Context | Name Used |
|---------|-----------|
| GitHub Actions on Windows | `amd64` |
| GitHub Actions on macOS | `amd64` or `x86_64` |
| Traditional Unix | `x86_64` |
| Debian packages | `amd64` |
| Maven property `${os.arch}` | Platform-dependent (usually `amd64` on Windows) |
| Directory names (convention) | Usually `x86_64` |

**Problem Scenario**:
```xml
<!-- In repository/pom.xml -->
<copy todir="./lib">
  <fileset dir="../lib/ext/swt/win/${os.arch}">
    <include name="swt.jar" />
  </fileset>
</copy>
```

On GitHub Actions Windows runner:
- `${os.arch}` evaluates to `amd64`
- Looks for `lib/ext/swt/win/amd64/`
- But only `lib/ext/swt/win/x86_64/` exists
- Build fails

**Solution 1**: Create Architecture Aliases
```bash
# Create amd64 as copy of x86_64
cp -r lib/ext/swt/win/x86_64 lib/ext/swt/win/amd64
cp -r lib/ext/swt/osx/x86_64 lib/ext/swt/osx/amd64
```

**Solution 2**: Use Symlinks (Linux/macOS only)
```bash
ln -s x86_64 lib/ext/swt/win/amd64
ln -s x86_64 lib/ext/swt/osx/amd64
```

**Solution 3**: Modify POM to Check Both
```xml
<copy todir="./lib">
  <fileset dir="../lib/ext/swt/win/x86_64" erroronmissingdir="false">
    <include name="swt.jar" />
  </fileset>
  <fileset dir="../lib/ext/swt/win/amd64" erroronmissingdir="false">
    <include name="swt.jar" />
  </fileset>
</copy>
```

**Recommended**: Solution 1 (copy) - works everywhere, no special file system support needed.

---

## SWT Platform Dependencies

### Problem: Platform-Specific SWT Artifacts Not Found

**Symptom**:
```
Could not find artifact org.eclipse.platform:org.eclipse.swt.win32.win32.amd64:jar:3.126.0
```

**Root Cause**: Maven profiles in `hdfview/pom.xml` define platform-specific SWT dependencies, but they're not available in Maven Central or local repository.

**How SWT Platform Selection Works**:

```xml
<!-- hdfview/pom.xml -->
<profiles>
  <profile>
    <id>swt-windows</id>
    <activation>
      <os><family>windows</family></os>
    </activation>
    <properties>
      <osgi.platform>win32.win32.${os.arch}</osgi.platform>
      <swt.artifactId>org.eclipse.swt.${osgi.platform}</swt.artifactId>
    </properties>
    <dependencies>
      <dependency>
        <groupId>org.eclipse.platform</groupId>
        <artifactId>${swt.artifactId}</artifactId>
        <version>${swt.version}</version>
      </dependency>
    </dependencies>
  </profile>
</profiles>
```

On Windows with `os.arch=amd64`:
- Profile activates: `swt-windows`
- `osgi.platform` = `win32.win32.amd64`
- `swt.artifactId` = `org.eclipse.swt.win32.win32.amd64`
- Maven looks for: `org.eclipse.platform:org.eclipse.swt.win32.win32.amd64:3.126.0`

**Platform-Specific Artifact IDs**:
| Platform | Artifact ID | Version |
|----------|-------------|---------|
| Linux | `org.eclipse.swt.gtk.linux.x86_64` | 3.126.0 |
| Windows | `org.eclipse.swt.win32.win32.amd64` | 3.126.0 |
| macOS | `org.eclipse.swt.cocoa.macosx.amd64` | 3.126.0 |

**Solution Process**:

1. **Repository module copies platform SWT JAR**:
```bash
# Maven antrun plugin copies platform-specific JAR
mvn generate-sources -pl repository
# Result: repository/lib/swt.jar (platform-specific)
```

2. **Install SWT JAR with correct coordinates**:

**Windows**:
```powershell
mvn install:install-file "-Dfile=repository\lib\swt.jar" `
  "-DgroupId=org.eclipse.platform" `
  "-DartifactId=org.eclipse.swt.win32.win32.amd64" `
  "-Dversion=3.126.0" `
  "-Dpackaging=jar" `
  "-DgeneratePom=true"
```

**macOS**:
```bash
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.cocoa.macosx.amd64 \
  -Dversion=3.126.0 \
  -Dpackaging=jar \
  -DgeneratePom=true
```

**Linux**:
```bash
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.gtk.linux.x86_64 \
  -Dversion=3.126.0 \
  -Dpackaging=jar \
  -DgeneratePom=true
```

3. **Build hdfview module**:
```bash
mvn package -pl hdfview -Dmaven.test.skip=true
# Now finds org.eclipse.platform:org.eclipse.swt.win32.win32.amd64:3.126.0 in local repo
```

**Key Points**:
- SWT JARs are platform-specific (Windows JAR won't work on macOS)
- Artifact IDs must match the platform profile definitions
- Must install to local Maven repo before building dependent modules
- Version must match `${swt.version}` in parent POM

---

## Dependency Version Mismatches

### Problem: Dependency Versions Don't Match POM Properties

**Symptom**:
```
Could not find artifact jarhdf:jarhdf:jar:4.3.1
```

**Root Cause**: Workflows installing JARs with hardcoded version numbers that don't match parent POM property definitions.

**Example Parent POM Properties**:
```xml
<!-- pom.xml -->
<properties>
  <hdf.version>4.3.1</hdf.version>      <!-- HDF4/jarhdf version -->
  <hdf5.version>2.0.0</hdf5.version>    <!-- HDF5/jarhdf5 version -->
  <swt.version>3.126.0</swt.version>
</properties>
```

**Wrong Workflow Code**:
```bash
# Installing both with 2.0.0 - WRONG!
mvn install:install-file -Dfile=repository/lib/jarhdf5-2.0.0.jar \
  -DgroupId=jarhdf5 -DartifactId=jarhdf5 -Dversion=2.0.0 \
  -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -Dfile=repository/lib/jarhdf-4.3.1.jar \
  -DgroupId=jarhdf -DartifactId=jarhdf -Dversion=2.0.0 \
  -Dpackaging=jar -DgeneratePom=true
```

**Correct Workflow Code**:
```bash
# Use correct versions matching parent POM
mvn install:install-file -Dfile=repository/lib/jarhdf5-2.0.0.jar \
  -DgroupId=jarhdf5 -DartifactId=jarhdf5 -Dversion=2.0.0 \
  -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -Dfile=repository/lib/jarhdf-4.3.1.jar \
  -DgroupId=jarhdf -DartifactId=jarhdf -Dversion=4.3.1 \
  -Dpackaging=jar -DgeneratePom=true
```

**Version Mapping**:
| Dependency | Parent POM Property | Correct Version | Artifact ID |
|------------|---------------------|-----------------|-------------|
| HDF5 Java | `hdf5.version` | 2.0.0 | jarhdf5 |
| HDF4 Java | `hdf.version` | 4.3.1 | jarhdf |
| SWT | `swt.version` | 3.126.0 | org.eclipse.swt.* |
| FITS | N/A | 1.0.0 | fits |
| NetCDF | N/A | 1.0.0 | netcdf |

**How to Verify Correct Versions**:
```bash
# Check parent POM
grep -A 1 "hdf.version\|hdf5.version\|swt.version" pom.xml

# Check module dependencies
grep -A 5 "jarhdf\|jarhdf5\|swt" object/pom.xml
grep -A 5 "jarhdf\|jarhdf5\|swt" hdfview/pom.xml
```

**Pattern to Follow**:
1. Always check parent POM for version properties
2. Use those exact versions when installing JARs
3. If unsure, look at module POMs to see what versions they expect
4. Document version mappings in workflow comments

---

## Troubleshooting Checklist

When encountering cross-platform build failures:

### 1. Identify the Platform
- [ ] Is it Windows-only?
- [ ] Is it macOS-only?
- [ ] Is it both Windows and macOS?
- [ ] Does it work on Linux?

### 2. Identify the Phase
- [ ] What Maven phase is failing?
- [ ] What Maven plugin is failing?
- [ ] What step in the workflow is failing?

### 3. Check for Common Issues

**PowerShell** (Windows only):
- [ ] Are all Maven -D parameters quoted?
- [ ] Are variables properly referenced with `$(...)`?
- [ ] Is backtick (`) used for line continuation?

**Paths**:
- [ ] Are Windows paths converted to forward slashes?
- [ ] Are paths in build.properties using forward slashes?
- [ ] Are environment variables set correctly?

**Archives**:
- [ ] Is the archive structure different on this platform?
- [ ] Are we using the correct extraction commands?
- [ ] Are we searching for the right directory patterns?

**Dependencies**:
- [ ] Are all required JARs being installed?
- [ ] Do version numbers match parent POM?
- [ ] Are platform-specific artifacts using correct coordinates?

**Maven**:
- [ ] Are we running unnecessary lifecycle phases?
- [ ] Are quality plugins trying to run in build workflows?
- [ ] Should we stop at an earlier phase?

**Architecture**:
- [ ] Does `os.arch` match directory names?
- [ ] Do we need architecture aliases?
- [ ] Is the right SWT platform JAR being used?

### 4. Debugging Techniques

**Add Debug Output**:
```powershell
# Windows
Write-Host "Current directory:"
Get-ChildItem | Format-Table Name
Write-Host "Environment variables:"
Write-Host "HDF4LIB_PATH: $env:HDF4LIB_PATH"
```

```bash
# Linux/macOS
echo "Current directory:"
ls -la
echo "Environment variables:"
echo "HDF4LIB_PATH: $HDF4LIB_PATH"
```

**Test Locally**:
```bash
# Replicate environment locally
export HDF4LIB_PATH=/path/to/hdf4
export HDF5LIB_PATH=/path/to/hdf5

# Run same commands as workflow
mvn generate-sources -pl repository
mvn package -pl object,hdfview -Dmaven.test.skip=true
```

**Check Maven Output**:
```bash
# Verbose output
mvn -X package

# Debug output
mvn -e package

# Show effective POM
mvn help:effective-pom

# Show active profiles
mvn help:active-profiles
```

---

## Quick Reference

### PowerShell Command Pattern
```powershell
mvn install:install-file "-Dfile=$($jar.FullName)" `
  "-DgroupId=group" `
  "-DartifactId=artifact" `
  "-Dversion=1.0.0" `
  "-Dpackaging=jar" `
  "-DgeneratePom=true"
```

### Path Conversion Pattern
```powershell
$JAVA_PATH = $env:WINDOWS_PATH -replace '\\', '/'
```

### Platform Archive Detection
```powershell
# Windows
$DIR = Get-ChildItem -Filter "HDF-*-win64" -Directory | Select-Object -First 1
```

```bash
# Linux/macOS
HDF4DIR=${{ github.workspace }}/hdf4/HDF_Group/HDF/
FILE_NAME=$(ls $HDF4DIR)
```

### Build Phase Selection
```bash
# Repository: Only need generate-sources
mvn generate-sources -pl repository

# Modules: Only need package
mvn package -pl object,hdfview -Dmaven.test.skip=true
```

### SWT Installation
```bash
# Template (replace PLATFORM_ARTIFACT_ID)
mvn install:install-file -Dfile=repository/lib/swt.jar \
  -DgroupId=org.eclipse.platform \
  -DartifactId=org.eclipse.swt.PLATFORM_ARTIFACT_ID \
  -Dversion=3.126.0 \
  -Dpackaging=jar \
  -DgeneratePom=true

# Platform artifact IDs:
# Linux:   org.eclipse.swt.gtk.linux.x86_64
# Windows: org.eclipse.swt.win32.win32.amd64
# macOS:   org.eclipse.swt.cocoa.macosx.amd64
```

---

## Related Documentation

- **Session Summary**: `.claude/Session-Summary-2025-11-07.md`
- **Previous Session**: `.claude/Final-Status-2025-11-06.md`
- **HDF Strategy**: `.claude/HDF-Library-Strategy-2025-11-06.md`
- **CI Implementation**: `.claude/Cross-Platform-CI-Implementation-2025-11-06.md`

---

**Document Created**: November 7, 2025
**Last Updated**: November 7, 2025
**Status**: Reference guide based on real troubleshooting experience
