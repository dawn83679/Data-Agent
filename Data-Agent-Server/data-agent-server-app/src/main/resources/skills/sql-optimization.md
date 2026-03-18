# SQL Optimization Rules

## When to Optimize
- 3+ table JOINs
- Correlated or nested subqueries
- User explicitly requests optimization
- existingSql is provided for review

## Optimization Checklist

### 1. Index Utilization
- Verify WHERE/JOIN columns have indexes
- Composite index: column order must match query filter order (leftmost prefix rule)
- Avoid functions on indexed columns: `WHERE YEAR(created_at) = 2024` → `WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'`
- LIKE with leading wildcard (`LIKE '%foo'`) cannot use index

### 2. JOIN Optimization
- Smaller table as driving table (or let optimizer decide)
- Ensure JOIN columns have matching types — implicit cast kills index
- Replace subquery with JOIN when possible: `WHERE id IN (SELECT ...)` → `JOIN ... ON`
- Avoid unnecessary CROSS JOINs / cartesian products

### 3. Subquery Rewrite
- Correlated subquery → JOIN or window function
- `EXISTS` vs `IN`: use EXISTS for large subquery result sets, IN for small sets
- Scalar subquery in SELECT → LEFT JOIN with aggregation

### 4. Aggregation & Sorting
- Avoid `SELECT DISTINCT` to mask duplicate JOIN — fix the JOIN logic instead
- `ORDER BY` on non-indexed column with large result → consider index or limit first
- `GROUP BY` with `HAVING` — move filterable conditions to `WHERE` when possible

### 5. Data Volume Control
- Large table without WHERE/LIMIT is a red flag
- Pagination: `OFFSET` on large datasets is slow → use keyset pagination (`WHERE id > last_id`)
- Batch operations (INSERT/UPDATE/DELETE > 1000 rows): split into batches

### 6. Execution Plan Hints
- Suggest `EXPLAIN` / `EXPLAIN ANALYZE` for production validation
- Watch for: full table scan, filesort, temporary table, nested loop on large sets

## Output Format
When optimization is applied, provide:
- `optimizedSql`: the rewritten SQL
- `optimizationPoints[]`: list of specific changes made and why
  - Example: "Replaced correlated subquery with LEFT JOIN — avoids N+1 execution"
  - Example: "Added composite index suggestion: idx_orders_user_date(user_id, created_at)"
