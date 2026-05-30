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

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance check-in and records endpoints")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    @PostMapping("/checkin")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Register student attendance with GPS coordinates")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Attendance registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Session inactive or QR expired"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Student not enrolled in course"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attendance already registered")
    })
    public ResponseEntity<ApiResponse<CheckinResponse>> checkin(
            @Valid @RequestBody CheckinRequest request) {
        Long studentId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        CheckinResponse data = attendanceService.checkin(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Attendance registered", data));
    }

    @GetMapping("/sessions/{sessionId}/records")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "List attendance records for a session")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Records listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not the session owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getRecords(
            @PathVariable Long sessionId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        List<AttendanceRecordResponse> data = attendanceService.getSessionRecords(sessionId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get attendance history for the authenticated student")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> getHistory() {
        Long studentId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        List<AttendanceHistoryResponse> data = attendanceService.getStudentHistory(studentId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
