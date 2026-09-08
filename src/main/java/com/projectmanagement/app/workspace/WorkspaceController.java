package com.projectmanagement.app.workspace;

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
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody WorkspaceRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<WorkspaceResponse> mine() {
        return service.getMine();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceResponse get(
            @PathVariable Long id) {

        return service.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceRequest request) {

        return service.update(id, request);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public List<WorkspaceMemberResponse> members(
            @PathVariable Long id) {

        return service.getMembers(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceMemberRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.addMember(id, request));
    }

    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public WorkspaceMemberResponse updateMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody WorkspaceMemberRequest request) {

        return service.updateMember(id, userId, request);
    }

    @PostMapping("/{id}/ownership/{newOwnerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long id,
            @PathVariable Long newOwnerId) {

        service.transferOwnership(id, newOwnerId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {

        service.removeMember(id, userId);

        return ResponseEntity.noContent().build();
    }
}