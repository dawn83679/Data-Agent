package edu.zsc.ai.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Controller
 * Handles system administration tasks
 * 
 * @author Data-Agent Team
 */
@Tag(name = "Admin", description = "System administration APIs")
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    // Redis cache monitoring endpoints have been removed
    // as Redis dependency has been eliminated from the system
    
}
