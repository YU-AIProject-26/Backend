package com.acta.springserver.domain.todo.dto.response;

import java.util.List;

public record TodoListResponse(
        int totalCount,
        int completedCount,
        int inProgressCount,
        int urgentCount,
        List<TodoItemResponse> items
) {
}

