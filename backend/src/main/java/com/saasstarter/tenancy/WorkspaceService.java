package com.saasstarter.tenancy;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.saasstarter.identity.User;
import com.saasstarter.identity.UserRepository;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final UserRepository users;

    public WorkspaceService(WorkspaceRepository workspaces, WorkspaceMemberRepository members, UserRepository users) {
        this.workspaces = workspaces;
        this.members = members;
        this.users = users;
    }

    public List<WorkspaceResponse> list(UUID userId) {
        return members.findByUserIdAndStatus(userId, WorkspaceStatus.ACTIVE).stream()
            .map(member -> new WorkspaceResponse(member.getWorkspace(), member.getRole()))
            .toList();
    }

    @Transactional
    public WorkspaceResponse create(UUID ownerId, CreateWorkspaceRequest request) {
        User owner = users.findById(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        Workspace workspace = new Workspace();
        workspace.setName(request.name().trim());
        workspace.setWorkspaceType(request.workspaceType());
        workspace.setOwner(owner);
        workspaces.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(owner);
        member.setRole(WorkspaceRole.OWNER);
        members.save(member);
        return new WorkspaceResponse(workspace, WorkspaceRole.OWNER);
    }

    public Workspace requireMembership(UUID workspaceId, UUID userId) {
        return members.findByWorkspaceIdAndUserIdAndStatus(workspaceId, userId, WorkspaceStatus.ACTIVE)
            .map(WorkspaceMember::getWorkspace)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace access denied"));
    }

    public Workspace findWorkspace(UUID workspaceId) {
        return workspaces.findById(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
    }

    public WorkspaceRole requireRole(UUID workspaceId, UUID userId) {
        return members.findByWorkspaceIdAndUserIdAndStatus(workspaceId, userId, WorkspaceStatus.ACTIVE)
            .map(WorkspaceMember::getRole)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace access denied"));
    }

    public List<MemberResponse> members(UUID workspaceId, UUID userId) {
        requireMembership(workspaceId, userId);
        return members.findByWorkspaceIdAndStatus(workspaceId, WorkspaceStatus.ACTIVE).stream()
            .map(MemberResponse::new)
            .toList();
    }

    @Transactional
    public MemberResponse addMember(UUID workspaceId, UUID requesterId, String email, WorkspaceRole role) {
        WorkspaceRole requesterRole = requireRole(workspaceId, requesterId);
        requireAdmin(requesterRole);
        if (role == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace ownership cannot be assigned");
        }
        if (role == WorkspaceRole.ADMIN && requesterRole != WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can assign administrator access");
        }
        User user = users.findByEmailIgnoreCase(email.trim().toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Create an account for this email before adding it"));
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember member = members.findByWorkspaceIdAndUserId(workspaceId, user.getId()).orElse(null);
        if (member != null) {
            if (member.getStatus() == WorkspaceStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This user is already a workspace member");
            }
            member.setStatus(WorkspaceStatus.ACTIVE);
            member.setRole(role);
        } else {
            member = new WorkspaceMember();
            member.setWorkspace(workspace);
            member.setUser(user);
            member.setRole(role);
        }
        return new MemberResponse(members.save(member));
    }

    @Transactional
    public MemberResponse updateMemberRole(UUID workspaceId, UUID requesterId, UUID memberId, WorkspaceRole role) {
        WorkspaceRole requesterRole = requireRole(workspaceId, requesterId);
        requireAdmin(requesterRole);
        if (role == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace ownership cannot be assigned");
        }
        WorkspaceMember member = findMember(workspaceId, memberId);
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workspace owner role cannot be changed here");
        }
        if (requesterRole != WorkspaceRole.OWNER
            && (role == WorkspaceRole.ADMIN || member.getRole() == WorkspaceRole.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can manage administrator access");
        }
        member.setRole(role);
        return new MemberResponse(members.save(member));
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID requesterId, UUID memberId) {
        WorkspaceRole requesterRole = requireRole(workspaceId, requesterId);
        requireAdmin(requesterRole);
        WorkspaceMember member = findMember(workspaceId, memberId);
        if (member.getRole() == WorkspaceRole.OWNER || member.getUser().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The owner cannot be removed from this workspace");
        }
        if (requesterRole != WorkspaceRole.OWNER && member.getRole() == WorkspaceRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can remove an administrator");
        }
        member.setStatus(WorkspaceStatus.ARCHIVED);
        members.save(member);
    }

    private WorkspaceMember findMember(UUID workspaceId, UUID memberId) {
        WorkspaceMember member = members.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace member not found"));
        if (!member.getWorkspace().getId().equals(workspaceId) || member.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace member not found");
        }
        return member;
    }

    private void requireAdmin(WorkspaceRole role) {
        if (role != WorkspaceRole.OWNER && role != WorkspaceRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace administrator access required");
        }
    }

    public record CreateWorkspaceRequest(String name, WorkspaceType workspaceType) {}
    public record WorkspaceResponse(UUID id, String name, WorkspaceType workspaceType, String defaultCurrency, WorkspaceRole role) {
        WorkspaceResponse(Workspace workspace, WorkspaceRole role) {
            this(workspace.getId(), workspace.getName(), workspace.getWorkspaceType(), workspace.getDefaultCurrency(), role);
        }
    }
    public record MemberResponse(UUID id, UUID userId, String email, WorkspaceRole role) {
        MemberResponse(WorkspaceMember member) {
            this(member.getId(), member.getUser().getId(), member.getUser().getEmail(), member.getRole());
        }
    }
}
