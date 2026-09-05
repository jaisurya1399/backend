package com.projectmanagement.app.ticket;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketRelationService {

        private final TicketRelationRepository ticketRelationRepository;
        private final TicketRepository ticketRepository;

        @Transactional(readOnly = true)
        public List<TicketRelationResponse> getAll() {

                return ticketRelationRepository.findAll()
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public TicketRelationResponse getById(Long id) {

                TicketRelation relation = ticketRelationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket relation not found with id: " + id));

                return mapToResponse(relation);
        }

        @Transactional(readOnly = true)
        public List<TicketRelationResponse> getByTicket(Long ticketId) {

                validateTicket(ticketId);

                return ticketRelationRepository
                                .findByTicketIdOrderBySortAsc(ticketId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TicketRelationResponse> getByRelation(Long relationId) {

                validateTicket(relationId);

                return ticketRelationRepository
                                .findByRelationId(relationId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TicketRelationResponse> getByType(String type) {

                return ticketRelationRepository
                                .findByType(type)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TicketRelationResponse> getByTicketAndType(
                        Long ticketId,
                        String type) {

                validateTicket(ticketId);

                return ticketRelationRepository
                                .findByTicketIdAndType(ticketId, type)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public boolean exists(
                        Long ticketId,
                        Long relationId,
                        String type) {

                return ticketRelationRepository
                                .existsByTicketIdAndRelationIdAndType(
                                                ticketId,
                                                relationId,
                                                type);
        }

        public TicketRelationResponse create(
                        TicketRelationRequest request) {

                validateTicket(request.getTicketId());
                validateTicket(request.getRelationId());

                if (request.getTicketId().equals(request.getRelationId())) {
                        throw new RuntimeException(
                                        "A ticket cannot have a relation with itself");
                }

                if (ticketRelationRepository
                                .existsByTicketIdAndRelationIdAndType(
                                                request.getTicketId(),
                                                request.getRelationId(),
                                                request.getType())) {

                        throw new RuntimeException(
                                        "This ticket relation already exists");
                }

                Ticket ticket = ticketRepository
                                .findById(request.getTicketId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: "
                                                                + request.getTicketId()));

                Ticket relation = ticketRepository
                                .findById(request.getRelationId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Related ticket not found with id: "
                                                                + request.getRelationId()));

                TicketRelation ticketRelation = TicketRelation.builder()
                                .ticket(ticket)
                                .relation(relation)
                                .type(request.getType())
                                .sort(
                                                request.getSort() != null
                                                                ? request.getSort()
                                                                : 1)
                                .build();

                return mapToResponse(
                                ticketRelationRepository.save(ticketRelation));
        }

        public TicketRelationResponse update(
                        Long id,
                        TicketRelationRequest request) {

                TicketRelation ticketRelation = ticketRelationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket relation not found with id: "
                                                                + id));

                validateTicket(request.getTicketId());
                validateTicket(request.getRelationId());

                if (request.getTicketId().equals(request.getRelationId())) {
                        throw new RuntimeException(
                                        "A ticket cannot have a relation with itself");
                }

                Ticket ticket = ticketRepository
                                .findById(request.getTicketId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Ticket not found with id: "
                                                                + request.getTicketId()));

                Ticket relation = ticketRepository
                                .findById(request.getRelationId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Related ticket not found with id: "
                                                                + request.getRelationId()));

                ticketRelation.setTicket(ticket);
                ticketRelation.setRelation(relation);
                ticketRelation.setType(request.getType());

                if (request.getSort() != null) {
                        ticketRelation.setSort(request.getSort());
                }

                return mapToResponse(
                                ticketRelationRepository.save(ticketRelation));
        }

        public void delete(Long id) {

                if (!ticketRelationRepository.existsById(id)) {
                        throw new RuntimeException(
                                        "Ticket relation not found with id: " + id);
                }

                ticketRelationRepository.deleteById(id);
        }

        public void deleteByTicket(Long ticketId) {

                validateTicket(ticketId);

                ticketRelationRepository.deleteByTicketId(ticketId);
        }

        public void deleteByRelation(Long relationId) {

                validateTicket(relationId);

                ticketRelationRepository.deleteByRelationId(relationId);
        }

        @Transactional(readOnly = true)
        public long countByTicket(Long ticketId) {

                validateTicket(ticketId);

                return ticketRelationRepository.countByTicketId(ticketId);
        }

        @Transactional(readOnly = true)
        public long countByRelation(Long relationId) {

                validateTicket(relationId);

                return ticketRelationRepository.countByRelationId(relationId);
        }

        private void validateTicket(Long ticketId) {

                if (!ticketRepository.existsById(ticketId)) {
                        throw new RuntimeException(
                                        "Ticket not found with id: " + ticketId);
                }
        }

        private TicketRelationResponse mapToResponse(
                        TicketRelation relation) {

                Ticket ticket = relation.getTicket();
                Ticket relatedTicket = relation.getRelation();

                return TicketRelationResponse.builder()
                                .id(relation.getId())

                                .ticketId(ticket.getId())
                                .ticketName(ticket.getName())
                                .ticketCode(ticket.getCode())

                                .relationId(relatedTicket.getId())
                                .relationName(relatedTicket.getName())
                                .relationCode(relatedTicket.getCode())

                                .type(relation.getType())
                                .sort(relation.getSort())

                                .createdAt(relation.getCreatedAt())
                                .updatedAt(relation.getUpdatedAt())

                                .build();
        }
}