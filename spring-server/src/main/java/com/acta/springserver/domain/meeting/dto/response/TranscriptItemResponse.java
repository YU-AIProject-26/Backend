package com.acta.springserver.domain.meeting.dto.response;

public record TranscriptItemResponse(
        String speakerName,
        Integer startedAtSeconds,
        String text,
        boolean keyStatement
) {
}

