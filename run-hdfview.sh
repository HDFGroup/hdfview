#!/bin/bash

# HDFView Launcher Script
# Validates environment and launches HDFView application

set -e  # Exit on any error

echo "=== HDFView Environment Check & Launcher ==="
echo

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [[ ! -f "pom.xml" ]] || [[ ! -f "build.properties" ]]; then
    print_error "Please run this script from the HDFView project root directory"
    print_error "Expected files: pom.xml, build.properties"
    exit 1
fi

print_success "Found project files (pom.xml, build.properties)"

# Load build.properties for validation
if [[ -f "build.properties" ]]; then
    print_status "Loading build.properties..."
    source build.properties
    print_success "build.properties loaded"
else
    print_error "build.properties file not found!"
    print_error "Copy build.properties.template to build.properties and configure it"
    exit 1
fi

# Validate Java version
print_status "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -ge 21 ]]; then
    print_success "Java $JAVA_VERSION detected (Java 21+ required)"
else
    print_error "Java 21+ required, found Java $JAVA_VERSION"
    exit 1
fi

# Validate Maven
print_status "Checking Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
    print_success "Maven $MVN_VERSION found"
else
    print_error "Maven not found in PATH"
    exit 1
fi

# Check HDF5 libraries (required)
print_status "Checking HDF5 libraries..."
if [[ -n "$hdf5_lib_dir" ]] && [[ -d "$hdf5_lib_dir" ]]; then
    print_success "HDF5 library directory found: $hdf5_lib_dir"

    # Check for specific HDF5 files
    HDF5_FOUND=0
    for file in libhdf5.so libhdf5.dylib hdf5.dll libhdf5.a; do
        if [[ -f "$hdf5_lib_dir/$file" ]]; then
            print_success "Found HDF5 library: $file"
            HDF5_FOUND=1
            break
        fi
    done

    if [[ $HDF5_FOUND -eq 0 ]]; then
        print_warning "HDF5 library files not found in $hdf5_lib_dir"
        print_warning "Application may fail to load HDF5 files"
    fi
else
    print_error "HDF5 library directory not configured or missing: $hdf5_lib_dir"
    print_error "Set hdf5.lib.dir in build.properties"
    exit 1
fi

# Check HDF5 plugins
if [[ -n "$hdf5_plugin_dir" ]] && [[ -d "$hdf5_plugin_dir" ]]; then
    print_success "HDF5 plugin directory found: $hdf5_plugin_dir"
else
    print_warning "HDF5 plugin directory not found: $hdf5_plugin_dir"
    print_warning "Some HDF5 features may be limited"
fi

# Check HDF4 libraries (optional)
print_status "Checking HDF4 libraries (optional)..."
if [[ -n "$hdf_lib_dir" ]] && [[ -d "$hdf_lib_dir" ]]; then
    print_success "HDF4 library directory found: $hdf_lib_dir"
else
    print_warning "HDF4 library directory not found: $hdf_lib_dir"
    print_warning "HDF4 support will be disabled"
fi

# Check if project needs building
print_status "Checking build status..."
if [[ ! -d "hdfview/target" ]] || [[ ! -f "libs/hdfview-3.4-SNAPSHOT.jar" ]]; then
    print_warning "Project not built or outdated build detected"
    BUILD_NEEDED=1
else
    print_success "Project appears to be built"
    BUILD_NEEDED=0
fi

# Check SWT dependencies
print_status "Checking SWT platform support..."
PLATFORM=$(uname -s)
case "$PLATFORM" in
    Linux)
        print_success "Linux platform detected - SWT GTK support available"
        ;;
    Darwin)
        print_success "macOS platform detected - SWT Cocoa support available"
        ;;
    CYGWIN*|MINGW*|MSYS*)
        print_success "Windows platform detected - SWT Win32 support available"
        ;;
    *)
        print_warning "Unknown platform: $PLATFORM - SWT support may be limited"
        ;;
esac

echo
print_status "Environment validation complete!"
echo

# Build if needed
if [[ $BUILD_NEEDED -eq 1 ]]; then
    print_status "Building project (this may take a few minutes)..."
    mvn clean install -q
    print_success "Build completed"
    echo
fi

# Set up runtime environment
export LD_LIBRARY_PATH="$hdf5_lib_dir:$hdf_lib_dir:$LD_LIBRARY_PATH"
export HDF5_PLUGIN_PATH="$hdf5_plugin_dir"

# JVM arguments for proper module access
JVM_ARGS=(
    "--add-opens" "java.base/java.lang=ALL-UNNAMED"
    "--add-opens" "java.base/java.time=ALL-UNNAMED"
    "--add-opens" "java.base/java.time.format=ALL-UNNAMED"
    "--add-opens" "java.base/java.util=ALL-UNNAMED"
    "--enable-native-access=jarhdf5"
    "-Djava.library.path=$hdf5_lib_dir:$hdf_lib_dir"
)

# Launch options
echo "Choose launch method:"
echo "1. Maven exec:java (recommended for development)"
echo "2. Direct JAR execution"
echo "3. Just validate environment (no launch)"
echo
read -p "Enter choice [1-3]: " CHOICE

case $CHOICE in
    1)
        print_status "Launching HDFView via Maven..."
        echo "Command: mvn exec:java -Dexec.mainClass=\"hdf.view.HDFView\" -pl hdfview"
        echo
        mvn exec:java -Dexec.mainClass="hdf.view.HDFView" -pl hdfview
        ;;
    2)
        print_status "Launching HDFView via direct JAR execution..."
        if [[ ! -f "libs/hdfview-3.4-SNAPSHOT.jar" ]]; then
            print_error "JAR file not found. Run option 1 first to build."
            exit 1
        fi

        CLASSPATH="libs/hdfview-3.4-SNAPSHOT.jar:hdfview/target/lib/*"
        echo "Command: java ${JVM_ARGS[*]} -cp \"$CLASSPATH\" hdf.view.HDFView"
        echo
        java "${JVM_ARGS[@]}" -cp "$CLASSPATH" hdf.view.HDFView
        ;;
    3)
        print_success "Environment validation complete. Ready to run HDFView!"
        echo
        echo "To launch manually:"
        echo "Option 1 (Maven): mvn exec:java -Dexec.mainClass=\"hdf.view.HDFView\" -pl hdfview"
        echo "Option 2 (JAR): java ${JVM_ARGS[*]} -cp \"libs/hdfview-3.4-SNAPSHOT.jar:hdfview/target/lib/*\" hdf.view.HDFView"
        ;;
    *)
        print_error "Invalid choice. Exiting."
        exit 1
        ;;
esac

print_success "Script completed!"