package edu.zsc.ai.config;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

/**
 * MyBatis-Plus Auto-fill Handler
 * Automatically fills creation time and update time
 *
 * @author Data-Agent Team
 */
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

    /**
     * Auto-fill on insert
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // Auto-fill creation time (support both createTime and createdAt)
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        
        // Auto-fill update time (support both updateTime and updatedAt)
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * Auto-fill on update
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // Auto-fill update time (support both updateTime and updatedAt)
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
