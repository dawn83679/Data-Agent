package edu.zsc.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;

import edu.zsc.ai.model.entity.User;

/**
 * User Service
 *
 * @author Data-Agent Team
 */
public interface UserService extends IService<User> {

    /**
     * Get user by email
     */
    User getByEmail(String email);

    /**
     * Get user by phone
     */
    User getByPhone(String phone);

    /**
     * Check if email exists
     */
    boolean emailExists(String email);

    /**
     * Check if phone exists
     */
    boolean phoneExists(String phone);

    /**
     * Reset user password
     * This will invalidate all sessions and refresh tokens
     *
     * @param email user email
     * @param newPassword new password (plain text, will be encoded)
     * @return true if reset successful
     */
    boolean resetPassword(String email, String newPassword);
}
