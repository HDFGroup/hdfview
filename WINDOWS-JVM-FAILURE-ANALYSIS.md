# Windows "Failed to launch JVM" Root Cause Analysis

## Summary

The Windows installer fails to launch with "Failed to launch JVM" error because **HDF5 native DLLs are missing** from the installation. The jpackage input preparation is failing silently on Windows, causing jpackage to package the wrong directory contents.

## Evidence from Wine Installation

Inspecting the Windows MSI installation at `/home/byrn/.wine/drive_c/users/byrn/AppData/Local/HDFView/`:

### What's WRONG in the Installation

1. **Development files present** (should NOT be there):
   - `pom.xml` - The actual hdfview module pom.xml
   - `src/main/` - Maven source directory
   - `src/test/` - Maven test directory
   - `.gitignore` - Git ignore file

2. **Missing native libraries**:
   - No HDF5 DLLs in `app/` directory
   - No HDF4 DLLs in `app/` directory
   - Only plugin DLLs present in `app/plugin/`

3. **Runtime error**:
   ```
   java.lang.UnsatisfiedLinkError: no hdf5_java in java.library.path: C:\users\byrn\AppData\Local\HDFView\app
   ```

### What's CORRECT in the Installation

- ✓ Java runtime bundled in `runtime/` (163MB)
- ✓ Application JARs present (24 JARs, no test dependencies)
- ✓ Documentation included (HTML + images)
- ✓ Sample files included
- ✓ HDFView.exe launcher created
- ✓ HDFView.cfg configuration correct

## Root Cause

The presence of `pom.xml` and `src/` directories indicates that **jpackage used the hdfview module directory as input instead of the prepared staging directory** (`hdfview/target/jpackage-input`).

### Why This Happens

The `maven-antrun-plugin`'s `prepare-jpackage-input` execution (hdfview/pom.xml:258-361) is **failing silently on Windows** due to:

1. **All copy tasks have `failonerror="false"`** - Errors are suppressed
2. **Windows path compatibility issues** - Ant tasks may not handle Windows paths correctly
3. **Flattenmapper behavior** - `<flattenmapper/>` might not work on Windows
4. **HDF library path issues** - `${hdf5.lib.dir}` and `${hdf.lib.dir}` paths might be invalid on Windows

When the staging directory is not properly created, jpackage receives an **incomplete or empty input directory**. The exec-maven-plugin configuration has:

```xml
<workingDirectory>${project.basedir}</workingDirectory>
<argument>--input</argument>
<argument>${project.build.directory}/jpackage-input</argument>
```

If `hdfview/target/jpackage-input` is empty or doesn't exist, jpackage may:
1. Fall back to using the working directory (`${project.basedir}` = `hdfview/`)
2. Package whatever files it finds there (pom.xml, src/, etc.)
3. Miss the native libraries entirely

## Comparison: Windows vs Linux

| Aspect | Linux (Working) | Windows (Broken) |
|--------|----------------|------------------|
| Input directory preparation | ✓ Succeeds | ✗ Fails silently |
| Native libraries | ✓ All .so files copied | ✗ No .dll files |
| Development files | ✓ Excluded | ✗ Included (pom.xml, src/) |
| Java runtime | ✓ Bundled | ✓ Bundled |
| Application JARs | ✓ 24 JARs | ✓ 24 JARs |
| Launch result | ✓ Works | ✗ UnsatisfiedLinkError |

## Why macOS Works

The macOS build (`.github/workflows/maven-build.yml:1546-1637`) **manually prepares the jpackage input directory** using shell commands instead of relying on the Ant plugin:

```bash
JPACKAGE_INPUT=hdfview/target/jpackage-input
rm -rf $JPACKAGE_INPUT
mkdir -p $JPACKAGE_INPUT
cp libs/*.jar $JPACKAGE_INPUT/
cp $HDF5LIB_PATH/lib/*.dylib $JPACKAGE_INPUT/
# etc...
```

This approach:
- Works reliably on macOS
- Uses explicit shell commands
- Has visible errors
- Controls exactly what gets packaged

## The Solution

There are two approaches to fix this:

### Option 1: Manual Preparation for Windows (Recommended)

Modify `.github/workflows/maven-build.yml` to use manual jpackage input preparation for Windows, similar to macOS:

```powershell
# Create staging directory
$JPACKAGE_INPUT = "hdfview\target\jpackage-input"
Remove-Item -Recurse -Force $JPACKAGE_INPUT -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $JPACKAGE_INPUT

# Copy JARs
Copy-Item "libs\*.jar" $JPACKAGE_INPUT\

# Copy HDF libraries
Copy-Item "$env:HDF5LIB_PATH\bin\*.dll" $JPACKAGE_INPUT\
Copy-Item "$env:HDF4LIB_PATH\bin\*.dll" $JPACKAGE_INPUT\

# Copy plugins, samples, docs...
```

**Pros**:
- Reliable, explicit control
- Errors are visible
- Matches macOS approach
- Quick to implement

**Cons**:
- Duplicates logic from pom.xml
- Maintenance overhead

### Option 2: Fix Ant Plugin Configuration

Fix the `maven-antrun-plugin` to work correctly on Windows:

1. Use Windows-compatible path separators
2. Add error checking and reporting
3. Set `failonerror="true"` for critical operations
4. Test path resolution on Windows

**Pros**:
- Fixes the root cause
- Single source of truth in pom.xml
- Works for all platforms

**Cons**:
- Harder to debug
- Requires Windows testing
- Ant behavior can be platform-specific

## Recommended Action

**Use Option 1** (manual preparation for Windows):

1. Add a `Prepare jpackage Input Directory` step in `build-windows-app` job (before `Create jpackage App Image`)
2. Use PowerShell to explicitly prepare the staging directory
3. Match the logic from the Ant plugin but with Windows-native commands
4. Remove reliance on Ant plugin for Windows builds

This is the fastest, most reliable fix and follows the proven macOS pattern.

## Testing the Fix

After implementing the fix, verify:

```powershell
# Check native libraries are included
ls hdfview\target\dist\HDFView\app\*.dll

# Check no development files
ls hdfview\target\dist\HDFView\pom.xml  # Should not exist
ls hdfview\target\dist\HDFView\src      # Should not exist

# Count DLLs (should have ~15 HDF DLLs)
(Get-ChildItem hdfview\target\dist\HDFView\app\*.dll).Count
```

## Date
2025-12-26
