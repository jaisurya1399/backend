package com.projectmanagement.app.sprint;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class SprintCapacityResponse {
    private Long id;
    private Long sprintId;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal capacityPoints;
    private BigDecimal capacityHours;
    private BigDecimal assignedEstimate;
    private BigDecimal utilizationPercent;
}
