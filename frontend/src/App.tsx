import { DeployerPanel } from './components/DeployerPanel'
import { HistoryPanel } from './components/HistoryPanel'
import { StackPolicyPanel } from './components/StackPolicyPanel'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="app__header">
        <h1>deploy-gate admin</h1>
        <p>등록된 deployer, stack policy, 최근 승인/거부 이력을 조회하는 읽기 전용 화면입니다.</p>
      </header>

      <main className="app__grid">
        <DeployerPanel />
        <StackPolicyPanel />
        <HistoryPanel />
      </main>
    </div>
  )
}

export default App
