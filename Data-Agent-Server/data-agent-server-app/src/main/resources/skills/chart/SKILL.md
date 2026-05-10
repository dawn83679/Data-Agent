---
name: chart
description: 当模型需要把已验证表格数据通过 renderChart 作为最终图表答案交付时使用；包含图表选择规则、ECharts payload 模板和失败规避指导。
metadata:
  short-description: 渲染最终图表输出
---

# 图表可视化规则

## 原则
- 数据优先：先取得查询结果，再决定图表类型。
- 一个图表，一个答案：只渲染一个能直接回答问题的图表。
- 前端会自动处理主题颜色，不要设置 backgroundColor 或 textStyle.color。
- 图表渲染后立即停止，不要再用文本复述数据或解释图表。

## 图表类型选择
| 数据形态 | 图表类型 | 使用场景 |
|---|---|---|
| 时间序列 / 趋势 | LINE | 数据随时间变化 |
| 类别对比 | BAR | 跨类别比较数值 |
| 部分占整体 | PIE | 展示占比或百分比 |
| 相关性 | SCATTER | 展示两个变量之间的关系 |
| 趋势 + 规模 | AREA | 类似 LINE，但强调量级 |

## ECharts Option 模板

### LINE（chartType = "LINE"）
```json
{
  "title": { "text": "标题" },
  "tooltip": { "trigger": "axis" },
  "legend": { "data": ["系列 A"] },
  "xAxis": { "type": "category", "data": ["一月", "二月", "三月"] },
  "yAxis": { "type": "value" },
  "series": [
    { "name": "系列 A", "type": "line", "data": [120, 200, 150] }
  ]
}
```

### BAR（chartType = "BAR"）
```json
{
  "title": { "text": "标题" },
  "tooltip": { "trigger": "axis" },
  "legend": { "data": ["系列 A"] },
  "xAxis": { "type": "category", "data": ["第一季度", "第二季度", "第三季度"] },
  "yAxis": { "type": "value" },
  "series": [
    { "name": "系列 A", "type": "bar", "data": [300, 500, 400] }
  ]
}
```

### PIE（chartType = "PIE"）
```json
{
  "title": { "text": "标题" },
  "tooltip": { "trigger": "item" },
  "legend": { "data": ["类别 A", "类别 B", "类别 C"] },
  "series": [
    {
      "name": "分布",
      "type": "pie",
      "radius": "55%",
      "data": [
        { "name": "类别 A", "value": 335 },
        { "name": "类别 B", "value": 310 },
        { "name": "类别 C", "value": 234 }
      ]
    }
  ]
}
```

### SCATTER（chartType = "SCATTER"）
```json
{
  "title": { "text": "标题" },
  "tooltip": { "trigger": "item" },
  "xAxis": { "type": "value" },
  "yAxis": { "type": "value" },
  "series": [
    {
      "name": "相关性",
      "type": "scatter",
      "data": [[10, 8.04], [8, 6.95], [13, 7.58], [9, 8.81]]
    }
  ]
}
```

### AREA（chartType = "AREA"）
```json
{
  "title": { "text": "标题" },
  "tooltip": { "trigger": "axis" },
  "legend": { "data": ["系列 A"] },
  "xAxis": { "type": "category", "data": ["一月", "二月", "三月"] },
  "yAxis": { "type": "value" },
  "series": [
    {
      "name": "系列 A",
      "type": "line",
      "areaStyle": {},
      "data": [120, 200, 150]
    }
  ]
}
```

## 常见错误
- PIE 同时设置 xAxis 或 yAxis。
- series.type 与 chartType 不匹配。
- JSON 无效。
- data 数组为空。
- 数据格式不符合图表类型要求。
- legend.data 与 series[].name 不匹配。
- 设置 backgroundColor 或 textStyle.color。
