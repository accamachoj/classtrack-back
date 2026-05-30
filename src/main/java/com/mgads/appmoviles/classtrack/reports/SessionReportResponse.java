package com.mgads.appmoviles.classtrack.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionReportResponse {
    private Long sessionId;
    private long totalStudents;
    private long attendees;
    private long absent;
}
