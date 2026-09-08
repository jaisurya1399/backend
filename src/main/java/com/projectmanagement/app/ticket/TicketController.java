package com.projectmanagement.app.ticket;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

        @GetMapping
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getAll(
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getAll(workspaceId)
                                                : ticketService.getAll());
        }

        @GetMapping("/active")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getAllActive(
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getAllActive(workspaceId)
                                                : ticketService.getAllActive());
        }

        @GetMapping("/deleted")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getDeleted(
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getDeleted(workspaceId)
                                                : ticketService.getDeleted());
        }

        @GetMapping("/my-tasks")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getMyTasks(
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getMyTasks(workspaceId)
                                                : ticketService.getMyTasks());
        }

        @GetMapping("/code/{code}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> getByCode(
                        @PathVariable String code,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByCode(code, workspaceId)
                                                : ticketService.getByCode(code));
        }

        @GetMapping("/project/{projectId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByProject(
                        @PathVariable Long projectId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByProject(workspaceId, projectId)
                                                : ticketService.getByProject(projectId));
        }

        @GetMapping("/project/{projectId}/active")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getActiveByProject(
                        @PathVariable Long projectId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getActiveByProject(workspaceId, projectId)
                                                : ticketService.getActiveByProject(projectId));
        }

        @GetMapping("/project/{projectId}/root")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getRootTicketsByProject(
                        @PathVariable Long projectId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getRootTicketsByProject(
                                                                workspaceId,
                                                                projectId)
                                                : ticketService.getRootTicketsByProject(projectId));
        }

        @GetMapping("/project/{projectId}/board")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<BoardColumnResponse>> getBoard(
                        @PathVariable Long projectId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getBoard(workspaceId, projectId)
                                                : ticketService.getBoard(projectId));
        }

        @GetMapping("/{id:\\d+}/children")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getChildren(
                        @PathVariable Long id,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getChildren(workspaceId, id)
                                                : ticketService.getChildren(id));
        }

        @GetMapping("/owner/{ownerId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByOwner(
                        @PathVariable Long ownerId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByOwner(workspaceId, ownerId)
                                                : ticketService.getByOwner(ownerId));
        }

        @GetMapping("/responsible/{responsibleId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByResponsible(
                        @PathVariable Long responsibleId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByResponsible(
                                                                workspaceId,
                                                                responsibleId)
                                                : ticketService.getByResponsible(responsibleId));
        }

        @GetMapping("/status/{statusId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByStatus(
                        @PathVariable Long statusId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByStatus(workspaceId, statusId)
                                                : ticketService.getByStatus(statusId));
        }

        @GetMapping("/type/{typeId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByType(
                        @PathVariable Long typeId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByType(workspaceId, typeId)
                                                : ticketService.getByType(typeId));
        }

        @GetMapping("/priority/{priorityId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByPriority(
                        @PathVariable Long priorityId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByPriority(workspaceId, priorityId)
                                                : ticketService.getByPriority(priorityId));
        }

        @GetMapping("/epic/{epicId}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> getByEpic(
                        @PathVariable Long epicId,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getByEpic(workspaceId, epicId)
                                                : ticketService.getByEpic(epicId));
        }

        @GetMapping("/search")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> search(
                        @RequestParam String keyword,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.search(workspaceId, keyword)
                                                : ticketService.search(keyword));
        }

        @GetMapping("/project/{projectId}/search")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> searchByProject(
                        @PathVariable Long projectId,
                        @RequestParam String keyword,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.searchInProject(
                                                                workspaceId,
                                                                projectId,
                                                                keyword)
                                                : ticketService.searchInProject(
                                                                projectId,
                                                                keyword));
        }

        @GetMapping("/project/{projectId}/filter")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketPageResponse> filter(
                        @PathVariable Long projectId,
                        @Valid @ModelAttribute TicketFilterRequest filter,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.filter(
                                                                workspaceId,
                                                                projectId,
                                                                filter)
                                                : ticketService.filter(
                                                                projectId,
                                                                filter));
        }

        @GetMapping("/{id:\\d+}")
        @PreAuthorize("hasAuthority('ticket.view') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> getById(
                        @PathVariable Long id,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.getById(id, workspaceId)
                                                : ticketService.getById(id));
        }

        @PostMapping
        @PreAuthorize("hasAuthority('ticket.create') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> create(
                        @Valid @RequestBody TicketRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ticketService.create(request));
        }

        @PutMapping("/{id:\\d+}")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> update(
                        @PathVariable Long id,
                        @Valid @RequestBody TicketRequest request) {

                return ResponseEntity.ok(
                                ticketService.update(id, request));
        }

        @PutMapping("/{id:\\d+}/transition")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> transition(
                        @PathVariable Long id,
                        @Valid @RequestBody TicketTransitionRequest request) {

                return ResponseEntity.ok(
                                ticketService.transition(id, request));
        }

        @PutMapping("/project/{projectId}/plan")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<List<TicketResponse>> plan(
                        @PathVariable Long projectId,
                        @Valid @RequestBody TicketPlanningRequest request,
                        @RequestParam(required = false) Long workspaceId) {

                return ResponseEntity.ok(
                                workspaceId != null
                                                ? ticketService.plan(
                                                                workspaceId,
                                                                projectId,
                                                                request)
                                                : ticketService.plan(
                                                                projectId,
                                                                request));
        }

        @PutMapping("/{id:\\d+}/restore")
        @PreAuthorize("hasAuthority('ticket.update') or hasRole('ADMIN')")
        public ResponseEntity<TicketResponse> restore(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ticketService.restore(id));
        }

        @DeleteMapping("/{id:\\d+}")
        @PreAuthorize("hasAuthority('ticket.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> delete(
                        @PathVariable Long id) {

                ticketService.delete(id);

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/{id:\\d+}/permanent")
        @PreAuthorize("hasAuthority('ticket.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> permanentlyDelete(
                        @PathVariable Long id) {

                ticketService.permanentDelete(id);

                return ResponseEntity.noContent().build();
        }
}