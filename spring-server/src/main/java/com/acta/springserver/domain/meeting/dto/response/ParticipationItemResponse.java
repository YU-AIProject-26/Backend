package com.acta.springserver.domain.meeting.dto.response;

public record ParticipationItemResponse(
        String speakerName,
        Integer percentage
) {
}

