/** 后端统一响应包封：成功 {"code":"0","data":...}，失败 {"code":"E5xxx","message":"..."} */
export interface ApiEnvelope<T> {
  code: string
  data: T
}

export interface ApiErrorBody {
  code: string
  message: string
}

export interface Tokens {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface Me {
  userId: string
  deviceId: string
  lvl: number
}

export type ConsentLevel = 'L0' | 'L1' | 'L2' | 'L3' | 'L4'

export interface ConsentScope {
  notifyApps?: string[]
  qqGroups?: string[]
  quietHours?: string[]
}

export interface ConsentView {
  level: ConsentLevel
  scope: ConsentScope
  grantedAt: string
  revokedAt: string | null
  updatedAt: string
}

export interface ConsentUpdateResult {
  level: ConsentLevel
  revoked: { groups: number; apps: number }
  effectiveAt: string
}

export interface DeviceView {
  id: string
  platform: string | null
  lastSeenAt: string
  revokedAt: string | null
}

export interface AuditItem {
  id: string
  action: string
  payload: Record<string, unknown>
  createdAt: string
}

export interface AuditPage {
  items: AuditItem[]
  nextCursor: string | null
}
