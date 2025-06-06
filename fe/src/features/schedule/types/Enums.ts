export enum ScheduleType {
  PERSONAL = "PERSONAL",
  DEPARTMENT = "DEPARTMENT",
  COMPANY = "COMPANY",
}

export enum ScheduleStatus {
  NONE = "NONE", // 없음
  IMPORTANT_MEETING = "IMPORTANT_MEETING", // 중요회의
  BUSINESS_TRIP = "BUSINESS_TRIP", // 출장
  MEETING = "MEETING", // 회의
  COMPLETED = "COMPLETED", // 일정완료
  CANCELED = "CANCELED", // 일정취소
  VACATION = "VACATION", // 휴가
}
