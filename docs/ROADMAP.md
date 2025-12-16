# HDFView Development Roadmap

This document outlines potential areas for future development and improvement of HDFView.

**Last Updated**: December 16, 2025

---

## Completed Phases

### ✅ Phase 1: Maven Migration (2024)
- Removed Ant build system
- Implemented pure Maven multi-module build
- Configured cross-platform SWT support
- Integrated native library dependencies

### ✅ Phase 2A: JUnit 5 Migration (2024-2025)
- Migrated all tests to JUnit 5
- Implemented modern test infrastructure
- Added test categorization and tagging
- Enabled parallel test execution

### ✅ Phase 2B: CI/CD Pipeline (2025)
- Implemented multi-platform CI (Linux, macOS, Windows)
- Created automated quality gates
- Added security scanning
- Configured automated releases

### ✅ Phase 2C: Quality Analysis (2025)
- Integrated PMD static analysis
- Configured Checkstyle enforcement
- Implemented code coverage reporting
- Created quality management scripts

---

## Future Development Areas

### 1. Test Infrastructure Improvements

**Priority**: High
**Effort**: Medium

#### Outstanding Issues
- Resolve remaining UI test failures (4 tests)
- Fix upstream HDF library bugs affecting tests
- Improve test isolation and reliability

#### Potential Improvements
- Increase code coverage beyond 60% target
- Add more integration tests for complex workflows
- Implement property-based testing for data handling
- Create visual regression tests for UI

#### Test-Specific Improvements
- Investigate headless UI testing options (Xvfb, virtual display)
- Add performance benchmarks for large file operations
- Create smoke tests for critical user workflows

### 2. Code Quality Improvements

**Priority**: Medium
**Effort**: Ongoing

#### PMD Violation Reduction
- Current baseline: ~3850 violations
- Target: Reduce by 500 violations per quarter
- Focus areas:
  - Code complexity reduction
  - Dead code removal
  - Best practices enforcement

#### Code Coverage Enhancement
- Implement JaCoCo offline instrumentation
- Target: 70% line coverage, 60% branch coverage
- Focus on under-tested modules

#### Documentation
- Add missing JavaDoc for public APIs
- Create architecture decision records (ADRs)
- Document complex algorithms and data structures

### 3. Performance Optimization

**Priority**: Medium
**Effort**: High

#### Large File Handling
- Profile and optimize memory usage for large HDF5 files
- Implement lazy loading for dataset previews
- Add progress indicators for long operations
- Consider streaming approaches for massive datasets

#### UI Responsiveness
- Move long-running operations to background threads
- Implement cancellable operations
- Add responsive progress feedback
- Optimize table rendering for large datasets

#### Caching
- Implement intelligent caching for frequently accessed data
- Add cache management UI
- Consider LRU eviction strategies

### 4. Native Library Integration

**Priority**: Medium
**Effort**: Medium

#### Float8/Float16/BFLOAT16 Support
- Monitor HDF5 upstream for native support
- Remove workarounds when native types available
- Add comprehensive tests for new float types
- Update documentation

#### HDF5 2.0.0+ Migration
- Track HDF5 2.0.0 regression fixes (issue #6076)
- Plan migration path for HDF5 2.x series
- Test with latest HDF5 releases
- Update CI/CD for new HDF5 versions

### 5. User Experience Enhancements

**Priority**: Medium
**Effort**: Variable

#### UI Improvements
- Modernize look and feel
- Improve accessibility (WCAG compliance)
- Add keyboard shortcuts
- Implement undo/redo functionality

#### Data Editing
- Enhance compound dataset editing
- Add data validation during editing
- Improve error messages and recovery
- Add batch operations for datasets

#### File Operations
- Implement recent files list
- Add file comparison functionality
- Support drag-and-drop file opening
- Add bookmark/favorites for datasets

### 6. Feature Additions

**Priority**: Low to Medium
**Effort**: High

#### Data Analysis
- Add basic statistical analysis tools
- Implement data filtering and search
- Create data export to common formats (CSV, JSON)
- Add plotting capabilities for 1D/2D data

#### Metadata Management
- Improve attribute editing
- Add metadata templates
- Implement metadata validation
- Support metadata import/export

#### Multi-File Operations
- Support comparing multiple HDF files
- Add batch processing capabilities
- Implement file merging/splitting
- Support virtual datasets (VDS)

### 7. Platform Support

**Priority**: Low
**Effort**: Medium

#### Cross-Platform Improvements
- Test and document macOS-specific behaviors
- Improve Windows launcher experience
- Add Linux distribution packages
- Consider platform-specific optimizations

#### Deployment
- Create installer packages (DMG, MSI, DEB, RPM)
- Implement auto-update mechanism
- Add portable/standalone versions
- Consider containerization (Docker)

### 8. UI Framework Modernization (Phase 2D)

**Priority**: Deferred
**Effort**: Very High

**Note**: This was deferred during Phase 2 to prioritize test migration and quality improvements.

#### Research Required
- Evaluate JavaFX as SWT alternative
- Benchmark performance with large datasets
- Assess migration effort and risks
- Consider incremental migration path

#### Considerations
- SWT is mature but aging
- JavaFX has better modern UI capabilities
- Migration would be major undertaking
- Need to preserve existing functionality

See `.claude/Phase2D-UI-Framework-Research.md` for detailed research plan.

### 9. Developer Experience

**Priority**: Low
**Effort**: Low to Medium

#### Build System
- Consider Gradle as Maven alternative
- Optimize build performance
- Simplify native library configuration
- Improve error messages during build

#### Documentation
- Create video tutorials for common tasks
- Add inline code examples
- Improve API documentation
- Create troubleshooting guides

#### Tools
- Add code formatting automation (clang-format integration)
- Create development container setup
- Implement pre-commit hooks
- Add more helper scripts

### 10. Security and Maintenance

**Priority**: Ongoing
**Effort**: Low

#### Security
- Regular dependency updates
- Security vulnerability scanning
- Code security reviews
- Penetration testing consideration

#### Maintenance
- Java version updates (track Java LTS releases)
- Dependency updates (Maven plugins, libraries)
- Native library updates (HDF4, HDF5, NetCDF)
- Platform compatibility testing

---

## Contributing to Roadmap Items

Interested in working on any of these items? Here's how to get started:

1. **Check existing issues**: Many roadmap items may have related GitHub issues
2. **Discuss first**: Open an issue or discussion to talk about your approach
3. **Start small**: Consider starting with smaller improvements before major features
4. **Follow guidelines**: See `CONTRIBUTING.md` for development workflow
5. **Ask for help**: The community is here to support your contributions

---

## Prioritization Factors

When considering which roadmap items to pursue, consider:

- **User impact**: How many users will benefit?
- **Maintenance burden**: Will this increase or decrease maintenance?
- **Compatibility**: Does this maintain backward compatibility?
- **Effort**: What's the development and testing effort required?
- **Dependencies**: Are there blockers or prerequisites?

---

## Regular Maintenance Tasks

Some tasks should be performed regularly:

### Quarterly
- Review and update dependencies
- Analyze security scan results
- Review and reduce PMD violations (target: -500/quarter)
- Update documentation for new features

### Annually
- Review Java LTS roadmap
- Evaluate new testing frameworks
- Assess build system performance
- Review and update development tools

---

## How to Propose New Roadmap Items

Have an idea not listed here? We'd love to hear it!

1. Open a GitHub issue with the "enhancement" label
2. Describe the feature and its benefits
3. Consider implementation approach and effort
4. Discuss with maintainers and community
5. If accepted, it will be added to this roadmap

---

## References

- **Project Issues**: https://github.com/HDFGroup/hdfview/issues
- **Contributing Guide**: `CONTRIBUTING.md`
- **CI/CD Documentation**: `docs/guides/CI-CD-Pipeline-Guide.md`
- **HDF Group**: https://www.hdfgroup.org/

---

*This roadmap is a living document and will be updated as priorities evolve and new opportunities emerge.*
