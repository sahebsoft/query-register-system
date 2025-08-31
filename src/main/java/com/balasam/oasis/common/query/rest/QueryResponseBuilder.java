package com.balasam.oasis.common.query.rest;

import com.balasam.oasis.common.query.core.result.QueryResult;
import com.balasam.oasis.common.query.core.result.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds HTTP responses from query results
 */
@Component
public class QueryResponseBuilder {
    
    private final ObjectMapper objectMapper;
    
    public QueryResponseBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public ResponseEntity<?> build(QueryResult result, String format, String queryName) {
        switch (format.toLowerCase()) {
            case "csv":
                return buildCsvResponse(result, queryName);
            case "excel":
                return buildExcelResponse(result, queryName);
            case "xml":
                return buildXmlResponse(result, queryName);
            case "json":
            default:
                return buildJsonResponse(result, queryName);
        }
    }
    
    public ResponseEntity<?> buildSingle(Object singleResult, String format, String queryName) {
        if (singleResult == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Convert Row to Map if needed
        Map<String, Object> data;
        if (singleResult instanceof Row) {
            data = ((Row) singleResult).toMap();
        } else if (singleResult instanceof Map) {
            data = (Map<String, Object>) singleResult;
        } else {
            data = ImmutableMap.of("result", singleResult);
        }
        
        switch (format.toLowerCase()) {
            case "xml":
                return buildXmlSingleResponse(data, queryName);
            case "json":
            default:
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(data);
        }
    }
    
    public ResponseEntity<?> buildExport(QueryResult result, String format, String queryName) {
        switch (format.toLowerCase()) {
            case "csv":
                return buildCsvExport(result, queryName);
            case "excel":
                return buildExcelExport(result, queryName);
            default:
                return build(result, format, queryName);
        }
    }
    
    private ResponseEntity<?> buildJsonResponse(QueryResult result, String queryName) {
        Map<String, Object> response = ImmutableMap.of(
            "data", result.toListOfMaps(),
            "metadata", result.getMetadata() != null ? result.getMetadata() : ImmutableMap.of(),
            "links", buildLinks(result, queryName)
        );
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
    
    private ResponseEntity<?> buildCsvResponse(QueryResult result, String queryName) {
        try {
            String csv = convertToCsv(result);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + queryName + ".csv\"")
                .body(csv);
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to generate CSV: " + e.getMessage());
        }
    }
    
    private ResponseEntity<?> buildCsvExport(QueryResult result, String queryName) {
        try {
            String csv = convertToCsv(result);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + queryName + "_export.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to generate CSV export: " + e.getMessage());
        }
    }
    
    private ResponseEntity<?> buildExcelResponse(QueryResult result, String queryName) {
        // For Excel, we would need Apache POI or similar library
        // This is a simplified implementation
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("Excel format not yet implemented");
    }
    
    private ResponseEntity<?> buildExcelExport(QueryResult result, String queryName) {
        // For Excel export, we would need Apache POI or similar library
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("Excel export not yet implemented");
    }
    
    private ResponseEntity<?> buildXmlResponse(QueryResult result, String queryName) {
        // XML response implementation
        // Could use JAXB or Jackson XML
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("XML format not yet implemented");
    }
    
    private ResponseEntity<?> buildXmlSingleResponse(Map<String, Object> data, String queryName) {
        // XML single response implementation
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("XML format for single object not yet implemented");
    }
    
    private String convertToCsv(QueryResult result) throws Exception {
        if (result.isEmpty()) {
            return "";
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        
        List<Map<String, Object>> data = result.toListOfMaps();
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            List<String> headers = new ArrayList<>(firstRow.keySet());
            
            // Write headers
            writer.println(String.join(",", headers));
            
            // Write data rows
            for (Map<String, Object> row : data) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    Object value = row.get(header);
                    values.add(escapeCsvValue(value));
                }
                writer.println(String.join(",", values));
            }
        }
        
        writer.flush();
        return baos.toString(StandardCharsets.UTF_8);
    }
    
    private String escapeCsvValue(Object value) {
        if (value == null) {
            return "";
        }
        
        String str = value.toString();
        
        // Escape if contains comma, quote, or newline
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            str = str.replace("\"", "\"\"");
            return "\"" + str + "\"";
        }
        
        return str;
    }
    
    private Map<String, String> buildLinks(QueryResult result, String queryName) {
        if (result.getMetadata() == null || result.getMetadata().getPagination() == null) {
            return ImmutableMap.of();
        }
        
        var pagination = result.getMetadata().getPagination();
        String baseUrl = "/api/query/" + queryName;
        
        ImmutableMap.Builder<String, String> links = ImmutableMap.builder();
        
        // Self link
        links.put("self", baseUrl + "?_start=" + pagination.getStart() + "&_end=" + pagination.getEnd());
        
        // Next link
        if (pagination.isHasNext()) {
            int nextStart = pagination.getEnd();
            int nextEnd = nextStart + pagination.getPageSize();
            links.put("next", baseUrl + "?_start=" + nextStart + "&_end=" + nextEnd);
        }
        
        // Previous link
        if (pagination.isHasPrevious()) {
            int prevEnd = pagination.getStart();
            int prevStart = Math.max(0, prevEnd - pagination.getPageSize());
            links.put("previous", baseUrl + "?_start=" + prevStart + "&_end=" + prevEnd);
        }
        
        // First link
        links.put("first", baseUrl + "?_start=0&_end=" + pagination.getPageSize());
        
        // Last link
        if (pagination.getTotal() > 0) {
            int lastStart = (pagination.getTotal() / pagination.getPageSize()) * pagination.getPageSize();
            links.put("last", baseUrl + "?_start=" + lastStart + "&_end=" + pagination.getTotal());
        }
        
        return links.build();
    }
}