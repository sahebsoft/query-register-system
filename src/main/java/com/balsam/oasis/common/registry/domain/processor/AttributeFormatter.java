package com.balsam.oasis.common.registry.domain.processor;


@FunctionalInterface
public interface AttributeFormatter<T> {
    String format(T value);
}