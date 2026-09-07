package com.projectmanagement.app.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final JwtService jwtService;
        private final PasswordEncoder passwordEncoder;
        private final RefreshTokenRepository refreshTokenRepository;
        private final RevokedAccessTokenRepository revokedAccessTokenRepository;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final PasswordResetDeliveryService passwordResetDeliveryService;
        private final EmailVerificationTokenRepository emailVerificationTokenRepository;
        private final EmailVerificationDeliveryService emailVerificationDeliveryService;
        private final LoginRateLimitService loginRateLimitService;
        @Value("${auth.refresh-expiration-days:30}")
        private long refreshExpirationDays;
        @Value("${auth.password-reset-expiration-minutes:30}")
        private long passwordResetExpirationMinutes;
        @Value("${auth.email-verification-expiration-hours:24}")
        private long emailVerificationExpirationHours;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        RefreshTokenRepository refreshTokenRepository,
                        RevokedAccessTokenRepository revokedAccessTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordResetDeliveryService passwordResetDeliveryService,
                        EmailVerificationTokenRepository emailVerificationTokenRepository,
                        EmailVerificationDeliveryService emailVerificationDeliveryService,
                        LoginRateLimitService loginRateLimitService) {

                this.authenticationManager = authenticationManager;
                this.userRepository = userRepository;
                this.jwtService = jwtService;
                this.passwordEncoder = passwordEncoder;
                this.refreshTokenRepository = refreshTokenRepository;
                this.revokedAccessTokenRepository = revokedAccessTokenRepository;
                this.passwordResetTokenRepository = passwordResetTokenRepository;
                this.passwordResetDeliveryService = passwordResetDeliveryService;
                this.emailVerificationTokenRepository = emailVerificationTokenRepository;
                this.emailVerificationDeliveryService = emailVerificationDeliveryService;
                this.loginRateLimitService = loginRateLimitService;
        }

        public AuthResponse login(AuthRequest request, String clientIp) {
                String email = request.getEmail().trim().toLowerCase();
                loginRateLimitService.checkAllowed(email, clientIp);

                Authentication authentication;
                try {
                        authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(email, request.getPassword()));
                } catch (AuthenticationException exception) {
                        loginRateLimitService.recordFailure(email, clientIp);
                        throw exception;
                }
                loginRateLimitService.recordSuccess(email, clientIp);

                User user = userRepository
                                .findByEmail(email)
                                .filter(existingUser -> existingUser.getDeletedAt() == null)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found"));

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                String token = jwtService.generateToken(userDetails);
                String refreshToken = createRefreshToken(user);

                String role = userRepository
                                .findRoleNameByUserId(user.getId())
                                .map(String::toUpperCase)
                                .orElse(null);

                return AuthResponse.builder()
                                .accessToken(token)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(role)
                                .build();
        }

        @Transactional
        public AuthResponse refresh(RefreshRequest request) {
                RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken()))
                                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
                if (stored.getRevokedAt() != null || !stored.getExpiresAt().isAfter(LocalDateTime.now())
                                || stored.getUser().getDeletedAt() != null)
                        throw new RuntimeException("Invalid refresh token");
                stored.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(stored);
                User user = stored.getUser();
                String accessToken = jwtService.generateToken(user.getEmail());
                return AuthResponse.builder().accessToken(accessToken).refreshToken(createRefreshToken(user))
                                .tokenType("Bearer")
                                .userId(user.getId()).name(user.getName()).email(user.getEmail())
                                .role(userRepository.findRoleNameByUserId(user.getId()).map(String::toUpperCase)
                                                .orElse(null))
                                .build();
        }

        @Transactional
        public void logout(String accessToken, String authenticatedEmail, LogoutRequest request) {
                refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken())).ifPresent(token -> {
                        if (token.getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
                                token.setRevokedAt(LocalDateTime.now());
                                refreshTokenRepository.save(token);
                        }
                });
                String tokenId = jwtService.extractTokenId(accessToken);
                if (tokenId == null || tokenId.isBlank()) {
                        throw new RuntimeException("Invalid access token");
                }
                revokedAccessTokenRepository.save(RevokedAccessToken.builder().tokenId(tokenId)
                                .expiresAt(jwtService.extractExpiration(accessToken).toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDateTime())
                                .build());
        }

        @Transactional
        public void requestPasswordReset(PasswordResetRequest request) {
                userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                                .filter(user -> user.getDeletedAt() == null)
                                .ifPresent(user -> {
                                        LocalDateTime now = LocalDateTime.now();
                                        passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                                                        .forEach(token -> token.setUsedAt(now));

                                        String rawToken = randomToken();
                                        passwordResetTokenRepository.save(PasswordResetToken.builder()
                                                        .user(user)
                                                        .tokenHash(hash(rawToken))
                                                        .expiresAt(now.plusMinutes(passwordResetExpirationMinutes))
                                                        .build());
                                        passwordResetDeliveryService.deliver(user, rawToken);
                                });
        }

        @Transactional
        public void confirmPasswordReset(PasswordResetConfirmRequest request) {
                PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(request.getToken()))
                                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));
                if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())
                                || token.getUser().getDeletedAt() != null) {
                        throw new RuntimeException("Invalid or expired password reset token");
                }

                User user = token.getUser();
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                token.setUsedAt(LocalDateTime.now());
                refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())
                                .forEach(refreshToken -> refreshToken.setRevokedAt(LocalDateTime.now()));
        }

        @Transactional
        public void confirmEmailVerification(EmailVerificationRequest request) {
                EmailVerificationToken token = emailVerificationTokenRepository
                                .findByTokenHash(hash(request.getToken()))
                                .orElseThrow(() -> new RuntimeException("Invalid or expired email verification token"));
                if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())
                                || token.getUser().getDeletedAt() != null) {
                        throw new RuntimeException("Invalid or expired email verification token");
                }
                token.setUsedAt(LocalDateTime.now());
                token.getUser().setEmailVerifiedAt(LocalDateTime.now());
        }

        @Transactional
        public void resendEmailVerification(String email) {
                User user = userRepository.findByEmail(email)
                                .filter(existingUser -> existingUser.getDeletedAt() == null)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                if (user.getEmailVerifiedAt() == null) {
                        issueEmailVerification(user);
                }
        }

        private void issueEmailVerification(User user) {
                LocalDateTime now = LocalDateTime.now();
                emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                                .forEach(token -> token.setUsedAt(now));
                String rawToken = randomToken();
                emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                                .user(user)
                                .tokenHash(hash(rawToken))
                                .expiresAt(now.plusHours(emailVerificationExpirationHours))
                                .build());
                emailVerificationDeliveryService.deliver(user, rawToken);
        }

        private String createRefreshToken(User user) {
                String token = randomToken();
                refreshTokenRepository.save(RefreshToken.builder().user(user).tokenHash(hash(token))
                                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays)).build());
                return token;
        }

        private String randomToken() {
                byte[] bytes = new byte[48];
                new SecureRandom().nextBytes(bytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        private String hash(String value) {
                try {
                        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                        .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException(exception);
                }
        }

        public AuthMeResponse getCurrentUser(String email) {

                User user = userRepository
                                .findByEmail(email)
                                .filter(existingUser -> existingUser.getDeletedAt() == null)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found"));

                String role = userRepository
                                .findRoleNameByUserId(user.getId())
                                .map(String::toUpperCase)
                                .orElse(null);

                return AuthMeResponse.builder()
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(role)
                                .build();
        }

        @Transactional
        public SignupResponse signup(SignupRequest request) {

                String email = request.getEmail()
                                .trim()
                                .toLowerCase();

                // Check existing user
                if (userRepository.findByEmail(email).isPresent()) {
                        throw new RuntimeException(
                                        "User already exists with email: " + email);
                }

                // Create user
                User user = User.builder()
                                .name(request.getName().trim())
                                .email(email)
                                .password(passwordEncoder.encode(request.getPassword()))
                                .build();

                User savedUser = userRepository.save(user);
                issueEmailVerification(savedUser);

                return SignupResponse.builder()
                                .userId(savedUser.getId())
                                .name(savedUser.getName())
                                .email(savedUser.getEmail())
                                .message("User registered successfully. Please verify your email.")
                                .build();
        }
}
