package com.example.mingle.domain.schedule.controller;

import com.example.mingle.domain.schedule.dto.ScheduleRequest;
import com.example.mingle.domain.schedule.dto.ScheduleResponse;
import com.example.mingle.domain.schedule.entity.ScheduleStatus;
import com.example.mingle.domain.schedule.entity.ScheduleType;
import com.example.mingle.domain.schedule.service.ScheduleService;
import com.example.mingle.domain.user.team.dto.DepartmentResponse;
import com.example.mingle.domain.user.team.repository.DepartmentRepository;
import com.example.mingle.domain.user.user.entity.UserRole;
import com.example.mingle.global.exception.ApiException;
import com.example.mingle.global.exception.ErrorCode;
import com.example.mingle.global.rq.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedule")
@Tag(name = "Schedule", description = "일정 관리 API")
public class ApiV1ScheduleController {

    private final ScheduleService scheduleService;
    private final Rq rq;
    private final DepartmentRepository departmentRepository;

    // 개인 일정 생성
    @Operation(summary = "개인 일정 생성", description = "새 일정을 생성하고 생성된 일정을 반환합니다.")
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Parameter(description = "생성할 일정 정보", required = true)
            @Valid @RequestBody ScheduleRequest request
    ) {
        Long userId = rq.getActor().getId();
        ScheduleResponse response = scheduleService.createSchedule(request, userId, request.getPostId());
        return ResponseEntity.ok(response);
    }

    // 부서 일정 생성
    @Operation(summary = "부서 일정 생성",
            description = "해당 유저의 부서 일정을 생성합니다.")
    @PostMapping("/department")
    public ResponseEntity<ScheduleResponse> createDepartmentSchedule(
            @RequestBody ScheduleRequest request
    ) {
        Long userId = rq.getActor().getId();
        ScheduleResponse departmentSchedule = scheduleService.createDepartmentSchedule(request, userId, request.getPostId());
        return ResponseEntity.ok(departmentSchedule);
    }

    // 단건 조회
    @Operation(summary = "일정 단건 조회",
            description = "스케줄 ID로 단건 일정을 가져옵니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @PathVariable Long scheduleId
    ) {
        Long userId = rq.getActor().getId();
        ScheduleResponse response = scheduleService.getScheduleById(userId, scheduleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ScheduleResponse>> searchSchedules(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "memo", defaultValue = "false") boolean memo
    ) {
        Long userId = rq.getActor().getId();

        Long deptId = rq.getActor().getDepartment().getId();

        List<ScheduleResponse> results =
                scheduleService.searchVisibleSchedules(userId, deptId, keyword, memo);

        return ResponseEntity.ok(results);
    }

    // 상태별 일정 조회
    @Operation(summary = "상태별 일정 조회", description = "특정 상태의 일정을 조회합니다.")
    @GetMapping("/status")
    public ResponseEntity<Page<ScheduleResponse>> getSchedulesByStatus(
            @Parameter(description = "조회할 일정 상태", example = "MEETING, IMPORTANT_MEETING, VACATION")
            @RequestParam ScheduleStatus status,
            @Parameter(description = "일정 타입", example = "PERSONAL, COMPANY, DEPARTMENT")
            @RequestParam ScheduleType scheduleType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Long userId = rq.getActor().getId();
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by("startTime").descending());
        Page<ScheduleResponse> responses = scheduleService.getSchedulesByStatus(userId, status, scheduleType, pageable);
        return ResponseEntity.ok(responses);
    }

    // 월간 일정 조회
    @Operation(summary = "월간 일정 조회", description = "특정 달의 일정을 조회합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<List<ScheduleResponse>> getMonthlyView(
            @Parameter(description = "일정 타입", example = "DEPARTMENT, COMPANY, PERSONAL")
            @RequestParam(required = false) ScheduleType type,
            @Parameter(description = "조회 기준 날짜(ISO)", example = "2025-05-13")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId
    ) {
        Long userId = rq.getActor().getId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ScheduleResponse> responses = scheduleService.getMonthlyView(userId, type, targetDate, departmentId);
        return ResponseEntity.ok(responses);
    }

    // 주간 일정 조회
    @Operation(summary = "주간 일정 조회", description = "특정 주의 일정을 조회합니다.")
    @GetMapping("/weekly")
    public ResponseEntity<List<ScheduleResponse>> getWeeklyView(
            @Parameter(description = "일정 타입", example = "DEPARTMENT, COMPANY, PERSONAL")
            @RequestParam(required = false) ScheduleType type,
            @Parameter(description = "조회 기준 날짜(ISO)", example = "2025-05-13")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId
    ) {
        Long userId = rq.getActor().getId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ScheduleResponse> responses = scheduleService.getWeeklyView(userId, targetDate, type, departmentId);
        return ResponseEntity.ok(responses);
    }

    // 일간 일정 조회
    @Operation(summary = "일간 일정 조회", description = "특정 일의 일정을 조회합니다.")
    @GetMapping("/daily")
    public ResponseEntity<List<ScheduleResponse>> getDailyView(
            @Parameter(description = "일정 타입", example = "DEPARTMENT, COMPANY, PERSONAL")
            @RequestParam(required = false) ScheduleType type,
            @Parameter(description = "조회 기준 날짜(ISO)", example = "2025-05-13")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId
    ) {
        Long userId = rq.getActor().getId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ScheduleResponse> responses = scheduleService.getDailyView(userId, targetDate, type, departmentId);
        return ResponseEntity.ok(responses);
    }

    // 일정 수정
    @PutMapping("/{scheduleId}")
    @Operation(summary = "일정 수정", description = "기존 일정을 수정하고 수정된 일정을 반환합니다.")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @Parameter(description = "수정할 일정 ID", example = "1")
            @PathVariable(name = "scheduleId") Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        Long userId = rq.getActor().getId();
        ScheduleResponse response = scheduleService.updateSchedule(request, scheduleId, userId);
        return ResponseEntity.ok(response);
    }

    // 일정 삭제
    @Operation(summary = "일정 삭제", description = "지정한 일정을 삭제합니다.")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "삭제할 일정 ID", example = "1")
            @PathVariable(name = "scheduleId") Long scheduleId

    ) {
        Long userId = rq.getActor().getId();
        scheduleService.deleteSchedule(scheduleId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 관리자 전용 API =====================

    // 회사 일정 생성
    @Operation(summary = "관리자 전용 회사 일정 생성",
            description = "ADMIN 권한으로 회사 공통 일정을 생성합니다.")
    @PostMapping("/admin/company")
    public ResponseEntity<ScheduleResponse> createCompanySchedule(
            @RequestBody ScheduleRequest request
    ) {
        if (!UserRole.ADMIN.equals(rq.getActor().getRole())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        Long userId = rq.getActor().getId();
        ScheduleResponse companySchedule = scheduleService.createCompanySchedule(request, userId, request.getPostId());
        return ResponseEntity.ok(companySchedule);
    }

    // 모든 일정 수정 (관리자 전용)
    @Operation(summary = "관리자 전용 일정 수정",
            description = "ADMIN 권한으로 부서 일정을 수정합니다.")
    @PutMapping("/admin/{scheduleId}")
    public ResponseEntity<ScheduleResponse> updateScheduleAdmin(
            @PathVariable(name = "scheduleId") Long scheduleId,
            @RequestBody ScheduleRequest request
    ) {
        if (!UserRole.ADMIN.equals(rq.getActor().getRole())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        Long userId = rq.getActor().getId();
        ScheduleResponse scheduleResponse = scheduleService.updateAnySchedule(request, scheduleId, userId);
        return ResponseEntity.ok(scheduleResponse);
    }

    // 부서 목록 가져오기
    @Operation(summary = "전체 부서 목록 가져오기", description = "관리자가 팀 일정을 생성할 때 드롭다운에 노출할 부서 목록을 반환합니다.")
    @GetMapping("/departments")
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAll().stream()
                .map(dept -> new DepartmentResponse(
                        dept.getId(),
                        dept.getDepartmentName()))
                .collect(Collectors.toList());
    }
}


