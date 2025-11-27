<template>
  <div class="google-callback">
    <h1>Google 登录中...</h1>
    <p v-if="loading">正在与服务器交换登录信息，请稍候。</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">登录成功，正在跳转...</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authApi, applyTokenPair } from '@/api'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const error = ref('')
const success = ref(false)

onMounted(async () => {
  try {
    const code = route.query.code as string | undefined
    const state = route.query.state as string | undefined

    if (!code) {
      error.value = '缺少 Google 授权码（code），无法完成登录。'
      loading.value = false
      return
    }

    const redirectUri =
      (import.meta.env.VITE_GOOGLE_REDIRECT_URI as string | undefined) ||
      `${window.location.origin}/auth/google/callback`

    // 调用后端交换 code 为 TokenPairResponse
    const res = await authApi.googleLogin({
      code,
      redirectUri,
    })

    applyTokenPair(res.data)

    // 登录成功
    success.value = true
    loading.value = false

    // 根据 state 或默认跳转到首页
    const redirect = state || '/'
    setTimeout(() => {
      router.replace(redirect)
    }, 800)
  } catch (e: any) {
    console.error('Google 登录回调失败:', e)
    error.value = e?.message || 'Google 登录失败，请稍后重试。'
    loading.value = false
  }
})
</script>

<style scoped>
.google-callback {
  max-width: 480px;
  margin: 80px auto;
  padding: 24px;
  text-align: center;
  border-radius: 8px;
  background-color: #f9fafb;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

h1 {
  font-size: 20px;
  margin-bottom: 16px;
}

p {
  margin: 8px 0;
  color: #4b5563;
}

.error {
  color: #b91c1c;
}

.success {
  color: #15803d;
}
</style>


