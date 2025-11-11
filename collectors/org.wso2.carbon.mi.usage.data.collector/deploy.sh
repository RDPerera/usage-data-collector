#!/bin/bash

# Script to deploy usage data collector JARs to WSO2 MI
# This script builds and copies publisher JARs and collector JAR (if buildable) to the MI lib directory
# and cleans the dropins directory

# Define paths
PROJECT_DIR="/Users/dilanperera/Projects/usage-data-collector"
COLLECTOR_JAR="$PROJECT_DIR/collectors/org.wso2.carbon.mi.usage.data.collector/target/org.wso2.micro.integrator.usage.data.collector-1.0.0-SNAPSHOT.jar"
PUBLISHER_API_JAR="$PROJECT_DIR/publisher/org.wso2.carbon.usage.data.publisher.api/target/org.wso2.carbon.usage.data.publisher.api-1.0.0-SNAPSHOT.jar"
PUBLISHER_IMPL_JAR="$PROJECT_DIR/publisher/org.wso2.carbon.usage.data.publisher.impl/target/org.wso2.carbon.usage.data.publisher.impl-1.0.0-SNAPSHOT.jar"
MI_HOME="/Users/dilanperera/Downloads/MicroIntegrators/wso2mi-4.5.0"
LIB_DIR="$MI_HOME/lib"
DROPINS_DIR="$MI_HOME/dropins"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "====================================="
echo "WSO2 MI Deployment Script"
echo "====================================="

# Build the publisher modules first (they always build successfully)
echo "Building publisher modules with Maven..."
cd "$PROJECT_DIR/publisher"
mvn clean install

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Publisher Maven build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Publisher modules build completed successfully${NC}"

# Build the collector module (may fail due to Synapse dependencies)
echo "Building collector module with Maven..."
cd "$PROJECT_DIR/collectors"
mvn clean install
COLLECTOR_BUILD_STATUS=$?

if [ $COLLECTOR_BUILD_STATUS -ne 0 ]; then
    echo -e "${YELLOW}Warning: Collector Maven build failed (expected due to Synapse dependencies)${NC}"
    echo -e "${YELLOW}Will deploy publisher JARs only${NC}"
fi

# Check if publisher JAR files exist
if [ ! -f "$PUBLISHER_API_JAR" ]; then
    echo -e "${RED}Error: Publisher API JAR file not found at $PUBLISHER_API_JAR${NC}"
    exit 1
fi

if [ ! -f "$PUBLISHER_IMPL_JAR" ]; then
    echo -e "${RED}Error: Publisher Implementation JAR file not found at $PUBLISHER_IMPL_JAR${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found Publisher API JAR: $PUBLISHER_API_JAR${NC}"
echo -e "${GREEN}✓ Found Publisher Implementation JAR: $PUBLISHER_IMPL_JAR${NC}"

# Check if collector JAR exists (only if build was successful)
if [ $COLLECTOR_BUILD_STATUS -eq 0 ]; then
    if [ ! -f "$COLLECTOR_JAR" ]; then
        echo -e "${RED}Error: Collector JAR file not found at $COLLECTOR_JAR${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Found Collector JAR: $COLLECTOR_JAR${NC}"
else
    echo -e "${YELLOW}⚠ Collector JAR not available due to build failure${NC}"
fi

# Check if MI directories exist
if [ ! -d "$LIB_DIR" ]; then
    echo -e "${RED}Error: MI lib directory not found at $LIB_DIR${NC}"
    exit 1
fi

if [ ! -d "$DROPINS_DIR" ]; then
    echo -e "${RED}Error: MI dropins directory not found at $DROPINS_DIR${NC}"
    exit 1
fi

# Copy publisher JARs to lib directory
echo "Copying Publisher API JAR to $LIB_DIR..."
cp "$PUBLISHER_API_JAR" "$LIB_DIR/"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Publisher API JAR copied successfully${NC}"
else
    echo -e "${RED}Error: Failed to copy Publisher API JAR${NC}"
    exit 1
fi

echo "Copying Publisher Implementation JAR to $LIB_DIR..."
cp "$PUBLISHER_IMPL_JAR" "$LIB_DIR/"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Publisher Implementation JAR copied successfully${NC}"
else
    echo -e "${RED}Error: Failed to copy Publisher Implementation JAR${NC}"
    exit 1
fi

# Copy collector JAR to lib directory (only if available)
if [ $COLLECTOR_BUILD_STATUS -eq 0 ]; then
    echo "Copying Collector JAR to $LIB_DIR..."
    cp "$COLLECTOR_JAR" "$LIB_DIR/"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Collector JAR copied successfully${NC}"
    else
        echo -e "${RED}Error: Failed to copy Collector JAR${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠ Skipping Collector JAR deployment (not available)${NC}"
fi

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

echo "====================================="
echo -e "${GREEN}Deployment Summary:${NC}"
echo -e "${GREEN}✓ Publisher API JAR deployed${NC}"
echo -e "${GREEN}✓ Publisher Implementation JAR deployed${NC}"
if [ $COLLECTOR_BUILD_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ Collector JAR deployed${NC}"
else
    echo -e "${YELLOW}⚠ Collector JAR skipped (build issues)${NC}"
fi
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo "====================================="
