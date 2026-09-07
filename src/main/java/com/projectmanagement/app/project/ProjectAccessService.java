package com.projectmanagement.app.project;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.projectmanagement.app.auth.CurrentUserService;
import com.projectmanagement.app.workspace.WorkspaceMemberRepository;

@Service
@Transactional(readOnly = true)
public class ProjectAccessService {
    private final CurrentUserService currentUserService;
    private final ProjectUserRepository projectUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    public ProjectAccessService(CurrentUserService currentUserService, ProjectUserRepository projectUserRepository, WorkspaceMemberRepository workspaceMemberRepository) {
        this.currentUserService = currentUserService; this.projectUserRepository = projectUserRepository; this.workspaceMemberRepository = workspaceMemberRepository;
    }
    public boolean canView(Project project) {
        if (isSystemAdmin()) return true;
        Long userId = currentUserService.getCurrentUserId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), userId)) return false;
        return project.getOwner().getId().equals(userId) || projectUserRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }
    public void requireView(Project project) { if (!canView(project)) throw new RuntimeException("You do not have access to this project"); }
    public void requireEditor(Project project) { requireRole(project, ProjectRole.MEMBER); }
    public void requireManager(Project project) { requireRole(project, ProjectRole.ADMIN); }
    private void requireRole(Project project, ProjectRole minimumRole) {
        if (isSystemAdmin()) return;
        Long userId = currentUserService.getCurrentUserId();
        requireView(project);
        if (project.getOwner().getId().equals(userId)) return;
        ProjectRole role = projectUserRepository.findByProjectIdAndUserId(project.getId(), userId)
                .map(member -> ProjectRole.valueOf(member.getRole())).orElse(null);
        boolean allowed = minimumRole == ProjectRole.MEMBER ? role == ProjectRole.ADMIN || role == ProjectRole.MEMBER : role == ProjectRole.ADMIN;
        if (!allowed) throw new RuntimeException(minimumRole == ProjectRole.ADMIN ? "Project admin access is required" : "Project edit access is required");
    }
    private boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
