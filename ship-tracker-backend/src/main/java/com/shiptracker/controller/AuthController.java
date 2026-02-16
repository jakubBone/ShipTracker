package com.shiptracker.controller;

import com.shiptracker.dto.LoginRequest;
import com.shiptracker.dto.LoginResponse;
import com.shiptracker.dto.UserResponse;
import com.shiptracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login and start session")
    @SecurityRequirements({})
    @ApiResponse(responseCode = "200", description = "Login successful, JSESSIONID cookie set")
    @ApiResponse(responseCode = "400", description = "Blank username or password")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {
        authService.login(request.username(), request.password(), session);
        return ResponseEntity.ok(new LoginResponse("Logged in successfully"));
    }

    @Operation(summary = "Logout and invalidate session")
    @SecurityRequirements({})
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpSession session, HttpServletResponse response) {
        authService.logout(session, response);
        return ResponseEntity.ok(new LoginResponse("Logged out successfully"));
    }

    @Operation(summary = "Get currently logged-in username")
    @ApiResponse(responseCode = "200", description = "Username of authenticated user")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Principal principal) {
        return ResponseEntity.ok(new UserResponse(principal.getName()));
    }
}
