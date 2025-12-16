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

package org.wso2.carbon.usage.data.collector.apim.publisher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dto.EventHubConfigurationDto;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * APIM-specific implementation of Publisher interface. Handles APIM database access and API calls using HTTP protocol.
 * Automatically registered as an OSGi service.
 *
 * Features: - Uses APIUtil.getHttpClient() for consistency with APIM - SSL and timeout configuration inherited from
 * APIM settings - Non-blocking error handling (returns failure responses)
 */
@Component(name = "org.wso2.carbon.usage.data.collector.apim.publisher", service = Publisher.class, immediate = true)
public class ApimPublisher implements Publisher {

    private static final Log log = LogFactory.getLog(ApimPublisher.class);

    // APIM-specific JNDI DataSource name
    private static final String APIM_DATASOURCE_NAME = "jdbc/WSO2AM_DB";

    // Default receiver base URL (fallback when configuration is not available)
    private static final String DEFAULT_RECEIVER_BASE_URL = "https://localhost:9443";

    // Timeout configuration (in milliseconds)
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;  // 5 seconds
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 10000;  // 10 seconds

    private volatile DataSource dataSource;

    @Override
    public DataSource getDataSource() throws PublisherException {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    try {
                        InitialContext ctx = new InitialContext();
                        dataSource = (DataSource) ctx.lookup(APIM_DATASOURCE_NAME);

                        if (dataSource == null) {
                            throw new PublisherException("DataSource is null for JNDI name: " + APIM_DATASOURCE_NAME);
                        }
                    } catch (NamingException e) {
                        String errorMsg = "Failed to lookup APIM DataSource from JNDI: " + APIM_DATASOURCE_NAME;
                        if (log.isDebugEnabled()) {
                            log.error(errorMsg, e);
                        }
                        throw new PublisherException(errorMsg, e);
                    }
                }
            }
        }
        return dataSource;
    }

    @Override
    public ApiResponse callReceiverApi(ApiRequest request) throws PublisherException {
        try {
            String receiverBaseUrl = getReceiverBaseUrl();
            String fullUrl = buildFullUrl(receiverBaseUrl, request.getEndpoint());
            return sendHttpRequest(fullUrl, request);
        } catch (Exception e) {
            String errorMsg = "Receiver API call failed: " + e.getMessage();
            if (log.isDebugEnabled()) {
                log.error(errorMsg, e);
            }
            // Return failure response instead of throwing exception
            // This allows retry logic in Publisher interface to handle it
            return ApiResponse.failure(500, errorMsg);
        }
    }

    /**
     * Gets the receiver base URL from EventHubConfigurationDto. Falls back to default URL if configuration is not
     * available. Package-private for testing.
     *
     * @return Receiver base URL from configuration or default
     */
    String getReceiverBaseUrl() {
        try {
            // Get ServiceReferenceHolder instance
            ServiceReferenceHolder serviceReferenceHolder = ServiceReferenceHolder.getInstance();
            if (serviceReferenceHolder != null) {
                // Get APIManagerConfiguration
                APIManagerConfiguration apiManagerConfig = serviceReferenceHolder.getAPIManagerConfigurationService()
                        .getAPIManagerConfiguration();
                if (apiManagerConfig != null) {
                    // Get EventHubConfigurationDto
                    EventHubConfigurationDto eventHubConfig = apiManagerConfig.getEventHubConfigurationDto();
                    if (eventHubConfig != null) {
                        // Get service URL
                        String serviceUrl = eventHubConfig.getServiceUrl();
                        if (serviceUrl != null && !serviceUrl.trim().isEmpty()) {
                            return serviceUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error(
                        "Could not retrieve receiver URL from EventHubConfiguration, using default: "
                                + DEFAULT_RECEIVER_BASE_URL, e);
            }
        }
        return DEFAULT_RECEIVER_BASE_URL;
    }

    @Override
    public ApiResponse callExternalApi(ApiRequest request) throws PublisherException {
        try {
            return sendHttpRequest(request.getEndpoint(), request);
        } catch (IOException e) {
            String errorMsg = "WSO2 API call failed: " + e.getMessage();
            if (log.isDebugEnabled()) {
                log.error(errorMsg, e);
            }
            // Return failure response instead of throwing exception
            return ApiResponse.failure(500, errorMsg);
        }
    }

    /**
     * Build full URL from base URL and endpoint.
     */
    private String buildFullUrl(String baseUrl, String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return baseUrl;
        }
        // Ensure single slash between base URL and endpoint
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl + endpoint.substring(1);
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        } else {
            return baseUrl + endpoint;
        }
    }

    /**
     * Sends HTTP POST request to the specified URL. Uses APIUtil.getHttpClient() for consistency with APIM codebase.
     */
    private ApiResponse sendHttpRequest(String url, ApiRequest request) throws IOException, PublisherException {
        HttpPost httpPost = getHttpPost(url, request);

        // Set request body based on Content-Type
        if (request.getData() != null) {
            setRequestEntity(httpPost, request);
        }

        // Use APIUtil.getHttpClient() - consistent with APIM codebase
        // Cast to CloseableHttpClient for proper resource management
        CloseableHttpClient httpClient;
        try {
            httpClient = (CloseableHttpClient) APIUtil.getHttpClient(url);
        } catch (APIManagementException e) {
            throw new PublisherException("Failed to get HTTP client from APIUtil", e);
        }

        // Execute request with automatic resource cleanup using try-with-resources
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = httpResponse.getEntity() != null ?
                    EntityUtils.toString(httpResponse.getEntity()) :
                    "";

            // Create ApiResponse based on status code
            ApiResponse response;
            if (statusCode >= 200 && statusCode < 300) {
                response = ApiResponse.success(statusCode, responseBody);
            } else {
                response = ApiResponse.failure(statusCode, responseBody);
            }

            // Copy response headers to ApiResponse
            if (httpResponse.getAllHeaders() != null) {
                for (org.apache.http.Header header : httpResponse.getAllHeaders()) {
                    response.addHeader(header.getName(), header.getValue());
                }
            }

            return response;
        }
    }

    /**
     * Sets the request entity based on the Content-Type header.
     * Supports application/json and application/x-www-form-urlencoded.
     * Handles UsageData subclasses (DeploymentInformation, MetaInformation, UsageCount) and Map objects.
     *
     * @param httpPost HttpPost request to set the entity on
     * @param request  ApiRequest containing the data and headers
     * @throws UnsupportedEncodingException if encoding fails
     */
    private void setRequestEntity(HttpPost httpPost, ApiRequest request) throws UnsupportedEncodingException {
        String contentType = getContentType(request);
        Object data = request.getData();

        Gson gson = new GsonBuilder().create();

        if (contentType.contains("application/json")) {
            // Create JSON entity - Gson handles all object types
            String jsonPayload = gson.toJson(data);
            httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
        } else if (contentType.contains("application/x-www-form-urlencoded")) {
            // Create URL-encoded form entity
            Map<String, Object> dataFields = (data instanceof Map)
                    ? (Map<String, Object>) data
                    : gson.fromJson(gson.toJson(data), Map.class);

            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : dataFields.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } else {
            // Default to JSON if content type is not recognized
            String jsonPayload = gson.toJson(data);
            httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
        }
    }

    /**
     * Extracts the Content-Type from the request headers. Defaults to "application/json" if not specified.
     *
     * @param request ApiRequest containing headers
     * @return Content-Type value
     */
    private String getContentType(ApiRequest request) {
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                if ("Content-Type".equalsIgnoreCase(header.getKey())) {
                    return header.getValue();
                }
            }
        }
        return "application/json"; // Default to JSON
    }

    @NotNull
    private static HttpPost getHttpPost(String url, ApiRequest request) {
        HttpPost httpPost = new HttpPost(url);

        // Set custom headers from request
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                httpPost.setHeader(header.getKey(), header.getValue());
            }
        }
        return httpPost;
    }
}

