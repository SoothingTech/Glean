<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendSmsCode } from '@/api/account'
import { ApiError, notifyError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const phone = ref('')
const code = ref('')
const submitting = ref(false)
const sending = ref(false)
const countdown = ref(0)
const errorText = ref('')

const canSubmit = computed(() => /^1[3-9]\d{9}$/.test(phone.value) && code.value.length >= 4)

const pieces = [
  { offset: '-26px', delay: '0ms', solid: false },
  { offset: '14px', delay: '60ms', solid: false },
  { offset: '-12px', delay: '120ms', solid: false },
  { offset: '22px', delay: '180ms', solid: true },
  { offset: '-18px', delay: '240ms', solid: false },
  { offset: '8px', delay: '300ms', solid: false },
]

let timer: number | undefined

onMounted(() => {
  if (route.query.expired) {
    errorText.value = '登录状态已失效，请重新登录。'
  }
})

async function send() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    errorText.value = '请输入 11 位手机号。'
    return
  }
  sending.value = true
  errorText.value = ''
  try {
    await sendSmsCode(phone.value)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    window.clearInterval(timer)
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) window.clearInterval(timer)
    }, 1000)
  } catch (error) {
    errorText.value = error instanceof ApiError ? error.message : '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  errorText.value = ''
  try {
    await auth.login(phone.value, code.value)
    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : { name: 'consent' })
  } catch (error) {
    errorText.value = error instanceof ApiError ? error.message : '登录失败'
    if (!(error instanceof ApiError)) notifyError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login">
    <aside class="login__aside">
      <div class="aside__top">
        <div class="wordmark">
          <span class="wordmark__cn">拾零</span>
          <span class="wordmark__en">Glean</span>
        </div>

        <div class="fragments" aria-hidden="true">
          <span
            v-for="(piece, index) in pieces"
            :key="index"
            class="fragments__piece"
            :class="{ 'fragments__piece--solid': piece.solid }"
            :style="{ '--offset': piece.offset, '--delay': piece.delay }"
          />
        </div>
      </div>

      <p class="aside__lede">
        把散落在聊天、通知、截图里的待办拾起来，
        按时间与地点排进一张表，到时间提醒你。
      </p>
    </aside>

    <main class="login__main">
      <form class="form" @submit.prevent="submit">
        <h1 class="form__title">登录</h1>

        <label class="field">
          <span class="field__label">手机号</span>
          <el-input
            v-model="phone"
            size="large"
            maxlength="11"
            placeholder="11 位手机号"
            autocomplete="tel"
          />
        </label>

        <label class="field">
          <span class="field__label">验证码</span>
          <div class="field__row">
            <el-input
              v-model="code"
              size="large"
              maxlength="8"
              placeholder="6 位验证码"
              autocomplete="one-time-code"
            />
            <el-button size="large" :loading="sending" :disabled="countdown > 0" @click="send">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </label>

        <el-button
          class="form__submit"
          type="primary"
          size="large"
          native-type="submit"
          :loading="submitting"
          :disabled="!canSubmit"
        >
          进入
        </el-button>

        <p v-if="errorText" class="form__note form__note--warn" role="alert">{{ errorText }}</p>
      </form>
    </main>
  </div>
</template>
