package com.splitshare.splitshare.service;

/**
 * Simple data class for error responses.
 * Used to return user-friendly error messages in a structured format.
 */
public class ErrorResponse {
    private String message;

    /**
     * Constructor for creating a new error response
     *
     * @param message The error message to display to the user
     */
    public ErrorResponse(String message) {
        this.message = message;
    }

    /**
     * Getter for the error message
     */
    public String getMessage() {
        return message;
    }
}
