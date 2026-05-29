package com.acta.springserver.domain.todo.service;

import com.acta.springserver.domain.meeting.entity.Meeting;
import com.acta.springserver.domain.meeting.repository.MeetingRepository;
import com.acta.springserver.domain.todo.dto.request.TodoCreateRequest;
import com.acta.springserver.domain.todo.dto.request.TodoUpdateRequest;
import com.acta.springserver.domain.todo.dto.response.TodoItemResponse;
import com.acta.springserver.domain.todo.dto.response.TodoListResponse;
import com.acta.springserver.domain.todo.entity.Todo;
import com.acta.springserver.domain.todo.entity.TodoPriority;
import com.acta.springserver.domain.todo.entity.TodoStatus;
import com.acta.springserver.domain.todo.repository.TodoRepository;
import com.acta.springserver.domain.user.entity.User;
import com.acta.springserver.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    public TodoListResponse getTodos(Long userId, String q, String status, String priority, String assigneeName, String sortBy) {
        List<Todo> allTodos = todoRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        List<Todo> filtered = allTodos.stream()
                .filter(todo -> matchesQuery(todo, q))
                .filter(todo -> matchesStatus(todo, status))
                .filter(todo -> matchesPriority(todo, priority))
                .filter(todo -> matchesAssignee(todo, assigneeName))
                .sorted(resolveComparator(sortBy))
                .toList();

        return new TodoListResponse(
                allTodos.size(),
                (int) allTodos.stream().filter(todo -> todo.getStatus() == TodoStatus.COMPLETED).count(),
                (int) allTodos.stream().filter(todo -> todo.getStatus() == TodoStatus.IN_PROGRESS).count(),
                (int) allTodos.stream().filter(todo -> todo.getPriority() == TodoPriority.URGENT).count(),
                filtered.stream().map(this::toResponse).toList()
        );
    }

    @Transactional
    public TodoItemResponse createTodo(Long userId, TodoCreateRequest request) {
        User user = getUser(userId);
        Meeting meeting = getMeetingOrNull(userId, request.getMeetingId());
        LocalDateTime now = LocalDateTime.now();

        Todo todo = Todo.builder()
                .user(user)
                .meeting(meeting)
                .title(request.getTitle())
                .sourceTitle(resolveSourceTitle(request.getSourceTitle(), meeting))
                .createdByName(request.getCreatedByName())
                .assigneeName(request.getAssigneeName())
                .dueDate(parseDate(request.getDueDate()))
                .status(parseStatus(request.getStatus(), TodoStatus.PENDING))
                .priority(parsePriority(request.getPriority(), TodoPriority.MEDIUM))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Todo saved = todoRepository.save(todo);
        return toResponse(saved);
    }

    @Transactional
    public TodoItemResponse updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {
        Todo todo = getTodo(userId, todoId);
        todo.update(
                request.getTitle(),
                request.getSourceTitle(),
                request.getCreatedByName(),
                request.getAssigneeName(),
                parseDate(request.getDueDate()),
                parseStatus(request.getStatus(), null),
                parsePriority(request.getPriority(), null)
        );
        return toResponse(todo);
    }

    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = getTodo(userId, todoId);
        todoRepository.delete(todo);
    }

    private Todo getTodo(Long userId, Long todoId) {
        return todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found."));
    }

    private Meeting getMeetingOrNull(Long userId, Long meetingId) {
        if (meetingId == null || meetingId <= 0) {
            return null;
        }
        return meetingRepository.findByIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found."));
    }

    private User getUser(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private String resolveSourceTitle(String sourceTitle, Meeting meeting) {
        if (sourceTitle != null && !sourceTitle.isBlank()) {
            return sourceTitle;
        }
        return meeting != null ? meeting.getTitle() : null;
    }

    private boolean matchesQuery(Todo todo, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String lower = q.toLowerCase();
        return contains(todo.getTitle(), lower)
                || contains(todo.getSourceTitle(), lower)
                || contains(todo.getCreatedByName(), lower)
                || contains(todo.getAssigneeName(), lower);
    }

    private boolean contains(String source, String lower) {
        return source != null && source.toLowerCase().contains(lower);
    }

    private boolean matchesStatus(Todo todo, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return todo.getStatus().name().equalsIgnoreCase(status);
    }

    private boolean matchesPriority(Todo todo, String priority) {
        if (priority == null || priority.isBlank()) {
            return true;
        }
        return todo.getPriority().name().equalsIgnoreCase(priority);
    }

    private boolean matchesAssignee(Todo todo, String assigneeName) {
        if (assigneeName == null || assigneeName.isBlank()) {
            return true;
        }
        return todo.getAssigneeName() != null && todo.getAssigneeName().equalsIgnoreCase(assigneeName);
    }

    private Comparator<Todo> resolveComparator(String sortBy) {
        if ("priority".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingInt(todo -> priorityRank(todo.getPriority()));
        }
        if ("createdAt".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Todo::getCreatedAt).reversed();
        }
        return Comparator.comparing(todo -> todo.getDueDate() != null ? todo.getDueDate() : LocalDate.MAX);
    }

    private int priorityRank(TodoPriority priority) {
        return switch (priority) {
            case URGENT -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    private LocalDate parseDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(dueDate);
    }

    private TodoStatus parseStatus(String value, TodoStatus defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return TodoStatus.valueOf(value.toUpperCase());
    }

    private TodoPriority parsePriority(String value, TodoPriority defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return TodoPriority.valueOf(value.toUpperCase());
    }

    private TodoItemResponse toResponse(Todo todo) {
        return new TodoItemResponse(
                todo.getId(),
                todo.getMeeting() != null ? todo.getMeeting().getId() : null,
                todo.getTitle(),
                todo.getSourceTitle(),
                todo.getCreatedByName(),
                todo.getAssigneeName(),
                todo.getDueDate() != null ? todo.getDueDate().toString() : null,
                todo.getStatus().name(),
                todo.getPriority().name()
        );
    }
}
