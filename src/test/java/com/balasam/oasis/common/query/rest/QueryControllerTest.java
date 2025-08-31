package com.balasam.oasis.common.query.rest;

import com.balasam.oasis.common.query.example.ExampleQueryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for Query REST API
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "query.registration.enabled=true",
    "query.registration.rest.enabled=true"
})
@Transactional
class QueryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testSimpleQueryExecution() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("_start", "0")
                .param("_end", "10")
                .param("filter.status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(jsonPath("$.metadata").exists());
    }
    
    @Test
    void testQueryWithMultipleFilters() throws Exception {
        mockMvc.perform(get("/api/query/userDashboard")
                .param("_start", "0")
                .param("_end", "5")
                .param("filter.status", "ACTIVE,PENDING")
                .param("filter.lifetimeValue.gt", "1000")
                .param("sort", "lifetimeValue.desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(lessThanOrEqualTo(5)))
            .andExpect(jsonPath("$.metadata.appliedFilters").exists())
            .andExpect(jsonPath("$.metadata.appliedSort").isArray());
    }
    
    @Test
    void testQueryWithParameters() throws Exception {
        mockMvc.perform(get("/api/query/userDashboard")
                .param("param.includeInactive", "false")
                .param("param.minOrderDate", "2023-01-01")
                .param("_start", "0")
                .param("_end", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.metadata.parameters").exists())
            .andExpect(jsonPath("$.metadata.parameters.includeInactive.value").value(false));
    }
    
    @Test
    void testPaginationLinks() throws Exception {
        mockMvc.perform(get("/api/query/users")
                .param("_start", "3")
                .param("_end", "6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.links.self").exists())
            .andExpect(jsonPath("$.links.next").exists())
            .andExpect(jsonPath("$.links.previous").exists())
            .andExpect(jsonPath("$.links.first").exists());
    }
    
    @Test
    void testFilterOperatorShortcuts() throws Exception {
        // Test various operator shortcuts
        mockMvc.perform(get("/api/query/userDashboard")
                .param("filter.createdDate.gte", "2023-01-01")
                .param("filter.createdDate.lte", "2024-12-31")
                .param("filter.email.like", "%company.com")
                .param("filter.status.in", "ACTIVE,PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void testSortingWithMultipleFields() throws Exception {
        mockMvc.perform(get("/api/query/userDashboard")
                .param("sort", "status.asc,lifetimeValue.desc,username.asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata.appliedSort").isArray())
            .andExpect(jsonPath("$.metadata.appliedSort.length()").value(3));
    }
    
    @Test
    void testMetadataLevels() throws Exception {
        // Full metadata
        mockMvc.perform(get("/api/query/users")
                .param("_meta", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata").exists())
            .andExpect(jsonPath("$.metadata.attributes").exists())
            .andExpect(jsonPath("$.metadata.performance").exists());
        
        // No metadata
        mockMvc.perform(get("/api/query/users")
                .param("_meta", "none"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.metadata").doesNotExist());
    }
    
    @Test
    void testInvalidQueryName() throws Exception {
        mockMvc.perform(get("/api/query/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void testValidationError() throws Exception {
        // Invalid filter operator
        mockMvc.perform(get("/api/query/users")
                .param("filter.id.invalid", "123"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testPostQueryExecution() throws Exception {
        String requestBody = "{\n" +
            "    \"params\": {\n" +
            "        \"includeInactive\": false\n" +
            "    },\n" +
            "    \"filters\": {\n" +
            "        \"status\": {\n" +
            "            \"attribute\": \"status\",\n" +
            "            \"operator\": \"IN\",\n" +
            "            \"values\": [\"ACTIVE\", \"PENDING\"]\n" +
            "        }\n" +
            "    },\n" +
            "    \"sorts\": [\n" +
            "        {\n" +
            "            \"attribute\": \"lifetimeValue\",\n" +
            "            \"direction\": \"DESC\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"start\": 0,\n" +
            "    \"end\": 10,\n" +
            "    \"includeMetadata\": true\n" +
            "}";
        
        mockMvc.perform(post("/api/query/userDashboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.metadata").exists());
    }
}