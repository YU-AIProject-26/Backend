package com.acta.springserver.domain.meeting.service;

import com.acta.springserver.domain.meeting.dto.request.MeetingCreateRequest;
import com.acta.springserver.domain.meeting.dto.request.MeetingUpdateRequest;
import com.acta.springserver.domain.meeting.dto.request.TranscriptUpdateRequest;
import com.acta.springserver.domain.meeting.dto.response.FocusMetricResponse;
import com.acta.springserver.domain.meeting.dto.response.MeetingDetailResponse;
import com.acta.springserver.domain.meeting.dto.response.MeetingListResponse;
import com.acta.springserver.domain.meeting.dto.response.ParticipationItemResponse;
import com.acta.springserver.domain.meeting.dto.response.ScheduleItemResponse;
import com.acta.springserver.domain.meeting.dto.response.TranscriptItemResponse;
import com.acta.springserver.domain.meeting.entity.Meeting;
import com.acta.springserver.domain.meeting.entity.MeetingStatus;
import com.acta.springserver.domain.meeting.entity.TranscriptSegment;
import com.acta.springserver.domain.meeting.repository.MeetingRepository;
import com.acta.springserver.domain.meeting.repository.TranscriptSegmentRepository;
import com.acta.springserver.domain.schedule.dto.response.ScheduleResponse;
import com.acta.springserver.domain.schedule.service.ScheduleService;
import com.acta.springserver.domain.todo.dto.response.TodoItemResponse;
import com.acta.springserver.domain.todo.entity.Todo;
import com.acta.springserver.domain.user.entity.User;
import com.acta.springserver.domain.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final ScheduleService scheduleService;
    private final UserRepository userRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public List<MeetingListResponse> getMeetings(Long userId, String q, String status) {
        return meetingRepository.findAllByUserId(userId).stream()
                .filter(meeting -> matchesQuery(meeting, q))
                .filter(meeting -> matchesStatus(meeting, status))
                .sorted(Comparator.comparing(Meeting::getMeetingAt).reversed())
                .map(this::toMeetingListResponse)
                .toList();
    }

    public MeetingDetailResponse getMeetingDetail(Long userId, Long meetingId) {
        Meeting meeting = getMeeting(userId, meetingId);
        return toMeetingDetailResponse(userId, meeting);
    }

    @Transactional
    public MeetingDetailResponse createMeeting(Long userId, MeetingCreateRequest request) {
        User user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String audioPath = saveFile(request.getAudioFile());

        Meeting meeting = Meeting.builder()
                .user(user)
                .title(resolveTitle(request.getTitle(), request.getAudioFile()))
                .description(request.getDescription())
                .oneLineSummary(audioPath != null ? "Upload completed. Waiting for analysis." : request.getDescription())
                .meetingAt(parseDateTimeOrNow(request.getMeetingAt()))
                .durationMinutes(defaultIfNull(request.getDurationMinutes(), 0))
                .participantCount(defaultIfNull(request.getParticipantCount(), 0))
                .folderName(request.getFolderName())
                .tags(request.getTags())
                .status(audioPath != null ? MeetingStatus.PROCESSING : MeetingStatus.UPLOADED)
                .audioPath(audioPath)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Meeting saved = meetingRepository.save(meeting);
        ensureDefaultTranscriptSegments(saved);
        return toMeetingDetailResponse(userId, saved);
    }

    @Transactional
    public MeetingDetailResponse updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
        Meeting meeting = getMeeting(userId, meetingId);
        meeting.update(
                request.getTitle(),
                request.getDescription(),
                request.getOneLineSummary(),
                parseDateTimeOrNull(request.getMeetingAt()),
                request.getDurationMinutes(),
                request.getParticipantCount(),
                request.getFolderName(),
                request.getTags(),
                parseStatus(request.getStatus())
        );
        return toMeetingDetailResponse(userId, meeting);
    }

    @Transactional
    public TranscriptItemResponse updateTranscript(Long userId, Long meetingId, Long segmentId, TranscriptUpdateRequest request) {
        getMeeting(userId, meetingId);
        TranscriptSegment segment = transcriptSegmentRepository.findByIdAndMeetingId(segmentId, meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript segment not found."));

        segment.update(request.getSpeakerName(), request.getText(), request.getKeyStatement());
        return toTranscriptItemResponse(segment);
    }

    @Transactional
    public void deleteMeeting(Long userId, Long meetingId) {
        Meeting meeting = getMeeting(userId, meetingId);
        String audioPath = meeting.getAudioPath();

        meetingRepository.delete(meeting);
        deleteStoredAudioFile(audioPath);
    }

    private Meeting getMeeting(Long userId, Long meetingId) {
        return meetingRepository.findByIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found."));
    }

    private User getUser(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private boolean matchesQuery(Meeting meeting, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String lower = q.toLowerCase();
        return contains(meeting.getTitle(), lower)
                || contains(meeting.getDescription(), lower)
                || contains(meeting.getOneLineSummary(), lower);
    }

    private boolean matchesStatus(Meeting meeting, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return meeting.getStatus().name().equalsIgnoreCase(status);
    }

    private boolean contains(String source, String lower) {
        return source != null && source.toLowerCase().contains(lower);
    }

    private MeetingListResponse toMeetingListResponse(Meeting meeting) {
        return new MeetingListResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getOneLineSummary(),
                meeting.getMeetingAt(),
                meeting.getDurationMinutes(),
                meeting.getFolderName(),
                meeting.getStatus().name(),
                splitTags(meeting.getTags()),
                buildAudioUrl(meeting.getAudioPath())
        );
    }

    private MeetingDetailResponse toMeetingDetailResponse(Long userId, Meeting meeting) {
        List<TodoItemResponse> todos = meeting.getTodos().stream()
                .sorted(Comparator.comparing(todo -> todo.getDueDate() != null ? todo.getDueDate() : LocalDate.MAX))
                .map(this::toTodoItemResponse)
                .toList();

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getOneLineSummary(),
                meeting.getMeetingAt(),
                meeting.getDurationMinutes(),
                meeting.getParticipantCount(),
                meeting.getFolderName(),
                meeting.getStatus().name(),
                splitTags(meeting.getTags()),
                buildAudioUrl(meeting.getAudioPath()),
                meeting.getSummary() != null ? meeting.getSummary() : defaultSummary(),
                meeting.getEfficiencyScore() != null ? meeting.getEfficiencyScore() : 82,
                meeting.getEfficiencyFeedback() != null ? meeting.getEfficiencyFeedback() : defaultEfficiencyFeedback(),
                defaultKeyPoints(),
                buildTranscriptResponses(meeting.getId()),
                defaultParticipationStats(),
                defaultFocusMetrics(),
                todos,
                buildScheduleResponses(userId, meeting.getId())
        );
    }

    private TodoItemResponse toTodoItemResponse(Todo todo) {
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

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private String resolveTitle(String title, MultipartFile audioFile) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (audioFile != null && audioFile.getOriginalFilename() != null) {
            String filename = audioFile.getOriginalFilename();
            int extensionIndex = filename.lastIndexOf('.');
            return extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;
        }
        return "new_meeting";
    }

    private Integer defaultIfNull(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }

    private LocalDateTime parseDateTimeOrNow(String value) {
        LocalDateTime parsed = parseDateTimeOrNull(value);
        return parsed != null ? parsed : LocalDateTime.now();
    }

    private LocalDateTime parseDateTimeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }
        return LocalDateTime.parse(value);
    }

    private MeetingStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return MeetingStatus.valueOf(status.toUpperCase());
    }

    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.wav";
            String storedName = UUID.randomUUID() + "_" + originalName;
            Path target = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save audio file.", exception);
        }
    }

    private String buildAudioUrl(String audioPath) {
        if (audioPath == null || audioPath.isBlank()) {
            return null;
        }
        return "/uploads/" + Paths.get(audioPath).getFileName();
    }

    private void deleteStoredAudioFile(String audioPath) {
        if (audioPath == null || audioPath.isBlank()) {
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path target = Paths.get(audioPath).toAbsolutePath().normalize();
            if (target.startsWith(uploadPath)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // Meeting deletion should not fail just because an uploaded file is already missing or locked.
        }
    }

    private void ensureDefaultTranscriptSegments(Meeting meeting) {
        if (!transcriptSegmentRepository.findByMeetingIdOrderByStartedAtSecondsAsc(meeting.getId()).isEmpty()) {
            return;
        }

        transcriptSegmentRepository.saveAll(List.of(
                TranscriptSegment.builder()
                        .meeting(meeting)
                        .speakerName("Speaker A")
                        .startedAtSeconds(135)
                        .text("Hello. We will start today's meeting now.")
                        .keyStatement(true)
                        .build(),
                TranscriptSegment.builder()
                        .meeting(meeting)
                        .speakerName("Speaker B")
                        .startedAtSeconds(332)
                        .text("Last quarter data shows strong digital marketing performance.")
                        .keyStatement(false)
                        .build(),
                TranscriptSegment.builder()
                        .meeting(meeting)
                        .speakerName("Speaker C")
                        .startedAtSeconds(527)
                        .text("Let's review whether we should allocate more budget to digital channels.")
                        .keyStatement(true)
                        .build()
        ));
    }

    private List<TranscriptItemResponse> buildTranscriptResponses(Long meetingId) {
        List<TranscriptSegment> segments = transcriptSegmentRepository.findByMeetingIdOrderByStartedAtSecondsAsc(meetingId);
        if (segments.isEmpty()) {
            return defaultTranscripts();
        }
        return segments.stream()
                .map(this::toTranscriptItemResponse)
                .toList();
    }

    private TranscriptItemResponse toTranscriptItemResponse(TranscriptSegment segment) {
        return new TranscriptItemResponse(
                segment.getSpeakerName(),
                segment.getStartedAtSeconds(),
                segment.getText(),
                segment.isKeyStatement()
        );
    }

    private String defaultSummary() {
        return "Q2 marketing campaign strategy and budget discussion were completed. This response is a temporary placeholder until the AI analysis server is connected.";
    }

    private String defaultEfficiencyFeedback() {
        return "This is temporary mock analysis data. Replace it with AI-generated feedback after integration.";
    }

    private List<String> defaultKeyPoints() {
        return List.of(
                "Q2 target: 5,000 new users and 20% MAU growth",
                "Digital marketing budget allocation reviewed",
                "Influencer campaign launch schedule discussed",
                "Customer retention improvement item added"
        );
    }

    private List<TranscriptItemResponse> defaultTranscripts() {
        return List.of(
                new TranscriptItemResponse("Speaker A", 135, "Hello. We will start today's meeting now.", true),
                new TranscriptItemResponse("Speaker B", 332, "Last quarter data shows strong digital marketing performance.", false),
                new TranscriptItemResponse("Speaker C", 527, "Let's review whether we should allocate more budget to digital channels.", true)
        );
    }

    private List<ParticipationItemResponse> defaultParticipationStats() {
        return List.of(
                new ParticipationItemResponse("Speaker A", 30),
                new ParticipationItemResponse("Speaker B", 25),
                new ParticipationItemResponse("Speaker C", 20),
                new ParticipationItemResponse("Speaker D", 15),
                new ParticipationItemResponse("Speaker E", 10)
        );
    }

    private List<FocusMetricResponse> defaultFocusMetrics() {
        return List.of(
                new FocusMetricResponse("00:00-15:00", 74),
                new FocusMetricResponse("15:00-30:00", 88),
                new FocusMetricResponse("30:00-45:00", 79),
                new FocusMetricResponse("45:00-60:00", 85)
        );
    }

    private List<ScheduleItemResponse> buildScheduleResponses(Long userId, Long meetingId) {
        List<ScheduleResponse> schedules = scheduleService.getMeetingSchedules(userId, meetingId);
        if (schedules.isEmpty()) {
            return defaultSchedules();
        }
        return schedules.stream()
                .map(item -> new ScheduleItemResponse(
                        item.title(),
                        item.startsAt(),
                        item.participantCount() != null ? "Participants: " + item.participantCount() : null
                ))
                .toList();
    }

    private List<ScheduleItemResponse> defaultSchedules() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return List.of(
                new ScheduleItemResponse("Influencer kickoff meeting", formatter.format(LocalDateTime.now().plusDays(3)), "Participants: 2"),
                new ScheduleItemResponse("Q2 campaign final review", formatter.format(LocalDateTime.now().plusDays(7)), "Participants: 5"),
                new ScheduleItemResponse("Weekly marketing check-in", formatter.format(LocalDateTime.now().plusDays(10)), "Participants: 4")
        );
    }
}
