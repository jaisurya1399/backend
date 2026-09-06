package com.projectmanagement.app.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.epic.Epic;
import com.projectmanagement.app.epic.EpicRepository;
import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectRepository;
import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

@Service
@Transactional
public class TicketService {

        private final TicketRepository ticketRepository;
        private final ProjectRepository projectRepository;
        private final UserRepository userRepository;
        private final TicketStatusRepository ticketStatusRepository;
        private final TicketTypeRepository ticketTypeRepository;
        private final TicketPriorityRepository ticketPriorityRepository;
        private final EpicRepository epicRepository;

        public TicketService(
                        TicketRepository ticketRepository,
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        TicketStatusRepository ticketStatusRepository,
                        TicketTypeRepository ticketTypeRepository,
                        TicketPriorityRepository ticketPriorityRepository,
                        EpicRepository epicRepository) {

                this.ticketRepository = ticketRepository;
                this.projectRepository = projectRepository;
                this.userRepository = userRepository;
                this.ticketStatusRepository = ticketStatusRepository;
                this.ticketTypeRepository = ticketTypeRepository;
                this.ticketPriorityRepository = ticketPriorityRepository;
                this.epicRepository = epicRepository;
        }

        // ============================================================
        // GET ALL
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getAll() {

                return ticketRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getAllActive() {

                return ticketRepository
                                .findByDeletedAtIsNull()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET DELETED
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getDeleted() {

                return ticketRepository
                                .findByDeletedAtIsNotNull()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET MY TASKS
        // ============================================================
        // Uses currently authenticated user's email from JWT.
        //
        // Then finds the corresponding User record.
        //
        // Finally gets tickets where:
        //
        // tickets.responsible_id = currentUser.id
        //
        // Deleted tickets are excluded.
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getMyTasks() {

                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null ||
                                !authentication.isAuthenticated() ||
                                authentication.getName() == null ||
                                authentication.getName().isBlank()) {

                        throw new RuntimeException(
                                        "User is not authenticated");
                }

                String email = authentication
                                .getName()
                                .trim();

                User currentUser = userRepository
                                .findByEmail(email)
                                .filter(user -> user.getDeletedAt() == null)
                                .orElseThrow(() -> new RuntimeException(
                                                "Current user not found with email: "
                                                                + email));

                return ticketRepository
                                .findByResponsibleIdAndDeletedAtIsNull(
                                                currentUser.getId())
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY ID
        // ============================================================

        @Transactional(readOnly = true)
        public TicketResponse getById(Long id) {

                Ticket ticket = ticketRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: " + id));

                return toResponse(ticket);
        }

        // ============================================================
        // GET BY CODE
        // ============================================================

        @Transactional(readOnly = true)
        public TicketResponse getByCode(String code) {

                Ticket ticket = ticketRepository
                                .findByCode(code)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with code: " + code));

                return toResponse(ticket);
        }

        // ============================================================
        // GET BY PROJECT
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByProject(
                        Long projectId) {

                return ticketRepository
                                .findByProjectIdOrderByOrderAsc(projectId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET ACTIVE BY PROJECT
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getActiveByProject(
                        Long projectId) {

                return ticketRepository
                                .findByProjectIdAndDeletedAtIsNullOrderByOrderAsc(
                                                projectId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY OWNER
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByOwner(
                        Long ownerId) {

                return ticketRepository
                                .findByOwnerId(ownerId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY RESPONSIBLE
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByResponsible(
                        Long responsibleId) {

                return ticketRepository
                                .findByResponsibleId(responsibleId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY STATUS
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByStatus(
                        Long statusId) {

                return ticketRepository
                                .findByStatusId(statusId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY TYPE
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByType(
                        Long typeId) {

                return ticketRepository
                                .findByTypeId(typeId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY PRIORITY
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByPriority(
                        Long priorityId) {

                return ticketRepository
                                .findByPriorityId(priorityId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // GET BY EPIC
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> getByEpic(
                        Long epicId) {

                return ticketRepository
                                .findByEpicId(epicId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // SEARCH
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> search(
                        String name) {

                return ticketRepository
                                .findByNameContainingIgnoreCase(name)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // SEARCH IN PROJECT
        // ============================================================

        @Transactional(readOnly = true)
        public List<TicketResponse> searchInProject(
                        Long projectId,
                        String name) {

                return ticketRepository
                                .findByProjectIdAndNameContainingIgnoreCase(
                                                projectId,
                                                name)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ============================================================
        // CREATE
        // ============================================================

        public TicketResponse create(
                        TicketRequest request) {

                Project project = projectRepository
                                .findById(request.getProjectId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: "
                                                                + request.getProjectId()));

                User owner = userRepository
                                .findById(request.getOwnerId())
                                .filter(user -> user.getDeletedAt() == null)
                                .orElseThrow(() -> new RuntimeException(
                                                "Owner not found with id: "
                                                                + request.getOwnerId()));

                User responsible = null;

                if (request.getResponsibleId() != null) {

                        responsible = userRepository
                                        .findById(request.getResponsibleId())
                                        .filter(user -> user.getDeletedAt() == null)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Responsible user not found with id: "
                                                                        + request.getResponsibleId()));
                }

                TicketStatus status = ticketStatusRepository
                                .findById(request.getStatusId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket status not found with id: "
                                                                + request.getStatusId()));

                TicketType type = ticketTypeRepository
                                .findById(request.getTypeId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket type not found with id: "
                                                                + request.getTypeId()));

                TicketPriority priority = ticketPriorityRepository
                                .findById(request.getPriorityId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket priority not found with id: "
                                                                + request.getPriorityId()));

                Epic epic = null;

                if (request.getEpicId() != null) {

                        epic = epicRepository
                                        .findById(request.getEpicId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Epic not found with id: "
                                                                        + request.getEpicId()));

                        validateEpicBelongsToProject(
                                        epic,
                                        project);
                }

                String code = request.getCode();

                if (code == null || code.isBlank()) {

                        code = generateTicketCode(project);

                } else {

                        code = code.trim();

                        if (ticketRepository.existsByCode(code)) {

                                throw new RuntimeException(
                                                "Ticket code already exists: " + code);
                        }
                }

                Ticket ticket = Ticket.builder()
                                .name(request.getName().trim())
                                .content(request.getContent())
                                .owner(owner)
                                .responsible(responsible)
                                .status(status)
                                .project(project)
                                .code(code)
                                .type(type)
                                .order(
                                                request.getOrder() != null
                                                                ? request.getOrder()
                                                                : 0)
                                .priority(priority)
                                .estimation(
                                                request.getEstimation() != null
                                                                ? request.getEstimation()
                                                                : BigDecimal.ZERO)
                                .epic(epic)
                                .build();

                Ticket saved = ticketRepository.save(ticket);

                return toResponse(saved);
        }

        // ============================================================
        // UPDATE
        // ============================================================

        public TicketResponse update(
                        Long id,
                        TicketRequest request) {

                Ticket ticket = ticketRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: " + id));

                Project project = projectRepository
                                .findById(request.getProjectId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Project not found with id: "
                                                                + request.getProjectId()));

                User owner = userRepository
                                .findById(request.getOwnerId())
                                .filter(user -> user.getDeletedAt() == null)
                                .orElseThrow(() -> new RuntimeException(
                                                "Owner not found with id: "
                                                                + request.getOwnerId()));

                User responsible = null;

                if (request.getResponsibleId() != null) {

                        responsible = userRepository
                                        .findById(request.getResponsibleId())
                                        .filter(user -> user.getDeletedAt() == null)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Responsible user not found with id: "
                                                                        + request.getResponsibleId()));
                }

                TicketStatus status = ticketStatusRepository
                                .findById(request.getStatusId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket status not found with id: "
                                                                + request.getStatusId()));

                TicketType type = ticketTypeRepository
                                .findById(request.getTypeId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket type not found with id: "
                                                                + request.getTypeId()));

                TicketPriority priority = ticketPriorityRepository
                                .findById(request.getPriorityId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket priority not found with id: "
                                                                + request.getPriorityId()));

                Epic epic = null;

                if (request.getEpicId() != null) {

                        epic = epicRepository
                                        .findById(request.getEpicId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Epic not found with id: "
                                                                        + request.getEpicId()));

                        validateEpicBelongsToProject(
                                        epic,
                                        project);
                }

                if (request.getCode() != null &&
                                !request.getCode().isBlank()) {

                        String newCode = request.getCode().trim();

                        if (!newCode.equals(ticket.getCode()) &&
                                        ticketRepository.existsByCodeAndIdNot(
                                                        newCode,
                                                        id)) {

                                throw new RuntimeException(
                                                "Ticket code already exists: "
                                                                + newCode);
                        }

                        ticket.setCode(newCode);
                }

                ticket.setName(request.getName().trim());
                ticket.setContent(request.getContent());
                ticket.setOwner(owner);
                ticket.setResponsible(responsible);
                ticket.setStatus(status);
                ticket.setProject(project);
                ticket.setType(type);

                ticket.setOrder(
                                request.getOrder() != null
                                                ? request.getOrder()
                                                : 0);

                ticket.setPriority(priority);

                ticket.setEstimation(
                                request.getEstimation() != null
                                                ? request.getEstimation()
                                                : BigDecimal.ZERO);

                ticket.setEpic(epic);

                Ticket updated = ticketRepository.save(ticket);

                return toResponse(updated);
        }

        // ============================================================
        // SOFT DELETE
        // ============================================================

        public void delete(Long id) {

                Ticket ticket = ticketRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: " + id));

                ticket.setDeletedAt(LocalDateTime.now());

                ticketRepository.save(ticket);
        }

        // ============================================================
        // RESTORE
        // ============================================================

        public TicketResponse restore(Long id) {

                Ticket ticket = ticketRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: " + id));

                ticket.setDeletedAt(null);

                return toResponse(
                                ticketRepository.save(ticket));
        }

        // ============================================================
        // PERMANENT DELETE
        // ============================================================

        public void permanentDelete(Long id) {

                if (!ticketRepository.existsById(id)) {

                        throw new RuntimeException(
                                        "Ticket not found with id: " + id);
                }

                ticketRepository.deleteById(id);
        }

        // ============================================================
        // GENERATE TICKET CODE
        // ============================================================

        private String generateTicketCode(
                        Project project) {

                String prefix = project.getTicketPrefix();

                if (prefix == null || prefix.isBlank()) {
                        prefix = "TICKET";
                }

                prefix = prefix.trim();

                long count = ticketRepository.countByProjectId(
                                project.getId());

                String code;

                do {

                        count++;

                        code = prefix + "-" + count;

                } while (ticketRepository.existsByCode(code));

                return code;
        }

        // ============================================================
        // VALIDATE EPIC PROJECT
        // ============================================================

        private void validateEpicBelongsToProject(
                        Epic epic,
                        Project project) {

                if (epic.getProject() == null ||
                                !epic.getProject()
                                                .getId()
                                                .equals(project.getId())) {

                        throw new RuntimeException(
                                        "Epic does not belong to the selected project");
                }
        }

        // ============================================================
        // ENTITY -> RESPONSE
        // ============================================================

        private TicketResponse toResponse(
                        Ticket ticket) {

                Long ownerId = null;
                String ownerName = null;
                String ownerEmail = null;

                if (ticket.getOwner() != null) {

                        ownerId = ticket.getOwner().getId();
                        ownerName = ticket.getOwner().getName();
                        ownerEmail = ticket.getOwner().getEmail();
                }

                Long responsibleId = null;
                String responsibleName = null;
                String responsibleEmail = null;

                if (ticket.getResponsible() != null) {

                        responsibleId = ticket.getResponsible().getId();
                        responsibleName = ticket.getResponsible().getName();
                        responsibleEmail = ticket.getResponsible().getEmail();
                }

                Long statusId = null;
                String statusName = null;
                String statusColor = null;

                if (ticket.getStatus() != null) {

                        statusId = ticket.getStatus().getId();
                        statusName = ticket.getStatus().getName();
                        statusColor = ticket.getStatus().getColor();
                }

                Long projectId = null;
                String projectName = null;

                if (ticket.getProject() != null) {

                        projectId = ticket.getProject().getId();
                        projectName = ticket.getProject().getName();
                }

                Long typeId = null;
                String typeName = null;
                String typeIcon = null;
                String typeColor = null;

                if (ticket.getType() != null) {

                        typeId = ticket.getType().getId();
                        typeName = ticket.getType().getName();
                        typeIcon = ticket.getType().getIcon();
                        typeColor = ticket.getType().getColor();
                }

                Long priorityId = null;
                String priorityName = null;
                String priorityColor = null;

                if (ticket.getPriority() != null) {

                        priorityId = ticket.getPriority().getId();
                        priorityName = ticket.getPriority().getName();
                        priorityColor = ticket.getPriority().getColor();
                }

                Long epicId = null;
                String epicName = null;

                if (ticket.getEpic() != null) {

                        epicId = ticket.getEpic().getId();
                        epicName = ticket.getEpic().getName();
                }

                return TicketResponse.builder()
                                .id(ticket.getId())
                                .name(ticket.getName())
                                .content(ticket.getContent())

                                .ownerId(ownerId)
                                .ownerName(ownerName)
                                .ownerEmail(ownerEmail)

                                .responsibleId(responsibleId)
                                .responsibleName(responsibleName)
                                .responsibleEmail(responsibleEmail)

                                .statusId(statusId)
                                .statusName(statusName)
                                .statusColor(statusColor)

                                .projectId(projectId)
                                .projectName(projectName)

                                .code(ticket.getCode())

                                .typeId(typeId)
                                .typeName(typeName)
                                .typeIcon(typeIcon)
                                .typeColor(typeColor)

                                .order(ticket.getOrder())

                                .priorityId(priorityId)
                                .priorityName(priorityName)
                                .priorityColor(priorityColor)

                                .estimation(ticket.getEstimation())

                                .epicId(epicId)
                                .epicName(epicName)

                                .deletedAt(ticket.getDeletedAt())
                                .createdAt(ticket.getCreatedAt())
                                .updatedAt(ticket.getUpdatedAt())

                                .build();
        }
}