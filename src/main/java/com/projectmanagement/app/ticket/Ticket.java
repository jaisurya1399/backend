package com.projectmanagement.app.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.projectmanagement.app.epic.Epic;
import com.projectmanagement.app.label.Label;
import com.projectmanagement.app.milestone.Milestone;
import com.projectmanagement.app.project.Project;
import com.projectmanagement.app.sprint.Sprint;
import com.projectmanagement.app.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "tickets_owner_id_foreign"))
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id", foreignKey = @ForeignKey(name = "tickets_responsible_id_foreign"))
    private User responsible;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false, foreignKey = @ForeignKey(name = "tickets_status_id_foreign"))
    private TicketStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "tickets_project_id_foreign"))
    private Project project;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "code", nullable = false, length = 255)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false, foreignKey = @ForeignKey(name = "tickets_type_id_foreign"))
    private TicketType type;

    @Column(name = "\"order\"", nullable = false)
    @Builder.Default
    private Integer order = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "priority_id", nullable = false, foreignKey = @ForeignKey(name = "tickets_priority_id_foreign"))
    private TicketPriority priority;

    @Column(name = "estimation", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal estimation = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id", foreignKey = @ForeignKey(name = "tickets_epic_id_foreign"))
    private Epic epic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "tickets_parent_id_foreign"))
    private Ticket parent;

    // =========================================================
    // SPRINT
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", foreignKey = @ForeignKey(name = "tickets_sprint_id_foreign"))
    private Sprint sprint;

    // =========================================================
    // MILESTONE
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", foreignKey = @ForeignKey(name = "tickets_milestone_id_foreign"))
    private Milestone milestone;

    // =========================================================
    // LABELS
    // =========================================================

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ticket_labels", joinColumns = @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "ticket_labels_ticket_id_foreign")), inverseJoinColumns = @JoinColumn(name = "label_id", foreignKey = @ForeignKey(name = "ticket_labels_label_id_foreign")))
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    // =========================================================
    // CREATE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (order == null) {
            order = 0;
        }

        if (estimation == null) {
            estimation = BigDecimal.ZERO;
        }

        if (labels == null) {
            labels = new HashSet<>();
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
