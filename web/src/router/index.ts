import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { onUnauthorized } from '@/api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppShell.vue'),
      children: [
        { path: '', redirect: '/consent' },
        {
          path: 'consent',
          name: 'consent',
          component: () => import('@/views/ConsentView.vue'),
        },
        {
          path: 'devices',
          name: 'devices',
          component: () => import('@/views/DevicesView.vue'),
        },
        {
          path: 'audit',
          name: 'audit',
          component: () => import('@/views/AuditView.vue'),
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/consent' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.signedIn) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.signedIn) {
    return { path: '/consent' }
  }
  return true
})

// 刷新令牌失败（含被远程下线）时统一回登录页
onUnauthorized(() => {
  const auth = useAuthStore()
  auth.forget()
  void router.replace({ name: 'login', query: { expired: '1' } })
})

export default router
