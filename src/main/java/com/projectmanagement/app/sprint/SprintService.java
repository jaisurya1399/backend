package com.projectmanagement.app.sprint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
import com.projectmanagement.app.project.ProjectUserRepository;
import com.projectmanagement.app.project.ProjectWorkingHoursResponse;
import com.projectmanagement.app.project.ProjectWorkingHoursService;
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
        private final SprintIssueSnapshotRepository snapshotRepository;
        private final SprintCapacityRepository capacityRepository;
        private final MemberAvailabilityRepository memberAvailabilityRepository;
        private final ProjectWorkingHoursService projectWorkingHoursService;

        public SprintService(
                        SprintRepository sprintRepository,
                        ProjectRepository projectRepository,
                        ProjectUserRepository projectUserRepository,
                        TicketRepository ticketRepository,
                        UserRepository userRepository,
                        ProjectAccessService projectAccessService,
                        SprintIssueSnapshotRepository snapshotRepository,
                        SprintCapacityRepository capacityRepository,
                        MemberAvailabilityRepository memberAvailabilityRepository,
                        ProjectWorkingHoursService projectWorkingHoursService) {
                this.sprintRepository = sprintRepository;
                this.projectRepository = projectRepository;
                this.projectUserRepository = projectUserRepository;
                this.ticketRepository = ticketRepository;
                this.userRepository = userRepository;
                this.projectAccessService = projectAccessService;
                this.snapshotRepository = snapshotRepository;
                this.capacityRepository = capacityRepository;
                this.memberAvailabilityRepository = memberAvailabilityRepository;
                this.projectWorkingHoursService = projectWorkingHoursService;
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
                                snapshot = SprintIssueSnapshot.builder().sprint(sprint).ticketId(ticket.getId())
                                                .build();
                        }
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
                if (snapshots.isEmpty()) {
                        liveTickets.forEach(t -> committedByTicket.put(t.getId(), nz(t.getEstimation())));
                }

                BigDecimal total = committedByTicket.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                LocalDate start = sprint.getStartDate() == null
                                ? (sprint.getCreatedAt() == null ? LocalDate.now()
                                                : sprint.getCreatedAt().toLocalDate())
                                : sprint.getStartDate();
                LocalDate end = sprint.getEndDate() == null ? LocalDate.now() : sprint.getEndDate();
                if (end.isBefore(start))
                        end = start;

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
                                                        && !ticket.getResolvedAt().toLocalDate().isAfter(date);
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
        public List<Ticket> getSprintTickets(Long sprintId) {
                getSprint(sprintId);
                return ticketRepository.findBySprintIdOrderByOrderAsc(sprintId);
        }

        // =========================================================
        // GET PROJECT BACKLOG
        // =========================================================
        @Transactional(readOnly = true)
        public List<Ticket> getBacklog(Long projectId) {
                if (!projectRepository.existsById(projectId)) {
                        throw new RuntimeException("Project not found");
                }
                return ticketRepository.findByProjectIdAndSprintIsNullOrderByOrderAsc(projectId);
        }

        // =========================================================
        // ADD TICKET TO SPRINT
        // =========================================================
        public Ticket addTicket(Long sprintId, Long ticketId, Long userId) {
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

                ticket.setSprint(sprint);
                return ticketRepository.save(ticket);
        }

        // =========================================================
        // REMOVE TICKET FROM SPRINT
        // =========================================================
        public Ticket removeTicket(Long sprintId, Long ticketId, Long userId) {
                Sprint sprint = getSprint(sprintId);

                validateProjectAccess(sprint.getProject().getId(), userId);

                Ticket ticket = ticketRepository.findById(ticketId)
                                .orElseThrow(() -> new RuntimeException("Ticket not found"));

                if (ticket.getSprint() == null || !ticket.getSprint().getId().equals(sprintId)) {
                        throw new RuntimeException("Ticket is not assigned to this sprint");
                }

                ticket.setSprint(null);
                return ticketRepository.save(ticket);
        }

        // =========================================================
        // MOVE TICKET TO BACKLOG
        // =========================================================
        public Ticket moveToBacklog(Long ticketId, Long userId) {
                Ticket ticket = ticketRepository.findById(ticketId)
                                .orElseThrow(() -> new RuntimeException("Ticket not found"));

                validateProjectAccess(ticket.getProject().getId(), userId);

                ticket.setSprint(null);
                return ticketRepository.save(ticket);
        }

        // =========================================================
        // SPRINT STATISTICS
        // =========================================================
        @Transactional(readOnly = true)
        public SprintStatisticsResponse statistics(Long sprintId) {
                Sprint sprint = getSprint(sprintId);

                List<Ticket> tickets = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId);

                BigDecimal totalEstimation = tickets.stream()
                                .map(Ticket::getEstimation)
                                .filter(value -> value != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

                BigDecimal remainingEstimation = totalEstimation.subtract(completedEstimation);

                return SprintStatisticsResponse.builder()
                                .sprintId(sprint.getId())
                                .sprintName(sprint.getName())
                                .totalTickets((long) tickets.size())
                                .assignedTickets((long) tickets.size())
                                .backlogTickets(0L)
                                .totalEstimation(totalEstimation)
                                .completedEstimation(completedEstimation)
                                .remainingEstimation(remainingEstimation)
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
                List<SprintIssueSnapshot> snapshots = snapshotRepository.findBySprintIdOrderByTicketIdAsc(sprintId);
                List<Ticket> current = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId);
                Map<Long, Ticket> currentById = new LinkedHashMap<>();
                current.forEach(t -> currentById.put(t.getId(), t));
                BigDecimal committed = snapshots.stream().map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO,
                                BigDecimal::add);
                BigDecimal completed = snapshots.stream()
                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                .map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal currentEstimate = current.stream().map(t -> nz(t.getEstimation())).reduce(BigDecimal.ZERO,
                                BigDecimal::add);
                BigDecimal scopeChange = currentEstimate.subtract(committed);
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
                                .completionPercent(completion).scopeChangeEstimate(scopeChange)
                                .commitmentCompletionPercent(completion).issues(issues).build();
        }

        // =========================================================
        // COMMITMENT VS COMPLETION
        // =========================================================
        @Transactional(readOnly = true)
        public SprintCommitmentResponse commitment(Long sprintId) {
                Sprint sprint = getSprint(sprintId);
                projectAccessService.requireView(sprint.getProject());
                List<SprintIssueSnapshot> snapshots = snapshotRepository.findBySprintIdOrderByTicketIdAsc(sprintId);
                List<Ticket> current = ticketRepository.findBySprintIdOrderByOrderAsc(sprintId);
                BigDecimal committed = snapshots.stream().map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO,
                                BigDecimal::add);
                BigDecimal completed = snapshots.stream()
                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                                .map(x -> nz(x.getEstimation())).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal currentEstimate = current.stream().map(t -> nz(t.getEstimation())).reduce(BigDecimal.ZERO,
                                BigDecimal::add);
                BigDecimal pct = committed.signum() == 0 ? BigDecimal.ZERO
                                : completed.multiply(BigDecimal.valueOf(100)).divide(committed, 2,
                                                java.math.RoundingMode.HALF_UP);
                return SprintCommitmentResponse.builder().sprintId(sprintId).sprintName(sprint.getName())
                                .committedEstimate(committed)
                                .completedEstimate(completed)
                                .remainingCommittedEstimate(committed.subtract(completed).max(BigDecimal.ZERO))
                                .scopeChangeEstimate(currentEstimate.subtract(committed))
                                .committedTickets((long) snapshots.size())
                                .completedTickets(snapshots.stream()
                                                .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
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

                LocalDate sprintStart = sprint.getStartDate();
                LocalDate sprintEnd = sprint.getEndDate();

                List<ProjectWorkingHoursResponse> workingHourHistory = sprintStart != null && sprintEnd != null
                                ? projectWorkingHoursService.getHistory(project.getId())
                                : List.of();

                List<MemberAvailability> availabilityEntries = sprintStart != null && sprintEnd != null
                                ? memberAvailabilityRepository
                                                .findByProjectIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
                                                                project.getId(), sprintStart, sprintEnd)
                                : List.of();

                Map<LocalDate, MemberAvailability> projectHolidays = new LinkedHashMap<>();
                Map<Long, Map<LocalDate, MemberAvailability>> memberAvailability = new LinkedHashMap<>();

                for (MemberAvailability entry : availabilityEntries) {
                        if (entry.getUser() == null
                                        && entry.getAvailabilityType() == MemberAvailabilityType.HOLIDAY) {
                                projectHolidays.put(entry.getAvailabilityDate(), entry);
                        } else if (entry.getUser() != null) {
                                memberAvailability
                                                .computeIfAbsent(entry.getUser().getId(), k -> new LinkedHashMap<>())
                                                .put(entry.getAvailabilityDate(), entry);
                        }
                }

                return projectUserRepository.findByProjectId(project.getId())
                                .stream()
                                .map(pu -> {
                                        SprintCapacity configuredCapacity = configured.get(pu.getUser().getId());
                                        BigDecimal points = configuredCapacity == null
                                                        ? BigDecimal.ZERO
                                                        : nz(configuredCapacity.getCapacityPoints());

                                        CapacityBreakdown breakdown = calculateCapacity(
                                                        pu, sprintStart, sprintEnd, workingHourHistory,
                                                        projectHolidays, memberAvailability);

                                        BigDecimal estimate = assigned.getOrDefault(
                                                        pu.getUser().getId(), BigDecimal.ZERO);

                                        BigDecimal utilization = points.signum() == 0
                                                        ? BigDecimal.ZERO
                                                        : estimate.multiply(BigDecimal.valueOf(100))
                                                                        .divide(points, 2, RoundingMode.HALF_UP);

                                        return SprintCapacityResponse.builder()
                                                        .id(configuredCapacity == null ? null
                                                                        : configuredCapacity.getId())
                                                        .sprintId(sprintId)
                                                        .userId(pu.getUser().getId())
                                                        .userName(pu.getUser().getName())
                                                        .userEmail(pu.getUser().getEmail())
                                                        .capacityPoints(points)
                                                        .capacityHours(breakdown.availableHours)
                                                        .baseCapacityHours(breakdown.baseHours)
                                                        .reducedCapacityHours(breakdown.baseHours
                                                                        .subtract(breakdown.availableHours))
                                                        .workingDays(breakdown.workingDays)
                                                        .holidayDays(breakdown.holidayDays)
                                                        .halfDayDays(breakdown.halfDayDays)
                                                        .unavailableDays(breakdown.unavailableDays)
                                                        .weekendDays(breakdown.weekendDays)
                                                        .assignedEstimate(estimate)
                                                        .utilizationPercent(utilization)
                                                        .build();
                                })
                                .toList();
        }

        public SprintCapacityResponse saveCapacity(
                        Long sprintId,
                        SprintCapacityRequest request,
                        Long userId) {

                if (request == null || request.getUserId() == null) {
                        throw new IllegalArgumentException("Capacity user is required");
                }

                Sprint sprint = getSprint(sprintId);
                validateProjectAccess(sprint.getProject().getId(), userId);

                if (!projectUserRepository.existsByProjectIdAndUserId(
                                sprint.getProject().getId(), request.getUserId())) {
                        throw new RuntimeException("User is not a member of this project");
                }

                SprintCapacity capacity = capacityRepository
                                .findBySprintIdAndUserId(sprintId, request.getUserId())
                                .orElseGet(SprintCapacity::new);

                capacity.setSprint(sprint);
                capacity.setUser(getUser(request.getUserId()));
                capacity.setCapacityPoints(nz(request.getCapacityPoints()).max(BigDecimal.ZERO));

                // Capacity hours are derived from the availability calendar.
                CapacityBreakdown breakdown = calculateCapacityForUser(
                                sprint, request.getUserId());
                capacity.setCapacityHours(breakdown.availableHours);

                capacityRepository.save(capacity);

                return getCapacity(sprintId)
                                .stream()
                                .filter(x -> x.getUserId().equals(request.getUserId()))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Capacity row not found"));
        }

        private CapacityBreakdown calculateCapacityForUser(
                        Sprint sprint, Long userId) {

                Project project = sprint.getProject();
                ProjectUserMembershipAdapter membership = findMembership(project.getId(), userId);

                List<ProjectWorkingHoursResponse> history = sprint.getStartDate() != null && sprint.getEndDate() != null
                                ? projectWorkingHoursService.getHistory(project.getId())
                                : List.of();

                List<MemberAvailability> entries = sprint.getStartDate() != null && sprint.getEndDate() != null
                                ? memberAvailabilityRepository
                                                .findByProjectIdAndAvailabilityDateBetweenOrderByAvailabilityDateAsc(
                                                                project.getId(), sprint.getStartDate(),
                                                                sprint.getEndDate())
                                : List.of();

                Map<LocalDate, MemberAvailability> holidays = new LinkedHashMap<>();
                Map<LocalDate, MemberAvailability> own = new LinkedHashMap<>();
                for (MemberAvailability entry : entries) {
                        if (entry.getUser() == null && entry.getAvailabilityType() == MemberAvailabilityType.HOLIDAY) {
                                holidays.put(entry.getAvailabilityDate(), entry);
                        } else if (entry.getUser() != null && entry.getUser().getId().equals(userId)) {
                                own.put(entry.getAvailabilityDate(), entry);
                        }
                }

                return calculateCapacity(
                                membership.projectUser,
                                sprint.getStartDate(),
                                sprint.getEndDate(),
                                history,
                                holidays,
                                Map.of(userId, own));
        }

        private CapacityBreakdown calculateCapacity(
                        com.projectmanagement.app.project.ProjectUser projectUser,
                        LocalDate start,
                        LocalDate end,
                        List<ProjectWorkingHoursResponse> history,
                        Map<LocalDate, MemberAvailability> holidays,
                        Map<Long, Map<LocalDate, MemberAvailability>> memberAvailability) {

                CapacityBreakdown result = new CapacityBreakdown();
                if (start == null || end == null || end.isBefore(start)) {
                        return result;
                }

                LocalDate joinDate = projectUser.getCreatedAt() == null
                                ? start
                                : projectUser.getCreatedAt().toLocalDate();

                LocalDate cursor = start.isAfter(joinDate) ? start : joinDate;
                Map<LocalDate, MemberAvailability> own = memberAvailability.getOrDefault(projectUser.getUser().getId(),
                                Map.of());

                while (!cursor.isAfter(end)) {
                        int dayOfWeek = cursor.getDayOfWeek().getValue();
                        if (dayOfWeek >= 6) {
                                result.weekendDays++;
                                cursor = cursor.plusDays(1);
                                continue;
                        }

                        BigDecimal workingHours = effectiveWorkingHours(history, cursor);
                        result.baseHours = result.baseHours.add(workingHours);
                        result.workingDays++;

                        MemberAvailability entry = own.get(cursor);
                        MemberAvailability holiday = holidays.get(cursor);

                        // A project holiday applies to every member.
                        if (holiday != null) {
                                result.holidayDays++;
                                result.availableHours = result.availableHours;
                        } else if (entry == null) {
                                result.availableHours = result.availableHours.add(workingHours);
                        } else {
                                switch (entry.getAvailabilityType()) {
                                        case AVAILABLE -> result.availableHours = result.availableHours
                                                        .add(entry.getAvailableHours() == null
                                                                        ? workingHours
                                                                        : entry.getAvailableHours()
                                                                                        .max(BigDecimal.ZERO));
                                        case HALF_DAY -> {
                                                result.halfDayDays++;
                                                result.availableHours = result.availableHours
                                                                .add(workingHours.divide(BigDecimal.valueOf(2), 2,
                                                                                RoundingMode.HALF_UP));
                                        }
                                        case HOLIDAY -> result.holidayDays++;
                                        case UNAVAILABLE -> result.unavailableDays++;
                                }
                        }

                        cursor = cursor.plusDays(1);
                }

                result.availableHours = result.availableHours.max(BigDecimal.ZERO);
                result.baseHours = result.baseHours.max(BigDecimal.ZERO);
                return result;
        }

        private BigDecimal effectiveWorkingHours(
                        List<ProjectWorkingHoursResponse> history,
                        LocalDate date) {

                return history.stream()
                                .filter(h -> h.getEffectiveFrom() != null
                                                && !h.getEffectiveFrom().isAfter(date))
                                .max(Comparator.comparing(ProjectWorkingHoursResponse::getEffectiveFrom))
                                .map(ProjectWorkingHoursResponse::getWorkingHours)
                                .orElse(BigDecimal.valueOf(8));
        }

        private ProjectUserMembershipAdapter findMembership(Long projectId, Long userId) {
                return projectUserRepository.findByProjectIdAndUserId(projectId, userId)
                                .map(ProjectUserMembershipAdapter::new)
                                .orElseThrow(() -> new RuntimeException("User is not a member of this project"));
        }

        private static class ProjectUserMembershipAdapter {
                private final com.projectmanagement.app.project.ProjectUser projectUser;

                private ProjectUserMembershipAdapter(com.projectmanagement.app.project.ProjectUser projectUser) {
                        this.projectUser = projectUser;
                }
        }

        private static class CapacityBreakdown {
                private BigDecimal baseHours = BigDecimal.ZERO;
                private BigDecimal availableHours = BigDecimal.ZERO;
                private int workingDays;
                private int holidayDays;
                private int halfDayDays;
                private int unavailableDays;
                private int weekendDays;
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
