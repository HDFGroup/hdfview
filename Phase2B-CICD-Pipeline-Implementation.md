# Phase 2B: CI/CD Pipeline - Detailed Implementation Plan

**Duration**: 2-3 weeks
**Status**: Ready to begin after Phase 2A Task 2.2 complete
**Dependencies**: JUnit 5 infrastructure established

## Overview

Replace Ant-based GitHub Actions workflows with comprehensive Maven-based CI/CD pipeline focused on development workflow and quality gates. Current analysis shows 10+ existing Ant workflows that need Maven equivalents.

## Current Workflow Analysis

### Existing Ant-Based Workflows
- **`ant.yml`**: Main CI build with multi-platform support
- **`daily-build.yml`**: Scheduled builds with artifact management
- **`release.yml`**: Release process automation
- **`ant-app.yml`**: Application packaging and distribution
- **Format workflows**: `clang-format-check.yml`, `clang-format-fix.yml`
- **Artifact workflows**: `publish-release.yml`, `publish-branch.yml`, `release-files.yml`

### Development-Focused Approach (per guidance)
- **Priority**: Development workflow over multi-platform matrix builds
- **Focus**: Fast feedback, quality gates, development support
- **Defer**: Complex multi-platform packaging until Phase 3

## Detailed Task Implementation

### Task 2.8: Audit and Plan CI/CD Migration (2 days)

#### Day 1: Comprehensive Workflow Analysis
**Objective**: Document current CI/CD capabilities and requirements

**Actions**:
1. **Analyze Current Ant Workflows**:
   ```bash
   # Document workflow structure
   for file in .github/workflows/*.yml; do
     echo "=== $file ==="
     grep -E "(name:|on:|jobs:|run:|uses:)" "$file" | head -20
   done > current-workflows-analysis.md
   ```

2. **Map Ant Tasks to Maven Equivalents**:
   | Ant Workflow | Current Function | Maven Equivalent | Priority |
   |--------------|------------------|------------------|----------|
   | `ant.yml` | Build + Test | `mvn compile test` | High |
   | `daily-build.yml` | Scheduled build | Maven + caching | Medium |
   | `release.yml` | Release process | Maven release plugin | High |
   | `ant-app.yml` | App packaging | Maven assembly | Medium |

3. **Identify Development Workflow Requirements**:
   - Pull request validation (build, test, quality)
   - Push to main branch validation
   - Dependency security scanning
   - Code coverage reporting
   - Static analysis reporting
   - Build artifact generation and caching

#### Day 2: Maven-Only Migration Strategy and Risk Assessment
**Objective**: Plan direct migration to Maven workflows without parallel operation

**Actions**:
1. **Create Direct Migration Strategy**:
   - **Week 1**: Create new Maven workflows on feature branch
   - **Week 1**: Comprehensive testing and validation of all functionality
   - **Week 1**: Switch branch protection to Maven workflows immediately
   - **Week 1**: Disable/remove Ant workflows (keep commented as backup)

2. **Risk Assessment and Mitigation**:
   | Risk | Impact | Mitigation |
   |------|--------|------------|
   | Complete workflow failure | High | Comprehensive feature branch testing + rollback plan |
   | Missing Ant functionality | High | Detailed feature mapping and validation |
   | Team workflow disruption | Medium | Clear documentation and immediate team communication |
   | Performance regression | Medium | Optimize caching and parallelization from day 1 |

3. **Define Success Metrics**:
   - Build time <10 minutes for development workflow
   - Quality analysis completion within build time
   - Zero false positives in quality gates
   - 100% feature parity with current Ant workflows

**Deliverables**:
- Complete current workflow analysis document
- Migration strategy with timeline and risk mitigation
- Success metrics and validation criteria

### Task 2.9: Create Core Maven Workflows (4 days)

#### Day 1: Development Workflow - Build and Test
**Objective**: Create primary development workflow for PR and push validation

**Actions**:
1. **Create `.github/workflows/maven-ci.yml`**:
   ```yaml
   name: Maven CI Pipeline

   on:
     push:
       branches: [ master-maven, develop ]
     pull_request:
       branches: [ master-maven, develop ]

   permissions:
     contents: read
     checks: write
     pull-requests: write

   jobs:
     build-and-test:
       runs-on: ubuntu-latest
       timeout-minutes: 15

       steps:
       - name: Checkout Code
         uses: actions/checkout@v4

       - name: Set up JDK 21
         uses: actions/setup-java@v4
         with:
           java-version: '21'
           distribution: 'temurin'

       - name: Cache Maven dependencies
         uses: actions/cache@v3
         with:
           path: ~/.m2
           key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
           restore-keys: ${{ runner.os }}-m2

       - name: Set up build.properties
         run: |
           cp build.properties.template build.properties
           # Configure with dummy paths for CI
           sed -i 's|/path/to/hdf5|/usr/lib|g' build.properties
           sed -i 's|/path/to/hdf4|/usr/lib|g' build.properties

       - name: Install HDF Libraries (System)
         run: |
           sudo apt-get update
           sudo apt-get install -y libhdf5-dev libhdf4-dev

       - name: Maven Compile
         run: mvn clean compile -B

       - name: Run Unit Tests
         run: mvn test -Dgroups="unit & fast" -B

       - name: Run Integration Tests
         run: mvn test -Dgroups="integration" -B

       - name: Generate Test Report
         uses: dorny/test-reporter@v1
         if: success() || failure()
         with:
           name: Maven Tests
           path: '**/target/surefire-reports/*.xml'
           reporter: java-junit
   ```

2. **Test Workflow Development**:
   - Create feature branch with workflow
   - Test with actual PR to validate functionality
   - Monitor build times and optimize caching

#### Day 2: Quality Gate Workflow - Coverage and Analysis
**Objective**: Create comprehensive quality analysis workflow

**Actions**:
1. **Create `.github/workflows/maven-quality.yml`**:
   ```yaml
   name: Maven Quality Gates

   on:
     push:
       branches: [ master-maven ]
     pull_request:
       branches: [ master-maven ]

   jobs:
     quality-analysis:
       runs-on: ubuntu-latest
       timeout-minutes: 20

       steps:
       - name: Checkout Code
         uses: actions/checkout@v4
         with:
           fetch-depth: 0  # Full history for analysis

       - name: Set up JDK 21
         uses: actions/setup-java@v4
         with:
           java-version: '21'
           distribution: 'temurin'

       - name: Cache Maven dependencies
         uses: actions/cache@v3
         with:
           path: ~/.m2
           key: ${{ runner.os }}-m2-quality-${{ hashFiles('**/pom.xml') }}

       - name: Set up build.properties
         run: |
           cp build.properties.template build.properties
           # Configure for CI environment

       - name: Run Tests with Coverage
         run: mvn clean test jacoco:report -B

       - name: Upload Coverage to Codecov
         uses: codecov/codecov-action@v3
         with:
           files: ./target/site/jacoco/jacoco.xml
           flags: unittests
           name: codecov-umbrella

       - name: Generate Coverage Report
         run: |
           mvn jacoco:report
           echo "## Code Coverage Report" >> $GITHUB_STEP_SUMMARY
           echo "Coverage reports generated in target/site/jacoco/" >> $GITHUB_STEP_SUMMARY

       - name: Archive Coverage Reports
         uses: actions/upload-artifact@v4
         with:
           name: coverage-reports
           path: |
             **/target/site/jacoco/
             **/target/surefire-reports/
   ```

2. **Integrate Static Analysis** (Phase 2C dependency):
   - Configure PMD and Checkstyle execution
   - Set up quality gate thresholds
   - Generate analysis reports

#### Day 3: Dependency Management and Security
**Objective**: Add dependency vulnerability scanning and management

**Actions**:
1. **Add Dependency Check to Quality Workflow**:
   ```yaml
       - name: OWASP Dependency Check
         run: mvn org.owasp:dependency-check-maven:check -B

       - name: Upload Dependency Check Report
         uses: actions/upload-artifact@v4
         if: success() || failure()
         with:
           name: dependency-check-report
           path: target/dependency-check-report.html
   ```

2. **Configure GitHub Security Features**:
   ```yaml
       - name: Initialize CodeQL
         uses: github/codeql-action/init@v2
         with:
           languages: java

       - name: Perform CodeQL Analysis
         uses: github/codeql-action/analyze@v2
   ```

3. **Set up Dependabot** (`.github/dependabot.yml`):
   ```yaml
   version: 2
   updates:
     - package-ecosystem: "maven"
       directory: "/"
       schedule:
         interval: "weekly"
       open-pull-requests-limit: 10
   ```

#### Day 4: Build Optimization and Caching Strategy
**Objective**: Optimize build performance and implement comprehensive caching

**Actions**:
1. **Advanced Maven Caching**:
   ```yaml
       - name: Cache Maven Repository
         uses: actions/cache@v3
         with:
           path: |
             ~/.m2/repository
             !~/.m2/repository/org/hdfgroup
           key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
           restore-keys: |
             ${{ runner.os }}-maven-

       - name: Cache HDF Libraries
         uses: actions/cache@v3
         with:
           path: /usr/lib/hdf*
           key: ${{ runner.os }}-hdf-libs-${{ hashFiles('.github/workflows/install-hdf.sh') }}
   ```

2. **Parallel Build Configuration**:
   ```bash
   mvn clean compile test -B \
     -Dmaven.test.parallel=methods \
     -Dmaven.test.threadCount=4 \
     -Dmaven.compile.fork=true
   ```

3. **Build Performance Monitoring**:
   - Add build time tracking
   - Monitor cache hit rates
   - Track dependency download times

**Deliverables**:
- Core Maven CI workflow for development
- Quality gate workflow with coverage and analysis
- Security scanning and dependency management
- Optimized build performance with caching

### Task 2.10: Configure Build Optimization (2 days)

#### Day 1: Advanced Caching and Performance Tuning
**Objective**: Optimize build times and resource usage

**Actions**:
1. **Implement Multi-Level Caching Strategy**:
   ```yaml
   # Layer 1: Maven repository cache
   - name: Cache Maven Repository
     uses: actions/cache@v3
     with:
       path: ~/.m2/repository
       key: maven-repo-${{ runner.os }}-${{ hashFiles('**/pom.xml') }}

   # Layer 2: Compiled classes cache
   - name: Cache Compiled Classes
     uses: actions/cache@v3
     with:
       path: |
         **/target/classes
         **/target/test-classes
       key: compiled-${{ runner.os }}-${{ hashFiles('**/*.java', '**/pom.xml') }}

   # Layer 3: Static analysis cache
   - name: Cache Analysis Data
     uses: actions/cache@v3
     with:
       path: |
         ~/.sonar/cache
         **/target/pmd-cache
       key: analysis-${{ runner.os }}-${{ github.sha }}
       restore-keys: analysis-${{ runner.os }}-
   ```

2. **Configure Maven Performance Options**:
   ```xml
   <!-- .mvn/maven.config -->
   -Dmaven.artifact.threads=10
   -Dmaven.compile.fork=true
   -Dmaven.test.redirectTestOutputToFile=true
   -XX:+TieredCompilation
   -XX:TieredStopAtLevel=1
   ```

3. **Implement Incremental Builds**:
   ```yaml
   - name: Check for changes
     id: changes
     uses: dorny/paths-filter@v2
     with:
       filters: |
         java:
           - '**/*.java'
         pom:
           - '**/pom.xml'
         resources:
           - '**/src/main/resources/**'
   ```

#### Day 2: Build Performance Monitoring and Reporting
**Objective**: Track and monitor build performance metrics

**Actions**:
1. **Add Build Performance Tracking**:
   ```yaml
   - name: Record Build Start Time
     run: echo "BUILD_START=$(date +%s)" >> $GITHUB_ENV

   - name: Maven Build with Timing
     run: |
       time mvn clean compile test -B

   - name: Calculate Build Duration
     run: |
       BUILD_END=$(date +%s)
       DURATION=$((BUILD_END - BUILD_START))
       echo "Build took ${DURATION} seconds"
       echo "BUILD_DURATION=${DURATION}" >> $GITHUB_ENV
   ```

2. **Create Performance Dashboard**:
   - Track build times over time
   - Monitor cache hit rates
   - Alert on performance regressions

3. **Optimize Resource Usage**:
   ```yaml
   - name: Configure JVM for CI
     run: |
       export MAVEN_OPTS="-Xmx2g -Xms1g -XX:+UseParallelGC"
       echo "MAVEN_OPTS=${MAVEN_OPTS}" >> $GITHUB_ENV
   ```

**Deliverables**:
- Comprehensive caching strategy reducing build times by 50%
- Build performance monitoring and alerting
- Resource optimization for CI environment

### Task 2.11: Set Up Quality Reporting and Gates (3 days)

#### Day 1: Code Coverage Integration and Reporting
**Objective**: Implement comprehensive code coverage tracking and reporting

**Actions**:
1. **Enhanced JaCoCo Configuration** (build on existing Phase 1 setup):
   ```xml
   <!-- Add to parent POM -->
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <configuration>
           <rules>
               <rule>
                   <element>BUNDLE</element>
                   <limits>
                       <limit>
                           <counter>LINE</counter>
                           <value>COVEREDRATIO</value>
                           <minimum>0.60</minimum>
                       </limit>
                   </limits>
               </rule>
           </rules>
       </configuration>
   </plugin>
   ```

2. **Coverage Reporting Workflow Integration**:
   ```yaml
   - name: Generate Coverage Report
     run: mvn jacoco:report

   - name: Coverage Comment on PR
     uses: madrapps/jacoco-report@v1.6.1
     if: github.event_name == 'pull_request'
     with:
       paths: |
         ${{ github.workspace }}/target/site/jacoco/jacoco.xml
       token: ${{ secrets.GITHUB_TOKEN }}
       min-coverage-overall: 60
       min-coverage-changed-files: 80
   ```

3. **Coverage Trend Tracking**:
   ```yaml
   - name: Upload Coverage Data
     uses: codecov/codecov-action@v3
     with:
       files: target/site/jacoco/jacoco.xml
       flags: unittests
       fail_ci_if_error: false
   ```

#### Day 2: Static Analysis Reporting (depends on Phase 2C)
**Objective**: Integrate static analysis tools with CI reporting

**Actions**:
1. **PMD Integration** (Java 21 compatible):
   ```yaml
   - name: Run PMD Analysis
     run: mvn pmd:pmd pmd:cpd -B

   - name: PMD Report
     uses: jwgmeligmeyling/pmd-github-action@master
     if: success() || failure()
     with:
       path: '**/target/pmd.xml'
   ```

2. **Checkstyle Integration**:
   ```yaml
   - name: Run Checkstyle
     run: mvn checkstyle:check -B

   - name: Annotate Checkstyle Results
     uses: jwgmeligmeyling/checkstyle-github-action@master
     with:
       path: '**/target/checkstyle-result.xml'
   ```

3. **Quality Gate Integration**:
   ```yaml
   - name: Quality Gate Check
     run: |
       # Fail build if quality thresholds not met
       mvn verify -Dquality.failOnError=true
   ```

#### Day 3: PR Status Checks and Dashboard
**Objective**: Create comprehensive quality reporting and PR integration

**Actions**:
1. **PR Status Checks Configuration**:
   ```yaml
   - name: Post Quality Summary
     uses: actions/github-script@v6
     if: github.event_name == 'pull_request'
     with:
       script: |
         const fs = require('fs');

         // Read coverage data
         const coverage = JSON.parse(fs.readFileSync('target/site/jacoco/jacoco.json'));

         // Create quality summary
         const summary = `
         ## Quality Summary
         - **Coverage**: ${coverage.lineCoverage}%
         - **PMD Issues**: ${pmdIssues}
         - **Checkstyle Issues**: ${checkstyleIssues}
         `;

         github.rest.issues.createComment({
           issue_number: context.issue.number,
           owner: context.repo.owner,
           repo: context.repo.repo,
           body: summary
         });
   ```

2. **Quality Dashboard Creation**:
   - Set up quality metrics collection
   - Create trend visualizations
   - Configure alerting for regressions

**Deliverables**:
- Comprehensive coverage reporting with PR integration
- Static analysis integration and reporting
- Quality gates preventing regression
- Quality dashboard and trend monitoring

### Task 2.12: Implement Artifact and Release Management (2 days)

#### Day 1: Artifact Management and GitHub Packages
**Objective**: Set up artifact generation and repository management

**Actions**:
1. **Configure GitHub Packages**:
   ```xml
   <!-- Add to parent POM -->
   <distributionManagement>
       <repository>
           <id>github</id>
           <name>HDFView GitHub Packages</name>
           <url>https://maven.pkg.github.com/HDFGroup/hdfview</url>
       </repository>
   </distributionManagement>
   ```

2. **Artifact Generation Workflow**:
   ```yaml
   name: Build and Publish Artifacts

   on:
     push:
       tags: [ 'v*' ]
     workflow_dispatch:

   jobs:
     build-artifacts:
       runs-on: ubuntu-latest
       steps:
       - name: Checkout Code
         uses: actions/checkout@v4

       - name: Build Application JAR
         run: mvn clean package -DskipTests

       - name: Create Distribution
         run: mvn assembly:single

       - name: Upload Artifacts
         uses: actions/upload-artifact@v4
         with:
           name: hdfview-artifacts
           path: |
             target/hdfview-*.jar
             target/hdfview-*-distribution.zip
   ```

#### Day 2: Release Automation
**Objective**: Automate release process with proper versioning

**Actions**:
1. **Release Workflow**:
   ```yaml
   name: Release

   on:
     workflow_dispatch:
       inputs:
         version:
           description: 'Release version'
           required: true
           type: string

   jobs:
     release:
       runs-on: ubuntu-latest
       steps:
       - name: Create Release
         uses: actions/create-release@v1
         with:
           tag_name: v${{ github.event.inputs.version }}
           release_name: Release ${{ github.event.inputs.version }}
           draft: false
           prerelease: false
   ```

2. **Version Management**:
   - Configure Maven release plugin
   - Set up automatic changelog generation
   - Implement semantic versioning

**Deliverables**:
- Automated artifact generation and publishing
- GitHub Packages integration
- Release automation with proper versioning

### Task 2.13: Documentation and Team Integration (2 days)

#### Day 1: CI/CD Documentation Creation
**Objective**: Create comprehensive documentation for new CI/CD system

**Actions**:
1. **Create CI/CD Documentation** (`docs/ci-cd-guide.md`):
   ```markdown
   # HDFView CI/CD Guide

   ## Overview
   HDFView uses Maven-based GitHub Actions workflows for continuous integration.

   ## Workflows
   - **maven-ci.yml**: Primary development workflow
   - **maven-quality.yml**: Quality gates and analysis
   - **maven-artifacts.yml**: Artifact generation and publishing

   ## Development Workflow
   1. Create feature branch
   2. Push triggers CI build and tests
   3. Quality analysis runs automatically
   4. PR requires passing quality gates
   5. Merge triggers artifact generation

   ## Quality Gates
   - Minimum 60% code coverage
   - Zero high-priority PMD issues
   - Zero Checkstyle violations
   - All tests must pass
   ```

2. **Update Contributing Guidelines**:
   - Document new PR requirements
   - Explain quality gate expectations
   - Provide troubleshooting guide

#### Day 2: Team Onboarding and Troubleshooting
**Objective**: Prepare team for new CI/CD system

**Actions**:
1. **Create Troubleshooting Guide** (`docs/ci-cd-troubleshooting.md`):
   ```markdown
   # CI/CD Troubleshooting

   ## Common Issues

   ### Build Fails on Coverage Check
   - **Cause**: Coverage below 60% threshold
   - **Solution**: Add tests or adjust exclusions

   ### PMD/Checkstyle Violations
   - **Cause**: Code style violations
   - **Solution**: Run `mvn pmd:check checkstyle:check` locally
   ```

2. **Setup Build Status Badges**:
   ```markdown
   [![CI](https://github.com/HDFGroup/hdfview/actions/workflows/maven-ci.yml/badge.svg)](https://github.com/HDFGroup/hdfview/actions/workflows/maven-ci.yml)
   [![Quality](https://github.com/HDFGroup/hdfview/actions/workflows/maven-quality.yml/badge.svg)](https://github.com/HDFGroup/hdfview/actions/workflows/maven-quality.yml)
   ```

3. **Team Training Materials**:
   - Create CI/CD overview presentation
   - Document migration from Ant workflows
   - Provide local development setup guide

**Deliverables**:
- Comprehensive CI/CD documentation
- Team onboarding materials
- Troubleshooting guides and support resources

## Success Metrics and Acceptance Criteria

### Quantitative Goals
- **Build Time**: <10 minutes for development workflow
- **Quality Analysis**: Complete within build time
- **Cache Hit Rate**: >80% for Maven dependencies
- **Performance**: 50% improvement over Ant workflows

### Qualitative Goals
- Complete feature parity with existing Ant workflows
- Automated quality gates preventing regression
- Developer-friendly CI/CD experience
- Comprehensive documentation and support

### Final Validation Checklist
- [ ] All Ant workflows have Maven equivalents
- [ ] Development builds complete in <10 minutes
- [ ] Quality gates functioning and enforced
- [ ] Artifact generation and publishing working
- [ ] Documentation complete and accessible
- [ ] Team onboarded and trained

## Risk Mitigation

### Technical Risks
- **Performance Regression**: Monitor build times, optimize caching
- **Quality Gate False Positives**: Comprehensive testing and validation
- **HDF Library Integration**: Test native library loading in CI environment

### Process Risks
- **Team Disruption**: Parallel implementation, gradual migration
- **Feature Gaps**: Comprehensive feature mapping and validation
- **Documentation Gaps**: Extensive documentation and training materials

### Mitigation Strategies
- Implement new workflows alongside existing Ant workflows
- Validate on feature branches before switching defaults
- Provide comprehensive documentation and troubleshooting guides
- Establish clear rollback procedures if issues arise

This implementation plan provides detailed, actionable steps for creating a modern, efficient CI/CD pipeline that supports HDFView development while maintaining quality and performance standards.