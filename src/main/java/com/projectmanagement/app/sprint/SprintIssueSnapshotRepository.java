package com.projectmanagement.app.sprint;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintIssueSnapshotRepository extends JpaRepository<SprintIssueSnapshot, Long> {
    List<SprintIssueSnapshot> findBySprintIdOrderByTicketIdAsc(Long sprintId);
}
