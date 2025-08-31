package com.balasam.oasis.common.query.config;

import com.balasam.oasis.common.query.processor.*;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Global processors and converters shared across queries
 */
@Data
@Builder
public class GlobalProcessors {
    
    @Builder.Default
    private final Map<String, Calculator> calculators = new HashMap<>();
    
    @Builder.Default
    private final Map<Class<?>, Converter> converters = new HashMap<>();
    
    @Builder.Default
    private final Map<String, Validator> validators = new HashMap<>();
    
    @Builder.Default
    private final Map<String, Formatter> formatters = new HashMap<>();
    
    @Builder.Default
    private final Map<String, Processor> processors = new HashMap<>();
    
    public static class GlobalProcessorsBuilder {
        private Map<String, Calculator> calculators = new HashMap<>();
        private Map<Class<?>, Converter> converters = new HashMap<>();
        private Map<String, Validator> validators = new HashMap<>();
        private Map<String, Formatter> formatters = new HashMap<>();
        private Map<String, Processor> processors = new HashMap<>();
        
        public GlobalProcessorsBuilder addCalculator(String name, Calculator calculator) {
            if (this.calculators == null) {
                this.calculators = new HashMap<>();
            }
            this.calculators.put(name, calculator);
            return this;
        }
        
        public GlobalProcessorsBuilder addConverter(Class<?> type, Converter converter) {
            if (this.converters == null) {
                this.converters = new HashMap<>();
            }
            this.converters.put(type, converter);
            return this;
        }
        
        public GlobalProcessorsBuilder addValidator(String name, Validator validator) {
            if (this.validators == null) {
                this.validators = new HashMap<>();
            }
            this.validators.put(name, validator);
            return this;
        }
        
        public GlobalProcessorsBuilder addFormatter(String name, Formatter formatter) {
            if (this.formatters == null) {
                this.formatters = new HashMap<>();
            }
            this.formatters.put(name, formatter);
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
    
    public Calculator getCalculator(String name) {
        return calculators.get(name);
    }
    
    public Converter getConverter(Class<?> type) {
        return converters.get(type);
    }
    
    public Validator getValidator(String name) {
        return validators.get(name);
    }
    
    public Formatter getFormatter(String name) {
        return formatters.get(name);
    }
    
    public Processor getProcessor(String name) {
        return processors.get(name);
    }
}