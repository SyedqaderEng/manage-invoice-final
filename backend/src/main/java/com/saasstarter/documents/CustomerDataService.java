package com.saasstarter.documents;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.saasstarter.identity.User;
import com.saasstarter.identity.UserRepository;

@Service
public class CustomerDataService {
    private final CategoryRepository categories;
    private final VendorRepository vendors;
    private final UserRepository users;

    public CustomerDataService(CategoryRepository categories, VendorRepository vendors, UserRepository users) {
        this.categories = categories;
        this.vendors = vendors;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(UUID ownerId) {
        return categories.findByOwnerIdAndActiveTrueOrderByNameAsc(ownerId).stream()
            .map(CategoryResponse::new).toList();
    }

    @Transactional
    public CategoryResponse createCategory(UUID ownerId, CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByOwnerIdAndNameIgnoreCase(ownerId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
        }
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setOwner(findUser(ownerId));
        category.setName(name);
        category.setDescription(blankToNull(request.description()));
        category.setColor(request.color());
        return new CategoryResponse(categories.save(category));
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> listVendors(UUID ownerId) {
        return vendors.findByOwnerIdOrderByDisplayNameAsc(ownerId).stream()
            .map(VendorResponse::new).toList();
    }

    @Transactional
    public VendorResponse createVendor(UUID ownerId, VendorRequest request) {
        String displayName = request.displayName().trim();
        String normalizedName = normalize(displayName);
        if (vendors.existsByOwnerIdAndNormalizedName(ownerId, normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vendor with this name already exists");
        }
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setOwner(findUser(ownerId));
        vendor.setDisplayName(displayName);
        vendor.setNormalizedName(normalizedName);
        vendor.setEmail(blankToNull(request.email()));
        vendor.setPhone(blankToNull(request.phone()));
        vendor.setAddress(blankToNull(request.address()));
        if (request.defaultCategoryId() != null) {
            vendor.setDefaultCategory(categories.findByIdAndOwnerIdAndActiveTrue(request.defaultCategoryId(), ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found for this account")));
        }
        return new VendorResponse(vendors.save(vendor));
    }

    private User findUser(UUID ownerId) {
        return users.findById(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CategoryRequest(String name, String description, String color) {}
    public record VendorRequest(String displayName, String email, String phone, String address, UUID defaultCategoryId) {}
    public record CategoryResponse(UUID id, String name, String description, String color, String categoryType) {
        public CategoryResponse(Category category) {
            this(category.getId(), category.getName(), category.getDescription(), category.getColor(), category.getCategoryType());
        }
    }
    public record VendorResponse(UUID id, String displayName, String email, String phone, String address, UUID defaultCategoryId) {
        public VendorResponse(Vendor vendor) {
            this(vendor.getId(), vendor.getDisplayName(), vendor.getEmail(), vendor.getPhone(), vendor.getAddress(),
                vendor.getDefaultCategory() == null ? null : vendor.getDefaultCategory().getId());
        }
    }
}
