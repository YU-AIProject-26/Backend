package com.acta.springserver.domain.meeting.dto.response;

import com.acta.springserver.domain.todo.dto.response.TodoItemResponse;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingDetailResponse(
        Long id,
        String title,
        String description,
        String oneLineSummary,
        LocalDateTime meetingAt,
        Integer durationMinutes,
        Integer participantCount,
        String folderName,
        String status,
        List<String> tags,
        String audioUrl,
        String summary,
        Integer efficiencyScore,
        String efficiencyFeedback,
        List<String> keyPoints,
        List<TranscriptItemResponse> transcripts,
        List<ParticipationItemResponse> participationStats,
        List<FocusMetricResponse> focusMetrics,
        List<TodoItemResponse> todos,
        List<ScheduleItemResponse> schedules
) {
}

