package com.mgads.appmoviles.classtrack.students;

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
@RequestMapping("/api/v1/courses/{courseId}/students")
@RequiredArgsConstructor
@Tag(name = "Course Students", description = "Student linking endpoints")
public class CourseStudentController {

    private final CourseStudentService courseStudentService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Link a student to a course")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Student linked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User is not a student"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course or student not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Student already linked")
    })
    public ResponseEntity<ApiResponse<Void>> linkStudent(
            @PathVariable Long courseId,
            @Valid @RequestBody LinkStudentRequest request) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        courseStudentService.linkStudent(courseId, teacherId, request.getStudentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Student linked"));
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "List students in a course")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not the course owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents(@PathVariable Long courseId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        List<StudentResponse> data = courseStudentService.listStudents(courseId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Remove a student from a course")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student removed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not linked to course")
    })
    public ResponseEntity<ApiResponse<Void>> removeStudent(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        courseStudentService.removeStudent(courseId, teacherId, studentId);
        return ResponseEntity.ok(ApiResponse.ok("Student removed"));
    }
}
