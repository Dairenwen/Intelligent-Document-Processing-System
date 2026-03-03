import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: '', redirect: '/documents' },
      { path: 'documents', component: () => import('../views/DocumentList.vue') },
      { path: 'ai-chat', component: () => import('../views/AIChat.vue') },
      { path: 'ai-generate', component: () => import('../views/AIGenerate.vue') },
    ]
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})