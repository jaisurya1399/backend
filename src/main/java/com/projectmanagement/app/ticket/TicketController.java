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

        public TicketController(
                        TicketService ticketService) {

                this.ticketService = ticketService;
        }

        // =========================================================
        // GET ALL
        // =========================================================

        @GetMapping
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getAll() {

                return ResponseEntity.ok(
                                ticketService.getAll());
        }

        // =========================================================
        // GET ACTIVE
        // =========================================================

        @GetMapping("/active")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getAllActive() {

                return ResponseEntity.ok(
                                ticketService.getAllActive());
        }

        // =========================================================
        // GET DELETED
        // =========================================================

        @GetMapping("/deleted")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getDeleted() {

                return ResponseEntity.ok(
                                ticketService.getDeleted());
        }

        // =========================================================
        // GET BY CODE
        // =========================================================

        @GetMapping("/code/{code}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> getByCode(
                        @PathVariable String code) {

                return ResponseEntity.ok(
                                ticketService.getByCode(code));
        }

        // =========================================================
        // GET BY PROJECT
        // =========================================================

        @GetMapping("/project/{projectId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByProject(
                        @PathVariable Long projectId) {

                return ResponseEntity.ok(
                                ticketService.getByProject(projectId));
        }

        // =========================================================
        // GET ACTIVE BY PROJECT
        // =========================================================

        @GetMapping("/project/{projectId}/active")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getActiveByProject(
                        @PathVariable Long projectId) {

                return ResponseEntity.ok(
                                ticketService.getActiveByProject(
                                                projectId));
        }

        // =========================================================
        // GET BY OWNER
        // =========================================================

        @GetMapping("/owner/{ownerId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByOwner(
                        @PathVariable Long ownerId) {

                return ResponseEntity.ok(
                                ticketService.getByOwner(ownerId));
        }

        // =========================================================
        // GET BY RESPONSIBLE
        // =========================================================

        @GetMapping("/responsible/{responsibleId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByResponsible(
                        @PathVariable Long responsibleId) {

                return ResponseEntity.ok(
                                ticketService.getByResponsible(
                                                responsibleId));
        }

        // =========================================================
        // GET BY STATUS
        // =========================================================

        @GetMapping("/status/{statusId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByStatus(
                        @PathVariable Long statusId) {

                return ResponseEntity.ok(
                                ticketService.getByStatus(statusId));
        }

        // =========================================================
        // GET BY TYPE
        // =========================================================

        @GetMapping("/type/{typeId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByType(
                        @PathVariable Long typeId) {

                return ResponseEntity.ok(
                                ticketService.getByType(typeId));
        }

        // =========================================================
        // GET BY PRIORITY
        // =========================================================

        @GetMapping("/priority/{priorityId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByPriority(
                        @PathVariable Long priorityId) {

                return ResponseEntity.ok(
                                ticketService.getByPriority(
                                                priorityId));
        }

        // =========================================================
        // GET BY EPIC
        // =========================================================

        @GetMapping("/epic/{epicId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByEpic(
                        @PathVariable Long epicId) {

                return ResponseEntity.ok(
                                ticketService.getByEpic(epicId));
        }

        // =========================================================
        // SEARCH
        // =========================================================

        @GetMapping("/search")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> search(
                        @RequestParam String name) {

                return ResponseEntity.ok(
                                ticketService.search(name));
        }

        // =========================================================
        // SEARCH IN PROJECT
        // =========================================================

        @GetMapping("/project/{projectId}/search")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> searchInProject(
                        @PathVariable Long projectId,
                        @RequestParam String name) {

                return ResponseEntity.ok(
                                ticketService.searchInProject(
                                                projectId,
                                                name));
        }

        // =========================================================
        // GET BY ID
        // =========================================================

        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> getById(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ticketService.getById(id));
        }

        // =========================================================
        // CREATE
        // =========================================================

        @PostMapping
        @PreAuthorize("hasAuthority('ticket.create') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> create(
                        @Valid @RequestBody TicketRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(
                                                ticketService.create(request));
        }

        // =========================================================
        // UPDATE
        // =========================================================

        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> update(
                        @PathVariable Long id,
                        @Valid @RequestBody TicketRequest request) {

                return ResponseEntity.ok(
                                ticketService.update(
                                                id,
                                                request));
        }

        // =========================================================
        // RESTORE
        // =========================================================

        @PutMapping("/{id}/restore")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> restore(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ticketService.restore(id));
        }

        // =========================================================
        // SOFT DELETE
        // =========================================================

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('ticket.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> delete(
                        @PathVariable Long id) {

                ticketService.delete(id);

                return ResponseEntity.noContent().build();
        }

        // =========================================================
        // PERMANENT DELETE
        // =========================================================

        @DeleteMapping("/{id}/permanent")
        @PreAuthorize("hasAuthority('ticket.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> permanentDelete(
                        @PathVariable Long id) {

                ticketService.permanentDelete(id);

                return ResponseEntity.noContent().build();
        }
}