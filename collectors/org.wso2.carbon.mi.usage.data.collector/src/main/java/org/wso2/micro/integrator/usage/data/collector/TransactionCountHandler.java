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

package org.wso2.micro.integrator.usage.data.collector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.AbstractExtendedSynapseHandler;
import org.apache.synapse.MessageContext;
import org.wso2.micro.integrator.usage.data.collector.aggregator.TransactionAggregator;
import org.wso2.micro.integrator.usage.data.collector.publisher.TransactionReportPublisher;
import org.wso2.micro.integrator.usage.data.collector.record.TransactionReport;

/**
 * Simplified transaction counter handler that removes all configuration complexity.
 * Uses simple defaults and directly calls the publisher API via publishTransaction().
 * 
 * @author - Isuru Wijesiri, Dilan Perera
 * @version - 3.0.0
 */
public class TransactionCountHandler extends AbstractExtendedSynapseHandler {
    private static final Log LOG = LogFactory.getLog(TransactionCountHandler.class);
    private TransactionAggregator transactionAggregator;
    private TransactionReportPublisher publisher;
    private static boolean enabled = false;

    public TransactionCountHandler() {
        try {
            LOG.debug("Initializing Usage Data Collector");
            
            // Create publisher that directly calls the HTTP publisher
            this.publisher = new TransactionReportPublisher();
            
            // Initialize aggregator
            this.transactionAggregator = TransactionAggregator.getInstance();
            this.transactionAggregator.init(this.publisher);
            
            enabled = true;
            LOG.debug("Transaction Counter initialized successfully - will send data to analytics endpoint");
            
        } catch (Exception e) {
            LOG.error("Error while initializing Transaction Counter. Transaction counter will be disabled", e);
            enabled = false;
        }
    }

    @Override
    public boolean handleRequestInFlow(MessageContext messageContext) {
        if(!enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleRequestInFlow(messageContext);
        if(tCount > 0) {
            LOG.info("New transaction detected in RequestInFlow - Count: " + tCount);
            if(this.transactionAggregator != null && this.transactionAggregator.isEnabled()) {
                this.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleRequestOutFlow(MessageContext messageContext) {
        if(!enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleRequestOutFlow(messageContext);
        if(tCount > 0) {
            LOG.info("New transaction detected in RequestOutFlow - Count: " + tCount);
            if(this.transactionAggregator != null && this.transactionAggregator.isEnabled()) {
                this.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleResponseInFlow(MessageContext messageContext) {
        if(!enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleResponseInFlow(messageContext);
        if(tCount > 0) {
            LOG.info("New transaction detected in ResponseInFlow - Count: " + tCount);
            if(this.transactionAggregator != null && this.transactionAggregator.isEnabled()) {
                this.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleResponseOutFlow(MessageContext messageContext) {
        if(!enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleResponseOutFlow(messageContext);
        if(tCount > 0) {
            LOG.info("New transaction detected in ResponseOutFlow - Count: " + tCount);
            if(this.transactionAggregator != null && this.transactionAggregator.isEnabled()) {
                this.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleServerInit() {
        // Nothing to implement
        return true;
    }

    @Override
    public boolean handleServerShutDown() {
        LOG.debug("Shutting down Transaction Counter...");
        
        // Clean up resources
        if (transactionAggregator != null && transactionAggregator.isEnabled()) {
            transactionAggregator.shutdown();
        }
        if (publisher != null) {
            publisher.shutdown();
        }
        
        LOG.debug("Transaction Counter shutdown completed");
        return true;
    }

    @Override
    public boolean handleArtifactDeployment(String s, String s1, String s2) {
        // Nothing to implement
        return true;
    }

    @Override
    public boolean handleArtifactUnDeployment(String s, String s1, String s2) {
        // Nothing to implement
        return true;
    }

    @Override
    public boolean handleError(MessageContext messageContext) {
        // Nothing to implement
        return true;
    }
}
