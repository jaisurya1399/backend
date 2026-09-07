package com.projectmanagement.app.sprint;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectmanagement.app.ticket.Ticket;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(
            SprintService sprintService) {
        this.sprintService = sprintService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<SprintResponse> create(
            @Valid @RequestBody SprintRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        sprintService.create(
                                request,
                                userId));
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<List<SprintResponse>> getAll() {

        return ResponseEntity.ok(
                sprintService.getAll());
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<SprintResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                sprintService.getById(id));
    }

    // =========================================================
    // GET PROJECT SPRINTS
    // =========================================================

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SprintResponse>> getByProject(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                sprintService.getByProject(
                        projectId));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<SprintResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SprintRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.update(
                        id,
                        request,
                        userId));
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        sprintService.delete(
                id,
                userId);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // START
    // =========================================================

    @PostMapping("/{id}/start")
    public ResponseEntity<SprintResponse> start(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.start(
                        id,
                        userId));
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    @PostMapping("/{id}/complete")
    public ResponseEntity<SprintResponse> complete(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.complete(
                        id,
                        userId));
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SprintResponse> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.cancel(
                        id,
                        userId));
    }

    // =========================================================
    // SPRINT TICKETS
    // =========================================================

    @GetMapping("/{id}/tickets")
    public ResponseEntity<List<Ticket>> getSprintTickets(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                sprintService.getSprintTickets(id));
    }

    // =========================================================
    // PROJECT BACKLOG
    // =========================================================

    @GetMapping("/project/{projectId}/backlog")
    public ResponseEntity<List<Ticket>> getBacklog(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                sprintService.getBacklog(
                        projectId));
    }

    // =========================================================
    // ADD TICKET
    // =========================================================

    @PostMapping("/{sprintId}/tickets/{ticketId}")
    public ResponseEntity<Ticket> addTicket(
            @PathVariable Long sprintId,
            @PathVariable Long ticketId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.addTicket(
                        sprintId,
                        ticketId,
                        userId));
    }

    // =========================================================
    // REMOVE TICKET
    // =========================================================

    @DeleteMapping("/{sprintId}/tickets/{ticketId}")
    public ResponseEntity<Ticket> removeTicket(
            @PathVariable Long sprintId,
            @PathVariable Long ticketId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.removeTicket(
                        sprintId,
                        ticketId,
                        userId));
    }

    // =========================================================
    // MOVE TO BACKLOG
    // =========================================================

    @PostMapping("/tickets/{ticketId}/backlog")
    public ResponseEntity<Ticket> moveToBacklog(
            @PathVariable Long ticketId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                sprintService.moveToBacklog(
                        ticketId,
                        userId));
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    @GetMapping("/{id}/statistics")
    public ResponseEntity<SprintStatisticsResponse> statistics(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                sprintService.statistics(id));
    }

    // =========================================================
    // USER ID
    // =========================================================

    private Long getUserId(
            Authentication authentication) {

        if (authentication == null) {

            throw new RuntimeException(
                    "Authentication required");
        }

        /*
         * IMPORTANT:
         *
         * Replace this only if your existing
         * Authentication principal exposes user ID
         * differently.
         *
         * If authentication.getName() already returns
         * user ID, this works directly.
         */

        try {

            return Long.valueOf(
                    authentication.getName());

        } catch (NumberFormatException e) {

            throw new RuntimeException(
                    "Unable to determine authenticated user ID");
        }
    }
}