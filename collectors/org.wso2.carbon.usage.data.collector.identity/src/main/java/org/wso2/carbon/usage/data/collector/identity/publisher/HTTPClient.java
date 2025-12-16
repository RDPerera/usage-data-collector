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

package org.wso2.carbon.usage.data.collector.identity.publisher;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageCount;
import org.wso2.carbon.usage.data.collector.common.util.UsageDataUtil;
import org.wso2.carbon.usage.data.collector.identity.util.AppCredentialsUtil;
import org.wso2.carbon.utils.httpclient5.HTTPClientUtils;

import java.nio.charset.StandardCharsets;

import static org.wso2.carbon.usage.data.collector.identity.util.UsageCollectorConstants.PRODUCT;

/**
 *  HTTP Client related class.
 */
public class HTTPClient {

    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final Log LOG = LogFactory.getLog(HTTPClient.class);
    private static final CloseableHttpClient httpClient;

    static {
        httpClient = HTTPClientUtils.createClientWithCustomHostnameVerifier().build();
    }

    public ApiResponse executeApiRequest(ApiRequest request, String endpoint, String endpointLabel) {

        int timeout = request.getTimeoutMs() > 0 ? request.getTimeoutMs() : DEFAULT_TIMEOUT_MS;
        try {
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setConfig(buildRequestConfig(timeout));
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setEntity(new StringEntity(request.getData().toJson(), StandardCharsets.UTF_8));
            setAuthorizationHeader(httpPost);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = response.getEntity() != null ?
                        EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";

                if (statusCode >= 200 && statusCode < 300) {
                    return ApiResponse.success(statusCode, responseBody);
                } else {
                    return ApiResponse.failure(statusCode, "HTTP error: " + statusCode);
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to call " + endpointLabel + " at " + endpoint, e);
            }
            return ApiResponse.failure(500, e.getMessage());
        }
    }

    /**
     * Creates an ApiRequest for usage data with full customization.
     *
     * @param count The count value
     * @param type  The type of usage data
     * @return ApiRequest object
     */
    public static ApiRequest createUsageDataRequest(int count, String type) {

        String nodeId = UsageDataUtil.getNodeIpAddress();
        UsageCount data = new UsageCount(nodeId, PRODUCT, count, type);
        return new ApiRequest.Builder()
                .withEndpoint("usage-counts")
                .withData(data)
                .build();
    }

    private RequestConfig buildRequestConfig(int timeoutMs) {

        Timeout timeout = Timeout.ofMilliseconds(timeoutMs);
        return RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setResponseTimeout(timeout)
                .build();
    }

    private void setAuthorizationHeader(HttpUriRequestBase httpMethod) {

        AppCredentialsUtil credentialsUtil = AppCredentialsUtil.getInstance();

        if (!credentialsUtil.hasCredentials()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No authorization credentials configured, skipping auth header");
            }
            return;
        }
        String appName = credentialsUtil.getAppName();
        char[] appPasswordChars = credentialsUtil.getAppPassword();
        String toEncode = appName + ":" + new String(appPasswordChars);
        byte[] encoding = Base64.encodeBase64(toEncode.getBytes());
        String authHeader = new String(encoding, StandardCharsets.UTF_8);
        String CLIENT = "Client ";
        httpMethod.addHeader(HTTPConstants.HEADER_AUTHORIZATION, CLIENT + authHeader);
    }
}
