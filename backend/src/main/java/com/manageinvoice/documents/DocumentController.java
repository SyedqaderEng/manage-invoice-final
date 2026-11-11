package com.manageinvoice.documents;

import java.io.ByteArrayInputStream;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.manageinvoice.common.security.AuthenticatedUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService documents;
    private final DigitalReceiptService receipts;

    public DocumentController(DocumentService documents, DigitalReceiptService receipts) {
        this.documents = documents;
        this.receipts = receipts;
    }

    @GetMapping("/records/confirmed")
    public Page<DigitalReceiptService.SummaryRecord> confirmedRecords(
        @AuthenticationPrincipal AuthenticatedUser principal, @RequestParam(defaultValue = "0") int page
    ) {
        return receipts.confirmed(principal.id(), page);
    }

    @GetMapping("/{id}/record")
    public DigitalReceiptService.ReceiptResponse receipt(
        @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id
    ) {
        return receipts.get(principal.id(), id);
    }

    @PostMapping("/{id}/record")
    public DigitalReceiptService.ReceiptResponse createReceipt(
        @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
        @RequestParam(defaultValue = "true") boolean recognize
    ) {
        return receipts.create(principal.id(), id, recognize);
    }

    @PutMapping("/{id}/record")
    public DigitalReceiptService.ReceiptResponse saveReceipt(
        @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id,
        @Valid @RequestBody DigitalReceiptService.SaveRequest request
    ) {
        return receipts.save(principal.id(), id, request);
    }

    @PostMapping("/{id}/record/suggestions")
    public ReceiptData suggestReceipt(
        @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id
    ) {
        return receipts.suggest(principal.id(), id);
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
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(owned.document().getMimeType()))
                .contentLength(owned.document().getFileSize())
                .cacheControl(CacheControl.noStore())
                .body(new InputStreamResource(new ByteArrayInputStream(documents.read(owned))));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        DocumentService.OwnedDocument owned = documents.findOwned(principal.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(owned.document().getMimeType()))
                .contentLength(owned.document().getFileSize())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(owned.document().getOriginalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(new ByteArrayInputStream(documents.read(owned))));
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
