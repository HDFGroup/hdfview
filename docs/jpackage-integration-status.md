# jpackage Integration Status

## Completed

### ✅ CI Workflows (ci-linux.yml, ci-macos.yml, ci-windows.yml)
- All three platform CI workflows successfully create jpackage app-images
- App-images are tested and verified on each platform
- Artifacts uploaded for verification

### ✅ Maven Build Workflow (maven-build.yml)
- **Replaced manual app packages with jpackage app-images** for all platforms:
  - Linux: `build-linux-app` now uses `-Pjpackage-app-image` profile
  - Windows: `build-windows-app` now uses `-Pjpackage-app-image` profile
  - macOS: `build-macos-app` now uses `-Pjpackage-app-image` profile

- **Added skeleton installer jobs**:
  - `build-linux-installers` (DEB/RPM) - placeholder
  - `build-macos-installer` (DMG) - placeholder
  - `build-windows-installer` (MSI) - placeholder

### ✅ Release Workflow (release-files.yml)
- Already correctly configured to download and upload jpackage artifacts
- No changes needed - artifact names are consistent
- Handles both snapshot and release modes

### ✅ Version Normalization
- Added `jpackage.app.version` property (3.4.0) to all jpackage profiles
- Resolves jpackage version format requirements (numeric-only)

### ✅ Documentation
- Created local test script: `scripts/test-jpackage-local.sh`
- Updated JPACKAGE.md with usage instructions (from previous session)

## TODO: Platform Installers

The installer jobs (`build-linux-installers`, `build-macos-installer`, `build-windows-installer`) are currently placeholders. Full implementation requires:

### Linux Installers (DEB/RPM)

**Requirements:**
1. Activate jpackage-deb and jpackage-rpm profiles in maven-build.yml
2. Full Maven build environment (same as app-image jobs)
3. Package assets in `package_files/`:
   - `hdfview.png` (icon)
   - File association properties files

**Implementation approach:**
```yaml
- name: Create DEB Installer
  run: |
    # Download HDF libraries (same as app job)
    # Set up build.properties
    # Install HDF JARs to Maven repo
    # Run jpackage with DEB profile
    mvn verify -Pjpackage-deb -pl object,hdfview \
      -Dmaven.test.skip=true \
      -Djacoco.skip=true \
      -Dpmd.skip=true \
      -Ddependency-check.skip=true \
      -B

    # Upload: hdfview/target/dist/hdfview_3.4.0-1_amd64.deb
```

**Code signing (optional):**
- Linux packages can be signed with GPG
- Only needed for official HDFGroup releases
- Add conditional check: `if: github.repository == 'HDFGroup/hdfview'`

### macOS Installer (DMG)

**Requirements:**
1. Activate jpackage-installer-mac profile in maven-build.yml
2. Full Maven build environment
3. Package assets in `package_files/`:
   - File association properties files
4. **Code signing certificates** (required for distribution)
5. **Apple notarization** (required for macOS 10.15+)

**Code signing setup:**
```yaml
- name: Import Code Signing Certificate
  if: github.repository == 'HDFGroup/hdfview' && secrets.MACOS_CERTIFICATE
  env:
    MACOS_CERTIFICATE: ${{ secrets.MACOS_CERTIFICATE }}
    MACOS_CERTIFICATE_PWD: ${{ secrets.MACOS_CERTIFICATE_PWD }}
  run: |
    # Import certificate to keychain
    # Set up codesign identity
```

**Notarization setup:**
```yaml
- name: Notarize DMG
  if: github.repository == 'HDFGroup/hdfview' && secrets.APPLE_ID
  env:
    APPLE_ID: ${{ secrets.APPLE_ID }}
    APPLE_PASSWORD: ${{ secrets.APPLE_PASSWORD }}
    APPLE_TEAM_ID: ${{ secrets.APPLE_TEAM_ID }}
  run: |
    # Submit DMG to Apple notarization service
    # Wait for notarization to complete
    # Staple notarization ticket to DMG
```

### Windows Installer (MSI)

**Requirements:**
1. Activate jpackage-installer-windows profile in maven-build.yml
2. Full Maven build environment
3. Package assets in `package_files/`:
   - File association properties files
4. **Code signing certificate** (recommended for distribution)

**Code signing setup:**
```yaml
- name: Sign MSI
  if: github.repository == 'HDFGroup/hdfview' && secrets.WINDOWS_CERTIFICATE
  env:
    WINDOWS_CERTIFICATE: ${{ secrets.WINDOWS_CERTIFICATE }}
    WINDOWS_CERTIFICATE_PWD: ${{ secrets.WINDOWS_CERTIFICATE_PWD }}
  shell: pwsh
  run: |
    # Import certificate
    # Sign MSI with signtool
    signtool sign /f certificate.pfx /p $env:WINDOWS_CERTIFICATE_PWD /tr http://timestamp.digicert.com HDFView-3.4.0.msi
```

## TODO: Code Signing for Canonical Repository

### GitHub Secrets Required

For the canonical `HDFGroup/hdfview` repository:

**macOS:**
- `MACOS_CERTIFICATE` - Developer ID Application certificate (base64 encoded)
- `MACOS_CERTIFICATE_PWD` - Certificate password
- `APPLE_ID` - Apple Developer account email
- `APPLE_PASSWORD` - App-specific password for notarization
- `APPLE_TEAM_ID` - Apple Developer Team ID

**Windows:**
- `WINDOWS_CERTIFICATE` - Authenticode certificate (base64 encoded or PFX)
- `WINDOWS_CERTIFICATE_PWD` - Certificate password

**Linux (optional):**
- `GPG_PRIVATE_KEY` - GPG private key for signing DEB/RPM
- `GPG_PASSPHRASE` - GPG key passphrase

### Conditional Signing Logic

All signing steps should be conditional:
```yaml
if: github.repository == 'HDFGroup/hdfview'
```

This ensures:
- Forks can build without signing (for testing/development)
- Official releases are properly signed
- No signing errors on PR builds from forks

## Current Release Flow

### Daily Build (daily-build.yml)
1. Calls `maven-build.yml` with `use_environ: snapshots`
2. Creates jpackage app-images for all platforms
3. Uploads to GitHub release tag: `HDFView-99.99.99` (snapshot)
4. **Status: ✅ Ready to test**

### Release Build (release.yml)
Two modes:

**Snapshot mode** (default):
1. Calls `maven-build.yml` with `use_environ: release`
2. Creates jpackage app-images for all platforms
3. Uploads to GitHub release tag: `snapshot`
4. **Status: ✅ Ready to test**

**Release mode**:
1. Calls `maven-build.yml` with `use_environ: release`
2. Creates jpackage app-images for all platforms
3. Uploads to GitHub release tag: `HDFView-{version}` (e.g., HDFView-3.4.0)
4. **Status: ✅ Ready to test**

## Next Steps

1. **Test current integration:**
   - Run daily-build.yml manually to verify end-to-end flow
   - Verify artifacts are uploaded to snapshot tag
   - Download and test app-images on each platform

2. **Implement installer creation** (optional, can defer):
   - Add full environment setup to installer jobs
   - Test DEB/RPM creation on Linux
   - Test DMG creation on macOS (without signing initially)
   - Test MSI creation on Windows (without signing initially)

3. **Set up code signing** (for official releases only):
   - Obtain signing certificates from HDF Group
   - Add secrets to HDFGroup/hdfview repository
   - Implement conditional signing logic
   - Test notarization for macOS DMG

## Testing Recommendations

### For Fork/Development Testing
- Run `scripts/test-jpackage-local.sh` to verify local builds
- Test CI workflows on feature branch
- Verify app-images can be extracted and run

### For Official Release Testing
- Test daily-build.yml on a test branch first
- Verify artifact naming matches expected format
- Test app-images on clean systems without development tools
- Verify installers (once implemented) install and run correctly

## Notes

- App-images are fully functional and distributable (no installers required)
- Users can extract app-images and run directly
- Platform installers (DEB/RPM/DMG/MSI) are nice-to-have for easier installation
- Code signing is required for macOS DMG distribution (Gatekeeper)
- Code signing is recommended but not required for Windows MSI
