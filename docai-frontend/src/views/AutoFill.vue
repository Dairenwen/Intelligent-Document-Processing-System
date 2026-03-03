<template>
  <div class="autofill-page">
    <!-- 步骤条 -->
    <div class="steps-header card">
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step title="选择源文档" description="选择已上传的数据源文档" />
        <el-step title="上传模板" description="上传Word/Excel模板文件" />
        <el-step title="智能填充" description="AI自动提取数据并填充" />
        <el-step title="下载结果" description="下载填充后的文件" />
      </el-steps>
    </div>

    <!-- Step 1: 选择源文档 -->
    <div v-if="currentStep === 0" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <div>
            <h3>📂 选择源文档</h3>
            <p class="hint">选择已上传的文档作为数据源，AI将从中提取信息来填充模板</p>
          </div>
          <el-button type="primary" @click="showUploadDialog = true">
            <el-icon><UploadFilled /></el-icon> 上传新文档
          </el-button>
        </div>
        <div class="doc-selector">
          <div class="search-bar">
            <el-input v-model="searchKeyword" placeholder="搜索文档..." clearable>
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-radio-group v-model="filterType" size="small">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="docx">Word</el-radio-button>
              <el-radio-button label="xlsx">Excel</el-radio-button>
              <el-radio-button label="txt">TXT</el-radio-button>
              <el-radio-button label="md">MD</el-radio-button>
            </el-radio-group>
          </div>

          <div class="doc-list" v-loading="loadingDocs">
            <el-empty v-if="filteredDocs.length === 0" description="暂无文档，请先上传" />
            <div
              v-for="doc in filteredDocs"
              :key="doc.id"
              class="doc-item"
              :class="{ selected: selectedDocIds.includes(doc.id) }"
              @click="toggleDoc(doc.id)"
            >
              <el-checkbox :model-value="selectedDocIds.includes(doc.id)" @click.stop="toggleDoc(doc.id)" />
              <div class="doc-type-icon">
                <span v-if="doc.fileType === 'docx'">📘</span>
                <span v-else-if="doc.fileType === 'xlsx'">📊</span>
                <span v-else-if="doc.fileType === 'md'">📝</span>
                <span v-else>📄</span>
              </div>
              <div class="doc-item-info">
                <span class="doc-name">{{ doc.title }}</span>
                <span class="doc-meta">{{ formatSize(doc.fileSize) }} · {{ formatDate(doc.createdAt) }}</span>
              </div>
              <el-tag size="small" :type="typeTagMap[doc.fileType] || 'info'" effect="plain">
                {{ doc.fileType?.toUpperCase() }}
              </el-tag>
            </div>
          </div>

          <div class="select-summary">
            <span>已选择 <strong>{{ selectedDocIds.length }}</strong> 个源文档</span>
            <el-button type="primary" :disabled="selectedDocIds.length === 0" @click="currentStep = 1">
              下一步：上传模板 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Step 2: 上传模板 -->
    <div v-if="currentStep === 1" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <div>
            <h3>📋 上传模板文件</h3>
            <p class="hint">支持 .docx 和 .xlsx 格式的模板文件，模板中可使用 <code v-pre>{{字段名}}</code> 作为占位符</p>
          </div>
          <el-button @click="currentStep = 0"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
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
          <h4>💡 模板使用提示</h4>
          <ul>
            <li>Word模板：在需要填写的位置使用 <code v-pre>{{字段名}}</code> 占位符，或留空表格单元格</li>
            <li>Excel模板：第一行作为表头，下方空单元格将根据表头名称自动填充</li>
            <li>AI会自动识别模板结构并从源文档中提取匹配数据</li>
          </ul>
        </div>

        <div class="select-summary">
          <span>已选择 <strong>{{ templateFiles.length }}</strong> 个模板文件</span>
          <el-button type="primary" :disabled="templateFiles.length === 0" @click="startFill">
            <el-icon><MagicStick /></el-icon> 开始智能填充
          </el-button>
        </div>
      </div>
    </div>

    <!-- Step 3: 填充进度 -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <h3>🤖 AI 正在智能填充...</h3>
        </div>
        <div class="filling-progress">
          <div class="progress-animation">
            <div class="pulse-ring"></div>
            <div class="pulse-core">
              <el-icon :size="40" color="white"><MagicStick /></el-icon>
            </div>
          </div>
          <div class="progress-info">
            <el-progress :percentage="fillProgress" :stroke-width="8" color="#4F46E5" />
            <p class="progress-text">{{ progressText }}</p>
            <p class="progress-detail">源文档: {{ selectedDocIds.length }} 个 | 模板: {{ templateFiles.length }} 个</p>
          </div>
          <div class="progress-steps">
            <div class="p-step" :class="{ done: fillPhase >= 1, active: fillPhase === 0 }">
              <div class="p-dot"></div><span>解析源文档</span>
            </div>
            <div class="p-step" :class="{ done: fillPhase >= 2, active: fillPhase === 1 }">
              <div class="p-dot"></div><span>分析模板结构</span>
            </div>
            <div class="p-step" :class="{ done: fillPhase >= 3, active: fillPhase === 2 }">
              <div class="p-dot"></div><span>AI提取数据</span>
            </div>
            <div class="p-step" :class="{ done: fillPhase >= 4, active: fillPhase === 3 }">
              <div class="p-dot"></div><span>填充模板</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Step 4: 结果 -->
    <div v-if="currentStep === 3" class="step-content">
      <div class="step-panel card">
        <div class="panel-header">
          <h3>✅ 填充完成</h3>
        </div>
        <div class="result-area">
          <div class="result-success" v-if="fillResult">
            <div class="result-icon">🎉</div>
            <h4>智能填表已完成！</h4>
            <p>耗时: {{ fillTimeDisplay }}</p>
            <div class="result-actions">
              <el-button type="primary" size="large" @click="downloadResult">
                <el-icon><Download /></el-icon> 下载填充结果
              </el-button>
              <el-button size="large" @click="resetAll">继续填表</el-button>
            </div>
          </div>
          <div class="result-error" v-if="fillError">
            <div class="result-icon">❌</div>
            <h4>填充失败</h4>
            <p>{{ fillError }}</p>
            <el-button type="primary" @click="currentStep = 1">重试</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传文档弹窗 -->
    <el-dialog v-model="showUploadDialog" title="上传源文档" width="520px">
      <el-upload
        drag
        multiple
        action="/api/documents/upload"
        :on-success="onDocUploadSuccess"
        :before-upload="beforeDocUpload"
        accept=".docx,.xlsx,.txt,.md"
      >
        <el-icon :size="48" class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .docx / .xlsx / .txt / .md 格式，单个文件不超过100MB</div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDocuments, autoFillSingle, autoFillBatch, downloadBlob } from '../api'
import { ElMessage } from 'element-plus'
import {
  Search, UploadFilled, ArrowRight, ArrowLeft, MagicStick, Download
} from '@element-plus/icons-vue'

// State
const currentStep = ref(0)
const loadingDocs = ref(false)
const documents = ref([])
const selectedDocIds = ref([])
const templateFiles = ref([])
const searchKeyword = ref('')
const filterType = ref('')
const showUploadDialog = ref(false)

// Fill state
const fillProgress = ref(0)
const fillPhase = ref(0)
const progressText = ref('准备中...')
const fillResult = ref(null)
const fillError = ref('')
const fillTimeMs = ref(0)

const typeTagMap = { docx: 'primary', xlsx: 'success', txt: 'info', md: 'warning' }

const filteredDocs = computed(() => {
  return documents.value.filter(doc => {
    const matchKeyword = !searchKeyword.value ||
      doc.title?.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchType = !filterType.value || doc.fileType === filterType.value
    return matchKeyword && matchType
  })
})

const fillTimeDisplay = computed(() => {
  if (fillTimeMs.value < 1000) return fillTimeMs.value + 'ms'
  return (fillTimeMs.value / 1000).toFixed(1) + 's'
})

// Methods
const loadDocuments = async () => {
  loadingDocs.value = true
  try {
    const res = await getDocuments({ size: 100 })
    documents.value = res.data?.records || []
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loadingDocs.value = false
  }
}

const toggleDoc = (id) => {
  const idx = selectedDocIds.value.indexOf(id)
  if (idx >= 0) {
    selectedDocIds.value.splice(idx, 1)
  } else {
    selectedDocIds.value.push(id)
  }
}

const handleTemplateChange = (file) => {
  templateFiles.value.push(file)
}

const handleTemplateRemove = (file) => {
  const idx = templateFiles.value.indexOf(file)
  if (idx >= 0) templateFiles.value.splice(idx, 1)
}

const beforeDocUpload = (file) => {
  const validExts = ['.docx', '.xlsx', '.txt', '.md']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExts.includes(ext)) {
    ElMessage.error('不支持的文件格式: ' + ext)
    return false
  }
  return true
}

const onDocUploadSuccess = () => {
  ElMessage.success('上传成功')
  loadDocuments()
}

const startFill = async () => {
  currentStep.value = 2
  fillProgress.value = 0
  fillPhase.value = 0
  fillResult.value = null
  fillError.value = ''

  // 模拟进度
  const progressTimer = setInterval(() => {
    if (fillProgress.value < 90) {
      fillProgress.value += Math.random() * 8
      if (fillProgress.value > 25 && fillPhase.value < 1) { fillPhase.value = 1; progressText.value = '分析模板结构...' }
      if (fillProgress.value > 50 && fillPhase.value < 2) { fillPhase.value = 2; progressText.value = 'AI正在提取数据...' }
      if (fillProgress.value > 75 && fillPhase.value < 3) { fillPhase.value = 3; progressText.value = '填充模板中...' }
    }
  }, 500)

  try {
    const formData = new FormData()
    selectedDocIds.value.forEach(id => formData.append('sourceDocIds', id))

    let response
    if (templateFiles.value.length === 1) {
      formData.append('template', templateFiles.value[0].raw)
      response = await autoFillSingle(formData)
    } else {
      templateFiles.value.forEach(f => formData.append('templates', f.raw))
      response = await autoFillBatch(formData)
    }

    clearInterval(progressTimer)
    fillProgress.value = 100
    fillPhase.value = 4
    progressText.value = '完成！'

    fillTimeMs.value = parseInt(response.headers?.['x-fill-time-ms'] || '0')
    fillResult.value = response.data

    setTimeout(() => { currentStep.value = 3 }, 800)

  } catch (e) {
    clearInterval(progressTimer)
    fillError.value = e.response?.data?.message || e.message || '填充失败，请重试'
    fillProgress.value = 0
    currentStep.value = 3
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

const resetAll = () => {
  currentStep.value = 0
  selectedDocIds.value = []
  templateFiles.value = []
  fillResult.value = null
  fillError.value = ''
  fillProgress.value = 0
  fillPhase.value = 0
}

const formatSize = (size) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

const formatDate = (d) => d ? d.split('T')[0] : '-'

onMounted(loadDocuments)
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

/* Document selector */
.doc-selector {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px 28px;
  overflow: hidden;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.search-bar .el-input {
  max-width: 320px;
}

.doc-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.doc-item:hover {
  background: var(--bg-base);
}

.doc-item.selected {
  background: var(--primary-lighter);
  border-color: var(--primary-light);
}

.doc-type-icon {
  font-size: 24px;
  width: 32px;
  text-align: center;
}

.doc-item-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 12px;
  color: var(--text-muted);
}

.select-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 8px;
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
  padding: 60px 28px;
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
</style>
