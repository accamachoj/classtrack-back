package com.mgads.appmoviles.classtrack.auth;

import com.mgads.appmoviles.classtrack.users.UserEntity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "fullName is required")
    @Size(max = 150, message = "fullName must not exceed 150 characters")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    @NotNull(message = "role is required")
    private Role role;

    private String studentCode;
}
