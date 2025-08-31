package com.balasam.oasis.common.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for Query Registration System
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Query Registration System - Comprehensive Tests")
public class QuerySystemComprehensiveTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Any setup needed before each test
    }
    
    // ==============================
    // PAGINATION TESTS
    // ==============================
    
    @Test
    @DisplayName("Test pagination returns correct total count")
    void testPaginationTotalCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("_start", "0")
                .param("_end", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.metadata.pagination.total").value(10)) // Should be 10, not 5
            .andExpect(jsonPath("$.metadata.pagination.start").value(0))
            .andExpect(jsonPath("$.metadata.pagination.end").value(5))
            .andExpect(jsonPath("$.metadata.pagination.pageSize").value(5))
            .andExpect(jsonPath("$.metadata.pagination.hasNext").value(true))
            .andExpect(jsonPath("$.metadata.pagination.hasPrevious").value(false))
            .andReturn();
            
        // Verify data content
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        assertThat(data).hasSize(5);
    }
    
    @Test
    @DisplayName("Test pagination second page")
    void testPaginationSecondPage() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("_start", "5")
                .param("_end", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.metadata.pagination.total").value(10))
            .andExpect(jsonPath("$.metadata.pagination.start").value(5))
            .andExpect(jsonPath("$.metadata.pagination.end").value(10))
            .andExpect(jsonPath("$.metadata.pagination.hasNext").value(false))
            .andExpect(jsonPath("$.metadata.pagination.hasPrevious").value(true));
    }
    
    @Test
    @DisplayName("Test pagination with no parameters returns all records")
    void testPaginationNoParams() throws Exception {
        mockMvc.perform(get("/api/query/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(10))
            .andExpect(jsonPath("$.metadata.pagination.total").value(10))
            .andExpect(jsonPath("$.metadata.pagination.pageSize").value(50)); // Default page size
    }
    
    // ==============================
    // FILTERING TESTS
    // ==============================
    
    @Test
    @DisplayName("Test filtering by status=ACTIVE")
    void testFilterByStatusActive() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("filter.status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        // Verify all returned users have ACTIVE status
        assertThat(data).allMatch(user -> "ACTIVE".equals(user.get("status")));
        assertThat(data).hasSize(7); // Based on test data
        
        // Verify metadata shows filter was applied
        Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
        Map<String, Object> appliedFilters = (Map<String, Object>) metadata.get("appliedFilters");
        assertThat(appliedFilters).containsKey("status");
    }
    
    @Test
    @DisplayName("Test filtering with multiple values (IN operator)")
    void testFilterWithMultipleValues() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("filter.status", "ACTIVE,PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        // Verify all returned users have either ACTIVE or PENDING status
        assertThat(data).allMatch(user -> 
            "ACTIVE".equals(user.get("status")) || "PENDING".equals(user.get("status")));
    }
    
    @Test
    @DisplayName("Test filtering by email (filterable attribute)")
    void testFilterByEmail() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("filter.email", "john.doe@company.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].email").value("john.doe@company.com"))
            .andExpect(jsonPath("$.data[0].username").value("john.doe"));
    }
    
    @Test
    @DisplayName("Test filtering with pagination combined")
    void testFilterWithPagination() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("filter.status", "ACTIVE")
                .param("_start", "0")
                .param("_end", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.metadata.pagination.total").value(7)) // Total ACTIVE users
            .andExpect(jsonPath("$.metadata.pagination.pageSize").value(3));
    }
    
    // ==============================
    // SORTING TESTS
    // ==============================
    
    @Test
    @DisplayName("Test sorting by username ascending")
    void testSortByUsernameAsc() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("sort", "username.asc")
                .param("_start", "0")
                .param("_end", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        // Verify sorting order
        assertThat(data.get(0).get("username")).isEqualTo("alice.williams");
        assertThat(data.get(1).get("username")).isEqualTo("bob.johnson");
        assertThat(data.get(2).get("username")).isEqualTo("charlie.brown");
    }
    
    @Test
    @DisplayName("Test sorting by username descending")
    void testSortByUsernameDesc() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("sort", "username.desc")
                .param("_start", "0")
                .param("_end", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        // Verify sorting order (descending)
        assertThat(data.get(0).get("username")).isEqualTo("john.doe");
        assertThat(data.get(1).get("username")).isEqualTo("jane.smith");
        assertThat(data.get(2).get("username")).isEqualTo("helen.black");
    }
    
    @Test
    @DisplayName("Test multiple field sorting")
    void testMultipleFieldSorting() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("sort", "status.asc,username.asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.metadata.appliedSort").isArray())
            .andExpect(jsonPath("$.metadata.appliedSort.length()").value(2))
            .andExpect(jsonPath("$.metadata.appliedSort[0].field").value("status"))
            .andExpect(jsonPath("$.metadata.appliedSort[0].direction").value("ASC"))
            .andExpect(jsonPath("$.metadata.appliedSort[1].field").value("username"))
            .andExpect(jsonPath("$.metadata.appliedSort[1].direction").value("ASC"));
    }
    
    // ==============================
    // METADATA TESTS
    // ==============================
    
    @Test
    @DisplayName("Test metadata includes all attribute definitions")
    void testMetadataAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users"))
            .andExpect(status().isOk())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
        Map<String, Object> attributes = (Map<String, Object>) metadata.get("attributes");
        
        // Verify all defined attributes are in metadata
        assertThat(attributes).containsKeys("id", "username", "email", "status");
        
        // Verify attribute properties
        Map<String, Object> usernameAttr = (Map<String, Object>) attributes.get("username");
        assertThat(usernameAttr.get("name")).isEqualTo("username");
        assertThat(usernameAttr.get("type")).isEqualTo("String");
        assertThat(usernameAttr.get("filterable")).isEqualTo(true);
        assertThat(usernameAttr.get("sortable")).isEqualTo(true);
        
        Map<String, Object> statusAttr = (Map<String, Object>) attributes.get("status");
        assertThat(statusAttr.get("allowedValues")).isEqualTo(List.of("ACTIVE", "INACTIVE"));
    }
    
    @Test
    @DisplayName("Test metadata shows applied filters correctly")
    void testMetadataAppliedFilters() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("filter.status", "ACTIVE")
                .param("filter.email", "john.doe@company.com"))
            .andExpect(status().isOk())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
        Map<String, Object> appliedFilters = (Map<String, Object>) metadata.get("appliedFilters");
        
        assertThat(appliedFilters).containsKeys("status", "email");
        
        Map<String, Object> statusFilter = (Map<String, Object>) appliedFilters.get("status");
        assertThat(statusFilter.get("operator")).isEqualTo("EQUALS");
        assertThat(statusFilter.get("value")).isEqualTo("ACTIVE");
        
        Map<String, Object> emailFilter = (Map<String, Object>) appliedFilters.get("email");
        assertThat(emailFilter.get("value")).isEqualTo("john.doe@company.com");
    }
    
    @Test
    @DisplayName("Test performance metadata is included")
    void testPerformanceMetadata() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users"))
            .andExpect(status().isOk())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
        Map<String, Object> performance = (Map<String, Object>) metadata.get("performance");
        
        assertThat(performance).containsKeys(
            "executionTimeMs", 
            "rowsFetched", 
            "totalRowsScanned", 
            "cacheHit", 
            "queryPlan"
        );
        
        assertThat(performance.get("rowsFetched")).isEqualTo(10);
        assertThat(performance.get("cacheHit")).isEqualTo(false);
    }
    
    // ==============================
    // COMPLEX QUERY TESTS
    // ==============================
    
    @Test
    @DisplayName("Test userDashboard query with aggregations")
    void testUserDashboardQuery() throws Exception {
        mockMvc.perform(get("/api/query/userDashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.metadata.attributes").exists())
            .andExpect(jsonPath("$.metadata.attributes.lifetimeValue").exists())
            .andExpect(jsonPath("$.metadata.attributes.totalOrders").exists())
            .andExpect(jsonPath("$.metadata.attributes.averageOrderValue").exists())
            .andExpect(jsonPath("$.metadata.attributes.membershipTier").exists());
    }
    
    @Test
    @DisplayName("Test virtual attribute calculation (membershipTier)")
    void testVirtualAttributeCalculation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/userDashboard")
                .param("filter.membershipTier", "GOLD"))
            .andExpect(status().isOk())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        // Verify virtual attribute is calculated correctly
        for (Map<String, Object> user : data) {
            if (user.get("membershipTier") != null) {
                assertThat(user.get("membershipTier")).isEqualTo("GOLD");
            }
        }
    }
    
    // ==============================
    // ERROR HANDLING TESTS
    // ==============================
    
    @Test
    @DisplayName("Test invalid query name returns 404")
    void testInvalidQueryName() throws Exception {
        mockMvc.perform(get("/api/query/nonexistent"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("Test invalid filter attribute returns error")
    void testInvalidFilterAttribute() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("filter.invalidAttribute", "value"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Test invalid sort attribute returns error")
    void testInvalidSortAttribute() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("sort", "invalidAttribute.asc"))
            .andExpect(status().isBadRequest());
    }
    
    // ==============================
    // LINKS TESTS
    // ==============================
    
    @Test
    @DisplayName("Test links are generated correctly")
    void testLinksGeneration() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("_start", "3")
                .param("_end", "6"))
            .andExpect(status().isOk())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> links = (Map<String, Object>) response.get("links");
        
        assertThat(links).containsKeys("self", "first", "last", "next", "previous");
        assertThat(links.get("self")).isEqualTo("/api/query/users?_start=3&_end=6");
        assertThat(links.get("first")).isEqualTo("/api/query/users?_start=0&_end=3");
        assertThat(links.get("next")).isEqualTo("/api/query/users?_start=6&_end=9");
        assertThat(links.get("previous")).isEqualTo("/api/query/users?_start=0&_end=3");
    }
    
    // ==============================
    // COMBINED OPERATIONS TESTS
    // ==============================
    
    @Test
    @DisplayName("Test filtering + sorting + pagination combined")
    void testCombinedOperations() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("filter.status", "ACTIVE")
                .param("sort", "username.desc")
                .param("_start", "0")
                .param("_end", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        // Verify all conditions are met
        assertThat(data).allMatch(user -> "ACTIVE".equals(user.get("status")));
        assertThat(data).hasSize(3);
        
        // Verify sorting is applied (descending username among ACTIVE users)
        String firstUsername = (String) data.get(0).get("username");
        String secondUsername = (String) data.get(1).get("username");
        assertThat(firstUsername.compareTo(secondUsername)).isGreaterThan(0);
        
        // Verify metadata
        Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
        Map<String, Object> pagination = (Map<String, Object>) metadata.get("pagination");
        assertThat(pagination.get("total")).isEqualTo(7); // Total ACTIVE users
        
        Map<String, Object> appliedFilters = (Map<String, Object>) metadata.get("appliedFilters");
        assertThat(appliedFilters).containsKey("status");
        
        List<Map<String, Object>> appliedSort = (List<Map<String, Object>>) metadata.get("appliedSort");
        assertThat(appliedSort).hasSize(1);
        assertThat(appliedSort.get(0).get("field")).isEqualTo("username");
        assertThat(appliedSort.get(0).get("direction")).isEqualTo("DESC");
    }
    
    @Test
    @DisplayName("Test all attributes are correctly typed")
    void testAttributeTypes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/query/users")
                .param("_start", "0")
                .param("_end", "1"))
            .andExpect(status().isOk())
            .andReturn();
            
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        
        assertThat(data).hasSize(1);
        Map<String, Object> user = data.get(0);
        
        // Verify types
        assertThat(user.get("id")).isInstanceOf(Integer.class);
        assertThat(user.get("username")).isInstanceOf(String.class);
        assertThat(user.get("email")).isInstanceOf(String.class);
        assertThat(user.get("status")).isInstanceOf(String.class);
    }
}