# Phase 2C: Quality and Static Analysis - Detailed Implementation Plan

**Duration**: 2-3 weeks
**Status**: Ready to begin after Phase 2B Task 2.9 complete
**Dependencies**: CI/CD pipeline infrastructure established, JUnit 5 migration complete

## Overview

Implement comprehensive Java 21 compatible static analysis, expand existing JaCoCo integration, and establish quality gates that prevent regression in code quality, security, and performance. Focus on PMD and Checkstyle since SpotBugs doesn't support Java 21 yet.

## Current Quality Infrastructure Status (from Phase 1)

### Already Established
- ✅ **JaCoCo Integration**: Maven plugin configured for all modules with aggregated reporting
- ✅ **SpotBugs Foundation**: Complete configuration ready for activation when Java 21 supported
- ✅ **Build Validation**: Maven enforcer plugin with dependency and property validation
- ✅ **Quality Infrastructure**: Plugin management and reporting structure in place

### Java 21 Compatibility Analysis
- **PMD**: v7.0+ supports Java 21 ✅
- **Checkstyle**: v10.12+ supports Java 21 ✅
- **OWASP Dependency Check**: Latest version supports Java 21 ✅
- **SpotBugs**: v4.8.6 does NOT support Java 21 ❌ (foundation ready)

## Detailed Task Implementation

### Task 2.14: Enhance Code Coverage Infrastructure (2 days)

#### Day 1: Advanced JaCoCo Configuration and Reporting
**Objective**: Enhance existing JaCoCo setup with detailed reporting and exclusions

**Actions**:
1. **Enhanced JaCoCo Configuration** (build on Phase 1 foundation):
   ```xml
   <!-- Update in parent POM -->
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <version>0.8.11</version>
       <configuration>
           <excludes>
               <!-- UI Framework exclusions -->
               <exclude>**/hdf/view/ViewProperties*</exclude>
               <exclude>**/hdf/view/dialog/*Dialog*</exclude>
               <!-- Native library wrappers -->
               <exclude>**/hdf/hdf5lib/**</exclude>
               <exclude>**/hdf/hdf4lib/**</exclude>
               <!-- Generated code -->
               <exclude>**/generated/**</exclude>
               <!-- Test utilities -->
               <exclude>**/test/**/*Test*</exclude>
           </excludes>
           <rules>
               <rule>
                   <element>BUNDLE</element>
                   <limits>
                       <limit>
                           <counter>LINE</counter>
                           <value>COVEREDRATIO</value>
                           <minimum>0.60</minimum>
                       </limit>
                       <limit>
                           <counter>BRANCH</counter>
                           <value>COVEREDRATIO</value>
                           <minimum>0.50</minimum>
                       </limit>
                   </limits>
               </rule>
               <rule>
                   <element>PACKAGE</element>
                   <includes>
                       <include>hdf.object.*</include>
                       <include>hdf.view.datacontent.*</include>
                   </includes>
                   <limits>
                       <limit>
                           <counter>LINE</counter>
                           <value>COVEREDRATIO</value>
                           <minimum>0.70</minimum>
                       </limit>
                   </limits>
               </rule>
           </rules>
       </configuration>
       <executions>
           <execution>
               <id>default-prepare-agent</id>
               <goals>
                   <goal>prepare-agent</goal>
               </goals>
           </execution>
           <execution>
               <id>default-report</id>
               <goals>
                   <goal>report</goal>
               </goals>
           </execution>
           <execution>
               <id>check-coverage</id>
               <goals>
                   <goal>check</goal>
               </goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **Coverage Trend Analysis Setup**:
   ```xml
   <!-- Add to reporting section -->
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <reportSets>
           <reportSet>
               <reports>
                   <report>report</report>
               </reports>
           </reportSet>
       </reportSets>
   </plugin>
   ```

#### Day 2: Coverage Integration with CI and Reporting
**Objective**: Integrate enhanced coverage with CI pipeline and create reporting

**Actions**:
1. **Create Coverage Analysis Script** (`scripts/analyze-coverage.sh`):
   ```bash
   #!/bin/bash
   # Generate coverage reports and analyze trends

   echo "Generating JaCoCo coverage reports..."
   mvn clean test jacoco:report

   # Extract coverage percentages
   COVERAGE=$(xmllint --xpath "string(//counter[@type='LINE']/@covered div //counter[@type='LINE']/@missed)" \
              target/site/jacoco/jacoco.xml)

   echo "Current line coverage: ${COVERAGE}%"

   # Check if coverage meets minimum threshold
   if (( $(echo "$COVERAGE < 0.60" | bc -l) )); then
       echo "❌ Coverage below minimum threshold (60%)"
       exit 1
   else
       echo "✅ Coverage meets minimum threshold"
   fi
   ```

2. **Coverage Report Generation**:
   ```xml
   <!-- Maven site plugin for comprehensive reporting -->
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-site-plugin</artifactId>
       <version>4.0.0-M11</version>
       <configuration>
           <generateReports>true</generateReports>
       </configuration>
   </plugin>
   ```

3. **Integrate with Phase 2B CI workflows**:
   - Add coverage analysis to maven-quality.yml workflow
   - Generate coverage comments on PRs
   - Set up coverage trend tracking

**Deliverables**:
- Enhanced JaCoCo configuration with detailed exclusions and rules
- Coverage trend analysis and reporting
- Integration with CI pipeline for automated coverage checking

### Task 2.15: Set Coverage Thresholds and Quality Gates (2 days)

#### Day 1: Module-Specific Coverage Thresholds
**Objective**: Define and implement appropriate coverage thresholds per module

**Actions**:
1. **Analyze Current Coverage Baseline**:
   ```bash
   # Generate baseline coverage report
   mvn clean test jacoco:report

   # Analyze per-module coverage
   find . -name "jacoco.xml" -exec echo "=== {} ===" \; \
          -exec xmllint --xpath "//counter[@type='LINE']" {} \;
   ```

2. **Configure Module-Specific Thresholds**:
   ```xml
   <!-- object/pom.xml - Higher threshold for core logic -->
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <configuration>
           <rules>
               <rule>
                   <element>BUNDLE</element>
                   <limits>
                       <limit>
                           <counter>LINE</counter>
                           <value>COVEREDRATIO</value>
                           <minimum>0.70</minimum>
                       </limit>
                   </limits>
               </rule>
           </rules>
       </configuration>
   </plugin>

   <!-- hdfview/pom.xml - Lower threshold for UI code -->
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <configuration>
           <rules>
               <rule>
                   <element>BUNDLE</element>
                   <limits>
                       <limit>
                           <counter>LINE</counter>
                           <value>COVEREDRATIO</value>
                           <minimum>0.50</minimum>
                       </limit>
                   </limits>
               </rule>
           </rules>
       </configuration>
   </plugin>
   ```

#### Day 2: Progressive Quality Gates Implementation
**Objective**: Implement quality gates with graduated enforcement timeline

**Actions**:
1. **Progressive Enforcement Configuration**:
   ```xml
   <!-- Quality gates with progressive enforcement -->
   <profiles>
       <!-- Week 1-2: Report only -->
       <profile>
           <id>quality-report</id>
           <activation>
               <activeByDefault>true</activeByDefault>
           </activation>
           <build>
               <plugins>
                   <plugin>
                       <groupId>org.jacoco</groupId>
                       <artifactId>jacoco-maven-plugin</artifactId>
                       <configuration>
                           <rules>
                               <rule>
                                   <element>BUNDLE</element>
                                   <limits>
                                       <limit>
                                           <counter>LINE</counter>
                                           <value>COVEREDRATIO</value>
                                           <minimum>0.60</minimum>
                                       </limit>
                                   </limits>
                               </rule>
                           </rules>
                           <haltOnFailure>false</haltOnFailure>
                       </configuration>
                   </plugin>
               </plugins>
           </build>
       </profile>

       <!-- Week 3-4: Warnings on regression -->
       <profile>
           <id>quality-warn</id>
           <build>
               <plugins>
                   <plugin>
                       <groupId>org.jacoco</groupId>
                       <artifactId>jacoco-maven-plugin</artifactId>
                       <configuration>
                           <haltOnFailure>false</haltOnFailure>
                           <rules>
                               <rule>
                                   <element>BUNDLE</element>
                                   <limits>
                                       <limit>
                                           <counter>LINE</counter>
                                           <value>COVEREDRATIO</value>
                                           <minimum>0.60</minimum>
                                       </limit>
                                   </limits>
                               </rule>
                           </rules>
                       </configuration>
                   </plugin>
               </plugins>
           </build>
       </profile>

       <!-- Week 5+: Enforce quality gates -->
       <profile>
           <id>quality-enforce</id>
           <build>
               <plugins>
                   <plugin>
                       <groupId>org.jacoco</groupId>
                       <artifactId>jacoco-maven-plugin</artifactId>
                       <configuration>
                           <haltOnFailure>true</haltOnFailure>
                           <rules>
                               <rule>
                                   <element>BUNDLE</element>
                                   <limits>
                                       <limit>
                                           <counter>LINE</counter>
                                           <value>COVEREDRATIO</value>
                                           <minimum>0.60</minimum>
                                       </limit>
                                   </limits>
                               </rule>
                           </rules>
                       </configuration>
                   </plugin>
               </plugins>
           </build>
       </profile>
   </profiles>
   ```

2. **Incremental Coverage for New Code**:
   ```xml
   <plugin>
       <groupId>com.form.diff-coverage</groupId>
       <artifactId>diff-coverage-maven-plugin</artifactId>
       <version>2.5.0</version>
       <configuration>
           <comparisonBranch>refs/remotes/origin/master-maven</comparisonBranch>
           <coverageReportPath>target/site/jacoco/jacoco.xml</coverageReportPath>
           <lineCoverageMin>0.8</lineCoverageMin>
           <branchCoverageMin>0.7</branchCoverageMin>
           <failOnViolation>false</failOnViolation> <!-- Start with warnings -->
       </configuration>
   </plugin>
   ```

**Deliverables**:
- Module-specific coverage thresholds based on code complexity
- Incremental coverage requirements for new code (80% minimum)
- Quality gates integrated into Maven build lifecycle

### Task 2.16: Implement Java 21 Compatible Static Analysis (4 days)

#### Day 1: PMD Integration and Configuration
**Objective**: Set up PMD v7.0+ for comprehensive Java 21 static analysis

**Actions**:
1. **Add PMD Maven Plugin**:
   ```xml
   <!-- Add to parent POM pluginManagement -->
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-pmd-plugin</artifactId>
       <version>3.21.2</version>
       <dependencies>
           <dependency>
               <groupId>net.sourceforge.pmd</groupId>
               <artifactId>pmd-core</artifactId>
               <version>7.0.0</version>
           </dependency>
           <dependency>
               <groupId>net.sourceforge.pmd</groupId>
               <artifactId>pmd-java</artifactId>
               <version>7.0.0</version>
           </dependency>
       </dependencies>
       <configuration>
           <targetJdk>21</targetJdk>
           <rulesets>
               <ruleset>pmd-rules.xml</ruleset>
           </rulesets>
           <excludes>
               <exclude>**/hdf/hdf5lib/**/*.java</exclude>
               <exclude>**/hdf/hdf4lib/**/*.java</exclude>
               <exclude>**/test/**/*.java</exclude>
           </excludes>
           <includeTests>false</includeTests>
           <failOnViolation>true</failOnViolation>
           <verbose>true</verbose>
       </configuration>
       <executions>
           <execution>
               <id>pmd-check</id>
               <phase>verify</phase>
               <goals>
                   <goal>check</goal>
               </goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **Create HDFView-Specific PMD Ruleset** (`pmd-rules.xml`):
   ```xml
   <?xml version="1.0"?>
   <ruleset name="HDFView PMD Rules">
       <description>PMD rules customized for HDFView project</description>

       <!-- Include standard rulesets -->
       <rule ref="category/java/bestpractices.xml">
           <exclude name="JUnitTestsShouldIncludeAssert"/>
           <exclude name="UseVarargs"/> <!-- SWT methods don't use varargs -->
       </rule>

       <rule ref="category/java/codestyle.xml">
           <exclude name="OnlyOneReturn"/>
           <exclude name="AtLeastOneConstructor"/>
           <exclude name="CommentDefaultAccessModifier"/>
       </rule>

       <rule ref="category/java/design.xml">
           <exclude name="TooManyMethods"/> <!-- UI classes have many methods -->
           <exclude name="LawOfDemeter"/> <!-- UI event handling violates this -->
       </rule>

       <rule ref="category/java/performance.xml"/>

       <rule ref="category/java/security.xml"/>

       <!-- Custom rules for HDF patterns -->
       <rule ref="category/java/errorprone.xml/CloseResource">
           <properties>
               <property name="closeTargets"
                        value="H5File,H4File,NC2File,FileFormat"/>
           </properties>
       </rule>
   </ruleset>
   ```

#### Day 2: Checkstyle Integration and Configuration
**Objective**: Set up Checkstyle v10.12+ for code style enforcement

**Actions**:
1. **Add Checkstyle Maven Plugin**:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-checkstyle-plugin</artifactId>
       <version>3.3.1</version>
       <dependencies>
           <dependency>
               <groupId>com.puppycrawl.tools</groupId>
               <artifactId>checkstyle</artifactId>
               <version>10.12.5</version>
           </dependency>
       </dependencies>
       <configuration>
           <configLocation>checkstyle-rules.xml</configLocation>
           <includeTestSourceDirectory>false</includeTestSourceDirectory>
           <excludes>
               **/hdf/hdf5lib/**/*.java,
               **/hdf/hdf4lib/**/*.java,
               **/test/**/*.java
           </excludes>
           <failOnViolation>true</failOnViolation>
           <violationSeverity>warning</violationSeverity>
       </configuration>
       <executions>
           <execution>
               <id>checkstyle-check</id>
               <phase>verify</phase>
               <goals>
                   <goal>check</goal>
               </goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **Create HDFView Checkstyle Configuration** (`checkstyle-rules.xml`):
   ```xml
   <?xml version="1.0"?>
   <!DOCTYPE module PUBLIC
       "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
       "https://checkstyle.org/dtds/configuration_1_3.dtd">

   <module name="Checker">
       <property name="severity" value="warning"/>

       <!-- File length checks -->
       <module name="FileLength">
           <property name="max" value="2000"/>
       </module>

       <!-- Whitespace checks -->
       <module name="FileTabCharacter"/>

       <module name="TreeWalker">
           <!-- Naming conventions -->
           <module name="ConstantName"/>
           <module name="LocalFinalVariableName"/>
           <module name="LocalVariableName"/>
           <module name="MemberName"/>
           <module name="MethodName"/>
           <module name="PackageName"/>
           <module name="ParameterName"/>
           <module name="StaticVariableName"/>
           <module name="TypeName"/>

           <!-- Import checks -->
           <module name="AvoidStarImport"/>
           <module name="IllegalImport"/>
           <module name="RedundantImport"/>
           <module name="UnusedImports"/>

           <!-- Size violations -->
           <module name="MethodLength">
               <property name="max" value="150"/>
           </module>
           <module name="ParameterNumber">
               <property name="max" value="10"/>
           </module>

           <!-- Whitespace -->
           <module name="EmptyForIteratorPad"/>
           <module name="GenericWhitespace"/>
           <module name="MethodParamPad"/>
           <module name="NoWhitespaceAfter"/>
           <module name="NoWhitespaceBefore"/>
           <module name="OperatorWrap"/>
           <module name="ParenPad"/>
           <module name="TypecastParenPad"/>
           <module name="WhitespaceAfter"/>
           <module name="WhitespaceAround"/>

           <!-- Modifier checks -->
           <module name="ModifierOrder"/>
           <module name="RedundantModifier"/>

           <!-- Block checks -->
           <module name="AvoidNestedBlocks"/>
           <module name="EmptyBlock"/>
           <module name="LeftCurly"/>
           <module name="NeedBraces"/>
           <module name="RightCurly"/>

           <!-- Common coding problems -->
           <module name="EmptyStatement"/>
           <module name="EqualsHashCode"/>
           <module name="HiddenField">
               <property name="ignoreConstructorParameter" value="true"/>
               <property name="ignoreSetter" value="true"/>
           </module>
           <module name="IllegalInstantiation"/>
           <module name="InnerAssignment"/>
           <module name="MissingSwitchDefault"/>
           <module name="SimplifyBooleanExpression"/>
           <module name="SimplifyBooleanReturn"/>

           <!-- Class design -->
           <module name="FinalClass"/>
           <module name="HideUtilityClassConstructor"/>
           <module name="InterfaceIsType"/>
           <module name="VisibilityModifier"/>

           <!-- Miscellaneous -->
           <module name="ArrayTypeStyle"/>
           <module name="TodoComment">
               <property name="severity" value="info"/>
           </module>
           <module name="UpperEll"/>
       </module>
   </module>
   ```

#### Day 3: IDE Integration and Developer Tooling
**Objective**: Integrate static analysis with development environment

**Actions**:
1. **Create IDE Configuration Files**:
   ```xml
   <!-- .idea/inspections/Project_Default.xml for IntelliJ -->
   <component name="InspectionProjectProfileManager">
       <profile version="1.0">
           <option name="myName" value="HDFView"/>
           <inspection_tool class="PMD" enabled="true" level="WARNING" enabled_by_default="true">
               <option name="options">
                   <map>
                       <entry key="rulesets" value="pmd-rules.xml"/>
                   </map>
               </option>
           </inspection_tool>
       </profile>
   </component>
   ```

2. **Create Pre-commit Hook Script** (`scripts/pre-commit-quality.sh`):
   ```bash
   #!/bin/bash
   # Pre-commit quality checks

   echo "Running PMD analysis..."
   mvn pmd:check -q
   if [ $? -ne 0 ]; then
       echo "❌ PMD violations found. Fix issues before committing."
       exit 1
   fi

   echo "Running Checkstyle analysis..."
   mvn checkstyle:check -q
   if [ $? -ne 0 ]; then
       echo "❌ Checkstyle violations found. Fix issues before committing."
       exit 1
   fi

   echo "✅ Static analysis checks passed."
   ```

3. **Create Developer Quality Commands** (`scripts/check-quality.sh`):
   ```bash
   #!/bin/bash
   # Comprehensive quality check script

   echo "🔍 Running comprehensive quality analysis..."

   # PMD Analysis
   echo "Running PMD..."
   mvn pmd:pmd

   # Checkstyle Analysis
   echo "Running Checkstyle..."
   mvn checkstyle:checkstyle

   # Coverage Analysis
   echo "Running Coverage Analysis..."
   mvn clean test jacoco:report

   echo "📊 Quality reports generated:"
   echo "- PMD: target/site/pmd.html"
   echo "- Checkstyle: target/site/checkstyle.html"
   echo "- Coverage: target/site/jacoco/index.html"
   ```

#### Day 4: SpotBugs Foundation Enhancement
**Objective**: Enhance existing SpotBugs setup for future activation

**Actions**:
1. **Update SpotBugs Configuration** (build on Phase 1 foundation):
   ```xml
   <!-- Enhanced version of existing configuration -->
   <plugin>
       <groupId>com.github.spotbugs</groupId>
       <artifactId>spotbugs-maven-plugin</artifactId>
       <version>4.8.6.4</version>
       <dependencies>
           <dependency>
               <groupId>com.github.spotbugs</groupId>
               <artifactId>spotbugs</artifactId>
               <version>4.8.6</version>
           </dependency>
       </dependencies>
       <configuration>
           <effort>Max</effort>
           <threshold>Medium</threshold>
           <failOnError>false</failOnError>
           <includeTests>false</includeTests>
           <excludeFilterFile>spotbugs-exclude.xml</excludeFilterFile>
           <plugins>
               <plugin>
                   <groupId>com.h3xstream.findsecbugs</groupId>
                   <artifactId>findsecbugs-plugin</artifactId>
                   <version>1.12.0</version>
               </plugin>
           </plugins>
       </configuration>
       <!-- Executions commented out until Java 21 support -->
       <!--
       <executions>
           <execution>
               <id>spotbugs-check</id>
               <phase>verify</phase>
               <goals>
                   <goal>check</goal>
               </goals>
           </execution>
       </executions>
       -->
   </plugin>
   ```

2. **Monitor SpotBugs Java 21 Support**:
   ```markdown
   # SpotBugs Java 21 Activation Plan

   ## Current Status
   - SpotBugs 4.8.6 does NOT support Java 21 bytecode
   - Foundation complete and ready for activation
   - Issue tracking: https://github.com/spotbugs/spotbugs/issues/2524

   ## Activation Steps (when support available)
   1. Update SpotBugs version in pom.xml
   2. Uncomment executions section
   3. Run initial analysis: `mvn spotbugs:check`
   4. Address high-priority findings
   5. Enable in CI pipeline
   ```

**Deliverables**:
- Complete PMD integration with HDFView-specific rules
- Comprehensive Checkstyle configuration and enforcement
- IDE integration and developer tooling
- Enhanced SpotBugs foundation ready for Java 21 activation

### Task 2.17: Add Security and Dependency Analysis (2 days)

#### Day 1: OWASP Dependency Check Implementation
**Objective**: Set up comprehensive dependency vulnerability scanning

**Actions**:
1. **Add OWASP Dependency Check Plugin**:
   ```xml
   <plugin>
       <groupId>org.owasp</groupId>
       <artifactId>dependency-check-maven</artifactId>
       <version>9.0.7</version>
       <configuration>
           <failBuildOnCVSS>8.0</failBuildOnCVSS>
           <skipTestScope>true</skipTestScope>
           <skipProvidedScope>true</skipProvidedScope>
           <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
           <formats>
               <format>HTML</format>
               <format>XML</format>
               <format>JSON</format>
           </formats>
           <outputDirectory>target/dependency-check</outputDirectory>
       </configuration>
       <executions>
           <execution>
               <goals>
                   <goal>check</goal>
               </goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **Create Dependency Suppressions File** (`dependency-check-suppressions.xml`):
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
       <!-- SWT false positives - native library vulnerabilities don't apply to Java usage -->
       <suppress>
           <notes><![CDATA[SWT native library vulnerabilities not applicable to Java usage]]></notes>
           <gav regex="true">^org\.eclipse\.platform:org\.eclipse\.swt.*:.*$</gav>
           <cve>CVE-2021-28170</cve>
       </suppress>

       <!-- HDF library false positives - we use specific tested versions -->
       <suppress>
           <notes><![CDATA[HDF library version controlled and tested]]></notes>
           <gav regex="true">^.*:.*hdf.*:.*$</gav>
           <cve>CVE-2020-10809</cve>
       </suppress>
   </suppressions>
   ```

#### Day 2: License Compliance and Security Integration
**Objective**: Add license compliance checking and integrate with CI

**Actions**:
1. **License Compliance Plugin**:
   ```xml
   <plugin>
       <groupId>org.codehaus.mojo</groupId>
       <artifactId>license-maven-plugin</artifactId>
       <version>2.3.0</version>
       <configuration>
           <licenseMerges>
               <licenseMerge>Apache License, Version 2.0|The Apache Software License, Version 2.0</licenseMerge>
               <licenseMerge>BSD License|BSD|BSD-2-Clause|BSD-3-Clause</licenseMerge>
               <licenseMerge>MIT License|MIT</licenseMerge>
           </licenseMerges>
           <failOnMissing>false</failOnMissing>
           <failOnBlacklist>true</failOnBlacklist>
           <excludedLicenses>
               <excludedLicense>GPL v2</excludedLicense>
               <excludedLicense>GPL v3</excludedLicense>
           </excludedLicenses>
       </configuration>
   </plugin>
   ```

2. **Security Analysis Integration Script** (`scripts/security-analysis.sh`):
   ```bash
   #!/bin/bash
   # Comprehensive security analysis

   echo "🔒 Running security analysis..."

   # OWASP Dependency Check
   echo "Checking dependencies for vulnerabilities..."
   mvn org.owasp:dependency-check-maven:check

   # License compliance
   echo "Checking license compliance..."
   mvn license:check-file-header license:aggregate-third-party-report

   # Generate security report
   echo "📋 Security reports generated:"
   echo "- Vulnerability Report: target/dependency-check/dependency-check-report.html"
   echo "- License Report: target/generated-sources/license/THIRD-PARTY.txt"
   ```

**Deliverables**:
- OWASP dependency vulnerability scanning with CI integration
- License compliance checking and reporting
- Security analysis automation and reporting

### Task 2.18: Create Unified Quality Reporting (3 days)

#### Day 1: Quality Dashboard Setup
**Objective**: Create unified quality reporting and dashboard

**Actions**:
1. **Maven Site Configuration for Unified Reporting**:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-site-plugin</artifactId>
       <version>4.0.0-M11</version>
   </plugin>

   <!-- Reporting plugins -->
   <reporting>
       <plugins>
           <plugin>
               <groupId>org.jacoco</groupId>
               <artifactId>jacoco-maven-plugin</artifactId>
               <reportSets>
                   <reportSet>
                       <reports>
                           <report>report</report>
                       </reports>
                   </reportSet>
               </reportSets>
           </plugin>
           <plugin>
               <groupId>org.apache.maven.plugins</groupId>
               <artifactId>maven-pmd-plugin</artifactId>
               <reportSets>
                   <reportSet>
                       <reports>
                           <report>pmd</report>
                           <report>cpd</report>
                       </reports>
                   </reportSet>
               </reportSets>
           </plugin>
           <plugin>
               <groupId>org.apache.maven.plugins</groupId>
               <artifactId>maven-checkstyle-plugin</artifactId>
               <reportSets>
                   <reportSet>
                       <reports>
                           <report>checkstyle</report>
                       </reports>
                   </reportSet>
               </reportSets>
           </plugin>
           <plugin>
               <groupId>org.owasp</groupId>
               <artifactId>dependency-check-maven</artifactId>
               <reportSets>
                   <reportSet>
                       <reports>
                           <report>aggregate</report>
                       </reports>
                   </reportSet>
               </reportSets>
           </plugin>
       </plugins>
   </reporting>
   ```

2. **Quality Metrics Collection Script** (`scripts/collect-metrics.sh`):
   ```bash
   #!/bin/bash
   # Collect and aggregate quality metrics

   echo "📊 Collecting quality metrics..."

   # Generate all reports
   mvn clean test site

   # Extract metrics
   COVERAGE=$(xmllint --xpath "string(//counter[@type='LINE']/@covered div (//counter[@type='LINE']/@covered + //counter[@type='LINE']/@missed) * 100)" target/site/jacoco/jacoco.xml)
   PMD_VIOLATIONS=$(xmllint --xpath "count(//violation)" target/site/pmd.xml)
   CHECKSTYLE_VIOLATIONS=$(xmllint --xpath "count(//error)" target/checkstyle-result.xml)

   # Create metrics summary
   cat > target/quality-summary.json << EOF
   {
       "timestamp": "$(date -Iseconds)",
       "coverage": {
           "line": ${COVERAGE:-0}
       },
       "pmd": {
           "violations": ${PMD_VIOLATIONS:-0}
       },
       "checkstyle": {
           "violations": ${CHECKSTYLE_VIOLATIONS:-0}
       }
   }
   EOF

   echo "✅ Quality metrics collected in target/quality-summary.json"
   ```

#### Day 2: Quality Gate Enforcement and CI Integration
**Objective**: Implement comprehensive quality gates in build pipeline

**Actions**:
1. **Quality Gate Maven Configuration**:
   ```xml
   <!-- Create quality profile -->
   <profile>
       <id>quality-gate</id>
       <build>
           <plugins>
               <!-- JaCoCo with strict thresholds -->
               <plugin>
                   <groupId>org.jacoco</groupId>
                   <artifactId>jacoco-maven-plugin</artifactId>
                   <executions>
                       <execution>
                           <id>quality-gate-check</id>
                           <goals>
                               <goal>check</goal>
                           </goals>
                           <configuration>
                               <rules>
                                   <rule>
                                       <element>BUNDLE</element>
                                       <limits>
                                           <limit>
                                               <counter>LINE</counter>
                                               <value>COVEREDRATIO</value>
                                               <minimum>0.60</minimum>
                                           </limit>
                                       </limits>
                                   </rule>
                               </rules>
                           </configuration>
                       </execution>
                   </executions>
               </plugin>

               <!-- PMD with failure on violations -->
               <plugin>
                   <groupId>org.apache.maven.plugins</groupId>
                   <artifactId>maven-pmd-plugin</artifactId>
                   <executions>
                       <execution>
                           <id>quality-gate-pmd</id>
                           <goals>
                               <goal>check</goal>
                           </goals>
                           <configuration>
                               <failOnViolation>true</failOnViolation>
                               <violationSeverity>4</violationSeverity>
                           </configuration>
                       </execution>
                   </executions>
               </plugin>

               <!-- Checkstyle with failure on violations -->
               <plugin>
                   <groupId>org.apache.maven.plugins</groupId>
                   <artifactId>maven-checkstyle-plugin</artifactId>
                   <executions>
                       <execution>
                           <id>quality-gate-checkstyle</id>
                           <goals>
                               <goal>check</goal>
                           </goals>
                           <configuration>
                               <failOnViolation>true</failOnViolation>
                               <violationSeverity>warning</violationSeverity>
                           </configuration>
                       </execution>
                   </executions>
               </plugin>
           </plugins>
       </build>
   </profile>
   ```

2. **Quality Gate Validation Script** (`scripts/validate-quality.sh`):
   ```bash
   #!/bin/bash
   # Validate quality gates before merge

   echo "🚦 Validating quality gates..."

   # Run quality gate profile
   mvn clean verify -Pquality-gate

   if [ $? -eq 0 ]; then
       echo "✅ All quality gates passed"
       exit 0
   else
       echo "❌ Quality gates failed"
       exit 1
   fi
   ```

#### Day 3: Trend Analysis and Alerting
**Objective**: Set up quality trend monitoring and regression alerting

**Actions**:
1. **Quality Trend Analysis** (`scripts/trend-analysis.sh`):
   ```bash
   #!/bin/bash
   # Analyze quality trends over time

   # Store current metrics
   METRICS_FILE="quality-history/$(date +%Y%m%d-%H%M%S)-metrics.json"
   mkdir -p quality-history
   cp target/quality-summary.json "$METRICS_FILE"

   # Analyze trend (compare with previous runs)
   python3 scripts/analyze-trends.py quality-history/
   ```

2. **Create Quality Trend Analysis Script** (`scripts/analyze-trends.py`):
   ```python
   #!/usr/bin/env python3
   """Analyze quality trends and detect regressions"""

   import json
   import glob
   import sys
   from datetime import datetime

   def analyze_trends(history_dir):
       """Analyze quality metric trends"""
       files = sorted(glob.glob(f"{history_dir}/*.json"))
       if len(files) < 2:
           print("Need at least 2 data points for trend analysis")
           return

       # Load latest metrics
       with open(files[-1]) as f:
           current = json.load(f)

       with open(files[-2]) as f:
           previous = json.load(f)

       # Check for regressions
       regressions = []

       if current['coverage']['line'] < previous['coverage']['line'] - 5:
           regressions.append(f"Coverage decreased by {previous['coverage']['line'] - current['coverage']['line']:.1f}%")

       if current['pmd']['violations'] > previous['pmd']['violations'] * 1.2:
           regressions.append(f"PMD violations increased by {current['pmd']['violations'] - previous['pmd']['violations']}")

       if regressions:
           print("⚠️  Quality regressions detected:")
           for regression in regressions:
               print(f"  - {regression}")
           sys.exit(1)
       else:
           print("✅ No quality regressions detected")

   if __name__ == "__main__":
       analyze_trends(sys.argv[1])
   ```

**Deliverables**:
- Unified quality dashboard with all metrics
- Comprehensive quality gates preventing regression
- Quality trend analysis and regression detection

### Task 2.19: Performance and Memory Analysis (2 days)

#### Day 1: JMH Performance Benchmarking Setup
**Objective**: Set up JMH for performance testing of HDF operations

**Actions**:
1. **Add JMH Dependencies and Plugin**:
   ```xml
   <!-- Add to hdfview/pom.xml -->
   <dependency>
       <groupId>org.openjdk.jmh</groupId>
       <artifactId>jmh-core</artifactId>
       <version>1.37</version>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.openjdk.jmh</groupId>
       <artifactId>jmh-generator-annprocess</artifactId>
       <version>1.37</version>
       <scope>test</scope>
   </dependency>

   <!-- JMH Plugin -->
   <plugin>
       <groupId>org.openjdk.jmh</groupId>
       <artifactId>jmh-maven-plugin</artifactId>
       <version>1.37</version>
       <configuration>
           <benchmarkMode>
               <benchmarkMode>AverageTime</benchmarkMode>
           </benchmarkMode>
           <outputTimeUnit>MILLISECONDS</outputTimeUnit>
           <warmupIterations>3</warmupIterations>
           <measurementIterations>5</measurementIterations>
           <threads>1</threads>
           <fork>1</fork>
       </configuration>
   </plugin>
   ```

2. **Create Performance Benchmarks** (`hdfview/src/test/java/benchmarks/HDFPerformanceBenchmarks.java`):
   ```java
   package benchmarks;

   import hdf.object.h5.H5File;
   import hdf.object.Dataset;
   import org.openjdk.jmh.annotations.*;
   import org.openjdk.jmh.runner.Runner;
   import org.openjdk.jmh.runner.options.Options;
   import org.openjdk.jmh.runner.options.OptionsBuilder;

   import java.nio.file.Files;
   import java.nio.file.Path;
   import java.util.concurrent.TimeUnit;

   @BenchmarkMode(Mode.AverageTime)
   @OutputTimeUnit(TimeUnit.MILLISECONDS)
   @State(Scope.Benchmark)
   public class HDFPerformanceBenchmarks {

       private Path testFile;
       private H5File h5File;

       @Setup(Level.Trial)
       public void setupTrial() throws Exception {
           // Create temporary test file
           testFile = Files.createTempFile("benchmark", ".h5");
           h5File = new H5File(testFile.toString(), FileFormat.CREATE);

           // Create test datasets of various sizes
           createTestDatasets();
       }

       @TearDown(Level.Trial)
       public void teardownTrial() throws Exception {
           if (h5File != null) {
               h5File.close();
           }
           Files.deleteIfExists(testFile);
       }

       @Benchmark
       public void benchmarkFileOpen() throws Exception {
           H5File file = new H5File(testFile.toString(), FileFormat.READ);
           file.open();
           file.close();
       }

       @Benchmark
       public void benchmarkDatasetRead() throws Exception {
           Dataset dataset = (Dataset) h5File.get("small_dataset");
           Object data = dataset.getData();
       }

       @Benchmark
       public void benchmarkLargeDatasetRead() throws Exception {
           Dataset dataset = (Dataset) h5File.get("large_dataset");
           Object data = dataset.getData();
       }

       private void createTestDatasets() throws Exception {
           // Create datasets of various sizes for benchmarking
           // Implementation details...
       }

       public static void main(String[] args) throws Exception {
           Options opt = new OptionsBuilder()
               .include(HDFPerformanceBenchmarks.class.getSimpleName())
               .build();

           new Runner(opt).run();
       }
   }
   ```

#### Day 2: Memory Usage Monitoring and Regression Detection
**Objective**: Set up memory profiling and performance regression detection

**Actions**:
1. **Memory Usage Monitoring** (`scripts/memory-analysis.sh`):
   ```bash
   #!/bin/bash
   # Memory usage analysis for HDF operations

   echo "🧠 Running memory analysis..."

   # Run with memory profiling
   java -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
        -Xloggc:target/gc.log \
        -jar target/hdfview-*.jar --benchmark

   # Analyze GC log
   if command -v gcviewer &> /dev/null; then
       gcviewer -o csv target/gc.log > target/gc-analysis.csv
       echo "GC analysis saved to target/gc-analysis.csv"
   fi
   ```

2. **Performance Regression Detection** (`scripts/performance-check.sh`):
   ```bash
   #!/bin/bash
   # Check for performance regressions

   echo "⏱️  Running performance benchmarks..."

   # Run JMH benchmarks
   mvn jmh:benchmark -Djmh.resultsFile=target/benchmark-results.json

   # Compare with baseline (if exists)
   if [ -f performance-baseline.json ]; then
       python3 scripts/compare-performance.py \
           performance-baseline.json \
           target/benchmark-results.json
   else
       echo "No baseline found. Current results will serve as baseline."
       cp target/benchmark-results.json performance-baseline.json
   fi
   ```

**Deliverables**:
- JMH performance benchmarks for critical HDF operations
- Memory usage monitoring and GC analysis
- Performance regression detection and baseline tracking

### Task 2.20: Documentation Quality and API Standards (1 day)

#### Day 1: JavaDoc Enhancement and API Documentation
**Objective**: Enhance existing JavaDoc setup and create API documentation standards

**Actions**:
1. **Enhanced JavaDoc Configuration** (build on Phase 1 foundation):
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-javadoc-plugin</artifactId>
       <version>3.6.3</version>
       <configuration>
           <doclint>all,-missing</doclint>
           <failOnError>false</failOnError>
           <failOnWarnings>false</failOnWarnings>
           <quiet>true</quiet>
           <source>21</source>
           <additionalJOption>-Xdoclint:all,-missing</additionalJOption>
           <links>
               <link>https://docs.oracle.com/en/java/javase/21/docs/api/</link>
               <link>https://help.eclipse.org/2023-12/topic/org.eclipse.platform.doc.isv/reference/api/</link>
           </links>
           <groups>
               <group>
                   <title>HDF Object Model</title>
                   <packages>hdf.object*</packages>
               </group>
               <group>
                   <title>HDFView Application</title>
                   <packages>hdf.view*</packages>
               </group>
           </groups>
       </configuration>
       <executions>
           <execution>
               <id>aggregate</id>
               <goals>
                   <goal>aggregate</goal>
               </goals>
               <phase>site</phase>
           </execution>
       </executions>
   </plugin>
   ```

2. **API Documentation Standards** (`docs/api-documentation-standards.md`):
   ```markdown
   # API Documentation Standards

   ## JavaDoc Requirements
   - All public classes must have class-level JavaDoc
   - All public methods must have complete JavaDoc with @param and @return
   - Use @since tags for new functionality
   - Include code examples for complex APIs

   ## Documentation Quality Checks
   Run: `mvn javadoc:javadoc -Dshow=private`

   ## API Stability Guidelines
   - Mark experimental APIs with @Deprecated or @Experimental
   - Document breaking changes in release notes
   - Maintain backward compatibility when possible
   ```

3. **Documentation Quality Validation**:
   ```bash
   #!/bin/bash
   # scripts/check-docs.sh
   # Validate documentation quality

   echo "📚 Checking documentation quality..."

   # Generate JavaDoc with full linting
   mvn javadoc:javadoc -Ddoclint=all

   # Check for undocumented public APIs
   grep -r "public.*class\|public.*interface" src/main/java | \
       grep -v "@" | wc -l > target/undocumented-apis.count

   echo "Documentation check complete."
   echo "See target/site/apidocs/ for generated documentation."
   ```

**Deliverables**:
- Enhanced JavaDoc configuration with comprehensive API documentation
- API documentation standards and quality guidelines
- Automated documentation quality validation

## Success Metrics and Acceptance Criteria

### Quantitative Goals
- **Code Coverage**: >60% line coverage with >50% branch coverage
- **Static Analysis**: Zero high-priority PMD violations, zero Checkstyle errors
- **Security**: Zero high-severity (CVSS >8.0) dependency vulnerabilities
- **Performance**: Baseline performance benchmarks established
- **Documentation**: >90% public API documentation coverage

### Qualitative Goals
- Java 21 compatible static analysis running in CI
- Quality gates preventing regression in coverage and style
- Comprehensive security scanning and compliance reporting
- Performance monitoring and regression detection
- Unified quality dashboard and reporting

### Final Validation Checklist
- [ ] JaCoCo coverage reporting with 60%+ line coverage achieved
- [ ] PMD v7.0+ running with HDFView-specific rules
- [ ] Checkstyle v10.12+ enforcing code style standards
- [ ] OWASP dependency scanning with vulnerability thresholds
- [ ] Quality gates integrated into Maven build lifecycle
- [ ] Performance benchmarks baseline established
- [ ] Unified quality reporting dashboard functional
- [ ] SpotBugs foundation ready for Java 21 activation
- [ ] Documentation quality standards implemented

## Risk Mitigation

### Technical Risks
- **Java 21 Compatibility**: Use confirmed compatible tool versions (PMD 7.0+, Checkstyle 10.12+)
- **Performance Impact**: Optimize analysis execution, use caching strategies
- **False Positives**: Comprehensive exclusion rules for SWT/native library patterns

### Process Risks
- **Quality Gate Friction**: Gradual enforcement, clear developer guidance
- **Tool Learning Curve**: Comprehensive documentation and training materials
- **Build Performance**: Optimize analysis execution, parallel processing

### Mitigation Strategies
- Test all configurations thoroughly before CI integration
- Provide comprehensive developer tooling and IDE integration
- Implement gradual quality gate enforcement to avoid disruption
- Monitor and optimize build performance continuously
- Maintain clear documentation and troubleshooting guides

This implementation plan provides detailed, actionable steps for establishing comprehensive code quality and static analysis infrastructure that supports HDFView's Java 21 development while maintaining high standards for code quality, security, and performance.