package com.mgads.appmoviles.classtrack.courses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseListResponse {
    private Long id;
    private String name;
    private long studentCount;
}
