<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAudit } from '@/api/account'
import { notifyError } from '@/api/http'
import type { AuditItem } from '@/types/api'

const ACTION_LABELS: Record<string, string> = {
  'auth.login': '登录',
  'auth.logout': '登出',
  'consent.update': '授权变更',
  'device.revoke': '设备下线',
}

const items = ref<AuditItem[]>([])
const cursor = ref<string | null>(null)
const loading = ref(true)
const loadingMore = ref(false)

function actionLabel(action: string) {
  return ACTION_LABELS[action] ?? action
}

function detail(payload: Record<string, unknown>) {
  const level = payload.toLevel ? `${payload.fromLevel ?? '—'} → ${payload.toLevel}` : null
  const revoked =
    payload.revokedGroups || payload.revokedApps
      ? `断开 ${payload.revokedGroups ?? 0} 个群、${payload.revokedApps ?? 0} 个应用`
      : null
  const device = payload.deviceId ? `设备 ${payload.deviceId}` : null
  return [level, revoked, device].filter(Boolean).join('，')
}

async function load(reset = false) {
  if (reset) {
    items.value = []
    cursor.value = null
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const page = await listAudit(cursor.value ?? undefined, 10)
    items.value = reset ? page.items : [...items.value, ...page.items]
    cursor.value = page.nextCursor
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

onMounted(() => load(true))
</script>

<template>
  <section>
    <header class="page-head">
      <h1 class="page-head__title">操作记录</h1>
      <p class="page-head__sub">
        登录、登出、授权调整与设备下线都会留痕，按时间倒序排列。
      </p>
    </header>

    <div class="section">
      <el-empty v-if="!loading && items.length === 0" description="还没有记录" />
      <ul v-else class="audit">
        <li v-for="item in items" :key="item.id" class="audit__row">
          <span class="audit__time">{{ new Date(item.createdAt).toLocaleString('zh-CN') }}</span>
          <span>
            <span class="audit__action">{{ actionLabel(item.action) }}</span>
            <span v-if="detail(item.payload)" class="audit__detail">{{ detail(item.payload) }}</span>
          </span>
          <span class="audit__time">{{ item.action }}</span>
        </li>
      </ul>

      <div v-if="cursor" class="actions">
        <el-button :loading="loadingMore" @click="load(false)">加载更多</el-button>
      </div>
    </div>
  </section>
</template>
