package com.manageinvoice.documents;

import java.io.IOException;
import java.util.UUID;

public interface ObjectStorage {
    String store(UUID ownerId, UUID documentId, byte[] content) throws IOException;
    byte[] read(String key) throws IOException;
    void delete(String key) throws IOException;
}
