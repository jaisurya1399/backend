package com.projectmanagement.app.project;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.auth.CurrentUserService;
import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;
import com.projectmanagement.app.workspace.Workspace;
import com.projectmanagement.app.workspace.WorkspaceMemberRepository;
import com.projectmanagement.app.workspace.WorkspaceRepository;
import com.projectmanagement.app.workspace.WorkspaceRole;

@Service
public class ProjectService {

        private final ProjectRepository projectRepository;
        private final UserRepository userRepository;
        private final ProjectStatusRepository projectStatusRepository;
        private final CurrentUserService currentUserService;
        private final WorkspaceRepository workspaceRepository;
        private final WorkspaceMemberRepository workspaceMemberRepository;
        private final ProjectAccessService projectAccessService;

        public ProjectService(
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        ProjectStatusRepository projectStatusRepository,
                        CurrentUserService currentUserService,
                        WorkspaceRepository workspaceRepository,
                        WorkspaceMemberRepository workspaceMemberRepository,
                        ProjectAccessService projectAccessService) {

                this.projectRepository = projectRepository;
                this.userRepository = userRepository;
                this.projectStatusRepository = projectStatusRepository;
                this.currentUserService = currentUserService;
                this.workspaceRepository = workspaceRepository;
                this.workspaceMemberRepository = workspaceMemberRepository;
                this.projectAccessService = projectAccessService;
        }

        // ============================================================
        // GET ALL PROJECTS
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getAllProjects() {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (isAdmin(authentication)) {
                        return projectRepository.findAll()
                                        .stream()
                                        .map(this::toResponse)
                                        .toList();
                }

                Long userId = currentUserService.getCurrentUserId();

                return projectRepository.findVisibleProjectsForUser(userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ALL ACTIVE PROJECTS
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getAllActiveProjects() {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (isAdmin(authentication)) {
                        return projectRepository.findByDeletedAtIsNull()
                                        .stream()
                                        .map(this::toResponse)
                                        .toList();
                }

                Long userId = currentUserService.getCurrentUserId();

                return projectRepository.findVisibleActiveProjectsForUser(userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET PROJECT BY ID
        // ============================================================

        @Transactional(readOnly = true)
        public ProjectResponse getProjectById(Long id) {

                validateId(id);

                Project project = getProjectEntityById(id);

                projectAccessService.requireView(project);

                return toResponse(project);
        }

        // ============================================================
        // GET PROJECTS BY OWNER
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByOwner(Long ownerId) {

                validateUserId(ownerId);

                return projectRepository.findByOwnerId(ownerId)
                                .stream()
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE PROJECTS BY OWNER
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByOwner(Long ownerId) {

                validateUserId(ownerId);

                return projectRepository.findByOwnerIdAndDeletedAtIsNull(ownerId)
                                .stream()
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET PROJECTS BY STATUS
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByStatus(Long statusId) {

                validateStatusId(statusId);

                return projectRepository.findByStatusId(statusId)
                                .stream()
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE PROJECTS BY STATUS
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByStatus(Long statusId) {

                validateStatusId(statusId);

                return projectRepository.findByStatusIdAndDeletedAtIsNull(statusId)
                                .stream()
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET PROJECT BY NAME
        // ============================================================

        @Transactional(readOnly = true)
        public ProjectResponse getProjectByName(String name) {

                validateName(name);

                Project project = projectRepository
                                .findByNameAndDeletedAtIsNull(name.trim())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with name: " + name));

                projectAccessService.requireView(project);

                return toResponse(project);
        }

        // ============================================================
        // CREATE PROJECT
        // ============================================================

        @Transactional
        public ProjectResponse createProject(ProjectRequest request) {

                validateRequest(request);

                String normalizedName = request.getName().trim();
                String normalizedPrefix = request.getTicketPrefix().trim();

                Workspace workspace = getActiveWorkspace(request.getWorkspaceId());

                validateWorkspaceManager(workspace.getId());

                if (projectRepository.existsByName(normalizedName)) {
                        throw new RuntimeException(
                                        "Project already exists with name: " + normalizedName);
                }

                if (projectRepository.existsByTicketPrefix(normalizedPrefix)) {
                        throw new RuntimeException(
                                        "Ticket prefix already exists: " + normalizedPrefix);
                }

                User owner = getUserById(request.getOwnerId());

                validateWorkspaceMember(
                                workspace.getId(),
                                owner.getId());

                ProjectStatus status = getStatusById(request.getStatusId());

                Project project = Project.builder()
                                .name(normalizedName)
                                .description(request.getDescription())
                                .owner(owner)
                                .workspace(workspace)
                                .status(status)
                                .ticketPrefix(normalizedPrefix)
                                .statusType(
                                                request.getStatusType() != null
                                                                && !request.getStatusType().isBlank()
                                                                                ? request.getStatusType().trim()
                                                                                : "default")
                                .build();

                Project savedProject = projectRepository.save(project);

                return toResponse(savedProject);
        }

        // ============================================================
        // UPDATE PROJECT
        // ============================================================

        @Transactional
        public ProjectResponse updateProject(
                        Long id,
                        ProjectRequest request) {

                validateId(id);
                validateRequest(request);

                Project project = getProjectEntityById(id);

                projectAccessService.requireManager(project);

                if (project.getWorkspace() == null
                                || !project.getWorkspace().getId()
                                                .equals(request.getWorkspaceId())) {

                        throw new RuntimeException(
                                        "Project workspace cannot be changed");
                }

                String normalizedName = request.getName().trim();
                String normalizedPrefix = request.getTicketPrefix().trim();

                if (projectRepository.existsByNameAndIdNot(
                                normalizedName,
                                id)) {

                        throw new RuntimeException(
                                        "Project already exists with name: "
                                                        + normalizedName);
                }

                if (projectRepository.existsByTicketPrefixAndIdNot(
                                normalizedPrefix,
                                id)) {

                        throw new RuntimeException(
                                        "Ticket prefix already exists: "
                                                        + normalizedPrefix);
                }

                User owner = getUserById(request.getOwnerId());

                validateWorkspaceMember(
                                project.getWorkspace().getId(),
                                owner.getId());

                ProjectStatus status = getStatusById(request.getStatusId());

                project.setName(normalizedName);
                project.setDescription(request.getDescription());
                project.setOwner(owner);
                project.setStatus(status);
                project.setTicketPrefix(normalizedPrefix);

                if (request.getStatusType() != null
                                && !request.getStatusType().isBlank()) {

                        project.setStatusType(
                                        request.getStatusType().trim());
                }

                Project updatedProject = projectRepository.save(project);

                return toResponse(updatedProject);
        }

        // ============================================================
        // SOFT DELETE PROJECT
        // ============================================================

        @Transactional
        public void deleteProject(Long id) {

                validateId(id);

                Project project = getProjectEntityById(id);

                projectAccessService.requireManager(project);

                project.setDeletedAt(LocalDateTime.now());

                projectRepository.save(project);
        }

        // ============================================================
        // RESTORE PROJECT
        // ============================================================

        @Transactional
        public void restoreProject(Long id) {

                validateId(id);

                Project project = getProjectEntityById(id);

                projectAccessService.requireManager(project);

                project.setDeletedAt(null);

                projectRepository.save(project);
        }

        // ============================================================
        // PERMANENTLY DELETE PROJECT
        // ============================================================

        @Transactional
        public void permanentlyDeleteProject(Long id) {

                validateId(id);

                Project project = getProjectEntityById(id);

                projectAccessService.requireManager(project);

                projectRepository.delete(project);
        }

        // ============================================================
        // GET PROJECTS BY WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByWorkspace(
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (isAdmin(authentication)) {

                        return projectRepository
                                        .findByWorkspaceId(workspace.getId())
                                        .stream()
                                        .map(this::toResponse)
                                        .toList();
                }

                Long userId = currentUserService.getCurrentUserId();

                validateWorkspaceMember(
                                workspace.getId(),
                                userId);

                return projectRepository
                                .findVisibleProjectsForUserInWorkspace(
                                                workspace.getId(),
                                                userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE PROJECTS BY WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByWorkspace(
                        Long workspaceId) {

                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (isAdmin(authentication)) {

                        return projectRepository
                                        .findByWorkspaceIdAndDeletedAtIsNull(
                                                        workspace.getId())
                                        .stream()
                                        .map(this::toResponse)
                                        .toList();
                }

                Long userId = currentUserService.getCurrentUserId();

                validateWorkspaceMember(
                                workspace.getId(),
                                userId);

                return projectRepository
                                .findVisibleActiveProjectsForUserInWorkspace(
                                                workspace.getId(),
                                                userId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET PROJECTS BY OWNER + WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByOwner(
                        Long ownerId,
                        Long workspaceId) {

                validateUserId(ownerId);
                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                requireWorkspaceViewAccess(workspace.getId());

                return projectRepository
                                .findByOwnerId(ownerId)
                                .stream()
                                .filter(project -> belongsToWorkspace(project, workspace.getId()))
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE PROJECTS BY OWNER + WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByOwner(
                        Long ownerId,
                        Long workspaceId) {

                validateUserId(ownerId);
                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                requireWorkspaceViewAccess(workspace.getId());

                return projectRepository
                                .findByOwnerIdAndDeletedAtIsNull(ownerId)
                                .stream()
                                .filter(project -> belongsToWorkspace(project, workspace.getId()))
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET PROJECTS BY STATUS + WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByStatus(
                        Long statusId,
                        Long workspaceId) {

                validateStatusId(statusId);
                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                requireWorkspaceViewAccess(workspace.getId());

                return projectRepository
                                .findByStatusId(statusId)
                                .stream()
                                .filter(project -> belongsToWorkspace(project, workspace.getId()))
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE PROJECTS BY STATUS + WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByStatus(
                        Long statusId,
                        Long workspaceId) {

                validateStatusId(statusId);
                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                requireWorkspaceViewAccess(workspace.getId());

                return projectRepository
                                .findByStatusIdAndDeletedAtIsNull(statusId)
                                .stream()
                                .filter(project -> belongsToWorkspace(project, workspace.getId()))
                                .filter(this::canViewProject)
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET PROJECT BY NAME + WORKSPACE
        // ============================================================

        @Transactional(readOnly = true)
        public ProjectResponse getProjectByName(
                        String name,
                        Long workspaceId) {

                validateName(name);
                validateWorkspaceId(workspaceId);

                Workspace workspace = getActiveWorkspace(workspaceId);

                requireWorkspaceViewAccess(workspace.getId());

                Project project = projectRepository
                                .findByNameAndDeletedAtIsNull(name.trim())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with name: " + name));

                if (!belongsToWorkspace(
                                project,
                                workspace.getId())) {

                        throw new RuntimeException(
                                        "Project not found in the selected workspace");
                }

                projectAccessService.requireView(project);

                return toResponse(project);
        }

        // ============================================================
        // WORKSPACE ACCESS
        // ============================================================

        private void requireWorkspaceViewAccess(
                        Long workspaceId) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (isAdmin(authentication)) {
                        return;
                }

                Long userId = currentUserService.getCurrentUserId();

                validateWorkspaceMember(
                                workspaceId,
                                userId);
        }

        private boolean belongsToWorkspace(
                        Project project,
                        Long workspaceId) {

                return project != null
                                && project.getWorkspace() != null
                                && project.getWorkspace().getId() != null
                                && project.getWorkspace().getId().equals(workspaceId);
        }

        private boolean canViewProject(Project project) {

                try {
                        return projectAccessService.canView(project);
                } catch (RuntimeException ex) {
                        return false;
                }
        }

        // ============================================================
        // GET PROJECT ENTITY
        // ============================================================

        private Project getProjectEntityById(Long id) {

                return projectRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: " + id));
        }

        // ============================================================
        // GET USER
        // ============================================================

        private User getUserById(Long userId) {

                return userRepository
                                .findById(userId)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found with id: " + userId));
        }

        // ============================================================
        // GET ACTIVE WORKSPACE
        // ============================================================

        private Workspace getActiveWorkspace(
                        Long workspaceId) {

                return workspaceRepository
                                .findById(workspaceId)
                                .filter(workspace -> workspace.getDeletedAt() == null)
                                .orElseThrow(() -> new RuntimeException(
                                                "Workspace not found with id: "
                                                                + workspaceId));
        }

        // ============================================================
        // VALIDATE WORKSPACE MEMBER
        // ============================================================

        private void validateWorkspaceMember(
                        Long workspaceId,
                        Long userId) {

                if (!workspaceMemberRepository
                                .existsByWorkspaceIdAndUserId(
                                                workspaceId,
                                                userId)) {

                        throw new RuntimeException(
                                        "User must be a member of the workspace");
                }
        }

        // ============================================================
        // VALIDATE WORKSPACE MANAGER
        // ============================================================

        private void validateWorkspaceManager(
                        Long workspaceId) {

                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (isAdmin(authentication)) {
                        return;
                }

                Long currentUserId = currentUserService.getCurrentUserId();

                WorkspaceRole role = workspaceMemberRepository
                                .findByWorkspaceIdAndUserId(
                                                workspaceId,
                                                currentUserId)
                                .map(member -> member.getRole())
                                .orElseThrow(() -> new RuntimeException(
                                                "You do not have access to this workspace"));

                if (role != WorkspaceRole.OWNER
                                && role != WorkspaceRole.ADMIN) {

                        throw new RuntimeException(
                                        "Workspace admin access is required to create a project");
                }
        }

        // ============================================================
        // GET PROJECT STATUS
        // ============================================================

        private ProjectStatus getStatusById(
                        Long statusId) {

                return projectStatusRepository
                                .findById(statusId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project status not found with id: "
                                                                + statusId));
        }

        // ============================================================
        // CONVERT ENTITY TO RESPONSE
        // ============================================================

        private ProjectResponse toResponse(
                        Project project) {

                Long ownerId = null;
                String ownerName = null;
                String ownerEmail = null;

                if (project.getOwner() != null) {

                        ownerId = project.getOwner().getId();
                        ownerName = project.getOwner().getName();
                        ownerEmail = project.getOwner().getEmail();
                }

                Long statusId = null;
                String statusName = null;
                String statusColor = null;

                if (project.getStatus() != null) {

                        statusId = project.getStatus().getId();
                        statusName = project.getStatus().getName();
                        statusColor = project.getStatus().getColor();
                }

                Long workspaceId = null;
                String workspaceName = null;
                String workspaceSlug = null;

                if (project.getWorkspace() != null) {

                        workspaceId = project.getWorkspace().getId();
                        workspaceName = project.getWorkspace().getName();
                        workspaceSlug = project.getWorkspace().getSlug();
                }

                return ProjectResponse.builder()
                                .id(project.getId())
                                .name(project.getName())
                                .description(project.getDescription())

                                .workspaceId(workspaceId)
                                .workspaceName(workspaceName)
                                .workspaceSlug(workspaceSlug)

                                .ownerId(ownerId)
                                .ownerName(ownerName)
                                .ownerEmail(ownerEmail)

                                .statusId(statusId)
                                .statusName(statusName)
                                .statusColor(statusColor)

                                .ticketPrefix(project.getTicketPrefix())
                                .statusType(project.getStatusType())

                                .deletedAt(project.getDeletedAt())
                                .createdAt(project.getCreatedAt())
                                .updatedAt(project.getUpdatedAt())

                                .build();
        }

        // ============================================================
        // VALIDATE PROJECT REQUEST
        // ============================================================

        private void validateRequest(
                        ProjectRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Project request cannot be null");
                }

                validateName(request.getName());
                validateTicketPrefix(request.getTicketPrefix());
                validateUserId(request.getOwnerId());
                validateWorkspaceId(request.getWorkspaceId());
                validateStatusId(request.getStatusId());
        }

        // ============================================================
        // VALIDATE PROJECT ID
        // ============================================================

        private void validateId(Long id) {

                if (id == null || id <= 0) {

                        throw new IllegalArgumentException(
                                        "Project ID must be greater than zero");
                }
        }

        // ============================================================
        // VALIDATE USER ID
        // ============================================================

        private void validateUserId(Long userId) {

                if (userId == null || userId <= 0) {

                        throw new IllegalArgumentException(
                                        "Owner ID must be greater than zero");
                }
        }

        // ============================================================
        // VALIDATE STATUS ID
        // ============================================================

        private void validateStatusId(Long statusId) {

                if (statusId == null || statusId <= 0) {

                        throw new IllegalArgumentException(
                                        "Status ID must be greater than zero");
                }
        }

        // ============================================================
        // VALIDATE WORKSPACE ID
        // ============================================================

        private void validateWorkspaceId(
                        Long workspaceId) {

                if (workspaceId == null || workspaceId <= 0) {

                        throw new IllegalArgumentException(
                                        "Workspace ID must be greater than zero");
                }
        }

        // ============================================================
        // VALIDATE PROJECT NAME
        // ============================================================

        private void validateName(String name) {

                if (name == null || name.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Project name cannot be empty");
                }
        }

        // ============================================================
        // VALIDATE TICKET PREFIX
        // ============================================================

        private void validateTicketPrefix(
                        String ticketPrefix) {

                if (ticketPrefix == null
                                || ticketPrefix.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Ticket prefix cannot be empty");
                }
        }

        // ============================================================
        // CHECK ADMIN
        // ============================================================

        private boolean isAdmin(
                        Authentication authentication) {

                if (authentication == null
                                || authentication.getAuthorities() == null) {

                        return false;
                }

                return authentication
                                .getAuthorities()
                                .stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(
                                                authority.getAuthority()));
        }
}