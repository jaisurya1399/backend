package com.projectmanagement.app.workspace;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.auth.CurrentUserService;
import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {

        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    // ============================================================
    // CREATE WORKSPACE
    // ============================================================

    public WorkspaceResponse create(WorkspaceRequest request) {

        validateRequest(request);

        String slug = request.getSlug()
                .trim()
                .toLowerCase();

        if (workspaceRepository.existsBySlug(slug)) {
            throw new RuntimeException(
                    "Workspace slug already exists");
        }

        User user = currentUserService.getCurrentUser();

        Workspace workspace = Workspace.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .createdBy(user)
                .build();

        workspace = workspaceRepository.save(workspace);

        memberRepository.save(
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(user)
                        .role(WorkspaceRole.OWNER)
                        .build());

        return toResponse(workspace);
    }

    // ============================================================
    // GET MY WORKSPACES
    // ============================================================

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMine() {

        Long userId = currentUserService.getCurrentUserId();

        return memberRepository
                .findByUserId(userId)
                .stream()
                .map(WorkspaceMember::getWorkspace)
                .filter(workspace -> workspace.getDeletedAt() == null)
                .map(this::toResponse)
                .toList();
    }

    // ============================================================
    // GET WORKSPACE BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public WorkspaceResponse getById(Long id) {

        validateWorkspaceId(id);

        return toResponse(requireMember(id));
    }

    // ============================================================
    // UPDATE WORKSPACE
    // ============================================================

    public WorkspaceResponse update(
            Long id,
            WorkspaceRequest request) {

        validateWorkspaceId(id);
        validateRequest(request);

        Workspace workspace = requireManager(id);

        String slug = request.getSlug()
                .trim()
                .toLowerCase();

        if (workspaceRepository.existsBySlugAndIdNot(
                slug,
                id)) {

            throw new RuntimeException(
                    "Workspace slug already exists");
        }

        workspace.setName(
                request.getName().trim());

        workspace.setSlug(slug);

        workspace.setDescription(
                request.getDescription());

        return toResponse(
                workspaceRepository.save(workspace));
    }

    // ============================================================
    // GET WORKSPACE MEMBERS
    // ============================================================

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getMembers(
            Long workspaceId) {

        validateWorkspaceId(workspaceId);

        requireMember(workspaceId);

        return memberRepository
                .findByWorkspaceId(workspaceId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    // ============================================================
    // ADD WORKSPACE MEMBER
    // ============================================================

    public WorkspaceMemberResponse addMember(
            Long workspaceId,
            WorkspaceMemberRequest request) {

        validateWorkspaceId(workspaceId);

        Workspace workspace = requireManager(workspaceId);

        if (request == null ||
                request.getUserId() == null) {

            throw new IllegalArgumentException(
                    "User ID is required");
        }

        if (request.getRole() == null) {

            throw new IllegalArgumentException(
                    "Workspace role is required");
        }

        if (memberRepository
                .existsByWorkspaceIdAndUserId(
                        workspaceId,
                        request.getUserId())) {

            throw new RuntimeException(
                    "User is already a workspace member");
        }

        User user = userRepository
                .findById(request.getUserId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException(
                        "User not found"));

        if (request.getRole() == WorkspaceRole.OWNER) {

            throw new RuntimeException(
                    "Use ownership transfer to assign an owner");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(request.getRole())
                .build();

        return toMemberResponse(
                memberRepository.save(member));
    }

    // ============================================================
    // UPDATE WORKSPACE MEMBER
    // ============================================================

    public WorkspaceMemberResponse updateMember(
            Long workspaceId,
            Long userId,
            WorkspaceMemberRequest request) {

        validateWorkspaceId(workspaceId);

        requireManager(workspaceId);

        if (userId == null) {

            throw new IllegalArgumentException(
                    "User ID is required");
        }

        if (request == null ||
                request.getUserId() == null) {

            throw new IllegalArgumentException(
                    "User ID is required");
        }

        if (!userId.equals(request.getUserId())) {

            throw new RuntimeException(
                    "A membership cannot be reassigned to another user");
        }

        if (request.getRole() == null) {

            throw new IllegalArgumentException(
                    "Workspace role is required");
        }

        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        userId)
                .orElseThrow(() -> new RuntimeException(
                        "Workspace member not found"));

        if (member.getRole() == WorkspaceRole.OWNER ||
                request.getRole() == WorkspaceRole.OWNER) {

            throw new RuntimeException(
                    "Use ownership transfer to change the owner role");
        }

        member.setRole(request.getRole());

        return toMemberResponse(
                memberRepository.save(member));
    }

    // ============================================================
    // REMOVE WORKSPACE MEMBER
    // ============================================================

    public void removeMember(
            Long workspaceId,
            Long userId) {

        validateWorkspaceId(workspaceId);

        requireManager(workspaceId);

        if (userId == null) {

            throw new IllegalArgumentException(
                    "User ID is required");
        }

        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        userId)
                .orElseThrow(() -> new RuntimeException(
                        "Workspace member not found"));

        if (member.getRole() == WorkspaceRole.OWNER) {

            throw new RuntimeException(
                    "Transfer ownership before removing the owner");
        }

        memberRepository.delete(member);
    }

    // ============================================================
    // TRANSFER WORKSPACE OWNERSHIP
    // ============================================================

    public void transferOwnership(
            Long workspaceId,
            Long newOwnerId) {

        validateWorkspaceId(workspaceId);

        if (newOwnerId == null) {

            throw new IllegalArgumentException(
                    "New owner ID is required");
        }

        /*
         * Ownership transfer must always be performed by
         * the current workspace owner.
         *
         * Global ADMIN is intentionally not allowed to
         * bypass this rule.
         */
        Workspace workspace = requireMember(workspaceId);

        Long currentUserId = currentUserService.getCurrentUserId();

        WorkspaceMember currentOwner = memberRepository
                .findByWorkspaceIdAndUserId(
                        workspace.getId(),
                        currentUserId)
                .orElseThrow(() -> new RuntimeException(
                        "Workspace member not found"));

        if (currentOwner.getRole() != WorkspaceRole.OWNER) {

            throw new RuntimeException(
                    "Only the workspace owner can transfer ownership");
        }

        WorkspaceMember newOwner = memberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        newOwnerId)
                .orElseThrow(() -> new RuntimeException(
                        "New owner must already be a workspace member"));

        if (newOwner.getRole() == WorkspaceRole.OWNER) {
            return;
        }

        currentOwner.setRole(
                WorkspaceRole.ADMIN);

        newOwner.setRole(
                WorkspaceRole.OWNER);

        memberRepository.save(currentOwner);
        memberRepository.save(newOwner);
    }

    // ============================================================
    // REQUIRE WORKSPACE MEMBER
    // ============================================================

    private Workspace requireMember(
            Long workspaceId) {

        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .filter(w -> w.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException(
                        "Workspace not found"));

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        /*
         * Global ADMIN can access any active workspace.
         */
        if (isAdmin(authentication)) {
            return workspace;
        }

        Long currentUserId = currentUserService.getCurrentUserId();

        if (!memberRepository
                .existsByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUserId)) {

            throw new RuntimeException(
                    "You do not have access to this workspace");
        }

        return workspace;
    }

    // ============================================================
    // REQUIRE WORKSPACE MANAGER
    // ============================================================

    private Workspace requireManager(
            Long workspaceId) {

        Workspace workspace = requireMember(workspaceId);

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        /*
         * Global ADMIN can manage any workspace.
         */
        if (isAdmin(authentication)) {
            return workspace;
        }

        Long currentUserId = currentUserService.getCurrentUserId();

        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(
                        workspaceId,
                        currentUserId)
                .orElseThrow(() -> new RuntimeException(
                        "You do not have access to this workspace"));

        WorkspaceRole role = member.getRole();

        if (role != WorkspaceRole.OWNER &&
                role != WorkspaceRole.ADMIN) {

            throw new RuntimeException(
                    "Workspace admin access is required");
        }

        return workspace;
    }

    // ============================================================
    // CHECK GLOBAL ADMIN
    // ============================================================

    private boolean isAdmin(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(
                        authority.getAuthority()));
    }

    // ============================================================
    // VALIDATE WORKSPACE ID
    // ============================================================

    private void validateWorkspaceId(
            Long workspaceId) {

        if (workspaceId == null ||
                workspaceId <= 0) {

            throw new IllegalArgumentException(
                    "Workspace ID must be greater than zero");
        }
    }

    // ============================================================
    // VALIDATE REQUEST
    // ============================================================

    private void validateRequest(
            WorkspaceRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Workspace request is required");
        }

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Workspace name is required");
        }

        if (request.getSlug() == null ||
                request.getSlug().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Workspace slug is required");
        }
    }

    // ============================================================
    // WORKSPACE RESPONSE
    // ============================================================

    private WorkspaceResponse toResponse(
            Workspace workspace) {

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .description(workspace.getDescription())
                .createdById(
                        workspace.getCreatedBy() == null
                                ? null
                                : workspace.getCreatedBy().getId())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    // ============================================================
    // MEMBER RESPONSE
    // ============================================================

    private WorkspaceMemberResponse toMemberResponse(
            WorkspaceMember member) {

        return WorkspaceMemberResponse.builder()
                .id(member.getId())
                .workspaceId(
                        member.getWorkspace().getId())
                .userId(
                        member.getUser().getId())
                .userName(
                        member.getUser().getName())
                .userEmail(
                        member.getUser().getEmail())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}