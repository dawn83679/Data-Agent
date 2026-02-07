package edu.zsc.ai.domain.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("ai_message_block")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageBlock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private String blockType;

    private String content;

    private String extensionData;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
