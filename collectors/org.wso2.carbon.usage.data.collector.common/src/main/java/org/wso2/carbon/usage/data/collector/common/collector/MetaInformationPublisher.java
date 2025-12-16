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

package org.wso2.carbon.usage.data.collector.common.collector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.collector.common.collector.model.DeploymentData;
import org.wso2.carbon.usage.data.collector.common.internal.CommonUsageDataCollectorConstants;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.MetaInformation;
import org.wso2.carbon.usage.data.collector.common.util.MetaInfoHolder;
import org.wso2.carbon.usage.data.collector.common.util.UsageDataUtil;

/**
 * Publishes MetaInformation to the receiver at server startup.
 * Also initializes the MetaInfoHolder cache for use in all subsequent payloads.
 *
 * This dual approach ensures:
 * 1. Receiver gets MetaInformation early via /meta-information endpoint
 * 2. MetaInformation is cached for inclusion in every payload (redundancy)
 */
public class MetaInformationPublisher {

    private static final Log log = LogFactory.getLog(MetaInformationPublisher.class);

    private final Publisher publisher;
    private final DeploymentDataCollector deploymentDataCollector;

    public MetaInformationPublisher(Publisher publisher) {
        this.publisher = publisher;
        this.deploymentDataCollector = new DeploymentDataCollector(publisher);
    }

    /**
     * Publishes MetaInformation to the /meta-information endpoint at startup.
     * Also caches the meta information in MetaInfoHolder for use in all payloads.
     *
     * Note: This is a best-effort send. If it fails, meta info will still be
     * included in every subsequent payload, ensuring the receiver can create
     * the node record when it receives the first usage/deployment data.
     *
     * deploymentId and subscriptionKey are assigned by the receiver, not sent by collector.
     */
    public void publishAtStartup() {
        try {
            // Get node IP address
            String ipAddress = UsageDataUtil.getNodeIpAddress();

            // Collect deployment data to get product information
            DeploymentData deploymentData = deploymentDataCollector.collectDeploymentData();

            // Initialize the MetaInfoHolder cache (this will be used in all payloads)
            MetaInfoHolder.initialize(ipAddress, deploymentData);

            // Create MetaInformation payload using cached values
            MetaInformation metaInformation = new MetaInformation(
                    MetaInfoHolder.getNodeId(),
                    MetaInfoHolder.getProduct()
            );

            // Build API request
            ApiRequest request = new ApiRequest.Builder()
                    .withEndpoint(CommonUsageDataCollectorConstants.META_INFO_ENDPOINT)
                    .withData(metaInformation)
                    .build();

            // Publish to /meta-information endpoint using Publisher with built-in retry
            ApiResponse response = publisher.publishToReceiver(request);
        } catch (PublisherException e) {
            // Log error but don't fail - meta info will be in every payload anyway
            if(log.isDebugEnabled()) {
                log.warn("Failed to publish MetaInformation at startup after all retries. " +
                        "This is not critical as meta info will be included in every payload.", e);
            }
        } catch (Exception e) {
            // Log error but don't fail - meta info will be in every payload anyway
            if(log.isDebugEnabled()) {
                log.warn("Failed to publish MetaInformation at startup. " +
                        "This is not critical as meta info will be included in every payload.", e);
            }
        }
    }
}

