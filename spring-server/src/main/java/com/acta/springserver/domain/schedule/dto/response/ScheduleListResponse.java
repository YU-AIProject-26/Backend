package com.acta.springserver.domain.schedule.dto.response;

import java.util.List;

public record ScheduleListResponse(
        List<ScheduleResponse> items
) {
}

