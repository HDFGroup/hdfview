#!/bin/bash
#
# set-debug-logging.sh - Switch between debug logging configurations
#
# Usage: ./scripts/set-debug-logging.sh <config-name>
#
# Available configurations:
#   default  - Minimal logging (info level)
#   float16  - Debug Float16/BFLOAT16/Float8 issues (trace level for H5 classes)
#   list     - List all available configurations
#
# Examples:
#   ./scripts/set-debug-logging.sh float16
#   ./scripts/set-debug-logging.sh default

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEBUG_CONFIGS_DIR="$PROJECT_ROOT/.claude/debug-configs"
TARGET_FILE="$PROJECT_ROOT/hdfview/src/test/resources/simplelogger.properties"

# Function to list available configs
list_configs() {
    echo "Available debug logging configurations:"
    echo ""
    for config in "$DEBUG_CONFIGS_DIR"/simplelogger-*.properties; do
        if [ -f "$config" ]; then
            basename=$(basename "$config")
            name="${basename#simplelogger-}"
            name="${name%.properties}"

            # Get description from first comment line
            desc=$(grep "^#" "$config" | head -1 | sed 's/^# *//' || echo "No description")

            printf "  %-12s %s\n" "$name" "$desc"
        fi
    done
    echo ""
}

# Check arguments
if [ $# -eq 0 ]; then
    echo "Error: No configuration specified"
    echo ""
    echo "Usage: $0 <config-name>"
    echo ""
    list_configs
    exit 1
fi

CONFIG_NAME="$1"

# Handle special commands
if [ "$CONFIG_NAME" = "list" ]; then
    list_configs
    exit 0
fi

# Find the config file
CONFIG_FILE="$DEBUG_CONFIGS_DIR/simplelogger-$CONFIG_NAME.properties"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration '$CONFIG_NAME' not found"
    echo ""
    list_configs
    exit 1
fi

# Backup current config
BACKUP_FILE="$TARGET_FILE.backup-$(date +%Y%m%d-%H%M%S)"
if [ -f "$TARGET_FILE" ]; then
    cp "$TARGET_FILE" "$BACKUP_FILE"
    echo "Current config backed up to: $BACKUP_FILE"
fi

# Copy new config
cp "$CONFIG_FILE" "$TARGET_FILE"

echo ""
echo "✓ Switched to '$CONFIG_NAME' logging configuration"
echo ""
echo "Active logging config:"
grep "^org.slf4j.simpleLogger.log\." "$TARGET_FILE" | sed 's/^/  /' || echo "  Default (info level)"
echo ""
echo "Note: Recompile tests if you want the new logging to take effect:"
echo "  mvn test-compile -pl hdfview -DskipTests"
echo ""
