# Installer Signing Guide

This document describes the code signing process for HDFView installers on Windows and macOS platforms.

## Overview

HDFView installers must be code-signed to ensure users can trust the software and avoid security warnings. The signing process differs between platforms:

- **Windows**: Sign `.exe` installer with Authenticode certificate
- **macOS**: Sign `.app` bundle and `.dmg`/`.pkg` installers, then notarize with Apple

## Required Secrets

### Windows Signing

The following GitHub secrets must be configured in the canonical repository:

- `WINDOWS_CERTIFICATE`: Base64-encoded code signing certificate (.pfx file)
- `WINDOWS_CERT_PASSWORD`: Password for the certificate
- `WINDOWS_SIGNTOOL_PATH`: Path to signtool.exe (usually in Windows SDK)

### macOS Signing and Notarization

The following GitHub secrets and repository variables must be configured:

**Secrets (encrypted):**
- `APPLE_CERTS_BASE64`: Base64-encoded Apple Developer ID Application certificate (.p12)
- `APPLE_CERTS_BASE64_PASSWD`: Password for the certificate
- `KEYCHAIN_PASSWD`: Temporary keychain password (can be any secure value)

**Repository Variables (plain text):**
- `KEYCHAIN_NAME`: Name for the temporary keychain (e.g., "build")
- `SIGNER`: 10-character Team ID (e.g., "465P44SP96")
- `NOTARY_USER`: Apple ID email for notarization
- `NOTARY_KEY`: App-specific password for notarization (generate at appleid.apple.com)

## Windows Signing Process

### Steps

1. **Install certificate** from secret to temporary location
2. **Sign the installer** using signtool.exe:
   ```bash
   signtool sign /v /debug /fd SHA256 \
     /d "HDFView ${VERSION} Utility" \
     /f certificate.pfx \
     /p ${PASSWORD} \
     /t http://timestamp.digicert.com \
     installer.exe
   ```
3. **Verify signature**:
   ```bash
   signtool verify /pa /v installer.exe
   ```

### Notes

- Uses SHA256 hash algorithm
- Timestamp server: http://timestamp.digicert.com (DigiCert)
- Signing adds Authenticode signature to the .exe
- Only runs on Windows runners
- Only runs when secrets are available (canonical repository)

## macOS Signing Process

### Overview

macOS signing uses jpackage's built-in `--mac-sign` option to sign all binaries (dylibs, frameworks, executables) during app-image creation. This ensures proper code signing with timestamps and hardened runtime.

### Steps

1. **Create temporary keychain**:
   ```bash
   security -v create-keychain -p ${KEYCHAIN_PASSWD} ${KEYCHAIN_NAME}.keychain
   security -v list-keychain -d user -s ${KEYCHAIN_NAME}.keychain
   security -v unlock-keychain -p ${KEYCHAIN_PASSWD} ${KEYCHAIN_NAME}.keychain
   security -v set-keychain-settings -lut 21600 ${KEYCHAIN_NAME}.keychain
   ```

2. **Import certificate**:
   ```bash
   echo $APPLE_CERTS_BASE64 | base64 --decode > certificate.p12
   security -v import certificate.p12 -P ${APPLE_CERTS_BASE64_PASSWD} \
     -A -t cert -f pkcs12 -k ${KEYCHAIN_NAME}.keychain
   security -v set-key-partition-list -S apple-tool:,codesign:,apple: \
     -k ${KEYCHAIN_PASSWD} ${KEYCHAIN_NAME}.keychain
   ```

3. **Prepare jpackage input directory** (manually copy JARs and dylibs):
   ```bash
   mkdir -p hdfview/target/jpackage-input
   cp libs/hdfview-*.jar hdfview/target/jpackage-input/
   cp libs/object-*.jar hdfview/target/jpackage-input/
   cp hdfview/target/lib/*.jar hdfview/target/jpackage-input/
   cp ${HDF5LIB_PATH}/lib/*.dylib hdfview/target/jpackage-input/
   # ... copy other dependencies
   ```

4. **Create signed app-image with jpackage**:
   ```bash
   security unlock-keychain -p ${KEYCHAIN_PASSWD} ${KEYCHAIN_NAME}.keychain

   jpackage \
     --verbose \
     --name HDFView \
     --input hdfview/target/jpackage-input \
     --main-jar hdfview-99.99.99.jar \
     --main-class hdf.view.HDFView \
     --dest hdfview/target/dist \
     --type app-image \
     --app-version 99.99.99 \
     --mac-sign \
     --mac-package-identifier HDFView.hdfgroup.org \
     --mac-package-name HDFView-99.99.99 \
     --mac-package-signing-prefix org.hdfgroup.HDFView \
     --mac-signing-key-user-name "The HDF Group (${SIGNER})"
   ```

5. **Verify signature**:
   ```bash
   codesign -vvv --deep --strict hdfview/target/dist/HDFView.app
   ```

### Creating and Notarizing DMG Installer

1. **Create DMG from signed app-image**:
   ```bash
   jpackage \
     --type dmg \
     --app-image HDFView.app \
     --name HDFView \
     --app-version 99.99.99 \
     --mac-package-identifier HDFView.hdfgroup.org \
     --mac-package-name "HDFView-99.99.99" \
     --file-associations package_files/HDFViewHDF.properties \
     --file-associations package_files/HDFViewH4.properties \
     --file-associations package_files/HDFViewHDF4.properties \
     --file-associations package_files/HDFViewH5.properties \
     --file-associations package_files/HDFViewHDF5.properties \
     --dest .
   ```

   Note: The DMG inherits signatures from the signed app-image.

2. **Submit for notarization**:
   ```bash
   xcrun notarytool submit "HDFView-99.99.99.dmg" \
     --apple-id "${NOTARY_USER}" \
     --password "${NOTARY_KEY}" \
     --team-id "${SIGNER}" \
     --wait \
     --timeout 30m \
     --output-format json > notarization-response.json
   ```

3. **Check notarization status and fetch logs on failure**:
   ```bash
   STATUS=$(cat notarization-response.json | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
   SUBMISSION_ID=$(cat notarization-response.json | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

   if [ "$STATUS" != "Accepted" ]; then
     echo "Notarization failed with status: $STATUS"
     xcrun notarytool log "$SUBMISSION_ID" \
       --apple-id "${NOTARY_USER}" \
       --password "${NOTARY_KEY}" \
       --team-id "${SIGNER}"
     exit 1
   fi
   ```

4. **Staple notarization ticket**:
   ```bash
   xcrun stapler staple "HDFView-99.99.99.dmg"
   xcrun stapler validate "HDFView-99.99.99.dmg"
   ```

5. **Verify Gatekeeper acceptance** (optional):
   ```bash
   spctl -vvvv --assess --type install "HDFView-99.99.99.dmg"
   ```

### Notes

- **Critical**: All binaries must be signed DURING jpackage app-image creation using `--mac-sign`
  - Manual signing with `codesign --deep` after creation does NOT work reliably
  - jpackage ensures proper signing of all nested binaries (dylibs, frameworks, executables)
- Hardened runtime and timestamps are automatically applied by jpackage
- Notarization can take 5-30 minutes (`--wait` flag blocks until complete)
- Stapling embeds the notarization ticket in the DMG for offline verification
- Repository variables (SIGNER, NOTARY_USER, etc.) are used instead of old APPLE_ID variables
- Only runs on macOS runners
- Only runs when secrets are available (canonical repository)

## Entitlements

The macOS entitlements file (`lib/macosx/distribution.entitlements`) grants permissions needed for HDFView to run with the hardened runtime:

- `com.apple.security.cs.allow-jit`: Allow JIT compilation (required for Java)
- `com.apple.security.cs.allow-unsigned-executable-memory`: Allow unsigned code in memory
- `com.apple.security.cs.disable-executable-page-protection`: Disable page protection
- `com.apple.security.cs.disable-library-validation`: Allow loading unsigned libraries
- `com.apple.security.cs.allow-dyld-environment-variables`: Allow DYLD environment variables

These entitlements are required for Java applications to run properly under macOS hardened runtime.

## Conditional Signing

Signing is only performed when:

1. Running in the canonical repository (not forks)
2. Required secrets are available
3. Building installers (not binary archives)

This is implemented using workflow conditions:

```yaml
if: github.repository == 'HDFGroup/hdfview' && env.CERTIFICATE != ''
```

## Testing

### Local Testing (Without Signing)

- Installers can be built without signing for testing
- Windows: .exe will work but show "Unknown Publisher" warning
- macOS: .app/.dmg will show "Unidentified Developer" warning
- Users can bypass warnings, but signed installers provide better user experience

### Fork Testing

- Forks will skip signing steps (secrets not available)
- Installers will be created but unsigned
- Useful for testing installer creation process

### Canonical Testing

- Only canonical repository has signing secrets
- Full signing and notarization will occur
- Installers will be production-ready

## Troubleshooting

### Windows

**Problem**: signtool.exe not found
**Solution**: Ensure WINDOWS_SIGNTOOL_PATH points to correct SDK location

**Problem**: Certificate password incorrect
**Solution**: Verify WINDOWS_CERT_PASSWORD secret is correct

**Problem**: Timestamp server timeout
**Solution**: Retry signing; DigiCert timestamp server is usually reliable

### macOS

**Problem**: Keychain unlock fails
**Solution**: Ensure keychain password is correct and keychain was created successfully

**Problem**: Codesign fails with "ambiguous identity"
**Solution**: Ensure MACOS_DEVELOPER_ID exactly matches certificate identity

**Problem**: Notarization fails
**Solution**: Check Apple ID password (must be app-specific password, not account password)

**Problem**: Notarization pending for long time
**Solution**: Wait up to 15 minutes; Apple's notarization service can be slow

**Problem**: Stapling fails
**Solution**: Ensure notarization completed successfully; check UUID status

## References

- [Windows Code Signing with SignTool](https://docs.microsoft.com/en-us/windows/win32/seccrypto/signtool)
- [macOS Code Signing](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution)
- [Apple Notarization Guide](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution)
- [Hardened Runtime Entitlements](https://developer.apple.com/documentation/bundleresources/entitlements)
