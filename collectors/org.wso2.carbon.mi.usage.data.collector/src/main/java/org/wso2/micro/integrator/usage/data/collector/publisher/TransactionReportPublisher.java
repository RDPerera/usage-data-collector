/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.integrator.usage.data.collector.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.publisher.api.Publisher;
import org.wso2.carbon.usage.data.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.publisher.api.model.UsageData;
import org.wso2.micro.integrator.usage.data.collector.model.TransactionUsageData;
import org.wso2.micro.integrator.usage.data.collector.record.TransactionReport;

/**
 * Simplified Transaction Report Publisher that directly calls the publisher API.
 * No complex configuration - just creates a publisher instance and sends data.
 * 
 * @author - Dilan Perera
 * @version - 2.0.0
 */
public class TransactionReportPublisher {
    
    private static final Log LOG = LogFactory.getLog(TransactionReportPublisher.class);
    private final Publisher httpPublisher;
    
    /**
     * Constructor - creates the actual publisher instance directly
     */
    public TransactionReportPublisher() {
        // Directly instantiate the HTTP publisher - no OSGi complexity
        this.httpPublisher = new org.wso2.carbon.usage.data.publisher.impl.HttpPublisher();
        LOG.debug("TransactionReportPublisher initialized with HttpPublisher");
    }
    
    /**
     * Simplified method to publish a TransactionReport.
     * Converts to UsageData and sends directly to the HTTP publisher.
     * 
     * @param report The transaction report to publish
     * @return true if publishing was successful, false otherwise
     */
    public boolean publishTransaction(TransactionReport report) {
        try {
            TransactionUsageData usageData = new TransactionUsageData(report);
            
            // Log locally for debugging
            LOG.info("Publishing transaction report: " + 
                     "Transactions: " + report.getTotalCount() + 
                     ", Time Period: " + report.getHourStartTime());
            
            // Send directly to HTTP publisher
            httpPublisher.publish(usageData);
            LOG.debug("Transaction data sent to remote endpoint successfully");
            return true;
            
        } catch (PublisherException e) {
            LOG.error("Error while publishing transaction report", e);
            return false;
        }
    }
    
    /**
     * Shutdown method to clean up resources
     */
    public void shutdown() {
        if (httpPublisher != null) {
            httpPublisher.shutdown();
        }
        LOG.debug("TransactionReportPublisher shutdown completed");
    }
}