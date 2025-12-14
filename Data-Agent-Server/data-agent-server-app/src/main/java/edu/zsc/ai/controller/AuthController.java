package edu.zsc.ai.controller;

import edu.zsc.ai.domain.service.sys.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {


    @Autowired
    private AuthService authService;
}
