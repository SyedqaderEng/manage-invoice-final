package com.manageinvoice.documents;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Stores original documents in a private S3 bucket under {@code ownerId/documentId}. */
@Component
public class S3ObjectStorage implements ObjectStorage, AutoCloseable {
    private final S3Client client;
    private final String bucket;

    public S3ObjectStorage(@Value("${storage.s3.region}") String region,
                           @Value("${storage.s3.bucket}") String bucket,
                           @Value("${storage.s3.profile}") String profile) {
        if (region.isBlank() || profile.isBlank()
            || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new IllegalArgumentException("A valid S3 region, bucket, and AWS profile are required");
        }
        this.client = S3Client.builder()
            .region(software.amazon.awssdk.regions.Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().profileName(profile).build())
            .build();
        this.bucket = bucket;
    }

    @Override
    public String store(UUID ownerId, UUID documentId, byte[] content) throws IOException {
        String key = ownerId + "/" + documentId;
        try {
            client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/octet-stream")
                    .build(),
                RequestBody.fromBytes(content));
            return key;
        } catch (SdkException exception) {
            throw new IOException("Amazon S3 could not store the document", exception);
        }
    }

    @Override
    public byte[] read(String key) throws IOException {
        try (ResponseInputStream<GetObjectResponse> body = client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(validateKey(key))
                .build())) {
            byte[] content = body.readNBytes((int) DocumentService.MAX_FILE_SIZE + 1);
            if (content.length > DocumentService.MAX_FILE_SIZE) {
                throw new IOException("Stored document exceeds the maximum size");
            }
            return content;
        } catch (IOException exception) {
            throw exception;
        } catch (SdkException exception) {
            throw new IOException("Amazon S3 could not read the document", exception);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(validateKey(key))
                .build());
        } catch (SdkException exception) {
            throw new IOException("Amazon S3 could not delete the document", exception);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private static String validateKey(String key) {
        String[] parts = key.split("/", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid object key");
        UUID owner = UUID.fromString(parts[0]);
        UUID document = UUID.fromString(parts[1]);
        return owner + "/" + document;
    }
}
