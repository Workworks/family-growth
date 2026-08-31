package com.familygrowth.web;

import com.familygrowth.application.Stage9Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage9Models.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}")
class Stage9Controller {
    private final Stage9Service service;
    Stage9Controller(Stage9Service service) { this.service = service; }

    @PutMapping("/usage-policy")
    ApiResponse<UsagePolicy> policy(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @Valid @RequestBody PolicyRequest request) {
        return ApiResponse.ok(service.configure(actor, familyId, childId, request.zoneId(), request.dailyLimitMinutes(), request.sessionLimitMinutes(),request.restMinutes()==null?10:request.restMinutes(),request.quietStart()==null?LocalTime.of(21,30):request.quietStart(),request.quietEnd()==null?LocalTime.of(6,30):request.quietEnd()));
    }

    @GetMapping("/usage-policy")
    ApiResponse<UsagePolicy> policy(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId) {
        return ApiResponse.ok(service.policy(actor, familyId, childId));
    }

    @PostMapping("/usage-events")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<UsageEvent> event(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(service.addEvent(actor, familyId, childId, request.type(), request.minutes(), request.occurredAt(), key));
    }

    @GetMapping("/reports/today")
    ApiResponse<TodayReport> today(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(service.today(actor, familyId, childId, date));
    }

    @GetMapping("/reports/monthly")
    ApiResponse<MonthlyReport> monthly(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @RequestParam(required = false) YearMonth month) {
        return ApiResponse.ok(service.month(actor, familyId, childId, month));
    }

    @GetMapping("/usage-access")
    ApiResponse<UsageAccessState> access(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.access(actor,familyId,childId));}

    @PostMapping("/usage-allowances")
    ApiResponse<TemporaryAllowance> allowance(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AllowanceRequest request){return ApiResponse.ok(service.allow(actor,familyId,childId,request.minutes(),request.reason(),key));}

    record PolicyRequest(@NotBlank @Size(max = 60) String zoneId,
        @Min(10) @Max(480) int dailyLimitMinutes, @Min(5) @Max(240) int sessionLimitMinutes,@Min(5) @Max(60) Integer restMinutes,LocalTime quietStart,LocalTime quietEnd) {}
    record EventRequest(@NotNull UsageEventType type, @Min(1) @Max(60) int minutes, @NotNull Instant occurredAt) {}
    record AllowanceRequest(@Min(1) @Max(60) int minutes,@NotBlank @Size(max=240) String reason){}
}
