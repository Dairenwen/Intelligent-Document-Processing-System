<template>
  <div class="layout-wrapper">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- Logo -->
      <div class="sidebar-logo" @click="$router.push('/dashboard')">
        <div class="logo-icon-wrap">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <rect x="2" y="2" width="20" height="20" rx="4" fill="url(#logoGrad)" />
            <path d="M7 8h10M7 12h7M7 16h10" stroke="white" stroke-width="1.5" stroke-linecap="round" />
            <defs>
              <linearGradient id="logoGrad" x1="2" y1="2" x2="22" y2="22">
                <stop stop-color="#667eea" /><stop offset="1" stop-color="#764ba2" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <transition name="fade">
          <div v-if="!isCollapsed" class="logo-text">
            <span class="logo-title">DocAI</span>
            <span class="logo-subtitle">智能文档处理</span>
          </div>
        </transition>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <div class="nav-icon">
            <el-icon :size="20"><component :is="item.icon" /></el-icon>
          </div>
          <transition name="fade">
            <span v-if="!isCollapsed" class="nav-label">{{ item.label }}</span>
          </transition>
          <transition name="fade">
            <span v-if="!isCollapsed && item.badge" class="nav-badge">{{ item.badge }}</span>
          </transition>
        </router-link>
      </nav>

      <!-- 模块分隔 -->
      <div v-if="!isCollapsed" class="sidebar-section-title">三大核心模块</div>
      <nav class="sidebar-nav modules">
        <div v-for="mod in moduleInfo" :key="mod.id" class="module-tag" :class="mod.color">
          <div class="module-dot"></div>
          <span v-if="!isCollapsed">{{ mod.label }}</span>
        </div>
      </nav>

      <!-- 折叠按钮 -->
      <div class="sidebar-footer">
        <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon :size="18">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-area">
      <!-- 顶栏 -->
      <header class="topbar glass">
        <div class="topbar-left">
          <h2 class="page-title">{{ currentTitle }}</h2>
          <div class="breadcrumb-info">
            <el-tag size="small" effect="plain" round>{{ currentModule }}</el-tag>
          </div>
        </div>
        <div class="topbar-right">
          <el-tooltip content="系统状态" placement="bottom">
            <div class="status-indicator online">
              <span class="status-dot"></span>
              <span class="status-text">服务在线</span>
            </div>
          </el-tooltip>
          <el-dropdown trigger="click">
            <div class="user-area">
              <el-avatar :size="34" class="user-avatar">
                <span style="font-size: 14px;">管</span>
              </el-avatar>
              <span v-if="true" class="user-name">管理员</span>
              <el-icon class="arrow"><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item>系统设置</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand, CaretBottom } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapsed = ref(false)

const menuItems = [
  { path: '/dashboard', label: '工作台', icon: 'DataAnalysis' },
  { path: '/documents', label: '文档管理', icon: 'FolderOpened' },
  { path: '/autofill', label: '智能填表', icon: 'Grid', badge: '核心' },
  { path: '/ai-chat', label: 'AI 对话', icon: 'ChatDotRound' },
  { path: '/ai-generate', label: '智能写作', icon: 'EditPen' }
]

const moduleInfo = [
  { id: 1, label: '文档智能编辑交互', color: 'mod-blue' },
  { id: 2, label: '非结构化信息提取', color: 'mod-purple' },
  { id: 3, label: '表格自动数据填写', color: 'mod-green' }
]

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')

const titleMap = {
  '/dashboard': '工作台',
  '/documents': '文档管理',
  '/autofill': '智能填表',
  '/ai-chat': 'AI 智能对话',
  '/ai-generate': '智能公文写作'
}

const moduleMap = {
  '/dashboard': '系统总览',
  '/documents': '模块二：非结构化文档信息提取',
  '/autofill': '模块三：表格自动数据填写',
  '/ai-chat': '模块一：文档智能编辑交互',
  '/ai-generate': '模块一：文档智能编辑交互'
}

const currentTitle = computed(() => titleMap[route.path] || 'DocAI')
const currentModule = computed(() => moduleMap[route.path] || '智能文档处理系统')
</script>

<style scoped>
.layout-wrapper {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

/* ============ 侧边栏 ============ */
.sidebar {
  width: var(--sidebar-width);
  background: var(--bg-white);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-slow);
  z-index: 20;
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed);
}

/* Logo */
.sidebar-logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  cursor: pointer;
  transition: all var(--transition-base);
  flex-shrink: 0;
}

.sidebar-logo:hover {
  background: var(--primary-lighter);
}

.logo-icon-wrap {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  white-space: nowrap;
}

.logo-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.logo-subtitle {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 400;
}

/* 导航 */
.sidebar-nav {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  position: relative;
  white-space: nowrap;
}

.nav-item:hover {
  background: var(--bg-base);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--primary-lighter);
  color: var(--primary);
}

.nav-item.active .nav-icon {
  color: var(--primary);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--primary);
  border-radius: 0 4px 4px 0;
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-badge {
  margin-left: auto;
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 600;
  background: var(--primary-gradient);
  color: white;
  border-radius: var(--radius-full);
}

/* 模块信息 */
.sidebar-section-title {
  padding: 16px 24px 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar-nav.modules {
  padding-top: 0;
}

.module-tag {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.module-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.mod-blue .module-dot { background: #3B82F6; }
.mod-purple .module-dot { background: #8B5CF6; }
.mod-green .module-dot { background: #10B981; }

/* 折叠按钮 */
.sidebar-footer {
  margin-top: auto;
  padding: 12px;
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  background: var(--bg-base);
  color: var(--text-primary);
}

/* ============ 主内容区 ============ */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 顶栏 */
.topbar {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--border-light);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
}

.status-indicator.online {
  background: #ECFDF5;
  color: #059669;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.user-area:hover {
  background: var(--bg-base);
}

.user-avatar {
  background: var(--primary-gradient) !important;
  color: white;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.arrow {
  font-size: 12px;
  color: var(--text-muted);
}

/* 页面内容 */
.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  background: var(--bg-base);
}
</style>
