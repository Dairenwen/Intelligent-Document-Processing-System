import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'DataAnalysis' }
      },
      {
        path: 'documents',
        name: 'Documents',
        component: () => import('../views/DocumentList.vue'),
        meta: { title: '文档管理', icon: 'FolderOpened' }
      },
      {
        path: 'autofill',
        name: 'AutoFill',
        component: () => import('../views/AutoFill.vue'),
        meta: { title: '智能填表', icon: 'Grid' }
      },
      {
        path: 'ai-chat',
        name: 'AIChat',
        component: () => import('../views/AIChat.vue'),
        meta: { title: 'AI 对话', icon: 'ChatDotRound' }
      },
      {
        path: 'ai-generate',
        name: 'AIGenerate',
        component: () => import('../views/AIGenerate.vue'),
        meta: { title: '智能写作', icon: 'EditPen' }
      }
    ]
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
