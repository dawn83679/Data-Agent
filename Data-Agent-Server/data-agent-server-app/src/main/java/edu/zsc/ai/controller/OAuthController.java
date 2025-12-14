package edu.zsc.ai.controller;

import edu.zsc.ai.domain.model.enums.AuthProviderEnum;
import edu.zsc.ai.domain.model.dto.response.sys.TokenPairResponse;
import edu.zsc.ai.domain.service.oauth.OAuthLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthLoginService oAuthLoginService;

    @GetMapping("/google")
    public String getGoogleLoginUrl() {
        return oAuthLoginService.getAuthorizationUrl(AuthProviderEnum.GOOGLE.name());
    }

    @GetMapping("/callback/google")
    public TokenPairResponse googleCallback(@RequestParam String code) {
        return oAuthLoginService.login(AuthProviderEnum.GOOGLE.name(), code);
    }

    @GetMapping("/github")
    public String getGithubLoginUrl() {
        return oAuthLoginService.getAuthorizationUrl(AuthProviderEnum.GITHUB.name());
    }

    @GetMapping("/callback/github")
    public TokenPairResponse githubCallback(@RequestParam String code) {
        return oAuthLoginService.login(AuthProviderEnum.GITHUB.name(), code);
    }
}
