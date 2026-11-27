<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDevices, revokeDevice } from '@/api/account'
import { notifyError } from '@/api/http'
import type { DeviceView } from '@/types/api'

const devices = ref<DeviceView[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    devices.value = await listDevices()
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

async function revoke(row: DeviceView) {
  try {
    await ElMessageBox.confirm(
      `下线后，这台设备需要重新登录才能继续使用。`,
      `下线 ${row.id}`,
      { confirmButtonText: '下线', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await revokeDevice(row.id)
    ElMessage.success('已下线')
    await load()
  } catch (error) {
    notifyError(error)
  }
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(load)
</script>

<template>
  <section>
    <header class="page-head">
      <h1 class="page-head__title">设备</h1>
      <p class="page-head__sub">
        登录过的设备都在这里。下线一台设备，它的令牌会立刻失效，不必等过期。
      </p>
    </header>

    <div class="section">
      <el-table :data="devices" :loading="loading" style="width: 100%" empty-text="还没有设备">
        <el-table-column prop="id" label="设备" min-width="200" />
        <el-table-column label="平台" width="120">
          <template #default="{ row }">{{ row.platform ?? '未知' }}</template>
        </el-table-column>
        <el-table-column label="最近在线" width="200">
          <template #default="{ row }">{{ formatTime(row.lastSeenAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="chip" :class="{ 'chip--pine': !row.revokedAt }">
              {{ row.revokedAt ? '已下线' : '在线' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="Boolean(row.revokedAt)"
              @click="revoke(row)"
            >
              下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
