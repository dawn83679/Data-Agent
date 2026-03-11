<sql-shared-rules>
CRITICAL: SQL 必须使用全限定名（schema.table 或 database.schema.table）。
CRITICAL: 尊重 dbType — 使用正确的 SQL 方言：
  - PostgreSQL: LIMIT, INTERVAL '30 days', :: 类型转换
  - MySQL: LIMIT, INTERVAL 30 DAY, 无 :: 转换
  - Oracle: FETCH FIRST / ROWNUM, 无 LIMIT
  - SQL Server: TOP, DATEADD

常见错误防范：
- 笛卡尔积：多表查询必须有明确的 JOIN 和 ON 子句。
- 错误 JOIN 类型：需保留左表所有行时用 LEFT JOIN。
- GROUP BY 遗漏：SELECT 中未聚合列必须出现在 GROUP BY 中。
- NULL 陷阱：WHERE col != 'x' 不返回 NULL 行。需要时用 IS NULL / COALESCE。
- DISTINCT 掩盖问题：先查 JOIN 逻辑，不要直接加 DISTINCT。
- 全表扫描：>1 万行必须包含 WHERE 或 LIMIT。
- SELECT *：除非用户明确要求所有列，否则优先明确列列表。

写操作规则：
- askUserConfirm -> executeNonSelectSql。此顺序不可跳过或颠倒。
- UPDATE/DELETE 没有 WHERE：除非用户明确确认全表操作，否则禁止。
- FK 级联：DELETE 前检查外键。CASCADE 可能静默删除子表行 — 量化影响。
- UNIQUE/NOT NULL：INSERT/UPDATE 前检查约束。
- 大批量操作（>1000 行）：建议分批或事务。
- DROP/TRUNCATE：明确警告不可逆数据丢失。
</sql-shared-rules>
