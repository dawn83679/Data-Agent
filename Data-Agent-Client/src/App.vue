<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <div class="logo-section">
          <h1 class="logo">Data Agent</h1>
        </div>
        <nav class="main-nav">
          <RouterLink to="/" class="nav-link">首页</RouterLink>
          <RouterLink to="/drivers" class="nav-link">驱动管理</RouterLink>
          <RouterLink to="/connections" class="nav-link">连接管理</RouterLink>
          <RouterLink to="/about" class="nav-link">关于</RouterLink>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-md) var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-section {
  display: flex;
  align-items: center;
}

.logo {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0;
}

.main-nav {
  display: flex;
  gap: var(--spacing-sm);
}

.nav-link {
  padding: var(--spacing-sm) var(--spacing-md);
  text-decoration: none;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: var(--transition);
  font-weight: 500;
}

.nav-link:hover {
  background: var(--color-bg-hover);
  color: var(--color-text);
}

.nav-link.router-link-exact-active {
  background: var(--color-primary);
  color: white;
}

.app-main {
  flex: 1;
  background: var(--color-bg-secondary);
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .main-nav {
    flex-wrap: wrap;
    justify-content: center;
  }

  .logo {
    font-size: 20px;
  }
}
</style>
