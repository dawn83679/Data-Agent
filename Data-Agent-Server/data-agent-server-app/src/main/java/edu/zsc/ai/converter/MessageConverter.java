package edu.zsc.ai.converter;

import edu.zsc.ai.enums.ai.message.MessageRoleEnum;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Converter for AI messages
 *
 * @author zgq
 */
public class MessageConverter {

    /**
     * Convert role and content to Spring AI Message
     *
     * @param role    message role
     * @param content message content
     * @return Spring AI Message
     */
    public static Message toSpringMessage(String role, String content) {
        if (MessageRoleEnum.USER.name().equalsIgnoreCase(role)) {
            return new UserMessage(content);
        } else if (MessageRoleEnum.ASSISTANT.name().equalsIgnoreCase(role)) {
            return new AssistantMessage(content);
        } else if (MessageRoleEnum.SYSTEM.name().equalsIgnoreCase(role)) {
            return new SystemMessage(content);
        }
        return null;
    }
}
