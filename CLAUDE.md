# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HDFView is a Java-based GUI application for viewing and editing HDF (Hierarchical Data Format) files, including HDF4, HDF5, and NetCDF formats. The project is developed by The HDF Group and uses Eclipse SWT for its user interface.

## Build System

The project uses **Maven** as the build system with a multi-module structure. The Ant build system has been removed as part of Phase 1 migration - Maven is now the only supported build system.

### Build Commands

```bash
# Build the entire project
mvn clean compile

# Run tests 
mvn test

# Package application
mvn package

# Install dependencies and build
mvn clean install
```

### Key Build Configuration

- **Java Version**: Java 21 (set via `maven.compiler.source` and `maven.compiler.release`)
- **Build System**: Maven-only (Ant build removed in Phase 1 migration)
- **Build Properties**: External configuration loaded via properties-maven-plugin from `build.properties`
- **Native Libraries**: HDF4/HDF5 native libraries required (paths configured in `build.properties`)
- **Module System**: Disabled temporarily (non-modular build for SWT compatibility)

### Maven Plugins Integrated

- **Properties Plugin**: External property file loading (`build.properties`)
- **Exec Plugin**: Native library version extraction and application execution
- **JaCoCo Plugin**: Code coverage analysis and reporting (60% line, 50% branch coverage targets)
- **JavaDoc Plugin**: API documentation generation with multi-module aggregation
- **Surefire Plugin**: JUnit 5 test execution with parallel test support
- **PMD Plugin**: Static code analysis (v7.0+, Java 21 compatible)
- **Checkstyle Plugin**: Code style enforcement (v10.12+, Java 21 compatible)
- **OWASP Dependency Check**: Security vulnerability scanning
- **License Plugin**: License compliance checking

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
- **Testing**: JUnit 5 (migration in progress) + SWTBot for UI testing
  - Modern test infrastructure with parameterized tests, test tagging, and parallel execution
  - JUnit 4 vintage engine for backward compatibility during migration
- **File Formats**: HDF4, HDF5, NetCDF, FITS via native libraries
- **Logging**: SLF4J

### Source Structure

- Main source: `src/` (contains modular Java source with `org.hdfgroup.hdfview` and `org.hdfgroup.object`)
- Module sources: `hdfview/src/`, `object/src/`
- Tests: `test/` and `hdfview/src/test/` (17 UI test classes in JUnit 5 migration)

## Development Workflow

### Prerequisites

1. Configure native library paths in `build.properties`:
   - `hdf.lib.dir` - HDF4 native libraries
   - `hdf5.lib.dir` - HDF5 native libraries 
   - `hdf5.plugin.dir` - HDF5 plugins
   - `platform.hdf.lib` - Runtime library path (LD_LIBRARY_PATH on Linux)

2. The build process requires HDF4 and HDF5 native libraries to be available

### Testing

- **Test Framework**: JUnit 5 migration in progress (infrastructure complete)
  - Modern test foundation classes for HDF testing
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

### Platform-Specific Notes

- SWT dependencies are platform-specific
- Currently configured for Linux GTK (`gtk.linux.x86_64`)
- Platform profiles exist but are commented out in `hdfview/pom.xml`

## CI/CD and Quality Assurance

### GitHub Actions Workflows

The project uses automated GitHub Actions workflows for continuous integration and quality assurance:

- **`maven-ci-orchestrator.yml`**: Primary CI orchestrator
  - Triggers all platform-specific CI builds in parallel
  - Runs on push, pull_request, and manual dispatch
  - Calls: ci-linux.yml, ci-windows.yml, ci-macos.yml

- **`ci-linux.yml`, `ci-windows.yml`, `ci-macos.yml`**: Platform CI builds
  - Build, compile, and test (currently tests disabled during JUnit 5 migration)
  - Can be triggered independently via workflow_dispatch
  - Cross-platform build verification

- **`maven-quality.yml`**: Code quality analysis
  - JaCoCo code coverage (60% threshold)
  - PMD static analysis (violation limits)
  - Checkstyle code style enforcement
  - Runs daily at 2 AM UTC + on push/PR

- **`maven-security.yml`**: Security and compliance
  - OWASP dependency vulnerability scanning (CVSS 8.0+ threshold)
  - GitHub CodeQL security analysis
  - License compliance checking (prohibits GPL/AGPL)

- **`maven-build.yml`**: Cross-platform binary builds
  - 6 build jobs (Linux, Windows, macOS binaries and app packages)
  - Downloads HDF4/HDF5 from GitHub releases
  - Daily scheduled builds

- **`maven-release.yml`**: Automated releases
  - Version management from Git tags
  - GitHub Packages publication
  - Release notes generation

### Quality Tools and Standards

- **Code Coverage**: 60% line coverage, 50% branch coverage (JaCoCo)
- **Static Analysis**: PMD v7.0+ with HDFView-specific rulesets
- **Code Style**: Checkstyle v10.12+ (Google Java Style adapted)
- **Security**: OWASP Dependency Check with CVE monitoring
- **Progressive Enforcement**: Currently in report-only mode, moving to enforcement

### Quality Management Scripts

Located in `scripts/`:
- `validate-quality.sh` - Quick quality validation for pre-commit
- `check-quality.sh` - Comprehensive quality analysis
- `analyze-coverage.sh` - Coverage analysis with threshold checking
- `security-analysis.sh` - Security and license compliance scanning
- `collect-metrics.sh` - Unified quality metrics collection
- `migrate-junit5.sh` - Automated JUnit 4 to JUnit 5 migration

## File Locations

- Main application JAR output: `libs/`
- Dependencies copied to: `target/lib/`
- Native libraries: Referenced via `build.properties` paths
- Test data: Includes sample HDF5 file at `object/TestHDF5.h5`
- Documentation: `.claude/` directory contains all project documentation
- Quality configurations: `pmd-rules.xml`, `checkstyle-rules.xml`, `dependency-check-suppressions.xml`

## Modernization Status

### ✅ Phase 1: Maven Migration (COMPLETE)
- Pure Maven build system operational
- Ant build system removed
- Multi-module structure with proper dependency management
- SWT platform support for Linux x86_64
- Native library integration configured

### ✅ Phase 2A: JUnit 5 Migration (INFRASTRUCTURE COMPLETE, MIGRATION IN PROGRESS - 18%)
- JUnit 5 v5.10.0 infrastructure integrated
- Modern test foundation classes created
- Automated migration script available
- **Progress**: 3 of 17 test files migrated (18%)
- **Current Task**: Migrating remaining 14 test files to JUnit 5

### ✅ Phase 2B: CI/CD Pipeline (COMPLETE)
- 4 production GitHub Actions workflows operational
- Automated build, test, quality, and security scanning
- Release management with GitHub Packages
- All performance targets achieved

### ✅ Phase 2C: Quality Analysis (COMPLETE)
- Java 21 compatible static analysis tools integrated
- Progressive quality gate enforcement configured
- Code coverage, security scanning, and license compliance
- Quality management scripts for local development

### 📋 Phase 2D: UI Framework Research (DEFERRED)
- Detailed research plan documented in `.claude/Phase2D-UI-Framework-Research.md`
- Focus on JavaFX evaluation for large dataset performance
- Planned but deferred to prioritize test migration completion

### 🎯 Current Focus (November 15, 2025)
**JUnit 5 Test Migration**: Systematically migrating remaining test files to JUnit 5 using established infrastructure and migration tools.
- **Progress**: 3 of 17 files complete (18%)
- **Remaining**: 14 test files
- **Strategy**: Small files first, large files last
- **CI Tests**: Disabled until migration complete, then will re-enable and verify

## Documentation

Comprehensive project documentation is maintained in the `.claude/` directory:

- **Phase 1 Documentation**: `.claude/Phase1/` - Complete Maven migration history
- **Phase 2 Summaries**: Implementation details for JUnit 5, CI/CD, and Quality Analysis
- **Guides**: JUnit 5 migration, CI/CD operations, troubleshooting
- **Status Reports**: Current status and planning documents
- **Configuration Examples**: PMD rules, Checkstyle configuration, quality standards