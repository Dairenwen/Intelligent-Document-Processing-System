<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎使用 DocAI 智能文档处理系统</h1>
        <p class="welcome-desc">基于大模型AI的非结构化文档信息提取与自动化表格填写平台</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" @click="$router.push('/autofill')">
            <el-icon><Grid /></el-icon> 开始智能填表
          </el-button>
          <el-button size="large" plain @click="$router.push('/documents')">
            <el-icon><UploadFilled /></el-icon> 上传文档
          </el-button>
          <el-button size="large" plain @click="$router.push('/ai-chat')">
            <el-icon><ChatDotRound /></el-icon> AI 对话
          </el-button>
        </div>
      </div>
      <div class="welcome-illustration">
        <svg width="200" height="160" viewBox="0 0 200 160" fill="none">
          <rect x="30" y="20" width="80" height="100" rx="8" fill="#E0E7FF" stroke="#818CF8" stroke-width="2"/>
          <rect x="45" y="35" width="50" height="4" rx="2" fill="#818CF8"/>
          <rect x="45" y="47" width="40" height="4" rx="2" fill="#C7D2FE"/>
          <rect x="45" y="59" width="50" height="4" rx="2" fill="#C7D2FE"/>
          <rect x="45" y="71" width="35" height="4" rx="2" fill="#C7D2FE"/>
          <rect x="45" y="83" width="50" height="4" rx="2" fill="#C7D2FE"/>
          <rect x="45" y="95" width="45" height="4" rx="2" fill="#C7D2FE"/>
          <!-- Arrow -->
          <path d="M120 70 L145 70" stroke="#818CF8" stroke-width="3" stroke-linecap="round"/>
          <path d="M140 62 L150 70 L140 78" stroke="#818CF8" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <!-- Result doc -->
          <rect x="155" y="30" width="35" height="45" rx="4" fill="#818CF8"/>
          <rect x="162" y="38" width="20" height="3" rx="1" fill="white"/>
          <rect x="162" y="45" width="15" height="3" rx="1" fill="white" opacity="0.7"/>
          <rect x="162" y="52" width="20" height="3" rx="1" fill="white" opacity="0.7"/>
          <rect x="162" y="59" width="12" height="3" rx="1" fill="white" opacity="0.7"/>
          <!-- AI Icon -->
          <circle cx="150" cy="110" r="22" fill="#F0FDF4" stroke="#10B981" stroke-width="2"/>
          <text x="150" y="116" text-anchor="middle" font-size="18">🤖</text>
        </svg>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" v-for="stat in statCards" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <el-icon :size="22" :color="stat.color"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 三大核心模块 -->
    <div class="section-title">
      <h3>🚀 三大核心模块</h3>
      <p>覆盖从文档理解到数据提取、自动填表的完整工作流</p>
    </div>
    <div class="modules-grid">
      <div class="module-card" v-for="mod in modules" :key="mod.id" @click="$router.push(mod.route)">
        <div class="module-header">
          <div class="module-number">{{ mod.id }}</div>
          <el-tag :type="mod.tagType" effect="plain" size="small" round>{{ mod.status }}</el-tag>
        </div>
        <h4 class="module-name">{{ mod.name }}</h4>
        <p class="module-desc">{{ mod.desc }}</p>
        <div class="module-features">
          <span v-for="f in mod.features" :key="f" class="feature-tag">{{ f }}</span>
        </div>
        <div class="module-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="section-title">
      <h3>⚡ 快速操作</h3>
    </div>
    <div class="quick-actions">
      <div class="action-card" @click="$router.push('/documents')">
        <el-icon :size="28" color="#3B82F6"><UploadFilled /></el-icon>
        <span>上传文档</span>
      </div>
      <div class="action-card" @click="$router.push('/autofill')">
        <el-icon :size="28" color="#8B5CF6"><Grid /></el-icon>
        <span>自动填表</span>
      </div>
      <div class="action-card" @click="$router.push('/ai-chat')">
        <el-icon :size="28" color="#10B981"><ChatDotRound /></el-icon>
        <span>AI对话</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDocumentStats } from '../api'
import { UploadFilled, Grid, ChatDotRound, EditPen, ArrowRight, FolderOpened, Document, DataAnalysis } from '@element-plus/icons-vue'

const stats = ref({ total: 0, docx: 0, xlsx: 0, txt: 0, md: 0 })

const statCards = ref([
  { label: '总文档数', value: '0', icon: 'FolderOpened', color: '#4F46E5', bg: '#EEF2FF' },
  { label: 'Word 文档', value: '0', icon: 'Document', color: '#3B82F6', bg: '#EFF6FF' },
  { label: 'Excel 表格', value: '0', icon: 'Grid', color: '#10B981', bg: '#ECFDF5' },
  { label: '文本文件', value: '0', icon: 'EditPen', color: '#F59E0B', bg: '#FFFBEB' }
])

const modules = [
  {
    id: 1,
    name: '文档智能编辑操作交互',
    desc: '基于自然语言的文档编辑、排版、格式调整、内容提取，AI理解指令自动处理文档',
    route: '/ai-chat',
    tagType: 'primary',
    status: '已就绪',
    features: ['AI对话', '文档编辑', '格式调整', '内容提取']
  },
  {
    id: 2,
    name: '非结构化文档信息提取',
    desc: '支持docx/xlsx/txt/md等多格式文档解析，AI自动提取关键信息并存储到数据库',
    route: '/documents',
    tagType: 'success',
    status: '已就绪',
    features: ['多格式解析', 'AI信息提取', '实体识别', '数据库存储']
  },
  {
    id: 3,
    name: '表格自动数据填写',
    desc: '上传模板文件，系统自动从已提取的文档数据中匹配并填充表格',
    route: '/autofill',
    tagType: 'warning',
    status: '核心功能',
    features: ['模板识别', '数据匹配', '自动填充', '批量处理']
  }
]

onMounted(async () => {
  try {
    const res = await getDocumentStats()
    if (res.data) {
      stats.value = res.data
      statCards.value[0].value = String(res.data.total || 0)
      statCards.value[1].value = String(res.data.docx || 0)
      statCards.value[2].value = String(res.data.xlsx || 0)
      statCards.value[3].value = String((res.data.txt || 0) + (res.data.md || 0))
    }
  } catch (e) {
    // 静默处理
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1400px;
}

/* Welcome Banner */
.welcome-banner {
  background: var(--primary-gradient);
  border-radius: var(--radius-xl);
  padding: 40px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: white;
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -60%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 10px;
  letter-spacing: -0.02em;
}

.welcome-desc {
  font-size: 15px;
  opacity: 0.85;
  margin-bottom: 24px;
}

.welcome-actions {
  display: flex;
  gap: 12px;
}

.welcome-actions .el-button--primary {
  background: white !important;
  color: var(--primary) !important;
  border-color: white !important;
  font-weight: 600;
}

.welcome-actions .el-button--primary:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.welcome-actions .el-button:not(.el-button--primary) {
  color: white;
  border-color: rgba(255, 255, 255, 0.5);
}

.welcome-illustration {
  position: relative;
  z-index: 1;
  opacity: 0.9;
}

/* Stats */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-light);
  transition: all var(--transition-base);
  cursor: default;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

/* Section Title */
.section-title {
  padding-top: 4px;
}

.section-title h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.section-title p {
  font-size: 13px;
  color: var(--text-muted);
}

/* Module Cards */
.modules-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.module-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  padding: 28px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}

.module-card:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow-lg), var(--shadow-glow);
  transform: translateY(-4px);
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.module-number {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  background: var(--primary-gradient);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.module-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.module-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
}

.module-features {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.feature-tag {
  padding: 3px 10px;
  font-size: 11px;
  background: var(--primary-lighter);
  color: var(--primary);
  border-radius: var(--radius-full);
  font-weight: 500;
}

.module-arrow {
  position: absolute;
  bottom: 24px;
  right: 24px;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.module-card:hover .module-arrow {
  color: var(--primary);
  transform: translateX(4px);
}

/* Quick Actions */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.action-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  padding: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--border-light);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.action-card:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow-md);
  color: var(--primary);
  transform: translateY(-2px);
}

/* Responsive */
@media (max-width: 1200px) {
  .modules-grid { grid-template-columns: 1fr; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .quick-actions { grid-template-columns: repeat(2, 1fr); }
}
</style>
