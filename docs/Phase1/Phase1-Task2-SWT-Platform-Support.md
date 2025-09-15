# Phase 1 Task 2: Fix Platform-Specific SWT Dependency Resolution

## Overview
Enable proper cross-platform SWT dependency resolution for Linux, Windows, and macOS (including ARM64 support) by activating and fixing the currently commented-out Maven profiles.

## Current State Analysis

### Current SWT Configuration Issues
- **Commented-out profiles**: Platform-specific profiles in `hdfview/pom.xml:111-194` are disabled
- **Local dependencies**: Uses `org.eclipse.local` group instead of Maven Central
- **Single platform**: Currently hardcoded to Linux GTK (`gtk.linux.x86_64`)
- **Missing ARM64**: No Apple Silicon or Windows ARM support
- **Manual configuration**: Requires developers to manually manage platform dependencies

### Current Working Profile Structure
The existing (commented) profiles show proper structure for:
- **swt-unix**: OS family activation with `gtk.linux.${os.arch}` 
- **swt-mac**: Family activation with `cocoa.macosx.${os.arch}`
- **swt-windows**: Family activation with `win32.win32.${os.arch}`

## Detailed Task Breakdown

### Task 2.1: Uncomment and Fix SWT Profiles (1 day)

#### 2.1.1 Enable Existing Profiles (2 hours)
- [ ] **Uncomment platform profiles in hdfview/pom.xml**
  - Remove comment blocks around lines 111-194
  - Verify XML structure and indentation
  - Check profile activation syntax

- [ ] **Fix profile property resolution** (2 hours)
  - Verify `${os.arch}` property expansion works correctly
  - Test `os.family` activation on each platform
  - Fix any Maven profile syntax issues

#### 2.1.2 Update SWT Artifact References (2 hours)
- [ ] **Research current SWT versions**
  - Check latest stable SWT version on Maven Central
  - Verify availability of platform-specific artifacts
  - Document version compatibility with current project

- [ ] **Update artifact IDs and versions**
  - Replace `${swt.version}` property references
  - Update parent POM with correct SWT version
  - Verify artifact naming conventions match Maven Central

#### 2.1.3 Test Profile Activation (2 hours)
- [ ] **Verify profile activation logic**
  - Test profile activation on available platforms
  - Use `mvn help:active-profiles` to verify detection
  - Debug any activation issues

### Task 2.2: Configure Platform-Specific Dependencies (2 days)

#### 2.2.1 Linux Platform Configuration (2 hours)
- [ ] **Configure GTK Linux x86_64**
  - Artifact ID: `org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64`
  - Test on current Linux development environment
  - Verify UI functionality

- [ ] **Add Linux ARM64 support** (1 hour)
  - Artifact ID: `org.eclipse.platform:org.eclipse.swt.gtk.linux.aarch64`  
  - Configure profile for ARM64 detection
  - Add to swt-unix profile with architecture detection

#### 2.2.2 Windows Platform Configuration (3 hours)
- [ ] **Configure Windows x86_64** (1.5 hours)
  - Artifact ID: `org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64`
  - Update swt-windows profile activation
  - Configure Windows-specific properties

- [ ] **Add Windows ARM64 support** (1.5 hours)
  - Artifact ID: `org.eclipse.platform:org.eclipse.swt.win32.win32.aarch64`
  - Add architecture detection to Windows profile
  - Configure ARM64-specific activation

#### 2.2.3 macOS Platform Configuration (3 hours)
- [ ] **Configure macOS Intel (x86_64)** (1.5 hours)
  - Artifact ID: `org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64`
  - Update swt-mac profile for Intel Macs
  - Test Cocoa integration properties

- [ ] **Configure macOS Apple Silicon (ARM64)** (1.5 hours)
  - Artifact ID: `org.eclipse.platform:org.eclipse.swt.cocoa.macosx.aarch64`
  - Add architecture detection for Apple Silicon
  - Ensure proper ARM64 activation logic

### Task 2.3: Replace Local SWT Dependencies (1 day)

#### 2.3.1 Remove Local Repository Dependencies (3 hours)
- [ ] **Audit current local dependencies** (1 hour)
  - Document all `org.eclipse.local` group dependencies
  - Identify which are available on Maven Central
  - Map local versions to Maven Central equivalents

- [ ] **Replace SWT core dependency** (1 hour)
  - Remove `org.eclipse.local:org.eclipse.swt` dependency
  - Ensure platform profiles provide SWT implementation
  - Test that no direct SWT dependency is needed

- [ ] **Replace related Eclipse dependencies** (1 hour)
  - Update NatTable dependency if using local version
  - Update SWTBot dependencies to Maven Central versions
  - Remove local Eclipse JFace exclusions if no longer needed

#### 2.3.2 Update Parent POM Properties (2 hours)
- [ ] **Update version properties** (1 hour)
  - Set `swt.version` to latest stable (e.g., 3.124.0)
  - Remove local repository references
  - Add version management to parent POM

- [ ] **Configure dependency management** (1 hour)  
  - Add SWT platform dependencies to parent POM dependencyManagement
  - Ensure consistent versions across all modules
  - Configure exclusions for transitive dependencies

#### 2.3.3 Clean Up Repository Configuration (1 hour)
- [ ] **Remove local repository references**
  - Remove `id-local` repository from hdfview/pom.xml:22-26
  - Clean up any remaining local repository configuration
  - Verify Maven Central is sufficient for all dependencies

### Task 2.4: Test Multi-Platform Builds (1 day)

#### 2.4.1 Local Build Testing (3 hours)
- [ ] **Test Linux build** (1 hour)
  - Run `mvn clean install` on current Linux environment
  - Verify correct SWT artifact is resolved
  - Test application launch and basic UI functionality

- [ ] **Simulate other platform builds** (2 hours)
  - Use Maven profile activation to test Windows/macOS artifact resolution
  - Run `mvn dependency:tree` to verify platform dependencies
  - Use `mvn clean install -P swt-windows` to test Windows profile

#### 2.4.2 Cross-Platform Validation (2 hours)
- [ ] **Verify dependency resolution per platform** (1 hour)
  - Document expected artifacts for each platform/architecture combo
  - Create test script to validate profile activation
  - Verify no dependency conflicts between platforms

- [ ] **Test application startup per platform** (1 hour)
  - Ensure JAR includes correct platform-specific SWT
  - Test basic window creation and widget functionality
  - Verify no platform-specific runtime errors

#### 2.4.3 Documentation and Build Commands (3 hours)
- [ ] **Document platform-specific build commands** (1.5 hours)
  - Create platform build instructions for developers
  - Document how to build for specific platforms
  - Add troubleshooting guide for common platform issues

- [ ] **Update CLAUDE.md** (1.5 hours)
  - Remove references to single-platform configuration
  - Document new multi-platform build process
  - Add platform-specific testing instructions

## Architecture Considerations

### Profile Activation Strategy
```xml
<profile>
  <id>swt-unix</id>
  <activation>
    <os>
      <family>unix</family>
      <arch>x86_64</arch>
    </os>
  </activation>
  <dependencies>
    <dependency>
      <groupId>org.eclipse.platform</groupId>
      <artifactId>org.eclipse.swt.gtk.linux.x86_64</artifactId>
    </dependency>
  </dependencies>
</profile>
```

### Dependency Management Structure
- **Parent POM**: Defines SWT version and platform artifact versions
- **Module POM**: Includes platform profiles with conditional dependencies
- **Runtime**: Single JAR contains appropriate platform-specific SWT

### Architecture Detection Logic
- **Linux**: `os.family=unix` + `os.name!=Mac OS X` 
- **Windows**: `os.family=windows`
- **macOS**: `os.family=mac` or `os.name=Mac OS X`
- **ARM64**: `os.arch=aarch64`
- **x86_64**: `os.arch=x86_64` or `os.arch=amd64`

## Risk Assessment and Mitigation

### High Risk Items
1. **Maven Profile Activation**: Different Maven versions may have different OS detection
   - **Mitigation**: Test on multiple Maven versions, document requirements
2. **SWT Version Compatibility**: Newer SWT versions may break existing code
   - **Mitigation**: Start with current working version, increment carefully
3. **Platform Testing**: Limited access to all target platforms
   - **Mitigation**: Use CI/CD for cross-platform validation in Phase 2

### Medium Risk Items
1. **Dependency Conflicts**: Platform-specific dependencies may conflict
   - **Mitigation**: Use dependencyManagement and exclusions
2. **Architecture Detection**: ARM64 detection may vary across systems
   - **Mitigation**: Test architecture detection thoroughly, provide fallbacks

## Success Criteria

- [ ] Maven profiles automatically select correct SWT dependencies based on platform
- [ ] Application builds successfully on Linux x86_64 (primary test platform)  
- [ ] Dependency resolution works for all target platforms (Windows, macOS, Linux)
- [ ] Both x86_64 and ARM64 architectures supported for Windows and macOS
- [ ] No manual platform configuration required by developers
- [ ] `mvn dependency:tree` shows correct platform-specific SWT artifact
- [ ] Application launches and displays UI correctly on test platform
- [ ] No local repository dependencies remain
- [ ] CI/CD can build for multiple platforms (preparation for Phase 2)

## Timeline and Dependencies

- **Total**: 5 days
- **Prerequisite**: Task 1 (Maven Migration) must be completed first
- **Can run in parallel**: Task 3 (build.properties enhancement)
- **Blocks**: Application packaging and distribution workflows

## Integration Notes

- **Task 1 Integration**: Profiles must work with new Maven-only build system
- **Task 3 Integration**: Platform detection should align with build.properties templates  
- **Task 4 Integration**: Static analysis should work across all supported platforms
- **Phase 2 Preparation**: Multi-platform builds enable CI/CD matrix builds

## Rollback Strategy

- Re-comment platform profiles if critical issues arise
- Keep local SWT dependencies as backup during transition
- Document working single-platform configuration as fallback
- Test rollback process before implementing changes