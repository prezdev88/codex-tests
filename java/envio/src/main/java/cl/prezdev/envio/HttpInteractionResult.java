package cl.prezdev.envio;

public record HttpInteractionResult(String formattedBody, String rawRequest, String rawResponse, String errorMessage, int statusCode) {

    public static HttpInteractionResult success(String formattedBody, String rawRequest, String rawResponse, int statusCode) {
        return new HttpInteractionResult(formattedBody, rawRequest, rawResponse, null, statusCode);
    }

    public static HttpInteractionResult failure(String message, String rawRequest, String rawResponse) {
        return new HttpInteractionResult("", rawRequest, rawResponse, message, -1);
    }

    public boolean hasError() {
        return errorMessage != null;
    }
}
