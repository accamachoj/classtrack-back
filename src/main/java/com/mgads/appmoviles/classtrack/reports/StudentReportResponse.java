package com.mgads.appmoviles.classtrack.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentReportResponse {
    private Long studentId;
    private String studentName;
    private double attendancePercentage;
    private long sessionsAttended;
}
