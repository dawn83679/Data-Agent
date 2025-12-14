<template>
  <div class="database-view fixed inset-0 h-screen w-screen flex flex-col text-sm theme-bg-main theme-text-primary overflow-hidden">
    <!-- Top Main Toolbar -->
    <header class="h-10 theme-bg-panel flex items-center px-4 border-b theme-border justify-end select-none">
      <div class="flex items-center space-x-3">
        <button
          @click="toggleAI"
          class="w-8 h-8 flex items-center justify-center rounded theme-bg-hover text-purple-400 transition-colors"
          title="AI Assistant"
        >
          <i class="fa-solid fa-wand-magic-sparkles"></i>
        </button>
        <button
          @click="toggleTheme"
          class="w-8 h-8 flex items-center justify-center rounded theme-bg-hover theme-text-secondary"
          :title="currentTheme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
        >
          <i :class="currentTheme === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-moon'"></i>
        </button>
        <button
          @click="showSettingsModal"
          class="w-8 h-8 flex items-center justify-center rounded theme-bg-hover theme-text-secondary"
          title="Settings"
        >
          <i class="fa-solid fa-gear"></i>
        </button>
      </div>
    </header>

    <!-- Main Layout -->
    <div class="flex-1 flex overflow-hidden">
      <!-- Left Sidebar (Database Explorer) -->
      <aside
        ref="sidebarRef"
        class="w-64 theme-bg-panel flex flex-col transition-all duration-300"
      >
        <div class="flex items-center justify-between px-3 py-2 theme-text-secondary text-xs uppercase font-semibold tracking-wider">
          <span>Database Explorer</span>
          <div class="space-x-1">
            <i
              class="fa-solid fa-minus cursor-pointer hover:text-blue-500"
              @click="toggleSidebar"
            ></i>
            <i
              class="fa-solid fa-plus cursor-pointer hover:text-blue-500"
              @click="toggleAddMenu($event)"
            ></i>
          </div>
        </div>
        <div class="flex-1 overflow-y-auto p-1 select-none" ref="dbTreeRef"></div>
      </aside>

      <!-- Sidebar Add Menu -->
      <div
        v-show="showAddMenu"
        ref="addMenuRef"
        class="fixed z-[100] w-40 theme-bg-popup border theme-border rounded shadow-xl text-xs py-1 select-none font-medium"
      >
        <div
          class="flex items-center px-3 py-1.5 hover:bg-blue-600 hover:text-white cursor-pointer"
          @click="addNewGroup"
        >
          <i class="fa-solid fa-folder-plus w-4 mr-2 theme-text-secondary"></i>
          <span>Group</span>
        </div>
        <div
          class="flex items-center px-3 py-1.5 hover:bg-blue-600 hover:text-white cursor-pointer"
          @click="showDataSourceModal"
        >
          <i class="fa-solid fa-database w-4 mr-2 theme-text-secondary"></i>
          <span>Data Source</span>
        </div>
      </div>

      <!-- Left Resizer -->
      <div class="resizer-handle" ref="resizerLeftRef"></div>

      <!-- Content Area -->
      <main class="flex-1 flex flex-col theme-bg-main min-w-0">
        <!-- Tab Bar -->
        <div class="h-9 theme-bg-panel flex items-end space-x-1 overflow-x-auto no-scrollbar border-b theme-border" ref="tabBarRef"></div>

        <!-- Mode: Console -->
        <div class="flex-1 flex flex-col min-h-0">
          <!-- Toolbar -->
          <div class="h-8 flex items-center pl-2 pr-4 theme-bg-main border-b theme-border text-xs theme-text-secondary">
            <div class="flex items-center mr-4">
              <button
                @click="runQuery"
                class="w-8 h-8 flex items-center justify-center rounded theme-bg-hover text-green-500 transition-colors hover:bg-green-500/10"
                title="Execute (Ctrl+Enter / Cmd+Enter)"
              >
                <i class="fa-solid fa-play"></i>
              </button>
              <div class="w-px h-4 mr-2" style="background-color: var(--border-color)"></div>
              <div class="relative">
                <div
                  v-show="showTransactionMenu"
                  class="absolute top-full left-0 mt-1 w-56 theme-bg-popup border theme-border rounded shadow-xl text-xs select-none font-medium z-50"
                >
                  <TransactionMenu
                    :transaction="transaction"
                    @update:transaction="handleTransactionUpdate"
                  />
                </div>
                <div
                  class="flex items-center border theme-border rounded overflow-hidden h-6 transition-colors duration-200"
                >
                  <div
                    class="flex items-center px-2 h-full cursor-pointer hover:bg-white/5 transition-colors"
                    @click.stop="toggleTransactionMenu"
                  >
                    <span class="text-blue-400 font-medium text-xs"
                      >Tx:{{ transaction.mode === 'auto' ? 'Auto' : 'Manual' }}</span
                    >
                  </div>
                  <div v-show="transaction.active" class="w-px h-3 bg-gray-600"></div>
                  <div v-show="transaction.active" class="flex items-center h-full">
                    <button
                      @click="commitTransaction"
                      class="h-full w-7 flex items-center justify-center hover:bg-green-500/20 text-green-500 transition-colors"
                      title="Commit"
                    >
                      <i class="fa-solid fa-check text-xs"></i>
                    </button>
                    <div class="w-px h-3 bg-gray-600/30"></div>
                    <button
                      @click="rollbackTransaction"
                      class="h-full w-7 flex items-center justify-center hover:bg-red-500/20 text-red-500 transition-colors"
                      title="Rollback"
                    >
                      <i class="fa-solid fa-reply text-xs"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Editor Area -->
          <div class="flex-1 flex relative">
            <div
              class="w-10 theme-bg-main text-[#606366] text-right pr-3 py-2 code-font text-xs select-none border-r theme-border leading-6"
              ref="lineNumbersRef"
            >
              1
            </div>
            <div class="relative flex-1 h-full overflow-hidden">
              <pre
                ref="sqlHighlightRef"
                class="editor-layer"
                aria-hidden="true"
              ></pre>
              <textarea
                ref="sqlEditorRef"
                v-model="sqlContent"
                class="editor-layer"
                spellcheck="false"
                @scroll="handleEditorScroll"
                @input="handleEditorInput"
                @keydown="handleEditorKeydown"
              ></textarea>
            </div>
          </div>

          <!-- Resizer -->
          <div
            class="h-[5px] theme-bg-panel hover:bg-blue-500 cursor-row-resize flex items-center justify-center"
            ref="resizerBottomRef"
          >
            <div class="w-8 h-[1px] bg-gray-500"></div>
          </div>

          <!-- Bottom Panel -->
          <div class="h-64 theme-bg-panel flex flex-col" ref="bottomPanelRef">
            <div class="flex items-center h-9 px-2 theme-bg-panel border-b theme-border">
              <div class="flex space-x-1">
                <button
                  :class="[
                    'px-3 py-1 text-xs rounded-t border-t theme-border',
                    activeResultTab === 'result' ? 'theme-text-primary theme-bg-main' : 'theme-text-secondary hover:text-white'
                  ]"
                  @click="activeResultTab = 'result'"
                >
                  Result {{ queryResults.length }}
                </button>
                <button
                  :class="[
                    'px-3 py-1 text-xs rounded-t',
                    activeResultTab === 'output' ? 'theme-text-primary theme-bg-main border-t theme-border' : 'theme-text-secondary hover:text-white'
                  ]"
                  @click="activeResultTab = 'output'"
                >
                  Output
                </button>
              </div>
            </div>
            <div class="flex-1 overflow-auto theme-bg-main relative">
              <div
                v-show="loading"
                class="absolute inset-0 flex items-center justify-center theme-bg-main z-10"
              >
                <i class="fa-solid fa-circle-notch fa-spin text-blue-500 text-2xl"></i>
              </div>
              <div v-show="activeResultTab === 'result'" class="h-full w-full overflow-auto">
                <div class="min-w-full">
                  <table class="min-w-full text-left text-xs code-font border-collapse">
                    <thead class="theme-bg-panel sticky top-0">
                      <tr ref="resultsHeaderRef"></tr>
                    </thead>
                    <tbody ref="resultsBodyRef" class="theme-text-primary"></tbody>
                  </table>
                </div>
              </div>
              <div
                v-show="activeResultTab === 'output'"
                class="h-full w-full p-2 code-font text-xs theme-text-secondary whitespace-pre-wrap font-mono"
              >
                <div class="text-gray-500 mb-1">-- Output Console --</div>
                <div ref="outputRef"></div>
              </div>
            </div>
            <div class="h-6 theme-bg-panel border-t theme-border flex items-center px-2 text-[10px] theme-text-secondary justify-between">
              <div>{{ statusMessage }}</div>
              <div>Read-only</div>
            </div>
          </div>
        </div>
      </main>

      <!-- Right Resizer -->
      <div class="resizer-handle" ref="resizerRightRef"></div>

      <!-- Right Sidebar (AI Assistant) -->
      <aside
        ref="aiSidebarRef"
        :class="[
          'transition-all duration-300 flex flex-col overflow-hidden',
          aiSidebarVisible ? 'w-80 opacity-100' : 'w-0 opacity-0'
        ]"
        class="theme-bg-panel"
      >
        <AISidebar
          v-if="aiSidebarVisible"
          :messages="aiMessages"
          :agent="aiAgent"
          :model="aiModel"
          @send-message="handleAISendMessage"
          @close="toggleAI"
        />
      </aside>
    </div>

    <!-- Modals -->
    <DataSourceModal
      v-if="showDataSourceModalVisible"
      @close="showDataSourceModalVisible = false"
      @save="handleSaveDataSource"
    />

    <!-- Toast Container -->
    <div class="fixed top-4 right-4 z-[300] space-y-2">
      <Toast
        v-for="toast in toasts"
        :key="toast.id"
        :type="toast.type"
        :message="toast.message"
        @close="removeToast(toast.id)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useSqlEditor } from '@/composables/useSqlEditor'
import { useDatabaseTree } from '@/composables/useDatabaseTree'
import { useTabs } from '@/composables/useTabs'
import { useResizer } from '@/composables/useResizer'
import { useTransaction } from '@/composables/useTransaction'
import { useAI } from '@/composables/useAI'
import { useQuery } from '@/composables/useQuery'
import { useToast } from '@/composables/useToast'
import { useTheme } from '@/composables/useTheme'
import TransactionMenu from '@/components/database/TransactionMenu.vue'
import AISidebar from '@/components/database/AISidebar.vue'
import DataSourceModal from '@/components/database/DataSourceModal.vue'
import Toast from '@/components/database/Toast.vue'

// Refs
const sidebarRef = ref<HTMLElement>()
const dbTreeRef = ref<HTMLElement>()
const addMenuRef = ref<HTMLElement>()
const sqlEditorRef = ref<HTMLTextAreaElement>()
const sqlHighlightRef = ref<HTMLPreElement>()
const lineNumbersRef = ref<HTMLElement>()
const tabBarRef = ref<HTMLElement>()
const resizerLeftRef = ref<HTMLElement>()
const resizerBottomRef = ref<HTMLElement>()
const resizerRightRef = ref<HTMLElement>()
const bottomPanelRef = ref<HTMLElement>()
const aiSidebarRef = ref<HTMLElement>()
const resultsHeaderRef = ref<HTMLTableRowElement>()
const resultsBodyRef = ref<HTMLTableSectionElement>()
const outputRef = ref<HTMLElement>()

// State
const showAddMenu = ref(false)
const showDataSourceModalVisible = ref(false)
const aiSidebarVisible = ref(false)
const activeResultTab = ref<'result' | 'output'>('result')
const statusMessage = ref('Ready')

// Composables
const { sqlContent, handleEditorScroll, handleEditorInput, handleEditorKeydown, updateLineNumbers, highlightSQL } = useSqlEditor(sqlEditorRef, sqlHighlightRef, lineNumbersRef)
const { renderTree } = useDatabaseTree(dbTreeRef)
const { renderTabs } = useTabs(tabBarRef)
const { setupResizers } = useResizer()
const { transaction, showTransactionMenu, toggleTransactionMenu, handleTransactionUpdate, commitTransaction, rollbackTransaction } = useTransaction()
const { aiMessages, aiAgent, aiModel, handleAISendMessage } = useAI()
const { loading, queryResults, runQuery: executeQuery } = useQuery(resultsHeaderRef, resultsBodyRef, outputRef, statusMessage)
const { toasts, showToast, removeToast } = useToast()
const { currentTheme, toggleTheme } = useTheme()

// State for sidebar width
const lastSidebarWidth = ref(256)

// Methods
const toggleSidebar = () => {
  if (!sidebarRef.value) return
  
  const currentWidth = sidebarRef.value.getBoundingClientRect().width
  
  if (currentWidth < 10) {
    // 展开
    sidebarRef.value.classList.remove('w-0')
    sidebarRef.value.style.width = `${lastSidebarWidth.value}px`
    sidebarRef.value.style.opacity = '1'
  } else {
    // 折叠，保存当前宽度
    lastSidebarWidth.value = currentWidth > 50 ? currentWidth : 256
    sidebarRef.value.classList.add('w-0')
    sidebarRef.value.style.width = '0px'
    sidebarRef.value.style.opacity = '0'
  }
}

const toggleAddMenu = (event: MouseEvent) => {
  showAddMenu.value = !showAddMenu.value
  if (showAddMenu.value && addMenuRef.value && event.target) {
    // 定位菜单到按钮位置
    nextTick(() => {
      const target = event.target as HTMLElement
      const rect = target.getBoundingClientRect()
      if (addMenuRef.value) {
        addMenuRef.value.style.position = 'fixed'
        addMenuRef.value.style.top = `${rect.bottom + 5}px`
        addMenuRef.value.style.left = `${rect.left}px`
      }
    })
  }
}

const addNewGroup = () => {
  showToast('Group功能待实现', 'info')
  showAddMenu.value = false
}

const showDataSourceModal = () => {
  showDataSourceModalVisible.value = true
  showAddMenu.value = false
}

const handleSaveDataSource = (data: any) => {
  showToast('数据源保存成功', 'success')
  showDataSourceModalVisible.value = false
}

const toggleAI = () => {
  aiSidebarVisible.value = !aiSidebarVisible.value
}

const showSettingsModal = () => {
  showToast('设置功能待实现', 'info')
}

const runQuery = async () => {
  await executeQuery(sqlContent.value)
}

// Listen for execute query event
const handleExecuteQuery = () => {
  runQuery()
}

// 点击外部关闭添加菜单
const handleClickOutside = (event: MouseEvent) => {
  if (showAddMenu.value && addMenuRef.value && !addMenuRef.value.contains(event.target as Node)) {
    const plusButton = (event.target as HTMLElement)?.closest('.fa-plus')
    if (!plusButton) {
      showAddMenu.value = false
    }
  }
}

// Lifecycle
onMounted(async () => {
  await nextTick()
  
  // Initialize editor
  if (sqlEditorRef.value) {
    sqlEditorRef.value.value = 'SELECT * FROM users LIMIT 100;'
    sqlContent.value = 'SELECT * FROM users LIMIT 100;'
    updateLineNumbers()
    highlightSQL()
  }
  
  // Initialize tree
  renderTree()
  
  // Initialize tabs
  renderTabs()
  
  // Setup resizers
  setupResizers(resizerLeftRef, resizerBottomRef, resizerRightRef, sidebarRef, bottomPanelRef, aiSidebarRef)
  
  // Listen for execute query event
  window.addEventListener('execute-query', handleExecuteQuery)
  
  // Listen for click outside to close add menu
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('execute-query', handleExecuteQuery)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style>
@import '@/styles/database/theme.css';
@import '@/styles/database/components.css';
</style>
