package com.balasam.oasis.common.query.processor;

/**
 * Generic processor for value transformation
 */
@FunctionalInterface
public interface Processor {
    /**
     * Process a value
     * @param value the value to process
     * @return the processed value
     */
    Object process(Object value);
}