package edu.zsc.ai.domain.service.ai.export;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileExportStrategyResolver {

    private final List<FileExportStrategy> strategies;

    public FileExportStrategyResolver(List<FileExportStrategy> strategies) {
        this.strategies = strategies == null ? List.of() : List.copyOf(strategies);
    }

    public FileExportStrategy resolve(String format) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("format is required");
        }
        var normalized = edu.zsc.ai.common.enums.ai.FileExportFormatEnum.fromValue(format);
        return strategies.stream()
                .filter(strategy -> strategy.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported format '" + normalized.name() + "'. Supported formats: " + supportedFormatsText()
                ));
    }

    private String supportedFormatsText() {
        if (CollectionUtils.isEmpty(strategies)) {
            return "none";
        }
        return strategies.stream()
                .map(FileExportStrategy::format)
                .distinct()
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }
}
