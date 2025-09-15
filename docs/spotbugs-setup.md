# SpotBugs Static Analysis Setup

## Current Status

SpotBugs is **configured but not active** due to Java 21 compatibility limitations.

## Issue
SpotBugs 4.8.6 does not support Java 21 bytecode (class file major version 68). When attempting to run SpotBugs analysis on Java 21 compiled classes, you'll see:
```
java.lang.IllegalArgumentException: Unsupported class file major version 68
```

## Configuration Status

✅ **SpotBugs Maven plugin configured** in parent POM
✅ **Exclusion rules created** (`spotbugs-exclude.xml`)
✅ **Quality gates defined** (failOnError, thresholds)
✅ **Report generation configured** (HTML and XML)
🚫 **Execution disabled** due to Java 21 incompatibility

## How to Enable SpotBugs

### Option 1: Wait for Java 21 Support (Recommended)
Monitor SpotBugs releases for Java 21 support and then:
1. Update SpotBugs version in `pom.xml`
2. Uncomment the `<executions>` section in the SpotBugs plugin
3. Run: `mvn clean verify`

### Option 2: Temporary Java 17 Compilation
If you need SpotBugs analysis immediately:
1. Temporarily change Java target to 17 in parent POM
2. Recompile: `mvn clean compile`
3. Run SpotBugs: `mvn spotbugs:spotbugs`
4. Restore Java 21 settings

### Option 3: Alternative Tools
Consider these Java 21 compatible alternatives:
- **PMD**: Supports Java 21 (version 7.0+)
- **Checkstyle**: Supports Java 21 (version 10.12+)
- **SonarQube**: Supports Java 21 (version 10.0+)

## SpotBugs Commands

```bash
# Manual SpotBugs execution (will fail with Java 21)
mvn spotbugs:spotbugs

# Check for SpotBugs issues and fail build if found
mvn spotbugs:check

# Generate SpotBugs report only
mvn spotbugs:spotbugs -DfailOnError=false

# Run with different threshold
mvn spotbugs:spotbugs -Dspotbugs.threshold=High
```

## Configuration Details

### Current Plugin Configuration
- **Effort**: Max (thorough analysis)
- **Threshold**: Medium (report medium and high priority issues)
- **Fail on Error**: false (for initial setup)
- **Output**: HTML and XML reports in `target/spotbugs/`
- **Exclusions**: HDFView-specific patterns in `spotbugs-exclude.xml`
- **Scope**: Main source only (excludes tests)
- **Analysis Target**: `hdf.*` and `ncsa.*` packages

### Quality Rules in spotbugs-exclude.xml
- SWT UI initialization patterns
- Native library integration (JNI)
- HDF4/HDF5 wrapper classes
- Event handling and disposal patterns
- Test classes excluded

## When SpotBugs is Enabled

### Integration Points
SpotBugs will automatically run during:
- `mvn verify` (full build with quality checks)
- `mvn site` (documentation generation)
- CI/CD pipelines configured for quality gates

### Expected Reports
- **HTML Report**: `target/spotbugs/spotbugsHtml.html`
- **XML Report**: `target/spotbugs/spotbugsXml.xml` (for CI integration)

### Build Behavior
- **Warnings**: Will be displayed but won't fail build
- **Errors**: Currently set to not fail build (can be changed)
- **Reports**: Always generated regardless of issues found

## Troubleshooting

### Common Issues
1. **"Unsupported class file major version 68"**
   - Expected with Java 21 - wait for SpotBugs update

2. **"FindBugs filter file not found"**
   - Ensure `spotbugs-exclude.xml` exists in project root

3. **Build performance impact**
   - SpotBugs adds ~30-60 seconds to build time
   - Use `-Dspotbugs.skip=true` to disable temporarily

### Monitoring SpotBugs Updates
Check for Java 21 support in:
- [SpotBugs GitHub Releases](https://github.com/spotbugs/spotbugs/releases)
- [SpotBugs Maven Plugin Releases](https://github.com/spotbugs/spotbugs-maven-plugin/releases)

## Future Enhancements

When SpotBugs is active, consider:
1. **Stricter quality gates** (reduce threshold tolerance)
2. **Custom rules** for HDFView-specific patterns
3. **CI integration** with quality reports
4. **Baseline establishment** and trend monitoring