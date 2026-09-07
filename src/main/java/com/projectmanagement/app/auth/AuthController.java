package com.projectmanagement.app.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                authService.login(request, clientIpResolver.resolve(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorization,
            Authentication authentication,
            @Valid @RequestBody LogoutRequest request) {
        if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            return ResponseEntity.badRequest().build();
        }
        authService.logout(authorization.substring(7), authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        authService.confirmEmailVerification(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendEmailVerification(Authentication authentication) {
        authService.resendEmailVerification(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> getCurrentUser(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                authService.getCurrentUser(email));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.signup(request));
    }
}
