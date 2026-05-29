package com.acta.springserver.domain.todo.repository;

import com.acta.springserver.domain.todo.entity.Todo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findAllByOrderByCreatedAtDesc();

    List<Todo> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Todo> findByIdAndUserId(Long id, Long userId);
}
