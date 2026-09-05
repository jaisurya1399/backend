package com.projectmanagement.app.project;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

@Service
public class ProjectService {

        private final ProjectRepository projectRepository;
        private final UserRepository userRepository;
        private final ProjectStatusRepository projectStatusRepository;

        public ProjectService(
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        ProjectStatusRepository projectStatusRepository) {
                this.projectRepository = projectRepository;
                this.userRepository = userRepository;
                this.projectStatusRepository = projectStatusRepository;
        }

        @Transactional(readOnly = true)
        public List<ProjectResponse> getAllProjects() {
                return projectRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ProjectResponse> getAllActiveProjects() {
                return projectRepository.findByDeletedAtIsNull()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public ProjectResponse getProjectById(Long id) {
                validateId(id);

                return toResponse(
                                getProjectEntityById(id));
        }

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByOwner(
                        Long ownerId) {
                validateUserId(ownerId);

                return projectRepository
                                .findByOwnerId(ownerId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByOwner(
                        Long ownerId) {
                validateUserId(ownerId);

                return projectRepository
                                .findByOwnerIdAndDeletedAtIsNull(ownerId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ProjectResponse> getProjectsByStatus(
                        Long statusId) {
                validateStatusId(statusId);

                return projectRepository
                                .findByStatusId(statusId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ProjectResponse> getActiveProjectsByStatus(
                        Long statusId) {
                validateStatusId(statusId);

                return projectRepository
                                .findByStatusIdAndDeletedAtIsNull(statusId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public ProjectResponse getProjectByName(
                        String name) {
                validateName(name);

                Project project = projectRepository
                                .findByNameAndDeletedAtIsNull(
                                                name.trim())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with name: "
                                                                + name));

                return toResponse(project);
        }

        @Transactional
        public ProjectResponse createProject(
                        ProjectRequest request) {
                validateRequest(request);

                String normalizedName = request.getName().trim();

                String normalizedPrefix = request.getTicketPrefix().trim();

                if (projectRepository.existsByName(
                                normalizedName)) {
                        throw new RuntimeException(
                                        "Project already exists with name: "
                                                        + normalizedName);
                }

                if (projectRepository.existsByTicketPrefix(
                                normalizedPrefix)) {
                        throw new RuntimeException(
                                        "Ticket prefix already exists: "
                                                        + normalizedPrefix);
                }

                User owner = getUserById(request.getOwnerId());

                ProjectStatus status = getStatusById(request.getStatusId());

                Project project = Project.builder()
                                .name(normalizedName)
                                .description(request.getDescription())
                                .owner(owner)
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

        @Transactional
        public ProjectResponse updateProject(
                        Long id,
                        ProjectRequest request) {
                validateId(id);
                validateRequest(request);

                Project project = getProjectEntityById(id);

                String normalizedName = request.getName().trim();

                String normalizedPrefix = request.getTicketPrefix().trim();

                if (projectRepository.existsByNameAndIdNot(
                                normalizedName,
                                id)) {
                        throw new RuntimeException(
                                        "Project already exists with name: "
                                                        + normalizedName);
                }

                if (projectRepository
                                .existsByTicketPrefixAndIdNot(
                                                normalizedPrefix,
                                                id)) {
                        throw new RuntimeException(
                                        "Ticket prefix already exists: "
                                                        + normalizedPrefix);
                }

                User owner = getUserById(request.getOwnerId());

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

        @Transactional
        public void deleteProject(Long id) {
                validateId(id);

                Project project = getProjectEntityById(id);

                project.setDeletedAt(
                                LocalDateTime.now());

                projectRepository.save(project);
        }

        @Transactional
        public void restoreProject(Long id) {
                validateId(id);

                Project project = getProjectEntityById(id);

                project.setDeletedAt(null);

                projectRepository.save(project);
        }

        @Transactional
        public void permanentlyDeleteProject(Long id) {
                validateId(id);

                Project project = getProjectEntityById(id);

                projectRepository.delete(project);
        }

        private Project getProjectEntityById(Long id) {
                return projectRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: "
                                                                + id));
        }

        private User getUserById(Long userId) {
                return userRepository
                                .findById(userId)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found with id: "
                                                                + userId));
        }

        private ProjectStatus getStatusById(
                        Long statusId) {
                return projectStatusRepository
                                .findById(statusId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Project status not found with id: "
                                                                + statusId));
        }

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

                return ProjectResponse.builder()
                                .id(project.getId())
                                .name(project.getName())
                                .description(project.getDescription())

                                .ownerId(ownerId)
                                .ownerName(ownerName)
                                .ownerEmail(ownerEmail)

                                .statusId(statusId)
                                .statusName(statusName)
                                .statusColor(statusColor)

                                .ticketPrefix(
                                                project.getTicketPrefix())
                                .statusType(
                                                project.getStatusType())

                                .deletedAt(
                                                project.getDeletedAt())
                                .createdAt(
                                                project.getCreatedAt())
                                .updatedAt(
                                                project.getUpdatedAt())
                                .build();
        }

        private void validateRequest(
                        ProjectRequest request) {
                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Project request cannot be null");
                }

                validateName(request.getName());
                validateTicketPrefix(
                                request.getTicketPrefix());

                validateUserId(request.getOwnerId());
                validateStatusId(request.getStatusId());
        }

        private void validateId(Long id) {
                if (id == null || id <= 0) {
                        throw new IllegalArgumentException(
                                        "Project ID must be greater than zero");
                }
        }

        private void validateUserId(Long userId) {
                if (userId == null || userId <= 0) {
                        throw new IllegalArgumentException(
                                        "Owner ID must be greater than zero");
                }
        }

        private void validateStatusId(Long statusId) {
                if (statusId == null || statusId <= 0) {
                        throw new IllegalArgumentException(
                                        "Status ID must be greater than zero");
                }
        }

        private void validateName(String name) {
                if (name == null || name.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Project name cannot be empty");
                }
        }

        private void validateTicketPrefix(
                        String ticketPrefix) {
                if (ticketPrefix == null
                                || ticketPrefix.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Ticket prefix cannot be empty");
                }
        }
}