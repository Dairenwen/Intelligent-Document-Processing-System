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

    // Action to delete a single document with optimistic UI
    async deleteDocument(docId) {
      const docIndex = this.documents.findIndex(d => d.id === docId)
      if (docIndex === -1) return

      // Optimistically remove the document from the state
      const removedDoc = this.documents.splice(docIndex, 1)[0]
      this.total -= 1

      try {
        await apiDeleteDocument(docId)
        ElMessage.success('删除成功')
        // On success, we don't need to do anything, UI is already updated
      } catch (e) {
        // On failure, revert the change
        this.documents.splice(docIndex, 0, removedDoc)
        this.total += 1
        ElMessage.error('删除失败，请重试')
        throw e // Re-throw for the component to know about the failure if needed
      }
    },

    // Action to delete multiple documents with optimistic UI
    async batchDeleteDocuments(docIds) {
      const originalDocs = [...this.documents]
      const removedDocs = []
      
      // Optimistically remove documents
      this.documents = this.documents.filter(doc => {
        if (docIds.includes(doc.id)) {
          removedDocs.push(doc)
          return false
        }
        return true
      })
      const removedCount = removedDocs.length
      this.total -= removedCount

      try {
        await apiBatchDelete(docIds)
        ElMessage.success(`成功删除 ${removedCount} 个文档`)
      } catch (e) {
        // On failure, revert the state
        this.documents = originalDocs
        this.total += removedCount
        ElMessage.error('批量删除失败，请重试')
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
