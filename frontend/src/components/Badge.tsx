import type { ReactNode } from 'react'

const TONE_CLASS: Record<string, string> = {
  ALLOWED: 'badge badge--good',
  APPROVED: 'badge badge--good',
  NONE: 'badge badge--good',
  DENIED: 'badge badge--bad',
  REJECTED: 'badge badge--bad',
  PENDING: 'badge badge--pending',
  SINGLE_APPROVER: 'badge badge--pending',
  DUAL_APPROVER: 'badge badge--bad',
}

/** Small colored pill for enum-ish values (approval level, decision, ...). */
export function Badge({ children }: { children: ReactNode }) {
  const key = String(children)
  return <span className={TONE_CLASS[key] ?? 'badge'}>{children}</span>
}
