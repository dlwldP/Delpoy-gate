import type { ReactNode } from 'react'
import { useAsync } from '../useAsync'

/**
 * Wraps a fetch call with a consistent loading/error/empty/data shell so each
 * table component only has to describe how to render its own rows.
 */
export function DataPanel<T>({
  title,
  description,
  load,
  isEmpty,
  render,
}: {
  title: string
  description: string
  load: () => Promise<T>
  isEmpty: (data: T) => boolean
  render: (data: T) => ReactNode
}) {
  const state = useAsync(load)

  return (
    <section className="panel">
      <header className="panel__header">
        <h2>{title}</h2>
        <p>{description}</p>
      </header>

      {state.status === 'loading' && <p className="panel__status">불러오는 중…</p>}
      {state.status === 'error' && (
        <p className="panel__status panel__status--error">불러오지 못했습니다: {state.message}</p>
      )}
      {state.status === 'ready' && isEmpty(state.data) && <p className="panel__status">데이터가 없습니다.</p>}
      {state.status === 'ready' && !isEmpty(state.data) && render(state.data)}
    </section>
  )
}
