# CI/CD Troubleshooting Guide

**Version**: 1.0
**Date**: September 15, 2025

## Quick Diagnosis

### Build Status Check
```bash
# Check workflow status
gh workflow list
gh run list --workflow=maven-ci.yml

# View specific run details
gh run view <run-id>
```

### Common Error Patterns

| Error Type | Symptoms | Quick Fix |
|------------|----------|-----------|
| HDF Library | `UnsatisfiedLinkError` | Check library installation and paths |
| Test Compilation | Module access errors | Verify JVM arguments |
| Maven Dependencies | Resolution failures | Clear cache and rebuild |
| Quality Gates | Coverage/PMD failures | Review quality reports |
| Security Scan | OWASP/CodeQL failures | Check suppressions and thresholds |

## Detailed Troubleshooting

### 1. Build Failures

#### Native Library Issues

**Symptoms**:
```
java.lang.UnsatisfiedLinkError: no hdf5_java in java.library.path
```

**Diagnosis**:
```bash
# Check if libraries are installed
ldconfig -p | grep hdf

# Verify library paths
ls -la /usr/lib/x86_64-linux-gnu/libhdf*
```

**Solution**:
```bash
# Reinstall HDF libraries
sudo apt-get update
sudo apt-get install -y libhdf5-dev libhdf4-dev

# Verify build.properties
cat build.properties | grep hdf

# Expected content:
# hdf5.lib.dir=/usr/lib/x86_64-linux-gnu
# hdf.lib.dir=/usr/lib/x86_64-linux-gnu
```

#### Java Module System Conflicts

**Symptoms**:
```
module java.base does not "opens java.lang" to unnamed module
```

**Solution**:
```xml
<!-- Add to surefire plugin configuration -->
<argLine>
  --add-opens java.base/java.lang=ALL-UNNAMED
  --add-opens java.base/java.time=ALL-UNNAMED
  --enable-native-access=jarhdf5
</argLine>
```

#### Maven Dependency Resolution

**Symptoms**:
```
Could not resolve dependencies
Conflicting dependency versions
```

**Diagnosis**:
```bash
# Analyze dependency tree
mvn dependency:tree -Dverbose

# Check for conflicts
mvn dependency:analyze
```

**Solution**:
```bash
# Clear local repository
mvn dependency:purge-local-repository

# Force update
mvn clean compile -U
```

### 2. Test Failures

#### Test Compilation Issues

**Symptoms**:
```
Test compilation failed
Cannot find symbol
```

**Solution**:
```bash
# Compile tests specifically
mvn test-compile

# Check test classpath
mvn dependency:build-classpath -DincludeScope=test
```

#### Test Execution Timeouts

**Symptoms**:
```
Test timeout after 120 seconds
```

**Solution**:
```xml
<!-- Increase timeout in surefire configuration -->
<forkedProcessTimeoutInSeconds>300</forkedProcessTimeoutInSeconds>
```

#### Parallel Test Conflicts

**Symptoms**:
```
Tests fail when run in parallel but pass individually
```

**Solution**:
```bash
# Disable parallel execution for problematic tests
mvn test -Djunit.jupiter.execution.parallel.enabled=false

# Or tag tests appropriately
@Tag("serial")
```

### 3. Quality Gate Failures

#### Coverage Below Threshold

**Symptoms**:
```
Coverage 45% is below minimum threshold of 60%
```

**Diagnosis**:
```bash
# Generate coverage report
mvn jacoco:report

# View coverage details
open target/site/jacoco/index.html
```

**Solutions**:
1. **Add more tests**: Focus on uncovered branches
2. **Remove dead code**: Delete unused methods
3. **Adjust threshold**: If appropriate for current codebase

#### PMD Violations

**Symptoms**:
```
PMD violations: 75 (exceeds maximum of 50)
```

**Diagnosis**:
```bash
# Generate PMD report
mvn pmd:pmd

# View violations
open target/site/pmd.html
```

**Solutions**:
```java
// Common violations and fixes:

// 1. Unused imports
// Remove: import java.util.List; (if unused)

// 2. Long methods
// Split large methods into smaller ones

// 3. Complex conditionals
// Extract to well-named boolean methods
boolean isValidData() {
    return data != null && data.length > 0;
}
```

#### Checkstyle Errors

**Symptoms**:
```
Checkstyle errors: 12 (maximum 0 allowed)
```

**Solutions**:
```java
// Common issues:

// 1. Missing @Override
@Override
public String toString() { ... }

// 2. Unused imports
// Remove unused import statements

// 3. Whitespace issues
// Fix spacing, indentation, line endings
```

### 4. Security Scan Issues

#### OWASP Dependency Check Failures

**Symptoms**:
```
High severity vulnerability found: CVE-2024-XXXX
```

**Diagnosis**:
```bash
# View dependency check report
open target/dependency-check-report.html
```

**Solutions**:

1. **Update dependency**:
```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>vulnerable-lib</artifactId>
    <version>2.1.0</version> <!-- Updated version -->
</dependency>
```

2. **Add suppression** (if false positive):
```xml
<!-- Add to .owasp-suppressions.xml -->
<suppress>
    <notes>False positive - library not used in vulnerable way</notes>
    <packageUrl regex="true">^pkg:maven/org\.example/vulnerable-lib@.*$</packageUrl>
    <cve>CVE-2024-XXXX</cve>
</suppress>
```

#### CodeQL Analysis Failures

**Symptoms**:
```
CodeQL found potential security vulnerabilities
```

**Solutions**:
1. **Review findings**: Check GitHub Security tab
2. **Fix legitimate issues**: Address real security problems
3. **Suppress false positives**: Use CodeQL queries

#### License Compliance Failures

**Symptoms**:
```
Prohibited license found: GPL-3.0
```

**Solutions**:
1. **Replace dependency**: Find alternative with compatible license
2. **Update license**: If dependency changed license terms
3. **Add exception**: If use is legally acceptable

### 5. Performance Issues

#### Slow Build Times

**Symptoms**:
```
Build taking > 20 minutes
Tests timing out
```

**Optimizations**:

1. **Parallel execution**:
```xml
<parallel>methods</parallel>
<threadCount>4</threadCount>
```

2. **Selective testing**:
```bash
# Run only fast tests during development
mvn test -Dgroups="unit & fast"
```

3. **Cache optimization**:
```yaml
# Ensure proper cache configuration
- name: Cache Maven Dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```

#### Memory Issues

**Symptoms**:
```
OutOfMemoryError during build
GC overhead limit exceeded
```

**Solutions**:
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx4g -Xms2g"

# Optimize GC for CI
export MAVEN_OPTS="$MAVEN_OPTS -XX:+UseParallelGC"
```

### 6. Workflow-Specific Issues

#### GitHub Actions Workflow Failures

**Symptoms**:
```
Workflow failed to start
Action not found
```

**Solutions**:

1. **Update action versions**:
```yaml
# Update to latest versions
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
```

2. **Check permissions**:
```yaml
permissions:
  contents: read
  checks: write
  security-events: write
```

3. **Validate workflow syntax**:
```bash
# Use GitHub CLI to validate
gh workflow view --yaml
```

#### Artifact Upload Failures

**Symptoms**:
```
Failed to upload artifacts
Artifact not found
```

**Solutions**:
```yaml
# Ensure artifacts exist before upload
- name: Upload Artifacts
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: build-artifacts
    path: |
      target/*.jar
      libs/*.jar
```

## Emergency Procedures

### Bypass Quality Gates (Emergency Only)

```bash
# Skip quality checks temporarily
mvn package -DskipTests -Dpmd.skip -Dcheckstyle.skip

# Skip security scan
mvn package -Dowasp.skip
```

### Rollback Procedures

1. **Revert problematic changes**:
```bash
git revert <commit-hash>
git push origin master-maven
```

2. **Disable failing workflow**:
```bash
# Temporarily disable via GitHub UI
# Or comment out problematic steps
```

3. **Emergency release**:
```bash
# Create hotfix release
git checkout -b hotfix/emergency-fix
# Make minimal changes
git tag v1.2.3-hotfix
git push origin v1.2.3-hotfix
```

## Monitoring and Alerting

### Health Check Commands

```bash
# Check recent workflow runs
gh run list --limit 10

# Monitor success rates
gh run list --workflow=maven-ci.yml | grep -c "completed"

# Check for security alerts
gh api repos/:owner/:repo/security-advisories
```

### Performance Baselines

| Metric | Target | Alert Threshold |
|--------|--------|----------------|
| Build Time | < 15 min | > 25 min |
| Test Execution | < 10 min | > 20 min |
| Coverage | ≥ 60% | < 50% |
| PMD Violations | ≤ 20 | > 50 |

### Log Analysis

```bash
# Download workflow logs
gh run download <run-id>

# Search for specific errors
grep -r "ERROR" .

# Analyze test failures
grep -r "FAILED" surefire-reports/
```

## Prevention Strategies

### Pre-commit Checks

```bash
# Run locally before pushing
mvn clean compile test

# Quick quality check
mvn jacoco:report pmd:pmd checkstyle:checkstyle
```

### Development Best Practices

1. **Test locally**: Always run tests before pushing
2. **Incremental commits**: Small, focused changes
3. **Feature branches**: Use branches for new features
4. **Regular updates**: Keep dependencies current
5. **Monitor builds**: Check CI status regularly

### Automated Monitoring

```yaml
# Add to workflow for monitoring
- name: Report Build Metrics
  run: |
    echo "Build duration: ${{ steps.build.outputs.duration }}"
    echo "Test count: ${{ steps.test.outputs.count }}"
    echo "Coverage: ${{ steps.coverage.outputs.percentage }}"
```

## Support Contacts

### Escalation Path

1. **Team Lead**: Development team issues
2. **DevOps Engineer**: Infrastructure problems
3. **Security Team**: Security scan failures
4. **HDF Group**: Critical production issues

### Resources

- **Documentation**: `/docs/CI-CD-Pipeline-Guide.md`
- **GitHub Issues**: Project issue tracker
- **Team Chat**: Development channel
- **Emergency**: On-call rotation

## Appendix

### Useful Commands Reference

```bash
# Maven commands
mvn clean compile test package
mvn dependency:tree dependency:analyze
mvn jacoco:report pmd:pmd checkstyle:checkstyle

# GitHub CLI commands
gh workflow list run
gh run view download rerun
gh pr status checks

# Debugging commands
mvn -X clean compile  # Verbose output
mvn help:effective-pom  # Effective configuration
java -version  # Java environment
mvn --version  # Maven environment
```

### Configuration Templates

#### Emergency build.properties
```properties
# Minimal configuration for emergency builds
hdf5.lib.dir=/usr/lib/x86_64-linux-gnu
hdf.lib.dir=/usr/lib/x86_64-linux-gnu
platform.hdf.lib=/usr/lib/x86_64-linux-gnu
ci.build=true
skip.native.tests=true
```

#### Minimal test execution
```bash
# Emergency test run (unit tests only)
mvn test -Dgroups="unit" -DfailIfNoTests=false
```