package com.acta.springserver.domain.meeting.entity;

import com.acta.springserver.domain.schedule.entity.Schedule;
import com.acta.springserver.domain.todo.entity.Todo;
import com.acta.springserver.domain.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "meetings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String oneLineSummary;

    @Column(nullable = false)
    private LocalDateTime meetingAt;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer participantCount;

    @Column(length = 100)
    private String folderName;

    @Column(length = 255)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    @Column(length = 500)
    private String audioPath;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private Integer efficiencyScore;

    @Column(columnDefinition = "TEXT")
    private String efficiencyFeedback;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Todo> todos = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TranscriptSegment> transcripts = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    @Builder
    public Meeting(
            User user,
            String title,
            String description,
            String oneLineSummary,
            LocalDateTime meetingAt,
            Integer durationMinutes,
            Integer participantCount,
            String folderName,
            String tags,
            MeetingStatus status,
            String audioPath,
            String summary,
            Integer efficiencyScore,
            String efficiencyFeedback,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.oneLineSummary = oneLineSummary;
        this.meetingAt = meetingAt;
        this.durationMinutes = durationMinutes;
        this.participantCount = participantCount;
        this.folderName = folderName;
        this.tags = tags;
        this.status = status;
        this.audioPath = audioPath;
        this.summary = summary;
        this.efficiencyScore = efficiencyScore;
        this.efficiencyFeedback = efficiencyFeedback;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            String title,
            String description,
            String oneLineSummary,
            LocalDateTime meetingAt,
            Integer durationMinutes,
            Integer participantCount,
            String folderName,
            String tags,
            MeetingStatus status
    ) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (oneLineSummary != null) {
            this.oneLineSummary = oneLineSummary;
        }
        if (meetingAt != null) {
            this.meetingAt = meetingAt;
        }
        if (durationMinutes != null) {
            this.durationMinutes = durationMinutes;
        }
        if (participantCount != null) {
            this.participantCount = participantCount;
        }
        if (folderName != null) {
            this.folderName = folderName;
        }
        if (tags != null) {
            this.tags = tags;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
