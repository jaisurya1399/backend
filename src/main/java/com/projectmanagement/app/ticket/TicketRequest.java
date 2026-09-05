package com.projectmanagement.app.ticket;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
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
public class TicketRequest {

    @NotBlank(message = "Ticket name is required")
    @Size(max = 255, message = "Ticket name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Ticket content is required")
    private String content;

    @NotNull(message = "Owner ID is required")
    @Positive(message = "Owner ID must be positive")
    private Long ownerId;

    @Positive(message = "Responsible ID must be positive")
    private Long responsibleId;

    @NotNull(message = "Status ID is required")
    @Positive(message = "Status ID must be positive")
    private Long statusId;

    @NotNull(message = "Project ID is required")
    @Positive(message = "Project ID must be positive")
    private Long projectId;

    @Size(max = 255, message = "Ticket code must not exceed 255 characters")
    private String code;

    @NotNull(message = "Ticket type ID is required")
    @Positive(message = "Ticket type ID must be positive")
    private Long typeId;

    @Positive(message = "Order must be positive")
    @Builder.Default
    private Integer order = 0;

    @NotNull(message = "Priority ID is required")
    @Positive(message = "Priority ID must be positive")
    private Long priorityId;

    @DecimalMin(value = "0.00", message = "Estimation cannot be negative")
    @Builder.Default
    private BigDecimal estimation = BigDecimal.ZERO;

    @Positive(message = "Epic ID must be positive")
    private Long epicId;
}