/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.usage.data.collector.common.util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.collector.common.collector.model.DeploymentData;
/**
 * Holder class that caches meta information in memory.
 * This cached data is used for both the initial /meta-information endpoint call
 * and for including in every subsequent payload.
 * 
 * Note: deploymentId and subscriptionKey are assigned by the receiver, not cached here.
 */
public class MetaInfoHolder {
    private static final Log log = LogFactory.getLog(MetaInfoHolder.class);
    private static volatile String nodeId;
    private static volatile String product;
    private static volatile boolean initialized = false;

    /**
     * Initializes and caches the meta information.
     * Should be called once at server startup.
     *
     * @param ipAddress The IP address of this node (sent as nodeId)
     * @param deploymentData The deployment data containing product version
     */
    public static synchronized void initialize(String ipAddress, DeploymentData deploymentData) {
        if (initialized) {
            return;
        }
        nodeId = ipAddress;
        product = deploymentData.getProductVersion();
        initialized = true;
    }

    /**
     * Gets the cached node ID (IP address).
     */
    public static String getNodeId() {
        return nodeId;
    }

    /**
     * Gets the cached product name and version.
     */
    public static String getProduct() {
        return product;
    }

    /**
     * Checks if meta information has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Clears the cached meta information (for testing purposes).
     */
    public static synchronized void reset() {
        nodeId = null;
        product = null;
        initialized = false;
    }
}
