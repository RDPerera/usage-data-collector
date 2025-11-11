package org.wso2.carbon.usage.data.publisher.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.publisher.api.Publisher;
import org.wso2.carbon.usage.data.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.publisher.api.model.UsageData;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite publisher that delegates to multiple publisher implementations.
 * Ensures that data is published to all registered publishers.
 */
public class CompositePublisher implements Publisher {

    private static final Log log = LogFactory.getLog(CompositePublisher.class);
    private final List<Publisher> publishers;

    public CompositePublisher(List<Publisher> publishers) {
        this.publishers = publishers;
    }

    @Override
    public void publish(UsageData data) throws PublisherException {
        if (publishers.isEmpty()) {
            log.warn("No publishers available to publish data");
            return;
        }

        List<PublisherException> failures = new ArrayList<>();

        for (Publisher publisher : publishers) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Publishing data using: " + publisher.getClass().getName());
                }
                publisher.publish(data);
            } catch (PublisherException e) {
                log.error("Publisher failed: " + publisher.getClass().getName(), e);
                failures.add(e);
            }
        }

        // Throw exception if all publishers failed
        if (failures.size() == publishers.size()) {
            throw new PublisherException("All publishers failed to publish data", failures.get(0));
        } else if (!failures.isEmpty()) {
            log.warn(failures.size() + " out of " + publishers.size() + " publishers failed");
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down composite publisher with " + publishers.size() + " publisher(s)");
        for (Publisher publisher : publishers) {
            try {
                publisher.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown publisher: " + publisher.getClass().getName(), e);
            }
        }
    }
}

