import { fetchDeployers } from '../api'
import type { DeployerSummary } from '../types'
import { DataPanel } from './DataPanel'

export function DeployerPanel() {
  return (
    <DataPanel<DeployerSummary[]>
      title="Deployers"
      description="등록된 사용자와 각자가 보유한 claim 목록"
      load={fetchDeployers}
      isEmpty={(data) => data.length === 0}
      render={(deployers) => (
        <table>
          <thead>
            <tr>
              <th>이름</th>
              <th>claims</th>
            </tr>
          </thead>
          <tbody>
            {deployers.map((deployer) => (
              <tr key={deployer.id}>
                <td className="cell--name">{deployer.name}</td>
                <td>
                  <div className="claim-list">
                    {deployer.claims.length === 0 ? (
                      <span className="panel__status">claim 없음</span>
                    ) : (
                      deployer.claims.map((claim) => (
                        <code className="claim" key={claim}>
                          {claim}
                        </code>
                      ))
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    />
  )
}
