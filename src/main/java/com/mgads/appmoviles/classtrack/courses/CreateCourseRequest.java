package com.mgads.appmoviles.classtrack.courses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    private String name;

    private String description;
}
