package com.acta.springserver.domain.meeting.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingUpdateRequest {

    private String title;
    private String description;
    private String oneLineSummary;
    private String meetingAt;
    private Integer durationMinutes;
    private Integer participantCount;
    private String folderName;
    private String tags;
    private String status;
}

