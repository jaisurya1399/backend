package com.projectmanagement.app.meeting;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService service;

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public List<MeetingResponse> list(@PathVariable Long projectId) {
        return service.list(projectId);
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeetingResponse> create(@PathVariable Long projectId,
            @Valid @RequestBody MeetingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(projectId, req));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public MeetingResponse status(@PathVariable Long id, @RequestParam String value) {
        return service.updateStatus(id, value);
    }
}
