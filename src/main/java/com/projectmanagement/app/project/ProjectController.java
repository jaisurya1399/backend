package com.projectmanagement.app.project;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

        private final ProjectService projectService;

        public ProjectController(
                        ProjectService projectService) {
                this.projectService = projectService;
        }

        @GetMapping
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<List<ProjectResponse>> getAllProjects() {

                return ResponseEntity.ok(
                                projectService.getAllProjects());
        }

        @GetMapping("/active")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<List<ProjectResponse>> getAllActiveProjects() {

                return ResponseEntity.ok(
                                projectService.getAllActiveProjects());
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<ProjectResponse> getProjectById(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                projectService.getProjectById(id));
        }

        @GetMapping("/owner/{ownerId}")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<List<ProjectResponse>> getProjectsByOwner(
                        @PathVariable Long ownerId) {

                return ResponseEntity.ok(
                                projectService.getProjectsByOwner(ownerId));
        }

        @GetMapping("/owner/{ownerId}/active")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<List<ProjectResponse>> getActiveProjectsByOwner(
                        @PathVariable Long ownerId) {

                return ResponseEntity.ok(
                                projectService.getActiveProjectsByOwner(
                                                ownerId));
        }

        @GetMapping("/status/{statusId}")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<List<ProjectResponse>> getProjectsByStatus(
                        @PathVariable Long statusId) {

                return ResponseEntity.ok(
                                projectService.getProjectsByStatus(statusId));
        }

        @GetMapping("/status/{statusId}/active")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<List<ProjectResponse>> getActiveProjectsByStatus(
                        @PathVariable Long statusId) {

                return ResponseEntity.ok(
                                projectService.getActiveProjectsByStatus(
                                                statusId));
        }

        @GetMapping("/name/{name}")
        @PreAuthorize("hasAuthority('project.view') or hasRole('ADMIN')")
        public ResponseEntity<ProjectResponse> getProjectByName(
                        @PathVariable String name) {

                return ResponseEntity.ok(
                                projectService.getProjectByName(name));
        }

        @PostMapping
        @PreAuthorize("hasAuthority('project.create') or hasRole('ADMIN')")
        public ResponseEntity<ProjectResponse> createProject(
                        @Valid @RequestBody ProjectRequest request) {

                ProjectResponse response = projectService.createProject(request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('project.update') or hasRole('ADMIN')")
        public ResponseEntity<ProjectResponse> updateProject(
                        @PathVariable Long id,
                        @Valid @RequestBody ProjectRequest request) {

                return ResponseEntity.ok(
                                projectService.updateProject(
                                                id,
                                                request));
        }

        @PutMapping("/{id}/restore")
        @PreAuthorize("hasAuthority('project.update') or hasRole('ADMIN')")
        public ResponseEntity<Void> restoreProject(
                        @PathVariable Long id) {

                projectService.restoreProject(id);

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('project.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> deleteProject(
                        @PathVariable Long id) {

                projectService.deleteProject(id);

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/{id}/permanent")
        @PreAuthorize("hasAuthority('project.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> permanentlyDeleteProject(
                        @PathVariable Long id) {

                projectService.permanentlyDeleteProject(id);

                return ResponseEntity.noContent().build();
        }
}