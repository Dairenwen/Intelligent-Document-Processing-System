<template>
  <div class="documents-page">
    <!-- 工具栏 -->
    <div class="toolbar card">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索文档名称..."
          clearable
          @clear="loadDocuments"
          @keyup.enter="loadDocuments"
          style="width: 300px;"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterType" placeholder="文件类型" clearable style="width: 130px;" @change="loadDocuments">
          <el-option label="Word" value="docx" />
          <el-option label="Excel" value="xlsx" />
          <el-option label="TXT" value="txt" />
          <el-option label="Markdown" value="md" />
        </el-select>
        <el-button @click="loadDocuments">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="selectedIds.length > 0" type="danger" plain @click="batchDelete">
          <el-icon><Delete /></el-icon> 删除选中 ({{ selectedIds.length }})
        </el-button>
        <el-button type="primary" @click="showUploadDialog = true">
          <el-icon><UploadFilled /></el-icon> 上传文档
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <el-tag effect="plain" round>
        共 <strong>{{ total }}</strong> 个文档
      </el-tag>
      <el-tag v-if="keyword" type="info" closable @close="keyword = ''; loadDocuments()">
        搜索: {{ keyword }}
      </el-tag>
    </div>

    <!-- 文档表格 -->
    <div
      class="table-wrapper card"
      v-loading="loading"
      @dragenter.prevent="onListDragEnter"
      @dragover.prevent="onListDragOver"
      @dragleave.prevent="onListDragLeave"
      @drop.prevent="onListDrop"
    >
      <div v-if="dragActive" class="drop-overlay">
        <el-icon :size="28"><UploadFilled /></el-icon>
        <span>拖放到此处上传文档</span>
      </div>

      <el-empty v-if="!loading && documents.length === 0" description="暂无文档，上传你的第一个文件吧" />
      <el-table
        v-else
        :data="documents"
        @selection-change="handleSelectionChange"
        style="width: 100%;"
        row-class-name="table-row"
        stripe
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="文档名称" min-width="280">
          <template #default="{ row }">
            <div class="doc-name-cell">
              <span class="doc-icon">
                <el-icon :size="20" v-if="row.fileType === 'docx'" color="#3B82F6"><Document /></el-icon>
                <el-icon :size="20" v-else-if="row.fileType === 'xlsx'" color="#10B981"><Grid /></el-icon>
                <el-icon :size="20" v-else-if="row.fileType === 'md'" color="#F59E0B"><EditPen /></el-icon>
                <el-icon :size="20" v-else color="#6B7280"><Document /></el-icon>
              </span>
              <div class="doc-name-wrap">
                <span class="doc-title">{{ row.title }}</span>
                <span v-if="row.contentText" class="doc-preview">
                  {{ row.contentText.substring(0, 80) }}...
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提取状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.contentText && row.contentText.length > 0" size="small" type="success" effect="plain" round>
              已提取
            </el-tag>
            <el-tag v-else size="small" type="danger" effect="plain" round>
              未提取
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTagMap[row.fileType]" effect="plain" round>
              {{ row.fileType?.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ formatSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="140" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="文件预览">
                <el-button size="small" type="warning" plain @click="viewContent(row)" :disabled="!row.contentText">
                  <el-icon><View /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="前往AI对话分析">
                <el-button size="small" type="primary" plain @click="goChat(row)">
                  <el-icon><ChatLineRound /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="AI信息提取">
                <el-button size="small" type="success" plain @click="extractInfo(row)">
                  <el-icon><DataAnalysis /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="下载">
                <el-button size="small" type="info" plain @click="downloadDoc(row)">
                  <el-icon><Download /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除">
                <el-button size="small" type="danger" plain @click="deleteDoc(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadDocuments"
          background
          small
        />
      </div>
    </div>

    <!-- 上传文档弹窗 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="560px" @close="onUploadDialogClose">
      <div class="upload-area">
        <el-upload
          ref="uploadRef"
          drag
          multiple
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          accept=".docx,.xlsx,.txt,.md"
          :file-list="uploadFileList"
        >
          <el-icon :size="48" class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或 <em>点击选择</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 .docx / .xlsx / .txt / .md 格式，单个文件不超过100MB</div>
          </template>
        </el-upload>
      </div>
      <div class="upload-info" v-if="uploadFileList.length > 0">
        <p>已选择 <strong>{{ uploadFileList.length }}</strong> 个文件</p>
      </div>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="doUpload" :loading="uploading" :disabled="uploadFileList.length === 0">
          <el-icon><UploadFilled /></el-icon> 上传并提取信息
        </el-button>
      </template>
    </el-dialog>

    <!-- AI信息提取结果弹窗 -->
    <el-dialog v-model="showInfoDialog" :title="infoDialogTitle" width="700px">
      <div class="info-result" v-loading="infoLoading">
        <div v-if="!infoLoading && extractedInfo" class="info-content">
          <div class="info-section" v-if="infoDocTitle">
            <h4>文档：{{ infoDocTitle }}</h4>
          </div>
          <div class="info-section">
            <pre class="info-text">{{ extractedInfo }}</pre>
          </div>
        </div>
        <el-empty v-if="!infoLoading && !extractedInfo" description="暂无提取信息" />
      </div>
    </el-dialog>

    <!-- 查看提取内容弹窗 -->
    <el-dialog v-model="showContentDialog" title="文档提取内容" width="700px">
      <div class="content-result">
        <div class="content-section" v-if="viewDocTitle">
          <h4>文档：{{ viewDocTitle }}</h4>
        </div>
        <div class="content-body">
          <pre class="content-text">{{ viewDocContent }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDocuments, deleteDocument, batchDeleteDocuments, aiExtractInfo, uploadDocument } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  Search, UploadFilled, Download, Delete, ChatLineRound,
  DataAnalysis, Refresh, View, Document, Grid, EditPen
} from '@element-plus/icons-vue'

const router = useRouter()
const documents = ref([])
const keyword = ref('')
const filterType = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedIds = ref([])

// 上传相关
const showUploadDialog = ref(false)
const uploadFileList = ref([])
const uploading = ref(false)
const uploadRef = ref(null)
const dragActive = ref(false)
const dragDepth = ref(0)

// 信息提取相关
const showInfoDialog = ref(false)
const extractedInfo = ref('')
const infoLoading = ref(false)
const infoDialogTitle = ref('AI 关键信息提取')
const infoDocTitle = ref('')

// 查看内容
const showContentDialog = ref(false)
const viewDocTitle = ref('')
const viewDocContent = ref('')

const typeTagMap = { docx: 'primary', xlsx: 'success', txt: 'info', md: 'warning' }
const validExts = ['.docx', '.xlsx', '.txt', '.md']
const maxFileSize = 100 * 1024 * 1024

const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await getDocuments({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value,
      fileType: filterType.value
    })
    documents.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 文件选择处理
const handleFileChange = (file, fileList) => {
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExts.includes(ext)) {
    ElMessage.error('不支持的格式: ' + ext)
    fileList.pop()
    return
  }
  if (file.size > maxFileSize) {
    ElMessage.error('文件不能超过100MB')
    fileList.pop()
    return
  }
  uploadFileList.value = fileList
}

const handleFileRemove = (file, fileList) => {
  uploadFileList.value = fileList
}

const onUploadDialogClose = () => {
  uploadFileList.value = []
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const isDuplicateUpload = (rawFile) => {
  return uploadFileList.value.some((item) => {
    const raw = item.raw
    return raw && raw.name === rawFile.name && raw.size === rawFile.size && raw.lastModified === rawFile.lastModified
  })
}

const onListDragEnter = () => {
  dragDepth.value += 1
  dragActive.value = true
}

const onListDragOver = () => {
  if (!dragActive.value) dragActive.value = true
}

const onListDragLeave = () => {
  dragDepth.value = Math.max(0, dragDepth.value - 1)
  if (dragDepth.value === 0) {
    dragActive.value = false
  }
}

const onListDrop = (event) => {
  dragDepth.value = 0
  dragActive.value = false

  const droppedFiles = Array.from(event.dataTransfer?.files || [])
  if (droppedFiles.length === 0) return

  let added = 0
  let invalidType = 0
  let oversize = 0
  let duplicate = 0

  droppedFiles.forEach((raw, index) => {
    const ext = raw.name.substring(raw.name.lastIndexOf('.')).toLowerCase()
    if (!validExts.includes(ext)) {
      invalidType += 1
      return
    }
    if (raw.size > maxFileSize) {
      oversize += 1
      return
    }
    if (isDuplicateUpload(raw)) {
      duplicate += 1
      return
    }

    uploadFileList.value.push({
      uid: `drop_${Date.now()}_${index}`,
      name: raw.name,
      size: raw.size,
      status: 'ready',
      raw
    })
    added += 1
  })

  if (added > 0) {
    showUploadDialog.value = true
    ElMessage.success(`已添加 ${added} 个文件，点击“上传并提取信息”开始处理`)
  }
  if (invalidType > 0) {
    ElMessage.warning(`已忽略 ${invalidType} 个不支持格式文件`)
  }
  if (oversize > 0) {
    ElMessage.warning(`已忽略 ${oversize} 个超过100MB的文件`)
  }
  if (duplicate > 0) {
    ElMessage.info(`已忽略 ${duplicate} 个重复文件`)
  }
}

// 执行上传
const doUpload = async () => {
  if (uploadFileList.value.length === 0) return
  uploading.value = true
  let successCount = 0
  let failCount = 0

  for (const fileItem of uploadFileList.value) {
    try {
      const formData = new FormData()
      formData.append('file', fileItem.raw)
      await uploadDocument(formData)
      successCount++
    } catch (e) {
      failCount++
      console.error('上传失败:', fileItem.name, e)
    }
  }

  uploading.value = false
  if (successCount > 0) {
    ElMessage.success(`成功上传 ${successCount} 个文档，已自动提取文本信息`)
  }
  if (failCount > 0) {
    ElMessage.warning(`${failCount} 个文档上传失败`)
  }
  showUploadDialog.value = false
  uploadFileList.value = []
  loadDocuments()
}

const goChat = (row) => router.push(`/ai-chat?docId=${row.id}`)

const downloadDoc = (row) => window.open(`/api/documents/${row.id}/download`)

// AI关键信息提取
const extractInfo = async (row) => {
  infoDocTitle.value = row.title
  infoDialogTitle.value = 'AI 关键信息提取 - ' + row.title
  showInfoDialog.value = true
  infoLoading.value = true
  extractedInfo.value = ''
  try {
    const res = await aiExtractInfo({ documentId: row.id })
    extractedInfo.value = res.data?.info || '无法提取信息'
  } catch (e) {
    extractedInfo.value = '提取失败: ' + (e.message || '未知错误')
  } finally {
    infoLoading.value = false
  }
}

// 查看已提取的文档内容
const viewContent = (row) => {
  viewDocTitle.value = row.title
  viewDocContent.value = row.contentText || '暂无提取内容'
  showContentDialog.value = true
}

const deleteDoc = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除文档「${row.title}」？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (e) { /* cancelled */ }
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个文档？`, '批量删除', {
      type: 'warning',
      confirmButtonText: '全部删除',
      cancelButtonText: '取消'
    })
    await batchDeleteDocuments(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadDocuments()
  } catch (e) { /* cancelled */ }
}

const formatSize = (size) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

const formatDate = (d) => d ? d.replace('T', ' ').substring(0, 16) : '-'

onMounted(loadDocuments)
</script>

<style scoped>
.documents-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stats-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.table-wrapper {
  flex: 1;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

.drop-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(79, 70, 229, 0.12);
  border: 2px dashed var(--primary);
  color: var(--primary);
  font-size: 16px;
  font-weight: 600;
  pointer-events: none;
}

.table-wrapper :deep(.el-table) {
  flex: 1;
}

.doc-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-icon {
  font-size: 24px;
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}

.doc-name-wrap {
  min-width: 0;
}

.doc-title {
  display: block;
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-preview {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}

.table-row {
  cursor: default;
}

.pagination-wrap {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-light);
}

/* Upload dialog */
.upload-area {
  padding: 10px 0;
}

.upload-info {
  padding: 12px 0 0;
  font-size: 14px;
  color: var(--text-secondary);
}

/* Info dialog */
.info-result {
  min-height: 200px;
}

.info-section {
  margin-bottom: 16px;
}

.info-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.info-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  background: var(--bg-base);
  padding: 20px;
  border-radius: var(--radius-md);
  max-height: 500px;
  overflow-y: auto;
}

/* Content dialog */
.content-result {
  min-height: 200px;
}

.content-section {
  margin-bottom: 16px;
}

.content-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.content-body {
  max-height: 500px;
  overflow-y: auto;
}

.content-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-primary);
  background: var(--bg-base);
  padding: 20px;
  border-radius: var(--radius-md);
}
</style>
