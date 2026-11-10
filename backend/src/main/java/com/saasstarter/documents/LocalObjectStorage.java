package com.saasstarter.documents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.nio.file.attribute.PosixFilePermissions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalObjectStorage {
    private final Path root;

    public LocalObjectStorage(@Value("${storage.local.root:./storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public String store(UUID ownerId, UUID documentId, byte[] content) throws IOException {
        String key = ownerId + "/" + documentId;
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(target.getParent(), PosixFilePermissions.fromString("rwx------"));
        Files.createFile(target, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            Files.write(target, content, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
        return key;
    }

    public Path locate(String key) {
        return resolve(key);
    }

    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid private object key");
        }
        return resolved;
    }
}
