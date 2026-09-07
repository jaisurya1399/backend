package com.projectmanagement.app.workspace;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceRequest {
    @NotBlank @Size(max = 120) private String name;
    @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Slug must use lowercase letters, numbers and hyphens")
    @Size(max = 120) private String slug;
    private String description;
}
