package edu.zsc.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.UserMapper;
import edu.zsc.ai.model.entity.User;
import edu.zsc.ai.service.UserService;
import org.springframework.stereotype.Service;

/**
 * User Service Implementation
 *
 * @author Data-Agent Team
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByEmail(String email) {
        // Only select necessary fields for login
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .select(User::getId, User::getEmail, User::getPassword, 
                       User::getUsername, User::getStatus, User::getEmailVerified)
                .last("LIMIT 1"));
    }

    @Override
    public User getByPhone(String phone) {
        // Only select necessary fields for login
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .select(User::getId, User::getPhone, User::getPassword, 
                       User::getUsername, User::getStatus, User::getPhoneVerified)
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
}
