package com.acta.springserver.domain.meeting.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptUpdateRequest {

    private String speakerName;
    private String text;
    private Boolean keyStatement;
}

