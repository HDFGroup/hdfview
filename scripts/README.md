# Development Scripts

This directory contains helper scripts for common HDFView development tasks.

## Quick Reference

### 🧹 Clean Everything
```bash
./scripts/clean-all.sh
```
Deep clean to pristine state. Use when:
- Experiencing classloading issues
- Stale dependencies causing problems
- Want fresh start after major changes

### 🔨 Build for Development
```bash
./scripts/build-dev.sh              # Quick build (default)
./scripts/build-dev.sh --clean      # Clean + build
./scripts/build-dev.sh --with-tests # Build + run tests
```
Fast build skipping quality checks. Use for normal development.

### 🔍 Switch Debug Logging
```bash
./scripts/set-debug-logging.sh list     # List configs
./scripts/set-debug-logging.sh float16  # Debug Float16 issues
./scripts/set-debug-logging.sh default  # Minimal logging
```
Quick switch between debug configurations.

## Common Workflows

### First Time Setup
```bash
./scripts/clean-all.sh      # Ensure clean state
./scripts/build-dev.sh      # Build project
./run-hdfview.sh            # Launch HDFView
```

### Daily Development
```bash
# Make code changes, then:
mvn compile -pl object,hdfview -DskipTests
./run-hdfview.sh

# Or just:
./run-hdfview.sh  # Auto-builds if needed
```

### Debugging Issues
```bash
# Enable debug logging
./scripts/set-debug-logging.sh float16
mvn test-compile -pl hdfview -DskipTests

# Run specific test
mvn test -pl hdfview -Dtest=TestClassName#testMethod

# Restore normal logging
./scripts/set-debug-logging.sh default
```

### Fixing Build Problems
```bash
# Try incremental fix first:
mvn clean compile -pl object,hdfview -DskipTests

# If that doesn't work, deep clean:
./scripts/clean-all.sh
./scripts/build-dev.sh
```

## Script Details

### clean-all.sh
Removes:
- All Maven `target/` directories
- Maven local repository cache for hdfview
- Maven resolver status files
- `libs/` directory
- Stale `.class` files anywhere in project

Safe to run anytime - doesn't affect source code or configuration.

### build-dev.sh
Steps:
1. Compile object module
2. Package object JAR → `libs/`
3. Install object JAR → `~/.m2/repository`
4. Compile hdfview module
5. Compile test classes

Options:
- `--clean`: Run `mvn clean` first
- `--with-tests`: Run full test suite after build
- `--with-quality`: Include PMD/Checkstyle (much slower)

### set-debug-logging.sh
Manages logging configurations in `.claude/debug-configs/`:
- Backs up current config before switching
- Copies selected config to `hdfview/src/test/resources/simplelogger.properties`
- Lists available configurations with descriptions

Available configs:
- **default**: Minimal logging (info level)
- **debug-float16**: Trace logging for Float16/BFLOAT16/Float8 debugging

To add new configs, save to `.claude/debug-configs/simplelogger-<name>.properties`

## See Also

- **CLAUDE.md**: Full project documentation
- **run-hdfview.sh**: Main launcher script
- **.claude/debug-configs/README.md**: Debug logging guide
