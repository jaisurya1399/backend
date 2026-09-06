package com.projectmanagement.app.ticketattachment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.projectmanagement.app.ticket.Ticket;
import com.projectmanagement.app.ticket.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketAttachmentService {

        private final TicketAttachmentRepository attachmentRepository;
        private final TicketRepository ticketRepository;

        /**
         * Get all attachments for a ticket.
         */
        @Transactional(readOnly = true)
        public List<TicketAttachmentResponse> getByTicketId(
                        Long ticketId) {

                if (ticketId == null || ticketId <= 0) {
                        throw new IllegalArgumentException(
                                        "Invalid ticket ID");
                }

                if (!ticketRepository.existsById(ticketId)) {
                        throw new RuntimeException(
                                        "Ticket not found with id: " + ticketId);
                }

                return attachmentRepository
                                .findByTicketIdOrderByCreatedAtDesc(ticketId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Get attachment entity by ID.
         */
        @Transactional(readOnly = true)
        public TicketAttachment getEntity(Long id) {

                if (id == null || id <= 0) {
                        throw new IllegalArgumentException(
                                        "Invalid attachment ID");
                }

                return attachmentRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Attachment not found with id: " + id));
        }

        /**
         * Upload attachment.
         */
        @Transactional
        public TicketAttachmentResponse upload(
                        Long ticketId,
                        MultipartFile file) {

                if (ticketId == null || ticketId <= 0) {
                        throw new IllegalArgumentException(
                                        "Invalid ticket ID");
                }

                if (file == null) {
                        throw new IllegalArgumentException(
                                        "File is required");
                }

                if (file.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "File cannot be empty");
                }

                Ticket ticket = ticketRepository
                                .findById(ticketId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: " + ticketId));

                try {

                        String originalName = file.getOriginalFilename();

                        if (originalName == null ||
                                        originalName.isBlank()) {

                                originalName = "attachment";
                        }

                        /*
                         * Prevent path traversal from filename.
                         */
                        originalName = originalName
                                        .replace("\\", "/")
                                        .substring(
                                                        originalName
                                                                        .replace("\\", "/")
                                                                        .lastIndexOf("/") + 1);

                        String contentType = file.getContentType();

                        if (contentType == null ||
                                        contentType.isBlank()) {

                                contentType = "application/octet-stream";
                        }

                        byte[] fileData = file.getBytes();

                        TicketAttachment attachment = TicketAttachment.builder()
                                        .ticket(ticket)
                                        .fileName(originalName)
                                        .originalName(originalName)
                                        .contentType(contentType)
                                        .fileSize((long) fileData.length)
                                        .fileData(fileData)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                        TicketAttachment saved = attachmentRepository.save(attachment);

                        return toResponse(saved);

                } catch (Exception exception) {

                        /*
                         * Print actual exception in backend terminal.
                         * This is useful if DB/file mapping fails.
                         */
                        exception.printStackTrace();

                        throw new RuntimeException(
                                        "Failed to upload attachment: "
                                                        + exception.getMessage(),
                                        exception);
                }
        }

        /**
         * Delete attachment.
         */
        @Transactional
        public void delete(Long id) {

                if (id == null || id <= 0) {
                        throw new IllegalArgumentException(
                                        "Invalid attachment ID");
                }

                if (!attachmentRepository.existsById(id)) {
                        throw new RuntimeException(
                                        "Attachment not found with id: " + id);
                }

                attachmentRepository.deleteById(id);
        }

        /**
         * Convert entity to response DTO.
         */
        private TicketAttachmentResponse toResponse(
                        TicketAttachment attachment) {

                return TicketAttachmentResponse.builder()
                                .id(attachment.getId())
                                .ticketId(
                                                attachment.getTicket().getId())
                                .fileName(
                                                attachment.getFileName())
                                .originalName(
                                                attachment.getOriginalName())
                                .contentType(
                                                attachment.getContentType())
                                .fileSize(
                                                attachment.getFileSize())
                                .createdAt(
                                                attachment.getCreatedAt())
                                .updatedAt(
                                                attachment.getUpdatedAt())
                                .build();
        }
}