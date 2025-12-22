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

The following GitHub secrets must be configured:

- `MACOS_CERTIFICATE`: Base64-encoded Apple Developer ID Application certificate (.p12)
- `MACOS_CERT_PASSWORD`: Password for the certificate
- `MACOS_KEYCHAIN_PASSWORD`: Temporary keychain password (can be any secure value)
- `MACOS_DEVELOPER_ID`: Developer ID/Team ID (format: "Developer ID Application: YourName (TEAMID)")
- `APPLE_ID`: Apple ID email for notarization
- `APPLE_ID_PASSWORD`: App-specific password for notarization (generate at appleid.apple.com)
- `APPLE_TEAM_ID`: 10-character Team ID

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

### For .app Bundles

1. **Create temporary keychain**:
   ```bash
   security create-keychain -p ${KEYCHAIN_PASSWORD} build.keychain
   security unlock-keychain -p ${KEYCHAIN_PASSWORD} build.keychain
   security set-keychain-settings build.keychain
   ```

2. **Import certificate**:
   ```bash
   security import certificate.p12 -k build.keychain \
     -P ${CERT_PASSWORD} -T /usr/bin/codesign
   ```

3. **Set partition list** (allow codesign to access):
   ```bash
   security set-key-partition-list -S apple-tool:,apple: \
     -k ${KEYCHAIN_PASSWORD} build.keychain
   ```

4. **Code sign the app**:
   ```bash
   codesign --force --timestamp --options runtime \
     --entitlements lib/macosx/distribution.entitlements \
     --verbose=4 --strict \
     --sign "${DEVELOPER_ID}" \
     --deep \
     HDFView.app
   ```

5. **Verify signature**:
   ```bash
   codesign -dvv HDFView.app
   codesign -vvvv --strict HDFView.app
   ```

### For .dmg or .pkg Installers

1. **Create DMG/PKG** (already done by jpackage)

2. **Code sign the installer**:
   ```bash
   codesign --force --timestamp --options runtime \
     --entitlements lib/macosx/distribution.entitlements \
     --verbose=4 --strict \
     --sign "${DEVELOPER_ID}" \
     --deep \
     HDFView-${VERSION}.dmg
   ```

3. **Verify DMG**:
   ```bash
   codesign -dvv HDFView-${VERSION}.dmg
   hdiutil verify HDFView-${VERSION}.dmg
   ```

4. **Submit for notarization**:
   ```bash
   xcrun notarytool submit \
     --wait \
     --output-format json \
     --apple-id "${APPLE_ID}" \
     --password "${APPLE_ID_PASSWORD}" \
     --team-id "${TEAM_ID}" \
     HDFView-${VERSION}.dmg
   ```

5. **Staple notarization ticket**:
   ```bash
   xcrun stapler staple HDFView-${VERSION}.dmg
   xcrun stapler validate -v HDFView-${VERSION}.dmg
   ```

6. **Verify Gatekeeper**:
   ```bash
   spctl -vvvv --assess --type install HDFView-${VERSION}.dmg
   ```

### Notes

- Entitlements file required: `lib/macosx/distribution.entitlements`
- Hardened runtime required for notarization
- Notarization can take several minutes (--wait flag waits for completion)
- Stapling embeds the notarization ticket in the DMG for offline verification
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
