package com.acta.springserver.domain.meeting.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class MeetingCreateRequest {

    private String title;
    private String description;
    private String meetingAt;
    private Integer durationMinutes;
    private Integer participantCount;
    private String folderName;
    private String tags;
    private MultipartFile audioFile;
}

