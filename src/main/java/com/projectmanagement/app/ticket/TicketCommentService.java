package com.projectmanagement.app.ticket;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketCommentService {

    private final TicketCommentRepository ticketCommentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getAll() {
        return ticketCommentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getActive() {
        return ticketCommentRepository.findByDeletedAtIsNull()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketCommentResponse getById(Long id) {
        TicketComment comment = ticketCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket comment not found with id: " + id));

        return mapToResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getByTicket(Long ticketId) {

        if (!ticketRepository.existsById(ticketId)) {
            throw new RuntimeException("Ticket not found with id: " + ticketId);
        }

        return ticketCommentRepository
                .findByTicketIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        return ticketCommentRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TicketCommentResponse create(TicketCommentRequest request) {

        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new RuntimeException(
                        "Ticket not found with id: " + request.getTicketId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + request.getUserId()));

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .user(user)
                .content(request.getContent())
                .build();

        return mapToResponse(
                ticketCommentRepository.save(comment));
    }

    public TicketCommentResponse update(
            Long id,
            TicketCommentRequest request) {

        TicketComment comment = ticketCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket comment not found with id: " + id));

        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new RuntimeException(
                        "Ticket not found with id: " + request.getTicketId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + request.getUserId()));

        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setContent(request.getContent());

        return mapToResponse(
                ticketCommentRepository.save(comment));
    }

    public void softDelete(Long id) {

        TicketComment comment = ticketCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket comment not found with id: " + id));

        comment.setDeletedAt(java.time.LocalDateTime.now());

        ticketCommentRepository.save(comment);
    }

    public void restore(Long id) {

        TicketComment comment = ticketCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Ticket comment not found with id: " + id));

        comment.setDeletedAt(null);

        ticketCommentRepository.save(comment);
    }

    public void permanentDelete(Long id) {

        if (!ticketCommentRepository.existsById(id)) {
            throw new RuntimeException(
                    "Ticket comment not found with id: " + id);
        }

        ticketCommentRepository.deleteById(id);
    }

    public void deleteByTicket(Long ticketId) {

        if (!ticketRepository.existsById(ticketId)) {
            throw new RuntimeException(
                    "Ticket not found with id: " + ticketId);
        }

        ticketCommentRepository.deleteByTicketId(ticketId);
    }

    public void deleteByUser(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException(
                    "User not found with id: " + userId);
        }

        ticketCommentRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countByTicket(Long ticketId) {

        if (!ticketRepository.existsById(ticketId)) {
            throw new RuntimeException(
                    "Ticket not found with id: " + ticketId);
        }

        return ticketCommentRepository
                .countByTicketIdAndDeletedAtIsNull(ticketId);
    }

    private TicketCommentResponse mapToResponse(TicketComment comment) {

        Ticket ticket = comment.getTicket();
        User user = comment.getUser();

        return TicketCommentResponse.builder()
                .id(comment.getId())

                .ticketId(ticket.getId())
                .ticketName(ticket.getName())
                .ticketCode(ticket.getCode())

                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())

                .content(comment.getContent())

                .deletedAt(comment.getDeletedAt())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())

                .build();
    }
}