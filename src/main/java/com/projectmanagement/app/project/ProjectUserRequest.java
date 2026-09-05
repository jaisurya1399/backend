package com.projectmanagement.app.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class ProjectUserRequest {

    @NotNull
    @Positive
    private Long userId;

    @NotNull
    @Positive
    private Long projectId;

    @NotBlank
    @Size(max = 255)
    private String role;
}