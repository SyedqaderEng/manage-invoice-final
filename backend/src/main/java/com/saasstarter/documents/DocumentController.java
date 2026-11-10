package com.saasstarter.documents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.saasstarter.common.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    @GetMapping
    public Page<DocumentResponse> list(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return documents.list(principal.id(), pageable);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documents.upload(principal.id(), file));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        DocumentService.OwnedDocument owned = documents.findOwned(principal.id(), id);
        try {
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(owned.document().getMimeType()))
                .contentLength(owned.document().getFileSize())
                .cacheControl(CacheControl.noStore())
                .body(new InputStreamResource(java.nio.file.Files.newInputStream(owned.path())));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be read securely");
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        DocumentService.OwnedDocument owned = documents.findOwned(principal.id(), id);
        try {
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(owned.document().getMimeType()))
                .contentLength(owned.document().getFileSize())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(owned.document().getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(java.nio.file.Files.newInputStream(owned.path())));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be read securely");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        documents.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
