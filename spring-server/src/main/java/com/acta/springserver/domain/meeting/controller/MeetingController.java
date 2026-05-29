package com.acta.springserver.domain.meeting.controller;

import com.acta.springserver.domain.meeting.dto.request.MeetingCreateRequest;
import com.acta.springserver.domain.meeting.dto.request.MeetingUpdateRequest;
import com.acta.springserver.domain.meeting.dto.request.TranscriptUpdateRequest;
import com.acta.springserver.domain.meeting.dto.response.MeetingDetailResponse;
import com.acta.springserver.domain.meeting.dto.response.MeetingListResponse;
import com.acta.springserver.domain.meeting.dto.response.TranscriptItemResponse;
import com.acta.springserver.domain.meeting.service.MeetingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    public ResponseEntity<List<MeetingListResponse>> getMeetings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(meetingService.getMeetings(q, status));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingDetailResponse> getMeetingDetail(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getMeetingDetail(meetingId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MeetingDetailResponse> createMeeting(@ModelAttribute MeetingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(request));
    }

    @PatchMapping("/{meetingId}")
    public ResponseEntity<MeetingDetailResponse> updateMeeting(
            @PathVariable Long meetingId,
            @RequestBody MeetingUpdateRequest request
    ) {
        return ResponseEntity.ok(meetingService.updateMeeting(meetingId, request));
    }

    @PatchMapping("/{meetingId}/transcripts/{segmentId}")
    public ResponseEntity<TranscriptItemResponse> updateTranscript(
            @PathVariable Long meetingId,
            @PathVariable Long segmentId,
            @RequestBody TranscriptUpdateRequest request
    ) {
        return ResponseEntity.ok(meetingService.updateTranscript(meetingId, segmentId, request));
    }
}

