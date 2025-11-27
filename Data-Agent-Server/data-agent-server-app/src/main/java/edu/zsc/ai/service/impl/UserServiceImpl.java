package edu.zsc.ai.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.mapper.UserMapper;
import edu.zsc.ai.model.entity.User;
import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.service.SessionService;
import edu.zsc.ai.service.UserService;
import edu.zsc.ai.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public User getByEmail(String email) {
        // Only select necessary fields for login
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .select(User::getId, User::getEmail, User::getPasswordHash, 
                       User::getUsername, User::getVerified)
                .last("LIMIT 1"));
    }

    @Override
    public User getByPhone(String phone) {
        // Only select necessary fields for login
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .select(User::getId, User::getPhone, User::getPasswordHash, 
                       User::getUsername, User::getVerified)
                .last("LIMIT 1"));
    }

    @Override
    public boolean emailExists(String email) {
        // Use EXISTS query for better performance
        return this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1")) > 0;
    }

    @Override
    public boolean phoneExists(String phone) {
        // Use EXISTS query for better performance
        return this.count(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1")) > 0;
    }

    @Override
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        // 1. Get user by email
        User user = getByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // 2. Encode new password
        String encodedPassword = PasswordUtil.encode(newPassword);

        // 3. Update user password
        user.setPasswordHash(encodedPassword);
        boolean updated = this.updateById(user);
        
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to update password");
        }

        // 4. Revoke all sessions for this user
        sessionService.revokeAllUserSessions(user.getId());

        // 5. Revoke all refresh tokens for this user
        refreshTokenService.revokeAllUserTokens(user.getId());

        log.info("Password reset successfully for user: email={}, userId={}", email, user.getId());
        return true;
    }

    @Override
    @Transactional
    public boolean updateProfile(Long userId, String username, String avatar, String phone) {
        // Get user
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // Update fields if provided
        boolean updated = false;
        if (username != null && !username.trim().isEmpty()) {
            user.setUsername(username.trim());
            updated = true;
        }
        if (avatar != null && !avatar.trim().isEmpty()) {
            user.setAvatarUrl(avatar.trim());
            updated = true;
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone.trim());
            updated = true;
        }

        if (!updated) {
            log.debug("No fields to update for user: userId={}", userId);
            return true;
        }

        // Save changes
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to update user profile");
        }

        log.info("User profile updated successfully: userId={}", userId);
        return true;
    }
}
