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

package org.wso2.micro.integrator.usage.data.collector.record;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a professional transaction report containing aggregated metrics for one hour.
 * This report includes transaction counts, time windows, and server metadata for analytics
 * and monitoring purposes.
 * 
 * @author - Dilan Perera
 * @version - 1.0.0
 */
public class TransactionReport {
    

    
    private final String id;
    private final long totalCount;
    private final long hourStartTime;
    private final long hourEndTime;
    private final String recordedTime;
    
    /**
     * Create a new hourly transaction report.
     * 
     * @param totalCount Total number of transactions in this hour
     * @param hourStartTime Start time of the hour window (milliseconds since epoch)
     * @param hourEndTime End time of the hour window (milliseconds since epoch)
     */
    public TransactionReport(long totalCount, long hourStartTime, long hourEndTime) {
        this.id = UUID.randomUUID().toString();
        this.totalCount = totalCount;
        this.hourStartTime = hourStartTime;
        this.hourEndTime = hourEndTime;
        this.recordedTime = new Date(hourEndTime).toString();
    }
    

    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public long getTotalCount() {
        return totalCount;
    }
    
    public long getHourStartTime() {
        return hourStartTime;
    }
    
    public long getHourEndTime() {
        return hourEndTime;
    }
    
    public String getRecordedTime() {
        return recordedTime;
    }
    
    /**
     * Get formatted start time for display.
     * 
     * @return Formatted start time string
     */
    public String getFormattedStartTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(hourStartTime));
    }
    
    /**
     * Get formatted end time for display.
     * 
     * @return Formatted end time string
     */
    public String getFormattedEndTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(hourEndTime));
    }
    
    @Override
    public String toString() {
        return "TransactionReport{" +
                "id='" + id + '\'' +
                ", totalCount=" + totalCount +
                ", hourWindow='" + getFormattedStartTime() + " to " + getFormattedEndTime() + '\'' +
                '}';
    }
}
