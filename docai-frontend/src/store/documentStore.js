import { defineStore } from 'pinia'
import { getDocuments, deleteDocument as apiDeleteDocument, batchDeleteDocuments as apiBatchDelete } from '../api'
import { ElMessage } from 'element-plus'

const CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

export const useDocumentStore = defineStore('documents', {
  state: () => ({
    documents: [],
    total: 0,
    loading: false,
    lastLoaded: 0, // Timestamp of last successful load
    // Store filter/pagination state to detect if a full reload is needed
    lastFilters: {
      page: 1,
      size: 20,
      keyword: '',
      fileType: ''
    }
  }),

  getters: {
    docCount: (state) => state.total,
    extractedCount: (state) => {
      // This is still calculated on the client, but only on the cached data
      return state.documents.filter(d => d.contentText && d.contentText.trim()).length
    },
    isCacheValid: (state) => {
      return state.lastLoaded > 0 && (Date.now() - state.lastLoaded < CACHE_DURATION)
    }
  },

  actions: {
    // The main action to fetch documents with caching logic
    async fetchDocuments(filters = {}) {
      const { force = false, ...filterParams } = filters
      const params = {
        page: filterParams.page || 1,
        size: filterParams.size || 20,
        keyword: filterParams.keyword || '',
        fileType: filterParams.fileType || ''
      }

      // Check if we can use cache
      const isNewQuery = JSON.stringify(params) !== JSON.stringify(this.lastFilters)
      if (!force && this.isCacheValid && !isNewQuery) {
        console.log('Using cached documents.')
        return
      }

      this.loading = true
      try {
        const res = await getDocuments(params)
        this.documents = res.data?.records || []
        this.total = res.data?.total || 0
        this.lastLoaded = Date.now()
        this.lastFilters = params
        console.log('Fetched and updated documents.')
      } catch (e) {
        ElMessage.error('加载文档列表失败')
        // In case of error, invalidate cache to allow retry
        this.lastLoaded = 0
      } finally {
        this.loading = false
      }
    },

    // Action to delete a single document
    async deleteDocument(docId) {
      try {
        await apiDeleteDocument(docId)
        ElMessage.success('删除成功')
        // Invalidate cache and force reload
        await this.fetchDocuments({ ...this.lastFilters, force: true })
      } catch (e) {
        // The UI component will handle the message box, so we just re-throw or handle silently
        throw e
      }
    },

    // Action to delete multiple documents
    async batchDeleteDocuments(docIds) {
      try {
        await apiBatchDelete(docIds)
        ElMessage.success('批量删除成功')
        // Invalidate cache and force reload
        await this.fetchDocuments({ ...this.lastFilters, force: true })
      } catch (e) {
        throw e
      }
    },

    // Call this after a successful upload to refresh the list
    async handleUploadSuccess() {
      ElMessage.success('上传成功，正在刷新列表...')
      await this.fetchDocuments({ ...this.lastFilters, force: true })
    },

    // Invalidate cache manually
    invalidateCache() {
      this.lastLoaded = 0
      console.log('Document cache invalidated.')
    }
  }
})
