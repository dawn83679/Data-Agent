import { ref, watch } from 'vue'
import type { Ref } from 'vue'

/**
 * SQL编辑器组合式函数
 */
export function useSqlEditor(
  sqlEditorRef: Ref<HTMLTextAreaElement | undefined>,
  sqlHighlightRef: Ref<HTMLPreElement | undefined>,
  lineNumbersRef: Ref<HTMLElement | undefined>
) {
  const sqlContent = ref('')

  /**
   * 处理编辑器滚动事件
   */
  const handleEditorScroll = () => {
    if (!sqlEditorRef.value || !sqlHighlightRef.value) return
    
    sqlHighlightRef.value.scrollTop = sqlEditorRef.value.scrollTop
    sqlHighlightRef.value.scrollLeft = sqlEditorRef.value.scrollLeft
  }

  /**
   * 处理编辑器输入事件
   */
  const handleEditorInput = () => {
    updateLineNumbers()
    highlightSQL()
  }

  /**
   * 处理编辑器键盘事件
   */
  const handleEditorKeydown = (e: KeyboardEvent) => {
    // Ctrl/Cmd + Enter 执行查询
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      // 触发查询执行事件
      window.dispatchEvent(new CustomEvent('execute-query'))
      return
    }

    // Tab键处理
    if (e.key === 'Tab') {
      e.preventDefault()
      if (!sqlEditorRef.value) return
      
      const start = sqlEditorRef.value.selectionStart
      const end = sqlEditorRef.value.selectionEnd
      const value = sqlEditorRef.value.value
      
      sqlEditorRef.value.value = 
        value.substring(0, start) + 
        '    ' + 
        value.substring(end)
      
      sqlEditorRef.value.selectionStart = sqlEditorRef.value.selectionEnd = start + 4
      sqlContent.value = sqlEditorRef.value.value
      highlightSQL()
    }
  }

  /**
   * 更新行号
   */
  const updateLineNumbers = () => {
    if (!sqlEditorRef.value || !lineNumbersRef.value) return

    const lines = sqlEditorRef.value.value.split('\n').length
    let html = ''
    for (let i = 1; i <= lines; i++) {
      html += i + '<br>'
    }
    lineNumbersRef.value.innerHTML = html
  }

  /**
   * SQL语法高亮
   */
  const highlightSQL = () => {
    if (!sqlEditorRef.value || !sqlHighlightRef.value) return

    const sql = sqlEditorRef.value.value
    const highlighted = highlightSQLText(sql)
    sqlHighlightRef.value.innerHTML = highlighted
  }

  /**
   * 高亮SQL文本
   */
  const highlightSQLText = (sql: string): string => {
    if (!sql) return ''
    
    // SQL关键字（按长度降序排列，避免短关键字匹配长关键字的一部分）
    const keywords = [
      'AUTO_INCREMENT', 'CURRENT_TIMESTAMP', 'CURRENT_TIME', 'CURRENT_DATE',
      'SAVEPOINT', 'TRANSACTION', 'CONSTRAINT', 'REFERENCES', 'DESCRIBE',
      'SELECT', 'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'ALTER', 'DROP',
      'INNER', 'OUTER', 'TABLE', 'INDEX', 'VIEW', 'WHERE', 'JOIN', 'ON',
      'INTO', 'VALUES', 'FROM', 'ORDER', 'GROUP', 'HAVING', 'LIMIT',
      'OFFSET', 'UNION', 'EXISTS', 'BETWEEN', 'LIKE', 'WHEN', 'THEN',
      'ELSE', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'AND', 'OR', 'NOT',
      'NULL', 'DISTINCT', 'DATABASE', 'SCHEMA', 'EXPLAIN', 'PRIMARY',
      'FOREIGN', 'KEY', 'UNIQUE', 'CHECK', 'DEFAULT', 'TRIGGER',
      'PROCEDURE', 'FUNCTION', 'CURSOR', 'COMMIT', 'ROLLBACK', 'BEGIN',
      'LOCK', 'UNLOCK', 'GRANT', 'REVOKE', 'LEFT', 'RIGHT', 'FULL',
      'INNER', 'USE', 'SHOW', 'AS', 'IS', 'IN', 'IF'
    ].sort((a, b) => b.length - a.length)

    // 转义HTML（先转义，避免后续处理产生问题）
    let html = sql.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    
    // 使用占位符策略：先标记需要保护的区域（字符串和注释），再处理其他内容
    
    // 1. 先处理字符串（使用占位符保护）
    const stringPlaceholders: string[] = []
    html = html.replace(/'([^'\\]|\\.)*'|"([^"\\]|\\.)*"/g, (match) => {
      const placeholder = `__STRING_${stringPlaceholders.length}__`
      stringPlaceholders.push(match)
      return placeholder
    })
    
    // 2. 处理注释（使用占位符保护）
    const commentPlaceholders: string[] = []
    html = html.replace(/--.*$/gm, (match) => {
      const placeholder = `__COMMENT_LINE_${commentPlaceholders.length}__`
      commentPlaceholders.push(match)
      return placeholder
    })
    html = html.replace(/\/\*[\s\S]*?\*\//g, (match) => {
      const placeholder = `__COMMENT_BLOCK_${commentPlaceholders.length}__`
      commentPlaceholders.push(match)
      return placeholder
    })
    
    // 3. 高亮数字（不在字符串和注释中）
    html = html.replace(/\b\d+\.?\d*\b/g, (match) => {
      return `<span class="sql-number">${match}</span>`
    })
    
    // 4. 高亮关键字（不在字符串和注释中，因为已经用占位符替换了）
    // 按长度降序排列的关键字确保长关键字优先匹配
    keywords.forEach(keyword => {
      const regex = new RegExp(`\\b${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'gi')
      html = html.replace(regex, (match, offset, string) => {
        // 检查是否已经在span标签内（避免重复替换）
        const beforeMatch = string.substring(Math.max(0, offset - 50), offset)
        if (beforeMatch.includes('<span') && !beforeMatch.includes('</span>')) {
          return match // 在标签内，不替换
        }
        return `<span class="sql-keyword">${match}</span>`
      })
    })
    
    // 5. 恢复注释占位符并添加高亮
    commentPlaceholders.forEach((comment, index) => {
      const escaped = comment.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      html = html.replace(`__COMMENT_LINE_${index}__`, `<span class="sql-comment">${escaped}</span>`)
      html = html.replace(`__COMMENT_BLOCK_${index}__`, `<span class="sql-comment">${escaped}</span>`)
    })
    
    // 6. 恢复字符串占位符并添加高亮
    stringPlaceholders.forEach((str, index) => {
      const escaped = str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      html = html.replace(`__STRING_${index}__`, `<span class="sql-string">${escaped}</span>`)
    })
    
    // 7. 处理换行
    html = html.replace(/\n/g, '<br>')

    return html
  }

  // 监听sqlContent变化
  watch(sqlContent, () => {
    if (sqlEditorRef.value) {
      sqlEditorRef.value.value = sqlContent.value
      updateLineNumbers()
      highlightSQL()
    }
  })

  return {
    sqlContent,
    handleEditorScroll,
    handleEditorInput,
    handleEditorKeydown,
    updateLineNumbers,
    highlightSQL
  }
}
