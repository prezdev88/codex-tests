package cl.prezdev.envio;

public enum HttpMethod {
    GET(false),
    POST(true),
    PUT(true),
    DELETE(true),
    PATCH(true),
    HEAD(false),
    OPTIONS(false);

    private final boolean allowsBody;

    HttpMethod(boolean allowsBody) {
        this.allowsBody = allowsBody;
    }

    public boolean allowsBody() {
        return allowsBody;
    }
}
