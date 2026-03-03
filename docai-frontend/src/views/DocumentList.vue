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
        <el-upload
          :action="uploadAction"
          multiple
          :show-file-list="false"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          accept=".docx,.xlsx,.txt,.md"
        >
          <el-button type="primary">
            <el-icon><UploadFilled /></el-icon> 上传文档
          </el-button>
        </el-upload>
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
    <div class="table-wrapper card" v-loading="loading">
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
                <span v-if="row.fileType === 'docx'">📘</span>
                <span v-else-if="row.fileType === 'xlsx'">📊</span>
                <span v-else-if="row.fileType === 'md'">📝</span>
                <span v-else>📄</span>
              </span>
              <div class="doc-name-wrap">
                <span class="doc-title">{{ row.title }}</span>
                <span v-if="row.contentText" class="doc-preview">
                  {{ row.contentText.substring(0, 60) }}...
                </span>
              </div>
            </div>
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
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="AI分析">
                <el-button size="small" type="primary" plain @click="goChat(row)">
                  <el-icon><ChatLineRound /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="信息提取">
                <el-button size="small" type="success" plain @click="extractInfo(row)" :loading="row._extracting">
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

    <!-- 信息提取弹窗 -->
    <el-dialog v-model="showInfoDialog" title="📋 AI 关键信息提取" width="600px">
      <div class="info-result" v-loading="infoLoading">
        <div class="info-content" v-if="extractedInfo">
          <pre>{{ extractedInfo }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDocuments, deleteDocument, batchDeleteDocuments, aiExtractInfo } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  Search, UploadFilled, Download, Delete, ChatLineRound,
  DataAnalysis, Refresh
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
const showInfoDialog = ref(false)
const extractedInfo = ref('')
const infoLoading = ref(false)

const uploadAction = '/api/documents/upload'
const typeTagMap = { docx: 'primary', xlsx: 'success', txt: 'info', md: 'warning' }

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

const beforeUpload = (file) => {
  const validExts = ['.docx', '.xlsx', '.txt', '.md']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExts.includes(ext)) {
    ElMessage.error('不支持的格式: ' + ext)
    return false
  }
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('文件不能超过100MB')
    return false
  }
  return true
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功')
  loadDocuments()
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const goChat = (row) => router.push(`/ai-chat?docId=${row.id}`)

const downloadDoc = (row) => window.open(`/api/documents/${row.id}/download`)

const extractInfo = async (row) => {
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

/* Info dialog */
.info-result {
  min-height: 200px;
}

.info-content pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-primary);
  background: var(--bg-base);
  padding: 20px;
  border-radius: var(--radius-md);
}
</style>
