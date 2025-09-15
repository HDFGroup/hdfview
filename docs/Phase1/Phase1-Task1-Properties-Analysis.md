# Build Properties Analysis for Maven Migration

## Summary
Comprehensive analysis of all properties used by the Ant build system and their required Maven equivalents.

## Properties from build.properties (Active)

### Native Library Configuration (CRITICAL for Maven)
| Property | Current Value | Maven Equivalent | Notes |
|----------|---------------|------------------|-------|
| `hdf.lib.dir` | `/home/byrn/HDF_Projects/temp/HDF_Group/HDF/4.3.1-1/lib` | Maven property | **REQUIRED** - HDF4 library path |
| `hdf5.lib.dir` | `/home/byrn/HDF_Projects/temp/HDF_Group/HDF5/2.0.0/lib` | Maven property | **REQUIRED** - HDF5 library path |
| `hdf5.plugin.dir` | `/home/byrn/HDF_Projects/temp/HDF_Group/HDF5/2.0.0/lib/plugin` | Maven property | **REQUIRED** - HDF5 plugins |
| `platform.hdf.lib` | `${env.LD_LIBRARY_PATH}` | Maven property | **CRITICAL** - Runtime library path |

### Build Configuration
| Property | Current Value | Maven Equivalent | Notes |
|----------|---------------|------------------|-------|
| `build.debug` | `true` | `maven.compiler.debug` | Debug symbols in compilation |
| `build.antoutput.append` | `false` | N/A | Ant-specific logging |
| `build.log.level` | `info` | `maven.compiler.verbose` | Build logging level |
| `build.log.level.run` | `trace` | JVM arguments | Runtime logging |
| `build.log.level.test` | (empty) | Surefire plugin config | Test logging |
| `build.object.test` | (empty) | Surefire plugin config | Object test configuration |
| `build.jacoco` | (commented) | Jacoco plugin activation | Code coverage toggle |

### Platform-Specific Configuration
| Property | Current Value | Maven Equivalent | Notes |
|----------|---------------|------------------|-------|
| `install.mac.jre` | `${env.OSX_JRE_HOME}` | Profile property | macOS JRE location |
| `jre.dir.name` | `${env.OSX_JRE_NAME}` | Profile property | macOS JRE directory name |
| `userguide.dir` | `/home/byrn/HDF_Projects/temp/UsersGuide` | Maven property | Documentation path |
| `wix.dir` | `${env.WIX}/bin` | Profile property | Windows installer tools |

## Properties Defined in build.xml (75+ Dynamic Properties)

### Version Information (High Priority for Maven)
| Property | Source | Maven Equivalent | Migration Strategy |
|----------|--------|------------------|-------------------|
| `app.version` | VERSION file parsing | `project.version` | Properties plugin + file reading |
| `hdf4.version` | libhdf4.settings parsing | Custom property | Exec plugin with script |
| `hdf5.version` | libhdf5.settings parsing | Custom property | Exec plugin with script |

### Directory Structure (Maven Standard Layout)
| Property | Ant Value | Maven Standard | Migration Action |
|----------|-----------|----------------|------------------|
| `src.dir` | `src` | `src/main/java` | Update references |
| `classes.dir` | `build/classes` | `target/classes` | Maven standard |
| `jar.dir` | `build/jar` | `target` | Maven standard |
| `dist.dir` | `build/dist` | `target/assembly` | Assembly plugin |
| `release.dir` | `build/release` | `target/release` | Custom packaging |
| `lib.dir` | `lib` | `target/lib` | Dependency plugin |

### Module Structure
| Property | Ant Value | Maven Module | Notes |
|----------|-----------|--------------|-------|
| `hdfview.dir` | `org.hdfgroup.hdfview` | hdfview module | Multi-module structure |
| `object.dir` | `org.hdfgroup.object` | object module | Multi-module structure |
| `h4.dir` | `org.hdfgroup.object` | object module | HDF4 classes |
| `h5.dir` | `org.hdfgroup.object` | object module | HDF5 classes |
| `fits.dir` | `org.hdfgroup.object` | object module | FITS classes |
| `nc2.dir` | `org.hdfgroup.object` | object module | NetCDF classes |

### Platform Detection Logic (Critical for Maven Profiles)
| Property | Detection Logic | Maven Profile Activation |
|----------|-----------------|--------------------------|
| `isWindows` | `<os family="windows" />` | `<os><family>windows</family></os>` |
| `isUnix` | `<os family="unix" /> AND NOT <os family="mac" />` | `<os><family>unix</family><name>!Mac OS X</name></os>` |
| `isMac` | `<os family="mac" />` | `<os><family>mac</family></os>` |
| `machine.arch` | `${os.arch}` | `<os><arch>${os.arch}</arch></os>` |
| `uname.os` | OS name detection | `${os.name}` |

## Environment Variables Required by Ant Build

### Code Signing (macOS)
| Variable | Usage | Maven Equivalent |
|----------|-------|------------------|
| `BINSIGN` | Enable signing | Profile activation property |
| `SIGNER` | Signing account | Maven property |
| `KEYCHAIN_PASSWD` | Keychain password | Maven property (secure) |
| `KEYCHAIN_NAME` | Keychain name | Maven property |
| `NOTARY_USER` | Notarization user | Maven property |
| `NOTARY_KEY` | Notarization key | Maven property (secure) |

### Platform Tools
| Variable | Usage | Maven Equivalent |
|----------|-------|------------------|
| `WIX` | Windows installer tools | Profile property |
| `SIGNTOOLDIR` | Windows signing tools | Profile property |
| `OSX_JRE_HOME` | macOS JRE location | Profile property |
| `OSX_JRE_NAME` | macOS JRE name | Profile property |

## Critical Property Dependencies for Maven Migration

### Phase 1 Priority 1 (Must Have)
1. **Native library paths** - `hdf.lib.dir`, `hdf5.lib.dir`, `hdf5.plugin.dir`
2. **Platform runtime path** - `platform.hdf.lib`
3. **Build configuration** - `build.debug`
4. **Version information** - `app.version`, `hdf4.version`, `hdf5.version`

### Phase 1 Priority 2 (Important)
1. **Module directory structure** - All `*.dir` properties
2. **Platform detection** - `isWindows`, `isUnix`, `isMac`
3. **Test configuration** - `build.log.level.test`, `build.object.test`
4. **Jacoco integration** - `build.jacoco`

### Phase 1 Priority 3 (Nice to Have)
1. **Documentation paths** - `userguide.dir`
2. **Assembly configuration** - Distribution and packaging properties
3. **Development utilities** - Debug and logging properties

## Maven Property Migration Strategy

### 1. Direct Property Translation
Properties that map directly to Maven equivalents:
```xml
<properties>
    <hdf.lib.dir>${env.HDF4_HOME}/lib</hdf.lib.dir>
    <hdf5.lib.dir>${env.HDF5_HOME}/lib</hdf5.lib.dir>
    <hdf5.plugin.dir>${env.HDF5_HOME}/lib/plugin</hdf5.plugin.dir>
    <platform.hdf.lib>${env.LD_LIBRARY_PATH}</platform.hdf.lib>
    <maven.compiler.debug>true</maven.compiler.debug>
</properties>
```

### 2. Complex Property Processing
Properties requiring custom Maven plugin execution:

**Version Extraction:**
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>extract-hdf5-version</id>
            <phase>initialize</phase>
            <goals><goal>exec</goal></goals>
            <configuration>
                <executable>sh</executable>
                <arguments>
                    <argument>-c</argument>
                    <argument>grep "HDF5 Version:" "${hdf5.lib.dir}/libhdf5.settings" | sed 's/.*: *\([0-9.]*\).*/\1/' > ${project.build.directory}/hdf5.version</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3. Profile-Based Property Management
Platform-specific properties:
```xml
<profiles>
    <profile>
        <id>windows</id>
        <activation><os><family>windows</family></os></activation>
        <properties>
            <platform.hdf.lib>${env.PATH}</platform.hdf.lib>
            <native.lib.ext>.dll</native.lib.ext>
        </properties>
    </profile>
    <profile>
        <id>unix</id>
        <activation>
            <os><family>unix</family><name>!Mac OS X</name></os>
        </activation>
        <properties>
            <platform.hdf.lib>${env.LD_LIBRARY_PATH}</platform.hdf.lib>
            <native.lib.ext>.so</native.lib.ext>
        </properties>
    </profile>
</profiles>
```

## Missing Maven Properties Integration

### Current Gap: No Property Loading
- Ant: `<property file="build.properties" />`
- Maven: Requires `properties-maven-plugin` for external property file loading

### Solution: Maven Properties Plugin
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>properties-maven-plugin</artifactId>
    <version>1.2.1</version>
    <executions>
        <execution>
            <phase>initialize</phase>
            <goals><goal>read-project-properties</goal></goals>
            <configuration>
                <files><file>build.properties</file></files>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Risk Assessment

### High Risk Property Migrations
1. **Native library version parsing** - Complex regex-based extraction
2. **Platform-specific path handling** - Different path separators and environment variables
3. **Dynamic property generation** - Properties computed from other properties

### Medium Risk
1. **Directory structure changes** - Maven standard layout vs Ant custom paths
2. **Multi-module property inheritance** - Property scoping across modules
3. **Environment variable dependencies** - Build assumes specific environment setup

### Low Risk
1. **Simple property substitution** - Direct value mappings
2. **Boolean configuration flags** - Simple true/false properties
3. **Static path configurations** - Fixed directory references

This analysis provides the foundation for implementing Maven property management in Task 1.2.