package com.manageinvoice.documents;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stores original documents in a private Supabase Storage bucket under {@code ownerId/documentId}. */
@Component
public class SupabaseObjectStorage implements ObjectStorage {
    private final HttpClient client;
    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;

    public SupabaseObjectStorage(@Value("${security.supabase.url}") String url,
                                 @Value("${storage.supabase.service-key}") String serviceKey,
                                 @Value("${storage.supabase.bucket}") String bucket) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            || !uri.getHost().endsWith(".supabase.co") || uri.getUserInfo() != null
            || uri.getQuery() != null || uri.getFragment() != null
            || (!uri.getPath().isEmpty() && !"/".equals(uri.getPath()))
            || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("Invalid Supabase project URL");
        }
        if (serviceKey.isBlank() || !bucket.matches("[a-z0-9][a-z0-9-]{2,62}")) {
            throw new IllegalArgumentException("A service key and valid private storage bucket are required");
        }
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    @Override
    public String store(UUID ownerId, UUID documentId, byte[] content) throws IOException {
        String key = ownerId + "/" + documentId;
        HttpRequest request = request(key)
            .header("Content-Type", "application/octet-stream")
            .header("x-upsert", "false")
            .POST(HttpRequest.BodyPublishers.ofByteArray(content)).build();
        send(request, HttpResponse.BodyHandlers.discarding());
        return key;
    }

    @Override
    public byte[] read(String key) throws IOException {
        HttpResponse<java.io.InputStream> response = send(request(key).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        try (var body = response.body()) {
            byte[] content = body.readNBytes((int) DocumentService.MAX_FILE_SIZE + 1);
            if (content.length > DocumentService.MAX_FILE_SIZE) {
                throw new IOException("Stored document exceeds the maximum size");
            }
            return content;
        }
    }

    @Override
    public void delete(String key) throws IOException {
        String validKey = validateKey(key);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/storage/v1/object/" + bucket))
            .timeout(Duration.ofSeconds(45))
            .header("apikey", serviceKey).header("Authorization", "Bearer " + serviceKey)
            .header("Content-Type", "application/json")
            .method("DELETE", HttpRequest.BodyPublishers.ofString("{\"prefixes\":[\"" + validKey + "\"]}"))
            .build();
        send(request, HttpResponse.BodyHandlers.discarding());
    }

    private HttpRequest.Builder request(String key) {
        URI target = URI.create(baseUrl + "/storage/v1/object/" + bucket + "/" + validateKey(key));
        return HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(45))
            .header("apikey", serviceKey).header("Authorization", "Bearer " + serviceKey);
    }

    private static String validateKey(String key) {
        String[] parts = key.split("/", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid object key");
        UUID owner = UUID.fromString(parts[0]);
        UUID document = UUID.fromString(parts[1]);
        return owner + "/" + document;
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) throws IOException {
        try {
            HttpResponse<T> response = client.send(request, bodyHandler);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.body() instanceof java.io.InputStream stream) stream.close();
                throw new IOException("Supabase Storage returned HTTP " + response.statusCode());
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase Storage request was interrupted", exception);
        }
    }
}
