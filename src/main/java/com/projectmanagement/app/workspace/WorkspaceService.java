package com.projectmanagement.app.workspace;

import java.util.List;
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

    public WorkspaceService(WorkspaceRepository workspaceRepository, WorkspaceMemberRepository memberRepository,
            UserRepository userRepository, CurrentUserService currentUserService) {
        this.workspaceRepository = workspaceRepository; this.memberRepository = memberRepository;
        this.userRepository = userRepository; this.currentUserService = currentUserService;
    }

    public WorkspaceResponse create(WorkspaceRequest request) {
        String slug = request.getSlug().trim().toLowerCase();
        if (workspaceRepository.existsBySlug(slug)) throw new RuntimeException("Workspace slug already exists");
        User user = currentUserService.getCurrentUser();
        Workspace workspace = workspaceRepository.save(Workspace.builder().name(request.getName().trim()).slug(slug)
                .description(request.getDescription()).createdBy(user).build());
        memberRepository.save(WorkspaceMember.builder().workspace(workspace).user(user).role(WorkspaceRole.OWNER).build());
        return toResponse(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMine() {
        return memberRepository.findByUserId(currentUserService.getCurrentUserId()).stream()
                .map(WorkspaceMember::getWorkspace).filter(w -> w.getDeletedAt() == null).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getById(Long id) { return toResponse(requireMember(id)); }

    public WorkspaceResponse update(Long id, WorkspaceRequest request) {
        Workspace workspace = requireManager(id);
        String slug = request.getSlug().trim().toLowerCase();
        if (workspaceRepository.existsBySlugAndIdNot(slug, id)) throw new RuntimeException("Workspace slug already exists");
        workspace.setName(request.getName().trim()); workspace.setSlug(slug); workspace.setDescription(request.getDescription());
        return toResponse(workspaceRepository.save(workspace));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getMembers(Long workspaceId) {
        requireMember(workspaceId);
        return memberRepository.findByWorkspaceId(workspaceId).stream().map(this::toMemberResponse).toList();
    }

    public WorkspaceMemberResponse addMember(Long workspaceId, WorkspaceMemberRequest request) {
        Workspace workspace = requireManager(workspaceId);
        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getUserId())) throw new RuntimeException("User is already a workspace member");
        User user = userRepository.findById(request.getUserId()).filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getRole() == WorkspaceRole.OWNER) throw new RuntimeException("Use ownership transfer to assign an owner");
        return toMemberResponse(memberRepository.save(WorkspaceMember.builder().workspace(workspace).user(user).role(request.getRole()).build()));
    }

    public WorkspaceMemberResponse updateMember(Long workspaceId, Long userId, WorkspaceMemberRequest request) {
        requireManager(workspaceId);
        if (!userId.equals(request.getUserId())) throw new RuntimeException("A membership cannot be reassigned to another user");
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("Workspace member not found"));
        if (member.getRole() == WorkspaceRole.OWNER || request.getRole() == WorkspaceRole.OWNER)
            throw new RuntimeException("Use ownership transfer to change the owner role");
        member.setRole(request.getRole()); return toMemberResponse(memberRepository.save(member));
    }

    public void removeMember(Long workspaceId, Long userId) {
        requireManager(workspaceId);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("Workspace member not found"));
        if (member.getRole() == WorkspaceRole.OWNER) throw new RuntimeException("Transfer ownership before removing the owner");
        memberRepository.delete(member);
    }

    public void transferOwnership(Long workspaceId, Long newOwnerId) {
        requireMember(workspaceId);
        Long currentUserId = currentUserService.getCurrentUserId();
        WorkspaceMember currentOwner = memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Workspace member not found"));
        if (currentOwner.getRole() != WorkspaceRole.OWNER) throw new RuntimeException("Only the workspace owner can transfer ownership");
        WorkspaceMember newOwner = memberRepository.findByWorkspaceIdAndUserId(workspaceId, newOwnerId)
                .orElseThrow(() -> new RuntimeException("New owner must already be a workspace member"));
        if (newOwner.getRole() == WorkspaceRole.OWNER) return;
        currentOwner.setRole(WorkspaceRole.ADMIN);
        newOwner.setRole(WorkspaceRole.OWNER);
        memberRepository.save(currentOwner);
        memberRepository.save(newOwner);
    }

    private Workspace requireMember(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).filter(w -> w.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserService.getCurrentUserId()))
            throw new RuntimeException("You do not have access to this workspace");
        return workspace;
    }
    private Workspace requireManager(Long workspaceId) {
        Workspace workspace = requireMember(workspaceId);
        WorkspaceRole role = memberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserService.getCurrentUserId()).orElseThrow().getRole();
        if (role != WorkspaceRole.OWNER && role != WorkspaceRole.ADMIN) throw new RuntimeException("Workspace admin access is required");
        return workspace;
    }
    private WorkspaceResponse toResponse(Workspace w) { return WorkspaceResponse.builder().id(w.getId()).name(w.getName()).slug(w.getSlug()).description(w.getDescription()).createdById(w.getCreatedBy() == null ? null : w.getCreatedBy().getId()).createdAt(w.getCreatedAt()).updatedAt(w.getUpdatedAt()).build(); }
    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember m) { return WorkspaceMemberResponse.builder().id(m.getId()).workspaceId(m.getWorkspace().getId()).userId(m.getUser().getId()).userName(m.getUser().getName()).userEmail(m.getUser().getEmail()).role(m.getRole()).createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt()).build(); }
}
