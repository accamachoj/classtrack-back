package com.mgads.appmoviles.classtrack.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CheckinResponse {
    private Long attendanceId;
    private LocalDateTime registeredAt;
}
