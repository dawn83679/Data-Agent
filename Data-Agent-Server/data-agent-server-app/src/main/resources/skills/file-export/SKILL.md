---
name: file-export
description: Use when the model should generate an exportFile-ready tabular payload for downloadable output; includes when-to-export rules and strict headers/rows shaping guidance.
metadata:
  short-description: Build exportFile payloads
---

# File Export Rules

## Principles
- Export only when the user clearly wants a downloadable artifact.
- Prefer inline answers for small results. Do not export by default.
- Keep the `exportFile` payload strictly tabular: one `headers` array and one `rows` matrix.
- After export succeeds, do not print the whole file content into chat.

## When To Use `exportFile`
- The user asks to export, download, save, or generate a file.
- The data is already verified and ready for delivery.
- A structured table is available or can be assembled safely.

## When Not To Use `exportFile`
- The user only needs a short inline answer.
- The data is incomplete or still unverified.
- The output is narrative prose rather than rows and columns.

## Payload Rules
- `format` must use a currently supported format. In this environment, use `CSV`.
- `headers` must be human-readable and in final display order.
- `rows` must match `headers` exactly in column count for every row.
- Convert missing values into explicit empty cells rather than shifting columns.
- Keep column order stable and meaningful for the user.

## Good Patterns
- Export verified query results directly, with light header relabeling only when it improves readability.
- Export compact summary tables such as `month, revenue, growth_rate` or `customer, order_count, total_amount`.
- If the user asks for Excel or PDF but only CSV is supported, explain the limitation and only export CSV if it still satisfies the request.

## Common Mistakes
- Exporting before the data is ready.
- Putting nested objects into cells without flattening them to stable text values.
- Returning rows with inconsistent column counts.
- Exporting when a short inline response would have been better.
