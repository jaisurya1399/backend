package com.projectmanagement.app.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;
import com.projectmanagement.app.audit.AuditService;

@Service
@Transactional
public class ProjectUserService {

        private final ProjectUserRepository projectUserRepository;
        private final UserRepository userRepository;
        private final ProjectRepository projectRepository;
        private final ProjectAccessService projectAccessService;
        private final AuditService auditService;

        public ProjectUserService(
                        ProjectUserRepository projectUserRepository,
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        ProjectAccessService projectAccessService,
                        AuditService auditService) {

                this.projectUserRepository = projectUserRepository;
                this.projectRepository = projectRepository;
                this.userRepository = userRepository;
                this.projectAccessService = projectAccessService;
                this.auditService = auditService;
        }

        // -------------------------------------------------------------------------
        // READ
        // -------------------------------------------------------------------------

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getAllProjectUsers() {

                return projectUserRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public ProjectUserResponse getProjectUserById(Long id) {

                ProjectUser projectUser = projectUserRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                return toResponse(projectUser);
        }

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByProject(
                        Long projectId) {

                Project project = getProject(projectId);

                projectAccessService.requireView(project);

                return projectUserRepository.findByProjectId(projectId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByUser(
                        Long userId) {

                validateUser(userId);

                return projectUserRepository.findByUserId(userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByProjectAndRole(
                        Long projectId,
                        String role) {

                Project project = getProject(projectId);

                projectAccessService.requireView(project);

                return projectUserRepository
                                .findByProjectIdAndRole(
                                                projectId,
                                                normalizeRole(role))
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public boolean existsByProjectAndUser(
                        Long projectId,
                        Long userId) {

                Project project = getProject(projectId);

                projectAccessService.requireView(project);

                return projectUserRepository.existsByProjectIdAndUserId(
                                projectId,
                                userId);
        }

        // -------------------------------------------------------------------------
        // CREATE
        // -------------------------------------------------------------------------

        public ProjectUserResponse createProjectUser(
                        ProjectUserRequest request) {

                validateRequest(request);

                Project project = getProject(request.getProjectId());

                projectAccessService.requireManager(project);

                validateUser(request.getUserId());
                validateRequestRole(request);

                if (projectUserRepository.existsByProjectIdAndUserId(
                                request.getProjectId(),
                                request.getUserId())) {

                        throw new RuntimeException(
                                        "User is already assigned to this project");
                }

                User user = getUser(request.getUserId());

                ProjectUser projectUser = ProjectUser.builder()
                                .user(user)
                                .project(project)
                                .role(normalizeRole(request.getRole().name()))
                                .responsibilityRole(normalizeResponsibility(request))
                                .availabilitySelfUpdateOpen(Boolean.FALSE)
                                .build();

                ProjectUser saved = projectUserRepository.save(projectUser);
                auditService.record(project, null, "PROJECT_MEMBER_ADDED", "PROJECT_USER", saved.getId(),
                                java.util.Map.of("userId", user.getId(), "role", saved.getRole(),
                                                "responsibilityRole", saved.getResponsibilityRole() == null ? "" : saved.getResponsibilityRole()));
                return toResponse(saved);
        }

        // -------------------------------------------------------------------------
        // UPDATE
        // -------------------------------------------------------------------------

        public ProjectUserResponse updateProjectUser(
                        Long id,
                        ProjectUserRequest request) {

                validateRequest(request);

                ProjectUser projectUser = projectUserRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                Project oldProject = projectUser.getProject();

                projectAccessService.requireManager(oldProject);

                Project newProject = getProject(request.getProjectId());

                projectAccessService.requireManager(newProject);

                validateUser(request.getUserId());
                validateRequestRole(request);

                boolean userChanged = !projectUser.getUser().getId()
                                .equals(request.getUserId());

                boolean projectChanged = !projectUser.getProject().getId()
                                .equals(request.getProjectId());

                if ((userChanged || projectChanged)
                                && projectUserRepository
                                                .existsByProjectIdAndUserIdAndIdNot(
                                                                request.getProjectId(),
                                                                request.getUserId(),
                                                                id)) {

                        throw new RuntimeException(
                                        "User is already assigned to this project");
                }

                projectUser.setUser(
                                getUser(request.getUserId()));

                projectUser.setProject(newProject);

                projectUser.setRole(
                                normalizeRole(request.getRole().name()));

                projectUser.setResponsibilityRole(
                                normalizeResponsibility(request));

                /*
                 * Preserve the current availability submission setting.
                 *
                 * If the database contains NULL because this is an older record,
                 * default it to CLOSED.
                 */
                if (projectUser.getAvailabilitySelfUpdateOpen() == null) {
                        projectUser.setAvailabilitySelfUpdateOpen(
                                        Boolean.FALSE);
                }

                ProjectUser saved = projectUserRepository.save(projectUser);
                auditService.record(newProject, null, "PROJECT_MEMBER_UPDATED", "PROJECT_USER", saved.getId(),
                                java.util.Map.of("userId", saved.getUser().getId(), "role", saved.getRole(),
                                                "responsibilityRole", saved.getResponsibilityRole() == null ? "" : saved.getResponsibilityRole()));
                return toResponse(saved);
        }

        // -------------------------------------------------------------------------
        // AVAILABILITY SELF-UPDATE
        // -------------------------------------------------------------------------

        /**
         * Returns whether the selected project member is allowed
         * to submit/update their own availability.
         */
        @Transactional(readOnly = true)
        public boolean isAvailabilitySelfUpdateOpen(
                        Long projectUserId) {

                ProjectUser projectUser = getProjectUser(projectUserId);

                return Boolean.TRUE.equals(
                                projectUser.getAvailabilitySelfUpdateOpen());
        }

        public ProjectUserResponse setAvailabilitySelfUpdateOpen(
                        Long projectUserId,
                        boolean enabled) {

                ProjectUser projectUser = getProjectUser(projectUserId);

                Project project = projectUser.getProject();

                if (project == null) {
                        throw new RuntimeException(
                                        "Project not found for project user: " + projectUserId);
                }

                projectAccessService.requireManager(project);

                projectUser.setAvailabilitySelfUpdateOpen(enabled);

                ProjectUser savedProjectUser = projectUserRepository.save(projectUser);

                return toResponse(savedProjectUser);
        }

        /**
         * Opens availability submission for a project member.
         */
        public ProjectUserResponse openAvailabilitySelfUpdate(
                        Long projectUserId) {

                return setAvailabilitySelfUpdateOpen(
                                projectUserId,
                                true);
        }

        /**
         * Closes availability submission for a project member.
         *
         * Existing availability records are not deleted.
         */
        public ProjectUserResponse closeAvailabilitySelfUpdate(
                        Long projectUserId) {

                return setAvailabilitySelfUpdateOpen(
                                projectUserId,
                                false);
        }

        // -------------------------------------------------------------------------
        // DELETE
        // -------------------------------------------------------------------------

        public void deleteProjectUser(Long id) {

                ProjectUser projectUser = projectUserRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: "
                                                                + id));

                projectAccessService.requireManager(
                                projectUser.getProject());

                auditService.record(projectUser.getProject(), null, "PROJECT_MEMBER_REMOVED", "PROJECT_USER", projectUser.getId(),
                                java.util.Map.of("userId", projectUser.getUser().getId(), "role", projectUser.getRole()));
                projectUserRepository.delete(projectUser);
        }

        public void deleteProjectUsersByProject(
                        Long projectId) {

                Project project = getProject(projectId);

                projectAccessService.requireManager(project);

                projectUserRepository.deleteByProjectId(projectId);
        }

        public void deleteProjectUsersByUser(Long userId) {

                validateUser(userId);

                /*
                 * Global user cleanup remains restricted by
                 * controller/global permission.
                 */
                projectUserRepository.deleteByUserId(userId);
        }

        // -------------------------------------------------------------------------
        // COUNTS
        // -------------------------------------------------------------------------

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

        // -------------------------------------------------------------------------
        // VALIDATION
        // -------------------------------------------------------------------------

        private void validateRequest(
                        ProjectUserRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Project user request is required");
                }

                if (request.getProjectId() == null
                                || request.getProjectId() <= 0) {

                        throw new IllegalArgumentException(
                                        "Project ID must be positive");
                }

                if (request.getUserId() == null
                                || request.getUserId() <= 0) {

                        throw new IllegalArgumentException(
                                        "User ID must be positive");
                }

                if (request.getRole() == null) {
                        throw new IllegalArgumentException(
                                        "Project role is required");
                }
        }

        private void validateRequestRole(
                        ProjectUserRequest request) {

                ProjectRole role = request.getRole();

                /*
                 * MEMBER must have a responsibility.
                 *
                 * If frontend does not send one,
                 * default to Developer.
                 */
                if (role == ProjectRole.MEMBER
                                && request.getResponsibilityRole() == null) {

                        request.setResponsibilityRole(
                                        MemberResponsibility.DEVELOPER);
                }

                /*
                 * PROJECT_ADMIN and VIEWER cannot have
                 * a member responsibility.
                 */
                if (role != ProjectRole.MEMBER
                                && request.getResponsibilityRole() != null) {

                        throw new IllegalArgumentException(
                                        "Responsibility is only allowed for MEMBER access");
                }
        }

        private String normalizeResponsibility(
                        ProjectUserRequest request) {

                if (request.getRole() != ProjectRole.MEMBER) {
                        return null;
                }

                MemberResponsibility responsibility = request.getResponsibilityRole();

                if (responsibility == null) {
                        return MemberResponsibility.DEVELOPER.name();
                }

                return responsibility.name()
                                .trim()
                                .toUpperCase();
        }

        private String normalizeRole(String role) {

                if (role == null || role.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Project role is required");
                }

                String normalized = role.trim().toUpperCase();

                /*
                 * Backward compatibility:
                 *
                 * Old project role ADMIN
                 * becomes PROJECT_ADMIN.
                 */
                if ("ADMIN".equals(normalized)) {
                        return ProjectRole.PROJECT_ADMIN.name();
                }

                try {
                        return ProjectRole
                                        .valueOf(normalized)
                                        .name();

                } catch (IllegalArgumentException ex) {

                        throw new IllegalArgumentException(
                                        "Invalid project role: " + role);
                }
        }

        // -------------------------------------------------------------------------
        // HELPERS
        // -------------------------------------------------------------------------

        private ProjectUser getProjectUser(
                        Long projectUserId) {

                if (projectUserId == null
                                || projectUserId <= 0) {

                        throw new IllegalArgumentException(
                                        "Project user ID must be positive");
                }

                return projectUserRepository.findById(
                                projectUserId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: "
                                                                + projectUserId));
        }

        private Project getProject(Long projectId) {

                if (projectId == null || projectId <= 0) {

                        throw new IllegalArgumentException(
                                        "Project ID must be positive");
                }

                return projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: "
                                                                + projectId));
        }

        private User getUser(Long userId) {

                return userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found with id: " + userId));
        }

        private void validateUser(Long userId) {

                if (userId == null
                                || !userRepository.existsById(userId)) {

                        throw new RuntimeException(
                                        "User not found with id: " + userId);
                }
        }

        private ProjectUserResponse toResponse(
                        ProjectUser projectUser) {

                User user = projectUser.getUser();
                Project project = projectUser.getProject();

                return ProjectUserResponse.builder()
                                .id(projectUser.getId())
                                .userId(
                                                user != null
                                                                ? user.getId()
                                                                : null)
                                .userName(
                                                user != null
                                                                ? user.getName()
                                                                : null)
                                .userEmail(
                                                user != null
                                                                ? user.getEmail()
                                                                : null)
                                .projectId(
                                                project != null
                                                                ? project.getId()
                                                                : null)
                                .projectName(
                                                project != null
                                                                ? project.getName()
                                                                : null)
                                .role(projectUser.getRole())
                                .responsibilityRole(
                                                projectUser.getResponsibilityRole())
                                .availabilitySelfUpdateOpen(
                                                Boolean.TRUE.equals(
                                                                projectUser
                                                                                .getAvailabilitySelfUpdateOpen()))
                                .createdAt(
                                                projectUser.getCreatedAt())
                                .updatedAt(
                                                projectUser.getUpdatedAt())
                                .build();
        }
}