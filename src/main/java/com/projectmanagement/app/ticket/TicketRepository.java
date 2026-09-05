package com.projectmanagement.app.ticket;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository
                extends JpaRepository<Ticket, Long> {

        // =========================================================
        // ACTIVE / DELETED
        // =========================================================

        List<Ticket> findByDeletedAtIsNull();

        List<Ticket> findByDeletedAtIsNotNull();

        // =========================================================
        // PROJECT
        // =========================================================

        List<Ticket> findByProjectId(Long projectId);

        List<Ticket> findByProjectIdAndDeletedAtIsNull(
                        Long projectId);

        long countByProjectId(Long projectId);

        long countByProjectIdAndDeletedAtIsNull(
                        Long projectId);

        // =========================================================
        // OWNER
        // =========================================================

        List<Ticket> findByOwnerId(Long ownerId);

        List<Ticket> findByOwnerIdAndDeletedAtIsNull(
                        Long ownerId);

        long countByOwnerId(Long ownerId);

        // =========================================================
        // RESPONSIBLE
        // =========================================================

        List<Ticket> findByResponsibleId(Long responsibleId);

        List<Ticket> findByResponsibleIdAndDeletedAtIsNull(
                        Long responsibleId);

        long countByResponsibleId(Long responsibleId);

        // =========================================================
        // STATUS
        // =========================================================

        List<Ticket> findByStatusId(Long statusId);

        List<Ticket> findByStatusIdAndDeletedAtIsNull(
                        Long statusId);

        long countByStatusId(Long statusId);

        // =========================================================
        // TYPE
        // =========================================================

        List<Ticket> findByTypeId(Long typeId);

        List<Ticket> findByTypeIdAndDeletedAtIsNull(
                        Long typeId);

        // =========================================================
        // PRIORITY
        // =========================================================

        List<Ticket> findByPriorityId(Long priorityId);

        List<Ticket> findByPriorityIdAndDeletedAtIsNull(
                        Long priorityId);

        // =========================================================
        // EPIC
        // =========================================================

        List<Ticket> findByEpicId(Long epicId);

        List<Ticket> findByEpicIdAndDeletedAtIsNull(
                        Long epicId);

        // =========================================================
        // PROJECT + STATUS
        // =========================================================

        List<Ticket> findByProjectIdAndStatusId(
                        Long projectId,
                        Long statusId);

        List<Ticket> findByProjectIdAndStatusIdAndDeletedAtIsNull(
                        Long projectId,
                        Long statusId);

        // =========================================================
        // PROJECT + TYPE
        // =========================================================

        List<Ticket> findByProjectIdAndTypeId(
                        Long projectId,
                        Long typeId);

        // =========================================================
        // PROJECT + PRIORITY
        // =========================================================

        List<Ticket> findByProjectIdAndPriorityId(
                        Long projectId,
                        Long priorityId);

        // =========================================================
        // PROJECT + EPIC
        // =========================================================

        List<Ticket> findByProjectIdAndEpicId(
                        Long projectId,
                        Long epicId);

        List<Ticket> findByProjectIdAndEpicIdAndDeletedAtIsNull(
                        Long projectId,
                        Long epicId);

        // =========================================================
        // CODE
        // =========================================================

        Optional<Ticket> findByCode(String code);

        Optional<Ticket> findByCodeAndDeletedAtIsNull(
                        String code);

        boolean existsByCode(String code);

        boolean existsByCodeAndIdNot(
                        String code,
                        Long id);

        // =========================================================
        // NAME
        // =========================================================

        List<Ticket> findByNameContainingIgnoreCase(
                        String name);

        List<Ticket> findByProjectIdAndNameContainingIgnoreCase(
                        Long projectId,
                        String name);

        // =========================================================
        // ORDER
        // =========================================================

        List<Ticket> findByProjectIdOrderByOrderAsc(
                        Long projectId);

        List<Ticket> findByProjectIdAndDeletedAtIsNullOrderByOrderAsc(
                        Long projectId);

        // =========================================================
        // PROJECT + STATUS + ORDER
        // =========================================================

        List<Ticket> findByProjectIdAndStatusIdAndDeletedAtIsNullOrderByOrderAsc(
                        Long projectId,
                        Long statusId);

        // =========================================================
        // DELETE BY PROJECT
        // =========================================================

        void deleteByProjectId(Long projectId);

        // =========================================================
        // DELETE BY EPIC
        // =========================================================

        void deleteByEpicId(Long epicId);
}