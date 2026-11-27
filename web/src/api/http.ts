import axios, {
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope, ApiErrorBody, Tokens } from '@/types/api'

const TOKEN_KEY = 'glean.tokens'
const DEVICE_KEY = 'glean.deviceId'

/** 业务错误：携带后端错误码，页面据此决定提示文案 */
export class ApiError extends Error {
  code: string

  constructor(code: string, message: string) {
    super(message)
    this.code = code
  }
}

/** 错误码文案。与后端 5.8.1 定义一致 */
const CODE_MESSAGES: Record<string, string> = {
  E5001: '登录已失效，请重新登录',
  E5002: '无权访问该资源',
  E5003: '数据已被其他设备修改，请刷新',
  E5004: '重复提交，请刷新后重试',
  E5005: '授权级别不足，请先调整授权',
  E5006: '验证码错误或已过期',
  E9001: '输入不合法，请检查后重试',
}

export function loadTokens(): Tokens | null {
  const raw = localStorage.getItem(TOKEN_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Tokens
  } catch {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
}

export function saveTokens(tokens: Tokens): void {
  localStorage.setItem(TOKEN_KEY, JSON.stringify(tokens))
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 浏览器端的稳定设备标识，对应 JWT 的 dev claim */
export function ensureDeviceId(): string {
  let id = localStorage.getItem(DEVICE_KEY)
  if (!id) {
    id = `web-${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem(DEVICE_KEY, id)
  }
  return id
}

let unauthorizedHandler: (() => void) | null = null

/** 刷新令牌也失败时由路由层接管：清状态并回登录页 */
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const tokens = loadTokens()
  if (tokens?.accessToken) {
    config.headers.Authorization = `Bearer ${tokens.accessToken}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

/**
 * 用 refreshToken 换新的 accessToken。
 * 后端实现了「设备撤销水位」：登出或远程下线后刷新同样返回 401，此时不再重试，直接回登录页。
 */
function refreshAccessToken(): Promise<string | null> {
  if (refreshing) return refreshing

  const task = (async () => {
    const tokens = loadTokens()
    if (!tokens?.refreshToken) return null
    try {
      const response = await axios.post<ApiEnvelope<Tokens>>('/api/v1/auth/refresh', {
        refreshToken: tokens.refreshToken,
      })
      const next = response.data.data
      saveTokens(next)
      return next.accessToken
    } catch {
      clearTokens()
      return null
    }
  })()

  refreshing = task
  task.finally(() => {
    if (refreshing === task) refreshing = null
  })
  return task
}

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

function isAuthEndpoint(url?: string): boolean {
  return !!url && url.startsWith('/auth/')
}

http.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    if (!error.response) {
      throw new ApiError('NETWORK', '无法连接服务器，请确认后端已启动（localhost:8080）')
    }

    const status = error.response.status as number
    const body = error.response.data as ApiErrorBody | undefined
    const config = error.config as RetryableConfig | undefined

    if (status === 401 && config && !config._retried && !isAuthEndpoint(config.url)) {
      config._retried = true
      const token = await refreshAccessToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        return http.request(config)
      }
      unauthorizedHandler?.()
      throw new ApiError('E5001', CODE_MESSAGES.E5001)
    }

    const code = body?.code ?? `HTTP_${status}`
    // 5xx 多为网关/后端未就绪，直接说人话，别把状态码甩给用户
    const fallback = status >= 500 ? '服务暂时不可用，请稍后重试' : `请求失败（${status}）`
    const message = CODE_MESSAGES[code] ?? body?.message ?? fallback
    throw new ApiError(code, message)
  },
)

/** 统一请求入口：解开 {code,data} 包封，只返回 data */
export async function api<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config)
  const envelope = response.data
  if (envelope.code !== '0') {
    throw new ApiError(envelope.code, CODE_MESSAGES[envelope.code] ?? '请求失败')
  }
  return envelope.data
}

export function notifyError(error: unknown): void {
  const message = error instanceof ApiError ? error.message : '操作失败，请稍后重试'
  ElMessage.error(message)
}
