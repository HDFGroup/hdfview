# HDFView Testing Guide

This guide explains how to run tests for HDFView locally and in CI environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Test Organization](#test-organization)
- [Running Tests Locally](#running-tests-locally)
- [Test Execution Options](#test-execution-options)
- [Simulating CI Environment](#simulating-ci-environment)
- [Troubleshooting](#troubleshooting)
- [CI/CD Testing](#cicd-testing)

---

## Prerequisites

### Required

1. **Java 21** - Installed and configured
2. **Maven 3.9+** - Build system
3. **HDF5 Native Libraries** - Configured in `build.properties`
4. **HDF4 Native Libraries** - Configured in `build.properties`

### Optional (for UI tests)

5. **Display Server** - X11 on Linux, native on macOS/Windows
6. **Xvfb** - Virtual framebuffer for headless testing (Linux only)

### Configuration

Ensure your `build.properties` file has the correct paths:

```properties
# HDF5 Library Configuration
hdf5.lib.dir=/path/to/hdf5/lib

# HDF4 Library Configuration
hdf.lib.dir=/path/to/hdf4/lib

# Runtime library path
platform.hdf.lib=/path/to/hdf5/lib
```

See `docs/build.properties.example` for a complete template.

---

## Quick Start

### Run All Tests

```bash
# Run all tests (object module + UI tests)
mvn test
```

### Run Specific Module

```bash
# Object module only (no display needed)
mvn test -pl object

# UI tests only (requires display)
mvn test -pl hdfview
```

### Expected Results

**Object Module:**
- 9 tests should run
- Tests HDF5 object model and file operations
- No display required

**UI Module:**
- 17 test classes for GUI functionality
- Requires display (real or virtual via Xvfb)
- Tests SWT widgets, menus, dialogs, etc.

---

## Test Organization

HDFView uses JUnit 5 with tag-based test organization:

### Test Tags

| Tag | Purpose | Count | Display Required |
|-----|---------|-------|------------------|
| `@Tag("unit")` | Fast unit tests | TBD | No |
| `@Tag("integration")` | Integration tests | 17 | Varies |
| `@Tag("ui")` | GUI/SWT tests | 17 | Yes |

### Test Modules

```
hdfview/
├── object/                    # HDF object model
│   └── src/test/java/
│       └── object/            # 9 tests (no display)
│           ├── DatasetTest.java
│           └── GroupTest.java
│
└── hdfview/                   # GUI application
    └── src/test/java/
        └── uitest/            # 17 UI test classes
            ├── TestAll.java   # Test suite
            ├── TestHDFViewMenu.java
            ├── TestTreeViewFiles.java
            └── ...
```

### Maven Surefire Executions

Tests run in multiple phases:

1. **default-test**: Unit tests (`@Tag("unit")`)
2. **unit-tests**: Unit tests in parallel (4 threads)
3. **integration-tests**: Integration tests serially
4. **ui-tests**: UI tests serially with display config

---

## Running Tests Locally

### Option 1: All Tests (Recommended)

```bash
# Run complete test suite
mvn test

# Show detailed output
mvn test -B

# Continue on failures to see all results
mvn test -Dmaven.test.failure.ignore=true
```

### Option 2: By Module

```bash
# Object module (fast, no display)
mvn test -pl object

# UI module (requires display)
mvn test -pl hdfview

# Multiple modules
mvn test -pl object,hdfview
```

### Option 3: By Test Tag

```bash
# Unit tests only
mvn test -Dgroups="unit"

# Integration tests only
mvn test -Dgroups="integration"

# UI tests only
mvn test -Dgroups="ui"

# Combined tags (AND)
mvn test -Dgroups="ui & integration"

# Combined tags (OR)
mvn test -Dgroups="ui | integration"
```

### Option 4: Specific Test Class

```bash
# Run single test class
mvn test -Dtest=TestHDFViewMenu

# Run multiple test classes
mvn test -Dtest=TestHDFViewMenu,TestTreeViewFiles

# Run test method pattern
mvn test -Dtest=TestHDFViewMenu#verifyOpenButtonEnabled
```

### Option 5: With Maven Phases

```bash
# Clean, compile, and test
mvn clean test

# Test with coverage report
mvn test jacoco:report

# Package without running tests
mvn package -DskipTests
```

---

## Test Execution Options

### Verbose Output

```bash
# Show all test output
mvn test -X

# Show stack traces on failures
mvn test -e

# Quiet mode (minimal output)
mvn test -q
```

### Parallel Execution

Unit tests run in parallel by default (4 threads). To change:

```bash
# Run with 8 threads
mvn test -DthreadCount=8

# Disable parallel execution
mvn test -Dparallel=none
```

### Test Debugging

```bash
# Debug specific test (suspend until debugger attaches)
mvn test -Dtest=TestHDFViewMenu -Dmaven.surefire.debug

# Custom debug port
mvn test -Dtest=TestHDFViewMenu \
  -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000"
```

### Skipping Tests

```bash
# Skip test compilation and execution
mvn package -DskipTests

# Compile but don't run tests
mvn package -Dmaven.test.skip.exec=true
```

---

## Simulating CI Environment

To test exactly as CI runs, use Xvfb for headless execution.

### Install Xvfb (Linux)

**Fedora/RHEL:**
```bash
sudo dnf install xorg-x11-server-Xvfb gtk3 gtk3-devel
```

**Ubuntu/Debian:**
```bash
sudo apt-get install xvfb libgtk-3-0 libgtk-3-dev x11-xserver-utils
```

### Run with Xvfb

```bash
# Run all tests with virtual display
xvfb-run -a -s "-screen 0 1024x768x24" mvn test -B

# Run UI tests only
xvfb-run -a -s "-screen 0 1024x768x24" mvn test -pl hdfview -B

# With specific display number
xvfb-run --server-num=99 mvn test -pl hdfview
```

### Xvfb Options Explained

- `-a` - Auto-select display number
- `-s "-screen 0 1024x768x24"` - Screen 0, 1024x768 resolution, 24-bit color
- `--server-num=99` - Use display :99
- `-B` - Maven batch mode (non-interactive)

### Check Display Configuration

```bash
# Verify DISPLAY is set
echo $DISPLAY

# List X displays
ps aux | grep X

# Test X connection
xdpyinfo -display :0
```

---

## Troubleshooting

### Display Errors

**Error:** `Cannot open display`

**Solution 1:** Export DISPLAY variable
```bash
export DISPLAY=:0
mvn test -pl hdfview
```

**Solution 2:** Use Xvfb
```bash
xvfb-run -a mvn test -pl hdfview
```

**Solution 3:** Check X11 permissions
```bash
xhost +local:
```

### Native Library Errors

**Error:** `UnsatisfiedLinkError: Cannot load library`

**Solution:** Verify library paths in `build.properties`
```bash
# Check HDF5 library exists
ls -la /path/to/hdf5/lib/libhdf5.so*

# Check HDF4 library exists
ls -la /path/to/hdf4/lib/libhdf.so*

# Verify LD_LIBRARY_PATH
echo $LD_LIBRARY_PATH
```

**Fix:** Update `build.properties` or set environment:
```bash
export LD_LIBRARY_PATH=/path/to/hdf5/lib:/path/to/hdf4/lib:$LD_LIBRARY_PATH
mvn test
```

### Test Discovery Issues

**Error:** `No tests discovered`

**Check test tags:**
```bash
# List all test classes
find . -name "*Test.java" -type f

# Search for test annotations
grep -r "@Test" hdfview/src/test/java/
```

**Verify surefire configuration:**
```bash
# Show effective POM
mvn help:effective-pom | grep -A 20 surefire
```

### Memory Issues

**Error:** `OutOfMemoryError`

**Solution:** Increase Maven memory
```bash
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
mvn test
```

### SWT/Display Initialization Failures

**Error:** `NoClassDefFoundError: org.eclipse.swt...`

**Solution:** Verify SWT platform dependency
```bash
# Check platform in pom.xml (should match your OS)
grep "swt.platform" hdfview/pom.xml

# For Linux: gtk.linux.x86_64
# For macOS: cocoa.macosx.x86_64 or cocoa.macosx.aarch64
# For Windows: win32.win32.x86_64
```

---

## CI/CD Testing

### GitHub Actions Workflows

Tests run automatically in CI via GitHub Actions:

- **Linux CI** - Ubuntu with Xvfb (`.github/workflows/ci-linux.yml`)
- **macOS CI** - macOS with native display (`.github/workflows/ci-macos.yml`)
- **Windows CI** - Windows Server (`.github/workflows/ci-windows.yml`)

### CI Test Execution

```yaml
# Linux (with Xvfb)
xvfb-run -a -s "-screen 0 1024x768x24" mvn test -B

# macOS (native display)
mvn test -B

# Windows (native display, UI tests may be disabled)
mvn test -B
```

### Viewing CI Test Results

1. Go to [GitHub Actions](https://github.com/byrnHDF/hdfview/actions)
2. Click on a workflow run
3. View job logs for test output
4. Download test report artifacts

### Local CI Simulation

To exactly replicate CI environment:

```bash
# Install dependencies (like CI)
./scripts/install-swtbot.sh

# Run tests (like CI)
xvfb-run -a -s "-screen 0 1024x768x24" mvn clean test -B \
  -Dmaven.test.failure.ignore=false
```

---

## Best Practices

### Development Workflow

1. **Before committing:** Run tests locally
   ```bash
   mvn test
   ```

2. **Fix failures:** Don't commit broken tests
   ```bash
   mvn test -Dtest=FailingTest -X
   ```

3. **Check coverage:** Ensure adequate test coverage
   ```bash
   mvn test jacoco:report
   # Open: target/site/jacoco/index.html
   ```

4. **Push and verify:** Check CI passes
   - All platform builds succeed
   - Tests pass on all platforms

### Writing New Tests

1. **Add appropriate tags:**
   ```java
   @Tag("unit")          // Fast unit test
   @Tag("integration")   // Integration test
   @Tag("ui")           // GUI test (requires display)
   ```

2. **Use descriptive names:**
   ```java
   @Test
   public void verifyOpenButtonEnabled() { }
   ```

3. **Clean up resources:**
   ```java
   @AfterEach
   public void tearDown() {
       // Close files, dispose widgets, etc.
   }
   ```

4. **Test locally before CI:**
   ```bash
   mvn test -Dtest=YourNewTest
   ```

---

## Additional Resources

- **JUnit 5 Documentation:** https://junit.org/junit5/docs/current/user-guide/
- **SWTBot Testing:** https://www.eclipse.org/swtbot/
- **Maven Surefire Plugin:** https://maven.apache.org/surefire/maven-surefire-plugin/
- **Project Overview:** See `CLAUDE.md` in project root
- **Build Instructions:** See `docs/Build_HDFView.txt`

---

## Quick Reference Commands

```bash
# Most common test commands

# 1. Run all tests
mvn test

# 2. Run object tests only (fast)
mvn test -pl object

# 3. Run UI tests with Xvfb
xvfb-run -a mvn test -pl hdfview

# 4. Run specific test
mvn test -Dtest=TestHDFViewMenu

# 5. Run with coverage
mvn test jacoco:report

# 6. Skip tests during build
mvn package -DskipTests

# 7. Clean and test
mvn clean test

# 8. Continue on failures
mvn test -Dmaven.test.failure.ignore=true
```

---

**Last Updated:** November 18, 2025
**HDFView Version:** 3.4-SNAPSHOT
**Java Version:** 21
**Maven Version:** 3.9+
