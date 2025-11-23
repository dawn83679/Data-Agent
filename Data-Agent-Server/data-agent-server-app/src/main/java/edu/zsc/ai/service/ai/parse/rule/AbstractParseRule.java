package edu.zsc.ai.service.ai.parse.rule;

import edu.zsc.ai.model.dto.response.ai.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Abstract base class for parse rules
 * Provides common implementation with callback functionality
 */
@Slf4j
public abstract class AbstractParseRule implements ParseRule {

    private ParseCompleteCallback parseCompleteCallback;
    private ParseErrorCallback parseErrorCallback;

    @Override
    public void setParseCompleteCallback(ParseCompleteCallback callback) {
        this.parseCompleteCallback = callback;
    }

    @Override
    public void setParseErrorCallback(ParseErrorCallback callback) {
        this.parseErrorCallback = callback;
    }

    @Override
    public ParseCompleteCallback getParseCompleteCallback() {
        return parseCompleteCallback;
    }

    @Override
    public ParseErrorCallback getParseErrorCallback() {
        return parseErrorCallback;
    }

    /**
     * Safe parsing method with error handling
     *
     * @param buffer buffer content
     */
    @Override
    public List<ParseResult> tryParse(String buffer) {
        try {
            List<ParseResult> results = doParse(buffer);

            // Call parse complete callback
            if (parseCompleteCallback != null && CollectionUtils.isNotEmpty(results)) {
                try {
                    parseCompleteCallback.onParseComplete(this, results);
                } catch (Exception e) {
                    log.error("Exception in parse complete callback", e);
                }
            }

            return results;

        } catch (Exception e) {
            String errorMsg = String.format("Parse rule [%s] execution failed: %s", getName(), e.getMessage());
            log.error(errorMsg, e);

            // Call error callback
            if (parseErrorCallback != null) {
                try {
                    parseErrorCallback.onParseError(this, buffer, errorMsg, e);
                } catch (Exception callbackException) {
                    log.error("Exception in error callback", callbackException);
                }
            }

            return List.of(); // Return empty list
        }
    }

    /**
     * Specific parsing implementation, to be overridden by subclasses
     *
     * @param buffer buffer content
     * @return list of parse results
     */
    protected abstract List<ParseResult> doParse(String buffer);
}