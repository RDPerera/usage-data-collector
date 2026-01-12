#!/bin/bash

# Script to deploy usage data collector JARs to WSO2 MI
# This script builds and copies the OSGi bundles to the MI lib/plugins directory
# and cleans the dropins directory

# Define paths
PROJECT_DIR="/Users/dilanperera/Projects/usage-data-collector"
COMMON_COLLECTOR_JAR="$PROJECT_DIR/collectors/org.wso2.carbon.usage.data.collector.common/target/org.wso2.carbon.usage.data.collector.common-1.0.0-SNAPSHOT.jar"
MI_COLLECTOR_JAR="$PROJECT_DIR/collectors/org.wso2.carbon.usage.data.collector.mi/target/org.wso2.carbon.usage.data.collector.mi-1.0.0-SNAPSHOT.jar"
MI_HOME="/Users/dilanperera/Downloads/MicroIntegrators/wso2mi-1.2.0"
LIB_DIR="$MI_HOME/lib"
PLUGINS_DIR="$MI_HOME/wso2/components/plugins"
DROPINS_DIR="$MI_HOME/dropins"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "====================================="
echo "WSO2 MI Deployment Script"
echo "====================================="



# Build the common collector module
echo "Building common collector module..."
cd "$PROJECT_DIR/collectors/org.wso2.carbon.usage.data.collector.common"
mvn clean install

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Common Collector Maven build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Common Collector build completed successfully${NC}"

# Build the MI collector module with compilation enabled
echo "Building MI collector module..."
cd "$PROJECT_DIR/collectors/org.wso2.carbon.usage.data.collector.mi"
mvn clean install -Dskip.compilation=false

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: MI Collector Maven build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ MI Collector build completed successfully${NC}"

# Check if common collector JAR exists
if [ ! -f "$COMMON_COLLECTOR_JAR" ]; then
    echo -e "${RED}Error: Common Collector JAR not found at $COMMON_COLLECTOR_JAR${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found Common Collector JAR: $COMMON_COLLECTOR_JAR${NC}"

# Check if MI collector JAR exists
if [ ! -f "$MI_COLLECTOR_JAR" ]; then
    echo -e "${RED}Error: MI Collector JAR not found at $MI_COLLECTOR_JAR${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found MI Collector JAR: $MI_COLLECTOR_JAR${NC}"

# Check if MI directories exist
if [ ! -d "$LIB_DIR" ]; then
    echo -e "${RED}Error: MI lib directory not found at $LIB_DIR${NC}"
    exit 1
fi

if [ ! -d "$PLUGINS_DIR" ]; then
    echo -e "${RED}Error: MI plugins directory not found at $PLUGINS_DIR${NC}"
    exit 1
fi

if [ ! -d "$DROPINS_DIR" ]; then
    echo -e "${RED}Error: MI dropins directory not found at $DROPINS_DIR${NC}"
    exit 1
fi



# Remove old versions of our bundles from plugins directory
echo "Cleaning old collector bundles from plugins directory..."
rm -f "$PLUGINS_DIR/org.wso2.carbon.usage.data.collector.common"*.jar
rm -f "$PLUGINS_DIR/org.wso2.carbon.usage.data.collector.mi"*.jar
echo -e "${GREEN}✓ Old collector bundles removed from plugins directory${NC}"

# Remove contents of dropins directory
echo "Cleaning dropins directory..."
if [ "$(ls -A $DROPINS_DIR)" ]; then
    rm -rf "$DROPINS_DIR"/*
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Dropins directory cleaned successfully${NC}"
    else
        echo -e "${RED}Error: Failed to clean dropins directory${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}Dropins directory is already empty${NC}"
fi

# Copy common collector JAR to plugins directory (OSGi bundle)
echo "Copying Common Collector JAR to $PLUGINS_DIR..."
COMMON_COLLECTOR_FILENAME=$(basename "$COMMON_COLLECTOR_JAR" | sed 's/-SNAPSHOT//')
cp "$COMMON_COLLECTOR_JAR" "$PLUGINS_DIR/$COMMON_COLLECTOR_FILENAME"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Common Collector JAR copied successfully as $COMMON_COLLECTOR_FILENAME${NC}"
else
    echo -e "${RED}Error: Failed to copy Common Collector JAR${NC}"
    exit 1
fi

# Copy MI collector JAR to plugins directory
echo "Copying MI Collector JAR to $PLUGINS_DIR..."
MI_COLLECTOR_FILENAME=$(basename "$MI_COLLECTOR_JAR" | sed 's/-SNAPSHOT//')
cp "$MI_COLLECTOR_JAR" "$PLUGINS_DIR/$MI_COLLECTOR_FILENAME"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ MI Collector JAR copied successfully as $MI_COLLECTOR_FILENAME${NC}"
else
    echo -e "${RED}Error: Failed to copy MI Collector JAR${NC}"
    exit 1
fi

# Update bundles.info file to register the OSGi bundles
echo "Updating bundles.info..."
BUNDLES_INFO_FILE="$MI_HOME/wso2/components/micro-integrator-default/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info"

if [ ! -f "$BUNDLES_INFO_FILE" ]; then
    echo -e "${RED}Error: bundles.info file not found at $BUNDLES_INFO_FILE${NC}"
    exit 1
fi

# Remove old entries for our bundles (if any)
sed -i '' '/org.wso2.carbon.usage.data.collector.common/d' "$BUNDLES_INFO_FILE"
sed -i '' '/org.wso2.carbon.usage.data.collector.mi/d' "$BUNDLES_INFO_FILE"

# Add new entries for our bundles
echo "org.wso2.carbon.usage.data.collector.common,1.0.0.SNAPSHOT,../plugins/org.wso2.carbon.usage.data.collector.common-1.0.0.jar,4,true" >> "$BUNDLES_INFO_FILE"
echo "org.wso2.carbon.usage.data.collector.mi,1.0.0.SNAPSHOT,../plugins/org.wso2.carbon.usage.data.collector.mi-1.0.0.jar,4,true" >> "$BUNDLES_INFO_FILE"

echo -e "${GREEN}✓ bundles.info updated with collector bundle entries${NC}"

echo "====================================="
echo -e "${GREEN}Deployment Summary:${NC}"
echo -e "${GREEN}✓ Common Collector JAR deployed to wso2/components/plugins${NC}"
echo -e "${GREEN}✓ MI Collector JAR deployed to wso2/components/plugins${NC}"
echo -e "${GREEN}✓ Old bundles cleaned from plugins directory${NC}"
echo -e "${GREEN}✓ Dropins directory cleaned${NC}"
echo -e "${GREEN}✓ bundles.info updated${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo "====================================="
