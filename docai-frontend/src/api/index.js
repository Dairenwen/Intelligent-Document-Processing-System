import request from './request'

// 文档相关
export const uploadDocument = (formData) => request.post('/documents/upload', formData)
export const getDocuments = (params) => request.get('/documents', { params })
export const getDocument = (id) => request.get(`/documents/${id}`)
export const deleteDocument = (id) => request.delete(`/documents/${id}`)

// AI相关
export const aiChat = (data) => request.post('/ai/chat', data)
export const aiGenerate = (data) => request.post('/ai/generate', data)
export const aiPolish = (data) => request.post('/ai/polish', data)
export const aiAnalyzeData = (data) => request.post('/ai/analyze-data', data)