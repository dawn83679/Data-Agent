package edu.zsc.ai.domain.service.ai.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.cache.TodoCache;
import edu.zsc.ai.domain.mapper.ai.AiTodoTaskMapper;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;
import edu.zsc.ai.domain.service.ai.AiTodoTaskService;
import edu.zsc.ai.model.TodoList;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AiTodoTaskServiceImpl
 * Service implementation for AiTodoTask.
 * Integrates with TodoCache for performance.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTodoTaskServiceImpl extends ServiceImpl<AiTodoTaskMapper, AiTodoTask> implements AiTodoTaskService {

    private final TodoCache todoCache;

    @Override
    public AiTodoTask getByConversationId(Long conversationId) {
        if (conversationId == null) {
            return null;
        }

        // 1. Try cache first
        TodoList cachedList = todoCache.get(conversationId);
        if (cachedList != null) {
            log.debug("Cache hit for conversation: {}", conversationId);
            return AiTodoTask.builder()
                    .conversationId(conversationId)
                    .userId(StpUtil.getLoginIdAsLong())
                    .content(JsonUtil.object2json(cachedList))
                    .build();
        }

        // 2. Load from database with userId validation
        log.debug("Cache miss for conversation: {}, loading from DB", conversationId);
        Long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<AiTodoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiTodoTask::getConversationId, conversationId)
                .eq(AiTodoTask::getUserId, currentUserId);
        AiTodoTask task = this.getOne(queryWrapper);

        // 3. Update cache if found
        if (task != null && task.getContent() != null) {
            TodoList todoList = JsonUtil.json2Object(task.getContent(), TodoList.class);
            todoCache.put(conversationId, todoList);
        }

        return task;
    }

    @Override
    public boolean saveByConversationId(AiTodoTask task) {
        if (task == null || task.getConversationId() == null) {
            return false;
        }

        // Set current userId before saving
        task.setUserId(StpUtil.getLoginIdAsLong());

        boolean success = this.save(task);
        if (success) {
            updateCache(task);
        }
        return success;
    }

    @Override
    public boolean updateByConversationId(AiTodoTask task) {
        if (task == null || task.getConversationId() == null) {
            return false;
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaUpdateWrapper<AiTodoTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AiTodoTask::getConversationId, task.getConversationId())
                .eq(AiTodoTask::getUserId, currentUserId);
        
        boolean success = this.update(task, updateWrapper);
        if (success) {
            updateCache(task);
        }
        return success;
    }

    @Override
    public boolean removeByConversationId(Long conversationId) {
        if (conversationId == null) {
            return false;
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<AiTodoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiTodoTask::getConversationId, conversationId)
                .eq(AiTodoTask::getUserId, currentUserId);
        
        boolean success = this.remove(queryWrapper);
        if (success) {
            todoCache.invalidate(conversationId);
            log.debug("Invalidated cache for conversation: {}", conversationId);
        }
        return success;
    }

    /**
     * Helper to update cache from entity
     */
    private void updateCache(AiTodoTask task) {
        if (task.getContent() != null) {
            try {
                TodoList todoList = JsonUtil.json2Object(task.getContent(), TodoList.class);
                todoCache.put(task.getConversationId(), todoList);
                log.debug("Updated cache for conversation: {}", task.getConversationId());
            } catch (Exception e) {
                log.warn("Failed to update cache after DB operation for conversation: {}", 
                    task.getConversationId(), e);
            }
        }
    }
}
