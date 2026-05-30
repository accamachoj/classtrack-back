package com.mgads.appmoviles.classtrack.users;

import com.mgads.appmoviles.classtrack.common.ApiResponse;
import com.mgads.appmoviles.classtrack.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile and digital ID endpoints")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get current user profile")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        Long userId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(userId)));
    }

    @GetMapping("/digital-id")
    @Operation(summary = "Get digital QR identification data")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Digital ID retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<DigitalIdResponse>> getDigitalId() {
        Long userId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok(userService.getDigitalId(userId)));
    }
}
