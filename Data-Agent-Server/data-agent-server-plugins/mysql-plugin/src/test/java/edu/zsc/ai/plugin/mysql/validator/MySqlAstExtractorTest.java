package edu.zsc.ai.plugin.mysql.validator;

import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.mysql.parser.MySqlLexer;
import edu.zsc.ai.plugin.mysql.parser.MySqlParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MySqlAstExtractorTest {

    private MySqlAstExtractor extract(String sql) {
        MySqlLexer lexer = new MySqlLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MySqlParser parser = new MySqlParser(tokens);
        MySqlParser.RootContext tree = parser.root();

        MySqlAstExtractor extractor = new MySqlAstExtractor();
        extractor.visit(tree);
        return extractor;
    }

    // ==================== SqlType detection ====================

    @Test
    void testSqlType_Select() {
        MySqlAstExtractor ext = extract("SELECT * FROM users");
        assertEquals(SqlType.SELECT, ext.getSqlType());
    }

    @Test
    void testSqlType_WithCte() {
        MySqlAstExtractor ext = extract("WITH cte AS (SELECT 1) SELECT * FROM cte");
        assertEquals(SqlType.SELECT, ext.getSqlType());
    }

    @Test
    void testSqlType_Insert() {
        MySqlAstExtractor ext = extract("INSERT INTO users (name) VALUES ('test')");
        assertEquals(SqlType.INSERT, ext.getSqlType());
    }

    @Test
    void testSqlType_Replace() {
        MySqlAstExtractor ext = extract("REPLACE INTO users (id, name) VALUES (1, 'test')");
        assertEquals(SqlType.INSERT, ext.getSqlType());
    }

    @Test
    void testSqlType_Update() {
        MySqlAstExtractor ext = extract("UPDATE users SET name = 'new' WHERE id = 1");
        assertEquals(SqlType.UPDATE, ext.getSqlType());
    }

    @Test
    void testSqlType_Delete() {
        MySqlAstExtractor ext = extract("DELETE FROM users WHERE id = 1");
        assertEquals(SqlType.DELETE, ext.getSqlType());
    }

    @Test
    void testSqlType_CreateTable() {
        MySqlAstExtractor ext = extract("CREATE TABLE t (id INT PRIMARY KEY)");
        assertEquals(SqlType.CREATE, ext.getSqlType());
    }

    @Test
    void testSqlType_AlterTable() {
        MySqlAstExtractor ext = extract("ALTER TABLE t ADD COLUMN x INT");
        assertEquals(SqlType.ALTER, ext.getSqlType());
    }

    @Test
    void testSqlType_DropTable() {
        MySqlAstExtractor ext = extract("DROP TABLE t");
        assertEquals(SqlType.DROP, ext.getSqlType());
    }

    @Test
    void testSqlType_TruncateTable() {
        MySqlAstExtractor ext = extract("TRUNCATE TABLE t");
        assertEquals(SqlType.TRUNCATE, ext.getSqlType());
    }

    @Test
    void testSqlType_RenameTable() {
        MySqlAstExtractor ext = extract("RENAME TABLE t TO t2");
        assertEquals(SqlType.ALTER, ext.getSqlType());
    }

    @Test
    void testSqlType_ShowTables() {
        MySqlAstExtractor ext = extract("SHOW TABLES");
        assertEquals(SqlType.SHOW, ext.getSqlType());
    }

    @Test
    void testSqlType_SetVariable() {
        MySqlAstExtractor ext = extract("SET @var = 1");
        assertEquals(SqlType.SET, ext.getSqlType());
    }

    @Test
    void testSqlType_StartTransaction() {
        MySqlAstExtractor ext = extract("START TRANSACTION");
        assertEquals(SqlType.BEGIN, ext.getSqlType());
    }

    @Test
    void testSqlType_Commit() {
        MySqlAstExtractor ext = extract("COMMIT");
        assertEquals(SqlType.COMMIT, ext.getSqlType());
    }

    @Test
    void testSqlType_Rollback() {
        MySqlAstExtractor ext = extract("ROLLBACK");
        assertEquals(SqlType.ROLLBACK, ext.getSqlType());
    }

    @Test
    void testSqlType_DescribeTable() {
        MySqlAstExtractor ext = extract("DESCRIBE users");
        assertEquals(SqlType.DESCRIBE, ext.getSqlType());
    }

    @Test
    void testSqlType_UseDatabase() {
        MySqlAstExtractor ext = extract("USE test_db");
        assertEquals(SqlType.USE, ext.getSqlType());
    }

    // ==================== Table extraction ====================

    @Test
    void testTables_SingleTable() {
        MySqlAstExtractor ext = extract("SELECT * FROM users");
        List<String> tables = ext.getTables();
        assertTrue(tables.contains("users"));
    }

    @Test
    void testTables_MultipleTables() {
        MySqlAstExtractor ext = extract(
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id");
        List<String> tables = ext.getTables();
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("orders"));
    }

    @Test
    void testTables_Subquery() {
        MySqlAstExtractor ext = extract(
                "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)");
        List<String> tables = ext.getTables();
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("orders"));
    }

    @Test
    void testTables_InsertInto() {
        MySqlAstExtractor ext = extract("INSERT INTO products (name) VALUES ('x')");
        assertTrue(ext.getTables().contains("products"));
    }

    @Test
    void testTables_UpdateTable() {
        MySqlAstExtractor ext = extract("UPDATE orders SET status = 'done' WHERE id = 1");
        assertTrue(ext.getTables().contains("orders"));
    }

    @Test
    void testTables_DeleteFrom() {
        MySqlAstExtractor ext = extract("DELETE FROM logs WHERE created < '2024-01-01'");
        assertTrue(ext.getTables().contains("logs"));
    }

    @Test
    void testTables_NoDuplicates() {
        MySqlAstExtractor ext = extract(
                "SELECT * FROM users u1 JOIN users u2 ON u1.id = u2.manager_id");
        List<String> tables = ext.getTables();
        assertEquals(1, tables.stream().filter("users"::equals).count());
    }

    // ==================== Column extraction ====================

    @Test
    void testColumns_SelectColumns() {
        MySqlAstExtractor ext = extract("SELECT name, email FROM users");
        List<String> columns = ext.getColumns();
        assertFalse(columns.isEmpty());
    }

    @Test
    void testColumns_QualifiedColumnName() {
        MySqlAstExtractor ext = extract("SELECT u.name FROM users u");
        List<String> columns = ext.getColumns();
        assertFalse(columns.isEmpty());
    }

    // ==================== Default state ====================

    @Test
    void testDefaultSqlType_IsUnknown() {
        MySqlAstExtractor ext = new MySqlAstExtractor();
        assertEquals(SqlType.UNKNOWN, ext.getSqlType());
    }

    @Test
    void testDefaultTables_IsEmpty() {
        MySqlAstExtractor ext = new MySqlAstExtractor();
        assertTrue(ext.getTables().isEmpty());
    }

    @Test
    void testDefaultColumns_IsEmpty() {
        MySqlAstExtractor ext = new MySqlAstExtractor();
        assertTrue(ext.getColumns().isEmpty());
    }

    // ==================== Alias mapping ====================

    @Test
    void testAliasMap_BasicAlias() {
        MySqlAstExtractor ext = extract("SELECT u.name FROM users u");
        Map<String, String> aliases = ext.getAliasMap();
        assertEquals("users", aliases.get("u"));
    }

    @Test
    void testAliasMap_AsKeyword() {
        MySqlAstExtractor ext = extract("SELECT u.name FROM users AS u");
        Map<String, String> aliases = ext.getAliasMap();
        assertEquals("users", aliases.get("u"));
    }

    @Test
    void testAliasMap_JoinAliases() {
        MySqlAstExtractor ext = extract(
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id");
        Map<String, String> aliases = ext.getAliasMap();
        assertEquals("users", aliases.get("u"));
        assertEquals("orders", aliases.get("o"));
    }

    // ==================== Column-table mapping ====================

    @Test
    void testColumnTableMap_QualifiedColumn() {
        MySqlAstExtractor ext = extract("SELECT u.name FROM users u");
        Map<String, String> ctm = ext.getColumnTableMap();
        assertEquals("users", ctm.get("name"));
    }

    @Test
    void testColumnTableMap_UnqualifiedColumn() {
        MySqlAstExtractor ext = extract("SELECT name FROM users");
        Map<String, String> ctm = ext.getColumnTableMap();
        assertFalse(ctm.containsKey("name"));
    }

    @Test
    void testColumnTableMap_ThreePartColumnName() {
        MySqlAstExtractor ext = extract("SELECT mydb.users.name FROM mydb.users");
        Map<String, String> ctm = ext.getColumnTableMap();
        assertEquals("users", ctm.get("name"));
    }

    @Test
    void testColumnTableMap_SameColumnDifferentTables() {
        MySqlAstExtractor ext = extract(
                "SELECT u.id, o.id FROM users u JOIN orders o ON u.id = o.user_id");
        Map<String, String> ctm = ext.getColumnTableMap();
        // last occurrence wins (known limitation); ON clause u.id overwrites SELECT o.id
        assertEquals("users", ctm.get("id"));
        assertEquals("orders", ctm.get("user_id"));
    }

    // ==================== Schema-qualified table names ====================

    @Test
    void testTables_SchemaQualifiedTableName() {
        MySqlAstExtractor ext = extract("SELECT * FROM mydb.users");
        List<String> tables = ext.getTables();
        assertTrue(tables.contains("users"));
        assertFalse(tables.contains("mydb.users"));
    }

    // ==================== Backward compatibility ====================

    @Test
    void testBackwardCompat_GetColumnsStillReturnsRawText() {
        MySqlAstExtractor ext = extract("SELECT u.name FROM users u");
        List<String> columns = ext.getColumns();
        assertTrue(columns.contains("u.name"));
    }
}
