import { ref } from 'vue'
import type { Ref } from 'vue'

/**
 * 查询执行组合式函数
 */
export function useQuery(
  resultsHeaderRef: Ref<HTMLTableRowElement | undefined>,
  resultsBodyRef: Ref<HTMLTableSectionElement | undefined>,
  outputRef: Ref<HTMLElement | undefined>,
  statusMessage: Ref<string>
) {
  const loading = ref(false)
  const queryResults = ref<any[]>([])

  /**
   * 执行查询
   */
  const runQuery = async (sql: string) => {
    if (!sql.trim()) {
      statusMessage.value = 'SQL为空'
      return
    }

    loading.value = true
    statusMessage.value = '执行中...'

    try {
      // TODO: 调用后端API执行SQL查询
      // 这里需要根据实际的后端API调整
      // 目前先使用模拟数据
      await new Promise(resolve => setTimeout(resolve, 1000))

      // 模拟查询结果
      const mockResult = {
        columns: ['id', 'name', 'email', 'created_at'],
        rows: Array.from({ length: 10 }, (_, i) => ({
          id: 1000 + i,
          name: `user_${1000 + i}`,
          email: `user${1000 + i}@example.com`,
          created_at: new Date().toISOString()
        })),
        executionTime: 42,
        affectedRows: 10
      }

      displayQueryResult(mockResult)
      
      statusMessage.value = `执行成功，耗时 ${mockResult.executionTime}ms，影响 ${mockResult.affectedRows} 行`
    } catch (error: any) {
      console.error('Query execution failed:', error)
      statusMessage.value = `执行失败: ${error.message || '未知错误'}`
      
      if (outputRef.value) {
        outputRef.value.textContent = `错误: ${error.message || '未知错误'}`
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 显示查询结果
   */
  const displayQueryResult = (result: any) => {
    if (!resultsHeaderRef.value || !resultsBodyRef.value) return

    // 清空之前的结果
    resultsHeaderRef.value.innerHTML = ''
    resultsBodyRef.value.innerHTML = ''

    // 创建表头
    result.columns.forEach((column: string) => {
      const th = document.createElement('th')
      th.className = 'px-3 py-2 theme-text-primary font-semibold text-left border-r theme-border'
      th.textContent = column
      resultsHeaderRef.value!.appendChild(th)
    })

    // 创建表体
    result.rows.forEach((row: any) => {
      const tr = document.createElement('tr')
      tr.className = 'border-b theme-border hover:bg-white/5'

      result.columns.forEach((column: string) => {
        const td = document.createElement('td')
        td.className = 'px-3 py-2 theme-text-primary border-r theme-border'
        td.textContent = row[column] !== null && row[column] !== undefined ? String(row[column]) : 'NULL'
        tr.appendChild(td)
      })

      resultsBodyRef.value!.appendChild(tr)
    })

    queryResults.value = result.rows
  }

  return {
    loading,
    queryResults,
    runQuery
  }
}

