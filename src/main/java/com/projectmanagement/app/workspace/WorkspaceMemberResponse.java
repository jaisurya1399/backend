package com.projectmanagement.app.workspace;

import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceMemberResponse {
    private Long id; private Long workspaceId; private Long userId; private String userName; private String userEmail;
    private WorkspaceRole role; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
