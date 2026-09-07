package com.projectmanagement.app.workspace;

import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService service;
    public WorkspaceController(WorkspaceService service) { this.service = service; }
    @PostMapping public ResponseEntity<WorkspaceResponse> create(@Valid @RequestBody WorkspaceRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @GetMapping public List<WorkspaceResponse> mine() { return service.getMine(); }
    @GetMapping("/{id}") public WorkspaceResponse get(@PathVariable Long id) { return service.getById(id); }
    @PutMapping("/{id}") public WorkspaceResponse update(@PathVariable Long id, @Valid @RequestBody WorkspaceRequest request) { return service.update(id, request); }
    @GetMapping("/{id}/members") public List<WorkspaceMemberResponse> members(@PathVariable Long id) { return service.getMembers(id); }
    @PostMapping("/{id}/members") public ResponseEntity<WorkspaceMemberResponse> addMember(@PathVariable Long id, @Valid @RequestBody WorkspaceMemberRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.addMember(id, request)); }
    @PutMapping("/{id}/members/{userId}") public WorkspaceMemberResponse updateMember(@PathVariable Long id, @PathVariable Long userId, @Valid @RequestBody WorkspaceMemberRequest request) { return service.updateMember(id, userId, request); }
    @PostMapping("/{id}/ownership/{newOwnerId}") public ResponseEntity<Void> transferOwnership(@PathVariable Long id, @PathVariable Long newOwnerId) { service.transferOwnership(id, newOwnerId); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}/members/{userId}") public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) { service.removeMember(id, userId); return ResponseEntity.noContent().build(); }
}
