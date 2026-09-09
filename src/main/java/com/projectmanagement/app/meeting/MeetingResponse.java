package com.projectmanagement.app.meeting;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private Long id, projectId, createdBy;
    private String projectName, title, agenda, meetingUrl, status;
    private LocalDateTime startsAt, endsAt, createdAt;
}
