package com.projectmanagement.app.meeting;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.auth.CurrentUserService;
import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectAccessService;
import com.projectmanagement.app.project.ProjectRepository;

@Service
@Transactional
public class MeetingService {
    private final MeetingRepository repo;
    private final ProjectRepository projects;
    private final ProjectAccessService access;
    private final CurrentUserService current;

    public MeetingService(MeetingRepository r, ProjectRepository p, ProjectAccessService a, CurrentUserService c) {
        repo = r;
        projects = p;
        access = a;
        current = c;
    }

    public List<MeetingResponse> list(Long pid) {
        Project p = projects.findById(pid).orElseThrow(() -> new RuntimeException("Project not found"));
        access.requireView(p);
        return repo.findByProjectIdOrderByStartsAtDesc(pid).stream().map(this::to).collect(Collectors.toList());
    }

    public MeetingResponse create(Long pid, MeetingRequest req) {
        Project p = projects.findById(pid).orElseThrow(() -> new RuntimeException("Project not found"));
        access.requireManager(p);
        if (req.getEndsAt() != null && !req.getEndsAt().isAfter(req.getStartsAt()))
            throw new RuntimeException("Meeting end must be after start");
        Meeting m = Meeting.builder().project(p).title(req.getTitle().trim()).agenda(req.getAgenda())
                .startsAt(req.getStartsAt()).endsAt(req.getEndsAt()).meetingUrl(req.getMeetingUrl())
                .createdBy(current.getCurrentUserId()).status("SCHEDULED").build();
        return to(repo.save(m));
    }

    public MeetingResponse updateStatus(Long id, String status) {
        Meeting m = repo.findById(id).orElseThrow(() -> new RuntimeException("Meeting not found"));
        access.requireManager(m.getProject());
        m.setStatus(status.trim().toUpperCase());
        return to(repo.save(m));
    }

    private MeetingResponse to(Meeting m) {
        return MeetingResponse.builder().id(m.getId()).projectId(m.getProject().getId())
                .projectName(m.getProject().getName()).createdBy(m.getCreatedBy()).title(m.getTitle())
                .agenda(m.getAgenda()).meetingUrl(m.getMeetingUrl()).status(m.getStatus()).startsAt(m.getStartsAt())
                .endsAt(m.getEndsAt()).createdAt(m.getCreatedAt()).build();
    }
}
