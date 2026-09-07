package com.projectmanagement.app.sprint;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectRepository;
import com.projectmanagement.app.project.ProjectUserRepository;
import com.projectmanagement.app.project.ProjectAccessService;
import com.projectmanagement.app.ticket.Ticket;
import com.projectmanagement.app.ticket.TicketRepository;
import com.projectmanagement.app.ticket.TicketStatusCategory;
import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

@Service
@Transactional
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    public SprintService(
            SprintRepository sprintRepository,
            ProjectRepository projectRepository,
            ProjectUserRepository projectUserRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.projectUserRepository = projectUserRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    public SprintResponse create(
            SprintRequest request,
            Long userId) {

        Project project = getProject(request.getProjectId());

        validateProjectAccess(
                project.getId(),
                userId);

        validateSprintName(
                project.getId(),
                request.getName(),
                null);

        validateDates(
                request.getStartDate(),
                request.getEndDate());

        if (request.getStatus() == SprintStatus.ACTIVE) {

            validateNoActiveSprint(
                    project.getId());
        }

        User user = getUser(userId);

        Sprint sprint = Sprint.builder()
                .name(request.getName().trim())
                .goal(request.getGoal())
                .project(project)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(
                        request.getStatus() == null
                                ? SprintStatus.PLANNED
                                : request.getStatus())
                .createdBy(user)
                .build();

        return toResponse(
                sprintRepository.save(sprint));
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<SprintResponse> getAll() {

        return sprintRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public SprintResponse getById(Long id) {

        Sprint sprint = getSprint(id);

        return toResponse(sprint);
    }

    // =========================================================
    // GET PROJECT SPRINTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<SprintResponse> getByProject(
            Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found");
        }

        return sprintRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public SprintResponse update(
            Long id,
            SprintRequest request,
            Long userId) {

        Sprint sprint = getSprint(id);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        if (!sprint.getProject().getId()
                .equals(request.getProjectId())) {

            throw new RuntimeException(
                    "Sprint project cannot be changed");
        }

        validateSprintName(
                request.getProjectId(),
                request.getName(),
                id);

        validateDates(
                request.getStartDate(),
                request.getEndDate());

        SprintStatus newStatus = request.getStatus() == null
                ? sprint.getStatus()
                : request.getStatus();

        if (newStatus == SprintStatus.ACTIVE
                && sprint.getStatus() != SprintStatus.ACTIVE) {

            validateNoActiveSprint(
                    sprint.getProject().getId());
        }

        if (sprint.getStatus() == SprintStatus.COMPLETED
                && newStatus != SprintStatus.COMPLETED) {

            throw new RuntimeException(
                    "Completed sprint cannot be reopened");
        }

        sprint.setName(request.getName().trim());
        sprint.setGoal(request.getGoal());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setStatus(newStatus);

        return toResponse(
                sprintRepository.save(sprint));
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void delete(
            Long id,
            Long userId) {

        Sprint sprint = getSprint(id);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        if (sprint.getStatus() == SprintStatus.ACTIVE) {

            throw new RuntimeException(
                    "Active sprint cannot be deleted");
        }

        List<Ticket> tickets = ticketRepository.findBySprintIdOrderByOrderAsc(id);

        for (Ticket ticket : tickets) {
            ticket.setSprint(null);
        }

        ticketRepository.saveAll(tickets);

        sprintRepository.delete(sprint);
    }

    // =========================================================
    // START
    // =========================================================

    public SprintResponse start(
            Long id,
            Long userId) {

        Sprint sprint = getSprint(id);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        if (sprint.getStatus() == SprintStatus.COMPLETED) {

            throw new RuntimeException(
                    "Completed sprint cannot be started");
        }

        if (sprint.getStatus() == SprintStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cancelled sprint cannot be started");
        }

        validateNoActiveSprint(
                sprint.getProject().getId());

        sprint.setStatus(SprintStatus.ACTIVE);

        return toResponse(
                sprintRepository.save(sprint));
    }

    // =========================================================
    // COMPLETE
    // =========================================================

    public SprintResponse complete(
            Long id,
            Long userId) {

        Sprint sprint = getSprint(id);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        if (sprint.getStatus() != SprintStatus.ACTIVE) {

            throw new RuntimeException(
                    "Only active sprint can be completed");
        }

        sprint.setStatus(
                SprintStatus.COMPLETED);

        return toResponse(
                sprintRepository.save(sprint));
    }

    // =========================================================
    // CANCEL
    // =========================================================

    public SprintResponse cancel(
            Long id,
            Long userId) {

        Sprint sprint = getSprint(id);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        if (sprint.getStatus() == SprintStatus.COMPLETED) {

            throw new RuntimeException(
                    "Completed sprint cannot be cancelled");
        }

        sprint.setStatus(
                SprintStatus.CANCELLED);

        return toResponse(
                sprintRepository.save(sprint));
    }

    // =========================================================
    // GET SPRINT TICKETS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Ticket> getSprintTickets(
            Long sprintId) {

        getSprint(sprintId);

        return ticketRepository
                .findBySprintIdOrderByOrderAsc(sprintId);
    }

    // =========================================================
    // GET PROJECT BACKLOG
    // =========================================================

    @Transactional(readOnly = true)
    public List<Ticket> getBacklog(
            Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException(
                    "Project not found");
        }

        return ticketRepository
                .findByProjectIdAndSprintIsNullOrderByOrderAsc(
                        projectId);
    }

    // =========================================================
    // ADD TICKET TO SPRINT
    // =========================================================

    public Ticket addTicket(
            Long sprintId,
            Long ticketId,
            Long userId) {

        Sprint sprint = getSprint(sprintId);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        Ticket ticket = ticketRepository
                .findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket not found"));

        if (!ticket.getProject().getId()
                .equals(sprint.getProject().getId())) {

            throw new RuntimeException(
                    "Ticket does not belong to sprint project");
        }

        if (sprint.getStatus() == SprintStatus.COMPLETED
                || sprint.getStatus() == SprintStatus.CANCELLED) {

            throw new RuntimeException(
                    "Ticket cannot be added to completed/cancelled sprint");
        }

        ticket.setSprint(sprint);

        return ticketRepository.save(ticket);
    }

    // =========================================================
    // REMOVE TICKET FROM SPRINT
    // =========================================================

    public Ticket removeTicket(
            Long sprintId,
            Long ticketId,
            Long userId) {

        Sprint sprint = getSprint(sprintId);

        validateProjectAccess(
                sprint.getProject().getId(),
                userId);

        Ticket ticket = ticketRepository
                .findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket not found"));

        if (ticket.getSprint() == null
                || !ticket.getSprint().getId()
                        .equals(sprintId)) {

            throw new RuntimeException(
                    "Ticket is not assigned to this sprint");
        }

        ticket.setSprint(null);

        return ticketRepository.save(ticket);
    }

    // =========================================================
    // MOVE TICKET TO BACKLOG
    // =========================================================

    public Ticket moveToBacklog(
            Long ticketId,
            Long userId) {

        Ticket ticket = ticketRepository
                .findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket not found"));

        validateProjectAccess(
                ticket.getProject().getId(),
                userId);

        ticket.setSprint(null);

        return ticketRepository.save(ticket);
    }

    // =========================================================
    // SPRINT STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    public SprintStatisticsResponse statistics(
            Long sprintId) {

        Sprint sprint = getSprint(sprintId);

        List<Ticket> tickets = ticketRepository
                .findBySprintIdOrderByOrderAsc(
                        sprintId);

        BigDecimal totalEstimation = tickets.stream()
                .map(Ticket::getEstimation)
                .filter(value -> value != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);

        /*
         * Completed estimation can be calculated later
         * according to your TicketStatus configuration.
         *
         * Currently we calculate total sprint estimation
         * safely without assuming a specific status name.
         */

        BigDecimal completedEstimation = tickets.stream()
                .filter(ticket -> ticket.getStatus().getCategory() == TicketStatusCategory.DONE)
                .map(Ticket::getEstimation)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingEstimation = totalEstimation.subtract(
                completedEstimation);

        return SprintStatisticsResponse.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .totalTickets(
                        (long) tickets.size())
                .assignedTickets(
                        (long) tickets.size())
                .backlogTickets(0L)
                .totalEstimation(
                        totalEstimation)
                .completedEstimation(
                        completedEstimation)
                .remainingEstimation(
                        remainingEstimation)
                .build();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Sprint getSprint(Long id) {

        return sprintRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Sprint not found"));
    }

    private Project getProject(Long id) {

        return projectRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Project not found"));
    }

    private User getUser(Long id) {

        return userRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "User not found"));
    }

    private void validateProjectAccess(
            Long projectId,
            Long userId) {

        if (userId == null) {
            throw new RuntimeException(
                    "Authenticated user not found");
        }

        projectAccessService.requireEditor(getProject(projectId));
    }

    private void validateNoActiveSprint(
            Long projectId) {

        boolean active = sprintRepository
                .existsByProjectIdAndStatus(
                        projectId,
                        SprintStatus.ACTIVE);

        if (active) {

            throw new RuntimeException(
                    "Another active sprint already exists for this project");
        }
    }

    private void validateSprintName(
            Long projectId,
            String name,
            Long currentSprintId) {

        boolean exists;

        if (currentSprintId == null) {

            exists = sprintRepository
                    .existsByProjectIdAndName(
                            projectId,
                            name.trim());

        } else {

            exists = sprintRepository
                    .existsByProjectIdAndNameAndIdNot(
                            projectId,
                            name.trim(),
                            currentSprintId);
        }

        if (exists) {

            throw new RuntimeException(
                    "Sprint with this name already exists in this project");
        }
    }

    private void validateDates(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {

        if (startDate != null
                && endDate != null
                && endDate.isBefore(startDate)) {

            throw new RuntimeException(
                    "End date cannot be before start date");
        }
    }

    private SprintResponse toResponse(
            Sprint sprint) {

        Long ticketCount = ticketRepository.countBySprintId(
                sprint.getId());

        BigDecimal totalEstimation = ticketRepository
                .findBySprintIdOrderByOrderAsc(
                        sprint.getId())
                .stream()
                .map(Ticket::getEstimation)
                .filter(value -> value != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);

        return SprintResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .projectId(
                        sprint.getProject().getId())
                .projectName(
                        sprint.getProject().getName())
                .startDate(
                        sprint.getStartDate())
                .endDate(
                        sprint.getEndDate())
                .status(
                        sprint.getStatus())
                .createdBy(
                        sprint.getCreatedBy() == null
                                ? null
                                : sprint.getCreatedBy().getId())
                .createdAt(
                        sprint.getCreatedAt())
                .updatedAt(
                        sprint.getUpdatedAt())
                .ticketCount(ticketCount)
                .totalEstimation(
                        totalEstimation)
                .build();
    }
}
