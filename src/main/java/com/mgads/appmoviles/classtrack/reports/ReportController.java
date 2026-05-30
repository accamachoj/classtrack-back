package com.mgads.appmoviles.classtrack.reports;

import com.mgads.appmoviles.classtrack.common.ApiResponse;
import com.mgads.appmoviles.classtrack.security.AuthUtils;
import com.mgads.appmoviles.classtrack.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Academic attendance report endpoints")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get attendance report for a session")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not the session owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<ApiResponse<SessionReportResponse>> getSessionReport(
            @PathVariable Long sessionId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSessionReport(sessionId, teacherId)));
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get attendance report for a course")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not the course owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<ApiResponse<CourseReportResponse>> getCourseReport(
            @PathVariable Long courseId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok(reportService.getCourseReport(courseId, teacherId)));
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get attendance report for a student")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    public ResponseEntity<ApiResponse<StudentReportResponse>> getStudentReport(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getStudentReport(studentId)));
    }
}
