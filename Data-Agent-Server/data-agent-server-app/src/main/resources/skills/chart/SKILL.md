---
name: chart
description: Use when the model needs to turn verified tabular data into a final chart answer with renderChart; includes chart selection rules, ECharts payload templates, and failure-avoidance guidance.
metadata:
  short-description: Render final chart output
---

# Chart Visualization Rules

## Principles
- Data first: get query results before deciding chart type
- One chart, one answer: render ONE chart that directly answers the question
- Frontend auto-handles theme colors — do NOT set backgroundColor or textStyle.color
- After rendering chart, STOP — do not repeat data or explain chart in text

## Chart Type Selection
| Data Shape | Chart Type | When to Use |
|---|---|---|
| Time series / trends | LINE | Data changes over time |
| Category comparison | BAR | Compare values across categories |
| Part-to-whole | PIE | Show proportions/percentages |
| Correlation | SCATTER | Show relationship between two variables |
| Trend + volume | AREA | Like LINE but emphasizing magnitude |

## ECharts Option Templates

### LINE (chartType = "LINE")
```json
{
  "title": { "text": "Title" },
  "tooltip": { "trigger": "axis" },
  "legend": { "data": ["Series A"] },
  "xAxis": { "type": "category", "data": ["Jan", "Feb", "Mar"] },
  "yAxis": { "type": "value" },
  "series": [
    { "name": "Series A", "type": "line", "data": [120, 200, 150] }
  ]
}
```

### BAR (chartType = "BAR")
```json
{
  "title": { "text": "Title" },
  "tooltip": { "trigger": "axis" },
  "legend": { "data": ["Series A"] },
  "xAxis": { "type": "category", "data": ["Q1", "Q2", "Q3"] },
  "yAxis": { "type": "value" },
  "series": [
    { "name": "Series A", "type": "bar", "data": [300, 500, 400] }
  ]
}
```

### PIE (chartType = "PIE")
```json
{
  "title": { "text": "Title" },
  "tooltip": { "trigger": "item" },
  "legend": { "data": ["Cat A", "Cat B", "Cat C"] },
  "series": [
    {
      "name": "Distribution",
      "type": "pie",
      "radius": "55%",
      "data": [
        { "name": "Cat A", "value": 335 },
        { "name": "Cat B", "value": 310 },
        { "name": "Cat C", "value": 234 }
      ]
    }
  ]
}
```

### SCATTER (chartType = "SCATTER")
```json
{
  "title": { "text": "Title" },
  "tooltip": { "trigger": "item" },
  "xAxis": { "type": "value" },
  "yAxis": { "type": "value" },
  "series": [
    {
      "name": "Correlation",
      "type": "scatter",
      "data": [[10, 8.04], [8, 6.95], [13, 7.58], [9, 8.81]]
    }
  ]
}
```

### AREA (chartType = "AREA")
```json
{
  "title": { "text": "Title" },
  "tooltip": { "trigger": "axis" },
  "legend": { "data": ["Series A"] },
  "xAxis": { "type": "category", "data": ["Jan", "Feb", "Mar"] },
  "yAxis": { "type": "value" },
  "series": [
    {
      "name": "Series A",
      "type": "line",
      "areaStyle": {},
      "data": [120, 200, 150]
    }
  ]
}
```

## Common Pitfalls
- PIE with xAxis/yAxis
- series.type mismatch with chartType
- Invalid JSON
- Empty data arrays
- Wrong data format by chart type
- legend.data not matching series[].name
- Setting backgroundColor or textStyle.color
