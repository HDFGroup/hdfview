# HDFView CI/CD Pipeline Guide

**Version**: 1.0
**Date**: September 15, 2025
**Status**: Production Ready

## Overview

This guide documents the complete CI/CD pipeline implementation for HDFView, covering all automated workflows, quality gates, security scanning, and release processes.

## Pipeline Architecture

### Workflow Overview

The HDFView CI/CD pipeline consists of four main GitHub Actions workflows:

1. **`maven-ci.yml`** - Core development workflow (build, test, artifacts)
2. **`maven-quality.yml`** - Quality gates and analysis
3. **`maven-security.yml`** - Security and dependency scanning
4. **`maven-release.yml`** - Release and artifact management

### Trigger Strategy

```yaml
# Development workflows (CI, Quality, Security)
on:
  push:
    branches: [ master-maven, develop, main ]
  pull_request:
    branches: [ master-maven, develop, main ]
  schedule:
    - cron: '0 2 * * *'  # Quality analysis daily
    - cron: '0 3 * * 0'  # Security scan weekly

# Release workflow
on:
  push:
    tags: [ 'v*.*.*', 'release-*' ]
  workflow_dispatch:
```

## Core Workflows

### 1. Maven CI Pipeline (`maven-ci.yml`)

**Purpose**: Primary development workflow for building, testing, and packaging

**Triggers**:
- Push to `master-maven`, `develop`, `main` branches
- Pull requests to these branches

**Key Features**:
- **Build Validation**: Maven compile with Java 21
- **Test Execution**: Parallel unit tests, serial integration tests
- **Artifact Generation**: JAR files and dependencies
- **HDF Library Integration**: Automatic installation and configuration
- **Test Reporting**: JUnit test results with GitHub integration

**Test Categories**:
```bash
# Unit tests (parallel execution)
mvn test -Dgroups="unit & fast"

# Integration tests (serial execution)
mvn test -Dgroups="integration"
```

**Outputs**:
- Build artifacts (`build-artifacts-{run_number}`)
- Test results (`test-results-{run_number}`)
- Build summary in GitHub step summary

### 2. Quality Gates Pipeline (`maven-quality.yml`)

**Purpose**: Code quality analysis and enforcement

**Triggers**:
- Same as CI pipeline
- Daily scheduled runs at 2 AM UTC

**Quality Metrics**:
- **Code Coverage**: JaCoCo analysis with 60% minimum threshold
- **Static Analysis**: PMD (max 50 violations allowed)
- **Code Style**: Checkstyle (no errors allowed)
- **Performance**: Test execution time monitoring

**Quality Gate Thresholds**:
```yaml
Coverage: >= 60% (required)
PMD Violations: <= 50 (blocking), <= 20 (warning)
Checkstyle Errors: 0 (blocking)
```

**Reporting**:
- Codecov integration for coverage tracking
- PR comments with quality reports
- GitHub step summary with metrics
- Archived quality reports (30-day retention)

**Quality Gate Failure**: Workflow fails if thresholds are not met

### 3. Security Scanning Pipeline (`maven-security.yml`)

**Purpose**: Security vulnerability detection and compliance

**Triggers**:
- Same as CI pipeline
- Weekly scheduled runs on Sundays at 3 AM UTC

**Security Components**:

#### OWASP Dependency Check
- **Scan Type**: Known vulnerability database
- **Threshold**: CVSS 7+ fails build
- **Suppression**: Configured in `.owasp-suppressions.xml`
- **Formats**: HTML, JSON, XML reports

#### CodeQL Analysis
- **Languages**: Java
- **Query Sets**: Security and quality, security extended
- **Configuration**: `.github/codeql/codeql-config.yml`
- **Integration**: GitHub Security tab

#### License Compliance
- **Prohibited Licenses**: GPL-2.0, GPL-3.0, AGPL, SSPL, BUSL
- **Detection**: Dependency tree analysis
- **Action**: Build failure on prohibited licenses

**Outputs**:
- Security vulnerability reports
- License compliance reports
- GitHub Security alerts integration

### 4. Release Management Pipeline (`maven-release.yml`)

**Purpose**: Automated release creation and artifact publishing

**Triggers**:
- Git tags matching `v*.*.*` or `release-*`
- Manual workflow dispatch

**Release Process**:

1. **Version Determination**:
   ```bash
   # From tag: v1.2.3 → version 1.2.3
   # From manual: current POM version (release mode)
   ```

2. **Quality Validation**:
   - Full test suite execution
   - Coverage threshold enforcement (release builds)
   - Build artifact verification

3. **Artifact Creation**:
   - Application JARs
   - Source JARs
   - JavaDoc JARs
   - Distribution packages

4. **Release Publication**:
   - GitHub Release creation
   - Asset upload to GitHub Releases
   - Maven packages to GitHub Packages

**Release Types**:
- **Release**: Full production release
- **Snapshot**: Development snapshot
- **Hotfix**: Emergency fix release

## Configuration Files

### 1. Build Configuration

#### `build.properties`
```properties
# HDF Library Paths
hdf5.lib.dir=/usr/lib/x86_64-linux-gnu
hdf.lib.dir=/usr/lib/x86_64-linux-gnu
platform.hdf.lib=/usr/lib/x86_64-linux-gnu

# CI Settings
ci.build=true
```

#### Maven JVM Settings
```bash
MAVEN_OPTS="-Xmx2g -Xms1g -XX:+UseParallelGC -Djava.awt.headless=true"
```

### 2. Security Configuration

#### `.owasp-suppressions.xml`
Suppression rules for:
- SWT platform-specific dependencies
- HDF native libraries
- Test-only dependencies
- Known false positives

#### `.github/codeql/codeql-config.yml`
- Security-focused query packs
- Path exclusions for build artifacts
- Java-specific analysis configuration

### 3. Dependency Management

#### `.github/dependabot.yml`
- Weekly dependency updates
- Grouped updates by category
- Security updates applied immediately
- Custom commit messages and labels

## Caching Strategy

### Maven Dependencies
```yaml
path: |
  ~/.m2/repository
  !~/.m2/repository/org/hdfgroup
key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```

### HDF Libraries
```yaml
path: |
  /usr/lib/x86_64-linux-gnu/libhdf*
  /usr/include/hdf*
key: ${{ runner.os }}-hdf-libs-v1
```

### Analysis Data
```yaml
path: |
  ~/.sonar/cache
  **/target/pmd-cache
  **/target/checkstyle-cache
key: ${{ runner.os }}-analysis-${{ github.sha }}
```

## Test Execution Strategy

### Test Categories

#### Unit Tests
- **Tags**: `@Tag("unit")` + `@Tag("fast")`
- **Execution**: Parallel (4 threads)
- **Environment**: Headless JVM
- **Duration**: < 5 minutes

#### Integration Tests
- **Tags**: `@Tag("integration")`
- **Execution**: Serial
- **Environment**: HDF libraries required
- **Duration**: 5-15 minutes

#### UI Tests
- **Tags**: `@Tag("ui")`
- **Execution**: Serial
- **Environment**: Display required (headless=false)
- **Duration**: 10-30 minutes

### Test Execution Commands

```bash
# Fast development cycle
mvn test -Dgroups="unit & fast"

# Full integration testing
mvn test -Dgroups="integration"

# UI testing (with display)
mvn test -Dgroups="ui" -Djava.awt.headless=false

# All tests
mvn test
```

## Artifact Management

### Build Artifacts

#### Application JARs
- `hdfview/target/hdfview-{version}.jar` - Main application
- `object/target/object-{version}.jar` - Core library
- `libs/*.jar` - Distribution JARs

#### Dependencies
- `target/lib/` - Runtime dependencies
- Platform-specific SWT JARs
- HDF native library references

### Release Artifacts

#### GitHub Releases
- Automatic release creation for tagged versions
- JAR file uploads
- Generated release notes
- Asset checksums

#### GitHub Packages
- Maven package publication
- Artifact metadata
- Dependency resolution

### Retention Policies
- **Build Artifacts**: 7 days
- **Test Results**: 30 days
- **Quality Reports**: 30 days
- **Security Reports**: 30 days
- **Release Artifacts**: 90 days

## Monitoring and Notifications

### Build Status
- GitHub commit status checks
- PR status reporting
- Branch protection integration

### Quality Reporting
- PR comments with quality metrics
- Coverage trend tracking (Codecov)
- Quality gate status in step summaries

### Security Alerts
- GitHub Security tab integration
- OWASP vulnerability reports
- License compliance notifications

### Failure Notifications
- GitHub Actions native notifications
- PR status updates
- Step summary error reporting

## Troubleshooting

### Common Issues

#### 1. HDF Library Installation Failures
```bash
# Symptoms: Native library not found errors
# Solution: Check HDF library cache and installation
sudo apt-get update
sudo apt-get install -y libhdf5-dev libhdf4-dev
ldconfig -p | grep hdf
```

#### 2. Test Compilation Failures
```bash
# Symptoms: Module access errors
# Solution: Verify JVM arguments for test execution
--add-opens java.base/java.lang=ALL-UNNAMED
--enable-native-access=jarhdf5
```

#### 3. Maven Dependency Resolution
```bash
# Symptoms: Dependency conflicts
# Solution: Clean and rebuild with fresh cache
mvn dependency:purge-local-repository
mvn clean compile
```

#### 4. Quality Gate Failures
```bash
# Coverage below threshold
mvn jacoco:report
# Check target/site/jacoco/index.html

# PMD violations
mvn pmd:pmd
# Check target/pmd.xml

# Checkstyle errors
mvn checkstyle:checkstyle
# Check target/checkstyle-result.xml
```

### Debug Commands

```bash
# Verbose Maven execution
mvn -X clean compile

# Test execution with debug output
mvn test -Dmaven.surefire.debug

# Dependency analysis
mvn dependency:tree -Dverbose

# Effective POM analysis
mvn help:effective-pom

# Active profiles
mvn help:active-profiles
```

## Security Considerations

### Secrets Management
- No secrets stored in repository
- GitHub tokens use built-in `GITHUB_TOKEN`
- Native library paths in public configuration

### Access Control
- Branch protection rules required
- PR reviews required for main branches
- Status checks must pass before merge

### Vulnerability Response
- Weekly security scans
- Immediate security update notifications
- Suppression files for false positives
- CVE tracking and remediation

## Performance Optimization

### Build Performance
- **Parallel Test Execution**: 4 threads for unit tests
- **Incremental Compilation**: Maven compiler incremental mode
- **Selective Testing**: Category-based test execution
- **Aggressive Caching**: Multi-level cache strategy

### Resource Usage
- **Memory Allocation**: 2GB heap for CI builds, 4GB for releases
- **Timeout Management**: Workflow timeouts prevent hung builds
- **Concurrent Jobs**: Multiple workflows run in parallel

### Optimization Results
- **Build Time**: ~15 minutes (CI), ~25 minutes (Quality), ~30 minutes (Release)
- **Test Execution**: ~5 minutes (unit), ~10 minutes (integration)
- **Cache Hit Rate**: >80% for Maven dependencies

## Migration Notes

### From Ant to Maven
- All Ant build references removed
- Maven-only build system
- Updated dependency management
- Native library integration adapted

### JUnit 4 to JUnit 5
- Test infrastructure updated in Phase 2A
- Category-based execution implemented
- Parallel test execution enabled
- Modern assertion patterns

## Maintenance

### Regular Tasks
- **Weekly**: Review Dependabot PRs
- **Monthly**: Update workflow dependencies
- **Quarterly**: Review security suppressions
- **Annually**: Evaluate new GitHub Actions features

### Version Updates
- GitHub Actions: Dependabot managed
- Maven plugins: Dependabot managed
- Java version: Manual evaluation required
- HDF libraries: Manual evaluation required

### Monitoring Health
- Build success rates
- Test execution times
- Quality metric trends
- Security vulnerability counts

## Support and Documentation

### Additional Resources
- [JUnit 5 Migration Guide](JUnit5-Migration-Guide.md)
- [Maven Build System Guide](Maven-Build-Guide.md)
- [Security Guidelines](Security-Guidelines.md)
- [Contributing Guidelines](../CONTRIBUTING.md)

### Team Contacts
- **CI/CD Issues**: HDFView development team
- **Security Concerns**: HDF Group security team
- **Build System**: Maven maintainers

### Feedback and Improvements
Report issues and suggestions through:
- GitHub Issues
- Team discussions
- Documentation PRs