package com.acta.springserver.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "transcript_segments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false, length = 100)
    private String speakerName;

    @Column(nullable = false)
    private Integer startedAtSeconds;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private boolean keyStatement;

    @Builder
    public TranscriptSegment(
            Meeting meeting,
            String speakerName,
            Integer startedAtSeconds,
            String text,
            boolean keyStatement
    ) {
        this.meeting = meeting;
        this.speakerName = speakerName;
        this.startedAtSeconds = startedAtSeconds;
        this.text = text;
        this.keyStatement = keyStatement;
    }

    public void update(String speakerName, String text, Boolean keyStatement) {
        if (speakerName != null) {
            this.speakerName = speakerName;
        }
        if (text != null) {
            this.text = text;
        }
        if (keyStatement != null) {
            this.keyStatement = keyStatement;
        }
    }
}

