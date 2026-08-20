package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.UserRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.UserResponse;
import com.zakisupermarket.service.UserService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/users - storeId: {}", storeId);

        List<UserResponse> users = userService.getAllUsers(storeId);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUsersCount(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/users/count - storeId: {}", storeId);

        Long count = userService.getUsersCount(storeId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(ApiResponse.success(response, "Users count retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/users/{} - storeId: {}", id, storeId);

        UserResponse user = userService.getUser(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {
        request.setStoreId(SecurityUtils.getCurrentStoreId());

        log.info("POST /api/users - storeId: {}, username: {}",
                request.getStoreId(), request.getUsername());

        UserResponse user = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        request.setStoreId(storeId);

        log.info("PUT /api/users/{} - storeId: {}", id, storeId);

        UserResponse user = userService.updateUser(id, request, storeId);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("DELETE /api/users/{} - storeId: {}", id, storeId);

        userService.deleteUser(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam Long storeId,
            @RequestParam String query) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/users/search - storeId: {}, query: '{}'", storeId, query);

        List<UserResponse> users = userService.searchUsers(storeId, query);
        return ResponseEntity.ok(ApiResponse.success(users, "Search completed successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getActiveUsers(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/users/active - storeId: {}", storeId);

        List<UserResponse> users = userService.getActiveUsers(storeId);
        return ResponseEntity.ok(ApiResponse.success(users, "Active users retrieved successfully"));
    }
}
