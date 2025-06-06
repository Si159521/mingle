package com.example.mingle.domain.schedule.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleType {
    PERSONAL("개인 일정"),
    DEPARTMENT("부서 일정"),
    COMPANY("회사 공통 일정");

    private final String displayName;

}
