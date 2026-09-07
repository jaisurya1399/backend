package com.projectmanagement.app.ticket;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

        private final TicketService ticketService;

        public TicketController(TicketService ticketService) {
                this.ticketService = ticketService;
        }

        // ============================================================
        // GET ALL TICKETS
        // ============================================================

        @GetMapping
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getAll() {

                return ResponseEntity.ok(
                                ticketService.getAll());
        }

        // ============================================================
        // GET ACTIVE TICKETS
        // ============================================================

        @GetMapping("/active")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getAllActive() {

                return ResponseEntity.ok(
                                ticketService.getAllActive());
        }

        // ============================================================
        // GET DELETED TICKETS
        // ============================================================

        @GetMapping("/deleted")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getDeleted() {

                return ResponseEntity.ok(
                                ticketService.getDeleted());
        }

        // ============================================================
        // GET MY TASKS
        // ============================================================
        // Returns tickets where responsible_id belongs to
        // currently authenticated user.
        //
        // Example:
        // GET /api/tickets/my-tasks
        // ============================================================

        @GetMapping("/my-tasks")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getMyTasks() {

                return ResponseEntity.ok(
                                ticketService.getMyTasks());
        }

        // ============================================================
        // GET TICKET BY CODE
        // ============================================================

        @GetMapping("/code/{code}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> getByCode(
                        @PathVariable String code) {

                return ResponseEntity.ok(
                                ticketService.getByCode(code));
        }

        // ============================================================
        // GET TICKETS BY PROJECT
        // ============================================================

        @GetMapping("/project/{projectId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByProject(
                        @PathVariable Long projectId) {

                return ResponseEntity.ok(
                                ticketService.getByProject(projectId));
        }

        // ============================================================
        // GET ACTIVE TICKETS BY PROJECT
        // ============================================================

        @GetMapping("/project/{projectId}/active")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getActiveByProject(
                        @PathVariable Long projectId) {

                return ResponseEntity.ok(
                                ticketService.getActiveByProject(projectId));
        }

        @GetMapping("/project/{projectId}/root")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getRootTicketsByProject(@PathVariable Long projectId) {
                return ResponseEntity.ok(ticketService.getRootTicketsByProject(projectId));
        }

        @GetMapping("/{id:\\d+}/children")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getChildren(@PathVariable Long id) {
                return ResponseEntity.ok(ticketService.getChildren(id));
        }

        // ============================================================
        // GET TICKETS BY OWNER
        // ============================================================

        @GetMapping("/owner/{ownerId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByOwner(
                        @PathVariable Long ownerId) {

                return ResponseEntity.ok(
                                ticketService.getByOwner(ownerId));
        }

        // ============================================================
        // GET TICKETS BY RESPONSIBLE USER
        // ============================================================

        @GetMapping("/responsible/{responsibleId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByResponsible(
                        @PathVariable Long responsibleId) {

                return ResponseEntity.ok(
                                ticketService.getByResponsible(responsibleId));
        }

        // ============================================================
        // GET TICKETS BY STATUS
        // ============================================================

        @GetMapping("/status/{statusId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByStatus(
                        @PathVariable Long statusId) {

                return ResponseEntity.ok(
                                ticketService.getByStatus(statusId));
        }

        // ============================================================
        // GET TICKETS BY TYPE
        // ============================================================

        @GetMapping("/type/{typeId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByType(
                        @PathVariable Long typeId) {

                return ResponseEntity.ok(
                                ticketService.getByType(typeId));
        }

        // ============================================================
        // GET TICKETS BY PRIORITY
        // ============================================================

        @GetMapping("/priority/{priorityId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByPriority(
                        @PathVariable Long priorityId) {

                return ResponseEntity.ok(
                                ticketService.getByPriority(priorityId));
        }

        // ============================================================
        // GET TICKETS BY EPIC
        // ============================================================

        @GetMapping("/epic/{epicId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByEpic(
                        @PathVariable Long epicId) {

                return ResponseEntity.ok(
                                ticketService.getByEpic(epicId));
        }

        // ============================================================
        // SEARCH TICKETS
        // ============================================================

        @GetMapping("/search")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> search(
                        @RequestParam String keyword) {

                return ResponseEntity.ok(
                                ticketService.search(keyword));
        }

        // ============================================================
        // SEARCH TICKETS BY PROJECT
        // ============================================================

        @GetMapping("/project/{projectId}/search")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> searchByProject(
                        @PathVariable Long projectId,
                        @RequestParam String keyword) {

                return ResponseEntity.ok(
                                ticketService.searchInProject(
                                                projectId,
                                                keyword));
        }

        // ============================================================
        // GET TICKET BY ID
        // ============================================================
        // Numeric-only ID prevents:
        //
        // /api/tickets/my-tasks
        //
        // from being interpreted as:
        //
        // /api/tickets/{id}
        // ============================================================

        @GetMapping("/{id:\\d+}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> getById(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ticketService.getById(id));
        }

        // ============================================================
        // CREATE TICKET
        // ============================================================

        @PostMapping
        @PreAuthorize("hasAuthority('ticket.create') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> create(
                        @Valid @RequestBody TicketRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ticketService.create(request));
        }

        // ============================================================
        // UPDATE TICKET
        // ============================================================

        @PutMapping("/{id:\\d+}")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> update(
                        @PathVariable Long id,
                        @Valid @RequestBody TicketRequest request) {

                return ResponseEntity.ok(
                                ticketService.update(id, request));
        }

        // ============================================================
        // RESTORE TICKET
        // ============================================================

        @PutMapping("/{id:\\d+}/restore")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> restore(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ticketService.restore(id));
        }

        // ============================================================
        // SOFT DELETE TICKET
        // ============================================================

        @DeleteMapping("/{id:\\d+}")
        @PreAuthorize("hasAuthority('ticket.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> delete(
                        @PathVariable Long id) {

                ticketService.delete(id);

                return ResponseEntity.noContent().build();
        }

        // ============================================================
        // PERMANENT DELETE TICKET
        // ============================================================

        @DeleteMapping("/{id:\\d+}/permanent")
        @PreAuthorize("hasAuthority('ticket.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> permanentlyDelete(
                        @PathVariable Long id) {

                ticketService.permanentDelete(id);

                return ResponseEntity.noContent().build();
        }
}
