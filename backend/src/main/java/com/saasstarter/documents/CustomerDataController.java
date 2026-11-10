package com.saasstarter.documents;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saasstarter.common.security.AuthenticatedUser;
import com.saasstarter.documents.CustomerDataService.CategoryRequest;
import com.saasstarter.documents.CustomerDataService.CategoryResponse;
import com.saasstarter.documents.CustomerDataService.VendorRequest;
import com.saasstarter.documents.CustomerDataService.VendorResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
public class CustomerDataController {
    private final CustomerDataService customerData;

    public CustomerDataController(CustomerDataService customerData) {
        this.customerData = customerData;
    }

    @GetMapping("/api/v1/categories")
    public List<CategoryResponse> categories(@AuthenticationPrincipal AuthenticatedUser principal) {
        return customerData.listCategories(principal.id());
    }

    @PostMapping("/api/v1/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateCategoryRequest request
    ) {
        return customerData.createCategory(principal.id(),
            new CategoryRequest(request.name(), request.description(), request.color()));
    }

    @GetMapping("/api/v1/vendors")
    public List<VendorResponse> vendors(@AuthenticationPrincipal AuthenticatedUser principal) {
        return customerData.listVendors(principal.id());
    }

    @PostMapping("/api/v1/vendors")
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse createVendor(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateVendorRequest request
    ) {
        return customerData.createVendor(principal.id(),
            new VendorRequest(request.displayName(), request.email(), request.phone(), request.address(), request.defaultCategoryId()));
    }

    public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color
    ) {}

    public record CreateVendorRequest(
        @NotBlank @Size(max = 255) String displayName,
        @Email @Size(max = 320) String email,
        @Size(max = 50) String phone,
        @Size(max = 2000) String address,
        UUID defaultCategoryId
    ) {}
}
