package com.mgads.appmoviles.classtrack.attendance;

import com.mgads.appmoviles.classtrack.common.ApiResponse;
import com.mgads.appmoviles.classtrack.security.AuthUtils;
import com.mgads.appmoviles.classtrack.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance/sessions")
@RequiredArgsConstructor
@Tag(name = "Attendance Sessions", description = "Attendance session management endpoints")
public class AttendanceSessionController {

    private final AttendanceSessionService sessionService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create an attendance session and generate QR token")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session created with QR token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        SessionResponse data = sessionService.createSession(teacherId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Session created", data));
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get session detail")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSession(@PathVariable Long sessionId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        SessionDetailResponse data = sessionService.getSession(sessionId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/{sessionId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Close an active attendance session")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session closed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Session already closed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ApiResponse<Void>> closeSession(@PathVariable Long sessionId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        sessionService.closeSession(sessionId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok("Session closed"));
    }
}
