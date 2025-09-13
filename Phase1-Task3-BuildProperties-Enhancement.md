# Phase 1 Task 3: Improve Current build.properties System

## Overview
Enhance the existing `build.properties` approach for better native library management and developer experience while maintaining compatibility with the current HDF4/HDF5 native library integration.

## Current State Analysis

### Existing build.properties Structure
From `/home/byrn/HDF_Projects/hdfview/dev/build.properties`:
```properties
# Native library paths
hdf.lib.dir = /home/byrn/HDF_Projects/temp/HDF_Group/HDF/4.3.1-1/lib
hdf5.lib.dir = /home/byrn/HDF_Projects/temp/HDF_Group/HDF5/2.0.0/lib
hdf5.plugin.dir = /home/byrn/HDF_Projects/temp/HDF_Group/HDF5/2.0.0/lib/plugin

# Build configuration
build.debug = true
build.antoutput.append = false
build.log.level = info

# Platform-specific library path
platform.hdf.lib = ${env.LD_LIBRARY_PATH}

# Documentation and tooling paths
userguide.dir = /home/byrn/HDF_Projects/temp/UsersGuide
wix.dir = ${env.WIX}/bin
```

### Current Issues
- **Developer-specific paths**: Hardcoded absolute paths don't work across environments
- **No fallback mechanisms**: No environment variable fallbacks or auto-detection
- **Platform assumptions**: Linux-specific (`LD_LIBRARY_PATH`)
- **No validation**: Build continues even if native libraries are missing
- **Poor documentation**: No clear guidance on setup requirements

### Multiple build.properties Locations
- Root: `/home/byrn/HDF_Projects/hdfview/dev/build.properties`
- Object module: `/home/byrn/HDF_Projects/hdfview/dev/object/build.properties`
- HDFView module: `/home/byrn/HDF_Projects/hdfview/dev/hdfview/build.properties`
- Repository module: `/home/byrn/HDF_Projects/hdfview/dev/repository/build.properties`

## Detailed Task Breakdown

### Task 3.1: Standardize build.properties Structure (1 day)

#### 3.1.1 Audit Current Property Usage (2 hours)
- [ ] **Document all properties across modules**
  - Inventory properties in each module's build.properties
  - Identify which properties are actually used by Maven
  - Map property usage to specific Maven plugins/goals

- [ ] **Analyze property inheritance** (1 hour)
  - Determine how properties flow between parent and child modules
  - Document which properties need to be global vs module-specific
  - Identify duplicate properties that can be consolidated

#### 3.1.2 Create Template Structure (3 hours)
- [ ] **Design template structure** (1.5 hours)
  - Create logical property groupings (native libraries, paths, build config)
  - Define required vs optional properties
  - Design property naming conventions

- [ ] **Create build.properties.template** (1.5 hours)
  - Include all configurable properties with example values
  - Add inline comments explaining each property's purpose
  - Provide multiple example values for different scenarios
  - Include environment variable syntax examples

#### 3.1.3 Separate Configuration Types (3 hours)
- [ ] **Create development configuration template** (1 hour)
  - Properties for local development environment
  - Debug settings and logging configuration
  - Development-specific paths and options

- [ ] **Create production configuration template** (1 hour)
  - Properties for release builds
  - Production logging levels
  - Distribution-ready configurations

- [ ] **Document property purposes and examples** (1 hour)
  - Create comprehensive property documentation
  - Include setup instructions for each operating system
  - Provide troubleshooting guide for common issues

### Task 3.2: Add Environment Variable Support (1 day)

#### 3.2.1 Implement Environment Variable Fallbacks (4 hours)
- [ ] **Add HDF4_HOME support** (1 hour)
  - Modify property resolution to check `${env.HDF4_HOME}/lib` 
  - Implement fallback chain: explicit path → HDF4_HOME → default paths
  - Test environment variable detection

- [ ] **Add HDF5_HOME support** (1 hour)
  - Implement `${env.HDF5_HOME}/lib` fallback for hdf5.lib.dir
  - Add `${env.HDF5_HOME}/lib/plugin` fallback for plugin directory
  - Ensure plugin directory fallback works correctly

- [ ] **Add system path detection** (2 hours)
  - Add common installation path detection for Linux (/usr/lib, /usr/local/lib)
  - Add Windows common paths (%ProgramFiles%, %ProgramFiles(x86)%)
  - Add macOS common paths (/usr/local/lib, /opt/homebrew/lib)

#### 3.2.2 Implement Platform-Specific Variable Handling (2 hours)
- [ ] **Cross-platform library path variables**
  - Linux: `LD_LIBRARY_PATH` (current)
  - Windows: `PATH` 
  - macOS: `DYLD_LIBRARY_PATH`

- [ ] **Create platform detection properties**
  - Add platform.name property for build scripts
  - Add platform.lib.ext for library file extensions (.so, .dll, .dylib)
  - Add platform-specific path separators

#### 3.2.3 Maven Integration (2 hours)
- [ ] **Configure Maven property filtering**
  - Ensure Maven processes environment variable substitution
  - Configure build-helper-maven-plugin for property evaluation
  - Test property resolution in Maven build

### Task 3.3: Improve Native Library Path Management (2 days)

#### 3.3.1 Add Maven Profile-Based Library Path Resolution (4 hours)
- [ ] **Create library detection profiles** (2 hours)
  - Profile for HDF4 library presence/absence
  - Profile for HDF5 library validation
  - Profile activation based on library file existence

- [ ] **Integrate with platform profiles** (2 hours)
  - Combine with Task 2 platform detection
  - Create platform + library matrix (linux-hdf5, windows-hdf4, etc.)
  - Ensure consistent library path resolution across platforms

#### 3.3.2 Support Multiple HDF Library Versions (2 hours)
- [ ] **Version-specific property support**
  - Allow hdf4.version and hdf5.version properties
  - Support version-specific library directories
  - Enable side-by-side version installations

- [ ] **Library version validation**
  - Read version from libhdf5.settings and libhdf4.settings files
  - Validate compatibility with HDFView requirements
  - Warn about version mismatches

#### 3.3.3 Add Build-Time Validation (2 hours)
- [ ] **Implement library existence checks**
  - Verify native library files exist at specified paths
  - Check for required library dependencies
  - Validate plugin directory structure

- [ ] **Create validation Maven plugin configuration**
  - Use maven-enforcer-plugin for property validation
  - Add custom rules for HDF library requirements
  - Provide clear error messages for missing dependencies

### Task 3.4: Create Platform-Specific Property Templates (1 day)

#### 3.4.1 Linux Template (2 hours)
- [ ] **Create build.properties.linux.template**
  - Linux-specific default paths (`/usr/lib/x86_64-linux-gnu`, `/usr/local/lib`)
  - Ubuntu/Debian package installation paths
  - CentOS/RHEL package installation paths
  - Development library paths for manually compiled HDF libraries

#### 3.4.2 Windows Template (3 hours)
- [ ] **Create build.properties.windows.template** (2 hours)
  - Windows-specific default paths (`%ProgramFiles%\HDF_Group`)
  - Visual Studio library paths
  - MinGW library paths
  - CMake default installation paths

- [ ] **Add Windows-specific configurations** (1 hour)
  - PATH environment variable handling
  - Windows registry detection possibilities
  - Visual Studio compiler integration notes

#### 3.4.3 macOS Template (3 hours)
- [ ] **Create build.properties.macos.template** (2 hours)
  - Homebrew installation paths (`/opt/homebrew/lib`, `/usr/local/lib`)
  - MacPorts installation paths (`/opt/local/lib`)
  - Manual compilation paths
  - Apple Silicon vs Intel-specific paths

- [ ] **Add macOS-specific configurations** (1 hour)
  - DYLD_LIBRARY_PATH handling
  - System Integrity Protection (SIP) considerations
  - Framework vs library installation differences

### Task 3.5: Add Build Property Validation (1 day)

#### 3.5.1 Maven Plugin Property Validation (4 hours)
- [ ] **Configure maven-enforcer-plugin** (2 hours)
  - Add requireProperty rules for critical properties
  - Validate file existence for library paths
  - Check directory existence for plugin paths

- [ ] **Create custom validation rules** (2 hours)
  - Validate HDF library file presence (.so/.dll/.dylib)
  - Check library file permissions and accessibility
  - Validate library version compatibility

#### 3.5.2 Validation Error Messaging (2 hours)
- [ ] **Create helpful error messages**
  - Specific instructions for missing HDF4 libraries
  - Specific instructions for missing HDF5 libraries
  - Platform-specific setup guidance in error messages

- [ ] **Add validation reporting**
  - Summary of found vs missing libraries
  - Version information for detected libraries
  - Suggestions for resolving configuration issues

#### 3.5.3 Integration Testing (2 hours)
- [ ] **Test validation with missing libraries**
  - Verify build fails gracefully with clear messages
  - Test partial library availability scenarios
  - Verify validation doesn't impact successful builds

## Integration Points

### Task 2 Integration (SWT Platform Support)
- Platform detection properties should align with SWT platform profiles
- Consistent OS/architecture detection across both systems
- Shared platform-specific property naming conventions

### Task 1 Integration (Maven Migration) 
- Properties must work with Maven-only build system
- Remove any remaining Ant-specific property usage
- Integrate with new Maven plugin configurations

### Task 4 Integration (Static Analysis)
- Validation should work with static analysis builds
- Property validation should not interfere with quality checks
- Template files should pass static analysis rules

## Property Template Examples

### Enhanced Root Template Structure
```properties
# =============================================================================
# HDFView Build Configuration Template
# Copy to build.properties and customize for your environment
# =============================================================================

# -----------------------------------------------------------------------------
# Native Library Configuration (REQUIRED)
# -----------------------------------------------------------------------------
# HDF4 library directory (optional - comment out to disable HDF4 support)
hdf.lib.dir=${env.HDF4_HOME}/lib
#hdf.lib.dir=/usr/local/lib
#hdf.lib.dir=C:/Program Files/HDF_Group/HDF/4.3.1/lib

# HDF5 library directory (REQUIRED)
hdf5.lib.dir=${env.HDF5_HOME}/lib
#hdf5.lib.dir=/usr/local/lib
#hdf5.lib.dir=C:/Program Files/HDF_Group/HDF5/1.14.0/lib

# HDF5 plugin directory
hdf5.plugin.dir=${env.HDF5_HOME}/lib/plugin
#hdf5.plugin.dir=/usr/local/lib/hdf5/plugin

# Platform-specific library path (auto-detected)
# Linux: LD_LIBRARY_PATH, Windows: PATH, macOS: DYLD_LIBRARY_PATH
platform.hdf.lib=${env.LD_LIBRARY_PATH}

# -----------------------------------------------------------------------------
# Build Configuration
# -----------------------------------------------------------------------------
# Enable debugging information in compiled classes
build.debug=true
build.log.level=info

# -----------------------------------------------------------------------------
# Platform-Specific Settings (auto-detected)
# -----------------------------------------------------------------------------
# Override only if auto-detection fails
#platform.name=linux
#platform.lib.ext=.so
```

## Success Criteria

- [ ] Clear documentation for setting up `build.properties` on each platform
- [ ] Sensible defaults that work for common installation scenarios (Homebrew, apt, chocolatey)
- [ ] Environment variable fallbacks reduce manual configuration
- [ ] Build validation prevents builds with missing/invalid native libraries
- [ ] Platform-specific templates reduce setup complexity for new developers
- [ ] Template files provide comprehensive examples and documentation
- [ ] Property validation provides actionable error messages
- [ ] Multiple HDF library versions can coexist
- [ ] Zero-configuration builds work for standard installations

## Timeline and Dependencies

- **Total**: 6 days
- **Can run in parallel with**: Task 2 (SWT Platform Support)
- **Depends on**: Task 1 (Maven Migration) completion
- **Enables**: Better developer onboarding and CI/CD reliability

## Risk Assessment

### Low Risk
- Template creation and documentation
- Environment variable fallback implementation
- Platform-specific property examples

### Medium Risk  
- Maven plugin property validation integration
- Multiple library version support
- Cross-platform path handling edge cases

### High Risk
- Build validation that doesn't break existing workflows
- Property inheritance across Maven modules
- Integration with Task 1 Maven changes

## Testing Strategy

- Test on current Linux development environment
- Simulate different property configurations
- Test validation with intentionally missing libraries
- Verify environment variable fallbacks work correctly
- Test template files provide working configurations