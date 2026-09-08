package com.projectmanagement.app.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;
import com.projectmanagement.app.workspace.WorkspaceMemberRepository;

@Service
@Transactional
public class ProjectUserService {

        private final ProjectUserRepository projectUserRepository;
        private final UserRepository userRepository;
        private final ProjectRepository projectRepository;
        private final ProjectAccessService projectAccessService;
        private final WorkspaceMemberRepository workspaceMemberRepository;

        public ProjectUserService(
                        ProjectUserRepository projectUserRepository,
                        UserRepository userRepository,
                        ProjectRepository projectRepository,
                        ProjectAccessService projectAccessService,
                        WorkspaceMemberRepository workspaceMemberRepository) {

                this.projectUserRepository = projectUserRepository;
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
                this.projectAccessService = projectAccessService;
                this.workspaceMemberRepository = workspaceMemberRepository;
        }

        // ---------------------------------------------------------
        // GET ALL
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getAllProjectUsers() {

                return projectUserRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Get all project users belonging to a workspace.
         */
        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getAllProjectUsers(
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                return projectUserRepository.findAll()
                                .stream()
                                .filter(projectUser -> isProjectInWorkspace(
                                                projectUser.getProject(),
                                                workspaceId))
                                .map(this::toResponse)
                                .toList();
        }

        // ---------------------------------------------------------
        // GET BY ID
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public ProjectUserResponse getProjectUserById(Long id) {

                ProjectUser projectUser = projectUserRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                return toResponse(projectUser);
        }

        /**
         * Get project user by ID within a workspace.
         */
        @Transactional(readOnly = true)
        public ProjectUserResponse getProjectUserById(
                        Long id,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                ProjectUser projectUser = projectUserRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                validateProjectInWorkspace(
                                projectUser.getProject(),
                                workspaceId);

                return toResponse(projectUser);
        }

        // ---------------------------------------------------------
        // GET BY PROJECT
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByProject(
                        Long projectId) {

                return projectUserRepository
                                .findByProjectId(projectId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Get project users for a project within a workspace.
         */
        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByProject(
                        Long projectId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: " + projectId));

                validateProjectInWorkspace(project, workspaceId);

                return projectUserRepository
                                .findByProjectId(projectId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ---------------------------------------------------------
        // GET BY USER
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByUser(
                        Long userId) {

                return projectUserRepository
                                .findByUserId(userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Get project assignments for a user within a workspace.
         */
        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByUser(
                        Long userId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);
                validateUser(userId);

                return projectUserRepository
                                .findByUserId(userId)
                                .stream()
                                .filter(projectUser -> isProjectInWorkspace(
                                                projectUser.getProject(),
                                                workspaceId))
                                .map(this::toResponse)
                                .toList();
        }

        // ---------------------------------------------------------
        // GET BY PROJECT + ROLE
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByProjectAndRole(
                        Long projectId,
                        String role) {

                return projectUserRepository
                                .findByProjectIdAndRole(projectId, role)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Get project users by role within a workspace.
         */
        @Transactional(readOnly = true)
        public List<ProjectUserResponse> getProjectUsersByProjectAndRole(
                        Long projectId,
                        String role,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: " + projectId));

                validateProjectInWorkspace(project, workspaceId);

                return projectUserRepository
                                .findByProjectIdAndRole(projectId, role)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ---------------------------------------------------------
        // CHECK ASSIGNMENT
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public boolean existsByProjectAndUser(
                        Long projectId,
                        Long userId) {

                return projectUserRepository
                                .existsByProjectIdAndUserId(
                                                projectId,
                                                userId);
        }

        /**
         * Check assignment within a workspace.
         */
        @Transactional(readOnly = true)
        public boolean existsByProjectAndUser(
                        Long projectId,
                        Long userId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: " + projectId));

                validateProjectInWorkspace(project, workspaceId);
                validateWorkspaceMembership(project, userId);

                return projectUserRepository
                                .existsByProjectIdAndUserId(
                                                projectId,
                                                userId);
        }

        // ---------------------------------------------------------
        // CREATE
        // ---------------------------------------------------------

        public ProjectUserResponse createProjectUser(
                        ProjectUserRequest request) {

                validateUser(request.getUserId());
                validateProject(request.getProjectId());

                if (projectUserRepository.existsByProjectIdAndUserId(
                                request.getProjectId(),
                                request.getUserId())) {

                        throw new RuntimeException(
                                        "User is already assigned to this project");
                }

                User user = userRepository.findById(
                                request.getUserId())
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found with id: "
                                                                + request.getUserId()));

                Project project = projectRepository.findById(
                                request.getProjectId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: "
                                                                + request.getProjectId()));

                projectAccessService.requireManager(project);

                validateWorkspaceMembership(
                                project,
                                user.getId());

                ProjectUser projectUser = ProjectUser.builder()
                                .user(user)
                                .project(project)
                                .role(request.getRole().name())
                                .build();

                return toResponse(
                                projectUserRepository.save(projectUser));
        }

        // ---------------------------------------------------------
        // UPDATE
        // ---------------------------------------------------------

        public ProjectUserResponse updateProjectUser(
                        Long id,
                        ProjectUserRequest request) {

                ProjectUser projectUser = projectUserRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                Project existingProject = projectUser.getProject();

                projectAccessService.requireManager(existingProject);

                validateUser(request.getUserId());
                validateProject(request.getProjectId());

                boolean assignmentChanged = !projectUser.getUser().getId()
                                .equals(request.getUserId())
                                ||
                                !projectUser.getProject().getId()
                                                .equals(request.getProjectId());

                if (assignmentChanged
                                && projectUserRepository
                                                .existsByProjectIdAndUserIdAndIdNot(
                                                                request.getProjectId(),
                                                                request.getUserId(),
                                                                id)) {

                        throw new RuntimeException(
                                        "User is already assigned to this project");
                }

                User user = userRepository.findById(
                                request.getUserId())
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found with id: "
                                                                + request.getUserId()));

                Project project = projectRepository.findById(
                                request.getProjectId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: "
                                                                + request.getProjectId()));

                projectAccessService.requireManager(project);

                /*
                 * Do not allow moving a project-user assignment
                 * between different workspaces.
                 */
                validateSameWorkspace(
                                existingProject,
                                project);

                validateWorkspaceMembership(
                                project,
                                user.getId());

                projectUser.setUser(user);
                projectUser.setProject(project);
                projectUser.setRole(request.getRole().name());

                return toResponse(
                                projectUserRepository.save(projectUser));
        }

        // ---------------------------------------------------------
        // DELETE
        // ---------------------------------------------------------

        public void deleteProjectUser(Long id) {

                ProjectUser projectUser = projectUserRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                projectAccessService.requireManager(
                                projectUser.getProject());

                projectUserRepository.delete(projectUser);
        }

        /**
         * Delete project user within a workspace.
         */
        public void deleteProjectUser(
                        Long id,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                ProjectUser projectUser = projectUserRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project user not found with id: " + id));

                validateProjectInWorkspace(
                                projectUser.getProject(),
                                workspaceId);

                projectAccessService.requireManager(
                                projectUser.getProject());

                projectUserRepository.delete(projectUser);
        }

        // ---------------------------------------------------------
        // DELETE ALL USERS FROM PROJECT
        // ---------------------------------------------------------

        public void deleteProjectUsersByProject(
                        Long projectId) {

                validateProject(projectId);

                Project project = projectRepository
                                .getReferenceById(projectId);

                projectAccessService.requireManager(project);

                projectUserRepository.deleteByProjectId(projectId);
        }

        /**
         * Delete all users from a project within a workspace.
         */
        public void deleteProjectUsersByProject(
                        Long projectId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: " + projectId));

                validateProjectInWorkspace(
                                project,
                                workspaceId);

                projectAccessService.requireManager(project);

                projectUserRepository.deleteByProjectId(projectId);
        }

        // ---------------------------------------------------------
        // DELETE USER FROM ALL PROJECTS
        // ---------------------------------------------------------

        public void deleteProjectUsersByUser(Long userId) {

                validateUser(userId);

                projectUserRepository.deleteByUserId(userId);
        }

        /**
         * Delete a user's project assignments only from
         * the specified workspace.
         *
         * Repository currently exposes deleteByUserId only,
         * therefore workspace-specific deletion is performed
         * after resolving the workspace's project assignments.
         */
        public void deleteProjectUsersByUser(
                        Long userId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);
                validateUser(userId);

                List<ProjectUser> assignments = projectUserRepository.findByUserId(userId);

                assignments.stream()
                                .filter(projectUser -> isProjectInWorkspace(
                                                projectUser.getProject(),
                                                workspaceId))
                                .forEach(projectUserRepository::delete);
        }

        // ---------------------------------------------------------
        // COUNT USERS IN PROJECT
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public long countUsersByProject(
                        Long projectId) {

                return projectUserRepository
                                .countByProjectId(projectId);
        }

        @Transactional(readOnly = true)
        public long countUsersByProject(
                        Long projectId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: " + projectId));

                validateProjectInWorkspace(
                                project,
                                workspaceId);

                return projectUserRepository
                                .countByProjectId(projectId);
        }

        // ---------------------------------------------------------
        // COUNT PROJECTS FOR USER
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public long countProjectsByUser(
                        Long userId) {

                return projectUserRepository
                                .countByUserId(userId);
        }

        @Transactional(readOnly = true)
        public long countProjectsByUser(
                        Long userId,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);
                validateUser(userId);

                return projectUserRepository
                                .findByUserId(userId)
                                .stream()
                                .filter(projectUser -> isProjectInWorkspace(
                                                projectUser.getProject(),
                                                workspaceId))
                                .count();
        }

        // ---------------------------------------------------------
        // VALIDATION
        // ---------------------------------------------------------

        private void validateUser(Long userId) {

                if (userId == null || !userRepository.existsById(userId)) {
                        throw new RuntimeException(
                                        "User not found with id: " + userId);
                }
        }

        private void validateProject(Long projectId) {

                if (projectId == null
                                || !projectRepository.existsById(projectId)) {

                        throw new RuntimeException(
                                        "Project not found with id: " + projectId);
                }
        }

        private void validateWorkspaceId(Long workspaceId) {

                if (workspaceId == null || workspaceId <= 0) {
                        throw new RuntimeException(
                                        "Workspace ID must be a positive value");
                }
        }

        private void validateWorkspaceMembership(
                        Project project,
                        Long userId) {

                if (project == null
                                || project.getWorkspace() == null
                                || project.getWorkspace().getId() == null) {

                        throw new RuntimeException(
                                        "Project is not associated with a workspace");
                }

                if (!workspaceMemberRepository
                                .existsByWorkspaceIdAndUserId(
                                                project.getWorkspace().getId(),
                                                userId)) {

                        throw new RuntimeException(
                                        "Project member must belong to the workspace");
                }
        }

        private void validateProjectInWorkspace(
                        Project project,
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                if (project == null
                                || project.getWorkspace() == null
                                || project.getWorkspace().getId() == null) {

                        throw new RuntimeException(
                                        "Project is not associated with a workspace");
                }

                if (!workspaceId.equals(
                                project.getWorkspace().getId())) {

                        throw new RuntimeException(
                                        "Project does not belong to the specified workspace");
                }
        }

        private boolean isProjectInWorkspace(
                        Project project,
                        Long workspaceId) {

                return project != null
                                && project.getWorkspace() != null
                                && project.getWorkspace().getId() != null
                                && workspaceId.equals(
                                                project.getWorkspace().getId());
        }

        private void validateSameWorkspace(
                        Project sourceProject,
                        Project targetProject) {

                if (sourceProject == null
                                || sourceProject.getWorkspace() == null
                                || targetProject == null
                                || targetProject.getWorkspace() == null) {

                        throw new RuntimeException(
                                        "Both projects must belong to a workspace");
                }

                Long sourceWorkspaceId = sourceProject.getWorkspace().getId();

                Long targetWorkspaceId = targetProject.getWorkspace().getId();

                if (sourceWorkspaceId == null
                                || targetWorkspaceId == null
                                || !sourceWorkspaceId.equals(targetWorkspaceId)) {

                        throw new RuntimeException(
                                        "Project user assignment cannot be moved between workspaces");
                }
        }

        // ---------------------------------------------------------
        // RESPONSE MAPPER
        // ---------------------------------------------------------

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

                                .createdAt(projectUser.getCreatedAt())
                                .updatedAt(projectUser.getUpdatedAt())

                                .build();
        }
}