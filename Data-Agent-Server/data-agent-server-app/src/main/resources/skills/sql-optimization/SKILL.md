---
name: sql-optimization
description: Use when the model needs to optimize or rewrite complex SQL; includes join, index, subquery, sorting, aggregation, and execution-plan guidance.
metadata:
  short-description: Optimize complex SQL
---

# SQL Optimization Rules

## When to Optimize
- 3+ table JOINs
- Correlated or nested subqueries
- User explicitly requests optimization
- existingSql is provided for review

## Optimization Checklist

### 1. Index Utilization
- Verify WHERE/JOIN columns have indexes
- Composite index column order must match query filter order
- Avoid functions on indexed columns
- LIKE with a leading wildcard cannot use index

### 2. JOIN Optimization
- Smaller table as driving table when it helps
- Ensure JOIN columns have matching types
- Replace subquery with JOIN when possible
- Avoid unnecessary CROSS JOINs / cartesian products

### 3. Subquery Rewrite
- Correlated subquery -> JOIN or window function
- `EXISTS` vs `IN`: EXISTS for large subquery result sets, IN for small sets
- Scalar subquery in SELECT -> LEFT JOIN with aggregation

### 4. Aggregation & Sorting
- Avoid `SELECT DISTINCT` as a band-aid for bad JOINs
- Consider indexes or LIMIT before `ORDER BY` on huge datasets
- Move filterable conditions from `HAVING` to `WHERE` when possible

### 5. Data Volume Control
- Large table without WHERE/LIMIT is a red flag
- On large pagination, prefer keyset pagination over OFFSET
- Split very large batch writes into smaller chunks

### 6. Execution Plan Hints
- Suggest `EXPLAIN` / `EXPLAIN ANALYZE`
- Watch for: full table scan, filesort, temporary table, nested loop on large sets

## Output Format
When optimization is applied, provide:
- `optimizedSql`
- `optimizationPoints[]`
