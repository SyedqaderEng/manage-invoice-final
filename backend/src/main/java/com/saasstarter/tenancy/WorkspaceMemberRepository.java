package com.saasstarter.tenancy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    List<WorkspaceMember> findByUserIdAndStatus(UUID userId, WorkspaceStatus status);
    List<WorkspaceMember> findByWorkspaceIdAndStatus(UUID workspaceId, WorkspaceStatus status);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndStatus(UUID workspaceId, UUID userId, WorkspaceStatus status);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
