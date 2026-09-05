package com.projectmanagement.app.epic;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
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
public class EpicRequest {

    @NotNull
    @Positive
    private Long projectId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private LocalDate startsAt;

    @NotNull
    private LocalDate endsAt;

    @Positive
    private Long parentId;

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isValidDateRange() {

        if (startsAt == null || endsAt == null) {
            return true;
        }

        return !endsAt.isBefore(startsAt);
    }
}