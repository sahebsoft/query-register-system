package com.balasam.oasis.common.query.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.balasam.oasis.common.query.core.execution.QueryExecutor;
import com.balasam.oasis.common.query.core.execution.QueryExecutorImpl;

/**
 * Initializes metadata caches after application startup
 */
@Component
public class MetadataCacheInitializer {

    private static final Logger log = LoggerFactory.getLogger(MetadataCacheInitializer.class);

    @Autowired
    private QueryExecutor queryExecutor;

    @Autowired
    private QueryProperties properties;

    /**
     * Pre-warm metadata caches after all queries are registered
     * This runs after the application is fully started
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.getMetadata() != null &&
                properties.getMetadata().getCache() != null &&
                properties.getMetadata().getCache().isPrewarm()) {

            log.info("Pre-warming metadata caches on startup...");
            try {
                if (queryExecutor instanceof QueryExecutorImpl queryExecutorImpl) {
                    queryExecutorImpl.prewarmAllCaches();
                    log.info("Metadata cache pre-warming completed successfully");
                } else {
                    log.warn("QueryExecutor is not an instance of QueryExecutorImpl, cannot pre-warm caches");
                }
            } catch (Exception e) {
                log.error("Failed to pre-warm metadata caches - STOPPING APPLICATION", e);
                // If pre-warming fails, stop the application to prevent bad queries from running
                if (properties.getMetadata().getCache().isFailOnError()) {
                    log.error("Stopping application due to metadata cache pre-warming failure");
                    System.exit(1);
                }
            }
        }
    }
}