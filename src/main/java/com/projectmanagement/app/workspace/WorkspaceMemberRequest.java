package com.projectmanagement.app.workspace;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMemberRequest {
    @NotNull
    @Positive
    private Long userId;
    @NotNull
    private WorkspaceRole role;
}
