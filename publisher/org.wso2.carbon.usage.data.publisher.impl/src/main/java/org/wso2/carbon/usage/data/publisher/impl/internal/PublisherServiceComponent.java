/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.usage.data.publisher.impl.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.usage.data.publisher.api.Publisher;
import org.wso2.carbon.usage.data.publisher.impl.CompositePublisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OSGi service component that tracks all Publisher implementations.
 * Registers a CompositePublisher that delegates to all available publishers.
 */
@Component(
    name = "org.wso2.carbon.usage.data.publisher.impl",
    immediate = true
)
public class PublisherServiceComponent {

    private static final Log log = LogFactory.getLog(PublisherServiceComponent.class);
    private final List<Publisher> publishers = new CopyOnWriteArrayList<>();
    private ServiceRegistration<Publisher> serviceRegistration;
    private CompositePublisher compositePublisher;

    /**
     * Dynamically bind all Publisher service implementations.
     * Called when a Publisher service is registered.
     * Filters out CompositePublisher to prevent circular dependency.
     */
    @Reference(
        name = "publisher.service",
        service = Publisher.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        unbind = "unsetPublisher"
    )
    protected void setPublisher(Publisher publisher) {
        // Prevent circular dependency - don't track CompositePublisher
        if (publisher instanceof CompositePublisher) {
            log.debug("Ignoring CompositePublisher to prevent circular dependency");
            return;
        }

        publishers.add(publisher);
        log.info("Publisher registered: " + publisher.getClass().getName() +
                 " (Total publishers: " + publishers.size() + ")");
    }

    /**
     * Called when a Publisher service is unregistered.
     */
    protected void unsetPublisher(Publisher publisher) {
        // Only remove if it was actually tracked
        if (publisher instanceof CompositePublisher) {
            return;
        }

        publishers.remove(publisher);
        log.info("Publisher unregistered: " + publisher.getClass().getName() +
                 " (Total publishers: " + publishers.size() + ")");
    }

    @Activate
    protected void activate(ComponentContext context) {
        try {
            log.info("Activating Usage Data Publisher Service");

            // Create composite publisher that delegates to all registered publishers
            compositePublisher = new CompositePublisher(publishers);

            // Register the composite publisher as an OSGi service with marker property
            BundleContext bundleContext = context.getBundleContext();
            java.util.Dictionary<String, Object> properties = new java.util.Hashtable<>();
            properties.put("publisher.type", "composite");

            serviceRegistration = bundleContext.registerService(
                Publisher.class,
                compositePublisher,
                properties
            );

            log.info("Usage Data Publisher Service activated successfully with " +
                     publishers.size() + " publisher(s)");
        } catch (Exception e) {
            log.error("Failed to activate Usage Data Publisher Service", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Deactivating Usage Data Publisher Service");

        // Unregister the composite publisher service
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }

        // Shutdown the composite publisher
        if (compositePublisher != null) {
            compositePublisher.shutdown();
        }

        publishers.clear();
        log.info("Usage Data Publisher Service deactivated successfully");
    }
}

