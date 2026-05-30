package com.mgads.appmoviles.classtrack.courses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String name;
    private String description;
    private long studentCount;
}
