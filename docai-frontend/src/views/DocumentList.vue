<template>
  <div class="documents-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar-wrapper">
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索文档名称"
          clearable
          @clear="loadDocuments"
          @keyup.enter="loadDocuments"
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="loadDocuments" class="search-btn">搜索</el-button>
      </div>
      <div class="actions">
        <el-upload
          action="/api/documents/upload"
          :on-success="handleUploadSuccess"
          :show-file-list="false"
          :before-upload="beforeUpload"
        >
          <el-button type="primary" size="large" class="upload-btn">
            <el-icon class="el-icon--left"><UploadFilled /></el-icon> 上传本地文档
          </el-button>
        </el-upload>
      </div>
    </div>

    <!-- 文档列表区域 -->
    <div class="list-wrapper" v-loading="loading">
      <el-empty
        v-if="!loading && documents.length === 0"
        description="暂无文档，快去上传吧~"
        :image-size="200"
      ></el-empty>

      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="doc in documents" :key="doc.id">
          <div class="doc-card hover-card">
            <div class="doc-icon">
              <!-- 根据后缀显示不同图标 -->
              <img v-if="doc.fileType === 'docx'" src="https://img.icons8.com/color/96/microsoft-word-2019--v2.png" alt="word" />
              <img v-else-if="doc.fileType === 'xlsx'" src="https://img.icons8.com/color/96/microsoft-excel-2019--v2.png" alt="excel" />
              <img v-else src="https://img.icons8.com/color/96/file.png" alt="file" />
            </div>
            <div class="doc-info">
              <h3 class="doc-title" :title="doc.title">{{ doc.title }}</h3>
              <p class="doc-meta">
                <span>{{ formatSize(doc.fileSize) }}</span>
                <span class="divider">|</span>
                <span>{{ formatDate(doc.createdAt) }}</span>
              </p>
            </div>
            <div class="doc-actions">
              <el-tooltip content="智能分析" placement="top">
                <el-button circle type="primary" plain @click="goChat(doc)">
                  <el-icon><ChatLineRound /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="下载文档" placement="top">
                <el-button circle type="info" plain @click="downloadDoc(doc)">
                  <el-icon><Download /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除文档" placement="top">
                <el-button circle type="danger" plain @click="deleteDoc(doc)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 分页区域 (预留) -->
      <!-- <el-pagination ... /> -->
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDocuments, deleteDocument } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Search, UploadFilled, Download, Delete, ChatLineRound } from '@element-plus/icons-vue'

const router = useRouter()
const documents = ref([])
const keyword = ref('')
const loading = ref(false)

const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await getDocuments({ keyword: keyword.value })
    documents.value = res.data.records || []
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const beforeUpload = (file) => {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('上传文件大小不能超过 50MB!')
  }
  return isLt50M
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功')
  loadDocuments()
}

const downloadDoc = (row) => {
  window.open(`/api/documents/${row.id}/download`)
}

const goChat = (row) => {
  router.push(`/ai-chat?docId=${row.id}`)
}

const deleteDoc = async (row) => {
  try {
    await ElMessageBox.confirm('确定永久删除该文档吗？', '系统提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (e) {
    // cancelled
  }
}

// 工具函数
const formatSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

onMounted(loadDocuments)
</script>

<style scoped>
.documents-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.toolbar-wrapper {
  background: var(--white);
  padding: 16px 24px;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 400px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: #f5f6f7;
  box-shadow: none;
}

.search-input :deep(.el-input__inner) {
  height: 40px;
}

.search-btn {
  border-radius: 20px;
  padding: 0 24px;
  height: 40px;
}

.upload-btn {
  border-radius: 8px;
  padding: 12px 24px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3); 
  /* 阿里蓝主色调阴影 */
}

/* 卡片样式 */
.list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}

.doc-card {
  background: var(--white);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  border: 1px solid transparent;
  cursor: pointer;
  height: 220px;
  justify-content: space-between;
}

.doc-card:hover {
  border-color: #e6f4ff;
}

.doc-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  margin-top: 10px;
}

.doc-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.doc-info {
  text-align: center;
  width: 100%;
}

.doc-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.doc-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.divider {
  margin: 0 8px;
  color: #e0e0e0;
}

/* 悬停显示操作栏 */
.doc-actions {
  display: flex;
  gap: 12px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px;
  border-radius: 20px;
}

.doc-card:hover .doc-actions {
  opacity: 1;
  transform: translateY(0);
}
</style>
