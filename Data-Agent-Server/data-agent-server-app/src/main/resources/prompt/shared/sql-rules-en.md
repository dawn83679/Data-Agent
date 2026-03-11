<sql-shared-rules>
CRITICAL: SQL must use fully-qualified names (schema.table or database.schema.table).
CRITICAL: Respect dbType — use correct SQL dialect:
  - PostgreSQL: LIMIT, INTERVAL '30 days', :: for casts
  - MySQL: LIMIT, INTERVAL 30 DAY, no :: casts
  - Oracle: FETCH FIRST / ROWNUM, no LIMIT
  - SQL Server: TOP, DATEADD

Prevent these common errors:
- Cartesian product: multi-table queries MUST have explicit JOIN with ON clause.
- Wrong JOIN type: LEFT JOIN when all left-table rows must be preserved.
- GROUP BY omission: non-aggregated SELECT columns must appear in GROUP BY.
- NULL trap: WHERE col != 'x' excludes NULLs. Use IS NULL / COALESCE when needed.
- DISTINCT as band-aid: fix JOIN logic for duplicates first.
- Full table scan: >10k rows MUST include WHERE or LIMIT.
- SELECT *: prefer explicit column lists unless user explicitly asked for all.

Write operation rules:
- askUserConfirm -> executeNonSelectSql. This order MUST NOT be skipped or reversed.
- UPDATE/DELETE without WHERE: forbidden unless user explicitly confirms full-table intent.
- FK cascade: check foreign keys before DELETE. CASCADE may silently delete child rows — quantify impact.
- UNIQUE/NOT NULL: check constraints before INSERT/UPDATE.
- Large modifications (>1000 rows): suggest batching or transaction.
- DROP/TRUNCATE: explicitly warn about irreversible data loss.
</sql-shared-rules>
