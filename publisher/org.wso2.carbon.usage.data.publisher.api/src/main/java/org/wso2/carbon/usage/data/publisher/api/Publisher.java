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

package org.wso2.carbon.usage.data.publisher.api;

import org.wso2.carbon.usage.data.publisher.api.model.UsageData;

/**
 * Interface for publishing usage data to a remote receiver.
 */
public interface Publisher {

    /**
     * Publishes usage data to the configured receiver endpoint.
     *
     * @param data The usage data to publish
     * @throws PublisherException If publishing fails
     */
    void publish(UsageData data) throws PublisherException;

    /**
     * Shuts down the publisher and releases resources.
     */
    void shutdown();
}

