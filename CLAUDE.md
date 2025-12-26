# CLAUDE.md

This file provides guidance to Claude Code and other AI assistants when working with code in this repository.

## Project Overview

HDFView is a Java-based GUI application for viewing and editing HDF (Hierarchical Data Format) files, including HDF4, HDF5, and NetCDF formats. The project is developed by The HDF Group and uses Eclipse SWT for its user interface.

## Build System

The project uses **Maven** as the build system with a multi-module structure. The Ant build system was removed during Phase 1 migration - Maven is now the only supported build system.

### Build Commands

```bash
# Build the entire project
mvn clean compile

# Run tests
mvn test

# Package application (includes dependencies)
mvn package

# Package without tests (faster)
mvn clean package -DskipTests

# Install dependencies and build
mvn clean install
```

### Launcher Scripts

Cross-platform launcher scripts are provided for easy local execution:

- **`run-hdfview.sh`** - Unix/Linux/macOS launcher
- **`run-hdfview.bat`** - Windows launcher

Both scripts provide:
- Environment validation (Java, Maven, HDF libraries)
- Automatic property file parsing (`build.properties`)
- Automatic build if needed
- Three launch options:
  1. Maven exec:java (development)
  2. Direct JAR execution (recommended)
  3. Environment validation only

**Usage:**
```bash
# Unix/Linux/macOS
./run-hdfview.sh [--debug|--choose|--validate]

# Windows
run-hdfview.bat
```

### Development Helper Scripts

Additional scripts in `scripts/` directory for common development tasks:

#### Clean All (`scripts/clean-all.sh`)
Deep clean to pristine state - removes all build artifacts, Maven cache, and stale files.
Useful for resolving classloading issues and stale dependency problems.

```bash
./scripts/clean-all.sh
```

This removes:
- All Maven `target/` directories
- Maven local repository cache (`~/.m2/repository/org/hdfgroup/hdfview`)
- Maven resolver status files
- `libs/` directory
- Any stale `.class` files

#### Build for Development (`scripts/build-dev.sh`)
Quick build to ready-to-run state, skipping tests and quality checks by default.

```bash
# Quick development build
./scripts/build-dev.sh

# Build with tests
./scripts/build-dev.sh --with-tests

# Clean and build
./scripts/build-dev.sh --clean

# Full build with quality checks (slower)
./scripts/build-dev.sh --with-quality
```

The script:
1. Compiles object module
2. Packages object JAR to `libs/`
3. Installs to `~/.m2/repository`
4. Compiles hdfview module
5. Compiles test classes

#### Debug Logging Switcher (`scripts/set-debug-logging.sh`)
Quick switch between pre-configured logging levels for different debugging scenarios.

```bash
# List available configurations
./scripts/set-debug-logging.sh list

# Switch to Float16/BFLOAT16 debug logging
./scripts/set-debug-logging.sh float16

# Restore default minimal logging
./scripts/set-debug-logging.sh default
```

Debug configurations are stored in `.claude/debug-configs/` (local only, not in git).

**Common Workflow:**
```bash
# 1. Deep clean (first time or when having issues)
./scripts/clean-all.sh

# 2. Build for development
./scripts/build-dev.sh

# 3. Run HDFView
./run-hdfview.sh

# 4. Enable debug logging when needed
./scripts/set-debug-logging.sh float16
mvn test-compile -pl hdfview -DskipTests

# 5. Run tests
mvn test -pl hdfview -Dtest=TestClassName
```

### Key Build Configuration

- **Java Version**: Java 21 (set via `maven.compiler.source` and `maven.compiler.release`)
- **Build System**: Maven-only (Ant build removed in Phase 1 migration)
- **Build Properties**: External configuration loaded via properties-maven-plugin from `build.properties`
- **Native Libraries**: HDF4/HDF5 native libraries required (paths configured in `build.properties`)
- **Module System**: Disabled temporarily (non-modular build for SWT compatibility)

### Maven Plugins Integrated

- **Properties Plugin**: External property file loading (`build.properties`)
- **Resources Plugin**: Copies resources from both `src/main/resources` and `src/main/java` (icons, images)
- **Dependency Plugin**: Copies runtime dependencies to `hdfview/target/lib/` during package phase
- **Exec Plugin**: Native library version extraction, application execution, and jpackage installer creation
- **JaCoCo Plugin**: Code coverage analysis and reporting (60% line, 50% branch coverage targets)
- **JavaDoc Plugin**: API documentation generation with multi-module aggregation
- **Surefire Plugin**: JUnit 5 test execution with parallel test support
- **PMD Plugin**: Static code analysis (v7.0+, Java 21 compatible)
- **Checkstyle Plugin**: Code style enforcement (v10.12+, Java 21 compatible)
- **OWASP Dependency Check**: Security vulnerability scanning
- **License Plugin**: License compliance checking

### Distribution and Installers

HDFView uses Java 21's native jpackage tool to create platform-specific installers:

**Supported Formats:**
- **Windows**: MSI installer with Authenticode signing
- **macOS**: DMG disk image with code signing and notarization
- **Linux**: DEB and RPM packages

**jpackage Profiles:**
- `jpackage-app-image` - Creates application bundle (default for CI)
- `jpackage-installer-deb` - Creates DEB package
- `jpackage-installer-rpm` - Creates RPM package
- `jpackage-installer-mac` - Adds macOS-specific options for DMG
- `jpackage-installer-windows` - Adds Windows-specific options for MSI

**Creating Installers Locally:**
```bash
# Create app-image (platform application bundle)
mvn verify -Pjpackage-app-image -pl object,hdfview -DskipTests

# Create platform installer (requires app-image first)
# Linux: DEB
mvn package -Pjpackage-installer-deb -Djpackage.type=deb -pl hdfview

# Linux: RPM
mvn package -Pjpackage-installer-rpm -Djpackage.type=rpm -pl hdfview

# macOS: DMG (unsigned)
mvn package -Pjpackage-installer-mac -Djpackage.type=dmg -pl hdfview

# Windows: MSI (unsigned)
mvn package -Pjpackage-installer-windows -Djpackage.type=msi -pl hdfview
```

**Code Signing:**
- Signing only occurs in GitHub Actions on canonical repository
- Requires secrets configured (see `docs/Installer-Signing-Guide.md`)
- Forks can build unsigned installers for testing
- macOS requires Developer ID certificate and notarization with Apple

## Architecture

### Module Structure

The project consists of three main Maven modules:

1. **`repository/`** - Dependency management and local repository setup (must build first)
2. **`object/`** - Core HDF object API (`org.hdfgroup.object`)
   - Contains data model classes for HDF4, HDF5, NetCDF, and FITS formats
   - Main entry point: Object abstraction layer for different file formats
3. **`hdfview/`** - Main GUI application (`org.hdfgroup.hdfview`)
   - SWT-based user interface
   - Main class: `hdf.view.HDFView`
   - Depends on the `object` module

### Key Technologies

- **UI Framework**: Eclipse SWT (Standard Widget Toolkit)
- **Data Tables**: Eclipse NatTable (Nebula widgets)
- **Testing**: JUnit 5 + SWTBot for UI testing
  - Modern test infrastructure with parameterized tests, test tagging, and parallel execution
  - JUnit 4 vintage engine for backward compatibility during migration
- **File Formats**: HDF4, HDF5, NetCDF, FITS via native libraries
- **Logging**: SLF4J with simple logger implementation

### Source Structure

- Main source: `src/` (contains modular Java source with `org.hdfgroup.hdfview` and `org.hdfgroup.object`)
- Module sources: `hdfview/src/`, `object/src/`
- Tests: `test/` and `hdfview/src/test/`

## Development Workflow

### Prerequisites

1. Configure native library paths in `build.properties`:
   - `hdf.lib.dir` - HDF4 native libraries
   - `hdf5.lib.dir` - HDF5 native libraries
   - `hdf5.plugin.dir` - HDF5 plugins
   - `platform.hdf.lib` - Runtime library path (LD_LIBRARY_PATH on Linux)

2. The build process requires HDF4 and HDF5 native libraries to be available

### Testing

- **Test Framework**: JUnit 5 with modern test foundation classes
  - Test categorization: `@Tag("unit")`, `@Tag("integration")`, `@Tag("ui")`
  - Parameterized tests for data type variations
  - Parallel execution for unit tests (4 threads)
- **UI Testing**: SWTBot framework for automated GUI testing
- **Code Coverage**: JaCoCo with 60% line coverage target
- Test execution requires JVM arguments for module access:
  ```
  --add-opens java.base/java.lang=ALL-UNNAMED
  --add-opens java.base/java.time=ALL-UNNAMED
  --enable-native-access=jarhdf5
  ```
- **Run Tests**:
  ```bash
  mvn test                          # Run all tests
  mvn test -Dtest=TestClassName     # Run specific test
  mvn test -Dgroups="unit & fast"   # Run fast unit tests only
  mvn test -Dgroups="integration"   # Run integration tests
  ```

### Test Status

- **Object module**: 15 test classes, 149 tests - all passing
- **UI module**: 16 test classes with varying status
  - Most tests pass when run locally with display
  - Some tests are disabled due to upstream native library issues
  - UI tests require a real display (X11, Wayland, Windows, macOS)
- **CI**: Object tests run on all platforms (Linux, macOS, Windows)

### Platform-Specific Notes

- SWT dependencies are platform-specific
- Currently configured for Linux GTK (`gtk.linux.x86_64`)
- Platform profiles exist but are commented out in `hdfview/pom.xml`
- Cross-platform builds supported via CI/CD workflows

## CI/CD and Quality Assurance

### GitHub Actions Workflows

The project uses automated GitHub Actions workflows for continuous integration and quality assurance:

- **`maven-ci-orchestrator.yml`**: Primary CI orchestrator
  - Triggers all platform-specific CI builds in parallel
  - Runs on push, pull_request, and manual dispatch
  - Calls: ci-linux.yml, ci-windows.yml, ci-macos.yml

- **`ci-linux.yml`, `ci-windows.yml`, `ci-macos.yml`**: Platform CI builds
  - Build, compile, and test object module
  - Can be triggered independently via workflow_dispatch
  - Cross-platform build verification

- **`maven-quality.yml`**: Code quality analysis
  - PMD static analysis (4000 violation threshold)
  - Checkstyle code style enforcement
  - Code coverage reporting (non-blocking)
  - Runs daily at 2 AM UTC + on push/PR

- **`maven-security.yml`**: Security and compliance
  - OWASP dependency vulnerability scanning (CVSS 8.0+ threshold)
  - GitHub CodeQL security analysis
  - License compliance checking (prohibits GPL/AGPL)

- **`maven-build.yml`**: Cross-platform binary builds and Maven package publishing
  - 7 build jobs across 3 platforms:
    - **Linux**: binary tar.gz, app-image, installers (DEB/RPM - planned)
    - **Windows**: binary zip, app-image, MSI installer (signed)
    - **macOS**: binary tar.gz, app-image + DMG installer (consolidated, signed)
  - Downloads HDF4/HDF5 from GitHub releases
  - Code signing for Windows (Microsoft Trusted Signing) and macOS (Developer ID + Notarization)
  - Optional GitHub Packages publication (Maven registry)
  - Called by release.yml for comprehensive releases

### Quality Tools and Standards

- **Code Coverage**: 60% line coverage, 50% branch coverage targets (JaCoCo)
- **Static Analysis**: PMD v7.0+ with HDFView-specific rulesets
- **Code Style**: Checkstyle v10.12+ (adapted Google Java Style)
- **Security**: OWASP Dependency Check with CVE monitoring
- **Quality Gates**: PMD and Checkstyle enforced, coverage reporting only

### Quality Management Scripts

Located in `scripts/`:
- `validate-quality.sh` - Quick quality validation for pre-commit
- `check-quality.sh` - Comprehensive quality analysis
- `analyze-coverage.sh` - Coverage analysis with threshold checking
- `security-analysis.sh` - Security and license compliance scanning
- `collect-metrics.sh` - Unified quality metrics collection

## File Locations

- Main application JAR output: `libs/`
- Dependencies copied to: `target/lib/`
- Native libraries: Referenced via `build.properties` paths
- Test data: Sample HDF5 files in test resources
- Quality configurations: `pmd-rules.xml`, `checkstyle-rules.xml`, `dependency-check-suppressions.xml`
- Documentation: `docs/` directory for team documentation

## Modernization Status

### ✅ Phase 1: Maven Migration (COMPLETE)
- Pure Maven build system operational
- Ant build system removed
- Multi-module structure with proper dependency management
- SWT platform support for Linux x86_64
- Native library integration configured

### ✅ Phase 2A: JUnit 5 Migration (COMPLETE)
- JUnit 5 v5.10.0 infrastructure integrated
- Modern test foundation classes created
- All test files migrated to JUnit 5
- Tests compile and execute successfully

### ✅ Phase 2B: CI/CD Pipeline (COMPLETE)
- Multiple production GitHub Actions workflows operational
- Automated build, test, quality, and security scanning
- Release management with GitHub Packages
- All platforms (Linux, macOS, Windows) supported

### ✅ Phase 2C: Quality Analysis (COMPLETE)
- Java 21 compatible static analysis tools integrated
- Progressive quality gate enforcement configured
- Code coverage, security scanning, and license compliance
- Quality management scripts for local development

### 📋 Phase 2D: UI Framework Research (DEFERRED)
- Detailed research plan available for JavaFX evaluation
- Focus on large dataset performance
- Deferred to prioritize other modernization efforts

### ✅ Phase 2E: jpackage Integration & Installer Signing (COMPLETE)
- Migrated from Ant-based installer creation to Java 21's native jpackage
- Native installers for all platforms: MSI (Windows), DMG (macOS), DEB/RPM (Linux)
- Code signing implemented for Windows (Authenticode) and macOS (Developer ID + Notarization)
- **macOS**: Single consolidated job - signs during app-image creation using `jpackage --mac-sign`
  - Keychain setup matching ant.yml implementation
  - Signs all binaries (dylibs, frameworks, executables) during jpackage execution
  - Uses repository variables: SIGNER, KEYCHAIN_NAME, NOTARY_USER, NOTARY_KEY
  - Manual jpackage input directory preparation for reliability
  - Full notarization workflow: submit → wait → log errors → staple
- **Windows**: Microsoft Trusted Signing with Azure Code Signing
- Conditional signing (only on canonical repository with secrets)
- Comprehensive documentation for signing process and secret configuration
- See: `docs/Installer-Signing-Guide.md` and `.claude/dev-docs/jpackage-integration.md`

## Known Issues

### Float16/BFLOAT16 Support
- Float8, Float16, and BFLOAT16 datatypes are supported with workarounds
- Native HDF5 library support pending (HDF5 2.0.0 has regression)
- Application code is future-proof and ready for native support
- Crash protection in place for unsupported operations

### Test Infrastructure
- UI tests require real display (not available in CI)
- Some tests disabled due to upstream native library bugs
- Object module tests run in CI on all platforms
- Test status tracked in GitHub issues

## Documentation

Comprehensive project documentation is maintained in multiple locations:

### User Documentation (`docs/`)
- **Contributing Guide**: `CONTRIBUTING.md` - How to contribute to the project
- **Testing Guide**: `docs/Testing-Guide.md` - Complete guide for running tests locally and in CI
- **Build Instructions**: `docs/guides/Cross-Platform-Build-Quick-Reference.md` - How to build the project
- **Build Properties**: `docs/build.properties.example` - Configuration template
- **Users Guide**: `docs/UsersGuide/` - End-user documentation

### Developer Guides (`docs/guides/`)
- **CI/CD Pipeline Guide**: Complete CI/CD pipeline documentation
- **CI/CD Troubleshooting**: Troubleshooting common CI/CD issues
- **Cross-Platform Build Guide**: Quick reference for multi-platform builds
- **Windows/macOS Build Troubleshooting**: Platform-specific build issues

## AI-Specific Notes

When working with this codebase:

1. **Respect existing patterns**: The codebase has established patterns for HDF operations, UI interactions, and test infrastructure
2. **Test comprehensively**: Changes to HDF data handling should be tested with multiple data types
3. **Consider platform differences**: SWT behavior varies across platforms (Linux, macOS, Windows)
4. **Native library interactions**: Be cautious with HDF native library calls - they can cause JVM crashes if misused
5. **Quality checks**: Run `./scripts/validate-quality.sh` before suggesting code is complete
6. **Avoid over-engineering**: Keep changes minimal and focused on the specific requirement
7. **Documentation**: Update relevant documentation when making significant changes

## Getting Help

- **Contributing Guide**: See `CONTRIBUTING.md` for comprehensive developer documentation
- **GitHub Issues**: Search existing issues for known problems and solutions
- **CI/CD Guides**: See `docs/guides/` for detailed workflow documentation
- **Build Issues**: Check `docs/guides/Cross-Platform-Build-Quick-Reference.md` and platform-specific troubleshooting guides
