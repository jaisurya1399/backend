package com.projectmanagement.app.dashboardanalytics;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.board.BoardStatusHistory;
import com.projectmanagement.app.board.BoardStatusHistoryRepository;
import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectAccessService;
import com.projectmanagement.app.project.ProjectRepository;
import com.projectmanagement.app.sprint.Sprint;
import com.projectmanagement.app.sprint.SprintIssueSnapshot;
import com.projectmanagement.app.sprint.SprintIssueSnapshotRepository;
import com.projectmanagement.app.sprint.SprintRepository;
import com.projectmanagement.app.sprint.SprintStatus;
import com.projectmanagement.app.ticket.Ticket;
import com.projectmanagement.app.ticket.TicketRepository;
import com.projectmanagement.app.ticket.TicketStatusCategory;

@Service
@Transactional(readOnly = true)
public class ProjectDashboardAnalyticsService {
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;
    private final TicketRepository ticketRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueSnapshotRepository snapshotRepository;
    private final BoardStatusHistoryRepository historyRepository;

    public ProjectDashboardAnalyticsService(ProjectRepository projectRepository, ProjectAccessService accessService,
            TicketRepository ticketRepository, SprintRepository sprintRepository,
            SprintIssueSnapshotRepository snapshotRepository,
            BoardStatusHistoryRepository historyRepository) {
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.ticketRepository = ticketRepository;
        this.sprintRepository = sprintRepository;
        this.snapshotRepository = snapshotRepository;
        this.historyRepository = historyRepository;
    }

    public Map<String, Object> getAnalytics(Long projectId, int days) {
        if (days != 14 && days != 30 && days != 60 && days != 90)
            days = 30;
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        accessService.requireView(project);
        List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedAtIsNull(projectId);
        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<BoardStatusHistory> histories = historyRepository.findByProjectIdAndChangedAtBetweenOrderByChangedAtAsc(
                projectId, LocalDateTime.now().minusDays(days), LocalDateTime.now());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", Map.of("id", project.getId(), "name", project.getName()));
        out.put("velocity", velocity(sprints));
        out.put("sprintReport", sprintReports(sprints, tickets));
        out.put("cumulativeFlow", cumulativeFlow(tickets, histories, days));
        out.put("controlChart", controlChart(tickets, histories));
        out.put("leadCycleTime", leadCycle(tickets, histories));
        out.put("createdResolved", createdResolved(tickets, days));
        return out;
    }

    private List<Map<String, Object>> velocity(List<Sprint> sprints) {
        List<Map<String, Object>> result = new ArrayList<>();
        sprints.stream().filter(s -> s.getStatus() == SprintStatus.COMPLETED)
                .sorted(Comparator.comparing(Sprint::getEndDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(12).forEach(s -> {
                    List<SprintIssueSnapshot> snaps = snapshotRepository.findBySprintIdOrderByTicketIdAsc(s.getId());
                    BigDecimal committed = snaps.stream().map(SprintIssueSnapshot::getEstimation)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal completed = snaps.stream()
                            .filter(x -> x.getFinalStatusCategory() == TicketStatusCategory.DONE)
                            .map(SprintIssueSnapshot::getEstimation).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.add(row("sprint", s.getName(), "committed", committed, "completed", completed));
                });
        return result;
    }

    private List<Map<String, Object>> sprintReports(List<Sprint> sprints, List<Ticket> tickets) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Sprint s : sprints.stream().limit(12).toList()) {
            List<Ticket> st = tickets.stream()
                    .filter(t -> t.getSprint() != null && s.getId().equals(t.getSprint().getId())).toList();
            BigDecimal total = st.stream().map(Ticket::getEstimation).filter(Objects::nonNull).reduce(BigDecimal.ZERO,
                    BigDecimal::add);
            BigDecimal done = st.stream()
                    .filter(t -> t.getStatus() != null && t.getStatus().getCategory() == TicketStatusCategory.DONE)
                    .map(Ticket::getEstimation).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(row("sprint", s.getName(), "issues", st.size(), "estimated", total, "completed", done));
        }
        return result;
    }

    private List<Map<String, Object>> cumulativeFlow(List<Ticket> tickets, List<BoardStatusHistory> histories,
            int days) {
        Map<String, TicketStatusCategory> current = new HashMap<>();
        for (Ticket t : tickets)
            current.put(t.getId().toString(), category(t));
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (TicketStatusCategory c : TicketStatusCategory.values())
                counts.put(c.name(), 0);
            for (Ticket t : tickets) {
                TicketStatusCategory c = categoryAt(t, histories, d.plusDays(1).atStartOfDay());
                counts.put(c.name(), counts.get(c.name()) + 1);
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("date", d.toString());
            r.putAll(counts);
            result.add(r);
        }
        return result;
    }

    private TicketStatusCategory categoryAt(Ticket t, List<BoardStatusHistory> histories, LocalDateTime at) {
        TicketStatusCategory c = category(t);
        List<BoardStatusHistory> hs = histories.stream()
                .filter(h -> h.getTicket().getId().equals(t.getId()) && h.getChangedAt().isAfter(at)).toList();
        if (!hs.isEmpty()) {
            List<BoardStatusHistory> all = historyRepository.findByTicketIdOrderByChangedAtAsc(t.getId());
            for (BoardStatusHistory h : all) {
                if (h.getChangedAt().isAfter(at))
                    break;
                c = h.getToStatus().getCategory();
            }
        }
        return c;
    }

    private List<Map<String, Object>> controlChart(List<Ticket> tickets, List<BoardStatusHistory> histories) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Ticket t : tickets) {
            List<BoardStatusHistory> hs = historyRepository.findByTicketIdOrderByChangedAtAsc(t.getId());
            LocalDateTime start = hs.stream()
                    .filter(h -> h.getToStatus().getCategory() == TicketStatusCategory.IN_PROGRESS)
                    .map(BoardStatusHistory::getChangedAt).findFirst().orElse(null);
            LocalDateTime done = hs.stream().filter(h -> h.getToStatus().getCategory() == TicketStatusCategory.DONE)
                    .map(BoardStatusHistory::getChangedAt).findFirst().orElse(null);
            if (start != null && done != null && !done.isBefore(start))
                out.add(row("ticket", t.getCode(), "cycleHours",
                        round(Duration.between(start, done).toMinutes() / 60.0), "date",
                        done.toLocalDate().toString()));
        }
        return out.stream().sorted(Comparator.comparing(x -> String.valueOf(x.get("date")))).limit(200).toList();
    }

    private List<Map<String, Object>> leadCycle(List<Ticket> tickets, List<BoardStatusHistory> histories) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Ticket t : tickets) {
            LocalDateTime resolved = t.getResolvedAt();
            if (resolved == null)
                continue;
            double lead = Duration.between(t.getCreatedAt(), resolved).toMinutes() / 60.0;
            List<BoardStatusHistory> hs = historyRepository.findByTicketIdOrderByChangedAtAsc(t.getId());
            LocalDateTime start = hs.stream()
                    .filter(h -> h.getToStatus().getCategory() == TicketStatusCategory.IN_PROGRESS)
                    .map(BoardStatusHistory::getChangedAt).findFirst().orElse(null);
            Double cycle = start == null ? null : Duration.between(start, resolved).toMinutes() / 60.0;
            out.add(row("ticket", t.getCode(), "leadHours", round(lead), "cycleHours",
                    cycle == null ? null : round(cycle)));
        }
        return out;
    }

    private List<Map<String, Object>> createdResolved(List<Ticket> tickets, int days) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long c = tickets.stream().filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(d))
                    .count();
            long r = tickets.stream()
                    .filter(t -> t.getResolvedAt() != null && t.getResolvedAt().toLocalDate().equals(d)).count();
            out.add(row("date", d.toString(), "created", c, "resolved", r));
        }
        return out;
    }

    private TicketStatusCategory category(Ticket t) {
        return t.getStatus() == null || t.getStatus().getCategory() == null ? TicketStatusCategory.BACKLOG
                : t.getStatus().getCategory();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2)
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }
}
