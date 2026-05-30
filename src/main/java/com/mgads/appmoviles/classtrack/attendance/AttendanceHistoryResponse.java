package com.mgads.appmoviles.classtrack.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AttendanceHistoryResponse {
    private Long sessionId;
    private String courseName;
    private LocalDateTime registeredAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
