package com.mgads.appmoviles.classtrack.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseReportResponse {
    private Long courseId;
    private String courseName;
    private long totalStudents;
    private double attendanceRate;
}
