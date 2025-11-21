<template>
  <div class="login-view">
    <!-- 动态背景 -->
    <div class="background-animation">
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>
      <div class="grid-overlay"></div>
    </div>

    <div class="login-container">
      <div class="login-card">
        <!-- 品牌标识区域 -->
        <div class="brand-section">
          <div class="logo-container">
            <div class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 7V17C4 18.1046 4.89543 19 6 19H18C19.1046 19 20 18.1046 20 17V7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <path d="M3 7H21L19.5 4H4.5L3 7Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="9" cy="11" r="1" fill="currentColor"/>
                <circle cx="15" cy="11" r="1" fill="currentColor"/>
                <circle cx="12" cy="14" r="1" fill="currentColor"/>
                <path d="M7 11H8M16 11H17M11 14H13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="brand-text">
              <h1 class="brand-title">Data Agent</h1>
              <p class="brand-subtitle">智能数据库管理平台</p>
            </div>
          </div>
          <div class="welcome-text">
            <h2 class="welcome-title">欢迎回来！</h2>
            <p class="welcome-desc">连接您的数据世界，开启智能之旅</p>
          </div>
        </div>

        <!-- 智能标签页切换 -->
        <div class="smart-tabs">
          <div class="tab-slider" :class="{ 'slide-right': activeTab === 'register' }"></div>
          <button
            :class="['smart-tab', { active: activeTab === 'login' }]"
            @click="switchTab('login')"
          >
            <span class="tab-icon">🔐</span>
            <span class="tab-text">登录</span>
          </button>
          <button
            :class="['smart-tab', { active: activeTab === 'register' }]"
            @click="switchTab('register')"
          >
            <span class="tab-icon">✨</span>
            <span class="tab-text">注册</span>
          </button>
        </div>

        <!-- 智能登录表单 -->
        <div class="form-container">
          <Transition name="slide-fade" mode="out-in">
            <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="smart-form" key="login">
              <div class="input-group">
                <div class="input-wrapper">
                  <div class="input-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <input
                    id="login-username"
                    v-model="loginForm.username"
                    type="text"
                    placeholder="用户名"
                    required
                    :disabled="authStore.loading"
                    class="smart-input"
                  />
                  <label for="login-username" class="floating-label">用户名</label>
                </div>
              </div>

              <div class="input-group">
                <div class="input-wrapper">
                  <div class="input-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="16" r="1" fill="currentColor"/>
                      <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <input
                    id="login-password"
                    v-model="loginForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="密码"
                    required
                    :disabled="authStore.loading"
                    class="smart-input"
                  />
                  <label for="login-password" class="floating-label">密码</label>
                  <button 
                    type="button" 
                    class="password-toggle"
                    @click="showPassword = !showPassword"
                  >
                    <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M17.94 17.94C16.2306 19.243 14.1491 19.9649 12 20C5 20 1 12 1 12C2.24389 9.68192 4.02643 7.65663 6.17 6.06M9.9 4.24C10.5883 4.0789 11.2931 3.99836 12 4C19 4 23 12 23 12C22.393 13.1356 21.6691 14.2048 20.84 15.19M14.12 14.12C13.8454 14.4148 13.5141 14.6512 13.1462 14.8151C12.7782 14.9791 12.3809 15.0673 11.9781 15.0744C11.5753 15.0815 11.1749 15.0074 10.8016 14.8565C10.4283 14.7056 10.0887 14.481 9.80385 14.1962C9.51900 13.9113 9.29439 13.5717 9.14351 13.1984C8.99262 12.8251 8.91853 12.4247 8.92563 12.0219C8.93274 11.6191 9.02091 11.2218 9.18488 10.8538C9.34884 10.4858 9.58525 10.1546 9.88 9.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M1 1L23 23" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M1 12S5 4 12 4S23 12 23 12S19 20 12 20S1 12 1 12Z" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </button>
                </div>
              </div>

              <div class="form-options">
                <label class="checkbox-wrapper">
                  <input type="checkbox" v-model="rememberMe">
                  <span class="checkmark"></span>
                  <span class="checkbox-text">记住我</span>
                </label>
                <a href="#" class="forgot-link">忘记密码？</a>
              </div>

              <div v-if="authStore.error" class="error-message">
                <div class="error-icon">⚠️</div>
                <span>{{ authStore.error }}</span>
              </div>

              <button type="submit" class="smart-button primary" :disabled="authStore.loading">
                <span v-if="authStore.loading" class="loading-spinner"></span>
                <span class="button-text">{{ authStore.loading ? '登录中...' : '立即登录' }}</span>
                <div class="button-glow"></div>
              </button>
            </form>

            <!-- 智能注册表单 -->
            <form v-else @submit.prevent="handleRegister" class="smart-form" key="register">
              <div class="input-group">
                <div class="input-wrapper">
                  <div class="input-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <input
                    id="register-username"
                    v-model="registerForm.username"
                    type="text"
                    placeholder="用户名"
                    required
                    :disabled="authStore.loading"
                    class="smart-input"
                  />
                  <label for="register-username" class="floating-label">用户名</label>
                </div>
              </div>

              <div class="input-group">
                <div class="input-wrapper">
                  <div class="input-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M4 4H20C21.1 4 22 4.9 22 6V18C22 19.1 21.1 20 20 20H4C2.9 20 2 19.1 2 18V6C2 4.9 2.9 4 4 4Z" stroke="currentColor" stroke-width="2"/>
                      <polyline points="22,6 12,13 2,6" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <input
                    id="register-email"
                    v-model="registerForm.email"
                    type="email"
                    placeholder="邮箱（可选）"
                    :disabled="authStore.loading"
                    class="smart-input"
                  />
                  <label for="register-email" class="floating-label">邮箱（可选）</label>
                </div>
              </div>

              <div class="input-group">
                <div class="input-wrapper">
                  <div class="input-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
                      <circle cx="12" cy="16" r="1" fill="currentColor"/>
                      <path d="M7 11V7C7 5.67392 7.52678 4.40215 8.46447 3.46447C9.40215 2.52678 10.6739 2 12 2C13.3261 2 14.5979 2.52678 15.5355 3.46447C16.4732 4.40215 17 5.67392 17 7V11" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <input
                    id="register-password"
                    v-model="registerForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="密码"
                    required
                    :disabled="authStore.loading"
                    class="smart-input"
                  />
                  <label for="register-password" class="floating-label">密码</label>
                  <div class="password-strength">
                    <div class="strength-bar" :class="passwordStrength"></div>
                  </div>
                </div>
              </div>

              <div class="input-group">
                <div class="input-wrapper">
                  <div class="input-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M9 12L11 14L15 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </div>
                  <input
                    id="register-confirm-password"
                    v-model="registerForm.confirmPassword"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="确认密码"
                    required
                    :disabled="authStore.loading"
                    class="smart-input"
                  />
                  <label for="register-confirm-password" class="floating-label">确认密码</label>
                  <div v-if="registerForm.confirmPassword" class="password-match">
                    <div class="match-indicator" :class="{ match: passwordsMatch, mismatch: !passwordsMatch }">
                      {{ passwordsMatch ? '✓' : '✗' }}
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-options">
                <label class="checkbox-wrapper">
                  <input type="checkbox" v-model="agreeTerms" required>
                  <span class="checkmark"></span>
                  <span class="checkbox-text">我同意 <a href="#" class="terms-link">服务条款</a> 和 <a href="#" class="terms-link">隐私政策</a></span>
                </label>
              </div>

              <div v-if="registerError" class="error-message">
                <div class="error-icon">⚠️</div>
                <span>{{ registerError }}</span>
              </div>

              <div v-if="authStore.error" class="error-message">
                <div class="error-icon">⚠️</div>
                <span>{{ authStore.error }}</span>
              </div>

              <button type="submit" class="smart-button primary" :disabled="authStore.loading || !agreeTerms">
                <span v-if="authStore.loading" class="loading-spinner"></span>
                <span class="button-text">{{ authStore.loading ? '注册中...' : '创建账户' }}</span>
                <div class="button-glow"></div>
              </button>
            </form>
          </Transition>
        </div>

        <!-- 智能分隔线 -->
        <div class="smart-divider">
          <div class="divider-line"></div>
          <span class="divider-text">或者使用</span>
          <div class="divider-line"></div>
        </div>

        <!-- 现代化OAuth登录 -->
        <div class="oauth-section">
          <div class="oauth-grid">
            <button
              @click="handleGoogleLogin"
              class="oauth-button google"
              :disabled="authStore.loading"
            >
              <div class="oauth-icon-wrapper">
                <svg class="oauth-icon" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
              </div>
              <span class="oauth-text">Google</span>
            </button>

            <button
              @click="handleGitHubLogin"
              class="oauth-button github"
              :disabled="authStore.loading"
            >
              <div class="oauth-icon-wrapper">
                <svg class="oauth-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23 1.957-.544 4.059-.544 6.016 0 2.293-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
              </div>
              <span class="oauth-text">GitHub</span>
            </button>
          </div>
        </div>

        <!-- 智能提示信息 -->
        <div class="smart-info">
          <div class="info-card">
            <div class="info-icon">🔒</div>
            <div class="info-content">
              <h4 class="info-title">安全认证</h4>
              <p class="info-desc">采用双Token机制，自动刷新，安全便捷</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest, RegisterRequest } from '@/types/auth'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<'login' | 'register'>('login')
const registerError = ref('')
const showPassword = ref(false)
const rememberMe = ref(false)
const agreeTerms = ref(false)

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

// 计算属性
const passwordStrength = computed(() => {
  const password = registerForm.value.password
  if (!password) return ''
  
  let strength = 0
  if (password.length >= 8) strength++
  if (/[A-Z]/.test(password)) strength++
  if (/[a-z]/.test(password)) strength++
  if (/[0-9]/.test(password)) strength++
  if (/[^A-Za-z0-9]/.test(password)) strength++
  
  if (strength <= 2) return 'weak'
  if (strength <= 3) return 'medium'
  return 'strong'
})

const passwordsMatch = computed(() => {
  return registerForm.value.password === registerForm.value.confirmPassword
})

// 标签页切换动画
function switchTab(tab: 'login' | 'register') {
  activeTab.value = tab
}

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
/* 主容器 - 现代化科技背景 */
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  padding: 20px;
}

/* 动态背景动画 */
.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  animation: float 20s infinite linear;
}

.shape-1 {
  width: 80px;
  height: 80px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 120px;
  height: 120px;
  top: 70%;
  right: 10%;
  animation-delay: -5s;
}

.shape-3 {
  width: 60px;
  height: 60px;
  top: 30%;
  right: 30%;
  animation-delay: -10s;
}

.shape-4 {
  width: 100px;
  height: 100px;
  bottom: 20%;
  left: 20%;
  animation-delay: -15s;
}

.shape-5 {
  width: 40px;
  height: 40px;
  top: 50%;
  left: 50%;
  animation-delay: -8s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
    opacity: 0.7;
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
    opacity: 1;
  }
}

.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: grid-move 30s linear infinite;
}

@keyframes grid-move {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

/* 登录容器 - 玻璃拟态设计 */
.login-container {
  width: 100%;
  max-width: 480px;
  position: relative;
  z-index: 1;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 
    0 25px 50px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  animation: card-enter 0.8s ease-out;
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 品牌区域 */
.brand-section {
  text-align: center;
  margin-bottom: 32px;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  animation: logo-pulse 2s ease-in-out infinite;
}

@keyframes logo-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.logo-icon svg {
  width: 24px;
  height: 24px;
}

.brand-text {
  text-align: left;
}

.brand-title {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 4px 0 0 0;
  font-weight: 500;
}

.welcome-text {
  margin-bottom: 8px;
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.welcome-desc {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  font-weight: 400;
}

/* 智能标签页 */
.smart-tabs {
  position: relative;
  display: flex;
  background: rgba(243, 244, 246, 0.8);
  border-radius: 16px;
  padding: 4px;
  margin-bottom: 32px;
  backdrop-filter: blur(10px);
}

.tab-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 12px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.tab-slider.slide-right {
  transform: translateX(100%);
}

.smart-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: none;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.smart-tab.active {
  color: white;
}

.smart-tab:not(.active) {
  color: #6b7280;
}

.smart-tab:not(.active):hover {
  color: #374151;
}

.tab-icon {
  font-size: 18px;
}

/* 表单容器 */
.form-container {
  margin-bottom: 24px;
}

.smart-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 输入组 */
.input-group {
  position: relative;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  color: #9ca3af;
  z-index: 2;
  transition: color 0.3s ease;
}

.smart-input {
  width: 100%;
  padding: 16px 16px 16px 48px;
  border: 2px solid rgba(229, 231, 235, 0.8);
  border-radius: 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  color: #1f2937;
}

.smart-input:focus {
  outline: none;
  border-color: #667eea;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.smart-input:focus + .floating-label,
.smart-input:not(:placeholder-shown) + .floating-label {
  transform: translateY(-32px) scale(0.85);
  color: #667eea;
  font-weight: 600;
}

.smart-input:focus ~ .input-icon {
  color: #667eea;
}

.floating-label {
  position: absolute;
  left: 48px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: #9ca3af;
  pointer-events: none;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
  padding: 0 8px;
  border-radius: 4px;
}

/* 密码相关 */
.password-toggle {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  width: 20px;
  height: 20px;
  transition: color 0.3s ease;
}

.password-toggle:hover {
  color: #667eea;
}

.password-toggle svg {
  width: 100%;
  height: 100%;
}

.password-strength {
  position: absolute;
  bottom: -8px;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(229, 231, 235, 0.5);
  border-radius: 2px;
  overflow: hidden;
}

.strength-bar {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;
  width: 0;
}

.strength-bar.weak {
  width: 33%;
  background: #ef4444;
}

.strength-bar.medium {
  width: 66%;
  background: #f59e0b;
}

.strength-bar.strong {
  width: 100%;
  background: #10b981;
}

.password-match {
  position: absolute;
  right: 48px;
  top: 50%;
  transform: translateY(-50%);
}

.match-indicator {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  transition: all 0.3s ease;
}

.match-indicator.match {
  background: #dcfce7;
  color: #16a34a;
}

.match-indicator.mismatch {
  background: #fee2e2;
  color: #dc2626;
}

/* 表单选项 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
}

.checkbox-wrapper input[type="checkbox"] {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  position: relative;
  transition: all 0.3s ease;
  background: white;
}

.checkbox-wrapper input[type="checkbox"]:checked + .checkmark {
  background: #667eea;
  border-color: #667eea;
}

.checkbox-wrapper input[type="checkbox"]:checked + .checkmark::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.forgot-link,
.terms-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.forgot-link:hover,
.terms-link:hover {
  color: #4f46e5;
  text-decoration: underline;
}

/* 智能按钮 */
.smart-button {
  position: relative;
  width: 100%;
  padding: 18px 24px;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.smart-button.primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
}

.smart-button.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(102, 126, 234, 0.4);
}

.smart-button.primary:active {
  transform: translateY(0);
}

.smart-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.button-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}

.smart-button:hover .button-glow {
  left: 100%;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误消息 */
.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(254, 226, 226, 0.9);
  color: #dc2626;
  border-radius: 12px;
  font-size: 14px;
  border-left: 4px solid #dc2626;
  backdrop-filter: blur(10px);
}

.error-icon {
  font-size: 16px;
}

/* 智能分隔线 */
.smart-divider {
  display: flex;
  align-items: center;
  margin: 32px 0;
  position: relative;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(156, 163, 175, 0.5), transparent);
}

.divider-text {
  padding: 0 20px;
  font-size: 14px;
  color: #9ca3af;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  font-weight: 500;
}

/* OAuth区域 */
.oauth-section {
  margin-bottom: 24px;
}

.oauth-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.oauth-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border: 2px solid rgba(229, 231, 235, 0.8);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.oauth-button:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.oauth-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.oauth-icon-wrapper {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.oauth-icon {
  width: 24px;
  height: 24px;
}

/* 智能信息卡片 */
.smart-info {
  margin-top: 24px;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(239, 246, 255, 0.8);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.info-icon {
  font-size: 20px;
}

.info-content {
  flex: 1;
}

.info-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e40af;
  margin: 0 0 4px 0;
}

.info-desc {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  line-height: 1.4;
}

/* 过渡动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .login-view {
    padding: 16px;
  }
  
  .login-card {
    padding: 24px;
    border-radius: 20px;
  }
  
  .logo-container {
    flex-direction: column;
    gap: 12px;
  }
  
  .brand-text {
    text-align: center;
  }
  
  .oauth-grid {
    grid-template-columns: 1fr;
  }
  
  .form-options {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .login-card {
    background: rgba(17, 24, 39, 0.95);
    border: 1px solid rgba(75, 85, 99, 0.3);
  }
  
  .welcome-title {
    color: #f9fafb;
  }
  
  .welcome-desc,
  .brand-subtitle {
    color: #d1d5db;
  }
  
  .smart-input {
    background: rgba(31, 41, 55, 0.8);
    border-color: rgba(75, 85, 99, 0.8);
    color: #f9fafb;
  }
  
  .smart-input:focus {
    background: rgba(31, 41, 55, 0.95);
  }
  
  .floating-label {
    background: rgba(17, 24, 39, 0.9);
    color: #d1d5db;
  }
  
  .oauth-button {
    background: rgba(31, 41, 55, 0.8);
    border-color: rgba(75, 85, 99, 0.8);
    color: #f9fafb;
  }
  
  .info-card {
    background: rgba(31, 41, 55, 0.8);
    border-color: rgba(59, 130, 246, 0.3);
  }
  
  .info-title {
    color: #93c5fd;
  }
  
  .info-desc {
    color: #d1d5db;
  }
}
</style>

