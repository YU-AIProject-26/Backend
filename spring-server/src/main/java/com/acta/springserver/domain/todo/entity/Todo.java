package com.acta.springserver.domain.todo.entity;

import com.acta.springserver.domain.meeting.entity.Meeting;
import com.acta.springserver.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "todos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String sourceTitle;

    @Column(length = 100)
    private String createdByName;

    @Column(length = 100)
    private String assigneeName;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoPriority priority;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Todo(
            User user,
            Meeting meeting,
            String title,
            String sourceTitle,
            String createdByName,
            String assigneeName,
            LocalDate dueDate,
            TodoStatus status,
            TodoPriority priority,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.user = user;
        this.meeting = meeting;
        this.title = title;
        this.sourceTitle = sourceTitle;
        this.createdByName = createdByName;
        this.assigneeName = assigneeName;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            String title,
            String sourceTitle,
            String createdByName,
            String assigneeName,
            LocalDate dueDate,
            TodoStatus status,
            TodoPriority priority
    ) {
        if (title != null) {
            this.title = title;
        }
        if (sourceTitle != null) {
            this.sourceTitle = sourceTitle;
        }
        if (createdByName != null) {
            this.createdByName = createdByName;
        }
        if (assigneeName != null) {
            this.assigneeName = assigneeName;
        }
        if (dueDate != null) {
            this.dueDate = dueDate;
        }
        if (status != null) {
            this.status = status;
        }
        if (priority != null) {
            this.priority = priority;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
