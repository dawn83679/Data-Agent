package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateTableRequest extends BaseRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "databaseName is required")
    private String databaseName;

    @NotBlank(message = "tableName is required")
    private String tableName;

    @NotEmpty(message = "columns is required")
    private List<ColumnDefinition> columns;

    private String engine;

    private String charset;

    private String collation;

    private String comment;

    private List<String> primaryKey;

    private List<IndexDefinition> indexes;

    private List<ForeignKeyDefinition> foreignKeys;

    private List<String> constraints;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDefinition {
        @NotBlank(message = "column name is required")
        private String name;

        @NotBlank(message = "column type is required")
        private String type;

        private Integer length;

        private Integer decimals;

        private boolean nullable = true;

        private String keyType;

        private String defaultValue;

        private String comment;

        private boolean autoIncrement;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexDefinition {
        private String name;

        private List<String> columns;

        private String type;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignKeyDefinition {
        private String name;

        private String column;

        private String referencedTable;

        private String referencedColumn;

        private String onDelete;

        private String onUpdate;
    }
}
