package com.mgads.appmoviles.classtrack.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SessionResponse {
    private Long sessionId;
    private Long courseId;
    private String qrToken;
    private LocalDateTime expiresAt;
}
