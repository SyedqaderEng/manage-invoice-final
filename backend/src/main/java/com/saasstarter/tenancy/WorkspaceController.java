package com.saasstarter.tenancy;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saasstarter.common.security.AuthenticatedUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<WorkspaceService.WorkspaceResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return workspaceService.list(principal.id());
    }

    @PostMapping
    public WorkspaceService.WorkspaceResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.create(principal.id(), new WorkspaceService.CreateWorkspaceRequest(request.name(), request.workspaceType()));
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceService.MemberResponse> members(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable UUID workspaceId) {
        return workspaceService.members(workspaceId, principal.id());
    }

    @PostMapping("/{workspaceId}/members")
    public WorkspaceService.MemberResponse addMember(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable UUID workspaceId,
                                                      @Valid @RequestBody AddMemberRequest request) {
        return workspaceService.addMember(workspaceId, principal.id(), request.email(), request.role());
    }

    @PatchMapping("/{workspaceId}/members/{memberId}")
    public WorkspaceService.MemberResponse updateMemberRole(@AuthenticationPrincipal AuthenticatedUser principal,
                                                            @PathVariable UUID workspaceId,
                                                            @PathVariable UUID memberId,
                                                            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return workspaceService.updateMemberRole(workspaceId, principal.id(), memberId, request.role());
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable UUID workspaceId,
                                             @PathVariable UUID memberId) {
        workspaceService.removeMember(workspaceId, principal.id(), memberId);
        return ResponseEntity.noContent().build();
    }

    public record CreateWorkspaceRequest(@NotBlank String name, @NotNull WorkspaceType workspaceType) {}
    public record AddMemberRequest(@Email @NotBlank String email, @NotNull WorkspaceRole role) {}
    public record UpdateMemberRoleRequest(@NotNull WorkspaceRole role) {}
}
