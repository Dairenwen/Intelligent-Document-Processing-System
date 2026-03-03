import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000, // 2分钟超时，适配自动填表等耗时操作
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 可在此添加 token 等认证信息
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一错误处理
request.interceptors.response.use(
  response => {
    const data = response.data
    // 如果是blob（文件下载），直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    // 业务错误码处理
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.response) {
      const status = error.response.status
      const msg = error.response.data?.message || error.response.statusText
      switch (status) {
        case 400:
          ElMessage.error('请求参数错误: ' + msg)
          break
        case 413:
          ElMessage.error('上传文件过大')
          break
        case 500:
          ElMessage.error('服务器内部错误: ' + msg)
          break
        default:
          ElMessage.error(`请求失败(${status}): ${msg}`)
      }
    } else if (error.message?.includes('Network Error')) {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
