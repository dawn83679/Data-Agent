package edu.zsc.ai.service.ai.parse;

import edu.zsc.ai.model.dto.response.ai.ParseResult;
import edu.zsc.ai.service.ai.parse.rule.ParseRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class LLMStreamParser {
    private final List<ParseRule> rules = new ArrayList<>();

    private final StringBuilder buffer = new StringBuilder(1024);

    private boolean rulesSorted = false;

    /**
     * add parse rule
     */
    public void addRule(ParseRule rule) {
        rules.add(rule);
        rulesSorted = false;
        log.debug("add parse rule: {} (priority: {})", rule.getName(), rule.getPriority());
    }


    /**
     * 移除解析规则
     */
    public void removeRule(String ruleName) {
        rules.removeIf(rule -> rule.getName().equals(ruleName));
        rulesSorted = false;
        log.debug("移除解析规则: {}", ruleName);
    }

    /**
     * 获取所有解析规则（按优先级排序）
     */
    public List<ParseRule> getRules() {
        ensureRulesSorted();
        return new ArrayList<>(rules);
    }

    /**
     * 确保规则按优先级排序
     */
    private void ensureRulesSorted() {
        if (!rulesSorted) {
            rules.sort(Comparator.comparingInt(ParseRule::getPriority));
            rulesSorted = true;
            log.debug("parse rules sorted by priority");
        }
    }

    /**
     * 处理流式数据块
     */
    public List<ParseResult> processChunk(String chunk) {
        String beforeBuffer = buffer.toString();
        buffer.append(chunk);
        log.info("收到新分片: [{}]，拼接前buffer: [{}]，拼接后buffer内容: [{}]", chunk, beforeBuffer, buffer);

        ensureRulesSorted();

        for (ParseRule rule : rules) {
            List<ParseResult> results = rule.tryParse(buffer.toString());

            for (ParseResult result : results) {
                String beforeUpdate = buffer.toString();

                updateBuffer(result.getEndPos());

                log.info("规则[{}]命中，被移除内容: [{}]，剩余buffer: [{}]",
                        rule.getName(), beforeUpdate.substring(0, result.getEndPos()), buffer);
            }
            if (CollectionUtils.isNotEmpty(results)) {
                return results;
            }
        }
        return Collections.emptyList();
    }

    public String getBuffer() {
        return buffer.toString();
    }

    public void clearBuffer() {
        buffer.setLength(0);
    }

    private void updateBuffer(int endPosition) {
        if (endPosition > 0 && endPosition <= buffer.length()) {
            buffer.delete(0, endPosition);
        }
    }
}
