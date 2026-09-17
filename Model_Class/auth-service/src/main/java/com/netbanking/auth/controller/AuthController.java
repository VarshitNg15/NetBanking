package com.netbanking.auth.controller;

import com.netbanking.auth.dto.AuthDtos.*;
import com.netbanking.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, credentials login, MFA OTP verification, token refresh, logout, and password recovery.")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user identity", description = "Creates a new user credentials record, provisions a unique customerId, initializes customer shell in User-Service, and dispatches an email verification OTP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful, verification OTP issued"),
            @ApiResponse(responseCode = "400", description = "Validation failure or email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Authenticate user and issue JWTs", description = "Validates user email and password. If MFA is required, triggers a 6-digit login OTP. On full authentication, returns access token, refresh token, and user roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful (or MFA challenge returned)"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account is temporarily locked or disabled")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String userAgent = http.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ip, userAgent));
    }

    @Operation(summary = "Refresh access token", description = "Exchanges a valid, unrevoked refresh token for a newly signed JWT access token and rotated refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Revoke user session and logout", description = "Revokes the supplied refresh token and marks the session as LOGOUT in history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    @Operation(summary = "Verify OTP", description = "Validates a 6-digit OTP code for email verification, login MFA, or password reset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @Operation(summary = "Request password reset", description = "Issues a password reset token and dispatches an email OTP/link.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset instructions dispatched")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Operation(summary = "Reset password with token", description = "Consumes a valid password reset token and updates the user's password hash.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
