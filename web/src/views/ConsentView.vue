<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchConsent, updateConsent } from '@/api/account'
import { notifyError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ConsentLevel } from '@/types/api'

const auth = useAuthStore()

const levels: Array<{ code: ConsentLevel; name: string; desc: string }> = [
  { code: 'L0', name: '关闭', desc: '不采集任何来源，已有任务保留' },
  { code: 'L1', name: '手动投递', desc: '转发、粘贴、截图、语音' },
  { code: 'L2', name: '机器人会话', desc: 'L1 加 QQ / 微信的群与单聊' },
  { code: 'L3', name: '端侧通知感知', desc: 'L2 加白名单应用的通知' },
  { code: 'L4', name: '附件与语音', desc: 'L3 加截图与语音附件加密存储' },
]

const selected = ref<ConsentLevel>('L1')
const current = ref<ConsentLevel>('L1')
const loading = ref(true)
const saving = ref(false)
const updatedAt = ref<string | null>(null)

const scope = reactive({
  notifyApps: [] as string[],
  qqGroups: [] as string[],
  quietHours: ['23:00', '07:00'] as string[],
})

function applyView(data: { level: ConsentLevel; scope: Record<string, unknown>; updatedAt: string }) {
  current.value = data.level
  selected.value = data.level
  updatedAt.value = data.updatedAt
  const apps = data.scope?.notifyApps
  const groups = data.scope?.qqGroups
  const quiet = data.scope?.quietHours
  scope.notifyApps = Array.isArray(apps) ? (apps as string[]) : []
  scope.qqGroups = Array.isArray(groups) ? (groups as string[]) : []
  scope.quietHours =
    Array.isArray(quiet) && quiet.length === 2 ? (quiet as string[]) : ['23:00', '07:00']
}

async function load() {
  loading.value = true
  try {
    applyView(await fetchConsent())
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const result = await updateConsent(
      selected.value,
      {
        notifyApps: scope.notifyApps,
        qqGroups: scope.qqGroups,
        quietHours: scope.quietHours,
      },
      crypto.randomUUID(),
    )
    await load()
    await auth.refreshLevel(result.level)

    if (result.revoked.groups > 0 || result.revoked.apps > 0) {
      await ElMessageBox.alert(
        `将断开 ${result.revoked.groups} 个群、停止 ${result.revoked.apps} 个应用的通知。`,
        `已调整为 ${result.level}`,
        { confirmButtonText: '知道了' },
      )
    } else {
      ElMessage.success(`已调整为 ${result.level}`)
    }
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <header class="page-head">
      <h1 class="page-head__title">授权级别</h1>
      <p class="page-head__sub">
        级别是累进的：选 L3 就同时具备 L1、L2 的能力。调低级别会立即停止对应来源的采集。
      </p>
    </header>

    <div class="section">
      <div class="section__label">
        当前级别
        <span class="chip chip--pine" style="margin-left: 8px">{{ current }}</span>
        <span v-if="updatedAt" class="section__hint" style="display: inline; margin-left: 10px">
          更新于 {{ new Date(updatedAt).toLocaleString('zh-CN') }}
        </span>
      </div>

      <div class="levels" role="radiogroup" aria-label="授权级别">
        <button
          v-for="item in levels"
          :key="item.code"
          type="button"
          class="level"
          :class="{ 'is-active': selected === item.code }"
          role="radio"
          :aria-checked="selected === item.code"
          @click="selected = item.code"
        >
          <span class="level__code">{{ item.code }}</span>
          <span>
            <span class="level__name">{{ item.name }}</span>
            <span class="level__desc">{{ item.desc }}</span>
          </span>
          <span class="level__mark">{{ selected === item.code ? '已选' : '' }}</span>
        </button>
      </div>
    </div>

    <div class="section">
      <div class="section__label">采集范围</div>
      <p class="section__hint">留空表示不限制该来源。修改后立即生效。</p>

      <div class="stack">
        <span class="field__label">通知应用包名</span>
        <el-select
          v-model="scope.notifyApps"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="输入包名后回车，例如 com.tencent.mm"
          style="width: 100%"
        />
      </div>

      <div class="stack">
        <span class="field__label">群聊白名单</span>
        <el-select
          v-model="scope.qqGroups"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="输入群号后回车"
          style="width: 100%"
        />
      </div>

      <div class="stack">
        <span class="field__label">免打扰时段</span>
        <div style="display: flex; align-items: center; gap: 10px; margin-top: 8px">
          <el-time-select
            v-model="scope.quietHours[0]"
            start="00:00"
            step="00:30"
            end="23:30"
            placeholder="开始"
          />
          <span style="color: var(--muted)">至</span>
          <el-time-select
            v-model="scope.quietHours[1]"
            start="00:00"
            step="00:30"
            end="23:30"
            placeholder="结束"
          />
        </div>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" :loading="saving" :disabled="loading" @click="save">
        保存授权
      </el-button>
      <span class="section__hint">保存时会做幂等校验，重复提交不会重复生效</span>
    </div>
  </section>
</template>
