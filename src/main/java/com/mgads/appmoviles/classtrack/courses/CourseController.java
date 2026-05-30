package com.mgads.appmoviles.classtrack.courses;

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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a new course")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Course created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Only teachers can create courses")
    })
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CreateCourseRequest request) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        CourseResponse data = courseService.create(teacherId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Course created", data));
    }

    @GetMapping
    @Operation(summary = "List courses for the authenticated user")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Courses listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ApiResponse<List<CourseListResponse>>> list(Authentication auth) {
        Long userId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        List<CourseListResponse> data = courseService.listForUser(userId, role);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course detail")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<ApiResponse<CourseResponse>> getById(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getById(courseId)));
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Delete (deactivate) a course")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not the course owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long courseId) {
        Long teacherId = userService.resolveUserId(AuthUtils.getCurrentEmail());
        courseService.delete(courseId, teacherId);
        return ResponseEntity.ok(ApiResponse.ok("Course deleted"));
    }
}
