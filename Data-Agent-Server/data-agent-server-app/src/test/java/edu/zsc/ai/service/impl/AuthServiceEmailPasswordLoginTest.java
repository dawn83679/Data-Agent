package edu.zsc.ai.service.impl;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.zsc.ai.model.dto.request.LoginRequest;
import edu.zsc.ai.model.entity.User;
import edu.zsc.ai.service.LoginAttemptService;
import edu.zsc.ai.service.UserService;
import edu.zsc.ai.util.PasswordUtil;

/**
 * Email/Password Login Tests
 * Tests login endpoints using MockMvc
 * 
 * Note: @Transactional annotation removed because Redis operations are not transactional
 * and the annotation was interfering with Redis state persistence across test method calls.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Email/Password Login Tests")
class AuthServiceEmailPasswordLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String testEmail = "test@example.com";
    private final String testPassword = "Test123456!";

    @BeforeEach
    void setUp() {
        // Clear login failure records in Redis - ensure clean state at test start
        // Clear email-related counters and lock status
        loginAttemptService.clearFailureCount(testEmail);
        loginAttemptService.clearFailureCount("unverified@example.com");
        loginAttemptService.clearFailureCount("nonexistent@example.com");
        
        // Clear IP address-related counters and lock status
        loginAttemptService.clearFailureCount("127.0.0.1");
        loginAttemptService.clearFailureCount("0:0:0:0:0:0:0:1"); // IPv6 localhost
        
        // Clean up any existing test data
        User existingUser = userService.getByEmail(testEmail);
        if (existingUser != null) {
            userService.removeById(existingUser.getId());
        }

        // Create test user
        testUser = new User();
        testUser.setEmail(testEmail);
        testUser.setPasswordHash(PasswordUtil.encode(testPassword));
        testUser.setUsername("testuser");
        testUser.setVerified(true);
        userService.save(testUser);
        
        System.out.println("Test setup completed - Redis and database cleaned");
    }

    @AfterEach
    void tearDown() {
        // Clear login failure records in Redis - ensure no impact on other tests
        // Clear email-related counters and lock status
        loginAttemptService.clearFailureCount(testEmail);
        loginAttemptService.clearFailureCount("unverified@example.com");
        loginAttemptService.clearFailureCount("nonexistent@example.com");
        
        // Clear IP address-related counters and lock status
        loginAttemptService.clearFailureCount("127.0.0.1");
        loginAttemptService.clearFailureCount("0:0:0:0:0:0:0:1"); // IPv6 localhost
        
        // Clean up test data
        try {
            if (testUser != null && testUser.getId() != null) {
                userService.removeById(testUser.getId());
            }
            User unverifiedUser = userService.getByEmail("unverified@example.com");
            if (unverifiedUser != null) {
                userService.removeById(unverifiedUser.getId());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
            System.err.println("Error during test cleanup: " + e.getMessage());
        }
        
        System.out.println("Test teardown completed - Redis and database cleaned");
    }

    @Test
    @DisplayName("Test Successful Login")
    void testSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("accessToken"));
    }

    @Test
    @DisplayName("Test Login With Wrong Password")
    void testLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("Test Login With Non-Existent Email")
    void testLoginWithNonExistentEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword(testPassword);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("Test Login With Unverified Email")
    void testLoginWithUnverifiedEmail() throws Exception {
        // Create user with unverified email
        String unverifiedEmail = "unverified@example.com";
        User unverifiedUser = new User();
        unverifiedUser.setEmail(unverifiedEmail);
        unverifiedUser.setPasswordHash(PasswordUtil.encode(testPassword));
        unverifiedUser.setUsername("unverified");
        unverifiedUser.setVerified(false);
        userService.save(unverifiedUser);

        LoginRequest request = new LoginRequest();
        request.setEmail(unverifiedEmail);
        request.setPassword(testPassword);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    @DisplayName("Test Account Lock After Multiple Failures")
    void testAccountLockAfterMultipleFailures() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // Attempt multiple failed logins and verify counter decrements
        for (int i = 0; i < 5; i++) {
            int expectedRemaining = 5 - i - 1; // Remaining attempts: 4, 3, 2, 1, 0
            
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40100));
            
            // Verify remaining attempts
            int actualRemaining = loginAttemptService.getRemainingAttempts(testEmail);
            System.out.println("After attempt " + (i + 1) + ", remaining attempts: " + actualRemaining + " (expected: " + expectedRemaining + ")");
            
            // Only verify remaining attempts after first 4 failures (after 5th failure, account is locked and remaining is 0)
            if (i < 4) {
                assertTrue(actualRemaining == expectedRemaining, 
                    "After " + (i + 1) + " failed attempts, expected " + expectedRemaining + " remaining, but got " + actualRemaining);
            }
        }

        // Verify account is blocked
        assertTrue(loginAttemptService.isBlocked(testEmail), "Account should be blocked after 5 failed attempts");
        assertTrue(loginAttemptService.getRemainingAttempts(testEmail) == 0, "Remaining attempts should be 0 when blocked");

        // Even correct password should be rejected
        request.setPassword(testPassword);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(42900)); // ACCOUNT_LOCKED_ERROR
    }

    @Test
    @DisplayName("Test Clear Failure Count After Successful Login")
    void testClearFailureCountAfterSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // Fail a few times first
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40100));
        }

        // Then login successfully
        request.setPassword(testPassword);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());

        // Verify failure count is cleared (account not blocked)
        assertFalse(loginAttemptService.isBlocked(testEmail));
    }

    @Test
    @DisplayName("Test Login With Empty Email")
    void testLoginWithEmptyEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword(testPassword);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test Login With Empty Password")
    void testLoginWithEmptyPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test Remember Me Flag")
    void testRememberMeFlag() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRememberMe(true);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("Test Token Format")
    void testTokenFormat() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
    }

    @Test
    @DisplayName("Test Password Case Sensitivity")
    void testPasswordCaseSensitivity() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword.toUpperCase());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));
    }
}
