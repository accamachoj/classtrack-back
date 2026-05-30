package com.mgads.appmoviles.classtrack.students;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkStudentRequest {

    @NotNull(message = "studentId is required")
    private Long studentId;
}
