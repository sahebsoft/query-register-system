package com.balasam.oasis.common.query.core.definition;

import com.balasam.oasis.common.query.processor.Calculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributeDefTest {
    
    @Test
    void testRegularAttribute() {
        AttributeDef<String> attr = AttributeDef.name("countryId")
                .type(String.class)
                .aliasName("country_id")
                .filterable(true)
                .sortable(false)
                .build();
                
        assertEquals("countryId", attr.getName());
        assertEquals("country_id", attr.getAliasName());
        assertTrue(attr.isFilterable());
        assertFalse(attr.isSortable());
        assertFalse(attr.isTransient());
        assertFalse(attr.hasSortProperty());
    }
    
    @Test
    void testTransientAttributeWithSortProperty() {
        Calculator<String> calculator = (row, context) -> "AA";
        
        AttributeDef<String> attr = AttributeDef.name("countryDesc")
                .type(String.class)
                .calculated(calculator)
                .sortProperty("countryId")
                .build();
                
        assertEquals("countryDesc", attr.getName());
        assertNull(attr.getAliasName());
        assertTrue(attr.isTransient());
        assertTrue(attr.hasSortProperty());
        assertEquals("countryId", attr.getSortProperty());
        assertTrue(attr.hasCalculator());
        assertEquals(calculator, attr.getCalculator());
    }
    
    @Test
    void testTransientAttributeCannotBeFilterable() {
        Calculator<String> calculator = (row, context) -> "AA";
        
        assertThrows(IllegalStateException.class, () -> {
            AttributeDef.name("countryDesc")
                    .type(String.class)
                    .calculated(calculator)
                    .filterable(true)
                    .build();
        });
    }
    
    @Test
    void testTransientAttributeCannotBeSortableWithoutSortProperty() {
        Calculator<String> calculator = (row, context) -> "AA";
        
        assertThrows(IllegalStateException.class, () -> {
            AttributeDef.name("countryDesc")
                    .type(String.class)
                    .calculated(calculator)
                    .sortable(true)
                    .build();
        });
    }
    
    @Test
    void testTransientAttributeCanBeSortableWithSortProperty() {
        Calculator<String> calculator = (row, context) -> "AA";
        
        AttributeDef<String> attr = AttributeDef.name("countryDesc")
                .type(String.class)
                .calculated(calculator)
                .sortProperty("countryId")
                .sortable(true)
                .build();
                
        assertTrue(attr.isSortable());
        assertTrue(attr.hasSortProperty());
        assertEquals("countryId", attr.getSortProperty());
    }
    
    @Test
    void testRegularAttributeCannotHaveSortProperty() {
        assertThrows(IllegalStateException.class, () -> {
            AttributeDef.name("countryId")
                    .type(String.class)
                    .aliasName("country_id")
                    .sortProperty("someOtherField")
                    .build();
        });
    }
    
    @Test
    void testTransientAttributeMustHaveCalculator() {
        assertThrows(IllegalStateException.class, () -> {
            AttributeDef.name("countryDesc")
                    .type(String.class)
                    .transient_(true)
                    .build();
        });
    }
}