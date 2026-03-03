<template>
  <div class="generate-container">
    <!-- 左侧：配置面板 -->
    <div class="config-panel">
      <div class="panel-header">
        <h3><el-icon><Edit /></el-icon> 智能写作</h3>
        <p class="subtitle">填写需求，一键生成规范公文</p>
      </div>
      
      <el-form label-position="top" class="config-form">
        <el-form-item label="公文类型">
          <el-radio-group v-model="docType" size="large">
            <el-radio-button label="通知">通知</el-radio-button>
            <el-radio-button label="报告">报告</el-radio-button>
            <el-radio-button label="请示">请示</el-radio-button>
            <el-radio-button label="函">函</el-radio-button>
          </el-radio-group>
          <div style="margin-top: 8px;">
            <el-radio-group v-model="docType" size="small">
              <el-radio-button label="纪要">纪要</el-radio-button>
              <el-radio-button label="通报">通报</el-radio-button>
              <el-radio-button label="批复">批复</el-radio-button>
              <el-radio-button label="其他">其他</el-radio-button>
            </el-radio-group>
          </div>
        </el-form-item>

        <el-form-item label="公文标题">
          <el-input v-model="title" placeholder="如：关于举办XX活动的通知" clearable />
        </el-form-item>

        <el-form-item label="需求描述">
          <el-input
            v-model="requirement"
            type="textarea"
            :rows="8"
            placeholder="请详细描述公文的主题、背景、目的、主要内容等信息..."
            resize="none"
          />
        </el-form-item>

        <el-button type="primary" size="large" class="generate-btn" @click="generate" :loading="loading">
          <el-icon><MagicStick /></el-icon> {{ loading ? '正在生成...' : '立即生成' }}
        </el-button>
      </el-form>
    </div>

    <!-- 右侧：预览区域 -->
    <div class="preview-panel">
      <!-- 空状态 -->
      <div class="empty-state" v-if="!generatedContent && !loading">
        <el-icon class="empty-icon"><DocumentAdd /></el-icon>
        <p>左侧输入需求，AI 将为您生成专业公文</p>
      </div>
      
      <!-- 加载状态 -->
      <div class="loading-state" v-if="loading">
        <div class="loading-paper">
          <div class="line l1"></div>
          <div class="line l2"></div>
          <div class="line l3"></div>
          <div class="line l4"></div>
        </div>
        <p>AI 正在奋笔疾书...</p>
      </div>

      <!-- 文档预览 -->
      <div class="paper-wrapper" v-if="generatedContent && !loading">
         <div class="paper-actions">
           <el-tooltip content="复制"><el-button circle size="small" @click="copyContent"><el-icon><CopyDocument /></el-icon></el-button></el-tooltip>
           <el-tooltip content="AI润色"><el-button circle size="small" type="warning" @click="polishContent" :loading="polishing"><el-icon><Brush /></el-icon></el-button></el-tooltip>
           <el-tooltip content="下载Word"><el-button circle size="small" type="success" @click="downloadWord"><el-icon><Download /></el-icon></el-button></el-tooltip>
         </div>

         <div class="paper-sheet a4-paper">
            <h1 class="paper-title">{{ title || '公文标题' }}</h1>
            <div class="paper-content">
              <el-input
                v-model="generatedContent"
                type="textarea"
                autosize
                class="editable-textarea"
              />
            </div>
         </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { aiGenerate, aiPolish } from '../api'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { Edit, MagicStick, DocumentAdd, CopyDocument, Brush, Download } from '@element-plus/icons-vue'

const docType = ref('通知')
const title = ref('')
const requirement = ref('')
const generatedContent = ref('')
const loading = ref(false)
const polishing = ref(false)

const generate = async () => {
  if (!requirement.value.trim()) {
    ElMessage.warning('请填写具体需求')
    return
  }
  loading.value = true
  generatedContent.value = '' // clear previous
  try {
    const res = await aiGenerate({ docType: docType.value, requirement: requirement.value })
    generatedContent.value = res.data.content
    ElMessage.success('生成成功！')
  } catch (e) {
    ElMessage.error('生成失败，请重试')
  } finally {
    loading.value = false
  }
}

const polishContent = async () => {
  polishing.value = true
  try {
    const res = await aiPolish({ text: generatedContent.value })
    generatedContent.value = res.data.polished
    ElMessage.success('润色完成！')
  } catch (e) {
    ElMessage.error('润色失败')
  } finally {
    polishing.value = false
  }
}

const downloadWord = async () => {
  try {
    const response = await axios.post('/api/ai/generate-word',
      { title: title.value || '公文', content: generatedContent.value },
      { responseType: 'blob' }
    )
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = (title.value || '公文') + '.docx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

const copyContent = () => {
  navigator.clipboard.writeText(generatedContent.value)
  ElMessage.success('已复制到剪贴板')
}
</script>

<style scoped>
.generate-container {
  display: flex;
  height: 100%;
  gap: 24px;
}

.config-panel {
  width: 360px;
  background: var(--white);
  border-radius: var(--border-radius);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.panel-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  font-size: 18px;
  gap: 8px;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 12px;
  margin: 4px 0 20px 0;
}

.config-form {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.generate-btn {
  margin-top: auto;
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.4);
}

.preview-panel {
  flex: 1;
  background: #eef2f5; /* 深一点的背景，突出A4纸 */
  border-radius: var(--border-radius);
  position: relative;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  padding: 40px;
}

.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  height: 100%;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #dcdfe6;
}

/* Loading Animation */
.loading-paper {
  width: 60px;
  height: 80px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
  padding: 10px;
  position: relative;
  animation: pulse 1.5s infinite ease-in-out;
}

.line {
  height: 4px;
  background: #e0e0e0;
  margin-bottom: 6px;
  border-radius: 2px;
}
.l1 { width: 80%; }
.l2 { width: 100%; }
.l3 { width: 90%; }
.l4 { width: 70%; }

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.05); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

/* A4 Paper Style */
.paper-wrapper {
  position: relative;
  width: 100%;
  max-width: 794px; /* A4 width at 96dpi approx */
}

.paper-sheet {
  background: #fff;
  min-height: 1123px; /* A4 height */
  padding: 60px 80px;
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
  position: relative;
}

.paper-actions {
  position: absolute;
  top: 16px;
  right: -56px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.paper-title {
  text-align: center;
  font-family: 'SimSun', serif; /* 宋体，公文常用 */
  font-size: 28px;
  margin-bottom: 40px;
  color: #000;
}

.paper-content {
  font-family: 'FangSong', serif; /* 仿宋，公文正文常用 */
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

.editable-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 0;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
  resize: vertical; /* Allow manual resize if needed */
}
</style>
