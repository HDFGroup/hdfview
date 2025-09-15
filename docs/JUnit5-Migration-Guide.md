# JUnit 5 Migration Guide for HDFView

This document provides a comprehensive guide for migrating HDFView tests from JUnit 4 to JUnit 5.

## Migration Status

### ✅ Completed Infrastructure
- **JUnit 5 Dependencies**: Added to parent POM with version management
- **Surefire Plugin**: Configured for JUnit 5 with test categorization
- **Test Foundation Classes**: Created modern base classes and utilities
- **Sample Migration**: DatasetTest successfully migrated to JUnit 5

### 🔄 In Progress
- **Systematic Test Migration**: Individual test class migrations
- **Documentation**: Migration patterns and best practices

## Infrastructure Overview

### Maven Dependencies (Parent POM)
```xml
<!-- JUnit 5 version property -->
<junit.version>5.10.0</junit.version>
<junit4.version>4.13.2</junit4.version>

<!-- Dependency Management -->
<dependencyManagement>
    <dependencies>
        <!-- JUnit 5 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>${junit.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-params</artifactId>
            <version>${junit.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>${junit.version}</version>
        </dependency>
        <!-- JUnit 4 compatibility during transition -->
        <dependency>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
            <version>${junit.version}</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit4.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Surefire Plugin Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
    <configuration>
        <argLine>
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.time=ALL-UNNAMED
            --add-opens java.base/java.time.format=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --enable-native-access=jarhdf5
            -Djava.library.path=${platform.hdf.lib}
        </argLine>
        <!-- Test categorization and parallel execution -->
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
        <groups>unit</groups>
        <excludedGroups>ui</excludedGroups>
    </configuration>
</plugin>
```

## Migration Process

### Step 1: Import Statement Migration
**From JUnit 4:**
```java
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
```

**To JUnit 5:**
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
```

### Step 2: Annotation Migration
| JUnit 4 | JUnit 5 |
|---------|---------|
| `@BeforeClass` | `@BeforeAll` |
| `@AfterClass` | `@AfterAll` |
| `@Before` | `@BeforeEach` |
| `@After` | `@AfterEach` |
| `@Test` | `@Test` (same) |

### Step 3: Add Test Tags
```java
@Tag("unit")
@Tag("fast")
public class DatasetTest {
    // test methods
}
```

### Step 4: Use Modern Test Foundation Classes

#### BaseObjectTest
```java
public class MyTest extends BaseObjectTest {
    @BeforeEach
    void setup() {
        super.baseSetUp(testInfo);
        // additional setup
    }

    @AfterEach
    void cleanup() {
        super.baseTearDown();
        // additional cleanup
    }
}
```

#### HDF5Assertions
```java
import static test.HDF5Assertions.*;

@Test
void testDatasetExists() {
    assertDatasetExists(testFile, "/my/dataset");
    assertDatasetData(dataset, expectedData);
    assertFileStructure(testFile, "/path1", "/path2");
}
```

## Modern JUnit 5 Features

### Parameterized Tests
```java
@ParameterizedTest
@MethodSource("test.ParameterizedTestSupport#standardDatasetArguments")
void testDatasetTypes(String datasetName, String expectedType) {
    // Test implementation
}

@ParameterizedTest
@ValueSource(strings = {"dataset1", "dataset2", "dataset3"})
void testMultipleDatasets(String datasetName) {
    // Test implementation
}
```

### Nested Test Organization
```java
@Nested
@DisplayName("Dataset Name Tests")
class DatasetNameTests {
    @Test
    @DisplayName("Dataset getName() returns correct names")
    void testGetName() {
        // Test implementation
    }
}
```

### Dynamic Tests
```java
@TestFactory
Stream<DynamicTest> testVariousDatasetSizes() {
    return Stream.of(100, 1000, 10000)
        .map(size -> DynamicTest.dynamicTest(
            "Dataset size " + size,
            () -> testDatasetSize(size)
        ));
}
```

## Test Execution Modes

### Unit Tests (Fast, Parallel)
```bash
mvn test -Dgroups="unit & fast"
```

### Integration Tests (Serial)
```bash
mvn test -Dgroups="integration"
```

### UI Tests (Serial, Headless=false)
```bash
mvn test -Dgroups="ui" -Djava.awt.headless=false
```

## Migration Checklist

For each test class:

- [ ] Update import statements (JUnit 4 → JUnit 5)
- [ ] Update annotations (@BeforeClass → @BeforeAll, etc.)
- [ ] Add @Tag annotations for test categorization
- [ ] Update assertion methods (if using different patterns)
- [ ] Consider extending BaseObjectTest for common patterns
- [ ] Use HDF5Assertions for domain-specific assertions
- [ ] Add @DisplayName annotations for better test reporting
- [ ] Consider @Nested organization for complex test classes
- [ ] Add parameterized tests where beneficial
- [ ] Test compilation and execution

## Common Migration Patterns

### Simple Test Class Migration
```java
// Before (JUnit 4)
@BeforeClass
public static void setup() { }

@Before
public void init() { }

@Test
public void testSomething() {
    assertEquals(expected, actual);
}

@After
public void cleanup() { }

@AfterClass
public static void teardown() { }
```

```java
// After (JUnit 5)
@Tag("unit")
@Tag("fast")
class MyTest {
    @BeforeAll
    static void setup() { }

    @BeforeEach
    void init() { }

    @Test
    @DisplayName("Test something meaningful")
    void testSomething() {
        assertEquals(expected, actual);
    }

    @AfterEach
    void cleanup() { }

    @AfterAll
    static void teardown() { }
}
```

## Benefits of JUnit 5 Migration

1. **Modern Testing Features**: Parameterized tests, dynamic tests, nested organization
2. **Better Test Organization**: Tags, display names, nested test classes
3. **Improved Performance**: Parallel execution capabilities
4. **Enhanced Assertions**: More descriptive assertion methods
5. **Better IDE Integration**: Improved test discovery and reporting
6. **Future-Proof**: Active development and modern Java support

## Troubleshooting

### Common Issues
1. **Dependency Resolution**: Ensure parent POM dependency management is correct
2. **Module System**: Test module-info.java disabled temporarily for classpath mode
3. **Native Library Access**: Surefire configured with proper JVM arguments
4. **Parallel Execution**: UI tests must run serially, unit tests can be parallel

### Environment Requirements
- Java 21+
- Maven 3.6+
- HDF5 native libraries configured
- build.properties properly set

## Next Steps

1. **Complete Remaining Migrations**: Systematically migrate remaining 40+ test classes
2. **Remove JUnit 4 Dependencies**: Once all tests migrated
3. **Enhanced Testing**: Add more parameterized and dynamic tests
4. **CI/CD Integration**: Update workflows for JUnit 5 features
5. **Performance Optimization**: Leverage parallel execution capabilities

This migration establishes a modern, maintainable testing foundation for HDFView development.