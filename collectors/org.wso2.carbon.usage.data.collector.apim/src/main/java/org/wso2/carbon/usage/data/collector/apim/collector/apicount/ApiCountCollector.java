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

package org.wso2.carbon.usage.data.collector.apim.collector.apicount;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageCount;
import org.wso2.carbon.usage.data.collector.common.util.MetaInfoHolder;
import org.wso2.carbon.usage.data.collector.apim.internal.ApimUsageDataCollectorConstants;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * API Count Collector for APIM.
 * Collects API counts from the AM_API table and publishes them.
 *
 * This collector is managed by ApimUsageDataCollectorServiceComponent which
 * handles scheduler creation and lifecycle management.
 */
public class ApiCountCollector {

    private static final Log log = LogFactory.getLog(ApiCountCollector.class);

    private final Publisher publisher;

    /**
     * Constructor.
     *
     * @param publisher The Publisher instance for database access and publishing
     */
    public ApiCountCollector(Publisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Collects the API count from database and publishes it.
     * Package-private to allow access from ApiCountCollectorTask.
     */
    void collectAndPublish() {
        if (publisher == null) {
            if(log.isDebugEnabled()) {
                log.warn("Cannot collect API count - Publisher service not available");
            }
            return;
        }

        // Query and publish non-MCP API count independently
        try {
            long apiCount = queryApiCount();
            publishApiCount(apiCount, ApimUsageDataCollectorConstants.API_COUNT_TYPE);
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Error collecting and publishing API count (non-MCP)", e);
            }
        }

        // Query and publish MCP API count independently
        try {
            long mcpApiCount = queryMcpApiCount();
            publishApiCount(mcpApiCount, ApimUsageDataCollectorConstants.MCP_API_COUNT_TYPE);
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Error collecting and publishing MCP API count", e);
            }
        }
    }

    /**
     * Queries the database to get the non-MCP API count.
     *
     * @return The number of non-MCP APIs in the AM_API table
     * @throws PublisherException If database query fails
     */
    private long queryApiCount() throws PublisherException {
        DataSource dataSource = publisher.getDataSource();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(ApimUsageDataCollectorConstants.API_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                long count = resultSet.getLong("api_count");
                return count;
            } else {
                return 0;
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to query non-MCP API count from database";
            if(log.isDebugEnabled()) {
                log.error(errorMsg, e);
            }
            throw new PublisherException(errorMsg, e);
        }
    }

    /**
     * Queries the database to get the MCP API count.
     *
     * @return The number of MCP APIs in the AM_API table
     * @throws PublisherException If database query fails
     */
    private long queryMcpApiCount() throws PublisherException {
        DataSource dataSource = publisher.getDataSource();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(ApimUsageDataCollectorConstants.MCP_API_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                long count = resultSet.getLong("mcp_api_count");
                return count;
            } else {
                return 0;
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to query MCP API count from database";
            if(log.isDebugEnabled()) {
                log.error(errorMsg, e);
            }
            throw new PublisherException(errorMsg, e);
        }
    }

    /**
     * Publishes the API count to the receiver endpoint using Publisher.publishToReceiver().
     * Retry logic is handled automatically by the Publisher interface.
     *
     * @param apiCount The number of APIs
     * @param type The type of count (API_COUNT or MCP_API_COUNT)
     */
    private void publishApiCount(long apiCount, String type) {
        if (publisher == null) {
            if(log.isDebugEnabled()) {
                log.warn("Cannot publish " + type + " - Publisher not available");
            }
            return;
        }

        try {
            String nodeId = MetaInfoHolder.getNodeId();
            String product = MetaInfoHolder.getProduct();
            UsageCount usageCount = new UsageCount(nodeId, product, apiCount, type);

            ApiRequest request = new ApiRequest.Builder()
                    .withEndpoint(ApimUsageDataCollectorConstants.USAGE_COUNT_ENDPOINT)
                    .withData(usageCount)
                    .build();

            // Publisher.publishToReceiver() handles retry logic automatically
            ApiResponse response = publisher.publishToReceiver(request);

            if (log.isDebugEnabled()) {
                log.debug("Successfully published " + type + ": " + apiCount + ", status: " + response.getStatusCode());
            }
        } catch (PublisherException e) {
            if(log.isDebugEnabled()) {
                log.error("Failed to publish " + type + " after all retries: " + e.getMessage(), e);
            }
        }
    }
}

