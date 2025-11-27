package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.zsc.ai.model.dto.request.EmailCodeLoginRequest;
import edu.zsc.ai.model.entity.EmailVerificationCode;
import edu.zsc.ai.model.entity.User;
import edu.zsc.ai.service.UserService;
import edu.zsc.ai.service.VerificationCodeService;

/**
 * Email Verification Code Login Tests
 * Tests email code login endpoints using MockMvc
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Email Verification Code Login Tests")
class AuthServiceEmailCodeLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String testEmail = "codetest@example.com";
    private final String testCode = "123456";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data for all test emails
        cleanupTestUser(testEmail);
        cleanupTestUser("nonexistent@example.com");
        cleanupTestUser("unverified-code@example.com");

        // Create test user
        testUser = new User();
        testUser.setEmail(testEmail);
        testUser.setUsername("codetestuser");
        testUser.setVerified(true);
        userService.save(testUser);

        System.out.println("Test setup completed - Database cleaned");
    }

    @AfterEach
    void tearDown() {
        // Clean up all test data
        try {
            cleanupTestUser(testEmail);
            cleanupTestUser("nonexistent@example.com");
            cleanupTestUser("unverified-code@example.com");
        } catch (Exception e) {
            System.err.println("Error during test cleanup: " + e.getMessage());
        }

        System.out.println("Test teardown completed - Database cleaned");
    }

    @Test
    @DisplayName("Test Successful Login With Valid Code")
    void testSuccessfulLoginWithValidCode() throws Exception {
        // Create valid verification code
        createVerificationCode(testEmail, testCode, "LOGIN", 5);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(testEmail);
        request.setCode(testCode);

        MvcResult result = mockMvc.perform(post("/api/auth/login/email-code")
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
    @DisplayName("Test Login With Invalid Code")
    void testLoginWithInvalidCode() throws Exception {
        // Create valid verification code
        createVerificationCode(testEmail, testCode, "LOGIN", 5);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(testEmail);
        request.setCode("999999"); // Wrong code

        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000)); // PARAMS_ERROR
    }

    @Test
    @DisplayName("Test Login With Expired Code")
    void testLoginWithExpiredCode() throws Exception {
        // Create expired verification code (expired 1 minute ago)
        createVerificationCode(testEmail, testCode, "LOGIN", -1);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(testEmail);
        request.setCode(testCode);

        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000)); // PARAMS_ERROR
    }

    @Test
    @DisplayName("Test Login With Non-Existent Email")
    void testLoginWithNonExistentEmail() throws Exception {
        // Create valid verification code for non-existent email
        createVerificationCode("nonexistent@example.com", testCode, "LOGIN", 5);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setCode(testCode);

        // Should succeed and auto-register the user
        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)) // Success - auto-register
                .andExpect(jsonPath("$.data.accessToken").exists());

        // Cleanup auto-registered user
        User autoUser = userService.getByEmail("nonexistent@example.com");
        if (autoUser != null) {
            userService.removeById(autoUser.getId());
        }
    }

    @Test
    @DisplayName("Test Login With Unverified Email")
    void testLoginWithUnverifiedEmail() throws Exception {
        // Create unverified user
        String unverifiedEmail = "unverified-code@example.com";
        User unverifiedUser = new User();
        unverifiedUser.setEmail(unverifiedEmail);
        unverifiedUser.setUsername("unverified");
        unverifiedUser.setVerified(false);
        userService.save(unverifiedUser);

        // Create valid verification code
        createVerificationCode(unverifiedEmail, testCode, "LOGIN", 5);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(unverifiedEmail);
        request.setCode(testCode);

        // Email code login doesn't check verified status - it succeeds
        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)) // Success
                .andExpect(jsonPath("$.data.accessToken").exists());

        // Cleanup
        userService.removeById(unverifiedUser.getId());
    }

    @Test
    @DisplayName("Test Login With Already Used Code")
    void testLoginWithAlreadyUsedCode() throws Exception {
        // Clean up any existing verification codes for this email first
        verificationCodeService.remove(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, testEmail));
        
        // Create verification code and mark as verified
        EmailVerificationCode code = new EmailVerificationCode();
        code.setEmail(testEmail);
        code.setCode(testCode);
        code.setCodeType("LOGIN");
        code.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        code.setVerified(true); // Already used
        code.setVerifiedAt(LocalDateTime.now().minusMinutes(1));
        verificationCodeService.save(code);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(testEmail);
        request.setCode(testCode);

        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000)); // PARAMS_ERROR
    }

    @Test
    @DisplayName("Test Login With Empty Email")
    void testLoginWithEmptyEmail() throws Exception {
        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail("");
        request.setCode(testCode);

        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test Login With Empty Code")
    void testLoginWithEmptyCode() throws Exception {
        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(testEmail);
        request.setCode("");

        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test Login With Invalid Email Format")
    void testLoginWithInvalidEmailFormat() throws Exception {
        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail("invalid-email");
        request.setCode(testCode);

        mockMvc.perform(post("/api/auth/login/email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test Token Format After Code Login")
    void testTokenFormatAfterCodeLogin() throws Exception {
        // Create valid verification code
        createVerificationCode(testEmail, testCode, "LOGIN", 5);

        EmailCodeLoginRequest request = new EmailCodeLoginRequest();
        request.setEmail(testEmail);
        request.setCode(testCode);

        MvcResult result = mockMvc.perform(post("/api/auth/login/email-code")
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

    /**
     * Helper method to create verification code
     */
    private void createVerificationCode(String email, String code, String codeType, int expiryMinutes) {
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setCodeType(codeType);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        verificationCode.setVerified(false);
        verificationCodeService.save(verificationCode);
    }

    /**
     * Helper method to cleanup test user by email
     */
    private void cleanupTestUser(String email) {
        try {
            User user = userService.getByEmail(email);
            if (user != null) {
                userService.removeById(user.getId());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
