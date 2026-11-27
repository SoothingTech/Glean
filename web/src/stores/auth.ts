import { defineStore } from 'pinia'
import * as account from '@/api/account'
import { clearTokens, ensureDeviceId, loadTokens, saveTokens, type ApiError } from '@/api/http'
import type { ConsentLevel, Me, Tokens } from '@/types/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    tokens: loadTokens() as Tokens | null,
    me: null as Me | null,
    level: null as ConsentLevel | null,
  }),

  getters: {
    signedIn: (state) => Boolean(state.tokens?.accessToken),
  },

  actions: {
    async login(phone: string, code: string) {
      const tokens = await account.login({
        phone,
        code,
        deviceId: ensureDeviceId(),
        platform: 'Web',
      })
      saveTokens(tokens)
      this.tokens = tokens
      await this.loadMe()
    },

    async loadMe() {
      this.me = await account.fetchMe()
      try {
        this.level = (await account.fetchConsent()).level
      } catch (error) {
        // 授权接口失败不影响登录态，避免首屏被拦
        void (error as ApiError)
      }
    },

    async refreshLevel(level: ConsentLevel) {
      this.level = level
    },

    async signOut() {
      try {
        await account.logout()
      } catch {
        // 幂等：令牌已失效也算登出成功
      }
      clearTokens()
      this.tokens = null
      this.me = null
      this.level = null
    },

    forget() {
      clearTokens()
      this.tokens = null
      this.me = null
      this.level = null
    },
  },
})
