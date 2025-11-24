package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.mapper.LoginLogMapper;
import edu.zsc.ai.model.entity.LoginLog;
import edu.zsc.ai.service.LoginLogService;
import lombok.extern.slf4j.Slf4j;

/**
 * Login Log Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    @Override
    @Async
    public void recordSuccess(Long userId, String email, String ipAddress, String userAgent, String loginMethod) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setEmail(email);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginMethod(loginMethod);
        loginLog.setStatus("SUCCESS");
        loginLog.setCreateTime(LocalDateTime.now());

        this.save(loginLog);
        log.debug("Recorded successful login: email={}, method={}", email, loginMethod);
    }

    @Override
    @Async
    public void recordFailure(String email, String ipAddress, String userAgent, String loginMethod, String failureReason) {
        LoginLog loginLog = new LoginLog();
        loginLog.setEmail(email);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginMethod(loginMethod);
        loginLog.setStatus("FAILED");
        loginLog.setFailureReason(failureReason);
        loginLog.setCreateTime(LocalDateTime.now());

        this.save(loginLog);
        log.debug("Recorded failed login: email={}, reason={}", email, failureReason);
    }

    @Override
    public List<LoginLog> getUserLoginHistory(Long userId, int limit) {
        return this.list(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getCreateTime)
                .last("LIMIT " + limit));
    }

    @Override
    public List<LoginLog> getRecentFailedAttempts(String email, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return this.list(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getEmail, email)
                .eq(LoginLog::getStatus, "FAILED")
                .ge(LoginLog::getCreateTime, since)
                .orderByDesc(LoginLog::getCreateTime));
    }
}
