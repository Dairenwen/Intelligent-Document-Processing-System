import request from './request'
import axios from 'axios'

// ==================== 文档管理 ====================

export const uploadDocument = (formData, onProgress) =>
  request.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })

export const batchUploadDocuments = (formData, onProgress) =>
  request.post('/documents/upload/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000, // 5分钟超时
    onUploadProgress: onProgress
  })

export const getDocuments = (params) => request.get('/documents', { params })

export const getDocument = (id) => request.get(`/documents/${id}`)

export const deleteDocument = (id) => request.delete(`/documents/${id}`)

export const batchDeleteDocuments = (ids) => request.delete('/documents/batch', { data: ids })

export const getDocumentStats = () => request.get('/documents/stats')

export const getDocumentSummary = (id) => request.get(`/documents/${id}/summary`)

// ==================== AI 功能 ====================

export const aiChat = (data) => request.post('/ai/chat', data)

export const aiGenerate = (data) => request.post('/ai/generate', data)

export const aiPolish = (data) => request.post('/ai/polish', data)

export const aiAnalyzeData = (data) => request.post('/ai/analyze-data', data)

export const aiExtractInfo = (data) => request.post('/ai/extract-info', data)

// ==================== 自动填表 ====================

export const autoFillSingle = (formData, onProgress) =>
  axios.post('/api/autofill/single', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
    timeout: 180000, // 3分钟超时
    onUploadProgress: onProgress
  })

export const autoFillBatch = (formData, onProgress) =>
  axios.post('/api/autofill/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
    timeout: 600000, // 10分钟超时
    onUploadProgress: onProgress
  })

export const autoFillPreview = (formData) =>
  request.post('/autofill/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000
  })

// ==================== 工具函数 ====================

/**
 * 下载blob文件
 */
export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
