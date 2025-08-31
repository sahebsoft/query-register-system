package com.balasam.oasis.common.query.config;

import com.balasam.oasis.common.query.processor.Processor;
import com.balasam.oasis.common.query.processor.Validator;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Global processors and validators shared across queries
 * All transformations are handled by Processor
 * Type conversions are automatic based on attribute type
 */
@Data
@Builder
public class GlobalProcessors {
    
    @Builder.Default
    private final Map<String, Validator> validators = new HashMap<>();
    
    @Builder.Default
    private final Map<String, Processor> processors = new HashMap<>();
    
    public static class GlobalProcessorsBuilder {
        private Map<String, Validator> validators = new HashMap<>();
        private Map<String, Processor> processors = new HashMap<>();
        
        public GlobalProcessorsBuilder addValidator(String name, Validator validator) {
            if (this.validators == null) {
                this.validators = new HashMap<>();
            }
            this.validators.put(name, validator);
            return this;
        }
        
        public GlobalProcessorsBuilder addProcessor(String name, Processor processor) {
            if (this.processors == null) {
                this.processors = new HashMap<>();
            }
            this.processors.put(name, processor);
            return this;
        }
    }
    
    public Validator getValidator(String name) {
        return validators.get(name);
    }
    
    public Processor getProcessor(String name) {
        return processors.get(name);
    }
}