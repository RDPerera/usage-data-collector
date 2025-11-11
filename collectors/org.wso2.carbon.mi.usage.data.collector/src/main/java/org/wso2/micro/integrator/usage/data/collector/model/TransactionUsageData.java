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

package org.wso2.micro.integrator.usage.data.collector.model;

import org.wso2.carbon.usage.data.publisher.api.model.UsageData;
import org.wso2.micro.integrator.usage.data.collector.record.TransactionReport;

/**
 * Usage data model for transaction reports.
 * Extends the base UsageData class to provide transaction-specific data structure.
 * 
 * @author - Dilan Perera
 * @version - 1.0.0
 */
public class TransactionUsageData extends UsageData {
    
    private String id;
    private long totalCount;
    private long hourStartTime;
    private long hourEndTime;
    private String recordedTime;
    
    /**
     * Create TransactionUsageData from a TransactionReport.
     * 
     * @param report The transaction report to convert
     */
    public TransactionUsageData(TransactionReport report) {
        this.id = report.getId();
        this.totalCount = report.getTotalCount();
        this.hourStartTime = report.getHourStartTime();
        this.hourEndTime = report.getHourEndTime();
        this.recordedTime = report.getRecordedTime();
        this.timestamp = report.getRecordedTime();
    }
    
    @Override
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":\"").append(escapeJson(id)).append("\",");
        json.append("\"totalCount\":").append(totalCount).append(",");
        json.append("\"hourStartTime\":").append(hourStartTime).append(",");
        json.append("\"hourEndTime\":").append(hourEndTime).append(",");
        json.append("\"recordedTime\":\"").append(escapeJson(recordedTime)).append("\",");
        json.append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\"");
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"").replace("\\", "\\\\");
    }
    
    // Getters
    public String getId() { return id; }
    public long getTotalCount() { return totalCount; }
    public long getHourStartTime() { return hourStartTime; }
    public long getHourEndTime() { return hourEndTime; }
    public String getRecordedTime() { return recordedTime; }
}