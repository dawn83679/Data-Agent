package edu.zsc.ai.domain.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("ai_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String role;

    private Integer tokenCount;

    private Integer status;

    private Integer priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
