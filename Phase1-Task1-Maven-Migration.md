# Phase 1 Task 1: Complete Maven Migration and Remove Ant Build System

## Overview
Remove all Ant build infrastructure and ensure Maven handles all build tasks currently performed by the legacy Ant system.

## Current State Analysis

### Ant Build Targets Identified (75+ targets)
The `build.xml` file contains extensive build automation including:

**Core Build Tasks:**
- `clean`, `compile`, `jar`, `objectjar`, `modulejar`
- Platform-specific compilation: `compilehdf4`, `compilehdf5`, `compilefits`, `compilenc2`
- Version extraction from native library settings files

**Testing Infrastructure:**
- `junit`, `junit-uitest`, `junit-uimodules` 
- `jacoco` code coverage integration
- `sonar` static analysis integration
- Individual test execution capabilities

**Deployment & Packaging:**
- Platform-specific deployment: `deployWindows`, `deployUnix`, `deployMac`
- JPackage integration: `createJPackage*` targets for native installers
- Binary packaging: `binaryPack*` targets
- Code signing and notarization for macOS

**Documentation:**
- `javadoc` generation
- `createREADME*` variants for different platforms
- User guide compression (`createDocumentationTGZ`)

**Development Utilities:**
- `run`, `rundebug` for development testing
- `run-examples` for testing examples
- Property file generation

## Detailed Task Breakdown

### Task 1.1: Audit Ant Dependencies (1 day)

#### 1.1.1 Document Ant Build Capabilities
- [ ] **Map Ant targets to Maven equivalents** (2 hours)
  - Create mapping table: Ant target → Maven goal/plugin
  - Identify gaps where Maven plugins are needed
  - Document custom Ant tasks that need recreation

- [ ] **Inventory build properties usage** (2 hours)
  - Document how `build.properties` is used in Ant build
  - List all properties referenced in `build.xml` 
  - Identify properties that need Maven profile/property equivalents

- [ ] **Analyze platform-specific logic** (2 hours)
  - Document OS detection logic (`isWindows`, `isUnix`, `isMac` properties)
  - Map to Maven profiles activation
  - Identify platform-specific file operations

- [ ] **Document external dependencies** (2 hours)
  - JavaFX Ant tasks (`xmlns:fx`)
  - Jacoco Ant tasks (`xmlns:jacoco`) 
  - Sonar Ant tasks (`xmlns:sonar`)
  - Doxygen integration (`xmlns:doxygen`)

#### 1.1.2 Identify Maven Plugin Requirements
- [ ] **Research Maven plugins needed** (1 hour)
  - `maven-compiler-plugin` (already present)
  - `maven-surefire-plugin` for testing (already present)
  - `maven-failsafe-plugin` for integration tests
  - `jacoco-maven-plugin` for code coverage
  - `sonar-maven-plugin` for static analysis
  - `maven-javadoc-plugin` for documentation
  - `jpackage-maven-plugin` or equivalent for native packaging

#### 1.1.3 Risk Assessment
- [ ] **Identify critical build features** (1 hour)
  - Native library version extraction from `.settings` files
  - Platform-specific native library path handling
  - Code signing and notarization workflows
  - Custom property file generation

### Task 1.2: Migrate Missing Build Tasks (2-3 days)

#### 1.2.1 Version Information Extraction (4 hours)
- [ ] **Migrate VERSION file reading**
  - Current: Ant `loadfile` task with tokenfilter
  - Target: Maven `buildnumber-maven-plugin` or properties-maven-plugin
  - Ensure app.version property available to all modules

- [ ] **Migrate native library version extraction**
  - Current: Parses `libhdf4.settings` and `libhdf5.settings`
  - Target: Custom Maven plugin or exec-maven-plugin with script
  - Extract HDF4/HDF5 version numbers for build metadata

#### 1.2.2 Testing Infrastructure Migration (6 hours)
- [ ] **Migrate basic JUnit execution** (2 hours)
  - Ensure `maven-surefire-plugin` handles unit and object tests
  - Migrate test JVM arguments for module access
  - Configure test classpath with native libraries
  - **Defer SWTBot UI testing to Phase 2**

- [ ] **Migrate code coverage** (2 hours)
  - Replace Jacoco Ant tasks with `jacoco-maven-plugin`
  - Configure coverage reports (HTML, XML)
  - Ensure coverage data collection during tests

- [ ] **Basic Sonar integration setup** (2 hours)
  - Add `sonar-maven-plugin` to parent POM
  - Configure basic analysis properties
  - Ensure integration with coverage data
  - **Note: Full Sonar integration will be completed in Task 4**

#### 1.2.3 Documentation Generation (4 hours)
- [ ] **Migrate JavaDoc generation**
  - Configure `maven-javadoc-plugin`
  - Migrate custom JavaDoc settings from Ant
  - Configure multi-module JavaDoc aggregation

- [ ] **Migrate README generation**
  - Convert Ant-based README generation to Maven
  - Use `maven-resources-plugin` with filtering
  - Generate platform-specific READMEs

#### 1.2.4 Packaging and Distribution (10 hours)
- [ ] **Configure JPackage Maven integration** (6 hours)
  - Add `org.panteleyev:jpackage-maven-plugin` to parent POM
  - Configure platform-specific packaging profiles
  - Migrate installer creation logic from Ant
  - Configure application icons and metadata

- [ ] **Configure macOS code signing plugins** (2 hours)
  - Add `org.apache.maven.plugins:maven-antrun-plugin` for signing scripts
  - Configure keychain access and certificate handling
  - Set up notarization workflow integration

- [ ] **Migrate deployment structure** (2 hours)
  - Configure `maven-dependency-plugin` for lib copying
  - Ensure proper JAR output locations
  - Configure manifest and main class settings

### Task 1.3: Remove Ant Files (0.5 days)

#### 1.3.1 File Removal (2 hours)
- [ ] **Remove build.xml**
  - Delete main `build.xml` file
  - Remove any additional Ant build files

- [ ] **Clean up properties files** (1 hour)
  - Remove Ant-specific properties from `build.properties`
  - Keep properties needed by Maven build
  - Document property migration in CLAUDE.md

#### 1.3.2 Documentation Update (2 hours)
- [ ] **Update CLAUDE.md**
  - Remove references to Ant build
  - Update build commands section
  - Document new Maven-only workflow

- [ ] **Update README files**
  - Remove Ant build instructions
  - Ensure only Maven build process documented

### Task 1.4: Remove Maven-Antrun Dependencies (1 day)

#### 1.4.1 Remove Antrun Plugin (2 hours)
- [ ] **Remove maven-antrun-plugin from hdfview/pom.xml**
  - Delete plugin configuration at lines 211-226
  - Remove `exportAntProperties` usage
  - Update compiler plugin classpath configuration

#### 1.4.2 Fix Compiler Configuration (4 hours)
- [ ] **Update maven-compiler-plugin**
  - Remove Ant property dependencies (`${cp}${path.separator}`)
  - Configure proper Maven classpath handling
  - Test compilation without Ant integration

#### 1.4.3 Verification Testing (2 hours)
- [ ] **Test complete build process**
  - Run `mvn clean install` on all modules
  - Verify all tests pass
  - Ensure application launches correctly

## Implementation Decisions (Based on Clarifications)

1. **✅ Native Library Version Extraction**: Keep parsing `.settings` files with custom Maven plugin or exec-maven-plugin
2. **✅ Platform-Specific Packaging**: Implement full JPackage Maven integration now
3. **✅ Code Signing**: Use Maven plugins for macOS code signing and notarization
4. **✅ Sonar Integration**: Basic setup now, full integration deferred to Task 4 (Static Analysis)
5. **✅ Testing Scope**: Basic JUnit migration only, defer SWTBot UI testing to Phase 2

## Success Criteria

- [ ] `build.xml` and all Ant files removed from repository
- [ ] `mvn clean install` builds entire project successfully  
- [ ] All existing functionality preserved (application launches and works)
- [ ] All tests pass with Maven execution
- [ ] No Ant runtime dependencies remain
- [ ] Build time comparable to or better than Ant build
- [ ] Documentation updated to reflect Maven-only process

## Updated Estimated Timeline
- **Total**: 5.5-6.5 days (increased due to JPackage and signing integration)
- **Dependencies**: Must complete before other Phase 1 tasks
- **Risk Level**: Medium-High (extensive build system changes + native packaging)

## Rollback Plan
- Maintain git branch with working Ant build
- Test Maven build extensively before Ant removal
- Document any functionality gaps before proceeding