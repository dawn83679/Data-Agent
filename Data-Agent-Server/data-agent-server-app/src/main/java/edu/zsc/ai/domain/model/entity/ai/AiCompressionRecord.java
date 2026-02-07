package edu.zsc.ai.domain.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("ai_compression_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompressionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long userId;

    private Long startMessageId;

    private Long endMessageId;

    private Long summaryMessageId;

    private String compressionStrategy;

    private Integer tokenBefore;

    private Integer tokenAfter;

    private Short status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
