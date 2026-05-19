package com.acta.springserver.domain.schedule.controller;

import com.acta.springserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.acta.springserver.domain.schedule.dto.response.ScheduleListResponse;
import com.acta.springserver.domain.schedule.dto.response.ScheduleResponse;
import com.acta.springserver.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<ScheduleListResponse> getSchedules() {
        return ResponseEntity.ok(scheduleService.getSchedules());
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody ScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }
}
