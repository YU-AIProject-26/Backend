package com.acta.springserver.domain.meeting.dto.response;

public record ScheduleItemResponse(
        String title,
        String startsAt,
        String participantSummary
) {
}

