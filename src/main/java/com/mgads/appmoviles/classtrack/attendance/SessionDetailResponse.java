package com.mgads.appmoviles.classtrack.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionDetailResponse {
    private Long sessionId;
    private Long courseId;
    private String status;
}
