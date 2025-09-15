# Phase 2A: JUnit 5 Migration - Detailed Implementation Plan

**Duration**: 3-4 weeks
**Status**: Ready to begin
**Dependencies**: Phase 1 complete (✅)

## Overview

Migrate all 98 test files (66 in `/test` directory + 32 in Maven modules) from JUnit 4 to JUnit 5, implementing modern testing practices and improving test organization and performance.

## Current Test Inventory

### Test Distribution
- **UI Tests**: ~40 tests using SWTBot (in `org.hdfgroup.hdfview.test/uitest/`)
- **Object Model Tests**: ~20 tests (in `org.hdfgroup.object.test/`)
- **Module Tests**: ~32 tests (in `object/src/test/` and `hdfview/src/test/`)
- **Integration Tests**: ~6 tests (mixed file I/O and native library tests)

### Current Framework Usage
- **JUnit 4**: All tests using `@Test`, `@Before`, `@After`, `@BeforeClass`, `@AfterClass`
- **SWTBot**: UI testing framework (compatible with JUnit 5)
- **JUnit Assert**: Static imports from `org.junit.Assert`
- **Test Data**: HDF files in `/test` directory and `object/TestHDF5.h5`

## Detailed Task Implementation

### Task 2.1: Set Up JUnit 5 Infrastructure (2 days)

#### Day 1: Maven Configuration
**Objective**: Configure JUnit 5 dependencies and Surefire plugin

**Actions**:
1. **Update Parent POM Dependencies**:
   ```xml
   <properties>
       <junit.version>5.10.0</junit.version>
       <junit4.version>4.13.2</junit4.version>
       <swtbot.version>4.1.0</swtbot.version>
   </properties>

   <dependencyManagement>
       <dependencies>
           <!-- JUnit 5 -->
           <dependency>
               <groupId>org.junit.jupiter</groupId>
               <artifactId>junit-jupiter-engine</artifactId>
               <version>${junit.version}</version>
               <scope>test</scope>
           </dependency>
           <dependency>
               <groupId>org.junit.jupiter</groupId>
               <artifactId>junit-jupiter-params</artifactId>
               <version>${junit.version}</version>
               <scope>test</scope>
           </dependency>
           <!-- JUnit 4 compatibility during transition -->
           <dependency>
               <groupId>org.junit.vintage</groupId>
               <artifactId>junit-vintage-engine</artifactId>
               <version>${junit.version}</version>
               <scope>test</scope>
           </dependency>
           <dependency>
               <groupId>junit</groupId>
               <artifactId>junit</artifactId>
               <version>${junit4.version}</version>
               <scope>test</scope>
           </dependency>
       </dependencies>
   </dependencyManagement>
   ```

2. **Update Maven Surefire Plugin**:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <version>3.2.2</version>
       <configuration>
           <argLine>
               --add-opens java.base/java.lang=ALL-UNNAMED
               --add-opens java.base/java.time=ALL-UNNAMED
               --add-opens java.desktop/java.awt=ALL-UNNAMED
               --enable-native-access=jarhdf5
               -Djava.awt.headless=false
               -Dorg.eclipse.swt.browser.DefaultType=mozilla
           </argLine>
           <systemPropertyVariables>
               <java.library.path>${platform.hdf.lib}</java.library.path>
           </systemPropertyVariables>
           <includes>
               <include>**/*Test.java</include>
               <include>**/Test*.java</include>
           </includes>
           <parallel>methods</parallel>
           <threadCount>4</threadCount>
       </configuration>
   </plugin>
   ```

3. **Update Module Dependencies**: Add JUnit 5 dependencies to `object/pom.xml` and `hdfview/pom.xml`

#### Day 2: Test Infrastructure and Validation
**Objective**: Test JUnit 5 setup and create sample migrations

**Actions**:
1. **Create Test Migration Validation**:
   - Copy `DatasetTest.java` to `DatasetTestJUnit5.java`
   - Migrate to JUnit 5 annotations and assertions
   - Verify both JUnit 4 and 5 tests run simultaneously

2. **Test Parallel Execution**:
   ```bash
   mvn test -Dtest="**/DatasetTest*" -X
   ```

3. **Validate SWTBot Compatibility**:
   - Test SWTBot with JUnit 5 using simple UI test
   - Verify SWT display threading works with JUnit 5 lifecycle

4. **Document JVM Arguments**: Test and document required JVM arguments for module access

**Deliverables**:
- Updated parent POM with JUnit 5 infrastructure
- Working sample JUnit 5 test alongside JUnit 4 tests
- Validated SWTBot + JUnit 5 compatibility
- Documented JVM configuration requirements

### Task 2.2: Audit and Categorize Existing Tests (2 days)

#### Day 1: Comprehensive Test Analysis
**Objective**: Document all tests and categorize by type and complexity

**Actions**:
1. **Create Test Inventory Spreadsheet** (or markdown table):
   ```
   | File | Package | Type | Complexity | SWTBot | Test Count | Migration Priority |
   |------|---------|------|------------|--------|------------|-------------------|
   | DatasetTest.java | object | Unit | Medium | No | 15 | High |
   | TestTreeViewFiles.java | uitest | UI | High | Yes | 8 | Medium |
   ```

2. **Analyze Test Categories**:
   - **Unit Tests**: Pure logic tests, no UI or file I/O
   - **Integration Tests**: File I/O, native library calls
   - **UI Tests**: SWTBot-based GUI testing
   - **Performance Tests**: Large dataset or memory tests

3. **Identify JUnit 5 Migration Benefits**:
   - Tests suitable for `@ParameterizedTest`
   - Tests that would benefit from `@Nested` organization
   - Tests needing `@TempDir` for file management
   - Tests suitable for `@RepeatedTest`

#### Day 2: Migration Strategy and Test Dependencies
**Objective**: Plan migration approach and identify test data dependencies

**Actions**:
1. **Map Test Data Dependencies**:
   - HDF files used across tests
   - Native library initialization requirements
   - SWT display and shell management patterns

2. **Create Migration Priority Matrix**:
   - **High Priority**: Core functionality, simple migration
   - **Medium Priority**: Complex but valuable for JUnit 5 features
   - **Low Priority**: Complex UI tests or legacy patterns

3. **Document SWTBot Patterns**:
   - Common SWTBot initialization patterns
   - Test data setup and cleanup patterns
   - Display and shell management approaches

4. **Identify Shared Test Utilities**: Document helper classes and methods that need modernization

**Deliverables**:
- Complete test inventory with categorization
- Migration priority matrix
- Test data dependency documentation
- SWTBot pattern analysis

### Task 2.3: Create Modern Test Foundation (3 days)

#### Day 1: JUnit 5 Base Classes and Utilities
**Objective**: Create reusable test infrastructure for JUnit 5

**Actions**:
1. **Create Base Test Classes**:
   ```java
   // object/src/test/java/test/BaseObjectTest.java
   @ExtendWith(TestFileExtension.class)
   public abstract class BaseObjectTest {
       protected static final Logger log = LoggerFactory.getLogger(BaseObjectTest.class);

       @TempDir
       protected Path tempDir;

       protected H5File testFile;

       @BeforeEach
       void setupTestFile() {
           // Common HDF5 file setup
       }

       @AfterEach
       void cleanupTestFile() {
           // Ensure file cleanup
       }
   }
   ```

2. **Create SWTBot Test Base Class**:
   ```java
   // test/org.hdfgroup.hdfview.test/uitest/BaseSWTBotTest.java
   @ExtendWith(SWTBotExtension.class)
   public abstract class BaseSWTBotTest {
       protected SWTWorkbenchBot bot;

       @TempDir
       protected Path testDataDir;

       @BeforeEach
       void setupSWTBot() {
           // Initialize SWTBot and HDFView
       }

       @AfterEach
       void cleanupSWTBot() {
           // Close shells and cleanup
       }
   }
   ```

3. **Create Custom JUnit 5 Extensions**:
   - `TestFileExtension`: Manages HDF file lifecycle
   - `SWTBotExtension`: Manages SWT display and bot lifecycle
   - `HDFLibraryExtension`: Ensures native library initialization

#### Day 2: Test Data Management and Assertions
**Objective**: Modernize test data handling with JUnit 5 patterns

**Actions**:
1. **Create Test Data Manager**:
   ```java
   public class TestDataManager {
       public static void copyTestFile(String filename, Path destination) {
           // Copy from resources to temp directory
       }

       public static H5File createTestFile(Path path, String name) {
           // Programmatic test file creation
       }

       public static void generateTestDataset(H5File file, String name, Object data) {
           // Create datasets programmatically
       }
   }
   ```

2. **Create Modern Assertions**:
   ```java
   public class HDF5Assertions {
       public static void assertDatasetExists(H5File file, String name) {
           // Custom HDF5-specific assertions
       }

       public static void assertDatasetData(Dataset dataset, Object expected) {
           // Assert dataset contents match expected
       }

       public static void assertFileStructure(H5File file, String... expectedPaths) {
           // Assert file contains expected structure
       }
   }
   ```

3. **Update Test Resource Management**:
   - Centralize test HDF files in `src/test/resources`
   - Create programmatic data generation utilities
   - Document test file requirements

#### Day 3: Parameterized Test Support and Test Suites
**Objective**: Implement JUnit 5 advanced testing features

**Actions**:
1. **Create Parameterized Test Support**:
   ```java
   @ParameterizedTest
   @ValueSource(strings = {"HDF4", "HDF5", "NetCDF"})
   void testFileFormatSupport(String format) {
       // Test multiple file formats
   }

   @ParameterizedTest
   @EnumSource(DataType.class)
   void testDataTypeHandling(DataType type) {
       // Test all supported data types
   }
   ```

2. **Create Dynamic Test Generators**:
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

3. **Create Test Suites**:
   ```java
   @Suite
   @SelectClasses({
       DatasetTest.class,
       GroupTest.class,
       DatatypeTest.class
   })
   public class ObjectModelTestSuite {}
   ```

**Deliverables**:
- Complete JUnit 5 test foundation classes
- Test data management utilities
- Custom assertions and extensions
- Parameterized and dynamic test examples
- Test suite organization

### Task 2.4: Migrate Core Data Model Tests (4 days)

#### Day 1-2: Object Module Unit Tests (~20 tests)
**Objective**: Migrate all tests in `org.hdfgroup.object.test/`

**Migration Pattern**:
```java
// Before (JUnit 4)
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class DatasetTest {
    @Before
    public void setUp() { }

    @Test
    public void testDatasetCreation() {
        assertNotNull(dataset);
    }
}

// After (JUnit 5)
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

class DatasetTest extends BaseObjectTest {
    @BeforeEach
    void setUp() { }

    @Test
    void testDatasetCreation() {
        assertNotNull(dataset);
    }

    @Nested
    class DataTypeTests {
        @ParameterizedTest
        @EnumSource(DataType.class)
        void testDataType(DataType type) { }
    }
}
```

**Priority Order**:
1. `DatasetTest.java` - Core dataset functionality
2. `GroupTest.java` - Group operations
3. `DatatypeTest.java` - Data type handling
4. `H5CompoundDSTest.java` - Compound datasets
5. `HObjectTest.java` - Base object functionality

#### Day 3-4: Maven Module Tests (~15 tests)
**Objective**: Migrate tests in `object/src/test/` and `hdfview/src/test/`

**Actions**:
1. **Migrate `object/src/test/java/object/` tests**:
   - Focus on tests that can benefit from parameterized testing
   - Implement `@TempDir` for file-based tests
   - Use `@Nested` for logical test grouping

2. **Create Parameterized Tests for Format Support**:
   ```java
   @ParameterizedTest
   @CsvSource({
       "HDF4, .hdf, hdf.object.h4.H4File",
       "HDF5, .h5, hdf.object.h5.H5File",
       "NetCDF, .nc, hdf.object.nc2.NC2File"
   })
   void testFileFormatSupport(String format, String extension, String className) {
       // Test file format specific functionality
   }
   ```

**Deliverables**:
- All object model tests migrated to JUnit 5
- Parameterized tests for data types and formats
- Nested test organization
- Enhanced test data management

### Task 2.5: Migrate UI and Integration Tests (5 days)

#### Day 1-2: SWTBot Test Migration (~25 tests)
**Objective**: Migrate SWTBot UI tests to JUnit 5

**Migration Challenges**:
- SWT display management in JUnit 5 lifecycle
- Test isolation and cleanup
- SWTBot finder timeouts and conditions

**Migration Pattern**:
```java
// JUnit 5 SWTBot Test
@ExtendWith(SWTBotExtension.class)
class TestTreeViewFiles extends BaseSWTBotTest {

    @Test
    void openHDF5ScalarGroup() {
        // Test implementation using modern patterns
    }

    @ParameterizedTest
    @ValueSource(strings = {"tscalarintsize.h5", "tcompound.h5", "tarray.h5"})
    void testOpenVariousHDF5Files(String filename) {
        // Parameterized file opening tests
    }
}
```

#### Day 3: Integration Tests (~15 tests)
**Objective**: Migrate file I/O and native library integration tests

**Actions**:
1. **Update File I/O Tests**:
   - Use `@TempDir` for temporary file creation
   - Implement proper resource cleanup with `@AfterEach`
   - Add parameterized tests for different file sizes

2. **Native Library Tests**:
   - Ensure proper HDF library initialization
   - Test error conditions and exception handling
   - Validate memory cleanup

#### Day 4-5: Test Categories and Execution Modes
**Objective**: Organize tests with tags and execution profiles

**Actions**:
1. **Add Test Categories**:
   ```java
   @Tag("unit")
   @Tag("fast")
   class DatasetTest { }

   @Tag("integration")
   @Tag("slow")
   class FileIOTest { }

   @Tag("ui")
   @Tag("slow")
   class TestTreeViewFiles { }
   ```

2. **Create Maven Test Profiles**:
   ```xml
   <profile>
       <id>unit-tests</id>
       <properties>
           <groups>unit &amp; fast</groups>
       </properties>
   </profile>

   <profile>
       <id>integration-tests</id>
       <properties>
           <groups>integration | ui</groups>
       </properties>
   </profile>
   ```

**Deliverables**:
- All SWTBot tests migrated with proper lifecycle management
- Integration tests using `@TempDir` and modern patterns
- Test categorization with `@Tag` annotations
- Maven profiles for different test execution modes

### Task 2.6: Organize Test Structure and Execution (2 days)

#### Day 1: Test Performance and Parallel Execution
**Objective**: Optimize test execution time and organization

**Actions**:
1. **Configure Parallel Execution**:
   ```xml
   <configuration>
       <parallel>methods</parallel>
       <threadCount>4</threadCount>
       <parallelOptimized>true</parallelOptimized>
       <forkCount>1</forkCount>
       <reuseForks>true</reuseForks>
   </configuration>
   ```

2. **Optimize Test Categories**:
   - **Fast Tests** (<2s): Unit tests, no file I/O
   - **Medium Tests** (<30s): Integration tests, small files
   - **Slow Tests** (any): UI tests, large files

3. **Test Data Optimization**:
   - Share test files across tests where possible
   - Use programmatic data generation for simple cases
   - Cache expensive setup operations

#### Day 2: Documentation and IDE Integration
**Objective**: Document test execution and configure IDE support

**Actions**:
1. **Create Test Execution Documentation**:
   ```markdown
   # Test Execution Guide

   ## Run All Tests
   mvn test

   ## Run Only Fast Unit Tests
   mvn test -Dgroups="unit & fast"

   ## Run Integration Tests
   mvn test -Dgroups="integration"

   ## Run UI Tests
   mvn test -Dgroups="ui" -Djava.awt.headless=false
   ```

2. **Configure IDE Support**:
   - Eclipse: Create `.project` settings for JUnit 5
   - IntelliJ: Document JUnit 5 configuration
   - VS Code: Create test runner configuration

3. **Integrate with JaCoCo**:
   - Ensure code coverage works with JUnit 5
   - Configure coverage exclusions for test utilities
   - Set up coverage trend monitoring

**Deliverables**:
- Optimized test execution configuration
- Test performance baseline and monitoring
- Complete test execution documentation
- IDE configuration guides

### Task 2.7: Remove JUnit 4 Dependencies (1 day)

#### Day 1: Final Migration and Cleanup
**Objective**: Complete migration and remove JUnit 4 dependencies

**Actions**:
1. **Validate All Tests Migrated**:
   ```bash
   grep -r "import org.junit.Test" . --include="*.java"
   grep -r "import org.junit.Assert" . --include="*.java"
   ```

2. **Remove JUnit 4 Dependencies**:
   - Remove `junit-vintage-engine` from POMs
   - Remove JUnit 4 dependency declarations
   - Update CI/CD configurations

3. **Final Validation**:
   ```bash
   mvn clean test
   mvn test -Dgroups="unit"
   mvn test -Dgroups="integration"
   mvn test -Dgroups="ui" -Djava.awt.headless=false
   ```

4. **Update Documentation**:
   - Update `CLAUDE.md` with JUnit 5 information
   - Document new test execution patterns
   - Update contributor guidelines

**Deliverables**:
- Complete JUnit 5 migration with no JUnit 4 dependencies
- All tests passing in various execution modes
- Updated documentation and contributor guides

## Success Metrics and Acceptance Criteria

### Quantitative Goals
- **Migration Completeness**: 100% of tests (98 files) migrated to JUnit 5
- **Performance Improvement**: 20% reduction in test execution time
- **Test Organization**: Clear separation of unit (<2s), integration (<30s), UI (any) tests
- **Coverage Integration**: JaCoCo baseline established with trend monitoring

### Qualitative Goals
- Modern testing patterns using JUnit 5 features
- Improved test maintainability and readability
- Enhanced test data management with `@TempDir`
- Better test isolation and cleanup

### Final Validation Checklist
- [ ] All 98 test files migrated to JUnit 5
- [ ] No JUnit 4 dependencies remain
- [ ] All test execution modes work (unit, integration, UI)
- [ ] Test performance improved by 20%
- [ ] JaCoCo integration working
- [ ] Documentation updated
- [ ] IDE configurations provided
- [ ] CI/CD ready for Phase 2B

## Risk Mitigation

### Technical Risks
- **SWT Display Management**: Test SWTBot integration early
- **Native Library Issues**: Validate HDF library loading with JUnit 5
- **Test Isolation**: Ensure proper cleanup between tests

### Timeline Risks
- **Complex UI Tests**: Prioritize simple migrations first
- **SWTBot Compatibility Issues**: Have fallback plan for problematic tests
- **Performance Regression**: Monitor and optimize throughout migration

### Mitigation Strategies
- Maintain vintage engine until migration complete
- Implement incremental migration with validation at each step
- Create comprehensive test validation suite
- Document all issues and solutions for team reference

This implementation plan provides detailed, actionable steps for migrating HDFView's entire test suite to JUnit 5 while improving test organization, performance, and maintainability.