import { fetchStackPolicies } from '../api'
import type { StackPolicySummary } from '../types'
import { Badge } from './Badge'
import { DataPanel } from './DataPanel'

export function StackPolicyPanel() {
  return (
    <DataPanel<StackPolicySummary[]>
      title="Stack Policies"
      description="스택별 배포에 필요한 claim과 승인 레벨"
      load={fetchStackPolicies}
      isEmpty={(data) => data.length === 0}
      render={(policies) => (
        <table>
          <thead>
            <tr>
              <th>스택</th>
              <th>필요 claim</th>
              <th>승인 레벨</th>
            </tr>
          </thead>
          <tbody>
            {policies.map((policy) => (
              <tr key={policy.id}>
                <td className="cell--name">{policy.stackName}</td>
                <td>
                  <code className="claim">{policy.requiredClaim}</code>
                </td>
                <td>
                  <Badge>{policy.approvalLevel}</Badge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    />
  )
}
