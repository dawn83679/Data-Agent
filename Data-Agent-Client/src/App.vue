<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { computed, watch } from 'vue'
import HelloWorld from './components/HelloWorld.vue'

const route = useRoute()
// 数据库页面全屏显示，不显示header
const isFullScreen = computed(() => route.name === 'database')

// 动态添加/移除全屏类
watch(isFullScreen, (fullscreen) => {
  const app = document.getElementById('app')
  if (app) {
    if (fullscreen) {
      app.classList.add('fullscreen')
      document.body.classList.add('fullscreen-mode')
      document.body.style.overflow = 'hidden'
      document.body.style.margin = '0'
      document.body.style.padding = '0'
    } else {
      app.classList.remove('fullscreen')
      document.body.classList.remove('fullscreen-mode')
      document.body.style.overflow = ''
      document.body.style.margin = ''
      document.body.style.padding = ''
    }
  }
}, { immediate: true })
</script>

<template>
  <template v-if="!isFullScreen">
    <header>
      <img alt="Vue logo" class="logo" src="@/assets/logo.svg" width="125" height="125" />

      <div class="wrapper">
        <HelloWorld msg="You did it!" />

        <nav>
          <RouterLink to="/">Home</RouterLink>
          <RouterLink to="/about">About</RouterLink>
          <RouterLink to="/api-test">API 测试</RouterLink>
          <RouterLink to="/database">数据库管理</RouterLink>
        </nav>
      </div>
    </header>
  </template>

  <RouterView />
</template>

<style scoped>
header {
  line-height: 1.5;
  max-height: 100vh;
}

.logo {
  display: block;
  margin: 0 auto 2rem;
}

nav {
  width: 100%;
  font-size: 12px;
  text-align: center;
  margin-top: 2rem;
}

nav a.router-link-exact-active {
  color: var(--color-text);
}

nav a.router-link-exact-active:hover {
  background-color: transparent;
}

nav a {
  display: inline-block;
  padding: 0 1rem;
  border-left: 1px solid var(--color-border);
}

nav a:first-of-type {
  border: 0;
}

@media (min-width: 1024px) {
  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  .logo {
    margin: 0 2rem 0 0;
  }

  header .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }

  nav {
    text-align: left;
    margin-left: -1rem;
    font-size: 1rem;

    padding: 1rem 0;
    margin-top: 1rem;
  }
}
</style>
