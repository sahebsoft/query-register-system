package com.balsam.oasis.common.registry.web.dto.response;

/**
 * Common interface for successful query responses.
 * Both QueryListResponse and QuerySingleResponse implement this interface
 * to provide consistent behavior for success responses.
 */
public interface QuerySuccessResponse {
    
    /**
     * Whether the operation was successful
     */
    boolean isSuccess();
    
    /**
     * Get the optional metadata for the response
     */
    Object getMetadata();
}