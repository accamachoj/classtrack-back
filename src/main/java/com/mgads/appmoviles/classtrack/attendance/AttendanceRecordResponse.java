package com.mgads.appmoviles.classtrack.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AttendanceRecordResponse {
    private Long studentId;
    private String studentName;
    private LocalDateTime registeredAt;
}
