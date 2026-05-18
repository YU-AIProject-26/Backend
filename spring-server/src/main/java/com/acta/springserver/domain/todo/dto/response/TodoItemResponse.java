package com.acta.springserver.domain.todo.dto.response;

public record TodoItemResponse(
        Long id,
        Long meetingId,
        String title,
        String sourceTitle,
        String createdByName,
        String assigneeName,
        String dueDate,
        String status,
        String priority
) {
}

