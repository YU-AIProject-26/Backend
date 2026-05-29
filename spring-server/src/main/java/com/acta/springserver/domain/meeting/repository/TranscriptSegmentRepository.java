package com.acta.springserver.domain.meeting.repository;

import com.acta.springserver.domain.meeting.entity.TranscriptSegment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {

    List<TranscriptSegment> findByMeetingIdOrderByStartedAtSecondsAsc(Long meetingId);

    Optional<TranscriptSegment> findByIdAndMeetingId(Long id, Long meetingId);
}

