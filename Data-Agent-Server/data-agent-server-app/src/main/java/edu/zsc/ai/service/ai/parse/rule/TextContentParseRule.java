package edu.zsc.ai.service.ai.parse.rule;

import edu.zsc.ai.enums.ai.message.MessageBlockTypeEnum;
import edu.zsc.ai.constant.XmlTagConstants;
import edu.zsc.ai.model.dto.response.ai.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Text content parsing rule
 * Directly processes text content, but if contains < only processes content before <
 */
@Slf4j
public class TextContentParseRule extends AbstractParseRule {

    @Override
    public String getName() {
        return MessageBlockTypeEnum.TEXT.name();
    }

    @Override
    protected List<ParseResult> doParse(String buffer) {
        List<ParseResult> results = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(results)) {
            return results;
        }

        int xmlStart = buffer.indexOf("<");

        String content;
        int endPos;

        // No < symbol, process all content
        if (xmlStart == -1) {
            content = buffer;
            endPos = buffer.length();
        }
        // < symbol in middle, take content before it
        else if (xmlStart > 0) {
            content = buffer.substring(0, xmlStart);
            endPos = xmlStart;
        }
        // < symbol at beginning
        else {
            // Wait for more content
            if (buffer.length() < XmlTagConstants.MAX_TAG_LENGTH) {
                log.debug("Buffer starts with < and length insufficient for max tag length, waiting for more content: [{}]", buffer);
                return results;
            }

            // If it's a valid XML tag, let other processors handle it
            if (XmlTagConstants.containsTag(buffer)) {
                log.debug("Buffer starts with < and is valid tag, delegating to other processors: [{}]", buffer);
                return results;
            }

            // Otherwise treat entire content as text
            content = buffer;
            endPos = buffer.length();
            log.debug("Buffer starts with < but not valid tag, processing as text: [{}]", buffer);
        }

        if (StringUtils.isNotEmpty(content)) {
            results.add(new ParseResult(getName(), content, 0, endPos));
            log.debug("Text parsing - length: {}, content: [{}]", content.length(), content);
        }

        return results;
    }

    @Override
    public int getPriority() {
        return 500; // Lower priority, let structured content parsers process first
    }
}