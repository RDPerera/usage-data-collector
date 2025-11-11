/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.usage.data.publisher.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.usage.data.publisher.api.Publisher;
import org.wso2.carbon.usage.data.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.publisher.api.model.UsageData;

import java.io.IOException;

/**
 * HTTP implementation of the Publisher interface.
 * Publishes usage data to a remote receiver via HTTP POST.
 * Automatically registered as an OSGi service.
 */
@Component(
    name = "http.publisher.component",
    immediate = true,
    service = Publisher.class
)
public class HttpPublisher implements Publisher {

    private static final Log log = LogFactory.getLog(HttpPublisher.class);

    // Hardcoded configuration - using local test server
    private static final String RECEIVER_URL = "http://localhost:8080/receiver";
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    private static final int RETRY_COUNT = 3;
    private static final String CONTENT_TYPE = "application/json";

    private CloseableHttpClient httpClient;

    /**
     * Constructor - initialize HTTP client for direct instantiation
     */
    public HttpPublisher() {
        initializeHttpClient();
    }

    @Activate
    protected void activate(ComponentContext context) {
        if (httpClient == null) {
            initializeHttpClient();
        }
        LOG.debug("HttpPublisher activated and registered as OSGi service");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        shutdown();
        LOG.debug("HttpPublisher deactivated");
    }

    private void initializeHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        LOG.debug("HTTP Publisher initialized with receiver URL: " + RECEIVER_URL);
    }

    @Override
    public void publish(UsageData data) throws PublisherException {
        if (data == null) {
            throw new PublisherException("Usage data cannot be null");
        }

        String jsonPayload = data.toJson();
        log.debug("Publishing usage data: " + jsonPayload);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < RETRY_COUNT) {
            attempt++;
            try {
                sendHttpPost(jsonPayload);
                log.info("Successfully published usage data (attempt " + attempt + ")");
                return;
            } catch (IOException e) {
                lastException = e;
                log.warn("Failed to publish usage data (attempt " + attempt + "/" + RETRY_COUNT + "): "
                        + e.getMessage());

                if (attempt < RETRY_COUNT) {
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PublisherException("Publish interrupted", ie);
                    }
                }
            }
        }

        throw new PublisherException("Failed to publish usage data after " + RETRY_COUNT + " attempts",
                lastException);
    }

    private void sendHttpPost(String jsonPayload) throws IOException {
        if (httpClient == null) {
            throw new IOException("HTTP client is not initialized");
        }
        
        HttpPost httpPost = new HttpPost(RECEIVER_URL);
        httpPost.setHeader("Content-Type", CONTENT_TYPE);
        httpPost.setEntity(new StringEntity(jsonPayload));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP request failed with status code: " + statusCode);
            }
        }
    }

    @Override
    public void shutdown() {
        if (httpClient != null) {
            try {
                httpClient.close();
                LOG.debug("HTTP Publisher shutdown successfully");
            } catch (IOException e) {
                log.error("Error shutting down HTTP Publisher", e);
            }
        }
    }
}

