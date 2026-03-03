<template>
  <el-container class="app-wrapper">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="sidebar-container">
      <div class="logo-wrapper" :class="{ 'collapsed': isCollapse }">
        <div class="logo-icon">📄</div>
        <span v-if="!isCollapse" class="logo-text">DocAI 智能公文</span>
      </div>

      <el-menu
        :default-active="route.path"
        router
        :collapse="isCollapse"
        class="el-menu-vertical-demo"
        background-color="#ffffff"
        text-color="#1f2329"
        active-text-color="#1677ff"
      >
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <template #title>文档管理</template>
        </el-menu-item>
        <el-menu-item index="/ai-chat">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>AI 对话助手</template>
        </el-menu-item>
        <el-menu-item index="/ai-generate">
          <el-icon><EditPen /></el-icon>
          <template #title>智能公文写作</template>
        </el-menu-item>
      </el-menu>
      
      <!-- 底部折叠按钮 -->
      <div class="collapse-btn-wrapper" @click="toggleCollapse">
        <el-icon :class="{ 'is-collapsed': isCollapse }"><Fold /></el-icon>
      </div>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部 Header -->
      <el-header class="navbar">
        <div class="header-left">
          <span class="route-title">{{ currentRouteTitle }}</span>
        </div>
        <div class="header-right">
          <el-tooltip content="帮助文档" placement="bottom">
             <el-button circle plain><el-icon><QuestionFilled /></el-icon></el-button>
          </el-tooltip>
          <el-dropdown trigger="click">
            <div class="avatar-wrapper">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">管理员</span>
              <el-icon class="el-icon--right"><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主要内容区域 -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isCollapse = ref(false)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const currentRouteTitle = computed(() => {
  switch (route.path) {
    case '/documents': return '文档管理'
    case '/ai-chat': return 'AI 智能对话'
    case '/ai-generate': return '公文智能写作'
    default: return 'Docai'
  }
})
</script>

<style scoped>
.app-wrapper {
  height: 100vh;
  width: 100%;
  background-color: var(--bg-color);
}

.sidebar-container {
  background-color: #fff;
  border-right: 1px solid #eef0f2;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  box-shadow: 2px 0 8px rgba(0,0,0,0.02);
  z-index: 10;
}

.logo-wrapper {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid #f5f6f7;
  overflow: hidden;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
}

.collapsed .logo-text {
  display: none;
}

.collapsed .logo-icon {
  margin-right: 0;
}

.el-menu-vertical-demo {
  border-right: none;
  flex: 1;
}

.el-menu-item {
  margin: 4px 8px;
  border-radius: 6px;
  height: 48px;
  line-height: 48px;
}

.el-menu-item:hover {
  background-color: #f5f6f7 !important;
}

.el-menu-item.is-active {
  background-color: #e6f4ff !important;
  color: #1677ff !important;
  font-weight: 500;
}

.collapse-btn-wrapper {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #f5f6f7;
  cursor: pointer;
  color: #646a73;
}

.collapse-btn-wrapper:hover {
  background-color: #f5f6f7;
}

.main-container {
  flex-direction: column;
  background-color: #f5f6f7;
  overflow: hidden;
}

.navbar {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
  z-index: 9;
}

.route-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.avatar-wrapper:hover {
  background: #f5f6f7;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #1f2329;
}

.app-main {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
  position: relative;
}

/* 渐变过渡动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.4s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
