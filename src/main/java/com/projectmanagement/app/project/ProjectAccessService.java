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

    public ProjectAccessService(
            CurrentUserService currentUserService,
            ProjectUserRepository projectUserRepository,
            WorkspaceMemberRepository workspaceMemberRepository) {

        this.currentUserService = currentUserService;
        this.projectUserRepository = projectUserRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Checks whether the current user can view the project.
     *
     * System administrators can access all projects.
     * Other users must first belong to the project's workspace and then
     * either own the project or be explicitly assigned to the project.
     */
    public boolean canView(Project project) {
        if (project == null) {
            return false;
        }

        if (isSystemAdmin()) {
            return true;
        }

        if (project.getWorkspace() == null || project.getWorkspace().getId() == null) {
            return false;
        }

        if (project.getOwner() == null || project.getOwner().getId() == null) {
            return false;
        }

        Long userId = currentUserService.getCurrentUserId();

        if (userId == null) {
            return false;
        }

        Long workspaceId = project.getWorkspace().getId();

        boolean workspaceMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId,
                userId);

        if (!workspaceMember) {
            return false;
        }

        boolean isOwner = project.getOwner().getId().equals(userId);

        boolean isProjectMember = projectUserRepository.existsByProjectIdAndUserId(
                project.getId(),
                userId);

        return isOwner || isProjectMember;
    }

    /**
     * Requires the current user to have view access to the project.
     */
    public void requireView(Project project) {
        if (!canView(project)) {
            throw new RuntimeException(
                    "You do not have access to this project");
        }
    }

    /**
     * Requires project MEMBER or ADMIN access.
     *
     * Project owner is always allowed.
     */
    public void requireEditor(Project project) {
        requireRole(project, ProjectRole.MEMBER);
    }

    /**
     * Requires project ADMIN access.
     *
     * Project owner is always allowed.
     */
    public void requireManager(Project project) {
        requireRole(project, ProjectRole.ADMIN);
    }

    private void requireRole(
            Project project,
            ProjectRole minimumRole) {

        if (project == null) {
            throw new RuntimeException("Project is required");
        }

        if (isSystemAdmin()) {
            return;
        }

        Long userId = currentUserService.getCurrentUserId();

        if (userId == null) {
            throw new RuntimeException("Authenticated user is required");
        }

        requireView(project);

        if (project.getOwner() != null
                && project.getOwner().getId() != null
                && project.getOwner().getId().equals(userId)) {
            return;
        }

        ProjectRole role = projectUserRepository
                .findByProjectIdAndUserId(project.getId(), userId)
                .map(member -> {
                    try {
                        return ProjectRole.valueOf(member.getRole());
                    } catch (IllegalArgumentException | NullPointerException ex) {
                        return null;
                    }
                })
                .orElse(null);

        boolean allowed;

        if (minimumRole == ProjectRole.MEMBER) {
            allowed = role == ProjectRole.ADMIN
                    || role == ProjectRole.MEMBER;
        } else {
            allowed = role == ProjectRole.ADMIN;
        }

        if (!allowed) {
            if (minimumRole == ProjectRole.ADMIN) {
                throw new RuntimeException(
                        "Project admin access is required");
            }

            throw new RuntimeException(
                    "Project edit access is required");
        }
    }

    /**
     * Checks whether the authenticated user has global administrator access.
     */
    private boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(
                                authority.getAuthority()));
    }
}