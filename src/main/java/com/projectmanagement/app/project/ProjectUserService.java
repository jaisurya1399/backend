package com.projectmanagement.app.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

@Service
@Transactional
public class ProjectUserService {

    private final ProjectUserRepository projectUserRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectUserService(ProjectUserRepository projectUserRepository,
            UserRepository userRepository, ProjectRepository projectRepository,
            ProjectAccessService projectAccessService) {
        this.projectUserRepository = projectUserRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<ProjectUserResponse> getAllProjectUsers() {
        return projectUserRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectUserResponse getProjectUserById(Long id) {
        return toResponse(projectUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project user not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public List<ProjectUserResponse> getProjectUsersByProject(Long projectId) {
        Project project = getProject(projectId);
        projectAccessService.requireView(project);
        return projectUserRepository.findByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectUserResponse> getProjectUsersByUser(Long userId) {
        validateUser(userId);
        return projectUserRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectUserResponse> getProjectUsersByProjectAndRole(Long projectId, String role) {
        Project project = getProject(projectId);
        projectAccessService.requireView(project);
        return projectUserRepository.findByProjectIdAndRole(projectId, normalizeRole(role)).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public boolean existsByProjectAndUser(Long projectId, Long userId) {
        Project project = getProject(projectId);
        projectAccessService.requireView(project);
        return projectUserRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public ProjectUserResponse createProjectUser(ProjectUserRequest request) {
        Project project = getProject(request.getProjectId());
        projectAccessService.requireManager(project);
        validateUser(request.getUserId());
        validateRequestRole(request);

        if (projectUserRepository.existsByProjectIdAndUserId(request.getProjectId(), request.getUserId()))
            throw new RuntimeException("User is already assigned to this project");

        User user = getUser(request.getUserId());
        ProjectUser projectUser = ProjectUser.builder()
                .user(user).project(project).role(request.getRole().name())
                .responsibilityRole(normalizeResponsibility(request)).build();
        return toResponse(projectUserRepository.save(projectUser));
    }

    public ProjectUserResponse updateProjectUser(Long id, ProjectUserRequest request) {
        ProjectUser projectUser = projectUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project user not found with id: " + id));
        projectAccessService.requireManager(projectUser.getProject());
        Project project = getProject(request.getProjectId());
        projectAccessService.requireManager(project);
        validateUser(request.getUserId());
        validateRequestRole(request);

        boolean changed = !projectUser.getUser().getId().equals(request.getUserId())
                || !projectUser.getProject().getId().equals(request.getProjectId());
        if (changed && projectUserRepository.existsByProjectIdAndUserIdAndIdNot(
                request.getProjectId(), request.getUserId(), id))
            throw new RuntimeException("User is already assigned to this project");

        projectUser.setUser(getUser(request.getUserId()));
        projectUser.setProject(project);
        projectUser.setRole(request.getRole().name());
        projectUser.setResponsibilityRole(normalizeResponsibility(request));
        return toResponse(projectUserRepository.save(projectUser));
    }

    public void deleteProjectUser(Long id) {
        ProjectUser projectUser = projectUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project user not found with id: " + id));
        projectAccessService.requireManager(projectUser.getProject());
        projectUserRepository.delete(projectUser);
    }

    public void deleteProjectUsersByProject(Long projectId) {
        Project project = getProject(projectId);
        projectAccessService.requireManager(project);
        projectUserRepository.deleteByProjectId(projectId);
    }

    public void deleteProjectUsersByUser(Long userId) {
        validateUser(userId);
        // Global user cleanup remains restricted by the controller's global permission.
        projectUserRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countUsersByProject(Long projectId) {
        Project project = getProject(projectId);
        projectAccessService.requireView(project);
        return projectUserRepository.countByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public long countProjectsByUser(Long userId) {
        validateUser(userId);
        return projectUserRepository.countByUserId(userId);
    }

    private void validateRequestRole(ProjectUserRequest request) {
        if (request.getRole() == ProjectRole.MEMBER && request.getResponsibilityRole() == null)
            request.setResponsibilityRole(MemberResponsibility.DEVELOPER);
        if (request.getRole() != ProjectRole.MEMBER && request.getResponsibilityRole() != null)
            throw new IllegalArgumentException("Responsibility is only allowed for MEMBER access");
    }

    private String normalizeResponsibility(ProjectUserRequest request) {
        return request.getRole() == ProjectRole.MEMBER
                ? request.getResponsibilityRole().name()
                : null;
    }

    private String normalizeRole(String role) {
        if (role == null)
            throw new IllegalArgumentException("Project role is required");
        if ("ADMIN".equalsIgnoreCase(role))
            return ProjectRole.PROJECT_ADMIN.name();
        return role.trim().toUpperCase();
    }

    private Project getProject(Long projectId) {
        if (projectId == null || projectId <= 0)
            throw new IllegalArgumentException("Project ID must be positive");
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    private void validateUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId))
            throw new RuntimeException("User not found with id: " + userId);
    }

    private ProjectUserResponse toResponse(ProjectUser projectUser) {
        User user = projectUser.getUser();
        Project project = projectUser.getProject();
        return ProjectUserResponse.builder()
                .id(projectUser.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .projectId(project != null ? project.getId() : null)
                .projectName(project != null ? project.getName() : null)
                .role(projectUser.getRole())
                .responsibilityRole(projectUser.getResponsibilityRole())
                .createdAt(projectUser.getCreatedAt())
                .updatedAt(projectUser.getUpdatedAt())
                .build();
    }
}
