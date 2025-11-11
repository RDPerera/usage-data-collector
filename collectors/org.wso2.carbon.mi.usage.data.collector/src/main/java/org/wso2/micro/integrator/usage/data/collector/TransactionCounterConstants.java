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

public class TransactionCounterConstants {

    public static enum ServerType {
        GATEWAY, MI
    }

    public static final String IS_THERE_ASSOCIATED_INCOMING_REQUEST = "is_there_incoming_request";
    public static final String TRANSPORT_WS = "ws";
    public static final String TRANSPORT_WSS = "wss";

    public static final String SERVER_ID = "serverId";

    // APIM Gateway related constants
    public static final String APIM_CONFIG_CLASS = "org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder";
    public static final String GATEWAY_CONFIG_ROOT = "APIGateway.TransactionCounter";
    public static final String GATEWAY_SERVER_ID = GATEWAY_CONFIG_ROOT + ".ServerID";

    // MI related constants
    public static final String MI_CONFIG_CLASS = "org.wso2.config.mapper.ConfigParser";
    public static final String MI_CONFIG_ROOT = "integration.transaction_counter";
    public static final String MI_SERVER_ID = MI_CONFIG_ROOT + ".server_id";

    // Default values
    public static final String DEFAULT_SERVER_ID = "default";
}
