package edu.zsc.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import edu.zsc.ai.enums.ai.ModelEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {


    @Bean
    public ChatClient chatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.create(dashScopeChatModel);
    }

    @Bean("CompressContextChatClient")
    public ChatClient CompressContextChatClient(QwenProperties qwenProperties,CompressContextConfig config) {
        DashScopeChatModel dashScopeChatModel = DashScopeChatModel
                .builder()
                .dashScopeApi(DashScopeApi.builder()
                        .apiKey(qwenProperties.getApiKey())
                        .baseUrl(qwenProperties.getBaseUrl())
                        .build())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withEnableThinking(false)
                                .withModel(ModelEnum.QWEN3_CODER_PLUS.getModelName())
                                .withMaxToken(config.getMaxToken())
                                .build()
                )
                .build();

        return ChatClient.create(dashScopeChatModel);
    }
}
