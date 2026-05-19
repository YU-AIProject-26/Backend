package com.acta.springserver.domain.schedule.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleCreateRequest {

    private Long meetingId;
    private String title;
    private String startsAt;
    private String location;
    private Integer participantCount;
}

