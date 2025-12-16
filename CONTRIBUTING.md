# Contributing to HDFView

Thank you for your interest in contributing to HDFView! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Environment Setup](#development-environment-setup)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Code Quality](#code-quality)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Reporting Issues](#reporting-issues)

## Getting Started

HDFView is a Java-based GUI application for viewing and editing HDF files. The project uses Maven for build management and Eclipse SWT for the user interface.

### Prerequisites

- **Java 21** or later
- **Maven 3.6+**
- **HDF4 and HDF5 native libraries**
- **Git**

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/HDFGroup/hdfview.git
   cd hdfview
   ```

2. **Configure native libraries**
   ```bash
   # Copy the example configuration
   cp docs/build.properties.example build.properties

   # Edit build.properties to point to your HDF4/HDF5 installations
   vi build.properties
   ```

3. **Build the project**
   ```bash
   # Quick development build (skips tests and quality checks)
   ./scripts/build-dev.sh

   # Or full build with tests
   mvn clean install
   ```

4. **Run HDFView**
   ```bash
   ./run-hdfview.sh
   ```

## Development Environment Setup

### Required Configuration

Create a `build.properties` file in the project root (use `docs/build.properties.example` as template):

```properties
# HDF4 library path
hdf.lib.dir=/path/to/hdf4/lib

# HDF5 library path
hdf5.lib.dir=/path/to/hdf5/lib

# HDF5 plugin directory
hdf5.plugin.dir=/path/to/hdf5/plugin

# Runtime library path (LD_LIBRARY_PATH on Linux)
platform.hdf.lib=${hdf5.lib.dir}:${hdf.lib.dir}
```

### IDE Setup

The project can be imported into any IDE that supports Maven:

- **Eclipse**: Import as Maven project
- **IntelliJ IDEA**: Open the project directory
- **VS Code**: Open the project directory with Java extensions installed

## Development Workflow

### Build Commands

```bash
# Quick development build
./scripts/build-dev.sh

# Clean everything and build from scratch
./scripts/clean-all.sh
./scripts/build-dev.sh

# Full build with tests
mvn clean install

# Package without tests
mvn clean package -DskipTests
```

### Helper Scripts

The `scripts/` directory contains useful development tools:

- **`clean-all.sh`** - Deep clean to pristine state
- **`build-dev.sh`** - Quick development build
- **`set-debug-logging.sh`** - Toggle debug logging levels
- **`check-quality.sh`** - Run quality analysis locally
- **`analyze-coverage.sh`** - Generate code coverage reports

### Running the Application

Use the launcher scripts for easy execution:

```bash
# Launch with direct JAR (recommended)
./run-hdfview.sh

# Launch with debug logging
./run-hdfview.sh --debug

# Interactive mode
./run-hdfview.sh --choose
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TestClassName

# Run specific test method
mvn test -Dtest=TestClassName#methodName

# Run only unit tests (fast)
mvn test -Dgroups="unit & fast"

# Run integration tests
mvn test -Dgroups="integration"
```

### Test Categories

Tests are organized using JUnit 5 tags:
- `@Tag("unit")` - Fast unit tests
- `@Tag("integration")` - Integration tests
- `@Tag("ui")` - UI tests (require display)
- `@Tag("fast")` - Quick tests

### Writing Tests

- Use JUnit 5 for all new tests
- Follow existing test patterns in the codebase
- Use descriptive test names
- Include test documentation for complex scenarios
- Ensure tests are deterministic and not flaky

See `docs/Testing-Guide.md` for detailed testing documentation.

## Code Quality

### Quality Standards

The project enforces quality standards through automated checks:

- **Code Coverage**: 60% line coverage, 50% branch coverage target
- **PMD**: Static analysis for code quality
- **Checkstyle**: Code style enforcement
- **Security**: OWASP dependency checking

### Running Quality Checks Locally

```bash
# Quick validation (recommended before commit)
./scripts/validate-quality.sh

# Comprehensive analysis
./scripts/check-quality.sh

# Generate coverage report
./scripts/analyze-coverage.sh
```

### CI/CD

All pull requests trigger automated builds and quality checks:
- Linux, macOS, and Windows builds
- Test execution (object module)
- Code quality analysis
- Security scanning

See `docs/guides/CI-CD-Pipeline-Guide.md` for complete CI/CD documentation.

## Pull Request Process

### Before Creating a PR

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Write clean, documented code
   - Add or update tests as needed
   - Run quality checks locally

3. **Test your changes**
   ```bash
   mvn clean install
   ./scripts/validate-quality.sh
   ```

4. **Commit your changes**
   ```bash
   git add .
   git commit -m "Brief description of changes"
   ```

### Creating the PR

1. **Push your branch**
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Open a Pull Request** on GitHub with:
   - Clear title describing the change
   - Detailed description of what and why
   - Reference to related issues (if any)
   - Test results or evidence of testing

3. **Address review feedback**
   - Respond to reviewer comments
   - Make requested changes
   - Push additional commits as needed

### PR Requirements

- All CI checks must pass
- Code review approval from at least one maintainer
- No merge conflicts with target branch
- Documentation updated if needed

## Coding Standards

### Style Guidelines

The project follows adapted Google Java Style:
- Use spaces for indentation (2 spaces)
- Maximum line length: 100 characters
- Use meaningful variable and method names
- Add JavaDoc for public APIs

### Best Practices

- **Avoid over-engineering**: Keep changes focused and minimal
- **No premature optimization**: Write clear code first
- **Security conscious**: Watch for injection vulnerabilities
- **Error handling**: Validate at system boundaries (user input, external APIs)
- **Logging**: Use SLF4J for logging
- **Null safety**: Check for null appropriately

### Documentation

- Update JavaDoc for public API changes
- Update README.md or docs/ for user-facing changes
- Add inline comments for complex logic
- Keep CHANGELOG.md updated for significant changes

## Reporting Issues

### Bug Reports

When reporting a bug, include:
- HDFView version
- Operating system and version
- Java version (`java -version`)
- HDF4/HDF5 library versions
- Steps to reproduce
- Expected vs actual behavior
- Sample HDF file if relevant (and not too large)

### Feature Requests

When requesting a feature, describe:
- Use case and motivation
- Proposed solution or behavior
- Any alternatives considered
- Willingness to contribute implementation

### Security Issues

**Do not** report security vulnerabilities in public issues. Contact The HDF Group security team directly at security@hdfgroup.org.

## Development Phases

The project follows a phased modernization approach:

- **Phase 1**: Maven migration (✅ Complete)
- **Phase 2A**: JUnit 5 migration (✅ Complete)
- **Phase 2B**: CI/CD pipeline (✅ Complete)
- **Phase 2C**: Quality analysis (✅ Complete)
- **Phase 2D**: UI framework research (Deferred)

## Additional Resources

- **Build Guide**: `docs/Build_HDFView.txt`
- **Testing Guide**: `docs/Testing-Guide.md`
- **CI/CD Documentation**: `docs/guides/CI-CD-Pipeline-Guide.md`
- **Cross-Platform Builds**: `docs/guides/Cross-Platform-Build-Quick-Reference.md`
- **Troubleshooting**: `docs/guides/CI-CD-Troubleshooting.md`

## Getting Help

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For questions and community discussion
- **HDF Forum**: https://forum.hdfgroup.org/ for general HDF-related questions

## License

By contributing to HDFView, you agree that your contributions will be licensed under the project's existing license.

---

Thank you for contributing to HDFView!
