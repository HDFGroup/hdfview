# HDFView Codebase Analysis and Planning Session
**Date:** September 12, 2025  
**Purpose:** Initial codebase analysis and improvement planning for HDFView project

## Session Overview

This session involved a comprehensive analysis of the HDFView codebase and the creation of foundational documentation for future development work.

## Completed Tasks

### 1. CLAUDE.md Creation
Created a comprehensive guide for future Claude Code instances working with this repository, containing:
- **Project Overview**: HDFView as Java GUI for HDF file viewing/editing
- **Build System**: Maven-based with legacy Ant files, Java 21 target
- **Architecture**: Multi-module structure (repository, object, hdfview)
- **Development Workflow**: Native library setup, testing procedures
- **Key Technologies**: Eclipse SWT, NatTable, SWTBot, HDF4/5 native libraries

### 2. ImprovePlan.md Creation
Developed a comprehensive improvement roadmap covering 8 major areas:

#### Phase 1: Foundation (High Priority)
- Complete Maven migration and remove Ant build system
- Fix platform-specific SWT dependency resolution  
- Implement developer containers and setup automation
- Add basic static analysis and code quality tools

#### Phase 2: Modernization (Medium Priority)
- Upgrade to JUnit 5 and improve testing infrastructure
- Implement proper CI/CD with Maven
- Add code coverage and quality gates
- Evaluate UI framework migration options

#### Phase 3: Enhancement (Lower Priority)
- Architectural refactoring for better separation of concerns
- Performance optimization for large datasets
- Plugin architecture implementation
- Modern packaging and distribution

## Key Technical Insights

### Current State
- **Build System**: Dual Ant/Maven setup causing confusion
- **CI/CD**: GitHub Actions still use Ant while development uses Maven
- **UI Framework**: Eclipse SWT showing age, limited cross-platform consistency
- **Native Libraries**: Manual setup via build.properties with local paths
- **Testing**: Primarily UI tests using SWTBot, limited unit testing

### Improvement Opportunities
- **Developer Experience**: Containerized development environment needed
- **Code Quality**: Missing static analysis, code coverage reporting
- **Architecture**: Monolithic UI, tight coupling between UI and data models
- **Performance**: Synchronous I/O likely blocking UI thread operations
- **Distribution**: No automated cross-platform build process

## Repository Context
- **Project**: HDFView 3.3.3 (in development)
- **Maintainer**: The HDF Group
- **Technology Stack**: Java 21, Eclipse SWT, Maven, HDF4/5 native libraries
- **Branch**: master-maven (Maven migration branch)
- **Architecture**: 3 modules - repository (deps), object (HDF API), hdfview (GUI)

## Success Metrics Defined
- Reduce developer onboarding from days to hours
- Eliminate platform-specific build failures
- Achieve >80% code coverage
- Improve large file loading performance by 50%
- Increase contributor participation

## Next Steps
The improvement plan provides a foundation for:
1. Detailed implementation planning for each phase
2. Specific task breakdowns and timelines  
3. Technical spike investigations (especially UI framework evaluation)
4. Resource allocation and priority refinement

## Files Created
- `/home/byrn/HDF_Projects/hdfview/dev/CLAUDE.md` - Development guide for future Claude instances
- `/home/byrn/HDF_Projects/hdfview/dev/ImprovePlan.md` - Comprehensive improvement roadmap

---
*This summary captures the initial analysis and planning session. Future work should reference these documents when implementing specific improvements.*