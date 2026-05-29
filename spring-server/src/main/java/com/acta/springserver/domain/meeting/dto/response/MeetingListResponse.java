package com.acta.springserver.domain.meeting.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingListResponse(
        Long id,
        String title,
        String oneLineSummary,
        LocalDateTime meetingAt,
        Integer durationMinutes,
        String folderName,
        String status,
        List<String> tags,
        String audioUrl
) {
}

