package com.projectmanagement.app.workspace;

import java.time.LocalDateTime;
import com.projectmanagement.app.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_members", uniqueConstraints = @UniqueConstraint(name = "uk_workspace_members_workspace_user", columnNames = {"workspace_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "workspace_members_workspace_id_foreign"))
    private Workspace workspace;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "workspace_members_user_id_foreign"))
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkspaceRole role;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); if (updatedAt == null) updatedAt = createdAt; if (role == null) role = WorkspaceRole.MEMBER; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
