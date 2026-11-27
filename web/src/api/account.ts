import { api } from './http'
import type {
  AuditPage,
  ConsentLevel,
  ConsentScope,
  ConsentUpdateResult,
  ConsentView,
  DeviceView,
  Me,
  Tokens,
} from '@/types/api'

export function sendSmsCode(phone: string): Promise<{ expiresIn: number }> {
  return api({ url: '/auth/sms-code', method: 'post', data: { phone } })
}

export function login(payload: {
  phone: string
  code: string
  deviceId: string
  platform: string
}): Promise<Tokens> {
  return api({ url: '/auth/login', method: 'post', data: payload })
}

export function logout(): Promise<void> {
  return api({ url: '/auth/logout', method: 'post' })
}

export function fetchMe(): Promise<Me> {
  return api({ url: '/auth/me', method: 'get' })
}

export function fetchConsent(): Promise<ConsentView> {
  return api({ url: '/consent', method: 'get' })
}

export function updateConsent(
  level: ConsentLevel,
  scope: ConsentScope,
  clientRequestId: string,
): Promise<ConsentUpdateResult> {
  return api({
    url: '/consent',
    method: 'put',
    data: { level, scope, clientRequestId },
  })
}

export function listDevices(): Promise<DeviceView[]> {
  return api({ url: '/devices', method: 'get' })
}

export function revokeDevice(id: string): Promise<void> {
  return api({ url: `/devices/${encodeURIComponent(id)}`, method: 'delete' })
}

export function listAudit(cursor?: string, limit = 10): Promise<AuditPage> {
  return api({ url: '/audit', method: 'get', params: { cursor, limit } })
}
