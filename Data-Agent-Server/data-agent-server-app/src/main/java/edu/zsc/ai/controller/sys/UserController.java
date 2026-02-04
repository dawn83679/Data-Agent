package edu.zsc.ai.controller.sys;

import edu.zsc.ai.domain.model.dto.request.sys.UpdateUserRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.sys.UserResponse;
import edu.zsc.ai.domain.service.sys.AuthService;
import edu.zsc.ai.domain.service.sys.SysUsersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUsersService sysUsersService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        UserResponse data = authService.getCurrentUser();
        return ApiResponse.success(data);
    }

    @PutMapping("/me")
    public ApiResponse<Void> updateProfile(@RequestBody @Validated UpdateUserRequest request) {
        sysUsersService.updateUserProfile(request);
        return ApiResponse.success();
    }

}
