package com.mgads.appmoviles.classtrack.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionRequest {

    @NotNull(message = "courseId is required")
    private Long courseId;
}
