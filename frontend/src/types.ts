// Mirrors the read-only DTOs returned by deploygate's backend
// (deploygate.dto.DeployerSummary / StackPolicySummary / ApprovalHistoryEntry).

export interface DeployerSummary {
  id: number
  name: string
  claims: string[]
}

export type ApprovalLevel = 'NONE' | 'SINGLE_APPROVER' | 'DUAL_APPROVER'

export interface StackPolicySummary {
  id: number
  stackName: string
  requiredClaim: string
  approvalLevel: ApprovalLevel
}

export type ApprovalDecision = 'ALLOWED' | 'DENIED' | 'PENDING'

export interface ApprovalHistoryEntry {
  id: number
  deployer: string
  stack: string
  action: string
  result: ApprovalDecision
  decidedBy: string | null
  decidedAt: string
}
