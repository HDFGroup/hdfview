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
- **JaCoCo Plugin**: Code coverage analysis and reporting
- **JavaDoc Plugin**: API documentation generation with multi-module aggregation

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
- **Testing**: JUnit 4 + SWTBot for UI testing
- **File Formats**: HDF4, HDF5, NetCDF, FITS via native libraries
- **Logging**: SLF4J

### Source Structure

- Main source: `src/` (contains modular Java source with `org.hdfgroup.hdfview` and `org.hdfgroup.object`)
- Module sources: `hdfview/src/`, `object/src/`
- Tests: `test/` and `hdfview/src/test/` (66+ test classes, primarily UI tests)

## Development Workflow

### Prerequisites

1. Configure native library paths in `build.properties`:
   - `hdf.lib.dir` - HDF4 native libraries
   - `hdf5.lib.dir` - HDF5 native libraries 
   - `hdf5.plugin.dir` - HDF5 plugins
   - `platform.hdf.lib` - Runtime library path (LD_LIBRARY_PATH on Linux)

2. The build process requires HDF4 and HDF5 native libraries to be available

### Testing

- UI tests use SWTBot framework for automated GUI testing
- Test execution requires JVM arguments for module access:
  ```
  --add-opens java.base/java.lang=ALL-UNNAMED
  --add-opens java.base/java.time=ALL-UNNAMED
  --enable-native-access=jarhdf5
  ```
- Run individual test: `mvn test -Dtest=TestClassName`

### Platform-Specific Notes

- SWT dependencies are platform-specific
- Currently configured for Linux GTK (`gtk.linux.x86_64`)
- Platform profiles exist but are commented out in `hdfview/pom.xml`

## File Locations

- Main application JAR output: `libs/`
- Dependencies copied to: `target/lib/`
- Native libraries: Referenced via `build.properties` paths
- Test data: Includes sample HDF5 file at `object/TestHDF5.h5`