package edu.zsc.ai.controller.sys;

import edu.zsc.ai.domain.model.enums.AuthProviderEnum;
import edu.zsc.ai.domain.service.oauth.OAuthLoginService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthLoginService oAuthLoginService;

    @GetMapping("/google")
    public void getGoogleLoginUrl(
            @RequestParam @NotBlank(message = "fromUrl is required") String fromUrl,
            HttpServletResponse response) throws IOException {
        response.sendRedirect(oAuthLoginService.getAuthorizationUrl(fromUrl, AuthProviderEnum.GOOGLE.getValue()));
    }

    @GetMapping("/callback/google")
    public void googleCallback(
            @RequestParam @NotBlank(message = "code is required") String code,
            @RequestParam @NotBlank(message = "state is required") String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        String redirectUrl = oAuthLoginService.handleCallback(AuthProviderEnum.GOOGLE.getValue(), code, state, error);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/github")
    public void getGithubLoginUrl(
            @RequestParam @NotBlank(message = "fromUrl is required") String fromUrl,
            HttpServletResponse response) throws IOException {
        response.sendRedirect(oAuthLoginService.getAuthorizationUrl(fromUrl, AuthProviderEnum.GITHUB.getValue()));
    }

    @GetMapping("/callback/github")
    public void githubCallback(
            @RequestParam @NotBlank(message = "code is required") String code,
            @RequestParam @NotBlank(message = "state is required") String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        String redirectUrl = oAuthLoginService.handleCallback(AuthProviderEnum.GITHUB.getValue(), code, state, error);
        response.sendRedirect(redirectUrl);
    }
}
