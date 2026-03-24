<system_context purpose="runtime_environment" apply_to="time_interpretation" strength="reference">
{{SYSTEM_CONTEXT}}
</system_context>

<task purpose="current_user_goal" apply_to="planning,answer_target" strength="highest">
{{TASK}}
</task>

<response_preferences purpose="final_response_preferences" apply_to="language,format,visualization" strength="default">
{{RESPONSE_PREFERENCES}}
</response_preferences>

<scope_hints purpose="query_scope_guidance" apply_to="tool_selection,object_search,sql_scope" strength="strong">
{{SCOPE_HINTS}}
</scope_hints>

<durable_facts purpose="verified_background_facts" apply_to="reasoning,sql_generation" strength="reference">
{{DURABLE_FACTS}}
</durable_facts>

<explicit_references purpose="user_explicit_object_selection" apply_to="scope_resolution,object_priority" strength="highest">
{{EXPLICIT_REFERENCES}}
</explicit_references>
