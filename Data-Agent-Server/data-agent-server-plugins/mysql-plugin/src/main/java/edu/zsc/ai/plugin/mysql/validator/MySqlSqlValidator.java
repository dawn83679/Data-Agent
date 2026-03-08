package edu.zsc.ai.plugin.mysql.validator;

import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.model.sql.SqlError;
import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.model.sql.SqlValidationResult;
import edu.zsc.ai.plugin.mysql.parser.MySqlLexer;
import edu.zsc.ai.plugin.mysql.parser.MySqlParser;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

public class MySqlSqlValidator implements SqlValidator {

    @Override
    public SqlValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlValidationResult.invalid(SqlType.UNKNOWN,
                    List.of(new SqlError(1, 0, "SQL statement is empty")));
        }

        MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(sql));
        lexer.removeErrorListeners();
        List<SqlError> errors = new ArrayList<>();
        lexer.addErrorListener(new CollectingErrorListener(errors));

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new CollectingErrorListener(errors));

        MySqlParser.RootContext tree = parser.root();

        if (!errors.isEmpty()) {
            SqlType quickType = classifySql(sql);
            return SqlValidationResult.invalid(quickType, errors);
        }

        MySqlAstExtractor extractor = new MySqlAstExtractor();
        extractor.visit(tree);

        return SqlValidationResult.valid(
                extractor.getSqlType(),
                extractor.getTables(),
                extractor.getColumns()
        );
    }

    @Override
    public SqlType classifySql(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlType.UNKNOWN;
        }

        MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(sql));
        lexer.removeErrorListeners();

        // Find first meaningful token (skip whitespace/comments)
        Token token;
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                String text = token.getText().toUpperCase();
                return switch (text) {
                    case "SELECT" -> SqlType.SELECT;
                    case "WITH" -> SqlType.SELECT;
                    case "INSERT", "REPLACE" -> SqlType.INSERT;
                    case "UPDATE" -> SqlType.UPDATE;
                    case "DELETE" -> SqlType.DELETE;
                    case "CREATE" -> SqlType.CREATE;
                    case "ALTER" -> SqlType.ALTER;
                    case "DROP" -> SqlType.DROP;
                    case "TRUNCATE" -> SqlType.TRUNCATE;
                    case "GRANT" -> SqlType.GRANT;
                    case "REVOKE" -> SqlType.REVOKE;
                    case "SHOW" -> SqlType.SHOW;
                    case "EXPLAIN" -> SqlType.EXPLAIN;
                    case "DESCRIBE", "DESC" -> SqlType.DESCRIBE;
                    case "USE" -> SqlType.USE;
                    case "SET" -> SqlType.SET;
                    case "BEGIN", "START" -> SqlType.BEGIN;
                    case "COMMIT" -> SqlType.COMMIT;
                    case "ROLLBACK" -> SqlType.ROLLBACK;
                    default -> SqlType.UNKNOWN;
                };
            }
        }
        return SqlType.UNKNOWN;
    }

    private static class CollectingErrorListener extends BaseErrorListener {
        private final List<SqlError> errors;

        CollectingErrorListener(List<SqlError> errors) {
            this.errors = errors;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg,
                                RecognitionException e) {
            errors.add(new SqlError(line, charPositionInLine, msg));
        }
    }
}
