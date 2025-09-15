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

**Prerequisites**: ✅ **ALL PHASE 1 TASKS COMPLETE**
- ✅ **Task 1**: Maven Migration - Pure Maven build achieved
- ✅ **Task 2**: SWT Platform Support - Linux x86_64 fully operational
- ✅ **Task 3**: build.properties Enhancement - Consolidated with validation
- ✅ **Task 4**: Static Analysis - Infrastructure ready (pending Java 21 support)
- ✅ **Foundation**: JaCoCo, JavaDoc, Properties, Exec plugins integrated

#### 1. Complete JUnit 5 Migration and Testing Infrastructure

**Scope**: Migrate all 66+ tests from JUnit 4 to JUnit 5 with improved organization and modern testing practices.

**Duration**: 3-4 weeks

**Tasks**:
- **2.1 Set Up JUnit 5 Infrastructure** (2 days)
  - Add JUnit 5 dependencies (jupiter-engine, jupiter-params, vintage-engine) to parent POM
  - Configure Maven Surefire plugin v3.2+ for JUnit 5 support
  - Add JUnit 4 vintage engine for transition period compatibility
  - Update test execution JVM arguments for module access and SWT testing
  - Test infrastructure with sample migration

- **2.2 Audit and Categorize Existing Tests** (2 days)
  - Document all 66+ existing test classes by category (unit, integration, UI)
  - Identify SWTBot UI test patterns and dependencies
  - Map tests that benefit from JUnit 5 features (parameterized, dynamic, nested)
  - Create migration priority matrix based on complexity and value
  - Document test data dependencies and setup requirements

- **2.3 Create Modern Test Foundation** (3 days)
  - Design JUnit 5 test base classes with consistent patterns
  - Create SWTBot test utilities with modern assertions and `@TempDir` support
  - Implement test data management utilities for HDF files
  - Add parameterized test support for HDF4/HDF5/NetCDF format variations
  - Create test fixtures and helper classes for common operations

- **2.4 Migrate Core Data Model Tests** (4 days)
  - Convert all object module tests (~20 tests) to JUnit 5
  - Focus on HDF4Object, HDF5Object, NetCDF data model classes
  - Implement parameterized tests for different data types and formats
  - Add dynamic tests for varying dataset configurations
  - Use `@Nested` tests for logical test grouping

- **2.5 Migrate UI and Integration Tests** (5 days)
  - Convert all SWTBot UI tests (~40 tests) to JUnit 5
  - Update HDFView application tests with modern lifecycle management
  - Migrate file I/O integration tests with `@TempDir` and proper cleanup
  - Add test categories with `@Tag` annotations (unit, integration, ui, slow)
  - Implement test suites with `@Suite` for different execution modes

- **2.6 Organize Test Structure and Execution** (2 days)
  - Create Maven profiles for test execution modes (unit-only, integration, all)
  - Separate slow/UI tests from fast unit tests
  - Configure parallel test execution for improved performance
  - Add test coverage integration with JaCoCo for trend monitoring
  - Document test execution patterns and IDE setup

- **2.7 Remove JUnit 4 Dependencies** (1 day)
  - Remove vintage engine after all tests migrated
  - Clean up JUnit 4 dependencies and imports
  - Update CI/CD configuration for JUnit 5 only
  - Final validation of all test execution modes

**Acceptance Criteria**:
- All 66+ tests migrated to JUnit 5 with no JUnit 4 dependencies
- Test execution time improved by 20% through parallel execution and organization
- Clear separation: unit tests (<2s), integration tests (<30s), UI tests (any duration)
- Test coverage baseline established with JaCoCo integration
- Comprehensive test execution documentation for contributors

#### 2. Implement Maven-based CI/CD Pipeline

**Scope**: Replace Ant-based GitHub Actions with comprehensive Maven CI/CD focusing on development workflow and quality gates.

**Duration**: 2-3 weeks

**Tasks**:
- **2.8 Audit and Plan CI/CD Migration** (2 days)
  - Document all existing workflows in `.github/workflows/`
  - Identify Ant-specific steps requiring Maven equivalents
  - Map current artifact generation and deployment processes
  - Plan development-focused workflow (defer multi-platform matrix per guidance)
  - Create migration strategy minimizing disruption

- **2.9 Create Core Maven Workflows** (4 days)
  - **Development Workflow**: Build, test, quality analysis on push/PR
    - Maven clean compile test verify
    - JUnit 5 test execution with reporting
    - JaCoCo code coverage with trend analysis
    - Java 21 compatible static analysis (PMD, Checkstyle)
  - **Quality Gate Workflow**: Automated quality checks and reporting
    - Code coverage thresholds and trend monitoring
    - Static analysis violation reporting
    - Test success rate and performance metrics
  - **Dependency Management**: Security and license scanning
    - OWASP dependency vulnerability checks
    - GitHub Dependabot integration
    - License compliance validation

- **2.10 Configure Build Optimization** (2 days)
  - Implement Maven dependency caching for faster builds
  - Configure parallel test execution within CI
  - Set up incremental build strategies
  - Add build performance monitoring and reporting
  - Target <10 minute build times for development workflow

- **2.11 Set Up Quality Reporting and Gates** (3 days)
  - Configure automated code coverage reporting with JaCoCo
  - Set up static analysis reporting (PMD, Checkstyle output)
  - Implement quality gates that fail builds on regression
  - Add PR status checks for quality metrics
  - Create quality trend dashboards and notifications

- **2.12 Implement Artifact and Release Management** (2 days)
  - Configure GitHub Packages for Maven artifacts
  - Set up automated versioning for releases
  - Create release workflow with changelog generation
  - Implement artifact retention and cleanup policies
  - Document release process and artifact usage

- **2.13 Documentation and Team Integration** (2 days)
  - Create comprehensive CI/CD documentation for contributors
  - Set up build status badges for repository README
  - Configure notification strategies (GitHub, email)
  - Add troubleshooting guide for common CI issues
  - Document development workflow integration

**Acceptance Criteria**:
- All Ant workflows replaced with Maven-based equivalents
- Development builds complete in <10 minutes with full quality analysis
- Automated quality gates prevent regression in code quality and coverage
- Release process fully automated with proper versioning and artifacts
- Comprehensive documentation and team onboarding materials

#### 3. Implement Comprehensive Code Quality and Static Analysis

**Scope**: Expand JaCoCo integration and implement Java 21 compatible static analysis with comprehensive quality gates.

**Duration**: 2-3 weeks

**Tasks**:
- **2.14 Enhance Code Coverage Infrastructure** (2 days)
  - Enhance existing JaCoCo configuration with detailed reporting
  - Set up coverage trend tracking and historical analysis
  - Configure coverage aggregation across object and hdfview modules
  - Add coverage exclusions for generated code and native library wrappers
  - Integrate coverage reporting with CI/CD pipeline

- **2.15 Set Coverage Thresholds and Quality Gates** (2 days)
  - Define minimum coverage percentages: 60% line, 50% branch per module
  - Configure incremental coverage requirements for new code (80% minimum)
  - Set up coverage trend monitoring and regression detection
  - Implement Maven build failure on coverage regression
  - Add coverage metrics to PR status checks

- **2.16 Implement Java 21 Compatible Static Analysis** (4 days)
  - **Add PMD v7.0+**: Java 21 compatible static analysis
    - Configure HDFView-specific ruleset excluding SWT/JNI patterns
    - Focus on code quality, potential bugs, performance issues
    - Generate XML/HTML reports for CI integration
  - **Add Checkstyle v10.12+**: Code formatting and style enforcement
    - Configure Google Java Style or similar standard
    - Add HDFView-specific exclusions for legacy patterns
    - Integrate with IDE configurations (Eclipse, IntelliJ, VS Code)
  - **Enhance SpotBugs Foundation**: Update when Java 21 support available
    - Monitor SpotBugs releases for Java 21 compatibility
    - Prepare activation plan for existing configuration
    - Document alternative static analysis approach

- **2.17 Add Security and Dependency Analysis** (2 days)
  - **OWASP Dependency Check**: Vulnerability scanning for Maven dependencies
    - Configure database updates and reporting
    - Set up vulnerability threshold and build failure rules
    - Integrate with GitHub security advisories
  - **License Compliance**: Automated license scanning and reporting
    - Document approved licenses for HDFView project
    - Flag license conflicts and GPL compatibility issues
    - Generate license reports for distribution

- **2.18 Create Unified Quality Reporting** (3 days)
  - Set up quality gate enforcement in Maven build lifecycle
  - Configure build failure rules for each quality tool
  - Create unified quality reporting dashboard
  - Implement quality metrics trend analysis and alerts
  - Add quality status checks for PR reviews

- **2.19 Performance and Memory Analysis** (2 days)
  - Add JMH (Java Microbenchmark Harness) for performance testing
  - Create benchmarks for large HDF file operations (loading, parsing, rendering)
  - Set up performance regression detection in CI
  - Add memory usage profiling for large dataset operations
  - Document performance baseline and monitoring

- **2.20 Documentation Quality and API Standards** (1 day)
  - Enhance existing JavaDoc generation with coverage metrics
  - Add documentation linting for missing JavaDoc
  - Set up automated API documentation publishing
  - Configure inline documentation quality checks
  - Document API standards and contribution requirements

**Acceptance Criteria**:
- Achieve >60% code coverage with trend monitoring and quality gates
- Java 21 compatible static analysis (PMD, Checkstyle) running in CI
- Zero high-priority security vulnerabilities in dependencies
- Quality gates prevent regression in coverage, style, and security
- Performance benchmarks established with regression detection
- Comprehensive quality dashboard and reporting system

#### 4. Research UI Framework Alternatives (Deferred Implementation)

**Scope**: Research and evaluation only - provide recommendations for future UI modernization without implementation.

**Duration**: 1-2 weeks

**Tasks**:
- **2.21 Current SWT Assessment and Documentation** (3 days)
  - Comprehensive audit of all SWT components and widgets used
  - Document platform-specific issues, limitations, and workarounds
  - Assess current UI performance bottlenecks and memory usage
  - Evaluate accessibility compliance and theming limitations
  - Map integration points with native HDF libraries
  - Document user workflow dependencies on SWT-specific features

- **2.22 JavaFX Research and Proof-of-Concept** (4 days)
  - Create minimal proof-of-concept HDF data viewer in JavaFX
  - Evaluate TableView/TreeView performance with large HDF datasets
  - Test cross-platform look and feel consistency (Linux focus)
  - Assess JavaFX packaging options (jpackage, native installers)
  - Research JavaFX-SWT integration possibilities for incremental migration
  - Document JavaFX licensing and long-term support considerations

- **2.23 Alternative Framework Survey** (2 days)
  - Research Swing modernization options (FlatLaf, Darcula themes)
  - Evaluate web-based alternatives (Vaadin, Spring Boot + web UI)
  - Assess JavaFX WebView for hybrid approaches
  - Research desktop framework trends and community support
  - Compare native integration capabilities across frameworks
  - Document licensing, maintenance, and community implications

- **2.24 Migration Feasibility Analysis** (2 days)
  - Estimate migration effort for each major UI component
  - Identify high-risk areas (custom widgets, native library integration)
  - Assess impact on existing user workflows and data visualization
  - Evaluate backward compatibility and user adoption considerations
  - Document technical debt and modernization benefits
  - Create risk/benefit analysis matrix

- **2.25 Create Comprehensive UI Framework Report** (2 days)
  - **Framework Comparison Matrix**: Feature, performance, maintenance comparison
  - **Migration Strategy Options**: Big bang vs incremental vs hybrid approaches
  - **Risk Assessment**: Technical, user experience, and project risks
  - **Recommendation**: Clear framework choice with justification
  - **Future Roadmap**: Suggested Phase 3 implementation approach
  - **Decision Document (ADR)**: Architectural decision record for stakeholder review

**Acceptance Criteria**:
- Comprehensive evaluation report comparing SWT, JavaFX, and alternatives
- Working JavaFX proof-of-concept demonstrating HDF data visualization
- Clear framework recommendation with detailed justification
- Migration feasibility assessment with effort estimates and risk analysis
- Architectural Decision Record (ADR) ready for stakeholder review and approval
- **No implementation commitment** - research and recommendations only

#### Phase 2 Timeline and Dependencies

**Total Estimated Duration**: 8-10 weeks

**Dependency Order and Execution Strategy**:

**Phase 2A: Testing Foundation (Weeks 1-4)**
- **Tasks 2.1-2.7**: Complete JUnit 5 Migration (3-4 weeks)
  - Critical prerequisite for CI/CD quality gates
  - Can start immediately with stable Phase 1 foundation
  - Provides test infrastructure for quality measurement

**Phase 2B: CI/CD and Quality Infrastructure (Weeks 3-7)**
- **Tasks 2.8-2.13**: Maven CI/CD Pipeline (2-3 weeks)
  - Can start in Week 3 after basic JUnit 5 infrastructure ready
  - Parallel execution with final JUnit 5 migration tasks
- **Tasks 2.14-2.20**: Code Quality and Static Analysis (2-3 weeks)
  - Depends on CI/CD pipeline infrastructure
  - Can overlap with final CI/CD tasks

**Phase 2C: Research and Planning (Weeks 6-9)**
- **Tasks 2.21-2.25**: UI Framework Research (1-2 weeks)
  - Independent of other Phase 2 work
  - Can run in parallel with quality implementation
  - Research only - no implementation dependencies

**Parallel Execution Opportunities**:
- JUnit 5 final migration (2.6-2.7) + CI/CD setup (2.8-2.9) - Weeks 3-4
- CI/CD optimization (2.10) + Quality infrastructure (2.14-2.15) - Weeks 4-5
- Quality gates (2.16-2.18) + UI framework research (2.21-2.23) - Weeks 6-7

**Critical Dependencies**:
1. **JUnit 5 Infrastructure** (Tasks 2.1-2.2) must complete before CI/CD work
2. **CI/CD Core Workflows** (Tasks 2.8-2.9) must complete before quality gates
3. **Coverage Infrastructure** (Task 2.14) must complete before quality thresholds
4. **UI Framework Research** has no dependencies - can start anytime

**Risk Mitigation**:
- Maintain JUnit 4 vintage engine until all tests migrated
- Implement CI/CD incrementally to avoid breaking existing development workflow
- Test quality gates on feature branches before enforcing on main
- Keep UI framework research separate from implementation to avoid scope creep
- Monitor Java 21 ecosystem changes for SpotBugs activation opportunities

**Success Criteria for Phase 2**:
- All 66+ tests migrated to JUnit 5 with improved organization and performance
- Fully automated Maven-based CI/CD pipeline with <10 minute build times
- >60% code coverage with automated quality gates preventing regression
- Java 21 compatible static analysis (PMD, Checkstyle) integrated and enforced
- Comprehensive UI framework evaluation with clear recommendations for Phase 3
- Zero high-priority security vulnerabilities in dependencies
- Complete documentation and team onboarding materials

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