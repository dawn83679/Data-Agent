package edu.zsc.ai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.zsc.ai.service.LoginAttemptService;

/**
 * Login Attempt Service Test
 *
 * @author Data-Agent Team
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginAttemptServiceImplTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        // Clear any existing attempts
        loginAttemptService.loginSucceeded(TEST_EMAIL);
        loginAttemptService.loginSucceeded(TEST_IP);
    }

    @Test
    @DisplayName("Test Login Failed Count")
    void testLoginFailedCount() {
        // Initially not blocked
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(5);

        // First failure
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(4);

        // Second failure
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(3);

        // Third failure
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(2);

        // Fourth failure
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(1);

        System.out.println("✅ Login failure count is correct");
    }

    @Test
    @DisplayName("Test Block After Max Attempts")
    void testBlockAfterMaxAttempts() {
        // Fail 5 times
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }

        // Should be blocked now
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(0);
        assertThat(loginAttemptService.getBlockTimeRemaining(TEST_EMAIL)).isGreaterThan(0);

        System.out.println("✅ Account blocked after reaching max attempts");
        System.out.println("   Remaining block time: " + loginAttemptService.getBlockTimeRemaining(TEST_EMAIL) + " seconds");
    }

    @Test
    @DisplayName("Test Clear Attempts On Success")
    void testClearAttemptsOnSuccess() {
        // Fail 3 times
        loginAttemptService.loginFailed(TEST_EMAIL);
        loginAttemptService.loginFailed(TEST_EMAIL);
        loginAttemptService.loginFailed(TEST_EMAIL);

        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(2);

        // Successful login
        loginAttemptService.loginSucceeded(TEST_EMAIL);

        // Attempts should be cleared
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(5);

        System.out.println("✅ Failure records cleared after successful login");
    }

    @Test
    @DisplayName("Test IP Address Independent Tracking")
    void testIPAddressTracking() {
        // Fail email 3 times
        for (int i = 0; i < 3; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }

        // Fail IP 2 times
        for (int i = 0; i < 2; i++) {
            loginAttemptService.loginFailed(TEST_IP);
        }

        // Check separate tracking
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(2);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_IP)).isEqualTo(3);

        System.out.println("✅ Email and IP address tracked independently");
        System.out.println("   Email remaining attempts: " + loginAttemptService.getRemainingAttempts(TEST_EMAIL));
        System.out.println("   IP remaining attempts: " + loginAttemptService.getRemainingAttempts(TEST_IP));
    }

    @Test
    @DisplayName("Test Cannot Attempt When Blocked")
    void testCannotAttemptWhenBlocked() {
        // Block the email
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }

        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();

        // Try to fail again (should still be blocked)
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();

        System.out.println("✅ Cannot continue attempts after being blocked");
    }
}
