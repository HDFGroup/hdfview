# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HDFView is a Java-based GUI application for viewing and editing HDF (Hierarchical Data Format) files, including HDF4, HDF5, and NetCDF formats. The project is developed by The HDF Group and uses Eclipse SWT for its user interface.

## Build System

The project uses **Maven** as the primary build system with a multi-module structure. While legacy Ant build files exist (`build.xml`), the current development focuses on Maven.

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
- **Build Properties**: Configuration stored in `build.properties` file
- **Native Libraries**: HDF4/HDF5 native libraries required (paths set in `build.properties`)

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