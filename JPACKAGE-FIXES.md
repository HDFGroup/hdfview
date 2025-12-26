# jpackage Installer Fixes

## Issues Fixed

### 1. Test Dependencies Excluded from Runtime Package
**Problem:** JUnit, Hamcrest, and SWTBot test dependencies were being included in the jpackage app-image, bloating the package size and exposing test libraries to end users.

**Fix:** Modified `maven-dependency-plugin` configuration in `hdfview/pom.xml` (line 710) to use `<includeScope>runtime</includeScope>`, which excludes test and provided scope dependencies.

**Result:** Package now contains only 24 runtime JARs instead of ~40+ with test dependencies.

### 2. Complete Documentation Included
**Problem:** Only `.md` and `.txt` files were being copied from UsersGuide, missing all `.html` files and the `images/` directory with PNG/JPG/GIF files.

**Fix:** Modified documentation copy section in `hdfview/pom.xml` (lines 341-346) to include:
- `**/*.html`
- `**/*.png`
- `**/*.jpg`
- `**/*.gif`

**Result:** Complete UsersGuide now included with 89 files (HTML pages + images).

### 3. Sample Files Included
**Problem:** Samples directory path was incorrect (`${project.basedir}/samples` instead of `${project.basedir}/../samples`), causing sample files to not be included.

**Fix:** Modified samples copy section in `hdfview/pom.xml` (line 331) to use correct path `${project.basedir}/../samples`.

**Result:** All 9 sample HDF files now included:
- annras.hdf
- apollo17_earth.jpg
- data40x10.txt
- hdf5_test.h5
- hdf5_test.h5.ddl
- misr_am1_metadata.hdf
- Roy.nc
- swp05569slg.fits
- tst0001.fits

## Verified Results

After fixes, the jpackage app-image now contains:
- **Runtime JARs**: 24 (no test dependencies)
- **Samples**: 9 files in `lib/app/samples/`
- **Documentation**: 89 files in `lib/app/doc/` (HTML + images)
- **Plugins**: HDF5 plugins in `lib/app/plugin/`
- **Native Libraries**: All HDF4/HDF5 shared libraries
- **Java Runtime**: 163MB bundled JRE in `lib/runtime/`

## Remaining Issues

### Windows "Failed to launch JVM" Error
**Status:** Requires separate investigation

**Observations:**
- Linux app-image successfully includes bundled JRE and launches correctly
- Windows builds complete without errors in GitHub Actions
- Issue likely specific to Windows build environment or signing process
- May be related to MSI installer creation or Windows code signing

**Recommended Actions:**
1. Test unsigned Windows app-image locally (before MSI creation)
2. Check Windows launcher executable configuration
3. Verify Windows JRE bundling with jlink
4. Review Windows-specific jpackage options in pom.xml
5. Check GitHub Actions Windows build logs for warnings

## Files Modified

- `hdfview/pom.xml` (3 changes):
  - Line 710: Added `<includeScope>runtime</includeScope>` to maven-dependency-plugin
  - Line 331: Fixed samples path to `${project.basedir}/../samples`
  - Lines 341-346: Added HTML and image file includes for documentation

## Testing

To test the fixes locally:
```bash
# Build app-image with fixes
mvn verify -Pjpackage-app-image -pl object,hdfview -DskipTests -Ddependency-check.skip=true

# Verify samples included
ls -la hdfview/target/dist/HDFView/lib/app/samples/

# Verify docs included
find hdfview/target/dist/HDFView/lib/app/doc -type f | wc -l

# Verify no test JARs
ls hdfview/target/dist/HDFView/lib/app/*.jar | grep -i junit
```

Expected results:
- Samples: 9 files
- Docs: 89 files
- No JUnit JARs found

## Date
2025-12-26
