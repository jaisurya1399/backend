package com.projectmanagement.app.sprint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.project.MemberAvailability;
import com.projectmanagement.app.project.MemberAvailabilityRepository;
import com.projectmanagement.app.project.MemberAvailabilityType;
import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectAccessService;
import com.projectmanagement.app.project.ProjectRepository;
import com.projectmanagement.app.project.ProjectUser;
import com.projectmanagement.app.project.ProjectUserRepository;
import com.projectmanagement.app.project.ProjectWorkingHours;
import com.projectmanagement.app.project.ProjectWorkingHoursRepository;
import com.projectmanagement.app.ticket.Ticket;
import com.projectmanagement.app.ticket.TicketRepository;
import com.projectmanagement.app.ticket.TicketResponse;
import com.projectmanagement.app.ticket.TicketService;
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
        private final SprintIssueSnapshotRepository snapshotRepository;
        private final SprintCapacityRepository capacityRepository;
        private final MemberAvailabilityRepository availabilityRepository;
        private final ProjectWorkingHoursRepository workingHoursRepository;
        private final TicketService ticketService;

        public SprintService(
                        SprintRepository sprintRepository,
                        ProjectRepository projectRepository,
                        ProjectUserRepository projectUserRepository,
                        TicketRepository ticketRepository,
                        UserRepository userRepository,
                        ProjectAccessService projectAccessService,
                        SprintIssueSnapshotRepository snapshotRepository,
                        SprintCapacityRepository capacityRepository,
                        MemberAvailabilityRepository availabilityRepository,
                        ProjectWorkingHoursRepository workingHoursRepository,
                        TicketService ticketService) {
                this.sprintRepository = sprintRepository;
                this.projectRepository = projectRepository;
                this.projectUserRepository = projectUserRepository;
                this.ticketRepository = ticketRepository;
                this.userRepository = userRepository;
                this.projectAccessService = projectAccessService;
                this.snapshotRepository = snapshotRepository;
                this.capacityRepository = capacityRepository;
                this.availabilityRepository = availabilityRepository;
                this.workingHoursRepository = workingHoursRepository;
                this.ticketService = ticketService;
        }

        // =========================================================
        // CREATE
        // =========================================================
        public SprintResponse create(
                        SprintRequest request,
                        Long userId) {

                Project project = getProject(request.getProjectId());

                validateProjectAccess(project.getId(), userId);

                validateSprintName(project.getId(), request.getName(), null);

                validateDates(request.getStartDate(), request.getEndDate());

                if (request.getStatus() == SprintStatus.ACTIVE) {
                        validateNoActiveSprint(project.getId());
                }

                User user = getUser(userId);

                Sprint sprint = Sprint.builder()
                                .name(request.getName().trim())
                                .goal(request.getGoal())
                                .project(project)
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .status(request.getStatus() == null ? SprintStatus.PLANNED : request.getStatus())
                                .createdBy(user)
                                .build();

                return toResponse(sprintRepository.save(sprint));
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
        public List<SprintResponse> getByProject(Long projectId) {
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

                validateProjectAccess(sprint.getProject().getId(), userId);

                if (!sprint.getProject().getId().equals(request.getProjectId())) {
                        throw new RuntimeException("Sprint project cannot be changed");
                }

                validateSprintName(request.getProjectId(), request.getName(), id);

                validateDates(request.getStartDate(), request.getEndDate());

                SprintStatus newStatus = request.getStatus() == null ? sprint.getStatus() : request.getStatus();

                if (newStatus == SprintStatus.ACTIVE && sprint.getStatus() != SprintStatus.ACTIVE) {
                        validateNoActiveSprint(sprint.getProject().getId());
                }

                if (sprint.getStatus() == SprintStatus.COMPLETED && newStatus != SprintStatus.COMPLETED) {
                        throw new RuntimeException("Completed sprint cannot be reopened");
                }

                sprint.setName(request.getName().trim());
                sprint.setGoal(request.getGoal());
                sprint.setStartDate(request.getStartDate());
                sprint.setEndDate(request.getEndDate());
                sprint.setStatus(newStatus);

                return toResponse(sprintRepository.save(sprint));
        }

        // =========================================================
        // DELETE
        // =========================================================
        public void delete(Long id, Long userId) {
                Sprint sprint = getSprint(id);

                validateProjectAccess(sprint.getProject().getId(), userId);

                if (sprint.getStatus() == SprintStatus.ACTIVE) {
                        throw new RuntimeException("Active sprint cannot be deleted");
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
        public SprintResponse start(Long id, Long userId) {
                Sprint sprint = getSprint(id);

                validateProjectAccess(sprint.getProject().getId(), userId);

                if (sprint.getStatus() == SprintStatus.COMPLETED) {
                        throw new RuntimeException("Completed sprint cannot be started");
                }
                if (sprint.getStatus() == SprintStatus.CANCELLED) {
                        throw new RuntimeException("Cancelled sprint cannot be started");
                }

                validateNoActiveSprint(sprint.getProject().getId());

                List<Ticket> sprintTickets = ticketRepository.findBySprintIdOrderByOrderAsc(id);
                if (snapshotRepository.findBySprintIdOrderByTicketIdAsc(id).isEmpty() && !sprintTickets.isEmpty()) {
                        snapshotRepository.saveAll(sprintTickets.stream().map(ticket -> SprintIssueSnapshot.builder()
                                        .sprint(sprint)
                                        .ticketId(ticket.getId())
                                        .estimation(ticket.getEstimation())
                                        .resolvedAt(null)
                                        .finalStatusCategory(ticket.getStatus().getCategory())
                                        .build()).toList());
                }

                sprint.setStatus(SprintStatus.ACTIVE);
                return toResponse(sprintRepository.save(sprint));
        }

        // =========================================================
        // COMPLETE
        // =========================================================
        public SprintResponse complete(Long id, Long userId) {
                return complete(id, SprintCompletionRequest.builder().moveIncompleteToBacklog(true).build(), userId);
        }

        public SprintResponse complete(Long id, SprintCompletionRequest request, Long userId) {
                Sprint sprint = getSprint(id);

                validateProjectAccess(sprint.getProject().getId(), userId);

                if (sprint.getStatus() != SprintStatus.ACTIVE) {
                        throw new RuntimeException("Only active sprint can be completed");
                }

                Sprint carryOverSprint = null;
                if (request.getCarryOverSprintId() != null) {
                        carryOverSprint = sprintRepository
                                        .findByIdAndProjectId(
                                                        request.getCarryOverSprintId(),
                                                        sprint.getProject().getId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Carry-over sprint does not belong to this project"));

                        if (carryOverSprint.getId().equals(sprint.getId())) {
                                throw new RuntimeException("A sprint cannot carry over into itself");
                        }

                        if (carryOverSprint.getStatus() == SprintStatus.COMPLETED
                                        || carryOverSprint.getStatus() == SprintStatus.CANCELLED) {
                                throw new RuntimeException("Cannot carry issues into a completed or cancelled sprint");
                        }
                }

                List<Ticket> sprintTickets = ticketRepository.findBySprintIdOrderByOrderAsc(id);

                List<SprintIssueSnapshot> snapshots = snapshotRepository.findBySprintIdOrderByTicketIdAsc(id);
                Map<Long, SprintIssueSnapshot> snapshotByTicket = new LinkedHashMap<>();
                snapshots.forEach(snapshot -> snapshotByTicket.put(snapshot.getTicketId(), snapshot));
                List<SprintIssueSnapshot> finalSnapshots = new ArrayList<>();
                for (Ticket ticket : sprintTickets) {
                        SprintIssueSnapshot snapshot = snapshotByTicket.get(ticket.getId());
                        if (snapshot == null) {
                                // A ticket added after sprint start has no baseline snapshot.
                                // Capture its final sprint value here so completed-sprint
                                // history remains complete without changing the original baseline.
                                snapshot = SprintIssueSnapshot.builder()
                                                .sprint(sprint)
                                                .ticketId(ticket.getId())
                                                .estimation(nz(ticket.getEstimation()))
                                                .build();
                        }
                        snapshot.setEstimation(nz(snapshot.getEstimation()).signum() == 0
                                        ? nz(ticket.getEstimation())
                                        : snapshot.getEstimation());
                        snapshot.setResolvedAt(ticket.getResolvedAt());
                        snapshot.setFinalStatusCategory(ticket.getStatus().getCategory());
                        finalSnapshots.add(snapshot);
                }
                snapshotRepository.saveAll(finalSnapshots);

                List<Ticket> incomplete = sprintTickets.stream()
                                .filter(ticket -> ticket.getStatus().getCategory() != TicketStatusCategory.DONE
                                                && ticket.getStatus().getCategory() != TicketStatusCategory.CANCELLED)
                                .toList();

                // --- FIX: make captured variable effectively final for lambda usage
                final Sprint finalCarryOverSprint = carryOverSprint;

                if (finalCarryOverSprint != null) {
                        incomplete.forEach(ticket -> ticket.setSprint(finalCarryOverSprint));
                } else if (Boolean.TRUE.equals(request.getMoveIncompleteToBacklog())) {
                        incomplete.forEach(ticket -> ticket.setSprint(null));
                }

                ticketRepository.saveAll(incomplete);

                sprint.setStatus(SprintStatus.COMPLETED);
                return toResponse(sprintRepository.save(sprint));
        }

        // =========================================================
        // BURNDOWN
        // =========================================================
        public SprintBurndownResponse burndown(Long sprintId) {
                Sprint sprint = getSprint(sprintId);
                projectAccessService.requireView(sprint.getProject());

                List<SprintIssueSnapshot> snapshots = snapshotRepository.findBySprintIdOrderByTicketIdAsc(sprintId);
                Map<Long, BigDecimal> committedByTicket = new LinkedHashMap<>();
                snapshots.forEach(s -> committedByTicket.put(s.getTicketId(), nz(s.getEstimation())));

                List<Ticket> liveTickets = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId).stream()
                                .filter(t -> t.getDeletedAt() == null).toList();
                // ACTIVE/PLANNED sprints use the live sprint scope for the burndown.
                // The snapshot is the immutable commitment baseline and must not hide
                // tickets that were added to the sprint later from the current burndown.
                boolean historicalSprint = sprint.getStatus() == SprintStatus.COMPLETED
                                || sprint.getStatus() == SprintStatus.CANCELLED;
                if (!historicalSprint) {
                        committedByTicket.clear();
                        liveTickets.forEach(t -> committedByTicket.put(t.getId(), nz(t.getEstimation())));
                } else if (snapshots.isEmpty()) {
                        liveTickets.forEach(t -> committedByTicket.put(t.getId(), nz(t.getEstimation())));
                }

                BigDecimal total = committedByTicket.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                // Sprint analytics works with calendar dates only.
                LocalDate start = sprint.getStartDate() == null
                                ? dateOnly(sprint.getCreatedAt(), LocalDate.now())
                                : dateOnly(sprint.getStartDate());
                LocalDate end = sprint.getEndDate() == null
                                ? LocalDate.now()
                                : dateOnly(sprint.getEndDate());
                if (end.isBefore(start))
                        end = start;
                if (!historicalSprint && end.isAfter(LocalDate.now()))
                        end = LocalDate.now();

                Map<Long, Ticket> liveById = new LinkedHashMap<>();
                liveTickets.forEach(t -> liveById.put(t.getId(), t));
                List<SprintProgressPointResponse> points = new ArrayList<>();
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                        BigDecimal completed = BigDecimal.ZERO;
                        for (Map.Entry<Long, BigDecimal> entry : committedByTicket.entrySet()) {
                                Ticket ticket = liveById.get(entry.getKey());
                                boolean done = false;
                                if (ticket != null) {
                                        done = ticket.getStatus().getCategory() == TicketStatusCategory.DONE
                                                        && ticket.getResolvedAt() != null
                                                        && !dateOnly(ticket.getResolvedAt()).isAfter(date);
                                } else {
                                        SprintIssueSnapshot snapshot = snapshots.stream()
                                                        .filter(s -> Objects.equals(s.getTicketId(), entry.getKey()))
                                                        .findFirst().orElse(null);
                                        done = snapshot != null && snapshot
                                                        .getFinalStatusCategory() == TicketStatusCategory.DONE
                                                        && snapshot.getResolvedAt() != null
                                                        && !snapshot.getResolvedAt().toLocalDate().isAfter(date);
                                }
                                if (done)
                                        completed = completed.add(entry.getValue());
                        }
                        BigDecimal remaining = total.subtract(completed).max(BigDecimal.ZERO);
                        points.add(SprintProgressPointResponse.builder().date(date).scopeEstimate(total)
                                        .completedEstimate(completed).remainingEstimate(remaining).build());
                }
                return SprintBurndownResponse.builder().sprintId(sprintId).sprintName(sprint.getName())
                                .totalEstimate(total).points(points).build();
        }

        // =========================================================
        // CANCEL
        // =========================================================
        public SprintResponse cancel(Long id, Long userId) {
                Sprint sprint = getSprint(id);

                validateProjectAccess(sprint.getProject().getId(), userId);

                if (sprint.getStatus() == SprintStatus.COMPLETED) {
                        throw new RuntimeException("Completed sprint cannot be cancelled");
                }

                sprint.setStatus(SprintStatus.CANCELLED);
                return toResponse(sprintRepository.save(sprint));
        }

        // =========================================================
        // GET SPRINT TICKETS
        // =========================================================
        @Transactional(readOnly = true)
        public List<TicketResponse> getSprintTickets(Long sprintId) {
                getSprint(sprintId);
                return ticketRepository.findBySprintIdOrderByOrderAsc(sprintId).stream()
                                .filter(ticket -> ticket.getDeletedAt() == null)
                                .map(ticketService::toResponse)
                                .toList();
        }

        // =========================================================
        // GET PROJECT BACKLOG
        // =========================================================
        @Transactional(readOnly = true)
        public List<TicketResponse> getBacklog(Long projectId) {
                if (!projectRepository.existsById(projectId)) {
                        throw new RuntimeException("Project not found");
                }
                return ticketRepository.findByProjectIdAndSprintIsNullOrderByOrderAsc(projectId).stream()
                                .filter(ticket -> ticket.getDeletedAt() == null)
                                .map(ticketService::toResponse)
                                .toList();
        }

        // =========================================================
        // ADD TICKET TO SPRINT
        // =========================================================
        public TicketResponse addTicket(Long sprintId, Long ticketId, Long userId) {
                Sprint sprint = getSprint(sprintId);

                validateProjectAccess(sprint.getProject().getId(), userId);

                Ticket ticket = ticketRepository.findById(ticketId)
                                .orElseThrow(() -> new RuntimeException("Ticket not found"));

                if (!ticket.getProject().getId().equals(sprint.getProject().getId())) {
                        throw new RuntimeException("Ticket does not belong to sprint project");
                }

                if (sprint.getStatus() == SprintStatus.COMPLETED || sprint.getStatus() == SprintStatus.CANCELLED) {
                        throw new RuntimeException("Ticket cannot be added to completed/cancelled sprint");
                }

                // Backlog -> sprint is a real planning operation. Append the ticket
                // to the sprint instead of leaving an arbitrary/duplicate board order.
                List<Ticket> sprintTickets = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId);
                ticket.setSprint(sprint);
                ticket.setOrder(sprintTickets.size());

                // IMPORTANT: do not create a commitment snapshot here.
                // Snapshots represent the sprint-start baseline only. A ticket added
                // after an ACTIVE sprint has started is current scope / added scope.
                // If the sprint is PLANNED, start() will capture all tickets together.
                return ticketService.toResponse(ticketRepository.save(ticket));
        }

        // =========================================================
        // REMOVE TICKET FROM SPRINT
        // =========================================================
        public TicketResponse removeTicket(Long sprintId, Long ticketId, Long userId) {
                Sprint sprint = getSprint(sprintId);

                validateProjectAccess(sprint.getProject().getId(), userId);

                Ticket ticket = ticketRepository.findById(ticketId)
                                .orElseThrow(() -> new RuntimeException("Ticket not found"));

                if (ticket.getSprint() == null || !ticket.getSprint().getId().equals(sprintId)) {
                        throw new RuntimeException("Ticket is not assigned to this sprint");
                }

                ticket.setSprint(null);
                return ticketService.toResponse(ticketRepository.save(ticket));
        }

        // =========================================================
        // MOVE TICKET TO BACKLOG
        // =========================================================
        public TicketResponse moveToBacklog(Long ticketId, Long userId) {
                Ticket ticket = ticketRepository.findById(ticketId)
                                .orElseThrow(() -> new RuntimeException("Ticket not found"));

                validateProjectAccess(ticket.getProject().getId(), userId);

                ticket.setSprint(null);
                return ticketService.toResponse(ticketRepository.save(ticket));
        }

        // =========================================================
        // SPRINT STATISTICS
        // =========================================================
        @Transactional(readOnly = true)
        public SprintStatisticsResponse statistics(Long sprintId) {
                Sprint sprint = getSprint(sprintId);
                projectAccessService.requireView(sprint.getProject());

                List<Ticket> tickets = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId)
                                .stream()
                                .filter(ticket -> ticket.getDeletedAt() == null)
                                .toList();

                long assignedTickets = tickets.stream()
                                .filter(ticket -> ticket.getResponsible() != null)
                                .count();

                long unassignedTickets = tickets.stream()
                                .filter(ticket -> ticket.getResponsible() == null)
                                .count();

                BigDecimal totalEstimation = tickets.stream()
                                .map(Ticket::getEstimation)
                                .map(this::nz)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal completedEstimation = tickets.stream()
                                .filter(ticket -> ticket.getStatus() != null
                                                && ticket.getStatus().getCategory() == TicketStatusCategory.DONE)
                                .map(Ticket::getEstimation)
                                .map(this::nz)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal remainingEstimation = totalEstimation
                                .subtract(completedEstimation)
                                .max(BigDecimal.ZERO);

                BigDecimal completionPercent = totalEstimation.signum() == 0
                                ? BigDecimal.ZERO
                                : completedEstimation
                                                .multiply(BigDecimal.valueOf(100))
                                                .divide(totalEstimation, 2,
                                                                java.math.RoundingMode.HALF_UP);

                return SprintStatisticsResponse.builder()
                                .sprintId(sprint.getId())
                                .sprintName(sprint.getName())
                                .totalTickets((long) tickets.size())
                                .assignedTickets(assignedTickets)
                                .backlogTickets(unassignedTickets)
                                .totalEstimation(totalEstimation)
                                .completedEstimation(completedEstimation)
                                .remainingEstimation(remainingEstimation)
                                .completionPercent(completionPercent)
                                .build();
        }

        // =========================================================
        // SPRINT HISTORY
        // =========================================================
        @Transactional(readOnly = true)
        public List<SprintHistoryResponse> history(Long projectId) {
                Project project = getProject(projectId);
                projectAccessService.requireView(project);
                return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                                .filter(s -> s.getStatus() == SprintStatus.COMPLETED
                                                || s.getStatus() == SprintStatus.CANCELLED)
                                .map(this::historyRow).toList();
        }

        // =========================================================
        // SPRINT VELOCITY
        // =========================================================
        @Transactional(readOnly = true)
        public SprintVelocityResponse velocity(Long projectId) {
                Project project = getProject(projectId);
                projectAccessService.requireView(project);
                List<SprintVelocityResponse.SprintVelocityPoint> rows = sprintRepository
                                .findByProjectIdAndStatus(projectId, SprintStatus.COMPLETED).stream()
                                .sorted(Comparator.comparing(Sprint::getStartDate,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(s -> {
                                        List<SprintIssueSnapshot> snapshots = snapshotRepository
                                                        .findBySprintIdOrderByTicketIdAsc(s.getId());
                                        BigDecimal committed = snapshots.stream().map(x -> nz(x.getEstimation()))
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal completed = snapshots.stream().filter(
                                                        x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                                        .map(x -> nz(x.getEstimation()))
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        long completedTickets = snapshots.stream().filter(
                                                        x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                                        .count();
                                        return SprintVelocityResponse.SprintVelocityPoint.builder().sprintId(s.getId())
                                                        .sprintName(s.getName())
                                                        .committedEstimate(committed).completedEstimate(completed)
                                                        .committedTickets((long) snapshots.size())
                                                        .completedTickets(completedTickets).build();
                                }).toList();
                BigDecimal total = rows.stream().map(SprintVelocityResponse.SprintVelocityPoint::getCompletedEstimate)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = rows.isEmpty() ? BigDecimal.ZERO
                                : total.divide(BigDecimal.valueOf(rows.size()), 2, java.math.RoundingMode.HALF_UP);
                return SprintVelocityResponse.builder().projectId(projectId).averageVelocity(avg)
                                .totalCompletedEstimate(total).sprints(rows).build();
        }

        // =========================================================
        // SPRINT REPORT
        // =========================================================
        @Transactional(readOnly = true)
        public SprintReportResponse report(Long sprintId) {
                Sprint sprint = getSprint(sprintId);
                projectAccessService.requireView(sprint.getProject());

                List<Ticket> current = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId).stream()
                                .filter(t -> t.getDeletedAt() == null)
                                .toList();
                List<SprintIssueSnapshot> snapshots = snapshotRepository
                                .findBySprintIdOrderByTicketIdAsc(sprintId);

                boolean historicalSprint = sprint.getStatus() == SprintStatus.COMPLETED
                                || sprint.getStatus() == SprintStatus.CANCELLED;

                if (!historicalSprint) {
                        BigDecimal currentEstimate = current.stream().map(t -> nz(t.getEstimation()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal completed = current.stream()
                                        .filter(t -> t.getStatus() != null
                                                        && t.getStatus().getCategory() == TicketStatusCategory.DONE)
                                        .map(t -> nz(t.getEstimation()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal completion = currentEstimate.signum() == 0 ? BigDecimal.ZERO
                                        : completed.multiply(BigDecimal.valueOf(100)).divide(currentEstimate, 2,
                                                        java.math.RoundingMode.HALF_UP);
                        List<SprintReportResponse.SprintReportIssue> issues = current
                                        .stream().map(ticket -> SprintReportResponse.SprintReportIssue.builder()
                                                        .ticketId(ticket.getId()).code(ticket.getCode())
                                                        .name(ticket.getName()).estimation(nz(ticket.getEstimation()))
                                                        .status(ticket.getStatus() == null ? null
                                                                        : ticket.getStatus().getName())
                                                        .statusCategory(ticket.getStatus() == null ? null
                                                                        : ticket.getStatus().getCategory().name())
                                                        .completed(ticket.getStatus() != null
                                                                        && ticket.getStatus()
                                                                                        .getCategory() == TicketStatusCategory.DONE)
                                                        .build())
                                        .toList();
                        return SprintReportResponse.builder().sprintId(sprintId).sprintName(sprint.getName())
                                        .status(sprint.getStatus()).goal(sprint.getGoal())
                                        .committedTickets((long) current.size())
                                        .completedTickets(current.stream().filter(t -> t.getStatus() != null
                                                        && t.getStatus().getCategory() == TicketStatusCategory.DONE)
                                                        .count())
                                        .incompleteTickets(current.stream().filter(t -> t.getStatus() == null
                                                        || (t.getStatus().getCategory() != TicketStatusCategory.DONE
                                                                        && t.getStatus().getCategory() != TicketStatusCategory.CANCELLED))
                                                        .count())
                                        .committedEstimate(currentEstimate).completedEstimate(completed)
                                        .remainingEstimate(currentEstimate.subtract(completed).max(BigDecimal.ZERO))
                                        .completionPercent(completion).scopeChangeEstimate(BigDecimal.ZERO)
                                        .commitmentCompletionPercent(completion).issues(issues).build();
                }

                Map<Long, Ticket> currentById = new LinkedHashMap<>();
                current.forEach(t -> currentById.put(t.getId(), t));
                BigDecimal committed = snapshots.stream().map(x -> nz(x.getEstimation()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal completed = snapshots.stream()
                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                .map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO, BigDecimal::add);
                List<SprintReportResponse.SprintReportIssue> issues = snapshots.stream().map(snapshot -> {
                        Ticket ticket = currentById.get(snapshot.getTicketId());
                        String status = ticket != null && ticket.getStatus() != null ? ticket.getStatus().getName()
                                        : snapshot.getFinalStatusCategory().name();
                        boolean done = snapshot.getFinalStatusCategory() == TicketStatusCategory.DONE;
                        return SprintReportResponse.SprintReportIssue.builder().ticketId(snapshot.getTicketId())
                                        .code(ticket == null ? null : ticket.getCode())
                                        .name(ticket == null ? null : ticket.getName())
                                        .estimation(nz(snapshot.getEstimation())).status(status)
                                        .statusCategory(snapshot.getFinalStatusCategory().name()).completed(done)
                                        .build();
                }).toList();
                BigDecimal completion = committed.signum() == 0 ? BigDecimal.ZERO
                                : completed.multiply(BigDecimal.valueOf(100)).divide(committed, 2,
                                                java.math.RoundingMode.HALF_UP);
                BigDecimal currentEstimate = current.stream().map(t -> nz(t.getEstimation()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                return SprintReportResponse.builder().sprintId(sprintId).sprintName(sprint.getName())
                                .status(sprint.getStatus()).goal(sprint.getGoal())
                                .committedTickets((long) snapshots.size())
                                .completedTickets(snapshots.stream()
                                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                                .count())
                                .incompleteTickets(snapshots.stream().filter(x -> x
                                                .getFinalStatusCategory() != TicketStatusCategory.DONE
                                                && x.getFinalStatusCategory() != TicketStatusCategory.CANCELLED)
                                                .count())
                                .committedEstimate(committed).completedEstimate(completed)
                                .remainingEstimate(committed.subtract(completed).max(BigDecimal.ZERO))
                                .completionPercent(completion)
                                .scopeChangeEstimate(currentEstimate.subtract(committed))
                                .commitmentCompletionPercent(completion)
                                .issues(issues).build();
        }

        // =========================================================
        // COMMITMENT VS COMPLETION
        // =========================================================
        @Transactional(readOnly = true)
        public SprintCommitmentResponse commitment(Long sprintId) {
                Sprint sprint = getSprint(sprintId);
                projectAccessService.requireView(sprint.getProject());
                List<SprintIssueSnapshot> snapshots = snapshotRepository.findBySprintIdOrderByTicketIdAsc(sprintId);
                List<Ticket> current = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId).stream()
                                .filter(t -> t.getDeletedAt() == null).toList();
                boolean historicalSprint = sprint.getStatus() == SprintStatus.COMPLETED
                                || sprint.getStatus() == SprintStatus.CANCELLED;

                BigDecimal currentEstimate = current.stream().map(t -> nz(t.getEstimation()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal committed = snapshots.stream().map(x -> nz(x.getEstimation()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (snapshots.isEmpty() && !historicalSprint)
                        committed = currentEstimate;

                BigDecimal completed = historicalSprint
                                ? snapshots.stream()
                                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                                .map(x -> nz(x.getEstimation()))
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                : current.stream().filter(t -> t.getStatus() != null
                                                && t.getStatus().getCategory() == TicketStatusCategory.DONE)
                                                .map(t -> nz(t.getEstimation()))
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal pct = committed.signum() == 0 ? BigDecimal.ZERO
                                : completed.multiply(BigDecimal.valueOf(100)).divide(committed, 2,
                                                java.math.RoundingMode.HALF_UP);
                return SprintCommitmentResponse.builder().sprintId(sprintId).sprintName(sprint.getName())
                                .committedEstimate(committed).completedEstimate(completed)
                                .remainingCommittedEstimate(committed.subtract(completed).max(BigDecimal.ZERO))
                                .scopeChangeEstimate(currentEstimate.subtract(committed))
                                .committedTickets(historicalSprint ? (long) snapshots.size()
                                                : (snapshots.isEmpty() ? (long) current.size()
                                                                : (long) snapshots.size()))
                                .completedTickets(historicalSprint
                                                ? snapshots.stream().filter(x -> x
                                                                .getFinalStatusCategory() == TicketStatusCategory.DONE)
                                                                .count()
                                                : current.stream().filter(t -> t.getStatus() != null
                                                                && t.getStatus().getCategory() == TicketStatusCategory.DONE)
                                                                .count())
                                .currentTickets((long) current.size()).completionPercent(pct).build();
        }

        // =========================================================
        // CAPACITY PLANNING
        // =========================================================
        @Transactional(readOnly = true)
        public List<SprintCapacityResponse> getCapacity(Long sprintId) {
                Sprint sprint = getSprint(sprintId);
                Project project = sprint.getProject();
                projectAccessService.requireView(project);

                // Sprint start/end are LocalDate values; keep the analytics
                // contract strictly date-only (never introduce a timestamp here).
                LocalDate start = dateOnly(sprint.getStartDate());
                LocalDate end = dateOnly(sprint.getEndDate());
                if (start == null || end == null || end.isBefore(start)) {
                        return List.of();
                }

                Map<Long, SprintCapacity> configured = new LinkedHashMap<>();
                capacityRepository.findBySprintIdOrderByUser_NameAsc(sprintId)
                                .forEach(c -> configured.put(c.getUser().getId(), c));

                List<Ticket> tickets = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId);
                Map<Long, BigDecimal> assigned = new LinkedHashMap<>();
                tickets.forEach(t -> {
                        if (t.getResponsible() != null) {
                                assigned.merge(
                                                t.getResponsible().getId(),
                                                nz(t.getEstimation()),
                                                BigDecimal::add);
                        }
                });

                List<MemberAvailability> availability = availabilityRepository
                                .findByProjectIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
                                                project.getId(), start, end);

                Map<Long, Map<LocalDate, MemberAvailability>> memberAvailability = new LinkedHashMap<>();
                Map<LocalDate, MemberAvailability> projectHolidays = new LinkedHashMap<>();

                for (MemberAvailability entry : availability) {
                        LocalDate availabilityDate = dateOnly(entry.getAvailabilityDate());
                        if (availabilityDate == null) {
                                continue;
                        }

                        if (entry.getUser() == null) {
                                projectHolidays.put(availabilityDate, entry);
                        } else {
                                memberAvailability
                                                .computeIfAbsent(entry.getUser().getId(), k -> new LinkedHashMap<>())
                                                .put(availabilityDate, entry);
                        }
                }

                List<ProjectWorkingHours> workingHourHistory = workingHoursRepository
                                .findByProjectIdOrderByEffectiveFromAsc(project.getId());

                return projectUserRepository.findByProjectId(project.getId())
                                .stream()
                                .sorted(Comparator.comparing(
                                                (ProjectUser pu) -> pu.getUser() == null ? ""
                                                                : Objects.toString(pu.getUser().getName(), ""),
                                                String.CASE_INSENSITIVE_ORDER))
                                .map((ProjectUser pu) -> {
                                        Long memberId = pu.getUser().getId();
                                        CapacityCalculation calculation = calculateMemberCapacity(
                                                        pu,
                                                        start,
                                                        end,
                                                        projectHolidays,
                                                        memberAvailability.getOrDefault(memberId, Map.of()),
                                                        workingHourHistory);
                                        BigDecimal hours = calculation.getCapacityHours();

                                        SprintCapacity c = configured.get(memberId);
                                        BigDecimal points = c == null
                                                        ? BigDecimal.ZERO
                                                        : nz(c.getCapacityPoints());
                                        BigDecimal estimate = assigned.getOrDefault(
                                                        memberId, BigDecimal.ZERO);
                                        BigDecimal utilization = points.signum() == 0
                                                        ? null
                                                        : estimate.multiply(BigDecimal.valueOf(100))
                                                                        .divide(points, 2,
                                                                                        java.math.RoundingMode.HALF_UP);
                                        BigDecimal remainingPoints = points.subtract(estimate);

                                        String capacityStatus;
                                        if (points.signum() <= 0) {
                                                capacityStatus = "NOT_CONFIGURED";
                                        } else if (remainingPoints.signum() < 0) {
                                                capacityStatus = "OVERALLOCATED";
                                        } else if (utilization != null
                                                        && utilization.compareTo(BigDecimal.valueOf(80)) >= 0) {
                                                capacityStatus = "HIGH";
                                        } else {
                                                capacityStatus = "HEALTHY";
                                        }

                                        return SprintCapacityResponse.builder()
                                                        .id(c == null ? null : c.getId())
                                                        .sprintId(sprintId)
                                                        .userId(memberId)
                                                        .userName(pu.getUser().getName())
                                                        .userEmail(pu.getUser().getEmail())
                                                        .capacityPoints(points)
                                                        .capacityHours(hours)
                                                        .baseCapacityHours(calculation.getBaseCapacityHours())
                                                        .reducedCapacityHours(calculation.getReducedCapacityHours())
                                                        .workingDays(calculation.getWorkingDays())
                                                        .holidayDays(calculation.getHolidayDays())
                                                        .halfDayDays(calculation.getHalfDayDays())
                                                        .unavailableDays(calculation.getUnavailableDays())
                                                        .weekendDays(calculation.getWeekendDays())
                                                        .assignedEstimate(estimate)
                                                        .remainingCapacityPoints(remainingPoints)
                                                        .utilizationPercent(utilization)
                                                        .capacityStatus(capacityStatus)
                                                        .build();
                                })
                                .toList();
        }

        /**
         * Saves only the manually planned capacity points.
         * Capacity hours are always calculated from the project's working-hours
         * history and member availability; the client cannot override them.
         */
        public SprintCapacityResponse saveCapacity(
                        Long sprintId, SprintCapacityRequest request, Long userId) {

                Sprint sprint = getSprint(sprintId);
                validateProjectAccess(sprint.getProject().getId(), userId);

                if (request == null || request.getUserId() == null) {
                        throw new IllegalArgumentException("User is required");
                }

                if (!projectUserRepository.existsByProjectIdAndUserId(
                                sprint.getProject().getId(), request.getUserId())) {
                        throw new RuntimeException("User is not a member of this project");
                }

                SprintCapacity c = capacityRepository
                                .findBySprintIdAndUserId(sprintId, request.getUserId())
                                .orElseGet(SprintCapacity::new);

                c.setSprint(sprint);
                c.setUser(getUser(request.getUserId()));
                c.setCapacityPoints(nz(request.getCapacityPoints()).max(BigDecimal.ZERO));

                // Never persist client supplied capacity hours.
                c.setCapacityHours(BigDecimal.ZERO);
                capacityRepository.save(c);

                return getCapacity(sprintId).stream()
                                .filter(x -> x.getUserId().equals(request.getUserId()))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Capacity could not be saved"));
        }

        private CapacityCalculation calculateMemberCapacity(
                        ProjectUser projectUser,
                        LocalDate start,
                        LocalDate end,
                        Map<LocalDate, MemberAvailability> projectHolidays,
                        Map<LocalDate, MemberAvailability> memberEntries,
                        List<ProjectWorkingHours> workingHourHistory) {

                // Capacity planning is date-only. The project membership
                // createdAt is a timestamp, so explicitly discard its time part.
                LocalDate joinDate = dateOnly(projectUser.getCreatedAt(), start);

                BigDecimal baseCapacityHours = BigDecimal.ZERO;
                BigDecimal capacityHours = BigDecimal.ZERO;
                int holidayDays = 0;
                int halfDayDays = 0;
                int unavailableDays = 0;
                int weekendDays = 0;
                int workingDays = 0;

                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                        if (date.isBefore(joinDate)) {
                                continue;
                        }

                        // Saturday/Sunday are non-working days and do not contribute
                        // to either working days or available capacity.
                        if (date.getDayOfWeek().getValue() >= 6) {
                                weekendDays++;
                                continue;
                        }

                        BigDecimal dayHours = effectiveWorkingHours(workingHourHistory, date);
                        baseCapacityHours = baseCapacityHours.add(dayHours);

                        /*
                         * IMPORTANT:
                         * Availability entries apply to the sprint date regardless of
                         * when the availability record was created. A holiday on
                         * 2026-09-14 must reduce a sprint that contains 2026-09-14,
                         * even if the holiday record was created after the sprint began.
                         */

                        // Project-wide holiday.
                        MemberAvailability projectEntry = projectHolidays.get(date);
                        if (isLeave(projectEntry)) {
                                holidayDays++;
                                continue;
                        }

                        // Member-specific holiday/unavailable day.
                        MemberAvailability entry = memberEntries.get(date);
                        if (isLeave(entry)) {
                                if (entry.getAvailabilityType() == MemberAvailabilityType.HOLIDAY) {
                                        holidayDays++;
                                } else {
                                        unavailableDays++;
                                }
                                continue;
                        }

                        // This is an effective working day because it is not a
                        // weekend, holiday, or unavailable day.
                        workingDays++;

                        // HALF_DAY remains a working day but contributes half the hours.
                        if (entry != null && entry.getAvailabilityType() == MemberAvailabilityType.HALF_DAY) {
                                halfDayDays++;
                                capacityHours = capacityHours.add(
                                                dayHours.divide(
                                                                BigDecimal.valueOf(2),
                                                                2,
                                                                java.math.RoundingMode.HALF_UP));
                        } else if (entry != null
                                        && entry.getAvailabilityType() == MemberAvailabilityType.AVAILABLE
                                        && entry.getAvailableHours() != null) {
                                capacityHours = capacityHours.add(
                                                entry.getAvailableHours()
                                                                .max(BigDecimal.ZERO)
                                                                .min(dayHours));
                        } else {
                                capacityHours = capacityHours.add(dayHours);
                        }
                }

                BigDecimal reducedCapacityHours = baseCapacityHours
                                .subtract(capacityHours)
                                .max(BigDecimal.ZERO)
                                .setScale(2, java.math.RoundingMode.HALF_UP);

                return new CapacityCalculation(
                                capacityHours.setScale(2, java.math.RoundingMode.HALF_UP),
                                baseCapacityHours.setScale(2, java.math.RoundingMode.HALF_UP),
                                reducedCapacityHours,
                                workingDays,
                                holidayDays,
                                halfDayDays,
                                unavailableDays,
                                weekendDays);
        }

        /**
         * Returns true when the availability entry removes the member's full
         * working capacity for that calendar date.
         *
         * The record creation date is deliberately NOT considered here.
         * Capacity planning is date-based: if a holiday/unavailable entry exists
         * for a date inside the sprint, that date must reduce capacity.
         */
        private boolean isLeave(MemberAvailability entry) {
                if (entry == null || entry.getAvailabilityType() == null) {
                        return false;
                }

                return entry.getAvailabilityType() == MemberAvailabilityType.HOLIDAY
                                || entry.getAvailabilityType() == MemberAvailabilityType.UNAVAILABLE;
        }

        private static class CapacityCalculation {
                private final BigDecimal capacityHours;
                private final BigDecimal baseCapacityHours;
                private final BigDecimal reducedCapacityHours;
                private final int workingDays;
                private final int holidayDays;
                private final int halfDayDays;
                private final int unavailableDays;
                private final int weekendDays;

                CapacityCalculation(
                                BigDecimal capacityHours,
                                BigDecimal baseCapacityHours,
                                BigDecimal reducedCapacityHours,
                                int workingDays,
                                int holidayDays,
                                int halfDayDays,
                                int unavailableDays,
                                int weekendDays) {
                        this.capacityHours = capacityHours;
                        this.baseCapacityHours = baseCapacityHours;
                        this.reducedCapacityHours = reducedCapacityHours;
                        this.workingDays = workingDays;
                        this.holidayDays = holidayDays;
                        this.halfDayDays = halfDayDays;
                        this.unavailableDays = unavailableDays;
                        this.weekendDays = weekendDays;
                }

                public BigDecimal getCapacityHours() {
                        return capacityHours;
                }

                public BigDecimal getBaseCapacityHours() {
                        return baseCapacityHours;
                }

                public BigDecimal getReducedCapacityHours() {
                        return reducedCapacityHours;
                }

                public int getWorkingDays() {
                        return workingDays;
                }

                public int getHolidayDays() {
                        return holidayDays;
                }

                public int getHalfDayDays() {
                        return halfDayDays;
                }

                public int getUnavailableDays() {
                        return unavailableDays;
                }

                public int getWeekendDays() {
                        return weekendDays;
                }
        }

        /**
         * Converts a timestamp to its calendar date.
         *
         * IMPORTANT:
         * Time-of-day is intentionally ignored for sprint/capacity calculations.
         * For example:
         * 2026-09-14T00:00:00 -> 2026-09-14
         * 2026-09-14T23:59:59 -> 2026-09-14
         */
        private LocalDate dateOnly(LocalDateTime value, LocalDate fallback) {
                return value == null ? fallback : value.toLocalDate();
        }

        private LocalDate dateOnly(LocalDateTime value) {
                return value == null ? null : value.toLocalDate();
        }

        private LocalDate dateOnly(LocalDate value) {
                return value;
        }

        private BigDecimal effectiveWorkingHours(
                        List<ProjectWorkingHours> history, LocalDate date) {

                for (int i = history.size() - 1; i >= 0; i--) {
                        ProjectWorkingHours record = history.get(i);
                        if (record.getEffectiveFrom() != null
                                        && !record.getEffectiveFrom().isAfter(date)) {
                                return nz(record.getWorkingHours()).max(BigDecimal.ZERO);
                        }
                }

                return BigDecimal.valueOf(8);
        }

        private SprintHistoryResponse historyRow(Sprint sprint) {
                List<SprintIssueSnapshot> snapshots = snapshotRepository
                                .findBySprintIdOrderByTicketIdAsc(sprint.getId());
                BigDecimal committed = snapshots.stream().map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO,
                                BigDecimal::add);
                BigDecimal completed = snapshots.stream()
                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                .map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal pct = committed.signum() == 0 ? BigDecimal.ZERO
                                : completed.multiply(BigDecimal.valueOf(100)).divide(committed, 2,
                                                java.math.RoundingMode.HALF_UP);
                return SprintHistoryResponse.builder().sprintId(sprint.getId()).sprintName(sprint.getName())
                                .status(sprint.getStatus())
                                .startDate(sprint.getStartDate()).endDate(sprint.getEndDate())
                                .committedTickets((long) snapshots.size())
                                .completedTickets(snapshots.stream()
                                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                                .count())
                                .committedEstimate(committed).completedEstimate(completed).completionPercent(pct)
                                .build();
        }

        private BigDecimal nz(BigDecimal value) {
                return value == null ? BigDecimal.ZERO : value;
        }

        // =========================================================
        // HELPERS
        // =========================================================
        private Sprint getSprint(Long id) {
                return sprintRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Sprint not found"));
        }

        private Project getProject(Long id) {
                return projectRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Project not found"));
        }

        private User getUser(Long id) {
                return userRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("User not found"));
        }

        private void validateProjectAccess(Long projectId, Long userId) {
                if (userId == null) {
                        throw new RuntimeException("Authenticated user not found");
                }
                projectAccessService.requireEditor(getProject(projectId));
        }

        private void validateNoActiveSprint(Long projectId) {
                boolean active = sprintRepository.existsByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
                if (active) {
                        throw new RuntimeException("Another active sprint already exists for this project");
                }
        }

        private void validateSprintName(Long projectId, String name, Long currentSprintId) {
                boolean exists;
                if (currentSprintId == null) {
                        exists = sprintRepository.existsByProjectIdAndName(projectId, name.trim());
                } else {
                        exists = sprintRepository.existsByProjectIdAndNameAndIdNot(projectId, name.trim(),
                                        currentSprintId);
                }
                if (exists) {
                        throw new RuntimeException("Sprint with this name already exists in this project");
                }
        }

        private void validateDates(LocalDate startDate, LocalDate endDate) {
                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                        throw new RuntimeException("End date cannot be before start date");
                }
        }

        private SprintResponse toResponse(Sprint sprint) {
                Long ticketCount = ticketRepository.countBySprintId(sprint.getId());

                BigDecimal totalEstimation = ticketRepository
                                .findBySprintIdOrderByOrderAsc(sprint.getId())
                                .stream()
                                .map(Ticket::getEstimation)
                                .filter(value -> value != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return SprintResponse.builder()
                                .id(sprint.getId())
                                .name(sprint.getName())
                                .goal(sprint.getGoal())
                                .projectId(sprint.getProject().getId())
                                .projectName(sprint.getProject().getName())
                                .startDate(sprint.getStartDate())
                                .endDate(sprint.getEndDate())
                                .status(sprint.getStatus())
                                .createdBy(sprint.getCreatedBy() == null ? null : sprint.getCreatedBy().getId())
                                .createdAt(sprint.getCreatedAt())
                                .updatedAt(sprint.getUpdatedAt())
                                .ticketCount(ticketCount)
                                .totalEstimation(totalEstimation)
                                .build();
        }
}
