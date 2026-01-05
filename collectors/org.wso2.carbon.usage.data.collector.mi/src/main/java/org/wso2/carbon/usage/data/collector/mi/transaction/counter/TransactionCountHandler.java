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

package org.wso2.carbon.usage.data.collector.mi.transaction.counter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.AbstractExtendedSynapseHandler;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.usage.data.collector.mi.transaction.aggregator.TransactionAggregator;
import org.wso2.carbon.usage.data.collector.mi.transaction.publisher.TransactionPublisher;

public class TransactionCountHandler extends AbstractExtendedSynapseHandler {
    private static final Log log = LogFactory.getLog(TransactionCountHandler.class);
    private TransactionAggregator transactionAggregator;
    private TransactionPublisher publisher;
    private volatile boolean enabled = false;
    private static TransactionCountHandler instance;
    
    public static synchronized void registerTransactionPublisher(TransactionPublisher reporter) {
        if (instance == null) {
            instance = new TransactionCountHandler();
        }
        instance.publisher = reporter;
        if (instance.transactionAggregator == null) {
            instance.transactionAggregator = TransactionAggregator.getInstance();
        }
        synchronized (instance.transactionAggregator) {
            if (!instance.transactionAggregator.isEnabled()) {
                instance.transactionAggregator.init(reporter);
            }
        }
        instance.enabled = true;
    }
    
    public static synchronized void unregisterTransactionPublisher(TransactionPublisher reporter) {
        if (instance != null && instance.publisher == reporter) {
            instance.publisher = null;
        }
    }

    public TransactionCountHandler() {
        try {
            if (instance == null || instance.transactionAggregator == null) {
                instance = this;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("TransactionCountHandler: Error in constructor", e);
            }
            this.enabled = false;
        }
    }

    @Override
    public boolean handleServerInit() {
        // Nothing to implement
        return true;
    }

    @Override
    public boolean handleRequestInFlow(MessageContext messageContext) {
        if (instance == null || !instance.enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleRequestInFlow(messageContext);
        if(tCount > 0) {
            if (instance.transactionAggregator != null && instance.transactionAggregator.isEnabled()) {
                instance.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleServerShutDown() {
        if (log.isDebugEnabled()) {
            log.debug("Shutting down Transaction Counter...");
        }
        // Clean up resources
        if (transactionAggregator != null && transactionAggregator.isEnabled()) {
            transactionAggregator.shutdown();
        }
        if (log.isDebugEnabled()) {
            log.debug("Transaction Counter shutdown completed");
        }
        return true;
    }

    @Override
    public boolean handleRequestOutFlow(MessageContext messageContext) {
        if (instance == null || !instance.enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleRequestOutFlow(messageContext);
        if(tCount > 0) {
            if (instance.transactionAggregator != null && instance.transactionAggregator.isEnabled()) {
                instance.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleResponseInFlow(MessageContext messageContext) {
        if (instance == null || !instance.enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleResponseInFlow(messageContext);
        if(tCount > 0) {
            if (instance.transactionAggregator != null && instance.transactionAggregator.isEnabled()) {
                instance.transactionAggregator.addTransactions(tCount);
            }
        }
        return true;
    }

    @Override
    public boolean handleResponseOutFlow(MessageContext messageContext) {
        if (instance == null || !instance.enabled) {
            return true;
        }
        int tCount = TransactionCountingLogic.handleResponseOutFlow(messageContext);
        if(tCount > 0) {
            if (instance.transactionAggregator != null && instance.transactionAggregator.isEnabled()) {
                instance.transactionAggregator.addTransactions(tCount);
            }
        }
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
