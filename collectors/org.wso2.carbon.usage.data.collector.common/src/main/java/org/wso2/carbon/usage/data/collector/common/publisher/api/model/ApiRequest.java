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

package org.wso2.carbon.usage.data.collector.common.publisher.api.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Request parameters for API calls made by DataSource implementations.
 */
public class ApiRequest {

    private final Object data;
    private final String endpoint;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String httpMethod;
    private final int timeoutMs;
    private final int retryCount;

    private ApiRequest(Builder builder) {
        this.data = builder.data;
        this.endpoint = builder.endpoint;
        this.headers = builder.headers;
        this.queryParams = builder.queryParams;
        this.httpMethod = builder.httpMethod;
        this.timeoutMs = builder.timeoutMs;
        this.retryCount = builder.retryCount;
    }

    public Object getData() {
        return data;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public static class Builder {
        private Object data;
        private String endpoint;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();
        private String httpMethod = "POST";
        private int timeoutMs = 10000;
        private int retryCount = 3;

        public Builder withData(Object data) {
            this.data = data;
            return this;
        }

        public Builder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder addHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder withHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder addQueryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public Builder withQueryParams(Map<String, String> queryParams) {
            this.queryParams.putAll(queryParams);
            return this;
        }

        public Builder withHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder withTimeout(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder withRetryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public ApiRequest build() {
            if (data == null) {
                throw new IllegalArgumentException("Request data cannot be null");
            }
            return new ApiRequest(this);
        }
    }
}

