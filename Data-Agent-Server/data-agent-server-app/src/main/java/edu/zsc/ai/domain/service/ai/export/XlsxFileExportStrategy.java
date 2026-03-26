package edu.zsc.ai.domain.service.ai.export;

import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import cn.idev.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import edu.zsc.ai.common.enums.ai.FileExportFormatEnum;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.stereotype.Component;

@Component
public class XlsxFileExportStrategy extends AbstractFastExcelFileExportStrategy {

    @Override
    protected FileExportFormatEnum exportFormat() {
        return FileExportFormatEnum.XLSX;
    }

    @Override
    protected ExcelTypeEnum excelType() {
        return ExcelTypeEnum.XLSX;
    }

    @Override
    protected void configureWriter(ExcelWriterBuilder writerBuilder) {
        writerBuilder.registerWriteHandler(new HorizontalCellStyleStrategy(buildHeadStyle(), (WriteCellStyle) null))
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20));
    }

    private WriteCellStyle buildHeadStyle() {
        WriteFont headerFont = new WriteFont();
        headerFont.setBold(Boolean.TRUE);

        WriteCellStyle headerStyle = new WriteCellStyle();
        headerStyle.setWriteFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }
}
