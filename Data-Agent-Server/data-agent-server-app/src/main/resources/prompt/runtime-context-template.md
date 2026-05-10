<系统上下文 purpose="运行时环境" apply_to="时间解释" strength="参考">
{{SYSTEM_CONTEXT}}
</系统上下文>

<当前会话记忆 purpose="当前任务工作记忆" apply_to="范围追踪,任务延续" strength="最高">
{{CURRENT_CONVERSATION_MEMORY}}
</当前会话记忆>

<范围提示 purpose="查询范围提示" apply_to="工具选择,对象检索,SQL范围" strength="强">
{{SCOPE_HINTS}}
</范围提示>

<回答偏好 purpose="最终回答偏好" apply_to="语言,格式,可视化" strength="默认">
{{RESPONSE_PREFERENCES}}
</回答偏好>

<持久事实 purpose="已验证背景事实" apply_to="推理,SQL生成" strength="参考">
{{DURABLE_FACTS}}
</持久事实>

<显式引用 purpose="用户显式引用对象" apply_to="范围确定,对象优先级" strength="最高">
{{EXPLICIT_REFERENCES}}
</显式引用>
