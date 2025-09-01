package com.balasam.oasis.common.query.core.definition;

import lombok.Builder;
import lombok.Value;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import com.balasam.oasis.common.query.processor.AttributeProcessor;

import java.util.Set;
import java.util.List;
import java.util.function.Function;

/**
 * Immutable attribute definition for query fields
 * Simplified to reduce redundancy - all transformations handled by processor
 */
@Value
@Builder(toBuilder = true)
public class AttributeDef {
    String name; // Frontend/API name
    String dbColumn; // Database column name
    Class<?> type; // Java type for automatic conversion

    @Builder.Default
    boolean filterable = false;

    @Builder.Default
    boolean sortable = false;

    @Builder.Default
    boolean calculated = false; // Calculated from other fields

    @Builder.Default
    boolean virtual = false; // Not from database

    @Builder.Default
    boolean primaryKey = false;

    @Builder.Default
    Set<FilterOp> allowedOperators = ImmutableSet.of(FilterOp.EQUALS);

    @Builder.Default
    List<String> allowedValues = ImmutableList.of();

    Object defaultValue; // Default value when null

    // Single processor handles: conversion, formatting, masking, calculation
    AttributeProcessor processor;

    // Security rule determines if user can see this attribute
    Function<Object, Boolean> securityRule;

    @Builder.Default
    Set<String> dependencies = ImmutableSet.of(); // For virtual/calculated attributes

    String description;

    public boolean isSecured() {
        return securityRule != null;
    }

    public boolean hasProcessor() {
        return processor != null;
    }

    public boolean hasAllowedValues() {
        return allowedValues != null && !allowedValues.isEmpty();
    }

    public boolean supportsOperator(FilterOp op) {
        return allowedOperators != null && allowedOperators.contains(op);
    }
}