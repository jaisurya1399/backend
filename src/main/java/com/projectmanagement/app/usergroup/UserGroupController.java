package com.projectmanagement.app.usergroup;

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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserGroupController {
    private final UserGroupService service;

    @GetMapping
    public ResponseEntity<List<UserGroupResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserGroupResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<UserGroupResponse> create(@Valid @RequestBody UserGroupRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserGroupResponse> update(@PathVariable Long id, @Valid @RequestBody UserGroupRequest r) {
        return ResponseEntity.ok(service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<UserGroupResponse> addMember(@PathVariable Long groupId, @PathVariable Long userId) {
        return ResponseEntity.ok(service.addMember(groupId, userId));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<UserGroupResponse> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        return ResponseEntity.ok(service.removeMember(groupId, userId));
    }
}
