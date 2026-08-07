import type { ApprovalHistoryEntry, DeployerSummary, StackPolicySummary } from './types'

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) {
    throw new Error(`${path} responded with ${response.status}`)
  }
  return (await response.json()) as T
}

export function fetchDeployers(): Promise<DeployerSummary[]> {
  return getJson<DeployerSummary[]>('/admin/deployers')
}

export function fetchStackPolicies(): Promise<StackPolicySummary[]> {
  return getJson<StackPolicySummary[]>('/admin/stack-policies')
}

export function fetchHistory(limit = 50): Promise<ApprovalHistoryEntry[]> {
  return getJson<ApprovalHistoryEntry[]>(`/approval/history?limit=${limit}`)
}
