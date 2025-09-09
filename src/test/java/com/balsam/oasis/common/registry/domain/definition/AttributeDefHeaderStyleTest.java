package com.balsam.oasis.common.registry.domain.definition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AttributeDefHeaderStyleTest {

    @Test
    public void testHeaderStyleAttribute() {
        // Test that we can set headerStyle instead of displayOrder
        AttributeDef<String> attr = AttributeDef.name("amount")
                .type(String.class)
                .headerText("Amount")
                .alignment("right")
                .headerStyle("font-weight: bold; color: #007bff;")
                .build();

        assertEquals("Amount", attr.getHeaderText());
        assertEquals("right", attr.getAlignment());
        assertEquals("font-weight: bold; color: #007bff;", attr.getHeaderStyle());
        assertTrue(attr.isVisible());
    }

    @Test
    public void testHeaderStyleWithTableStyling() {
        // Example of using headerStyle for different column types
        AttributeDef<Object> currencyColumn = AttributeDef.name("price")
                .headerText("Price")
                .alignment("right")
                .headerStyle("background-color: #f8f9fa; font-weight: 600;")
                .width("120px")
                .build();

        AttributeDef<Object> statusColumn = AttributeDef.name("status")
                .headerText("Status")
                .alignment("center")
                .headerStyle("text-transform: uppercase; letter-spacing: 1px;")
                .width("100px")
                .build();

        // Verify the styles are set correctly
        assertNotNull(currencyColumn.getHeaderStyle());
        assertNotNull(statusColumn.getHeaderStyle());
        assertEquals("120px", currencyColumn.getWidth());
        assertEquals("100px", statusColumn.getWidth());
    }

    @Test
    public void testNoHeaderStyleDefault() {
        // headerStyle should be null by default
        AttributeDef<Object> attr = AttributeDef.name("simple")
                .build();

        assertNull(attr.getHeaderStyle());
        assertEquals("left", attr.getAlignment()); // default alignment
        assertTrue(attr.isVisible()); // default visibility
    }
}