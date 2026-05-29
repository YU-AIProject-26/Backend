package com.acta.springserver.domain.schedule.dto.response;

public record ScheduleResponse(
        Long id,
        Long meetingId,
        String title,
        String startsAt,
        String location,
        Integer participantCount
) {
}

