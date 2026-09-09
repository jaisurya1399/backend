package com.projectmanagement.app.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;

    @GetMapping("/direct/{userId}")
    @PreAuthorize("isAuthenticated()")
    public List<ChatMessageResponse> direct(@PathVariable Long userId) {
        return service.direct(userId);
    }

    @PostMapping("/direct/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendDirect(@PathVariable Long userId,
            @Valid @RequestBody ChatMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendDirect(userId, req));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public List<ChatMessageResponse> room(@PathVariable Long projectId) {
        return service.room(projectId);
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendRoom(@PathVariable Long projectId,
            @Valid @RequestBody ChatMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendRoom(projectId, req));
    }
}
