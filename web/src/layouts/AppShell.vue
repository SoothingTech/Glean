<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { notifyError } from '@/api/http'
import logoGlean from '@/assets/logo-glean.png'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const navItems = [
  { name: 'consent', label: '授权级别' },
  { name: 'devices', label: '设备' },
  { name: 'audit', label: '操作记录' },
]

const levelText = computed(() => (auth.level ? auth.level : '—'))

onMounted(async () => {
  try {
    await auth.loadMe()
  } catch (error) {
    notifyError(error)
  }
})

async function signOut() {
  await auth.signOut()
  void router.replace({ name: 'login' })
}
</script>

<template>
  <div class="shell">
    <aside class="shell__side">
      <img class="shell__brand" :src="logoGlean" alt="拾零 Glean" />
      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav__item"
          :class="{ 'is-active': route.name === item.name }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="shell__side-foot">账号与授权</div>
    </aside>

    <div class="shell__main">
      <header class="shell__topbar">
        <div class="shell__who">
          <span class="chip chip--pine">授权 {{ levelText }}</span>
          <span v-if="auth.me">设备 {{ auth.me.deviceId }}</span>
        </div>
        <el-button link type="primary" @click="signOut">退出登录</el-button>
      </header>

      <main class="shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
