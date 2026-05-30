package com.mgads.appmoviles.classtrack.users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DigitalIdResponse {
    private Long userId;
    private String fullName;
    private String studentCode;
    private String qrContent;
}
