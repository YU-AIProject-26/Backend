package com.acta.springserver.domain.schedule.entity;

import com.acta.springserver.domain.meeting.entity.Meeting;
import com.acta.springserver.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(length = 255)
    private String location;

    private Integer participantCount;

    @Builder
    public Schedule(
            User user,
            Meeting meeting,
            String title,
            LocalDateTime startsAt,
            String location,
            Integer participantCount
    ) {
        this.user = user;
        this.meeting = meeting;
        this.title = title;
        this.startsAt = startsAt;
        this.location = location;
        this.participantCount = participantCount;
    }
}
