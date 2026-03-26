package edu.zsc.ai.domain.service.ai.export;

import edu.zsc.ai.common.enums.ai.FileExportFormatEnum;
import edu.zsc.ai.domain.service.ai.export.model.FileExportArtifact;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import org.apache.commons.lang3.StringUtils;

public interface FileExportStrategy {

    String format();

    FileExportArtifact export(FileExportRequest request);

    default boolean supports(String candidateFormat) {
        return !StringUtils.isBlank(candidateFormat)
                && StringUtils.equalsIgnoreCase(format(), FileExportFormatEnum.fromValue(candidateFormat).name());
    }
}
