package edu.zsc.ai.domain.model.dto.request.sys;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    private String username;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

}
