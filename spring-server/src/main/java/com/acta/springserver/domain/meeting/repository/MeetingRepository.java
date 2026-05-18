package com.acta.springserver.domain.meeting.repository;

import com.acta.springserver.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}

