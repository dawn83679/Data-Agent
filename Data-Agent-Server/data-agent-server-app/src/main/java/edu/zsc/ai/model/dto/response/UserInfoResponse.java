package edu.zsc.ai.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Info Response
 *
 * @author Data-Agent Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;
    private String email;
    private String phone;
    private String username;
    private String avatar;
    private Boolean emailVerified;
    private Boolean phoneVerified;
}
