<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="chat-title">
        <el-icon class="header-icon"><Cpu /></el-icon>
        <span>{{ docTitle ? `文档分析：${docTitle}` : 'AI 智能助手' }}</span>
        <el-tag v-if="docTitle" size="small" type="success" effect="plain" class="doc-tag">已关联文档</el-tag>
      </div>
      <div class="chat-actions">
        <el-button circle text @click="clearChat"><el-icon><Delete /></el-icon></el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="messages-area" ref="chatArea">
      <div class="message-wrapper" v-for="(msg, index) in messages" :key="index" :class="msg.role">
        
        <!-- AI头像 -->
        <div class="avatar" v-if="msg.role === 'ai'">
          <img src="https://cdn-icons-png.flaticon.com/512/4712/4712035.png" alt="AI" />
        </div>

        <div class="message-content shadow-sm">
           <div class="bubble-text">{{ msg.content }}</div>
        </div>

        <!-- 用户头像 -->
        <div class="avatar" v-if="msg.role === 'user'">
           <img src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" alt="User" />
        </div>
      </div>

      <!-- Loading 状态 -->
      <div class="message-wrapper ai" v-if="loading">
        <div class="avatar">
          <img src="https://cdn-icons-png.flaticon.com/512/4712/4712035.png" alt="AI" />
        </div>
        <div class="message-content shadow-sm loading-bubble">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <div class="input-wrapper shadow-md">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="请输入您的问题... (按 Enter 发送)"
          @keydown.enter.prevent="sendMessage"
          resize="none"
          class="chat-input"
        />
        <el-button type="primary" circle class="send-btn" @click="sendMessage" :disabled="!inputText.trim() || loading">
           <el-icon><Position /></el-icon>
        </el-button>
      </div>
      <div class="footer-tip">生成内容仅供参考，请仔细甄别</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { aiChat, getDocument } from '../api' // 假设 api 路径正确
import { ElMessage } from 'element-plus'
import { Cpu, Delete, Position } from '@element-plus/icons-vue'

const route = useRoute()
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const docTitle = ref('')
const chatArea = ref(null)
const docId = route.query.docId ? Number(route.query.docId) : null

onMounted(async () => {
  if (docId) {
    try {
      const res = await getDocument(docId)
      docTitle.value = res.data.title
      messages.value.push({ role: 'ai', content: `您好，我已经学习了文档《${docTitle.value}》。您可以问我关于它的任何细节，或者让我为您总结核心内容。` })
    } catch (e) {
      ElMessage.error('文档加载失败')
    }
  } else {
    messages.value.push({ role: 'ai', content: '您好！我是您的智能公文助手。我可以帮您撰写、润色公文，或者回答各种办公问题。' })
  }
})

const sendMessage = async (e) => {
  if (e && e.shiftKey) return // Allow Shift+Enter for new line
  if (!inputText.value.trim() || loading.value) return

  const userMsg = inputText.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await aiChat({ message: userMsg, documentId: docId })
    messages.value.push({ role: 'ai', content: res.data.reply })
  } catch (err) {
    messages.value.push({ role: 'ai', content: '抱歉，服务暂时繁忙，请稍后再试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const clearChat = () => {
  messages.value = []
  if (docId) {
    messages.value.push({ role: 'ai', content: `已清空上下文。请继续提问关于《${docTitle.value}》的内容。` })
  } else {
    messages.value.push({ role: 'ai', content: '对话已重置。' })
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--white);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  z-index: 2;
}

.chat-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  gap: 8px;
}

.header-icon {
  font-size: 20px;
  color: var(--primary-color);
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background-color: #f9f9f9;
  scroll-behavior: smooth;
}

.message-wrapper {
  display: flex;
  margin-bottom: 24px;
  gap: 12px;
  align-items: flex-start;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: #fff;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eee;
}

.avatar img {
  width: 100%;
  height: 100%;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  position: relative;
  word-break: break-word;
}

.message-wrapper.ai .message-content {
  background-color: #ffffff;
  color: var(--text-main);
  border-radius: 4px 12px 12px 12px;
}

.message-wrapper.user .message-content {
  background-color: var(--primary-color);
  color: #fff;
  border-radius: 12px 4px 12px 12px;
}

.bubble-text {
  white-space: pre-wrap;
}

.doc-tag {
  margin-left: 8px;
  font-weight: normal;
}

/* Typing Indicator */
.loading-bubble {
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background-color: #ccc;
  border-radius: 50%;
  margin: 0 2px;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  padding: 16px 24px;
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
}

.input-wrapper {
  position: relative;
  border-radius: 24px;
  border: 1px solid #e0e0e0;
  background-color: #fff;
  transition: all 0.3s;
  padding: 4px;
  display: flex;
  align-items: flex-end;
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2);
}

.chat-input :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 10px 14px;
  resize: none;
  max-height: 120px;
}

.send-btn {
  margin: 0 8px 8px 0;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.footer-tip {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
