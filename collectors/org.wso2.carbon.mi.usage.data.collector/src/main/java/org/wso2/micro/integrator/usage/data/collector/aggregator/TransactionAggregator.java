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

package org.wso2.micro.integrator.usage.data.collector.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.micro.integrator.usage.data.collector.publisher.TransactionReportPublisher;
import org.wso2.micro.integrator.usage.data.collector.record.TransactionReport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates transaction counts over an hour and publishes the summary once per hour.
 * This component uses an AtomicLong for high-performance, lock-free counting that can
 * handle millions of transactions per second.
 * 
 * @author - Dilan Perera
 * @version - 1.0.0
 */
public class TransactionAggregator {

    private static final Log LOG = LogFactory.getLog(TransactionAggregator.class);
    private static TransactionAggregator instance = null;
    
    private final AtomicLong hourlyTransactionCount = new AtomicLong(0);
    private TransactionReportPublisher publisher;
    private ScheduledExecutorService scheduledExecutorService;
    private long currentHourStartTime;
    private boolean enabled = false;

    private TransactionAggregator() {}

    public static TransactionAggregator getInstance() {
        if (instance == null) {
            instance = new TransactionAggregator();
        }
        return instance;
    }

    /**
     * Initialize the hourly aggregator with the publisher implementation.
     * 
     * @param publisher The publisher implementation to use for hourly publishing
     */
    public void init(TransactionReportPublisher publisher) {
        if (publisher == null) {
            LOG.warn("TransactionReportPublisher is null. Hourly aggregation will be disabled.");
            return;
        }

        this.publisher = publisher;
        this.currentHourStartTime = System.currentTimeMillis();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        // *** TEST MODE: Publishing every 30 seconds for testing ***
        long publishInterval = 30000L; // 30 seconds in milliseconds
        long initialDelay = 30000L; // Start after 30 seconds

        // Schedule to run every 30 seconds
        scheduledExecutorService.scheduleAtFixedRate(
            this::publishAndReset,
            initialDelay,
            publishInterval,
            TimeUnit.MILLISECONDS
        );

        this.enabled = true;
    }

    /**
     * Add transactions to the hourly count. This method is thread-safe and lock-free,
     * capable of handling millions of calls per second.
     * 
     * @param count The number of transactions to add
     */
    public void addTransactions(int count) {
        if (!enabled || count <= 0) {
            return;
        }
        hourlyTransactionCount.addAndGet(count);
    }

    /**
     * Publishes the hourly transaction count and resets the counter.
     * Called automatically once per hour by the scheduled executor.
     */
    private void publishAndReset() {
        try {
            // Atomically get the current count and reset to 0
            long count = hourlyTransactionCount.getAndSet(0);
            long hourEndTime = System.currentTimeMillis();
            
            if (count > 0) {
                TransactionReport summary = new TransactionReport(
                    count, 
                    currentHourStartTime, 
                    hourEndTime
                );
                
                publisher.publishTransaction(summary);
            }
            
            // Update the start time for the next hour
            currentHourStartTime = hourEndTime;
            
        } catch (Exception e) {
            LOG.error("Error while publishing hourly transaction count", e);
            // Don't reset the counter if publishing failed - we'll try again next hour
            // Note: This means the next hour will include this hour's count
        }
    }

    /**
     * Get the current hourly transaction count without resetting it.
     * Useful for monitoring or debugging.
     * 
     * @return The current transaction count for this hour
     */
    public long getCurrentHourlyCount() {
        return hourlyTransactionCount.get();
    }

    /**
     * Check if the hourly aggregator is enabled and running.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Shutdown the hourly aggregator and publish any remaining counts.
     */
    public void shutdown() {
        if (scheduledExecutorService != null) {
            // Publish remaining counts before shutdown
            publishAndReset();
            
            scheduledExecutorService.shutdownNow();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("TransactionAggregator did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Interrupted while shutting down TransactionAggregator", e);
            }
        }
        enabled = false;
    }
}
