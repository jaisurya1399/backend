package com.projectmanagement.app.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    List<Project> findByDeletedAtIsNull();

    List<Project> findByOwnerId(Long ownerId);

    List<Project> findByOwnerIdAndDeletedAtIsNull(
            Long ownerId);

    List<Project> findByStatusId(Long statusId);

    List<Project> findByStatusIdAndDeletedAtIsNull(
            Long statusId);

    Optional<Project> findByName(String name);

    Optional<Project> findByNameAndDeletedAtIsNull(
            String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(
            String name,
            Long id);

    boolean existsByTicketPrefix(String ticketPrefix);

    boolean existsByTicketPrefixAndIdNot(
            String ticketPrefix,
            Long id);
}