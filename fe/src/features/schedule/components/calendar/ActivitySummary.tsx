// features/schedule/components/calendar/ActivitySummary.tsx
"use client";

import React, { useEffect, useState } from "react";
import { scheduleService } from "@/features/schedule/services/scheduleService";
import { ScheduleType, ScheduleStatus } from "@/features/schedule/types/Enums";
import { scheduleStatusLabels } from "@/features/schedule/types/scheduleLabels";
import { FooterCard } from "@/features/schedule/components/ui/FooterCard";

type Counts = Record<ScheduleStatus, number>;

interface Props {
  view?: "monthly" | "weekly" | "daily";
  scheduleType?: ScheduleType | "all";
  refreshKey?: any;
  date?: Date;
}

export default function ActivitySummary({
  view = "monthly",
  scheduleType = "all",
  date,
  refreshKey,
}: Props) {
  const [counts, setCounts] = useState<Counts>({} as Counts);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const base = date ?? new Date(); // 오늘 대신 넘겨받은 date 사용
        const queryDate =
          view === "monthly"
            ? new Date(base.getFullYear(), base.getMonth(), 1)
            : base;

        let events;
        switch (view) {
          case "monthly":
            events = await scheduleService.getMonthlySchedules(
              queryDate,
              scheduleType === "all" ? undefined : scheduleType
            );
            break;
          case "weekly":
            events = await scheduleService.getWeeklyView(
              queryDate,
              scheduleType === "all" ? undefined : scheduleType
            );
            break;
          default:
            events = await scheduleService.getDailyView(
              queryDate,
              scheduleType === "all" ? undefined : scheduleType
            );
        }

        const c: Counts = {} as Counts;
        Object.values(ScheduleStatus).forEach((s) => (c[s] = 0));
        events.forEach((e) => {
          const s = e.scheduleStatus as ScheduleStatus;
          c[s] = (c[s] || 0) + 1;
        });
        setCounts(c);
      } catch (e) {
        console.error("활동기록 불러오기 실패", e);
        setError("활동기록을 가져오지 못했습니다.");
      }
    }
    load();
  }, [view, date, scheduleType, refreshKey]);
  const title = `활동 기록 (${view})`;

  if (error) {
    return (
      <FooterCard
        title={title}
        count={0}
        className="bg-red-50 border-red-200"
      />
    );
  }

  // 원하는 4가지 상태만 표시 (회의, 중요회의, 휴가, 출장)
  const selectedStatuses = [
    ScheduleStatus.MEETING,
    ScheduleStatus.IMPORTANT_MEETING,
    ScheduleStatus.VACATION,
    ScheduleStatus.BUSINESS_TRIP,
  ];

  return (
    <div className="bg-white rounded-xl">
      <h3 className="text-lg font-semibold text-gray-800 mb-3 px-1">
        일정 요약
      </h3>
      <div className="grid grid-cols-4 gap-3">
        {selectedStatuses.map((status) => (
          <FooterCard
            key={status}
            title={scheduleStatusLabels[status]}
            count={counts[status] ?? 0}
            status={status}
          />
        ))}
      </div>
    </div>
  );
}
