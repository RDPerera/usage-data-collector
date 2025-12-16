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

/**
 * Runnable task that executes the API count collection.
 * This task wraps the collector to provide proper exception handling
 * and prevent the scheduler from stopping on errors.
 */
public class ApiCountCollectorTask implements Runnable {

    private static final Log log = LogFactory.getLog(ApiCountCollectorTask.class);

    private final ApiCountCollector collector;

    /**
     * Constructor.
     *
     * @param collector The API count collector instance
     */
    public ApiCountCollectorTask(ApiCountCollector collector) {
        this.collector = collector;
    }

    @Override
    public void run() {
        try {
            collector.collectAndPublish();
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Error executing API count collection task", e);
            }
            // Don't propagate exception - let scheduler continue
        }
    }
}

