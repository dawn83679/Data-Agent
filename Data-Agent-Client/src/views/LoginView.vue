<template>
  <div class="login-view">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1 class="login-title">Data Agent</h1>
          <p class="login-subtitle">欢迎回来，请登录您的账户</p>
        </div>

        <!-- 标签页切换：登录/注册 -->
        <div class="tab-container">
          <button
            :class="['tab-button', { active: activeTab === 'login' }]"
            @click="activeTab = 'login'"
          >
            登录
          </button>
          <button
            :class="['tab-button', { active: activeTab === 'register' }]"
            @click="activeTab = 'register'"
          >
            注册
          </button>
        </div>

        <!-- 登录表单 -->
        <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="login-username">用户名</label>
            <input
              id="login-username"
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              required
              :disabled="authStore.loading"
            />
          </div>

          <div class="form-group">
            <label for="login-password">密码</label>
            <input
              id="login-password"
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              required
              :disabled="authStore.loading"
            />
          </div>

          <div v-if="authStore.error" class="error-message">
            {{ authStore.error }}
          </div>

          <button type="submit" class="btn-primary" :disabled="authStore.loading">
            {{ authStore.loading ? '登录中...' : '登录' }}
          </button>
        </form>

        <!-- 注册表单 -->
        <form v-if="activeTab === 'register'" @submit.prevent="handleRegister" class="login-form">
          <div class="form-group">
            <label for="register-username">用户名</label>
            <input
              id="register-username"
              v-model="registerForm.username"
              type="text"
              placeholder="请输入用户名"
              required
              :disabled="authStore.loading"
            />
          </div>

          <div class="form-group">
            <label for="register-email">邮箱（可选）</label>
            <input
              id="register-email"
              v-model="registerForm.email"
              type="email"
              placeholder="请输入邮箱"
              :disabled="authStore.loading"
            />
          </div>

          <div class="form-group">
            <label for="register-password">密码</label>
            <input
              id="register-password"
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              required
              :disabled="authStore.loading"
            />
          </div>

          <div class="form-group">
            <label for="register-confirm-password">确认密码</label>
            <input
              id="register-confirm-password"
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              required
              :disabled="authStore.loading"
            />
          </div>

          <div v-if="registerError" class="error-message">
            {{ registerError }}
          </div>

          <div v-if="authStore.error" class="error-message">
            {{ authStore.error }}
          </div>

          <button type="submit" class="btn-primary" :disabled="authStore.loading">
            {{ authStore.loading ? '注册中...' : '注册' }}
          </button>
        </form>

        <!-- 分隔线 -->
        <div class="divider">
          <span>或</span>
        </div>

        <!-- OAuth登录按钮 -->
        <div class="oauth-buttons">
          <button
            @click="handleGoogleLogin"
            class="btn-oauth btn-google"
            :disabled="authStore.loading"
          >
            <svg class="oauth-icon" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              />
            </svg>
            <span>使用 Google 登录</span>
          </button>

          <button
            @click="handleGitHubLogin"
            class="btn-oauth btn-github"
            :disabled="authStore.loading"
          >
            <svg class="oauth-icon" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23 1.957-.544 4.059-.544 6.016 0 2.293-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"
              />
            </svg>
            <span>使用 GitHub 登录</span>
          </button>
        </div>

        <!-- Token刷新说明 -->
        <div class="token-info">
          <p class="info-text">
            <strong>Token机制：</strong>系统使用Access Token（短期）和Refresh Token（长期）进行身份验证。
            当Access Token过期时，系统会自动使用Refresh Token刷新，无需重新登录。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest, RegisterRequest } from '@/types/auth'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<'login' | 'register'>('login')
const registerError = ref('')

const loginForm = ref<LoginRequest>({
  username: '',
  password: '',
})

const registerForm = ref<RegisterRequest>({
  username: '',
  password: '',
  email: '',
  confirmPassword: '',
})

// 处理登录
async function handleLogin() {
  try {
    await authStore.login(loginForm.value)
    // 登录成功，跳转到首页
    router.push('/')
  } catch (err) {
    // 错误已在store中处理
    console.error('登录失败:', err)
  }
}

// 处理注册
async function handleRegister() {
  registerError.value = ''

  // 验证密码是否一致
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }

  // 验证密码长度
  if (registerForm.value.password.length < 6) {
    registerError.value = '密码长度至少为6位'
    return
  }

  try {
    await authStore.register(registerForm.value)
    // 注册成功，跳转到首页
    router.push('/')
  } catch (err) {
    // 错误已在store中处理
    console.error('注册失败:', err)
  }
}

// 处理Google登录
async function handleGoogleLogin() {
  try {
    const url = await authStore.getOAuthAuthUrl('google')
    // 跳转到Google授权页面
    window.location.href = url
  } catch (err) {
    console.error('获取Google登录URL失败:', err)
  }
}

// 处理GitHub登录
async function handleGitHubLogin() {
  try {
    const url = await authStore.getOAuthAuthUrl('github')
    // 跳转到GitHub授权页面
    window.location.href = url
  } catch (err) {
    console.error('获取GitHub登录URL失败:', err)
  }
}

// 处理OAuth回调
onMounted(async () => {
  const urlParams = new URLSearchParams(window.location.search)
  const code = urlParams.get('code')
  const provider = urlParams.get('provider') as 'google' | 'github' | null

  if (code && provider) {
    try {
      if (provider === 'google') {
        await authStore.loginWithGoogleOAuth(code)
      } else if (provider === 'github') {
        await authStore.loginWithGitHubOAuth(code)
      }
      // 清除URL参数
      window.history.replaceState({}, document.title, '/login')
      // 跳转到首页
      router.push('/')
    } catch (err) {
      console.error('OAuth登录失败:', err)
    }
  }
})
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: var(--spacing-lg);
}

.login-container {
  width: 100%;
  max-width: 450px;
}

.login-card {
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-lg);
}

.login-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 var(--spacing-sm) 0;
}

.login-subtitle {
  color: var(--color-text-secondary);
  margin: 0;
  font-size: 14px;
}

.tab-container {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  border-bottom: 2px solid var(--color-border);
}

.tab-button {
  flex: 1;
  padding: var(--spacing-md);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-secondary);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  margin-bottom: -2px;
}

.tab-button:hover {
  color: var(--color-text);
}

.tab-button.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.form-group input {
  padding: var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  transition: var(--transition);
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-group input:disabled {
  background: var(--color-bg-secondary);
  cursor: not-allowed;
}

.error-message {
  padding: var(--spacing-sm) var(--spacing-md);
  background: #fee2e2;
  color: var(--color-danger);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.btn-primary {
  padding: var(--spacing-md);
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  margin-top: var(--spacing-sm);
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.divider {
  display: flex;
  align-items: center;
  margin: var(--spacing-lg) 0;
  text-align: center;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--color-border);
}

.divider span {
  padding: 0 var(--spacing-md);
  color: var(--color-text-secondary);
  font-size: 14px;
}

.oauth-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.btn-oauth {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-oauth:hover:not(:disabled) {
  background: var(--color-bg-hover);
  border-color: var(--color-border-dark);
}

.btn-oauth:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-google {
  border-color: #dadce0;
}

.btn-google:hover:not(:disabled) {
  background: #f8f9fa;
  border-color: #c4c7c5;
}

.btn-github {
  border-color: #d1d5db;
}

.btn-github:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.oauth-icon {
  width: 20px;
  height: 20px;
}

.token-info {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-info);
}

.info-text {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.info-text strong {
  color: var(--color-text);
}
</style>

