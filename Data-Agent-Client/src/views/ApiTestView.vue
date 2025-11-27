<template>
  <div class="api-test-container">
    <h1>API 接口测试</h1>
    
    <div class="test-section">
      <h2>1. 数据库连接管理</h2>
      
      <div class="test-item">
        <h3>获取所有连接</h3>
        <button @click="testGetConnections">测试</button>
        <pre v-if="results.getConnections">{{ JSON.stringify(results.getConnections, null, 2) }}</pre>
      </div>

      <div class="test-item">
        <h3>测试连接</h3>
        <div class="form-group">
          <input v-model="testConnectionForm.dbType" placeholder="数据库类型 (如: MYSQL)" />
          <input v-model="testConnectionForm.host" placeholder="主机地址" />
          <input v-model.number="testConnectionForm.port" type="number" placeholder="端口" />
          <input v-model="testConnectionForm.database" placeholder="数据库名" />
          <input v-model="testConnectionForm.username" placeholder="用户名" />
          <input v-model="testConnectionForm.password" type="password" placeholder="密码" />
          <input v-model="testConnectionForm.driverJarPath" placeholder="驱动 JAR 路径" />
        </div>
        <button @click="testConnection">测试连接</button>
        <pre v-if="results.testConnection">{{ JSON.stringify(results.testConnection, null, 2) }}</pre>
      </div>

      <div class="test-item">
        <h3>创建连接配置</h3>
        <div class="form-group">
          <input v-model="createConnectionForm.name" placeholder="连接名称" />
          <input v-model="createConnectionForm.dbType" placeholder="数据库类型" />
          <input v-model="createConnectionForm.host" placeholder="主机地址" />
          <input v-model.number="createConnectionForm.port" type="number" placeholder="端口" />
          <input v-model="createConnectionForm.database" placeholder="数据库名" />
          <input v-model="createConnectionForm.username" placeholder="用户名" />
          <input v-model="createConnectionForm.password" type="password" placeholder="密码" />
          <input v-model="createConnectionForm.driverJarPath" placeholder="驱动 JAR 路径" />
        </div>
        <button @click="testCreateConnection">创建连接</button>
        <pre v-if="results.createConnection">{{ JSON.stringify(results.createConnection, null, 2) }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h2>2. 驱动管理</h2>
      
      <div class="test-item">
        <h3>获取可用驱动列表</h3>
        <input v-model="driverForm.databaseType" placeholder="数据库类型 (如: MySQL)" />
        <button @click="testGetAvailableDrivers">测试</button>
        <pre v-if="results.getAvailableDrivers">{{ JSON.stringify(results.getAvailableDrivers, null, 2) }}</pre>
      </div>

      <div class="test-item">
        <h3>获取已安装驱动列表</h3>
        <input v-model="driverForm.databaseType" placeholder="数据库类型 (如: MySQL)" />
        <button @click="testGetInstalledDrivers">测试</button>
        <pre v-if="results.getInstalledDrivers">{{ JSON.stringify(results.getInstalledDrivers, null, 2) }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h2>3. AI 对话管理</h2>
      
      <div class="test-item">
        <h3>创建对话</h3>
        <input v-model="conversationForm.title" placeholder="对话标题" />
        <button @click="testCreateConversation">创建</button>
        <pre v-if="results.createConversation">{{ JSON.stringify(results.createConversation, null, 2) }}</pre>
      </div>

      <div class="test-item">
        <h3>获取对话列表</h3>
        <button @click="testGetConversationList">获取列表</button>
        <pre v-if="results.getConversationList">{{ JSON.stringify(results.getConversationList, null, 2) }}</pre>
      </div>

      <div class="test-item">
        <h3>发送聊天消息（流式）</h3>
        <input v-model.number="chatForm.conversationId" type="number" placeholder="对话ID (可选)" />
        <textarea v-model="chatForm.message" placeholder="消息内容"></textarea>
        <button @click="testSendMessage">发送</button>
        <div v-if="chatMessages.length > 0" class="chat-messages">
          <div v-for="(msg, index) in chatMessages" :key="index" class="message">
            <strong>{{ msg.type }}:</strong> {{ JSON.stringify(msg.data) }}
          </div>
        </div>
      </div>
    </div>

    <div class="test-section">
      <h2>4. 错误信息</h2>
      <div v-if="error" class="error">{{ error }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { connectionApi, driverApi, chatApi } from '@/api'
import { createSSEConnection } from '@/utils/sse'

// 测试结果存储
const results = reactive({
  getConnections: null as any,
  testConnection: null as any,
  createConnection: null as any,
  getAvailableDrivers: null as any,
  getInstalledDrivers: null as any,
  createConversation: null as any,
  getConversationList: null as any,
})

// 错误信息
const error = ref('')

// 表单数据
const testConnectionForm = reactive({
  dbType: 'MYSQL',
  host: 'localhost',
  port: 3306,
  database: 'test',
  username: 'root',
  password: '',
  driverJarPath: '',
  timeout: 30,
})

const createConnectionForm = reactive({
  name: '测试连接',
  dbType: 'MYSQL',
  host: 'localhost',
  port: 3306,
  database: 'test',
  username: 'root',
  password: '',
  driverJarPath: '',
  timeout: 30,
})

const driverForm = reactive({
  databaseType: 'MySQL',
})

const conversationForm = reactive({
  title: '测试对话',
})

const chatForm = reactive({
  conversationId: undefined as number | undefined,
  message: '你好',
})

const chatMessages = ref<Array<{ type: string; data: any }>>([])
let closeChatConnection: (() => void) | null = null

// 测试函数
async function testGetConnections() {
  try {
    error.value = ''
    const result = await connectionApi.getConnections()
    results.getConnections = result.data
    console.log('获取连接列表成功:', result.data)
  } catch (err: any) {
    error.value = `获取连接列表失败: ${err.message}`
    console.error('错误:', err)
  }
}

async function testConnection() {
  try {
    error.value = ''
    const result = await connectionApi.testConnection(testConnectionForm)
    results.testConnection = result.data
    console.log('连接测试成功:', result.data)
  } catch (err: any) {
    error.value = `连接测试失败: ${err.message}`
    console.error('错误:', err)
  }
}

async function testCreateConnection() {
  try {
    error.value = ''
    const result = await connectionApi.createConnection(createConnectionForm)
    results.createConnection = result.data
    console.log('创建连接成功:', result.data)
  } catch (err: any) {
    error.value = `创建连接失败: ${err.message}`
    console.error('错误:', err)
  }
}

async function testGetAvailableDrivers() {
  try {
    error.value = ''
    const result = await driverApi.getAvailableDrivers(driverForm.databaseType)
    results.getAvailableDrivers = result.data
    console.log('获取可用驱动成功:', result.data)
  } catch (err: any) {
    error.value = `获取可用驱动失败: ${err.message}`
    console.error('错误:', err)
  }
}

async function testGetInstalledDrivers() {
  try {
    error.value = ''
    const result = await driverApi.getInstalledDrivers(driverForm.databaseType)
    results.getInstalledDrivers = result.data
    console.log('获取已安装驱动成功:', result.data)
  } catch (err: any) {
    error.value = `获取已安装驱动失败: ${err.message}`
    console.error('错误:', err)
  }
}

async function testCreateConversation() {
  try {
    error.value = ''
    const result = await chatApi.createConversation(conversationForm)
    results.createConversation = result.data
    console.log('创建对话成功:', result.data)
  } catch (err: any) {
    error.value = `创建对话失败: ${err.message}`
    console.error('错误:', err)
  }
}

async function testGetConversationList() {
  try {
    error.value = ''
    const result = await chatApi.getConversationList({
      current: 1,
      size: 10,
    })
    results.getConversationList = result.data
    console.log('获取对话列表成功:', result.data)
  } catch (err: any) {
    error.value = `获取对话列表失败: ${err.message}`
    console.error('错误:', err)
  }
}

function testSendMessage() {
  try {
    error.value = ''
    chatMessages.value = []
    
    // 关闭之前的连接
    if (closeChatConnection) {
      closeChatConnection()
    }

    const baseURL = import.meta.env.VITE_API_BASE_URL || ''
    const url = baseURL ? `${baseURL}/api/chat/send` : '/api/chat/send'
    
    closeChatConnection = createSSEConnection(url, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('satoken')}`,
      },
      body: {
        conversationId: chatForm.conversationId || undefined,
        message: chatForm.message,
        model: 'qwen3-coder-plus',
      },
      onMessage: (message) => {
        chatMessages.value.push(message)
        console.log('收到消息:', message)
      },
      onError: (err) => {
        error.value = `聊天错误: ${err.message}`
        console.error('聊天错误:', err)
      },
      onOpen: () => {
        console.log('聊天连接已建立')
      },
      onClose: () => {
        console.log('聊天连接已关闭')
        closeChatConnection = null
      },
    })
  } catch (err: any) {
    error.value = `发送消息失败: ${err.message}`
    console.error('错误:', err)
  }
}
</script>

<style scoped>
.api-test-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  color: #2c3e50;
  margin-bottom: 30px;
}

.test-section {
  margin-bottom: 40px;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.test-section h2 {
  color: #34495e;
  margin-bottom: 20px;
  border-bottom: 2px solid #3498db;
  padding-bottom: 10px;
}

.test-item {
  margin-bottom: 30px;
  padding: 15px;
  background: white;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.test-item h3 {
  color: #555;
  margin-bottom: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 15px;
}

.form-group input,
.form-group textarea {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group textarea {
  min-height: 80px;
  resize: vertical;
}

button {
  padding: 10px 20px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

button:hover {
  background: #2980b9;
}

button:active {
  background: #21618c;
}

pre {
  margin-top: 15px;
  padding: 15px;
  background: #f8f8f8;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.chat-messages {
  margin-top: 15px;
  padding: 15px;
  background: #f8f8f8;
  border: 1px solid #ddd;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.message {
  margin-bottom: 10px;
  padding: 10px;
  background: white;
  border-radius: 4px;
  border-left: 3px solid #3498db;
}

.message strong {
  color: #3498db;
  margin-right: 10px;
}

.error {
  padding: 15px;
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 4px;
  color: #c33;
  margin-top: 15px;
}
</style>

