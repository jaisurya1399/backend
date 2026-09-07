package com.projectmanagement.app.workspace;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceMemberRequest {
    @NotNull @Positive private Long userId;
    @NotNull private WorkspaceRole role;
}
