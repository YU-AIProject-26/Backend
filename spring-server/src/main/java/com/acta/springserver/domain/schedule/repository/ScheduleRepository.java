package com.acta.springserver.domain.schedule.repository;

import com.acta.springserver.domain.schedule.entity.Schedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findAllByOrderByStartsAtAsc();

    List<Schedule> findByMeetingIdOrderByStartsAtAsc(Long meetingId);

    List<Schedule> findAllByUserIdOrderByStartsAtAsc(Long userId);

    List<Schedule> findByMeetingIdAndUserIdOrderByStartsAtAsc(Long meetingId, Long userId);
}
