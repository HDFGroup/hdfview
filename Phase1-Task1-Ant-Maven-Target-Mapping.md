# Ant to Maven Target Mapping Analysis

## Summary
Analyzed **75+ Ant targets** in build.xml and mapped them to Maven equivalents for complete migration planning.

## Core Build Targets

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| `clean` | `mvn clean` | maven-clean-plugin:clean | ✅ Exists | Basic cleanup |
| `compile` | `mvn compile` | maven-compiler-plugin:compile | ✅ Exists | But needs SWT fix |
| `objectjar` | `mvn package` (object module) | maven-jar-plugin:jar | ✅ Exists | Multi-module |
| `jar` | `mvn package` (hdfview module) | maven-jar-plugin:jar | ✅ Exists | Main application |
| `modulejar` | `mvn package` | maven-jar-plugin:jar | 🔧 Needs config | Test modules |

## Testing Infrastructure

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| `junit` | `mvn test` | maven-surefire-plugin:test | 🔧 Needs config | Object tests |
| `junit-uitest` | `mvn integration-test` | maven-failsafe-plugin:integration-test | ❌ Missing | SWTBot UI tests |
| `junit-uimodules` | `mvn test` | maven-surefire-plugin:test | 🔧 Needs config | Module tests |
| `jacoco` | `mvn verify` | jacoco-maven-plugin | ❌ Missing | Code coverage |
| `sonar` | `mvn sonar:sonar` | sonar-maven-plugin | 🔧 Partial | Basic setup exists |

## Documentation Generation

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| `javadoc` | `mvn javadoc:javadoc` | maven-javadoc-plugin | ❌ Missing | API documentation |
| `createDocumentationTGZ` | `mvn assembly:single` | maven-assembly-plugin | ❌ Missing | User guide packaging |

## Version and Property Extraction

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| VERSION file reading | Properties plugin | properties-maven-plugin | 🔧 Needs config | App version |
| HDF4/HDF5 version extraction | Exec plugin | exec-maven-plugin | ❌ Missing | Parse .settings files |
| Platform detection | Maven profiles | Profile activation | 🔧 Partial | OS/arch detection |

## Application Execution

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| `run` | `mvn exec:java` | exec-maven-plugin | ❌ Missing | Run application |
| `rundebug` | `mvn exec:java` | exec-maven-plugin | ❌ Missing | Debug mode |
| `run-jar` | `java -jar` | N/A | ✅ Manual | Direct JAR execution |
| `run-examples` | `mvn exec:java` | exec-maven-plugin | ❌ Missing | Example execution |

## Packaging and Distribution

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| `deploy` | `mvn package` + assembly | maven-assembly-plugin | ❌ Missing | Platform-specific |
| `createJPackage*` | JPackage plugin | jpackage-maven-plugin | ❌ Missing | Native installers |
| `binaryPack*` | `mvn package` | maven-assembly-plugin | ❌ Missing | Distribution packaging |
| `packageSource` | `mvn source:jar` | maven-source-plugin | ❌ Missing | Source distribution |

## Platform-Specific Targets

| Ant Target Pattern | Maven Equivalent | Plugin/Goal | Status | Notes |
|--------------------|------------------|-------------|---------|--------|
| `deployWindows` | Windows profile | Profile activation | ❌ Missing | Windows deployment |
| `deployUnix` | Unix profile | Profile activation | ❌ Missing | Linux deployment |
| `deployMac` | Mac profile | Profile activation | ❌ Missing | macOS deployment |
| `createJPackageWindows` | Windows profile + JPackage | jpackage-maven-plugin | ❌ Missing | Windows installer |
| `createJPackageUnix` | Unix profile + JPackage | jpackage-maven-plugin | ❌ Missing | Linux packages (deb/rpm) |
| `createJPackageMac` | Mac profile + JPackage | jpackage-maven-plugin | ❌ Missing | macOS app bundle |

## Code Signing and Notarization (macOS)

| Ant Target | Maven Equivalent | Plugin/Goal | Status | Notes |
|------------|------------------|-------------|---------|--------|
| `*SignMac` targets | Maven antrun plugin | maven-antrun-plugin | ❌ Missing | Code signing scripts |
| `*notary*` targets | Maven antrun plugin | maven-antrun-plugin | ❌ Missing | Apple notarization |
| Keychain access | Environment variables | Properties | 🔧 Partial | Key management |

## Critical Missing Maven Plugins Required

### High Priority (Phase 1)
1. **jacoco-maven-plugin** - Code coverage analysis
2. **exec-maven-plugin** - Application execution and version extraction
3. **maven-javadoc-plugin** - Documentation generation
4. **properties-maven-plugin** - Property loading and processing

### Medium Priority (Phase 1)
5. **jpackage-maven-plugin** - Native installer creation
6. **maven-assembly-plugin** - Distribution packaging
7. **maven-failsafe-plugin** - Integration testing (UI tests)
8. **maven-source-plugin** - Source packaging

### Low Priority (Phase 2)
9. **maven-antrun-plugin** - Code signing scripts (temporary)
10. **build-helper-maven-plugin** - Property manipulation

## Complex Ant Logic Requiring Custom Maven Solutions

### 1. Native Library Version Extraction
**Current Ant logic:**
```xml
<loadfile property="hdf5.version" srcFile="${hdf5.lib.dir}/libhdf5.settings">
    <filterchain>
        <tokenfilter>
            <linetokenizer />
            <containsregex pattern="HDF5 Version:" />
        </tokenfilter>
        <tokenfilter>
            <stringtokenizer delims=":- \r\n" suppressdelims="true" />
            <containsregex pattern="[0-9]+\.[0-9]+\.[0-9]+" />
        </tokenfilter>
    </filterchain>
</loadfile>
```

**Proposed Maven solution:** Custom exec-maven-plugin execution with shell script or Java main class

### 2. Platform-Specific Build Logic
**Current Ant logic:**
```xml
<condition property="isWindows">
    <os family="windows" />
</condition>
<condition property="isUnix">
    <and>
        <os family="unix" />
        <not><os family="mac" /></not>
    </and>
</condition>
```

**Proposed Maven solution:** Profile activation with OS family detection

### 3. Conditional Compilation Based on Library Availability
**Current Ant logic:**
```xml
<condition property="h4path" value="${hdf.lib.dir}">
    <available file="${hdf4.settings.version}" />
</condition>
<target name="compilehdf4" depends="clean" if="h4path">
```

**Proposed Maven solution:** Maven profiles with file existence activation

## Risk Assessment

### High Risk Migration Items
1. **JPackage integration** - Complex multi-platform installer creation
2. **Code signing workflows** - macOS signing and notarization
3. **Native library version parsing** - Custom logic not easily replicated in Maven
4. **UI test execution** - SWTBot configuration with Maven

### Medium Risk Migration Items
1. **Platform-specific assembly** - Different packaging per OS
2. **Property inheritance** - Complex property resolution chains
3. **Multi-module test execution** - Cross-module test dependencies

### Low Risk Migration Items
1. **Basic compilation** - Standard Maven functionality
2. **JAR packaging** - Standard Maven functionality
3. **Dependency management** - Maven Central migration

## Migration Strategy Summary

**Total Ant Targets:** 75+
**Direct Maven Equivalents:** 15 (20%)
**Requires New Plugins:** 45 (60%)
**Custom Solutions Needed:** 15 (20%)

**Plugin Priority Installation Order:**
1. jacoco-maven-plugin (testing foundation)
2. exec-maven-plugin (execution and version extraction)
3. maven-javadoc-plugin (documentation)
4. jpackage-maven-plugin (distribution)
5. maven-assembly-plugin (packaging)
6. Additional plugins as needed

This mapping provides the foundation for Task 1.2 (Migrate Missing Build Tasks).