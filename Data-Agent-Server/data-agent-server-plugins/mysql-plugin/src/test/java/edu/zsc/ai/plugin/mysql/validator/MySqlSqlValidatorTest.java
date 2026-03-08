package edu.zsc.ai.plugin.mysql.validator;

import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.model.sql.SqlValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MySqlSqlValidatorTest {

    private final MySqlSqlValidator validator = new MySqlSqlValidator();

    // ==================== validate(): valid SQL ====================

    @Test
    void testValidate_Select() {
        SqlValidationResult result = validator.validate("SELECT id, name FROM users WHERE id = 1");
        assertTrue(result.valid());
        assertEquals(SqlType.SELECT, result.sqlType());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.tables().contains("users"));
    }

    @Test
    void testValidate_SelectWithJoin() {
        SqlValidationResult result = validator.validate(
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id");
        assertTrue(result.valid());
        assertEquals(SqlType.SELECT, result.sqlType());
        assertTrue(result.tables().contains("users"));
        assertTrue(result.tables().contains("orders"));
    }

    @Test
    void testValidate_SelectWithSubquery() {
        SqlValidationResult result = validator.validate(
                "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)");
        assertTrue(result.valid());
        assertEquals(SqlType.SELECT, result.sqlType());
        assertTrue(result.tables().contains("users"));
        assertTrue(result.tables().contains("orders"));
    }

    @Test
    void testValidate_WithCte() {
        SqlValidationResult result = validator.validate(
                "WITH cte AS (SELECT id FROM users) SELECT * FROM cte");
        assertTrue(result.valid());
        assertEquals(SqlType.SELECT, result.sqlType());
    }

    @Test
    void testValidate_Insert() {
        SqlValidationResult result = validator.validate(
                "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')");
        assertTrue(result.valid());
        assertEquals(SqlType.INSERT, result.sqlType());
        assertTrue(result.tables().contains("users"));
    }

    @Test
    void testValidate_Replace() {
        SqlValidationResult result = validator.validate(
                "REPLACE INTO users (id, name) VALUES (1, 'test')");
        assertTrue(result.valid());
        assertEquals(SqlType.INSERT, result.sqlType());
        assertTrue(result.tables().contains("users"));
    }

    @Test
    void testValidate_Update() {
        SqlValidationResult result = validator.validate(
                "UPDATE users SET name = 'new_name' WHERE id = 1");
        assertTrue(result.valid());
        assertEquals(SqlType.UPDATE, result.sqlType());
        assertTrue(result.tables().contains("users"));
    }

    @Test
    void testValidate_Delete() {
        SqlValidationResult result = validator.validate(
                "DELETE FROM users WHERE id = 1");
        assertTrue(result.valid());
        assertEquals(SqlType.DELETE, result.sqlType());
        assertTrue(result.tables().contains("users"));
    }

    @Test
    void testValidate_CreateTable() {
        SqlValidationResult result = validator.validate(
                "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100))");
        assertTrue(result.valid());
        assertEquals(SqlType.CREATE, result.sqlType());
    }

    @Test
    void testValidate_AlterTable() {
        SqlValidationResult result = validator.validate(
                "ALTER TABLE users ADD COLUMN age INT");
        assertTrue(result.valid());
        assertEquals(SqlType.ALTER, result.sqlType());
    }

    @Test
    void testValidate_DropTable() {
        SqlValidationResult result = validator.validate("DROP TABLE IF EXISTS test_table");
        assertTrue(result.valid());
        assertEquals(SqlType.DROP, result.sqlType());
    }

    @Test
    void testValidate_TruncateTable() {
        SqlValidationResult result = validator.validate("TRUNCATE TABLE users");
        assertTrue(result.valid());
        assertEquals(SqlType.TRUNCATE, result.sqlType());
    }

    @Test
    void testValidate_ShowTables() {
        SqlValidationResult result = validator.validate("SHOW TABLES");
        assertTrue(result.valid());
        assertEquals(SqlType.SHOW, result.sqlType());
    }

    @Test
    void testValidate_SetVariable() {
        SqlValidationResult result = validator.validate("SET @var = 1");
        assertTrue(result.valid());
        assertEquals(SqlType.SET, result.sqlType());
    }

    @Test
    void testValidate_UseDatabase() {
        SqlValidationResult result = validator.validate("USE test_db");
        assertTrue(result.valid());
        assertEquals(SqlType.USE, result.sqlType());
    }

    @Test
    void testValidate_DescribeTable() {
        SqlValidationResult result = validator.validate("DESCRIBE users");
        assertTrue(result.valid());
        assertEquals(SqlType.DESCRIBE, result.sqlType());
    }

    @Test
    void testValidate_BeginTransaction() {
        SqlValidationResult result = validator.validate("START TRANSACTION");
        assertTrue(result.valid());
        assertEquals(SqlType.BEGIN, result.sqlType());
    }

    @Test
    void testValidate_Commit() {
        SqlValidationResult result = validator.validate("COMMIT");
        assertTrue(result.valid());
        assertEquals(SqlType.COMMIT, result.sqlType());
    }

    @Test
    void testValidate_Rollback() {
        SqlValidationResult result = validator.validate("ROLLBACK");
        assertTrue(result.valid());
        assertEquals(SqlType.ROLLBACK, result.sqlType());
    }

    // ==================== validate(): invalid SQL ====================

    @Test
    void testValidate_SyntaxError() {
        SqlValidationResult result = validator.validate("SELEC * FROM users");
        assertFalse(result.valid());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void testValidate_IncompleteStatement() {
        SqlValidationResult result = validator.validate("SELECT FROM");
        assertFalse(result.valid());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void testValidate_NullInput() {
        SqlValidationResult result = validator.validate(null);
        assertFalse(result.valid());
        assertEquals(SqlType.UNKNOWN, result.sqlType());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void testValidate_EmptyString() {
        SqlValidationResult result = validator.validate("");
        assertFalse(result.valid());
        assertEquals(SqlType.UNKNOWN, result.sqlType());
    }

    @Test
    void testValidate_BlankString() {
        SqlValidationResult result = validator.validate("   ");
        assertFalse(result.valid());
        assertEquals(SqlType.UNKNOWN, result.sqlType());
    }

    // ==================== validate(): error details ====================

    @Test
    void testValidate_ErrorContainsLineAndColumn() {
        SqlValidationResult result = validator.validate("SELEC * FROM users");
        assertFalse(result.valid());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().get(0).line() > 0);
        assertNotNull(result.errors().get(0).message());
    }

    // ==================== classifySql() ====================

    @Test
    void testClassifySql_Select() {
        assertEquals(SqlType.SELECT, validator.classifySql("SELECT * FROM users"));
    }

    @Test
    void testClassifySql_With() {
        assertEquals(SqlType.SELECT, validator.classifySql("WITH cte AS (SELECT 1) SELECT * FROM cte"));
    }

    @Test
    void testClassifySql_Insert() {
        assertEquals(SqlType.INSERT, validator.classifySql("INSERT INTO users VALUES (1)"));
    }

    @Test
    void testClassifySql_Replace() {
        assertEquals(SqlType.INSERT, validator.classifySql("REPLACE INTO users VALUES (1)"));
    }

    @Test
    void testClassifySql_Update() {
        assertEquals(SqlType.UPDATE, validator.classifySql("UPDATE users SET name='a'"));
    }

    @Test
    void testClassifySql_Delete() {
        assertEquals(SqlType.DELETE, validator.classifySql("DELETE FROM users WHERE id=1"));
    }

    @Test
    void testClassifySql_Create() {
        assertEquals(SqlType.CREATE, validator.classifySql("CREATE TABLE t (id INT)"));
    }

    @Test
    void testClassifySql_Alter() {
        assertEquals(SqlType.ALTER, validator.classifySql("ALTER TABLE t ADD COLUMN x INT"));
    }

    @Test
    void testClassifySql_Drop() {
        assertEquals(SqlType.DROP, validator.classifySql("DROP TABLE t"));
    }

    @Test
    void testClassifySql_Truncate() {
        assertEquals(SqlType.TRUNCATE, validator.classifySql("TRUNCATE TABLE t"));
    }

    @Test
    void testClassifySql_Grant() {
        assertEquals(SqlType.GRANT, validator.classifySql("GRANT SELECT ON db.* TO 'user'@'host'"));
    }

    @Test
    void testClassifySql_Revoke() {
        assertEquals(SqlType.REVOKE, validator.classifySql("REVOKE SELECT ON db.* FROM 'user'@'host'"));
    }

    @Test
    void testClassifySql_Show() {
        assertEquals(SqlType.SHOW, validator.classifySql("SHOW DATABASES"));
    }

    @Test
    void testClassifySql_Explain() {
        assertEquals(SqlType.EXPLAIN, validator.classifySql("EXPLAIN SELECT * FROM users"));
    }

    @Test
    void testClassifySql_Describe() {
        assertEquals(SqlType.DESCRIBE, validator.classifySql("DESCRIBE users"));
    }

    @Test
    void testClassifySql_Desc() {
        assertEquals(SqlType.DESCRIBE, validator.classifySql("DESC users"));
    }

    @Test
    void testClassifySql_Use() {
        assertEquals(SqlType.USE, validator.classifySql("USE test_db"));
    }

    @Test
    void testClassifySql_Set() {
        assertEquals(SqlType.SET, validator.classifySql("SET @var = 1"));
    }

    @Test
    void testClassifySql_Begin() {
        assertEquals(SqlType.BEGIN, validator.classifySql("BEGIN"));
    }

    @Test
    void testClassifySql_Start() {
        assertEquals(SqlType.BEGIN, validator.classifySql("START TRANSACTION"));
    }

    @Test
    void testClassifySql_Commit() {
        assertEquals(SqlType.COMMIT, validator.classifySql("COMMIT"));
    }

    @Test
    void testClassifySql_Rollback() {
        assertEquals(SqlType.ROLLBACK, validator.classifySql("ROLLBACK"));
    }

    @Test
    void testClassifySql_CaseInsensitive() {
        assertEquals(SqlType.SELECT, validator.classifySql("select * from users"));
    }

    @Test
    void testClassifySql_LeadingWhitespace() {
        assertEquals(SqlType.SELECT, validator.classifySql("   SELECT * FROM users"));
    }

    @Test
    void testClassifySql_LeadingComment() {
        assertEquals(SqlType.SELECT, validator.classifySql("/* comment */ SELECT * FROM users"));
    }

    @Test
    void testClassifySql_NullInput() {
        assertEquals(SqlType.UNKNOWN, validator.classifySql(null));
    }

    @Test
    void testClassifySql_EmptyInput() {
        assertEquals(SqlType.UNKNOWN, validator.classifySql(""));
    }

    @Test
    void testClassifySql_BlankInput() {
        assertEquals(SqlType.UNKNOWN, validator.classifySql("   "));
    }

    @Test
    void testClassifySql_UnknownKeyword() {
        assertEquals(SqlType.UNKNOWN, validator.classifySql("FOOBAR something"));
    }
}
