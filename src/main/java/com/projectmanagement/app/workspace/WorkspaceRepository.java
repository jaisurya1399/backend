package com.projectmanagement.app.workspace;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository
        extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findBySlugAndDeletedAtIsNull(
            String slug);

    boolean existsBySlugAndDeletedAtIsNull(
            String slug);

    boolean existsBySlugAndDeletedAtIsNullAndIdNot(
            String slug,
            Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySlug(String slug);
}