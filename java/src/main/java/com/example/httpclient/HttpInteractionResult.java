package com.example.httpclient;

public record HttpInteractionResult(String formattedBody, String rawRequest, String rawResponse, String errorMessage) {

    public static HttpInteractionResult success(String formattedBody, String rawRequest, String rawResponse) {
        return new HttpInteractionResult(formattedBody, rawRequest, rawResponse, null);
    }

    public static HttpInteractionResult failure(String message, String rawRequest, String rawResponse) {
        return new HttpInteractionResult("", rawRequest, rawResponse, message);
    }

    public boolean hasError() {
        return errorMessage != null;
    }
}
