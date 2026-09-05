package com.projectmanagement.app.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

        private final NotificationService notificationService;

        @GetMapping
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<List<NotificationResponse>> getAll() {

                return ResponseEntity.ok(
                                notificationService.getAll());
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<NotificationResponse> getById(
                        @PathVariable UUID id) {

                return ResponseEntity.ok(
                                notificationService.getById(id));
        }

        @GetMapping("/user")
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<List<NotificationResponse>> getByNotifiable(
                        @RequestParam String notifiableType,
                        @RequestParam @Positive Long notifiableId) {

                return ResponseEntity.ok(
                                notificationService.getByNotifiable(
                                                notifiableType,
                                                notifiableId));
        }

        @GetMapping("/user/unread")
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<List<NotificationResponse>> getUnread(
                        @RequestParam String notifiableType,
                        @RequestParam @Positive Long notifiableId) {

                return ResponseEntity.ok(
                                notificationService.getUnread(
                                                notifiableType,
                                                notifiableId));
        }

        @GetMapping("/user/read")
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<List<NotificationResponse>> getRead(
                        @RequestParam String notifiableType,
                        @RequestParam @Positive Long notifiableId) {

                return ResponseEntity.ok(
                                notificationService.getRead(
                                                notifiableType,
                                                notifiableId));
        }

        @GetMapping("/user/count")
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<Long> count(
                        @RequestParam String notifiableType,
                        @RequestParam @Positive Long notifiableId) {

                return ResponseEntity.ok(
                                notificationService.count(
                                                notifiableType,
                                                notifiableId));
        }

        @GetMapping("/user/unread-count")
        @PreAuthorize("hasAuthority('notification.view') or hasRole('ADMIN')")
        public ResponseEntity<Long> countUnread(
                        @RequestParam String notifiableType,
                        @RequestParam @Positive Long notifiableId) {

                return ResponseEntity.ok(
                                notificationService.countUnread(
                                                notifiableType,
                                                notifiableId));
        }

        @PostMapping
        @PreAuthorize("hasAuthority('notification.create') or hasRole('ADMIN')")
        public ResponseEntity<NotificationResponse> create(
                        @Valid @RequestBody NotificationRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(notificationService.create(request));
        }

        @PutMapping("/{id}/read")
        @PreAuthorize("hasAuthority('notification.update') or hasRole('ADMIN')")
        public ResponseEntity<NotificationResponse> markAsRead(
                        @PathVariable UUID id) {

                return ResponseEntity.ok(
                                notificationService.markAsRead(id));
        }

        @PutMapping("/{id}/unread")
        @PreAuthorize("hasAuthority('notification.update') or hasRole('ADMIN')")
        public ResponseEntity<NotificationResponse> markAsUnread(
                        @PathVariable UUID id) {

                return ResponseEntity.ok(
                                notificationService.markAsUnread(id));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('notification.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> delete(
                        @PathVariable UUID id) {

                notificationService.delete(id);

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/user")
        @PreAuthorize("hasAuthority('notification.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> deleteByNotifiable(
                        @RequestParam String notifiableType,
                        @RequestParam @Positive Long notifiableId) {

                notificationService.deleteByNotifiable(
                                notifiableType,
                                notifiableId);

                return ResponseEntity.noContent().build();
        }
}