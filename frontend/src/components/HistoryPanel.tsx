import { fetchHistory } from '../api'
import type { ApprovalHistoryEntry } from '../types'
import { Badge } from './Badge'
import { DataPanel } from './DataPanel'

function formatTimestamp(isoString: string): string {
  const date = new Date(isoString)
  return Number.isNaN(date.getTime()) ? isoString : date.toLocaleString()
}

export function HistoryPanel() {
  return (
    <DataPanel<ApprovalHistoryEntry[]>
      title="Approval History"
      description="최근 감사 로그 (최대 50건)"
      load={() => fetchHistory(50)}
      isEmpty={(data) => data.length === 0}
      render={(entries) => (
        <table>
          <thead>
            <tr>
              <th>시각</th>
              <th>deployer</th>
              <th>스택</th>
              <th>action</th>
              <th>결과</th>
              <th>결정자</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr key={entry.id}>
                <td className="cell--muted">{formatTimestamp(entry.decidedAt)}</td>
                <td>{entry.deployer}</td>
                <td>{entry.stack}</td>
                <td className="cell--muted">{entry.action}</td>
                <td>
                  <Badge>{entry.result}</Badge>
                </td>
                <td className="cell--muted">{entry.decidedBy ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    />
  )
}
