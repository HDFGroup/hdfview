@echo off
REM =============================================================================
REM HDFView Launcher Script for Windows
REM
REM This script validates the environment and launches an already-built HDFView.
REM Build the project first using: mvn clean package -DskipTests
REM
REM Launch options:
REM   1. Maven exec:java (for development)
REM   2. Direct JAR execution (recommended)
REM
REM Requirements:
REM   - Java 21+
REM   - Maven 3.6+ (only for option 1)
REM   - HDF5 and HDF4 native libraries (configured in build.properties)
REM   - HDFView must be built before running this script
REM =============================================================================

setlocal enabledelayedexpansion

REM Script configuration
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo.
echo === HDFView Environment Check ^& Launcher ===
echo.

REM =============================================================================
REM Function to load properties from build.properties
REM =============================================================================
set "PROPS_FILE=build.properties"
if not exist "%PROPS_FILE%" (
    echo [ERROR] build.properties file not found!
    exit /b 1
)

echo [INFO] Loading build.properties...

REM Parse build.properties file
for /f "usebackq tokens=1,* delims==" %%a in ("%PROPS_FILE%") do (
    set "line=%%a"
    set "value=%%b"

    REM Skip empty lines and comments
    if not "!line!"=="" (
        echo !line! | findstr /r "^#" >nul
        if errorlevel 1 (
            REM Replace dots with underscores for variable names
            set "key=!line:.=_!"
            set "!key!=!value!"
        )
    )
)

echo [OK] build.properties loaded
echo.

REM =============================================================================
REM Environment Validation
REM =============================================================================

echo [INFO] Checking project structure...
if not exist "pom.xml" (
    echo [ERROR] Not in HDFView project root directory
    exit /b 1
)
if not exist "%PROPS_FILE%" (
    echo [ERROR] build.properties not found
    exit /b 1
)
echo [OK] Found project files
echo.

REM Check Java
echo [INFO] Checking Java version...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found in PATH
    echo [ERROR] Please install Java 21 or later
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%v
    set JAVA_VERSION=!JAVA_VERSION:"=!
    goto :java_version_done
)
:java_version_done
echo [OK] Java !JAVA_VERSION! detected
echo.

REM Check Maven
echo [INFO] Checking Maven...
call mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found in PATH
    echo [ERROR] Please install Maven 3.6 or later
    exit /b 1
)

for /f "tokens=3" %%v in ('call mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
    set MVN_VERSION=%%v
    goto :mvn_version_done
)
:mvn_version_done
echo [OK] Maven !MVN_VERSION! found
echo.

REM Check HDF5 libraries
echo [INFO] Checking HDF5 libraries...
if "!hdf5_lib_dir!"=="" (
    echo [ERROR] hdf5.lib.dir not configured in build.properties
    exit /b 1
)
if not exist "!hdf5_lib_dir!" (
    echo [ERROR] HDF5 library directory not found: !hdf5_lib_dir!
    echo [ERROR] Set hdf5.lib.dir in build.properties
    exit /b 1
)
echo [OK] HDF5 library directory found: !hdf5_lib_dir!

if not "!hdf5_plugin_dir!"=="" (
    if exist "!hdf5_plugin_dir!" (
        echo [OK] HDF5 plugin directory found: !hdf5_plugin_dir!
    ) else (
        echo [WARN] HDF5 plugin directory not found: !hdf5_plugin_dir!
    )
)
echo.

REM Check HDF4 libraries (optional)
echo [INFO] Checking HDF4 libraries (optional)...
if not "!hdf_lib_dir!"=="" (
    if exist "!hdf_lib_dir!" (
        echo [OK] HDF4 library directory found: !hdf_lib_dir!
    ) else (
        echo [WARN] HDF4 library directory not found: !hdf_lib_dir!
    )
) else (
    echo [INFO] HDF4 support not configured (optional)
)
echo.

REM Check build status
echo [INFO] Checking build status...
if not exist "libs\hdfview-3.4-SNAPSHOT.jar" (
    echo [ERROR] HDFView JAR not found: libs\hdfview-3.4-SNAPSHOT.jar
    echo [ERROR] Build the project first: mvn clean package -DskipTests
    exit /b 1
)
echo [OK] HDFView JAR found
echo.

REM Check platform
echo [INFO] Checking SWT platform support...
echo [OK] Windows platform detected - SWT support available
echo.

echo [INFO] Environment validation complete!
echo.

REM Set up runtime environment
set "PATH=!hdf5_lib_dir!;!hdf_lib_dir!;!PATH!"
if not "!hdf5_plugin_dir!"=="" (
    set "HDF5_PLUGIN_PATH=!hdf5_plugin_dir!"
)

REM JVM arguments for proper module access
set JVM_ARGS=--add-opens java.base/java.lang=ALL-UNNAMED
set JVM_ARGS=%JVM_ARGS% --add-opens java.base/java.time=ALL-UNNAMED
set JVM_ARGS=%JVM_ARGS% --add-opens java.base/java.time.format=ALL-UNNAMED
set JVM_ARGS=%JVM_ARGS% --add-opens java.base/java.util=ALL-UNNAMED
set JVM_ARGS=%JVM_ARGS% --enable-native-access=jarhdf5
set JVM_ARGS=%JVM_ARGS% -Djava.library.path=!hdf5_lib_dir!;!hdf_lib_dir!

REM SLF4J logging configuration
set SLF4J_IMPL=nop
if "%1"=="--debug" set SLF4J_IMPL=simple
if "%HDFVIEW_DEBUG%"=="1" set SLF4J_IMPL=simple

if "!SLF4J_IMPL!"=="simple" (
    echo [INFO] Debug logging enabled (slf4j-simple^)
) else (
    echo [INFO] Logging disabled (slf4j-nop^). Use --debug or set HDFVIEW_DEBUG=1 to enable.
)
echo.

REM Launch options
echo Choose launch method:
echo 1. Maven exec:java (recommended for development)
echo 2. Direct JAR execution
echo 3. Just validate environment (no launch)
echo.
set /p CHOICE="Enter choice [1-3]: "

if "!CHOICE!"=="1" goto :maven_exec
if "!CHOICE!"=="2" goto :jar_exec
if "!CHOICE!"=="3" goto :validate
echo [ERROR] Invalid choice. Exiting.
exit /b 1

:maven_exec
echo [INFO] Launching HDFView via Maven...
echo Command: mvn exec:java -Dexec.mainClass="hdf.view.HDFView" -pl hdfview
echo.
call mvn exec:java -Dexec.mainClass="hdf.view.HDFView" -pl hdfview
goto :end

:jar_exec
echo [INFO] Launching HDFView via direct JAR execution...
if not exist "libs\hdfview-3.4-SNAPSHOT.jar" (
    echo [ERROR] JAR file not found. Build the project first.
    exit /b 1
)

if not exist "hdfview\target\lib" (
    echo [ERROR] Dependencies not found: hdfview\target\lib
    echo [ERROR] Build the project first: mvn clean package -DskipTests
    exit /b 1
)

REM Build classpath, excluding slf4j-nop or slf4j-simple based on debug mode
set CLASSPATH=libs\hdfview-3.4-SNAPSHOT.jar
for %%j in (hdfview\target\lib\*.jar) do (
    set "jarname=%%~nxj"
    if "!SLF4J_IMPL!"=="simple" (
        REM Skip nop, include simple
        echo !jarname! | findstr /i "^slf4j-nop" >nul
        if errorlevel 1 set "CLASSPATH=!CLASSPATH!;%%j"
    ) else (
        REM Skip simple, include nop
        echo !jarname! | findstr /i "^slf4j-simple" >nul
        if errorlevel 1 set "CLASSPATH=!CLASSPATH!;%%j"
    )
)

echo Command: java %JVM_ARGS% -cp "..." hdf.view.HDFView
echo.
java %JVM_ARGS% -cp "%CLASSPATH%" hdf.view.HDFView
goto :end

:validate
echo [OK] Environment validation complete. Ready to run HDFView!
echo.
echo To launch manually:
echo Option 1 (Maven^): mvn exec:java -Dexec.mainClass="hdf.view.HDFView" -pl hdfview
echo Option 2 (JAR^): java %JVM_ARGS% -cp "libs\hdfview-3.4-SNAPSHOT.jar;hdfview\target\lib\*" hdf.view.HDFView
goto :end

:end
echo.
echo [OK] Script completed!
endlocal
