package com.projectmanagement.app.project;

import java.time.LocalDateTime;

import com.projectmanagement.app.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
@Table(name = "project_users", indexes = {
        @Index(name = "idx_project_users_user_id", columnList = "user_id"),
        @Index(name = "idx_project_users_project_id", columnList = "project_id"),
        @Index(name = "idx_project_users_role", columnList = "role"),
        @Index(name = "idx_project_users_responsibility", columnList = "responsibility_role")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "project_users_user_id_foreign"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "project_users_project_id_foreign"))
    private Project project;

    /** Project access level: PROJECT_ADMIN, MEMBER or VIEWER. */
    @Column(name = "role", nullable = false, length = 30)
    private String role;

    /**
     * Only meaningful for MEMBER. PROJECT_ADMIN and VIEWER keep this null.
     */
    @Column(name = "responsibility_role", length = 40)
    private String responsibilityRole;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null)
            createdAt = now;
        if (updatedAt == null)
            updatedAt = now;
        normalizeResponsibility();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeResponsibility();
    }

    private void normalizeResponsibility() {
        if (role == null || !ProjectRole.MEMBER.name().equals(role)) {
            responsibilityRole = null;
        } else if (responsibilityRole == null || responsibilityRole.isBlank()) {
            responsibilityRole = MemberResponsibility.DEVELOPER.name();
        } else {
            responsibilityRole = responsibilityRole.trim().toUpperCase();
        }
        if (role != null)
            role = role.trim().toUpperCase();
    }
}
