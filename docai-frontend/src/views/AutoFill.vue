<template>
  <div class="autofill-page">
    <!-- 步骤条 -->
    <div class="steps-header card">
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step title="上传模板" description="上传Word/Excel模板文件" />
        <el-step title="智能填充" description="AI自动匹配数据库文档数据" />
        <el-step title="结果预览" description="预览/下载填充结果" />
      </el-steps>
    </div>

    <!-- Step 1: 上传模板 -->
    <div v-if="currentStep === 0" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <div>
            <h3><el-icon :size="18"><Document /></el-icon> 上传模板文件</h3>
            <p class="hint">上传模板后，系统将自动从数据库中匹配已上传的文档数据进行智能填充</p>
          </div>
        </div>

        <!-- 数据源提示 -->
        <div class="source-info" v-loading="loadingStats">
          <div class="source-info-inner">
            <el-icon :size="20" color="#4F46E5"><InfoFilled /></el-icon>
            <span>数据库中已有 <strong>{{ docCount }}</strong> 个文档可用作数据源（其中 <strong>{{ extractedCount }}</strong> 个已成功提取内容）</span>
            <el-button size="small" type="primary" plain class="manage-docs-btn" @click="$router.push('/documents')">管理文档</el-button>
          </div>
          <div v-if="extractedCount === 0" class="source-warning">
            <el-icon color="#E65100"><WarningFilled /></el-icon>
            <span>暂无已提取数据的文档，请先前往文档管理页面上传并提取文档内容</span>
          </div>
        </div>

        <div class="template-upload-area">
          <el-upload
            ref="templateUploadRef"
            drag
            multiple
            :auto-upload="false"
            :on-change="handleTemplateChange"
            :on-remove="handleTemplateRemove"
            accept=".docx,.xlsx"
            class="template-uploader"
          >
            <div class="upload-inner">
              <el-icon :size="48" color="#818CF8"><UploadFilled /></el-icon>
              <p class="upload-main-text">将模板文件拖到此处，或 <em>点击选择</em></p>
              <p class="upload-hint">支持 .docx / .xlsx 格式，可同时上传多个模板</p>
            </div>
          </el-upload>
        </div>

        <div class="template-tips card" style="background: #FFFBEB; border-color: #FDE68A;">
          <h4><el-icon :size="14"><InfoFilled /></el-icon> 模板使用提示</h4>
          <ul>
            <li>Word模板：在需要填写的位置使用 <code v-pre>{{字段名}}</code> 占位符，或留空表格单元格</li>
            <li>Excel模板：第一行作为表头，下方空单元格将根据表头名称自动填充</li>
            <li>AI 会自动识别模板中的待填字段，并从数据库中所有已提取的文档中匹配数据</li>
          </ul>
        </div>

        <div class="select-summary">
          <span>已选择 <strong>{{ templateFiles.length }}</strong> 个模板文件</span>
          <el-button type="primary" :disabled="templateFiles.length === 0 || extractedCount === 0" @click="startFill">
            <el-icon><MagicStick /></el-icon> 开始智能填充
          </el-button>
        </div>
      </div>
    </div>

    <!-- Step 2: 填充进度 -->
    <div v-if="currentStep === 1" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <h3><el-icon :size="18"><MagicStick /></el-icon> AI 正在智能填充...</h3>
        </div>
        <div class="filling-progress">
          <div class="progress-animation">
            <div class="pulse-ring"></div>
            <div class="pulse-core">
              <el-icon :size="40" color="white"><MagicStick /></el-icon>
            </div>
          </div>
          <div class="progress-steps">
            <div class="p-step" :class="{ done: fillPhase >= 1, active: fillPhase === 0 }">
              <div class="p-dot"></div><span>读取数据库文档</span>
            </div>
            <div class="p-step" :class="{ done: fillPhase >= 2, active: fillPhase === 1 }">
              <div class="p-dot"></div><span>分析模板结构</span>
            </div>
            <div class="p-step" :class="{ done: fillPhase >= 3, active: fillPhase === 2 }">
              <div class="p-dot"></div><span>AI提取匹配数据</span>
            </div>
            <div class="p-step" :class="{ done: fillPhase >= 4, active: fillPhase === 3 }">
              <div class="p-dot"></div><span>填充模板</span>
            </div>
          </div>
          <div class="progress-info">
            <el-progress :percentage="Math.min(fillProgress, 100)" :stroke-width="8" color="#4F46E5" :format="formatProgress" />
            <p class="progress-detail">数据源: {{ extractedCount }} 个文档 | 模板: {{ templateFiles.length }} 个</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Step 3: 结果 -->
    <div v-if="currentStep >= 2" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <div>
            <h3>{{ fillError ? '填充失败' : '填充完成' }}</h3>
          </div>
          <el-button @click="resetAll">
            <el-icon><RefreshRight /></el-icon> 继续填表
          </el-button>
        </div>
        <div class="result-area">
          <div class="result-success" v-if="fillResult && !fillError">
            <div class="result-icon"><el-icon :size="64" color="#10B981"><SuccessFilled /></el-icon></div>
            <h4>智能填表已完成！</h4>
            <p>耗时: {{ fillTimeDisplay }}，已使用 {{ extractedCount }} 个文档数据源</p>
            <div class="result-file-list" v-if="resultFiles.length > 0">
              <div class="result-file-item" v-for="(f, i) in resultFiles" :key="i">
                <span class="rf-icon"><el-icon :size="24" :color="f.name.endsWith('.xlsx') ? '#10B981' : '#3B82F6'"><Document /></el-icon></span>
                <span class="rf-name">{{ f.name }}</span>
                <el-button size="small" type="primary" @click="downloadSingleResult(f)">
                  <el-icon><Download /></el-icon> 下载
                </el-button>
              </div>
            </div>
            <div class="result-actions" v-else>
              <el-button type="primary" size="large" @click="downloadResult">
                <el-icon><Download /></el-icon> 下载填充结果
              </el-button>
            </div>
          </div>
          <div class="result-error" v-if="fillError">
            <div class="result-icon"><el-icon :size="64" color="#EF4444"><CircleCloseFilled /></el-icon></div>
            <h4>填充失败</h4>
            <p>{{ fillError }}</p>
            <el-button type="primary" @click="currentStep = 0">重新选择模板</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDocuments, autoFillAuto, autoFillAutoBatch, downloadBlob } from '../api'
import { ElMessage } from 'element-plus'
import {
  UploadFilled, MagicStick, Download, InfoFilled, WarningFilled, RefreshRight,
  Document, SuccessFilled, CircleCloseFilled
} from '@element-plus/icons-vue'

const router = useRouter()

// State
const currentStep = ref(0)
const templateFiles = ref([])
const loadingStats = ref(false)
const docCount = ref(0)
const extractedCount = ref(0)

// Fill state
const fillProgress = ref(0)
const fillPhase = ref(0)
const progressText = ref('准备中...')
const fillResult = ref(null)
const fillError = ref('')
const fillTimeMs = ref(0)
const resultFiles = ref([])

const fillTimeDisplay = computed(() => {
  if (fillTimeMs.value < 1000) return fillTimeMs.value + 'ms'
  return (fillTimeMs.value / 1000).toFixed(1) + 's'
})

const formatProgress = (percentage) => {
  return Math.floor(percentage) + '%'
}

// Methods
const loadStats = async () => {
  loadingStats.value = true
  try {
    const res = await getDocuments({ size: 500 })
    const docs = res.data?.records || []
    docCount.value = docs.length
    extractedCount.value = docs.filter(d => d.contentText && d.contentText.trim()).length
  } catch (e) {
    console.error('加载文档统计失败', e)
  } finally {
    loadingStats.value = false
  }
}

const handleTemplateChange = (file) => {
  templateFiles.value.push(file)
}

const handleTemplateRemove = (file) => {
  const idx = templateFiles.value.indexOf(file)
  if (idx >= 0) templateFiles.value.splice(idx, 1)
}

const startFill = async () => {
  currentStep.value = 1
  fillProgress.value = 0
  fillPhase.value = 0
  fillResult.value = null
  fillError.value = ''
  resultFiles.value = []
  progressText.value = '正在读取数据库文档数据...'

  // 模拟进度
  const progressTimer = setInterval(() => {
    if (fillProgress.value < 90) {
      fillProgress.value += Math.random() * 6
      if (fillProgress.value > 20 && fillPhase.value < 1) { fillPhase.value = 1; progressText.value = '分析模板结构...' }
      if (fillProgress.value > 45 && fillPhase.value < 2) { fillPhase.value = 2; progressText.value = 'AI正在提取匹配数据...' }
      if (fillProgress.value > 70 && fillPhase.value < 3) { fillPhase.value = 3; progressText.value = '填充模板中...' }
    }
  }, 600)

  try {
    const formData = new FormData()
    let response

    if (templateFiles.value.length === 1) {
      formData.append('template', templateFiles.value[0].raw)
      response = await autoFillAuto(formData)
    } else {
      templateFiles.value.forEach(f => formData.append('templates', f.raw))
      response = await autoFillAutoBatch(formData)
    }

    clearInterval(progressTimer)
    fillProgress.value = 100
    fillPhase.value = 4
    progressText.value = '完成！'

    fillTimeMs.value = parseInt(response.headers?.['x-fill-time-ms'] || '0')
    fillResult.value = response.data

    // 构建结果文件列表
    if (templateFiles.value.length === 1) {
      resultFiles.value = [{
        name: 'filled_' + (templateFiles.value[0]?.name || 'result'),
        blob: response.data
      }]
    }

    setTimeout(() => { currentStep.value = 3 }, 800)

  } catch (e) {
    clearInterval(progressTimer)
    // Blob响应无法直接读取message，需要解析
    let errMsg = e.message || '填充失败，请重试'
    if (e.response?.data instanceof Blob) {
      try { errMsg = await e.response.data.text() } catch (_) {}
    } else if (e.response?.data?.message) {
      errMsg = e.response.data.message
    }
    fillError.value = errMsg
    fillProgress.value = 0
    currentStep.value = 2
    ElMessage.error('填充失败: ' + fillError.value)
  }
}

const downloadResult = () => {
  if (!fillResult.value) return
  const isZip = templateFiles.value.length > 1
  const filename = isZip ? 'filled_templates.zip' : 'filled_' + (templateFiles.value[0]?.name || 'result')
  downloadBlob(new Blob([fillResult.value]), filename)
  ElMessage.success('下载成功')
}

const downloadSingleResult = (f) => {
  downloadBlob(new Blob([f.blob]), f.name)
  ElMessage.success('下载成功')
}

const resetAll = () => {
  currentStep.value = 0
  templateFiles.value = []
  fillResult.value = null
  fillError.value = ''
  fillProgress.value = 0
  fillPhase.value = 0
  resultFiles.value = []
  loadStats()
}

onMounted(loadStats)
</script>

<style scoped>
.autofill-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.steps-header {
  padding: 24px 40px;
}

.step-content {
  flex: 1;
  min-height: 0;
}

.step-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 24px 28px 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-light);
}

.panel-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* Source info banner */
.source-info {
  margin: 16px 28px 0;
  padding: 14px 18px;
  background: rgba(79, 70, 229, 0.04);
  border: 1px solid rgba(79, 70, 229, 0.15);
  border-radius: var(--radius-md);
}

.source-info-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.source-info-inner strong {
  color: var(--primary);
  font-weight: 700;
}

.source-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(230, 81, 0, 0.3);
  font-size: 13px;
  color: #E65100;
}

.select-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px 20px;
  border-top: 1px solid var(--border-light);
  margin-top: 12px;
  font-size: 14px;
  color: var(--text-secondary);
}

/* Template upload */
.template-upload-area {
  padding: 24px 28px;
}

.template-uploader :deep(.el-upload-dragger) {
  border: 2px dashed var(--border);
  border-radius: var(--radius-lg);
  padding: 48px;
  transition: all var(--transition-base);
  background: var(--bg-base);
}

.template-uploader :deep(.el-upload-dragger:hover) {
  border-color: var(--primary-light);
  background: var(--primary-lighter);
}

.upload-inner {
  text-align: center;
}

.upload-main-text {
  font-size: 15px;
  color: var(--text-secondary);
  margin-top: 12px;
}

.upload-main-text em {
  color: var(--primary);
  font-style: normal;
  font-weight: 600;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.template-tips {
  margin: 0 28px 16px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
}

.template-tips h4 {
  font-size: 14px;
  margin-bottom: 8px;
}

.template-tips ul {
  font-size: 13px;
  color: var(--text-secondary);
  padding-left: 20px;
  line-height: 1.8;
}

.template-tips code {
  background: rgba(245, 158, 11, 0.15);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #D97706;
}

/* Filling progress */
.filling-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 28px;
  gap: 32px;
}

.progress-animation {
  position: relative;
  width: 100px;
  height: 100px;
}

.pulse-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 3px solid var(--primary-light);
  animation: pulse-ring-anim 1.5s infinite;
}

.pulse-core {
  position: absolute;
  inset: 15px;
  border-radius: 50%;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse-core-anim 1.5s infinite;
}

@keyframes pulse-ring-anim {
  0% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.2); opacity: 0; }
  100% { transform: scale(1); opacity: 0.6; }
}

@keyframes pulse-core-anim {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(0.95); }
}

.progress-info {
  width: 100%;
  max-width: 500px;
  text-align: center;
}

.progress-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 12px;
}

.progress-detail {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.progress-steps {
  display: flex;
  gap: 32px;
}

.p-step {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.p-step.active {
  color: var(--primary);
  font-weight: 600;
}

.p-step.done {
  color: var(--success);
}

.p-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.p-step.active .p-dot {
  animation: pulse-dot 1s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* Result */
.result-area {
  padding: 48px 28px;
  text-align: center;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.result-success h4,
.result-error h4 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

.result-success p,
.result-error p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.result-file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 480px;
  margin: 0 auto 24px;
  text-align: left;
}

.result-file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-base);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.rf-icon {
  font-size: 24px;
}

.rf-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.manage-docs-btn {
  background: rgba(79, 70, 229, 0.1) !important;
  border-color: var(--primary) !important;
  color: var(--primary) !important;
  font-weight: 500;
}

.manage-docs-btn:hover {
  background: rgba(79, 70, 229, 0.2) !important;
}
</style>
