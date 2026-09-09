package com.projectmanagement.app.sprint;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SprintCapacityRequest {

    @NotNull
    private Long userId;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal capacityPoints;

    /**
     * Kept for API backward compatibility. Server calculates capacity hours
     * from the member availability calendar and does not trust this value.
     */
    private BigDecimal capacityHours;
}
