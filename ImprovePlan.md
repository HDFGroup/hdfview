# HDFView Improvement Plan

This document outlines high-level improvement suggestions for the HDFView project based on analysis of the current codebase, build system, and development workflow.

## 1. Build System Modernization

### Current Issues
- **Dual Build Systems**: Both Ant (legacy) and Maven (current) exist, causing confusion
- **CI/CD Mismatch**: GitHub Actions workflows still use Ant while development uses Maven
- **Native Library Management**: Manual setup via `build.properties` with local file paths
- **Platform Dependencies**: SWT platform-specific dependencies are commented out in Maven

### High-Level Improvements
- **Complete Maven Migration**: Remove Ant build files and fully migrate CI/CD to Maven
- **Unified Build Configuration**: Single source of truth for dependencies and build configuration
- **Automated Native Library Management**: Use Maven profiles and dependency management for HDF libraries
- **Platform-Specific Builds**: Enable proper platform-specific SWT dependency resolution
- **Dependency Management**: Migrate from local repository approach to proper Maven Central dependencies where possible

## 2. Development Experience Enhancement

### Current Issues
- **Manual Environment Setup**: Developers need to manually configure native library paths
- **Platform-Specific Complexity**: Different setup requirements across Windows, macOS, Linux
- **No Containerized Development**: No Docker or dev container support
- **Limited Documentation**: Minimal developer onboarding documentation

### High-Level Improvements
- **Developer Containers**: Create VS Code devcontainer and Docker Compose setup for consistent development environment
- **Automated Setup Scripts**: Platform-specific setup scripts for native libraries and dependencies
- **Enhanced Developer Documentation**: Comprehensive getting-started guide with common troubleshooting
- **IDE Configuration**: Standard IDE configurations (Eclipse, IntelliJ, VS Code) for consistent development experience
- **Pre-commit Hooks**: Automated code formatting and basic quality checks

## 3. Technology Stack Modernization

### Current Issues
- **Outdated UI Framework**: Eclipse SWT is dated and has limited cross-platform consistency
- **Legacy Testing**: JUnit 4 instead of JUnit 5
- **Limited Modern Java Features**: Not leveraging Java 21 features effectively
- **No Modern Build Tools**: Could benefit from Gradle's advanced dependency management

### High-Level Improvements
- **UI Framework Evaluation**: Assess migration to JavaFX for better cross-platform consistency and modern UI patterns
- **Testing Framework Upgrade**: Migrate to JUnit 5 with improved parameterized testing and modern assertions
- **Modern Java Features**: Leverage records, sealed classes, pattern matching, and virtual threads where appropriate
- **Build Tool Assessment**: Evaluate Gradle migration for better dependency management and build performance
- **Modular Architecture**: Better leverage Java Platform Module System (JPMS)

## 4. Code Quality and Maintainability

### Current Issues
- **Limited Static Analysis**: No visible CheckStyle, SpotBugs, or similar tools
- **Code Coverage**: No automated code coverage reporting
- **Inconsistent Testing**: Primarily UI tests, limited unit testing
- **Code Formatting**: Uses clang-format for Java (unusual approach)

### High-Level Improvements
- **Static Analysis Integration**: Add SpotBugs, PMD, and CheckStyle with CI/CD integration
- **Code Coverage Reporting**: Implement JaCoCo with coverage thresholds and reporting
- **Testing Strategy**: Increase unit test coverage and implement integration testing
- **Code Formatting Standardization**: Consider migration to standard Java formatters (Google Java Format or similar)
- **Architectural Testing**: Add ArchUnit for architecture compliance testing
- **Documentation Generation**: Automated JavaDoc generation and publishing

## 5. Architecture and Performance

### Current Issues
- **Monolithic UI Architecture**: Tight coupling between UI and data models
- **Performance with Large Files**: SWT can be slow with large HDF datasets
- **Synchronous I/O**: Likely blocking UI thread operations
- **Limited Extensibility**: No plugin architecture for extending functionality

### High-Level Improvements
- **Architectural Refactoring**: Implement MVP/MVC pattern with better separation of concerns
- **Asynchronous I/O**: Move file operations to background threads with progress indicators
- **Caching Strategy**: Implement intelligent caching for large HDF file operations
- **Plugin Architecture**: Design extensible plugin system for custom data viewers and editors
- **Memory Management**: Optimize memory usage for large datasets
- **Reactive Programming**: Consider reactive streams for handling large data updates

## 6. Distribution and Packaging

### Current Issues
- **Manual Platform Builds**: No automated cross-platform build process
- **No Modern Packaging**: Missing native installer generation
- **No Container Distribution**: No Docker images for server deployments
- **Complex Deployment**: Requires manual native library setup

### High-Level Improvements
- **Native Packaging**: Implement jpackage for native installers (Windows MSI, macOS DMG, Linux DEB/RPM)
- **Container Images**: Docker images for headless HDF processing and web-based viewing
- **Automated Releases**: GitHub Actions for automated cross-platform releases
- **Self-Contained Applications**: Bundle native libraries with application distributions
- **Web Distribution**: Consider web-based viewer using WebAssembly for native libraries

## 7. User Experience and Accessibility

### Current Issues
- **Dated Look and Feel**: SWT provides dated appearance across platforms
- **Limited Accessibility**: No visible accessibility features
- **Complex UI Navigation**: Could benefit from modern UI patterns
- **No Dark Mode**: Missing modern UI theme support

### High-Level Improvements
- **Modern UI Design**: Implement contemporary design patterns and themes
- **Accessibility Compliance**: Add screen reader support and keyboard navigation
- **Responsive Design**: Better handling of different screen sizes and resolutions
- **Theme Support**: Dark mode and customizable themes
- **User Preferences**: Persistent user settings and workspace layouts
- **Progressive Disclosure**: Better organization of complex functionality

## 8. Community and Documentation

### Current Issues
- **Limited Contribution Guidelines**: No visible contribution documentation
- **Missing ADRs**: No architectural decision records
- **Developer Onboarding**: Steep learning curve for new contributors
- **API Documentation**: Limited API documentation generation

### High-Level Improvements
- **Contribution Guidelines**: Clear CONTRIBUTING.md with development setup and standards
- **Architectural Decision Records**: Document key architectural and technology decisions
- **Developer Portal**: Comprehensive developer documentation website
- **API Documentation**: Automated generation and publishing of API documentation
- **Code Examples**: Sample plugins and extension examples
- **Community Templates**: Issue and PR templates for better community engagement

## Implementation Priority

### Phase 1: Foundation (High Priority)

#### 1. Complete Maven Migration and Remove Ant Build System

**Scope**: Complete removal of Ant build infrastructure and ensure Maven handles all build tasks.

**Tasks**:
- **1.1 Audit Ant Dependencies** (1 day)
  - Document all tasks currently handled by `build.xml`
  - Identify any custom Ant tasks that need Maven equivalents
  - Check for Ant-specific build properties or configurations

- **1.2 Migrate Missing Build Tasks** (2-3 days)
  - Ensure all Ant build tasks have Maven equivalents
  - Update packaging and distribution tasks in Maven
  - Migrate any custom build scripts or generators

- **1.3 Remove Ant Files** (0.5 days)
  - Delete `build.xml` from root directory
  - Remove any remaining `.ant` or `ant.properties` files
  - Clean up Ant-related documentation references

- **1.4 Remove Maven-Antrun Dependencies** (1 day)
  - Remove `maven-antrun-plugin` from `hdfview/pom.xml:211-226`
  - Update compiler plugin configuration to remove Ant property dependencies
  - Test build process without Ant integration

**Acceptance Criteria**:
- No `build.xml` files remain in repository
- `mvn clean install` successfully builds entire project
- All existing functionality preserved (tests pass)
- No Ant runtime dependencies

#### 2. Fix Platform-Specific SWT Dependency Resolution

**Scope**: Enable proper cross-platform SWT dependency resolution for Linux, Windows, and macOS.

**Tasks**:
- **2.1 Uncomment and Fix SWT Profiles** (1 day)
  - Uncomment platform profiles in `hdfview/pom.xml:111-194`
  - Fix Maven profile syntax and property resolution
  - Update SWT artifact IDs for current versions

- **2.2 Configure Platform-Specific Dependencies** (2 days)
  - **Linux**: `org.eclipse.swt.gtk.linux.x86_64`
  - **Windows**: `org.eclipse.swt.win32.win32.x86_64`
  - **macOS**: `org.eclipse.swt.cocoa.macosx.x86_64` and `org.eclipse.swt.cocoa.macosx.aarch64`
  - Add ARM64 support for both Windows and macOS

- **2.3 Replace Local SWT Dependencies** (1 day)
  - Remove local Eclipse dependencies from `org.eclipse.local` group
  - Update to use Maven Central SWT dependencies
  - Update version properties in parent POM

- **2.4 Test Multi-Platform Builds** (1 day)
  - Verify builds work on each target platform
  - Test SWT UI launches correctly per platform
  - Document platform-specific build commands

**Acceptance Criteria**:
- Maven profiles automatically select correct SWT dependencies
- Application builds and runs on Linux, Windows, and macOS
- No manual platform configuration required
- CI can build for multiple platforms

#### 3. Improve Current build.properties System

**Scope**: Enhance the existing `build.properties` approach for better native library management and developer experience.

**Tasks**:
- **3.1 Standardize build.properties Structure** (1 day)
  - Create template `build.properties.template` with default values
  - Document all available properties and their purposes
  - Separate development vs production configurations

- **3.2 Add Environment Variable Support** (1 day)
  - Allow properties to fallback to environment variables
  - Support `HDF4_HOME`, `HDF5_HOME` environment variables
  - Add auto-detection of common installation paths

- **3.3 Improve Native Library Path Management** (2 days)
  - Add Maven profile-based library path resolution
  - Support multiple HDF library versions side by side
  - Add validation to ensure native libraries are found at build time

- **3.4 Create Platform-Specific Property Templates** (1 day)
  - `build.properties.linux.template`
  - `build.properties.windows.template` 
  - `build.properties.macos.template`
  - Include platform-specific default paths and configurations

- **3.5 Add Build Property Validation** (1 day)
  - Maven plugin to validate required properties are set
  - Check native library files exist at specified paths
  - Provide clear error messages for missing dependencies

**Acceptance Criteria**:
- Clear documentation for setting up `build.properties`
- Sensible defaults that work for common installation scenarios
- Validation prevents builds with missing native libraries
- Platform-specific templates reduce setup complexity

#### 4. Add Basic Static Analysis and Code Quality Tools

**Scope**: Integrate SpotBugs for static analysis with CI/CD integration capability.

**Tasks**:
- **4.1 Integrate SpotBugs** (1 day)
  - Add `com.github.spotbugs:spotbugs-maven-plugin` to parent POM
  - Configure exclusion rules for known false positives
  - Set appropriate analysis level (default: medium)

- **4.2 Configure Quality Gates** (1 day)
  - Set build failure thresholds for critical/high priority issues
  - Configure report generation in target directory
  - Add HTML and XML report outputs

- **4.3 Create Quality Profile** (0.5 days)
  - Define custom ruleset for HDFView specific patterns
  - Configure exclusions for UI and native library integration code
  - Document quality standards and expectations

- **4.4 CI Integration Preparation** (0.5 days)
  - Ensure SpotBugs reports are generated in CI-friendly format
  - Configure Maven to run static analysis in `verify` phase
  - Document integration points for Phase 2 CI/CD work

**Acceptance Criteria**:
- SpotBugs runs automatically during `mvn verify`
- Build fails on critical static analysis issues
- Reports generated in `target/spotbugs` directory
- Zero critical issues in codebase after cleanup

#### Phase 1 Timeline and Dependencies

**Total Estimated Duration**: 2-3 weeks

**Dependency Order**:
1. Tasks 1.1-1.4 (Ant Migration) - **Must complete first**
2. Tasks 2.1-2.4 (SWT Profiles) - **Can run in parallel with Task 3**
3. Tasks 3.1-3.5 (build.properties) - **Can run in parallel with Task 2**
4. Tasks 4.1-4.4 (Static Analysis) - **Can start after Maven migration**

**Risk Mitigation**:
- Test each change incrementally to avoid breaking builds
- Maintain backup branch before major changes
- Verify all tests pass after each major task completion

### Phase 2: Modernization (Medium Priority)
1. Upgrade to JUnit 5 and improve testing infrastructure
2. Implement proper CI/CD with Maven
3. Add code coverage and quality gates
4. Evaluate and plan UI framework migration

### Phase 3: Enhancement (Lower Priority)
1. Architectural refactoring for better separation of concerns
2. Performance optimization for large datasets
3. Plugin architecture implementation
4. Modern packaging and distribution

## Success Metrics

- **Developer Onboarding Time**: Reduce from days to hours with automated setup
- **Build Consistency**: Eliminate platform-specific build failures
- **Code Quality**: Achieve >80% code coverage and zero high-priority static analysis issues
- **Performance**: Improve large file loading times by 50%
- **Community Engagement**: Increase contributor participation with better tooling and documentation

This improvement plan provides a roadmap for modernizing HDFView while maintaining its core functionality and stability. Each improvement should be implemented incrementally with proper testing and community feedback.