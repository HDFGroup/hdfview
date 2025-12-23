# HDFView Version Management

This document describes HDFView's centralized version management system using the VERSION file as the single source of truth.

## Overview

HDFView uses a unified version management approach where the `VERSION` file at the project root is the single source of truth for all version information. This version is automatically propagated to:
- Maven project versions
- Runtime version display
- jpackage installer versions
- GitHub workflow tags and artifacts

## VERSION File Format

The `VERSION` file is located at the project root and contains:

```properties
# HDFView version number (MAJOR.MINOR.RELEASE format)
HDFVIEW_VERSION=99.99.99

# Tag prefix for GitHub releases (do not change)
HDFVIEW_TAG_PREFIX=HDFView

# Full version tag (constructed as: ${HDFVIEW_TAG_PREFIX}-${HDFVIEW_VERSION})
VERSION=HDFView-99.99.99
```

### Properties

- **HDFVIEW_VERSION**: Numeric version in MAJOR.MINOR.RELEASE format
- **HDFVIEW_TAG_PREFIX**: Prefix used for GitHub release tags (always "HDFView")
- **VERSION**: Full version tag for backwards compatibility

## Versioning Scheme

HDFView uses a three-tier versioning scheme:

| Branch Type | Version Format | Example | Purpose |
|------------|----------------|---------|---------|
| **master** | 99.99.99 | 99.99.99 | Development branch, always 99.99.99 |
| **release branch** | MAJOR.MINOR.99 | 3.5.99 | Release candidate branch |
| **release tag** | MAJOR.MINOR.RELEASE | 3.5.0 | Official release |

### Version Update Workflow

1. **Development (master)**:
   ```
   VERSION file: HDFVIEW_VERSION=99.99.99
   Maven version: 99.99.99-SNAPSHOT
   ```

2. **Creating Release Branch**:
   ```bash
   # Update VERSION file
   HDFVIEW_VERSION=3.5.99

   # Update pom.xml versions
   <version>3.5.99-SNAPSHOT</version>
   ```

3. **Creating Release**:
   ```bash
   # Update VERSION file
   HDFVIEW_VERSION=3.5.0

   # Update pom.xml versions (remove -SNAPSHOT)
   <version>3.5.0</version>

   # Tag release
   git tag HDFView-3.5.0
   ```

## How Versions are Propagated

### 1. Maven Build System

The root `pom.xml` uses the **properties-maven-plugin** to read the VERSION file during the `initialize` phase:

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>properties-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>read-version-file</id>
      <phase>initialize</phase>
      <goals>
        <goal>read-project-properties</goal>
      </goals>
      <configuration>
        <files>
          <file>VERSION</file>
        </files>
      </configuration>
    </execution>
  </executions>
</plugin>
```

This makes `${HDFVIEW_VERSION}` available throughout the Maven build.

### 2. Runtime Version Display

The version displayed in the HDFView application is populated via Maven resource filtering:

**Source** (`hdfview/src/main/resources/versions.properties`):
```properties
HDFVIEW_VERSION=${HDFVIEW_VERSION}
HDF5_VERSION=${hdf5.version}
HDF4_VERSION=${hdf.version}
JAVA_VERSION=${maven.compiler.release}
```

**Filtered Output** (`hdfview/target/classes/versions.properties`):
```properties
HDFVIEW_VERSION=99.99.99
HDF5_VERSION=2.0.0
HDF4_VERSION=4.3.1
JAVA_VERSION=21
```

The Java class `hdf.HDFVersions` reads this file at runtime.

### 3. jpackage Installers

All jpackage profiles use the `${HDFVIEW_VERSION}` property:

```xml
<profile>
  <id>jpackage-app-image</id>
  <properties>
    <jpackage.app.version>${HDFVIEW_VERSION}</jpackage.app.version>
  </properties>
</profile>
```

### 4. GitHub Workflows

Workflows extract the version from VERSION file using shell commands:

**Unix/Linux/macOS**:
```bash
- name: Extract version from VERSION file
  id: get-version
  run: |
    HDFVIEW_VERSION=$(grep "^HDFVIEW_VERSION=" VERSION | cut -d'=' -f2)
    echo "HDFVIEW_VERSION=$HDFVIEW_VERSION" >> $GITHUB_OUTPUT
```

**Windows (PowerShell)**:
```powershell
- name: Extract version from VERSION file
  id: get-version
  shell: pwsh
  run: |
    $version = (Get-Content VERSION | Select-String "^HDFVIEW_VERSION=").ToString().Split('=')[1]
    echo "HDFVIEW_VERSION=$version" >> $env:GITHUB_OUTPUT
```

Then use it in subsequent steps:
```yaml
jpackage --app-version ${{ steps.get-version.outputs.HDFVIEW_VERSION }}
```

## Manual Version Updates

When updating to a new version, update these files **manually**:

### Required Changes

1. **VERSION file** - Update `HDFVIEW_VERSION` and `VERSION`:
   ```properties
   HDFVIEW_VERSION=3.5.0
   VERSION=HDFView-3.5.0
   ```

2. **All pom.xml files** - Update `<version>` tags:
   ```xml
   <!-- Root pom.xml -->
   <version>3.5.0-SNAPSHOT</version>  <!-- or 3.5.0 for release -->

   <!-- repository/pom.xml, object/pom.xml, hdfview/pom.xml parent versions -->
   <parent>
     <version>3.5.0-SNAPSHOT</version>
   </parent>

   <!-- hdfview/pom.xml object dependency -->
   <dependency>
     <artifactId>object</artifactId>
     <version>3.5.0-SNAPSHOT</version>
   </dependency>
   ```

### Files That Auto-Update

These files automatically pick up the version from VERSION file:
- `hdfview/target/classes/versions.properties` (generated during build)
- jpackage installers (DMG, MSI, DEB, RPM)
- GitHub workflow artifacts

## Testing Version Propagation

### Local Build Test

```bash
# 1. Clean build
mvn clean install -DskipTests -Ddependency-check.skip=true

# 2. Verify versions.properties
cat hdfview/target/classes/versions.properties

# Expected output:
# HDFVIEW_VERSION=99.99.99
# HDF5_VERSION=2.0.0
# HDF4_VERSION=4.3.1
# JAVA_VERSION=21

# 3. Test jpackage
mvn verify -Pjpackage-app-image -pl object,hdfview -DskipTests
ls hdfview/target/dist/
```

### Workflow Test

Push changes to a branch and check GitHub Actions:
1. Verify version extraction in workflow logs
2. Check artifact names: `HDFView-{version}-{platform}`
3. Inspect generated installers

## Troubleshooting

### Maven doesn't pick up VERSION file

**Symptom**: Build uses old version or default 99.99.99

**Solution**: Ensure properties-maven-plugin execution runs in initialize phase before other plugins

### versions.properties not filtered

**Symptom**: versions.properties contains `${HDFVIEW_VERSION}` instead of actual version

**Solution**: Check that resource filtering is enabled in hdfview/pom.xml:
```xml
<resources>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>true</filtering>  <!-- Must be true -->
  </resource>
</resources>
```

### Workflow uses wrong version

**Symptom**: GitHub Actions creates artifacts with incorrect version

**Solution**: Verify version extraction step runs before jpackage/signing steps

## Future Enhancements

Potential improvements to the version management system:

1. **Automated pom.xml version sync**: Script to automatically update pom.xml versions from VERSION file
2. **Version validation**: Pre-commit hook to ensure VERSION file and pom.xml versions match
3. **Release automation**: GitHub Action to automatically update versions when creating release branches/tags

## See Also

- `VERSION` - Single source of truth for version information
- `pom.xml` - Maven project configuration
- `hdfview/src/main/resources/versions.properties` - Runtime version template
- `.github/workflows/maven-build.yml` - Installer build and signing workflow
- `.github/workflows/release.yml` - Release workflow (already uses VERSION file)
