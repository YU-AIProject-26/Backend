package com.acta.springserver.domain.todo.controller;

import com.acta.springserver.domain.todo.dto.request.TodoCreateRequest;
import com.acta.springserver.domain.todo.dto.request.TodoUpdateRequest;
import com.acta.springserver.domain.todo.dto.response.TodoItemResponse;
import com.acta.springserver.domain.todo.dto.response.TodoListResponse;
import com.acta.springserver.domain.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public ResponseEntity<TodoListResponse> getTodos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assigneeName,
            @RequestParam(defaultValue = "dueDate") String sortBy
    ) {
        return ResponseEntity.ok(todoService.getTodos(q, status, priority, assigneeName, sortBy));
    }

    @PostMapping
    public ResponseEntity<TodoItemResponse> createTodo(@RequestBody TodoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createTodo(request));
    }

    @PatchMapping("/{todoId}")
    public ResponseEntity<TodoItemResponse> updateTodo(
            @PathVariable Long todoId,
            @RequestBody TodoUpdateRequest request
    ) {
        return ResponseEntity.ok(todoService.updateTodo(todoId, request));
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long todoId) {
        todoService.deleteTodo(todoId);
        return ResponseEntity.noContent().build();
    }
}
