package com.signagetv.controller;

import com.signagetv.dto.ScheduleDto;
import com.signagetv.dto.ScheduleRequest;
import com.signagetv.security.SecurityUtils;
import com.signagetv.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleDto> list() {
        return scheduleService.list(SecurityUtils.currentLocalId());
    }

    @PostMapping
    public ScheduleDto create(@Valid @RequestBody ScheduleRequest req) {
        return scheduleService.create(SecurityUtils.currentLocalId(), req);
    }

    @PutMapping("/{id}")
    public ScheduleDto update(@PathVariable Long id, @RequestBody ScheduleRequest req) {
        return scheduleService.update(SecurityUtils.currentLocalId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(SecurityUtils.currentLocalId(), id);
        return ResponseEntity.noContent().build();
    }
}
