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

package org.wso2.carbon.usage.data.collector.identity.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.*;

import javax.sql.DataSource;

/**
 * Publisher implementation.
 */
public class PublisherImp implements Publisher {

    private static final Log LOG = LogFactory.getLog(PublisherImp.class);
    private static final String RECEIVER_ENDPOINT = "/usage/data/receiver";
    private static final String WSO2_ENDPOINT = "https://api.choreo.dev/test";

    @Override
    public DataSource getDataSource() {

        return IdentityDatabaseUtil.getDataSource();
    }

    @Override
    public ApiResponse callReceiverApi(ApiRequest request) throws PublisherException {

        String endpoint = getEndpoint(request);
        return new HTTPClient().executeApiRequest(request, endpoint, "receiver API");
    }

    @Override
    public ApiResponse callWso2Api(ApiRequest request) throws PublisherException {

        String endpoint = getWSO2Endpoint(request);
        return new HTTPClient().executeApiRequest(request, endpoint, "WSO2 API");
    }

    private static String getEndpoint(ApiRequest request) {

        String endpoint = getReceiverEndpoint();
        if (request.getData() instanceof UsageCount) {
            endpoint += "/usage-counts";
        } else if (request.getData() instanceof DeploymentInformation) {
            endpoint += "/deployment-information";
        } else if (request.getData() instanceof MetaInformation) {
            endpoint += "/meta-information";
        } else if (request.getEndpoint() != null && !request.getEndpoint().isEmpty()) {
            endpoint += "/" + request.getEndpoint();
        }
        return endpoint;
    }

    private static String getReceiverEndpoint() {

        try {
            return ServiceURLBuilder.create()
                    .addPath(RECEIVER_ENDPOINT)
                    .setTenant(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)
                    .build()
                    .getAbsoluteInternalURL();
        } catch (URLBuilderException e) {
           if(LOG.isDebugEnabled()) {
               LOG.debug(e.getMessage(), e);
           }
           return "";
        }
    }

    private static String getWSO2Endpoint(ApiRequest request) {

        String endpoint = WSO2_ENDPOINT;
        if (request.getEndpoint() != null && !request.getEndpoint().isEmpty()) {
            endpoint = request.getEndpoint();
        }
        return endpoint;
    }
}
