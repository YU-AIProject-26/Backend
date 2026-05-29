package com.acta.springserver.domain.schedule.service;

import com.acta.springserver.domain.meeting.entity.Meeting;
import com.acta.springserver.domain.meeting.repository.MeetingRepository;
import com.acta.springserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.acta.springserver.domain.schedule.dto.response.ScheduleListResponse;
import com.acta.springserver.domain.schedule.dto.response.ScheduleResponse;
import com.acta.springserver.domain.schedule.entity.Schedule;
import com.acta.springserver.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MeetingRepository meetingRepository;

    public ScheduleListResponse getSchedules() {
        List<ScheduleResponse> items = scheduleRepository.findAllByOrderByStartsAtAsc().stream()
                .map(this::toResponse)
                .toList();
        return new ScheduleListResponse(items);
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        Meeting meeting = null;
        if (request.getMeetingId() != null) {
            meeting = meetingRepository.findById(request.getMeetingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found."));
        }

        Schedule schedule = Schedule.builder()
                .meeting(meeting)
                .title(request.getTitle())
                .startsAt(LocalDateTime.parse(request.getStartsAt()))
                .location(request.getLocation())
                .participantCount(request.getParticipantCount())
                .build();

        return toResponse(scheduleRepository.save(schedule));
    }

    public List<ScheduleResponse> getMeetingSchedules(Long meetingId) {
        return scheduleRepository.findByMeetingIdOrderByStartsAtAsc(meetingId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getMeeting() != null ? schedule.getMeeting().getId() : null,
                schedule.getTitle(),
                schedule.getStartsAt().toString(),
                schedule.getLocation(),
                schedule.getParticipantCount()
        );
    }
}

