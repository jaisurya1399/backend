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

        public ProjectUserService(
                        ProjectUserRepository projectUserRepository,
                        UserRepository userRepository,
                        ProjectRepository projectRepository,
                        ProjectAccessService projectAccessService) {

                this.projectUserRepository = projectUserRepository;
                this.userRepository = userRepository;
                this.projectRepository = projectRepository;
                this.projectAccessService = projectAccessService;
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

        // ---------------------------------------------------------
        // DELETE USER FROM ALL PROJECTS
        // ---------------------------------------------------------

        public void deleteProjectUsersByUser(Long userId) {

                validateUser(userId);

                projectUserRepository.deleteByUserId(userId);
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

        // ---------------------------------------------------------
        // COUNT PROJECTS FOR USER
        // ---------------------------------------------------------

        @Transactional(readOnly = true)
        public long countProjectsByUser(
                        Long userId) {

                return projectUserRepository
                                .countByUserId(userId);
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