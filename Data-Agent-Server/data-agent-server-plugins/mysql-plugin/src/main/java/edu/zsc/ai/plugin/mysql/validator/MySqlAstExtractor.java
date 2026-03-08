package edu.zsc.ai.plugin.mysql.validator;

import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.mysql.parser.MySqlParser;
import edu.zsc.ai.plugin.mysql.parser.MySqlParserBaseVisitor;
import lombok.Getter;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * Visits the ANTLR parse tree to extract SqlType, referenced table names, and column names.
 * Also builds alias→table mapping and column→table attribution.
 */
public class MySqlAstExtractor extends MySqlParserBaseVisitor<Void> {

    @Getter
    private SqlType sqlType = SqlType.UNKNOWN;
    private final Set<String> tables = new LinkedHashSet<>();
    private final Set<String> columns = new LinkedHashSet<>();
    @Getter
    private final Map<String, String> aliasMap = new LinkedHashMap<>();       // alias → real table name
    private final Map<String, String> rawColumnQualifierMap = new LinkedHashMap<>();  // column name → raw qualifier
    private boolean insideAtomTableItem = false;

    public List<String> getTables() {
        return new ArrayList<>(tables);
    }

    public List<String> getColumns() {
        return new ArrayList<>(columns);
    }

    public Map<String, String> getColumnTableMap() {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawColumnQualifierMap.entrySet()) {
            String qualifier = entry.getValue();
            resolved.put(entry.getKey(), aliasMap.getOrDefault(qualifier, qualifier));
        }
        return resolved;
    }

    // --- SQL Type detection from top-level statement alternatives ---

    @Override
    public Void visitDmlStatement(MySqlParser.DmlStatementContext ctx) {
        if (ctx.selectStatement() != null || ctx.withStatement() != null || ctx.tableStatement() != null) {
            sqlType = SqlType.SELECT;
        } else if (ctx.insertStatement() != null || ctx.replaceStatement() != null) {
            sqlType = SqlType.INSERT;
        } else if (ctx.updateStatement() != null) {
            sqlType = SqlType.UPDATE;
        } else if (ctx.deleteStatement() != null) {
            sqlType = SqlType.DELETE;
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitDdlStatement(MySqlParser.DdlStatementContext ctx) {
        String text = ctx.getStart().getText().toUpperCase();
        sqlType = switch (text) {
            case "CREATE" -> SqlType.CREATE;
            case "ALTER" -> SqlType.ALTER;
            case "DROP" -> SqlType.DROP;
            case "TRUNCATE" -> SqlType.TRUNCATE;
            case "RENAME" -> SqlType.ALTER;
            default -> SqlType.UNKNOWN;
        };
        return visitChildren(ctx);
    }

    @Override
    public Void visitTransactionStatement(MySqlParser.TransactionStatementContext ctx) {
        String text = ctx.getStart().getText().toUpperCase();
        sqlType = switch (text) {
            case "START", "BEGIN" -> SqlType.BEGIN;
            case "COMMIT" -> SqlType.COMMIT;
            case "ROLLBACK" -> SqlType.ROLLBACK;
            default -> SqlType.UNKNOWN;
        };
        return visitChildren(ctx);
    }

    @Override
    public Void visitAdministrationStatement(MySqlParser.AdministrationStatementContext ctx) {
        if (ctx.showStatement() != null) {
            sqlType = SqlType.SHOW;
        } else if (ctx.setStatement() != null) {
            sqlType = SqlType.SET;
        } else if (ctx.grantStatement() != null || ctx.grantProxy() != null) {
            sqlType = SqlType.GRANT;
        } else if (ctx.revokeStatement() != null || ctx.revokeProxy() != null) {
            sqlType = SqlType.REVOKE;
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitUtilityStatement(MySqlParser.UtilityStatementContext ctx) {
        if (ctx.simpleDescribeStatement() != null || ctx.fullDescribeStatement() != null) {
            sqlType = SqlType.DESCRIBE;
        } else if (ctx.useStatement() != null) {
            sqlType = SqlType.USE;
        }
        return visitChildren(ctx);
    }

    // --- Extract table names with alias support ---

    @Override
    public Void visitAtomTableItem(MySqlParser.AtomTableItemContext ctx) {
        String tableName = extractTableName(ctx.tableName());
        tables.add(tableName);

        MySqlParser.UidContext aliasCtx = ctx.alias;
        if (aliasCtx != null) {
            aliasMap.put(aliasCtx.getText(), tableName);
        }

        insideAtomTableItem = true;
        visitChildren(ctx);
        insideAtomTableItem = false;
        return null;
    }

    @Override
    public Void visitTableName(MySqlParser.TableNameContext ctx) {
        if (!insideAtomTableItem) {
            tables.add(extractTableName(ctx));
        }
        return visitChildren(ctx);
    }

    // --- Extract column names with table attribution ---

    @Override
    public Void visitFullColumnName(MySqlParser.FullColumnNameContext ctx) {
        columns.add(ctx.getText());

        List<MySqlParser.DottedIdContext> dottedIds = ctx.dottedId();
        if (ctx.uid() != null && dottedIds != null) {
            if (dottedIds.size() == 1) {
                // qualifier.column pattern (e.g. u.name)
                String qualifier = ctx.uid().getText();
                String columnName = extractDottedIdText(dottedIds.get(0));
                rawColumnQualifierMap.put(columnName, qualifier);
            } else if (dottedIds.size() == 2) {
                // schema.table.column pattern (e.g. mydb.users.name)
                String tablePart = extractDottedIdText(dottedIds.get(0));
                String columnName = extractDottedIdText(dottedIds.get(1));
                rawColumnQualifierMap.put(columnName, tablePart);
            }
            // size == 0: unqualified column, no entry in rawColumnQualifierMap
        }

        return visitChildren(ctx);
    }

    // --- Helper methods ---

    private String extractTableName(MySqlParser.TableNameContext ctx) {
        MySqlParser.FullIdContext fullId = ctx.fullId();
        List<MySqlParser.UidContext> uids = fullId.uid();

        if (uids.size() == 2) {
            // schema.table → take table part
            return uids.get(1).getText();
        }

        TerminalNode dotId = fullId.DOT_ID();
        if (dotId != null) {
            // DOT_ID token like ".table" → strip leading dot
            return dotId.getText().substring(1);
        }

        // simple table name
        return uids.get(0).getText();
    }

    private String extractDottedIdText(MySqlParser.DottedIdContext ctx) {
        TerminalNode dotId = ctx.DOT_ID();
        if (dotId != null) {
            return dotId.getText().substring(1);
        }
        // '.' uid alternative
        return ctx.uid().getText();
    }
}
