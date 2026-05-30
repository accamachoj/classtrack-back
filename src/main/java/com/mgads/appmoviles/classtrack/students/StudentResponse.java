package com.mgads.appmoviles.classtrack.students;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String fullName;
    private String studentCode;
}
