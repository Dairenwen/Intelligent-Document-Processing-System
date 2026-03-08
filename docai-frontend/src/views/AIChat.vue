<template>
  <div class="chat-page">
    <!-- 侧边栏 - 文档上下文 -->
    <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <el-icon><DArrowLeft v-if="!sidebarCollapsed" /><DArrowRight v-else /></el-icon>
      </div>
      <div class="sidebar-body" v-if="!sidebarCollapsed">
        <div class="sidebar-section">
          <h4>
            <el-icon :size="14"><Link /></el-icon> 关联文档
          </h4>
          <div v-if="currentDoc" class="linked-doc">
            <div class="linked-doc-icon">
              <el-icon :size="20"><Document /></el-icon>
            </div>
            <div class="linked-doc-info">
              <span class="linked-doc-name">{{ currentDoc.title }}</span>
              <el-tag size="small" type="success" effect="plain">已加载</el-tag>
            </div>
            <div class="linked-doc-actions">
              <el-tooltip content="下载文档">
                <el-button size="small" circle @click="downloadDoc">
                  <el-icon><Download /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="取消关联">
                <el-button size="small" circle @click="unlinkDoc">
                  <el-icon><Close /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
          <div v-else class="no-doc-tip">
            <el-button size="small" type="primary" plain class="select-doc-btn" @click="showDocPicker = true">
              <el-icon><FolderOpened /></el-icon> 选择文档关联
            </el-button>
            <p class="tip-text">关联文档后可基于文档内容进行问答和编辑</p>
          </div>
        </div>

        <!-- 文档操作 -->
        <div class="sidebar-section" v-if="currentDoc">
          <h4>
            <el-icon :size="14"><Setting /></el-icon> 文档操作
          </h4>
          <div class="doc-commands">
            <div class="cmd-btn" @click="sendCommand('总结这篇文档的核心内容，提炼关键信息')">
              <el-icon :size="14"><Memo /></el-icon><span>内容摘要</span>
            </div>
            <div class="cmd-btn" @click="sendCommand('提取文档中所有关键数据，包括数字、日期、人名、机构等实体信息，以结构化形式输出')">
              <el-icon :size="14"><Search /></el-icon><span>信息提取</span>
            </div>
            <div class="cmd-btn" @click="sendCommand('请对文档内容进行润色和优化，使语言更规范流畅，格式更清晰')">
              <el-icon :size="14"><EditPen /></el-icon><span>润色优化</span>
            </div>
            <div class="cmd-btn" @click="sendCommand('请调整文档的格式结构，优化标题层级、段落划分、列表格式等，使其更加规范')">
              <el-icon :size="14"><SetUp /></el-icon><span>格式调整</span>
            </div>
            <div class="cmd-btn" @click="sendCommand('请分析文档中的数据，给出趋势分析和关键发现')">
              <el-icon :size="14"><DataAnalysis /></el-icon><span>数据分析</span>
            </div>
            <div class="cmd-btn" @click="exportAIResult">
              <el-icon :size="14"><Download /></el-icon><span>导出文档</span>
            </div>
          </div>
        </div>

        <div class="sidebar-section">
          <h4>
            <el-icon :size="14"><Promotion /></el-icon> 快捷提问
          </h4>
          <div class="quick-questions">
            <div
              class="quick-q"
              v-for="q in quickQuestions"
              :key="q"
              @click="sendCommand(q)"
            >{{ q }}</div>
          </div>
        </div>

        <!-- AI 动态效果区域 -->
        <div class="sidebar-ai-visual">
          <div class="ai-visual-canvas">
            <svg viewBox="0 0 200 120" class="neural-svg">
              <!-- Connecting lines -->
              <line x1="30" y1="30" x2="100" y2="25" class="nn-line nn-l1" />
              <line x1="30" y1="60" x2="100" y2="25" class="nn-line nn-l2" />
              <line x1="30" y1="60" x2="100" y2="60" class="nn-line nn-l3" />
              <line x1="30" y1="90" x2="100" y2="60" class="nn-line nn-l4" />
              <line x1="30" y1="90" x2="100" y2="95" class="nn-line nn-l5" />
              <line x1="30" y1="30" x2="100" y2="60" class="nn-line nn-l6" />
              <line x1="100" y1="25" x2="170" y2="45" class="nn-line nn-l7" />
              <line x1="100" y1="60" x2="170" y2="45" class="nn-line nn-l8" />
              <line x1="100" y1="60" x2="170" y2="80" class="nn-line nn-l9" />
              <line x1="100" y1="95" x2="170" y2="80" class="nn-line nn-l10" />
              <!-- Input nodes -->
              <circle cx="30" cy="30" r="5" class="nn-node nn-input" />
              <circle cx="30" cy="60" r="5" class="nn-node nn-input" />
              <circle cx="30" cy="90" r="5" class="nn-node nn-input" />
              <!-- Hidden nodes -->
              <circle cx="100" cy="25" r="6" class="nn-node nn-hidden" />
              <circle cx="100" cy="60" r="6" class="nn-node nn-hidden" />
              <circle cx="100" cy="95" r="6" class="nn-node nn-hidden" />
              <!-- Output nodes -->
              <circle cx="170" cy="45" r="5" class="nn-node nn-output" />
              <circle cx="170" cy="80" r="5" class="nn-node nn-output" />
            </svg>
            <div class="ai-pulse-ring"></div>
          </div>
          <div class="ai-visual-label">
            <span class="avl-en">Neural Processing</span>
            <span class="avl-dot" :class="{ active: loading || isStreaming }"></span>
            <span class="avl-status">{{ loading ? 'Thinking...' : isStreaming ? 'Generating' : 'Ready' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主聊天区 -->
    <div class="chat-main">
      <!-- 顶部栏 -->
      <div class="chat-header">
        <div class="chat-title-row">
          <div class="chat-logo">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="11" stroke="var(--primary)" stroke-width="2"/>
              <circle cx="12" cy="12" r="4" fill="var(--primary)"/>
              <line x1="12" y1="1" x2="12" y2="5" stroke="var(--primary)" stroke-width="2"/>
              <line x1="12" y1="19" x2="12" y2="23" stroke="var(--primary)" stroke-width="2"/>
              <line x1="1" y1="12" x2="5" y2="12" stroke="var(--primary)" stroke-width="2"/>
              <line x1="19" y1="12" x2="23" y2="12" stroke="var(--primary)" stroke-width="2"/>
            </svg>
          </div>
          <div>
            <span class="chat-title-text">AI 智能对话</span>
            <span class="chat-model-tag">GLM-4.7-Flash</span>
          </div>
        </div>
        <div class="chat-actions">
          <el-tooltip content="清空对话">
            <el-button text circle @click="clearChat">
              <el-icon :size="18"><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="messages-area" ref="chatArea">
        <!-- 欢迎卡片 -->
        <div class="welcome-card" v-if="messages.length <= 1">
          <div class="welcome-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="11" stroke="var(--primary)" stroke-width="1.5"/>
              <circle cx="12" cy="12" r="4" fill="var(--primary)"/>
              <line x1="12" y1="1" x2="12" y2="5" stroke="var(--primary)" stroke-width="1.5"/>
              <line x1="12" y1="19" x2="12" y2="23" stroke="var(--primary)" stroke-width="1.5"/>
              <line x1="1" y1="12" x2="5" y2="12" stroke="var(--primary)" stroke-width="1.5"/>
              <line x1="19" y1="12" x2="23" y2="12" stroke="var(--primary)" stroke-width="1.5"/>
            </svg>
          </div>
          <h3>欢迎使用 DocAI 智能助手</h3>
          <p>支持文档分析、内容总结、智能问答、文档编辑等能力</p>
          <div class="welcome-features">
            <div class="wf-item">
              <el-icon :size="16"><Memo /></el-icon>
              <span>文档内容理解与提问</span>
            </div>
            <div class="wf-item">
              <el-icon :size="16"><DataAnalysis /></el-icon>
              <span>数据分析与信息提取</span>
            </div>
            <div class="wf-item">
              <el-icon :size="16"><EditPen /></el-icon>
              <span>文档编辑、润色与格式调整</span>
            </div>
          </div>
        </div>

        <div
          class="message-wrapper"
          v-for="(msg, index) in messages"
          :key="index"
          :class="[msg.role]"
        >
          <!-- AI 消息 -->
          <template v-if="msg.role === 'ai'">
            <div class="ai-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" fill="var(--primary)" opacity="0.15"/>
                <circle cx="12" cy="12" r="4" fill="var(--primary)"/>
              </svg>
            </div>
            <div class="message-bubble ai-bubble" :class="{ 'streaming-bubble': msg._streaming }">
              <div class="bubble-text" v-html="renderMessageContent(msg)"></div>
              <div class="bubble-actions" v-if="!msg._isWelcome && !msg._streaming">
                <el-tooltip content="复制">
                  <button class="action-btn" @click="copyText(msg.content)">
                    <el-icon :size="14"><CopyDocument /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="导出文档">
                  <button class="action-btn" @click="exportContentToWord(msg.content)">
                    <el-icon :size="14"><Document /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="保存到文档" v-if="currentDoc">
                  <button class="action-btn save-btn" @click="saveToDocument(msg.content)">
                    <el-icon :size="14"><Upload /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>
          </template>

          <!-- 用户消息：消息在左，头像在右 -->
          <template v-else-if="msg.role === 'user'">
            <div class="message-bubble user-bubble">
              <div class="bubble-text">{{ msg.content }}</div>
            </div>
            <div class="user-avatar">{{ avatarChar }}</div>
          </template>
        </div>

        <!-- Loading -->
        <div class="message-wrapper ai" v-if="loading">
          <div class="ai-avatar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" fill="var(--primary)" opacity="0.15"/>
              <circle cx="12" cy="12" r="4" fill="var(--primary)"/>
            </svg>
          </div>
          <div class="message-bubble ai-bubble loading-bubble">
            <div class="typing-dots">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-box" :class="{ focused: inputFocused }">
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
            @keydown.enter.exact.prevent="sendMessage"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            resize="none"
          />
          <div class="input-actions">
            <span class="char-count" v-if="inputText.length > 0">{{ inputText.length }}</span>
            <el-button
              type="primary"
              circle
              :disabled="!inputText.trim() || loading || isStreaming"
              @click="sendMessage"
              class="send-btn"
            >
              <el-icon><Position /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="input-footer">
          <span>基于 GLM-4.7-Flash 大语言模型 -- 内容仅供参考</span>
        </div>
      </div>
    </div>

    <!-- 文档选择弹窗 -->
    <el-dialog v-model="showDocPicker" title="选择关联文档" width="600px" destroy-on-close>
      <div class="doc-picker-search">
        <el-input v-model="docSearchKey" placeholder="搜索文档..." clearable />
      </div>
      <div class="doc-picker-list" v-loading="loadingDocList">
        <el-empty v-if="filteredDocList.length === 0" description="暂无文档" :image-size="60" />
        <div
          v-for="doc in filteredDocList"
          :key="doc.id"
          class="doc-picker-item"
          @click="selectDoc(doc)"
        >
          <span class="dpi-icon"><el-icon :size="20"><Document /></el-icon></span>
          <div class="dpi-info">
            <span class="dpi-name">{{ doc.title }}</span>
            <span class="dpi-meta">{{ doc.fileType?.toUpperCase() }} - {{ formatDate(doc.createdAt) }}</span>
          </div>
          <el-tag v-if="doc.contentText" size="small" type="success" effect="plain">已提取</el-tag>
          <el-tag v-else size="small" type="info" effect="plain">未提取</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { aiChat, getDocument, getDocuments, downloadBlob, updateDocumentContent } from '../api'
import request from '../api/request'
import { ElMessage } from 'element-plus'
import {
  Delete, Position, CopyDocument, Document, Download, Close, FolderOpened,
  DArrowLeft, DArrowRight, Link, Setting, Memo, Search, EditPen, SetUp,
  DataAnalysis, Promotion, Upload
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import { startTask, getTask, clearTask, subscribe as subscribeTask } from '../utils/chatTaskManager'

// Configure marked for safe rendering
marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const chatArea = ref(null)
const inputFocused = ref(false)
const sidebarCollapsed = ref(false)

// Document state
const currentDoc = ref(null)
const parseDocId = (value) => {
  if (value === undefined || value === null || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) && n > 0 ? n : null
}
const currentDocId = ref(parseDocId(route.query.docId))
const showDocPicker = ref(false)
const docSearchKey = ref('')
const docList = ref([])
const loadingDocList = ref(false)
const lastAIContent = ref('')
const isStreaming = ref(false)
let _streamingTimer = null
let _unsubscribe = null

// 用户信息
const userId = localStorage.getItem('userId') || 'default'
const nickname = localStorage.getItem('nickname') || '用户'
const avatarChar = computed(() => nickname?.charAt(0) || 'U')
const linkedDocStorageKey = computed(() => `docai_linked_doc_${userId}`)

// 历史记录key
const chatStorageKey = computed(() => `docai_chat_${userId}_${currentDocId.value || 'general'}`)

const getPersistedLinkedDocId = () => {
  try {
    return parseDocId(localStorage.getItem(linkedDocStorageKey.value))
  } catch (e) {
    return null
  }
}

const persistLinkedDocId = (docId) => {
  try {
    if (docId) {
      localStorage.setItem(linkedDocStorageKey.value, String(docId))
    } else {
      localStorage.removeItem(linkedDocStorageKey.value)
    }
  } catch (e) {
    console.warn('持久化关联文档失败', e)
  }
}

const formatDate = (d) => d ? d.split('T')[0] : '-'

const filteredDocList = computed(() => {
  if (!docSearchKey.value) return docList.value
  const k = docSearchKey.value.toLowerCase()
  return docList.value.filter(d => d.title?.toLowerCase().includes(k))
})

const quickQuestions = computed(() => {
  if (currentDoc.value) {
    return [
      '总结这篇文档的核心内容',
      '提取文档中的关键数据',
      '帮我优化这篇文档的结构',
      '分析文档中的数据趋势'
    ]
  }
  return [
    '帮我写一个会议通知',
    '如何优化公文写作质量？',
    '请解释什么是非结构化数据',
    '写一个项目进度汇报模板'
  ]
})

// === 历史记录持久化 ===
const saveChatHistory = () => {
  if (isStreaming.value) return
  try {
    const toSave = messages.value.filter(m => !m._isWelcome).map(m => ({
      role: m.role,
      content: m._fullContent || m.content
    }))
    localStorage.setItem(chatStorageKey.value, JSON.stringify(toSave))
  } catch (e) {
    console.warn('保存聊天记录失败', e)
  }
}

const loadChatHistory = () => {
  try {
    const saved = localStorage.getItem(chatStorageKey.value)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch (e) {
    console.warn('加载聊天记录失败', e)
  }
  return null
}

const buildWelcomeMessage = (doc) => {
  if (doc) {
    return `您好，我已加载文档"${doc.title}"。\n\n我可以帮您：\n- 总结文档核心内容\n- 提取关键信息和数据\n- 编辑润色优化\n- 调整文档格式结构\n- 解答文档相关疑问\n\n您可以直接提问，或使用左侧快捷操作。`
  }
  return '您好！我是 DocAI 智能助手。我可以帮您撰写公文、分析文档、提取关键信息、编辑优化文档。\n\n提示：关联文档后可使用更多功能（点击左侧"选择文档关联"）。'
}

const restoreChatForCurrentContext = async () => {
  const history = loadChatHistory()
  if (history && history.length > 0) {
    messages.value = history
    if (currentDoc.value) {
      messages.value.unshift({
        role: 'ai',
        content: `已恢复与文档"${currentDoc.value.title}"的历史对话。`,
        _isWelcome: true
      })
    }
    await scrollToBottom()
    return
  }

  messages.value = [{
    role: 'ai',
    content: buildWelcomeMessage(currentDoc.value),
    _isWelcome: true
  }]
}

// 监听消息变化自动保存
watch(messages, () => {
  saveChatHistory()
}, { deep: true })

// 同一路由下 docId 变化时刷新关联文档
watch(() => route.query.docId, async (newDocId) => {
  const parsed = parseDocId(newDocId)
  currentDocId.value = parsed
  if (parsed) {
    persistLinkedDocId(parsed)
    await loadDocument(parsed)
  } else {
    currentDoc.value = null
    persistLinkedDocId(null)
  }
  await restoreChatForCurrentContext()
})

// Load document by ID
const loadDocument = async (id) => {
  if (!id || !Number.isFinite(id)) return
  try {
    const res = await getDocument(id)
    currentDoc.value = res.data
    currentDocId.value = id
  } catch (e) {
    ElMessage.error('文档加载失败: ' + (e.message || '未知错误'))
    currentDoc.value = null
    currentDocId.value = null
  }
}

// Load document list for picker
const loadDocList = async () => {
  loadingDocList.value = true
  try {
    const res = await getDocuments({ size: 200 })
    docList.value = res.data?.records || []
  } catch (e) {
    console.error('加载文档列表失败', e)
  } finally {
    loadingDocList.value = false
  }
}

const selectDoc = async (doc) => {
  showDocPicker.value = false
  currentDoc.value = doc
  currentDocId.value = doc.id
  persistLinkedDocId(doc.id)
  await restoreChatForCurrentContext()
  messages.value.push({
    role: 'ai',
    content: `已关联文档"${doc.title}"，后续切换页面后会自动保留该关联关系与对话记录。`,
    _isWelcome: true
  })
  await scrollToBottom()
}

const unlinkDoc = () => {
  currentDoc.value = null
  currentDocId.value = null
  persistLinkedDocId(null)
  restoreChatForCurrentContext()
  messages.value.push({
    role: 'ai',
    content: '已取消文档关联。您可以继续自由对话，或重新选择文档。',
    _isWelcome: true
  })
}

const downloadDoc = () => {
  if (!currentDocId.value) return
  window.open(`/api/documents/${currentDocId.value}/download`, '_blank')
}

onMounted(async () => {
  loadDocList()

  const routeDocId = parseDocId(route.query.docId)
  const persistedDocId = getPersistedLinkedDocId()
  const initialDocId = routeDocId || persistedDocId

  if (initialDocId) {
    currentDocId.value = initialDocId
    persistLinkedDocId(initialDocId)
    await loadDocument(initialDocId)
  }

  await restoreChatForCurrentContext()

  // 恢复后台AI任务
  const pendingTask = getTask()
  if (pendingTask) {
    if (pendingTask.status === 'pending') {
      loading.value = true
    }
    attachToTask(pendingTask)
  }
})

onBeforeUnmount(() => {
  // 停止打字动画，提交完整内容
  if (_streamingTimer) {
    clearTimeout(_streamingTimer)
    _streamingTimer = null
  }
  if (isStreaming.value) {
    messages.value.forEach(m => {
      if (m._streaming && m._fullContent) {
        m.content = m._fullContent
        m._streaming = false
        delete m._fullContent
      }
    })
    isStreaming.value = false
    lastAIContent.value = messages.value[messages.value.length - 1]?.content || ''
  }
  if (_unsubscribe) {
    _unsubscribe()
    _unsubscribe = null
  }
  saveChatHistory()
})

const sendCommand = (text) => {
  inputText.value = text
  sendMessage()
}

const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value || isStreaming.value) return

  const userMsg = inputText.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  inputText.value = ''
  loading.value = true
  await scrollToBottom()

  const linkedDocId = Number.isFinite(currentDocId.value) ? currentDocId.value : null
  const task = startTask(() => requestAIChatWithFallback(userMsg, linkedDocId))
  attachToTask(task)
}

// 订阅后台AI任务结果
const attachToTask = (task) => {
  if (_unsubscribe) { _unsubscribe(); _unsubscribe = null }
  _unsubscribe = subscribeTask(task, (type, data) => {
    if (type === 'complete') {
      loading.value = false
      beginTypewriter(data)
    } else {
      loading.value = false
      messages.value.push({
        role: 'ai',
        content: '抱歉，服务暂时繁忙，请稍后再试。如果问题持续，请检查网络连接或后端服务是否启动。'
      })
      clearTask()
      scrollToBottom()
    }
  })
}

// 逐字打字效果
const beginTypewriter = (fullText) => {
  const msg = { role: 'ai', content: '', _streaming: true, _fullContent: fullText }
  messages.value.push(msg)
  isStreaming.value = true
  loading.value = false

  const target = messages.value[messages.value.length - 1]
  let idx = 0
  const len = fullText.length
  const step = Math.max(1, Math.ceil(len / 200))

  const tick = () => {
    if (idx >= len) {
      target.content = fullText
      target._streaming = false
      delete target._fullContent
      isStreaming.value = false
      lastAIContent.value = fullText
      _streamingTimer = null
      clearTask()
      saveChatHistory()
      scrollToBottom()
      return
    }
    idx = Math.min(idx + step, len)
    target.content = fullText.substring(0, idx)
    scrollToBottom()
    _streamingTimer = setTimeout(tick, 16)
  }

  tick()
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const isTransientChatError = (err) => {
  const status = err?.response?.status
  if (status === 429 || status === 503) return true
  const msg = err?.message || ''
  return msg.includes('Network Error') || err?.code === 'ECONNABORTED'
}

const isDocumentContextError = (err) => {
  const msg = err?.message || ''
  return msg.includes('文档不存在') || msg.includes('文档内容为空') || msg.includes('For input string')
}

const sendChatOnce = async (message, documentId) => {
  return aiChat({ message, documentId })
}

const requestAIChatWithFallback = async (message, documentId) => {
  try {
    return await sendChatOnce(message, documentId)
  } catch (err) {
    // 关联文档不可用时，自动降级为普通对话，避免对话功能整体中断
    if (documentId && isDocumentContextError(err)) {
      try {
        ElMessage.warning('关联文档暂不可用，已切换为普通对话')
        return await sendChatOnce(message, null)
      } catch (fallbackErr) {
        if (!isTransientChatError(fallbackErr)) throw fallbackErr
      }
    } else if (!isTransientChatError(err)) {
      throw err
    }
  }

  // 临时连接问题进行一次重试
  await sleep(700)
  return await sendChatOnce(message, documentId)
}

const exportAIResult = async () => {
  if (!lastAIContent.value) {
    ElMessage.warning('暂无AI生成内容可导出')
    return
  }
  const docExt = currentDoc.value?.fileType || 'docx'
  const baseName = currentDoc.value ? currentDoc.value.title + '_AI结果' : 'AI生成内容'
  try {
    const response = await request.post('/ai/generate-word', {
      title: baseName,
      content: lastAIContent.value
    }, { responseType: 'blob' })
    downloadBlob(new Blob([response.data]), `${baseName}.${docExt}`)
    ElMessage.success('已导出文档')
  } catch (e) {
    ElMessage.error('导出失败: ' + (e.message || '未知错误'))
  }
}

const exportContentToWord = async (content) => {
  const docExt = currentDoc.value?.fileType || 'docx'
  const baseName = currentDoc.value ? currentDoc.value.title + '_AI导出' : 'AI_内容导出'
  try {
    const response = await request.post('/ai/generate-word', {
      title: baseName,
      content: content
    }, { responseType: 'blob' })
    downloadBlob(new Blob([response.data]), `${baseName}.${docExt}`)
    ElMessage.success('已导出文档')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

const saveToDocument = async (content) => {
  if (!currentDocId.value || !currentDoc.value) {
    ElMessage.warning('请先关联文档')
    return
  }
  // 如果AI返回内容以"===编辑结果==="开头, 提取实际编辑内容
  let contentToSave = content
  if (contentToSave.startsWith('===编辑结果===')) {
    contentToSave = contentToSave.replace(/^===编辑结果===\n?/, '')
  }
  try {
    await updateDocumentContent(currentDocId.value, contentToSave)
    ElMessage.success(`已保存到文档"${currentDoc.value.title}"`)
    // 更新本地文档状态
    currentDoc.value.contentText = contentToSave
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message || '未知错误'))
  }
}

const clearChat = () => {
  // 中止进行中的AI任务和打字动画
  if (_streamingTimer) { clearTimeout(_streamingTimer); _streamingTimer = null }
  isStreaming.value = false
  loading.value = false
  clearTask()
  if (_unsubscribe) { _unsubscribe(); _unsubscribe = null }

  messages.value = []
  lastAIContent.value = ''
  try { localStorage.removeItem(chatStorageKey.value) } catch (e) {}
  messages.value.push({
    role: 'ai',
    content: currentDoc.value
      ? `对话已重置。请继续提问关于"${currentDoc.value.title}"的内容。`
      : '对话已重置。请问有什么可以帮您？',
    _isWelcome: true
  })
}

const copyText = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制')
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制')
  })
}

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text)
}

const renderMessageContent = (msg) => {
  const html = renderMarkdown(msg.content || '')
  if (msg._streaming) {
    const cursor = '<span class="stream-cursor">▍</span>'
    const lastClose = html.lastIndexOf('</')
    return lastClose > 0 ? html.substring(0, lastClose) + cursor + html.substring(lastClose) : html + cursor
  }
  return html
}

const scrollToBottom = async () => {
  await nextTick()
  await nextTick()
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-page { display: flex; height: 100%; gap: 0; background: var(--bg-base); border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-md); }
.chat-sidebar { width: 280px; background: var(--bg-card); border-right: 1px solid var(--border-light); display: flex; flex-direction: column; position: relative; transition: width 0.3s ease; }
.chat-sidebar.collapsed { width: 0; border-right: none; }
.sidebar-toggle { position: absolute; right: -14px; top: 50%; transform: translateY(-50%); width: 28px; height: 28px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; z-index: 10; transition: all 0.2s; }
.sidebar-toggle:hover { background: var(--primary); color: white; border-color: var(--primary); }
.sidebar-body { padding: 20px; overflow-y: auto; flex: 1; }
.sidebar-section { margin-bottom: 24px; }
.sidebar-section h4 { font-size: 13px; font-weight: 600; color: var(--text-secondary); margin: 0 0 12px 0; display: flex; align-items: center; gap: 6px; }
.linked-doc { display: flex; gap: 10px; align-items: center; padding: 12px; background: rgba(79, 70, 229, 0.05); border: 1px solid rgba(79, 70, 229, 0.15); border-radius: var(--radius-md); flex-wrap: wrap; }
.linked-doc-icon { font-size: 24px; color: var(--primary); }
.linked-doc-info { display: flex; flex-direction: column; gap: 4px; min-width: 0; flex: 1; }
.linked-doc-name { font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.linked-doc-actions { display: flex; gap: 4px; }
.no-doc-tip { text-align: center; padding: 16px 0; }
.select-doc-btn { background: rgba(79, 70, 229, 0.1) !important; border-color: var(--primary) !important; color: var(--primary) !important; font-weight: 500; }
.select-doc-btn:hover { background: rgba(79, 70, 229, 0.2) !important; }
.doc-commands { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.cmd-btn { display: flex; align-items: center; gap: 6px; padding: 10px 10px; font-size: 12px; color: var(--text-secondary); background: var(--bg-base); border: 1px solid var(--border-light); border-radius: var(--radius-md); cursor: pointer; transition: all 0.2s; }
.cmd-btn:hover { background: rgba(79, 70, 229, 0.06); border-color: var(--primary); color: var(--primary); }
.quick-questions { display: flex; flex-direction: column; gap: 8px; }
.quick-q { padding: 10px 12px; font-size: 12px; color: var(--text-secondary); background: var(--bg-base); border: 1px solid var(--border-light); border-radius: var(--radius-md); cursor: pointer; transition: all 0.2s; line-height: 1.4; }
.quick-q:hover { background: rgba(79, 70, 229, 0.06); border-color: var(--primary); color: var(--primary); }
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.chat-header { padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-light); background: var(--bg-card); }
.chat-title-row { display: flex; align-items: center; gap: 12px; }
.chat-logo { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: rgba(79, 70, 229, 0.08); border-radius: var(--radius-md); }
.chat-title-text { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.chat-model-tag { display: inline-block; font-size: 11px; padding: 2px 8px; background: linear-gradient(135deg, var(--primary), var(--primary-hover)); color: white; border-radius: 10px; margin-left: 8px; font-weight: 500; }
.messages-area { flex: 1; overflow-y: auto; padding: 24px; scroll-behavior: smooth; }
.welcome-card { text-align: center; padding: 40px 20px; margin-bottom: 20px; }
.welcome-icon { margin-bottom: 12px; display: flex; justify-content: center; }
.welcome-card h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 8px 0; }
.welcome-card > p { color: var(--text-muted); font-size: 14px; margin: 0 0 24px 0; }
.welcome-features { display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; }
.wf-item { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); font-size: 13px; color: var(--text-secondary); }
.message-wrapper { display: flex; gap: 12px; margin-bottom: 20px; align-items: flex-start; }
.message-wrapper.user { flex-direction: row; justify-content: flex-end; }
.ai-avatar { width: 36px; height: 36px; border-radius: 50%; background: rgba(79, 70, 229, 0.08); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--primary-hover)); color: white; font-size: 14px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; order: 2; }
.message-wrapper.user .message-bubble { order: 1; }
.message-bubble { max-width: 72%; padding: 14px 18px; border-radius: 16px; font-size: 14px; line-height: 1.7; position: relative; word-break: break-word; min-width: 40px; }
.ai-bubble { background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border-light); border-radius: 4px 16px 16px 16px; }
.user-bubble { background: linear-gradient(135deg, var(--primary), var(--primary-hover)); color: white; border-radius: 16px 4px 16px 16px; }
.bubble-text :deep(code) { background: rgba(0, 0, 0, 0.06); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.bubble-text :deep(pre) { background: #1e1e2e; color: #cdd6f4; padding: 14px 16px; border-radius: 8px; overflow-x: auto; margin: 8px 0; font-size: 13px; line-height: 1.5; }
.bubble-text :deep(pre code) { background: none; padding: 0; color: inherit; font-size: 13px; }
.bubble-text :deep(strong) { font-weight: 700; }
.bubble-text :deep(li) { margin-left: 16px; list-style: disc; }
.bubble-text :deep(ol li) { list-style: decimal; }
.bubble-text :deep(h1), .bubble-text :deep(h2), .bubble-text :deep(h3), .bubble-text :deep(h4) { margin: 12px 0 6px 0; font-weight: 700; }
.bubble-text :deep(h1) { font-size: 20px; }
.bubble-text :deep(h2) { font-size: 17px; }
.bubble-text :deep(h3) { font-size: 15px; }
.bubble-text :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 13px; }
.bubble-text :deep(th), .bubble-text :deep(td) { border: 1px solid var(--border-light); padding: 6px 10px; text-align: left; }
.bubble-text :deep(th) { background: var(--bg-base); font-weight: 600; }
.bubble-text :deep(blockquote) { border-left: 3px solid var(--primary); padding: 4px 12px; margin: 8px 0; color: var(--text-secondary); background: rgba(79, 70, 229, 0.03); border-radius: 0 4px 4px 0; }
.bubble-text :deep(p) { margin: 4px 0; }
.bubble-text :deep(a) { color: var(--primary); text-decoration: underline; }
.bubble-actions { display: flex; gap: 4px; margin-top: 8px; opacity: 0; transition: opacity 0.2s; }
.message-bubble:hover .bubble-actions { opacity: 1; }
.action-btn { background: none; border: 1px solid var(--border-light); border-radius: var(--radius-sm); padding: 4px 8px; cursor: pointer; color: var(--text-muted); transition: all 0.2s; }
.action-btn:hover { background: var(--bg-base); color: var(--primary); border-color: var(--primary); }
.action-btn.save-btn { color: var(--primary); border-color: rgba(79, 70, 229, 0.3); }
.action-btn.save-btn:hover { background: rgba(79, 70, 229, 0.1); color: var(--primary); border-color: var(--primary); }
.loading-bubble { min-width: 60px; }
.streaming-bubble { border-color: rgba(79, 70, 229, 0.25); }
.bubble-text :deep(.stream-cursor) { color: var(--primary); font-weight: normal; animation: cursor-blink 0.8s step-end infinite; }
@keyframes cursor-blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
.avl-dot.active { background: var(--primary); animation: dot-blink 0.5s ease-in-out infinite; }
.typing-dots { display: flex; gap: 4px; align-items: center; height: 20px; }
.typing-dots span { width: 7px; height: 7px; background: var(--primary); border-radius: 50%; opacity: 0.4; animation: typingDot 1.4s infinite ease-in-out; }
.typing-dots span:nth-child(1) { animation-delay: 0s; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingDot { 0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); } 30% { opacity: 1; transform: scale(1.1); } }
.input-area { padding: 16px 24px; background: var(--bg-card); border-top: 1px solid var(--border-light); }
.input-box { display: flex; align-items: flex-end; background: var(--bg-base); border: 2px solid var(--border-light); border-radius: var(--radius-lg); padding: 8px; transition: all 0.3s; }
.input-box.focused { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1); }
.input-box :deep(.el-textarea__inner) { border: none; box-shadow: none; background: transparent; padding: 8px 12px; resize: none; }
.input-actions { display: flex; align-items: center; gap: 8px; padding: 0 8px 4px 0; }
.char-count { font-size: 11px; color: var(--text-muted); }
.send-btn { width: 36px; height: 36px; }
.input-footer { text-align: center; padding: 8px 0 0 0; font-size: 11px; color: var(--text-muted); }
.doc-picker-search { margin-bottom: 16px; }
.doc-picker-list { max-height: 400px; overflow-y: auto; }
.doc-picker-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: var(--radius-md); cursor: pointer; transition: all 0.2s; border: 1px solid transparent; }
.doc-picker-item:hover { background: rgba(79, 70, 229, 0.05); border-color: rgba(79, 70, 229, 0.15); }
.dpi-icon { color: var(--primary); }
.dpi-info { flex: 1; min-width: 0; }
.dpi-name { display: block; font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dpi-meta { font-size: 12px; color: var(--text-muted); }
.tip-text { font-size: 12px; color: var(--text-muted); margin-top: 8px; }

/* Sidebar AI Visual Effects */
.sidebar-ai-visual { margin-top: auto; padding: 16px 0 8px 0; border-top: 1px solid var(--border-light); }
.ai-visual-canvas { position: relative; display: flex; align-items: center; justify-content: center; height: 130px; overflow: hidden; }
.neural-svg { width: 100%; height: 100%; }
.nn-line { stroke: var(--primary); stroke-width: 0.8; opacity: 0.15; }
.nn-l1 { animation: nn-pulse 3s ease-in-out infinite; }
.nn-l2 { animation: nn-pulse 3s ease-in-out 0.3s infinite; }
.nn-l3 { animation: nn-pulse 3s ease-in-out 0.6s infinite; }
.nn-l4 { animation: nn-pulse 3s ease-in-out 0.9s infinite; }
.nn-l5 { animation: nn-pulse 3s ease-in-out 1.2s infinite; }
.nn-l6 { animation: nn-pulse 3s ease-in-out 0.15s infinite; }
.nn-l7 { animation: nn-pulse 3s ease-in-out 1.5s infinite; }
.nn-l8 { animation: nn-pulse 3s ease-in-out 1.8s infinite; }
.nn-l9 { animation: nn-pulse 3s ease-in-out 2.1s infinite; }
.nn-l10 { animation: nn-pulse 3s ease-in-out 2.4s infinite; }
@keyframes nn-pulse { 0%, 100% { opacity: 0.1; stroke-width: 0.8; } 50% { opacity: 0.5; stroke-width: 1.5; } }
.nn-node { fill: var(--primary); opacity: 0.4; }
.nn-input { animation: nn-node-glow 4s ease-in-out infinite; }
.nn-hidden { animation: nn-node-glow 4s ease-in-out 1s infinite; }
.nn-output { animation: nn-node-glow 4s ease-in-out 2s infinite; }
@keyframes nn-node-glow { 0%, 100% { opacity: 0.3; r: 5; } 50% { opacity: 0.8; r: 7; } }
.ai-pulse-ring { position: absolute; width: 40px; height: 40px; border: 2px solid var(--primary); border-radius: 50%; opacity: 0; animation: pulse-ring 4s ease-out infinite; top: 50%; left: 50%; transform: translate(-50%, -50%); }
@keyframes pulse-ring { 0% { opacity: 0.3; transform: translate(-50%, -50%) scale(0.5); } 100% { opacity: 0; transform: translate(-50%, -50%) scale(2.5); } }
.ai-visual-label { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 8px 0 0 0; }
.avl-en { font-size: 11px; color: var(--text-muted); letter-spacing: 0.05em; font-weight: 500; }
.avl-dot { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; animation: dot-blink 2s ease-in-out infinite; }
@keyframes dot-blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.avl-status { font-size: 11px; color: #22c55e; font-weight: 500; }
</style>
