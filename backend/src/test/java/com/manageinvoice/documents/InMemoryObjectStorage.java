package com.manageinvoice.documents;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-process stand-in for Supabase Storage used by the test profile. */
public class InMemoryObjectStorage implements ObjectStorage {
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public String store(UUID ownerId, UUID documentId, byte[] content) throws IOException {
        String key = ownerId + "/" + documentId;
        if (objects.putIfAbsent(key, content.clone()) != null) throw new IOException("Object already exists");
        return key;
    }

    @Override
    public byte[] read(String key) throws IOException {
        byte[] content = objects.get(key);
        if (content == null) throw new IOException("Object not found");
        return content.clone();
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }
}
