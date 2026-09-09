package com.projectmanagement.app.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.auth.CurrentUserService;
import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.project.ProjectAccessService;
import com.projectmanagement.app.project.ProjectRepository;
import com.projectmanagement.app.realtime.RealtimeEventService;

@Service
@Transactional
public class ChatService {
    private final ChatMessageRepository repo;
    private final CurrentUserService current;
    private final ProjectRepository projects;
    private final ProjectAccessService access;
    private final RealtimeEventService events;

    public ChatService(ChatMessageRepository repo, CurrentUserService current, ProjectRepository projects,
            ProjectAccessService access, RealtimeEventService events) {
        this.repo = repo;
        this.current = current;
        this.projects = projects;
        this.access = access;
        this.events = events;
    }

    public List<ChatMessageResponse> direct(Long other) {
        Long me = current.getCurrentUserId();
        return repo.findTop100ByRoomTypeAndSenderIdAndRecipientIdOrRoomTypeAndSenderIdAndRecipientIdOrderByCreatedAtAsc(
                "DIRECT", me, other, "DIRECT", other, me).stream().map(this::to).collect(Collectors.toList());
    }

    public List<ChatMessageResponse> room(Long projectId) {
        Project p = projects.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        access.requireView(p);
        return repo.findTop100ByRoomTypeAndRoomIdOrderByCreatedAtAsc("PROJECT", projectId).stream().map(this::to)
                .collect(Collectors.toList());
    }

    public ChatMessageResponse sendDirect(Long recipient, ChatMessageRequest req) {
        if (recipient == null)
            throw new RuntimeException("Recipient is required");
        ChatMessage m = ChatMessage.builder().roomType("DIRECT").senderId(current.getCurrentUserId())
                .recipientId(recipient).content(req.getContent().trim()).build();
        ChatMessageResponse out = to(repo.save(m));
        events.publishUser(recipient, "chat-message", out);
        events.publishUser(current.getCurrentUserId(), "chat-message", out);
        return out;
    }

    public ChatMessageResponse sendRoom(Long projectId, ChatMessageRequest req) {
        Project p = projects.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        access.requireEditor(p);
        ChatMessage m = ChatMessage.builder().roomType("PROJECT").roomId(projectId).senderId(current.getCurrentUserId())
                .content(req.getContent().trim()).build();
        ChatMessageResponse out = to(repo.save(m));
        events.publishProject(projectId, "chat-message", out);
        return out;
    }

    private ChatMessageResponse to(ChatMessage m) {
        return ChatMessageResponse.builder().id(m.getId()).roomType(m.getRoomType()).roomId(m.getRoomId())
                .senderId(m.getSenderId()).recipientId(m.getRecipientId()).content(m.getContent())
                .createdAt(m.getCreatedAt()).build();
    }
}
