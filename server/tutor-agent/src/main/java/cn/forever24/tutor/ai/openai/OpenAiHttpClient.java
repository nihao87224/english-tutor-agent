package cn.forever24.tutor.ai.openai;

import cn.forever24.tutor.ai.provider.AiProviderErrorType;
import cn.forever24.tutor.ai.provider.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public final class OpenAiHttpClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProviderProperties properties;

    public OpenAiHttpClient(ObjectMapper objectMapper, OpenAiProviderProperties properties) {
        this(HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build(), objectMapper, properties);
    }

    OpenAiHttpClient(HttpClient httpClient, ObjectMapper objectMapper, OpenAiProviderProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    JsonNode postJson(String path, ObjectNode body) {
        try {
            HttpRequest request = baseRequest(path)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerException(response.statusCode(), response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(AiProviderErrorType.TIMEOUT, "OpenAI request was interrupted");
        } catch (IOException exception) {
            throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE, "OpenAI request failed: " + exception.getMessage());
        }
    }

    JsonNode postMultipart(String path, Map<String, String> fields, byte[] fileContent, String filename, String contentType) {
        String boundary = "----english-tutor-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, fields, fileContent, filename, contentType);
        try {
            HttpRequest request = baseRequest(path)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerException(response.statusCode(), response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(AiProviderErrorType.TIMEOUT, "OpenAI transcription request was interrupted");
        } catch (IOException exception) {
            throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE, "OpenAI transcription request failed: " + exception.getMessage());
        }
    }

    BinaryResponse postJsonForBytes(String path, ObjectNode body) {
        try {
            HttpRequest request = baseRequest(path)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerException(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
            }
            return new BinaryResponse(
                    response.body(),
                    response.headers().firstValue("Content-Type").orElse("application/octet-stream"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(AiProviderErrorType.TIMEOUT, "OpenAI speech request was interrupted");
        } catch (IOException exception) {
            throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE, "OpenAI speech request failed: " + exception.getMessage());
        }
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder(resolve(path))
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey());
    }

    private URI resolve(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return properties.baseUrl().resolve(properties.baseUrl().getPath() + normalizedPath);
    }

    private AiProviderException providerException(int statusCode, String responseBody) {
        AiProviderErrorType type = statusCode == 408 || statusCode == 429 || statusCode >= 500
                ? AiProviderErrorType.PROVIDER_UNAVAILABLE
                : AiProviderErrorType.INVALID_OUTPUT;
        return new AiProviderException(type, "OpenAI returned HTTP " + statusCode + ": " + sanitize(responseBody));
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "empty response body";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private byte[] multipartBody(String boundary, Map<String, String> fields, byte[] fileContent, String filename, String contentType) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (Map.Entry<String, String> field : fields.entrySet()) {
                output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(("Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(field.getValue().getBytes(StandardCharsets.UTF_8));
                output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(fileContent);
            output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AiProviderException(AiProviderErrorType.UNKNOWN, "Failed to build OpenAI multipart body");
        }
    }

    record BinaryResponse(byte[] body, String contentType) {
    }
}
