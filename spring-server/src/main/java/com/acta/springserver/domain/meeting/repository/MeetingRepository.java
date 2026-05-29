package com.acta.springserver.domain.meeting.repository;

import com.acta.springserver.domain.meeting.entity.Meeting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByUserId(Long userId);

    Optional<Meeting> findByIdAndUserId(Long id, Long userId);
}
