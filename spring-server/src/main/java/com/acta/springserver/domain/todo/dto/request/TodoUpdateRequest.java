package com.acta.springserver.domain.todo.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoUpdateRequest {

    private String title;
    private String sourceTitle;
    private String createdByName;
    private String assigneeName;
    private String dueDate;
    private String status;
    private String priority;
}
