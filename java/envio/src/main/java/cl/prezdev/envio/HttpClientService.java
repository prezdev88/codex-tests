package cl.prezdev.envio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpClientService {

    private static final String NEW_LINE = System.lineSeparator();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpClientService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public HttpInteractionResult execute(HttpMethod method, String url, String body) {
        String rawRequest = "";
        String rawResponse = "";
        try {
            URI uri = buildUri(url);
            HttpRequest request = buildRequest(method, uri, body);
            rawRequest = buildRawRequest(method, uri, request, body);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            rawResponse = buildRawResponse(response);
            String formattedBody = formatBody(response.body());

            return HttpInteractionResult.success(formattedBody, rawRequest, rawResponse, response.statusCode());
        } catch (Exception exception) {
            return HttpInteractionResult.failure("Error: " + exception.getMessage(), rawRequest, rawResponse);
        }
    }

    private URI buildUri(String url) throws URISyntaxException {
        return new URI(url);
    }

    private HttpRequest buildRequest(HttpMethod method, URI uri, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");

        if (method.allowsBody() && body != null && !body.isBlank()) {
            builder.header("Content-Type", "application/json");
            builder.method(method.name(), HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
        }

        return builder.build();
    }

    private String buildRawRequest(HttpMethod method, URI uri, HttpRequest request, String body) {
        StringBuilder builder = new StringBuilder();
        builder.append(method.name())
                .append(" ")
                .append(buildRequestTarget(uri))
                .append(" HTTP/1.1")
                .append(NEW_LINE);

        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Host", List.of(buildHostHeader(uri)));
        request.headers().map().forEach(headers::put);

        headers.forEach((name, values) ->
                values.forEach(value -> builder.append(name).append(": ").append(value).append(NEW_LINE)));

        if (method.allowsBody() && body != null && !body.isBlank()) {
            builder.append(NEW_LINE).append(body);
        }

        return builder.toString();
    }

    private String buildHostHeader(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return "";
        }
        if (port == -1) {
            return host;
        }
        return host + ":" + port;
    }

    private String buildRequestTarget(URI uri) {
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (query == null || query.isBlank()) {
            return path;
        }
        return path + "?" + query;
    }

    private String buildRawResponse(HttpResponse<String> response) {
        StringBuilder builder = new StringBuilder();
        builder.append(resolveHttpVersion(response.version()))
                .append(" ")
                .append(response.statusCode())
                .append(" ")
                .append(HttpStatusMessageResolver.resolve(response.statusCode()))
                .append(NEW_LINE);

        response.headers().map().forEach((name, values) ->
                values.forEach(value -> builder.append(name).append(": ").append(value).append(NEW_LINE)));

        builder.append(NEW_LINE).append(response.body());
        return builder.toString();
    }

    private String formatBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            return body;
        }
    }

    private String resolveHttpVersion(HttpClient.Version version) {
        if (version == null) {
            return "HTTP/1.1";
        }
        return switch (version) {
            case HTTP_1_1 -> "HTTP/1.1";
            case HTTP_2 -> "HTTP/2";
        };
    }
}
