<template>
  <div class="home-view">
    <div class="hero-section">
      <h1 class="hero-title">Data Agent</h1>
      <p class="hero-subtitle">AGENT数据库连接与驱动管理平台</p>
    </div>

    <div class="feature-cards">
      <RouterLink to="/drivers" class="feature-card">
        <div class="card-icon">🔌</div>
        <h3>驱动管理</h3>
        <p>下载、管理和维护 JDBC 数据库驱动</p>
      </RouterLink>

      <RouterLink to="/connections" class="feature-card">
        <div class="card-icon">🔗</div>
        <h3>连接管理</h3>
        <p>创建和管理数据库连接配置</p>
      </RouterLink>
    </div>

    <div class="quick-actions">
      <h2>快速开始</h2>
      <div class="action-steps">
        <div class="step">
          <span class="step-number">1</span>
          <div class="step-content">
            <h4>下载驱动</h4>
            <p>选择数据库类型并下载对应的 JDBC 驱动</p>
          </div>
        </div>
        <div class="step">
          <span class="step-number">2</span>
          <div class="step-content">
            <h4>创建连接</h4>
            <p>配置数据库连接参数并保存</p>
          </div>
        </div>
        <div class="step">
          <span class="step-number">3</span>
          <div class="step-content">
            <h4>开始使用</h4>
            <p>使用已配置的连接进行数据库操作</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.home-view {
  padding: var(--spacing-xl);
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  padding: var(--spacing-xl) 0;
  margin-bottom: var(--spacing-xl);
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: var(--spacing-md);
}

.hero-subtitle {
  font-size: 20px;
  color: var(--color-text-secondary);
}

.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.feature-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  text-decoration: none;
  color: inherit;
  transition: var(--transition);
  cursor: pointer;
}

.feature-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.feature-card h3 {
  font-size: 24px;
  margin-bottom: var(--spacing-sm);
  color: var(--color-text);
}

.feature-card p {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.quick-actions {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
}

.quick-actions h2 {
  font-size: 28px;
  margin-bottom: var(--spacing-lg);
  color: var(--color-text);
}

.action-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
}

.step {
  display: flex;
  gap: var(--spacing-md);
}

.step-number {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
}

.step-content h4 {
  font-size: 18px;
  margin-bottom: var(--spacing-xs);
  color: var(--color-text);
}

.step-content p {
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
