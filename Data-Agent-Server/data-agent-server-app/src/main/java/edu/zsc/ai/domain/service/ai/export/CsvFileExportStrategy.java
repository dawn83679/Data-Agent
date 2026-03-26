package edu.zsc.ai.domain.service.ai.export;

import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import edu.zsc.ai.common.enums.ai.FileExportFormatEnum;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CsvFileExportStrategy extends AbstractFastExcelFileExportStrategy {

    @Override
    protected FileExportFormatEnum exportFormat() {
        return FileExportFormatEnum.CSV;
    }

    @Override
    protected ExcelTypeEnum excelType() {
        return ExcelTypeEnum.CSV;
    }

    @Override
    protected void configureWriter(ExcelWriterBuilder writerBuilder) {
        writerBuilder.charset(StandardCharsets.UTF_8)
                .withBom(Boolean.TRUE);
    }
}
