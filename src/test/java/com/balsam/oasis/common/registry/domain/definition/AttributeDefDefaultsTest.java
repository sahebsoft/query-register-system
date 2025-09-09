package com.balsam.oasis.common.registry.domain.definition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AttributeDefDefaultsTest {

    @Test
    public void testRegularAttributeDefaults() {
        // Regular attribute should have filterable and sortable as true by default
        AttributeDef<Object> regularAttr = AttributeDef.name("testAttr")
                .aliasName("test_column")
                .build();

        assertTrue(regularAttr.isFilterable(), "Regular attributes should be filterable by default");
        assertTrue(regularAttr.isSortable(), "Regular attributes should be sortable by default");
        assertFalse(regularAttr.isVirtual(), "Should not be virtual");
        assertEquals(Object.class, regularAttr.getType(), "Type should default to Object.class");
    }

    @Test
    public void testVirtualAttributeDefaults() {
        // Virtual/transient attributes should have filterable and sortable as false
        AttributeDef<String> virtualAttr = AttributeDef.name("calculated")
                .type(String.class)
                .virtual(true)
                .calculated((row, context) -> "calculated value")
                .build();

        assertFalse(virtualAttr.isFilterable(), "Virtual attributes cannot be filterable");
        assertFalse(virtualAttr.isSortable(), "Virtual attributes cannot be sortable without sortProperty");
        assertTrue(virtualAttr.isVirtual(), "Should be virtual");
    }

    @Test
    public void testExplicitlySetValues() {
        // Explicitly setting values should override defaults
        AttributeDef<Object> customAttr = AttributeDef.name("customAttr")
                .filterable(false)
                .sortable(false)
                .build();

        assertFalse(customAttr.isFilterable(), "Should respect explicit filterable(false)");
        assertFalse(customAttr.isSortable(), "Should respect explicit sortable(false)");
    }

    @Test
    public void testTypeChange() {
        // Changing type should preserve other settings
        AttributeDef<String> typedAttr = AttributeDef.name("typedAttr")
                .filterable(false) // Explicitly set to false
                .type(String.class)
                .sortable(true) // Explicitly set to true
                .build();

        assertFalse(typedAttr.isFilterable(), "Should preserve explicit filterable setting");
        assertTrue(typedAttr.isSortable(), "Should preserve explicit sortable setting");
        assertEquals(String.class, typedAttr.getType());
    }
}