package com.acta.springserver.domain.schedule.controller;

import com.acta.springserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.acta.springserver.domain.schedule.dto.response.ScheduleListResponse;
import com.acta.springserver.domain.schedule.dto.response.ScheduleResponse;
import com.acta.springserver.domain.schedule.service.ScheduleService;
import com.acta.springserver.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ScheduleListResponse> getSchedules(Authentication authentication) {
        return ResponseEntity.ok(scheduleService.getSchedules(getUserId(authentication)));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            Authentication authentication,
            @RequestBody ScheduleCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(getUserId(authentication), request));
    }

    private Long getUserId(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        return principal.getUserId();
    }
}
