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

package org.wso2.carbon.usage.data.collector.mi.transaction.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.util.MetaInfoHolder;
import org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport;

/**
 * Transaction Report Publisher implementation.
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.mi.transaction.reporter",
    service = TransactionPublisher.class,
    immediate = true
)
public class TransactionPublisherImpl implements TransactionPublisher {

    private static final Log log = LogFactory.getLog(TransactionPublisherImpl.class);

    private volatile Publisher publisher;
    private volatile boolean reportingActive = false;
    private org.wso2.carbon.usage.data.collector.mi.transaction.aggregator.TransactionAggregator aggregator;

    @Activate
    protected void activate() {
    org.wso2.carbon.usage.data.collector.mi.transaction.counter.TransactionCountHandler.registerTransactionPublisher(
        this);
    }

    @Deactivate
    protected void deactivate() {
        org.wso2.carbon.usage.data.collector.mi.transaction.counter.TransactionCountHandler.unregisterTransactionPublisher(
                this);
    }

    private org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest createApiRequestFromReport(TransactionReport report) {
        TransactionUsageData usageData = new TransactionUsageData();
        usageData.setNodeId(MetaInfoHolder.getNodeId());
        usageData.setProduct(MetaInfoHolder.getProduct());
        usageData.setCount(report.getTotalCount());
        usageData.setType("TRANSACTION_COUNT");
        usageData.setCreatedTime(report.getCreatedTime());

        return new org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest.Builder()
                .withEndpoint("deployment-usage-stats")
                .withData(usageData)
                .build();
    }

    private static class TransactionUsageData extends org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageData {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private String nodeId;
        private String product;
        private long count;
        private String type;

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
        public void setProduct(String product) {
            this.product = product;
        }
        public void setCount(long count) {
            this.count = count;
        }
        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toJson() {
            try {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("nodeId", nodeId);
                map.put("product", product);
                map.put("count", count);
                map.put("type", type);
                map.put("createdTime", createdTime);
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize TransactionUsageData to JSON", e);
            }
        }
    }

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetPublisher"
    )
    protected void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    protected void unsetPublisher(Publisher publisher) {
        this.publisher = null;
    }

    @Override
    public void startReporting() {
        if (publisher == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot start reporting - Publisher service not available");
            }
            return;
        }
        aggregator = org.wso2.carbon.usage.data.collector.mi.transaction.aggregator.TransactionAggregator.getInstance();
        if (aggregator == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot start reporting - TransactionAggregator not available");
            }
            return;
        }
        aggregator.init(this);
        reportingActive = true;
        if (log.isDebugEnabled()) {
            log.debug("Transaction reporting started and aggregator scheduled.");
        }
    }

    @Override
    public void stopReporting() {
        if (aggregator != null && aggregator.isEnabled()) {
            aggregator.shutdown();
            if (log.isDebugEnabled()) {
                log.debug("TransactionAggregator schedule stopped.");
            }
        }
        reportingActive = false;
        if (log.isDebugEnabled()) {
            log.debug("Transaction reporting stopped.");
        }
    }

    @Override
    public boolean publishTransaction(TransactionReport report) {
        return publishTransactionReport(report);
    }

    private boolean publishTransactionReport(TransactionReport report) {
        Publisher currentPublisher;
        synchronized (this) {
            currentPublisher = this.publisher;
        }
        if (currentPublisher == null) {
            if (log.isDebugEnabled()) {
                log.debug("TransactionReportPublisher: Cannot publish - Publisher service not available via OSGi");
            }
            return false;
        }

        // Check if MetaInfoHolder is initialized before publishing
        if (!MetaInfoHolder.isInitialized()) {
            if (log.isDebugEnabled()) {
                log.debug("TransactionReportPublisher: Cannot publish - MetaInfoHolder not yet initialized. " +
                        "Skipping this report cycle, transaction count will be published in the next cycle.");
            }
            return false;
        }

        try {
            org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest request = 
                createApiRequestFromReport(report);
            org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse response =
                    currentPublisher.callReceiverApi(request);
            if (response != null && response.isSuccess()) {
                return true;
            } else {
                int status = response != null ? response.getStatusCode() : -1;
                String body = response != null ? response.getResponseBody() : "null";
                if (log.isDebugEnabled()) {
                    log.debug("TransactionReportPublisher: Failed to publish transaction report. Status: " + status + ", Body: " + body);
                }
                return false;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("TransactionReportPublisher: Error while publishing transaction report via OSGi service", e);
            }
            return false;
        }
    }

    @Override
    public boolean reportNow() {
        if (publisher == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot report - Publisher service not available");
            }
            return false;
        }
        if (aggregator == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot report - TransactionAggregator not available");
            }
            return false;
        }
        try {
            long count = aggregator.getAndResetCurrentHourlyCount();
            org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport report =
                new org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport(count);
            boolean success = publishTransactionReport(report);
            if (success) {
                if (log.isDebugEnabled()) {
                    log.debug("Immediate transaction report published successfully.");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to publish immediate transaction report.");
                }
            }
            return success;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while reporting transaction data", e);
            }
            return false;
        }
    }

    @Override
    public boolean isReportingActive() {
        return reportingActive && publisher != null;
    }
}
