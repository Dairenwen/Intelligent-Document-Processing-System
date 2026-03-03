<template>
  <div class="chat-page">
    <!-- 侧边栏 - 文档上下文 -->
    <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <el-icon><DArrowLeft v-if="!sidebarCollapsed" /><DArrowRight v-else /></el-icon>
      </div>
      <div class="sidebar-body" v-if="!sidebarCollapsed">
        <div class="sidebar-section">
          <h4>📎 关联文档</h4>
          <div v-if="docTitle" class="linked-doc">
            <div class="linked-doc-icon">📘</div>
            <div class="linked-doc-info">
              <span class="linked-doc-name">{{ docTitle }}</span>
              <el-tag size="small" type="success" effect="plain">已加载</el-tag>
            </div>
          </div>
          <div v-else class="no-doc-tip">
            <el-empty :image-size="60" description="未关联文档">
              <el-button size="small" type="primary" plain @click="$router.push('/documents')">去关联</el-button>
            </el-empty>
          </div>
        </div>
        <div class="sidebar-section">
          <h4>💡 快捷提问</h4>
          <div class="quick-questions">
            <div
              class="quick-q"
              v-for="q in quickQuestions"
              :key="q"
              @click="inputText = q; sendMessage()"
            >{{ q }}</div>
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
            <span class="chat-model-tag">GLM-4-Flash</span>
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
          <div class="welcome-icon">🤖</div>
          <h3>欢迎使用 DocAI 智能助手</h3>
          <p>支持文档分析、内容总结、智能问答等能力</p>
          <div class="welcome-features">
            <div class="wf-item">
              <span class="wf-icon">📋</span>
              <span>文档内容理解与提问</span>
            </div>
            <div class="wf-item">
              <span class="wf-icon">📊</span>
              <span>数据分析与信息提取</span>
            </div>
            <div class="wf-item">
              <span class="wf-icon">✍️</span>
              <span>公文撰写、润色与优化</span>
            </div>
          </div>
        </div>

        <div
          class="message-wrapper"
          v-for="(msg, index) in messages"
          :key="index"
          :class="msg.role"
        >
          <!-- AI 消息 -->
          <template v-if="msg.role === 'ai'">
            <div class="ai-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" fill="var(--primary)" opacity="0.15"/>
                <circle cx="12" cy="12" r="4" fill="var(--primary)"/>
              </svg>
            </div>
            <div class="message-bubble ai-bubble">
              <div class="bubble-text" v-html="renderMarkdown(msg.content)"></div>
              <div class="bubble-actions" v-if="!msg._isWelcome">
                <el-tooltip content="复制">
                  <button class="action-btn" @click="copyText(msg.content)">
                    <el-icon :size="14"><CopyDocument /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>
          </template>

          <!-- 用户消息 -->
          <template v-if="msg.role === 'user'">
            <div class="message-bubble user-bubble">
              <div class="bubble-text">{{ msg.content }}</div>
            </div>
            <div class="user-avatar">U</div>
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
              :disabled="!inputText.trim() || loading"
              @click="sendMessage"
              class="send-btn"
            >
              <el-icon><Position /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="input-footer">
          <span>基于 GLM-4-Flash 大语言模型 · 内容仅供参考</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { aiChat, getDocument } from '../api'
import { ElMessage } from 'element-plus'
import {
  Delete, Position, CopyDocument,
  DArrowLeft, DArrowRight
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const docTitle = ref('')
const chatArea = ref(null)
const inputFocused = ref(false)
const sidebarCollapsed = ref(false)
const docId = route.query.docId ? Number(route.query.docId) : null

const quickQuestions = computed(() => {
  if (docId) {
    return [
      '总结这篇文档的核心内容',
      '提取文档中的关键数据',
      '这篇文档的写作风格如何？',
      '帮我优化这篇文档的结构'
    ]
  }
  return [
    '帮我写一个会议通知',
    '如何优化公文写作质量？',
    '请解释什么是非结构化数据',
    '写一个项目进度汇报模板'
  ]
})

onMounted(async () => {
  if (docId) {
    try {
      const res = await getDocument(docId)
      docTitle.value = res.data.title
      messages.value.push({
        role: 'ai',
        content: `您好，我已加载文档《${docTitle.value}》，您可以向我提问关于这篇文档的任何问题。\n\n我可以帮您：\n- 📋 总结文档核心内容\n- 🔍 提取关键信息和数据\n- ✏️ 优化文档质量\n- 💬 解答文档相关疑问`,
        _isWelcome: true
      })
    } catch (e) {
      ElMessage.error('文档加载失败')
    }
  } else {
    messages.value.push({
      role: 'ai',
      content: '您好！我是 DocAI 智能助手。我可以帮您撰写公文、分析文档、提取关键信息。请问有什么可以帮您？',
      _isWelcome: true
    })
  }
})

const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value) return

  const userMsg = inputText.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await aiChat({ message: userMsg, documentId: docId })
    messages.value.push({ role: 'ai', content: res.data.reply || res.data })
  } catch (err) {
    messages.value.push({
      role: 'ai',
      content: '⚠️ 抱歉，服务暂时繁忙，请稍后再试。如果问题持续，请检查网络连接。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const clearChat = () => {
  messages.value = []
  messages.value.push({
    role: 'ai',
    content: docId
      ? `对话已重置。请继续提问关于《${docTitle.value}》的内容。`
      : '对话已重置。请问有什么可以帮您？',
    _isWelcome: true
  })
}

const copyText = (text) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

const renderMarkdown = (text) => {
  if (!text) return ''
  // 简易 markdown 渲染
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
    .replace(/^- (.*)$/gm, '<li>$1</li>')
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100%;
  gap: 0;
  background: var(--bg-base);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

/* 侧边栏 */
.chat-sidebar {
  width: 260px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  position: relative;
  transition: width 0.3s ease;
}

.chat-sidebar.collapsed {
  width: 0;
  border-right: none;
}

.sidebar-toggle {
  position: absolute;
  right: -14px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
}

.sidebar-toggle:hover {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.sidebar-body {
  padding: 20px;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 24px;
}

.sidebar-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
}

.linked-doc {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: rgba(79, 70, 229, 0.05);
  border: 1px solid rgba(79, 70, 229, 0.15);
  border-radius: var(--radius-md);
}

.linked-doc-icon { font-size: 24px; }

.linked-doc-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.linked-doc-name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-q {
  padding: 10px 12px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-base);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}

.quick-q:hover {
  background: rgba(79, 70, 229, 0.06);
  border-color: var(--primary);
  color: var(--primary);
}

/* 主区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-card);
}

.chat-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-logo {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(79, 70, 229, 0.08);
  border-radius: var(--radius-md);
}

.chat-title-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.chat-model-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, var(--primary), var(--primary-hover));
  color: white;
  border-radius: 10px;
  margin-left: 8px;
  font-weight: 500;
}

/* 消息区域 */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.welcome-card {
  text-align: center;
  padding: 40px 20px;
  margin-bottom: 20px;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.welcome-card h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.welcome-card > p {
  color: var(--text-muted);
  font-size: 14px;
  margin: 0 0 24px 0;
}

.welcome-features {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.wf-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-secondary);
}

.wf-icon { font-size: 16px; }

/* 消息气泡 */
.message-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message-wrapper.user {
  justify-content: flex-end;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(79, 70, 229, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-hover));
  color: white;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-bubble {
  max-width: 72%;
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  position: relative;
  word-break: break-word;
}

.ai-bubble {
  background: var(--bg-card);
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: 4px 16px 16px 16px;
}

.user-bubble {
  background: linear-gradient(135deg, var(--primary), var(--primary-hover));
  color: white;
  border-radius: 16px 4px 16px 16px;
}

.bubble-text :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.bubble-text :deep(strong) {
  font-weight: 700;
}

.bubble-text :deep(li) {
  margin-left: 16px;
  list-style: disc;
}

.bubble-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-bubble:hover .bubble-actions {
  opacity: 1;
}

.action-btn {
  background: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 4px 8px;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.2s;
}

.action-btn:hover {
  background: var(--bg-base);
  color: var(--primary);
  border-color: var(--primary);
}

/* Typing dots */
.loading-bubble {
  min-width: 60px;
}

.typing-dots {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 20px;
}

.typing-dots span {
  width: 7px;
  height: 7px;
  background: var(--primary);
  border-radius: 50%;
  opacity: 0.4;
  animation: typingDot 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) { animation-delay: 0s; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingDot {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1.1); }
}

/* 输入区域 */
.input-area {
  padding: 16px 24px;
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
}

.input-box {
  display: flex;
  align-items: flex-end;
  background: var(--bg-base);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 8px;
  transition: all 0.3s;
}

.input-box.focused {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.input-box :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 8px 12px;
  resize: none;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px 4px 0;
}

.char-count {
  font-size: 11px;
  color: var(--text-muted);
}

.send-btn {
  width: 36px;
  height: 36px;
}

.input-footer {
  text-align: center;
  padding: 8px 0 0 0;
  font-size: 11px;
  color: var(--text-muted);
}
</style>
