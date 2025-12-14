import type { Ref } from 'vue'
import { connectionApi } from '@/api/connection'

/**
 * 数据库树形视图组合式函数
 */
export function useDatabaseTree(dbTreeRef: Ref<HTMLElement | undefined>) {
  /**
   * 渲染树形视图
   */
  const renderTree = async () => {
    if (!dbTreeRef.value) return

    // 检查是否有token，如果没有则直接显示未授权提示，不发起请求
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('satoken') : null
    if (!token) {
      renderTreeNodes(dbTreeRef.value, getUnauthorizedDbStructure())
      return
    }

    try {
      // 从API获取连接列表
      const response = await connectionApi.getConnections()
      if (response.code === 0 && response.data) {
        const connections = response.data
        if (connections && connections.length > 0) {
          const dbStructure = buildTreeStructure(connections)
          renderTreeNodes(dbTreeRef.value, dbStructure)
        } else {
          // 空列表，显示默认结构
          renderTreeNodes(dbTreeRef.value, getDefaultDbStructure())
        }
      } else {
        // 使用默认结构
        renderTreeNodes(dbTreeRef.value, getDefaultDbStructure())
      }
    } catch (error: any) {
      // 检查是否是401未授权错误（多种判断方式）
      const isUnauthorized = error?.isUnauthorized === true ||
                             error?.status === 401 ||
                             error?.message?.includes('未授权') || 
                             error?.message?.includes('Unauthorized') ||
                             error?.response?.status === 401 ||
                             (error?.response?.status >= 400 && error?.response?.status < 500)
      
      if (isUnauthorized) {
        // 未授权时静默处理，显示默认结构（提示用户登录或添加数据源）
        // 完全静默，不显示任何错误信息
        renderTreeNodes(dbTreeRef.value, getUnauthorizedDbStructure())
      } else {
        // 只有网络错误或其他非授权错误才打印日志
        console.error('Failed to load database structure:', error)
        renderTreeNodes(dbTreeRef.value, getDefaultDbStructure())
      }
    }
  }

  /**
   * 构建树结构
   */
  const buildTreeStructure = (connections: any[]) => {
    return connections.map(conn => {
      // 确保dbType被正确传递，处理可能的null/undefined
      const dbType = conn.dbType || conn.db_type || ''
      
      return {
        name: `${conn.name || conn.dbType || 'Unknown'}@${conn.host || 'localhost'}`,
        type: 'root',
        expanded: false,
        connectionId: conn.id,
        dbType: dbType, // 确保dbType存在
        children: []
      }
    })
  }

  /**
   * 获取数据库类型的图片路径
   */
  const getDbTypeImage = (dbType: string): string => {
    if (!dbType) return ''
    
    // 规范化数据库类型名称（处理大小写和各种格式）
    const normalizedType = dbType.toLowerCase().trim()
    
    const typeMap: Record<string, string> = {
      // MySQL
      'mysql': '/mysql.png',
      'mysqldb': '/mysql.png',
      // PostgreSQL
      'postgres': '/postgresql.png',
      'postgresql': '/postgresql.png',
      'postgresdb': '/postgresql.png',
      'pg': '/postgresql.png',
      // Redis
      'redis': '/redis.png',
      // ClickHouse
      'clickhouse': '/clickhouse.png',
      'clickhousedb': '/clickhouse.png',
      'ch': '/clickhouse.png'
    }
    
    // 先尝试完全匹配
    if (typeMap[normalizedType]) {
      return typeMap[normalizedType]
    }
    
    // 尝试部分匹配（处理类似 'POSTGRESQL' 这种）
    for (const [key, path] of Object.entries(typeMap)) {
      if (normalizedType.includes(key) || key.includes(normalizedType)) {
        return path
      }
    }
    
    return ''
  }

  /**
   * 渲染树节点
   */
  const renderTreeNodes = (container: HTMLElement, items: any[]) => {
    container.innerHTML = ''
    items.forEach((item, index) => {
      const node = buildNode(item, 0, index)
      container.appendChild(node)
    })
  }

  /**
   * 构建树节点
   */
  const buildNode = (item: any, level: number, index: number): HTMLElement => {
    const wrapper = document.createElement('div')
    const div = document.createElement('div')
    const padding = level * 14

    const { iconClass, colorClass } = getNodeIcon(item)

    div.className = `tree-item flex items-center px-2 py-1 cursor-pointer rounded text-xs theme-text-primary`
    div.style.paddingLeft = `${padding}px`

    let expandIcon = ''
    if (item.children && item.children.length > 0) {
      expandIcon = `<i class="fa-solid fa-chevron-${item.expanded ? 'down' : 'right'} text-[8px] mr-1 ${colorClass}"></i>`
    } else {
      expandIcon = '<span class="w-2 mr-1"></span>'
    }

    // 根据数据库类型决定使用图片还是图标（与原始UI逻辑一致）
    let nodeIcon = ''
    if (item.type === 'root') {
      // 优先使用dbType，如果没有则从name中提取（与原始UI一致）
      let dbTypeForImage = item.dbType
      if (!dbTypeForImage && item.name) {
        const nameLower = item.name.toLowerCase()
        if (nameLower.includes('mysql')) dbTypeForImage = 'mysql'
        else if (nameLower.includes('postgres')) dbTypeForImage = 'postgresql'
        else if (nameLower.includes('redis')) dbTypeForImage = 'redis'
        else if (nameLower.includes('clickhouse')) dbTypeForImage = 'clickhouse'
      }
      
      if (dbTypeForImage) {
        const imgPath = getDbTypeImage(dbTypeForImage)
        if (imgPath) {
          // 使用绝对路径（Vite项目中public目录的文件）
          nodeIcon = `<img src="${imgPath}" class="w-4 h-4 mr-2 object-contain flex-shrink-0" alt="${dbTypeForImage}" onerror="console.error('Failed to load image:', this.src); this.style.display='none'; const fallback = this.nextElementSibling; if (fallback && fallback.hasAttribute('data-fallback-icon')) fallback.style.display='inline';" />`
          // 如果图片加载失败，显示fallback图标
          nodeIcon += `<i class="fa-solid ${iconClass} text-xs mr-2 ${colorClass}" style="display:none;" data-fallback-icon></i>`
        } else {
          nodeIcon = `<i class="fa-solid ${iconClass} text-xs mr-2 ${colorClass}"></i>`
        }
      } else {
        // 默认使用PostgreSQL图片（与原始UI一致）
        nodeIcon = `<img src="/postgresql.png" class="w-4 h-4 mr-2 object-contain flex-shrink-0" alt="PG" onerror="this.style.display='none'; const fallback = this.nextElementSibling; if (fallback && fallback.hasAttribute('data-fallback-icon')) fallback.style.display='inline';" />`
        nodeIcon += `<i class="fa-solid ${iconClass} text-xs mr-2 ${colorClass}" style="display:none;" data-fallback-icon></i>`
      }
    } else if (item.type === 'schema') {
      const imgPath = '/schema.png'
      nodeIcon = `<img src="${imgPath}" class="w-3 h-3 mr-2 object-contain flex-shrink-0" alt="Schema" onerror="console.error('Failed to load schema image'); this.style.display='none'; const fallback = this.nextElementSibling; if (fallback && fallback.hasAttribute('data-fallback-icon')) fallback.style.display='inline';" />`
      nodeIcon += `<i class="fa-solid ${iconClass} text-xs mr-2 ${colorClass}" style="display:none;" data-fallback-icon></i>`
    } else {
      nodeIcon = `<i class="fa-solid ${iconClass} text-xs mr-2 ${colorClass}"></i>`
    }
    
    const name = item.name
    const count = item.count ? ` (${item.count})` : ''

    div.innerHTML = `${expandIcon}${nodeIcon}<span class="truncate">${name}${count}</span>`

    div.addEventListener('click', (e) => {
      e.stopPropagation()
      handleNodeClick(item, index)
    })

    wrapper.appendChild(div)

    if (item.expanded && item.children && item.children.length > 0) {
      item.children.forEach((child: any, childIndex: number) => {
        const childNode = buildNode(child, level + 1, childIndex)
        wrapper.appendChild(childNode)
      })
    }

    return wrapper
  }

  /**
   * 获取节点图标
   */
  const getNodeIcon = (item: any) => {
    let iconClass = 'fa-folder'
    let colorClass = 'text-gray-500'

    switch (item.type) {
      case 'root':
        iconClass = 'fa-server'
        colorClass = item.expanded ? 'text-purple-400' : 'theme-text-secondary'
        break
      case 'group':
        iconClass = 'fa-layer-group'
        colorClass = 'text-yellow-500'
        break
      case 'db':
        iconClass = 'fa-database'
        colorClass = 'text-blue-400'
        break
      case 'schema':
        iconClass = 'fa-box-archive'
        colorClass = 'text-orange-400'
        break
      case 'table':
        iconClass = 'fa-table'
        colorClass = 'text-green-400'
        break
      case 'view':
        iconClass = 'fa-eye'
        colorClass = 'text-cyan-400'
        break
      case 'column':
        iconClass = 'fa-columns'
        colorClass = 'text-gray-400'
        break
    }

    return { iconClass, colorClass }
  }

  /**
   * 处理节点点击
   */
  const handleNodeClick = (item: any, index: number) => {
    if (item.children && item.children.length > 0) {
      item.expanded = !item.expanded
      renderTree()
    }
    // 这里可以添加其他点击逻辑，比如加载表结构等
  }

  /**
   * 获取默认数据库结构
   */
  const getDefaultDbStructure = () => {
    return [
      {
        name: '未连接',
        type: 'root',
        expanded: true,
        children: [
          {
            name: '点击 + 添加数据源',
            type: 'db',
            children: []
          }
        ]
      }
    ]
  }

  /**
   * 获取未授权时的数据库结构
   */
  const getUnauthorizedDbStructure = () => {
    return [
      {
        name: '未登录',
        type: 'root',
        expanded: true,
        children: [
          {
            name: '请先登录后再使用',
            type: 'db',
            children: []
          },
          {
            name: '或点击 + 添加本地数据源',
            type: 'db',
            children: []
          }
        ]
      }
    ]
  }

  return {
    renderTree
  }
}

