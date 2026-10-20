import { useEffect, useRef, useState } from 'react'
import './App.css'

const API = '/api/cache'

function App() {
  const [state, setState] = useState(null)
  const [key, setKey] = useState('')
  const [value, setValue] = useState('')
  const [log, setLog] = useState([])
  const [activeKey, setActiveKey] = useState(null)
  const [lastAction, setLastAction] = useState(null)
  const [error, setError] = useState('')
  const [result, setResult] = useState('')
  const [busy, setBusy] = useState(false)
  const [ghost, setGhost] = useState(null)

  const previousEntries = useRef([])
  const animationTimer = useRef(null)
  const ghostTimer = useRef(null)

  const loadState = async () => {
    try {
      const response = await fetch(`${API}/state`)
      if (!response.ok) throw new Error('Unable to load cache state')

      const data = await response.json()
      setState(data)
      previousEntries.current = data.entries || []
      setError('')
      return data
    } catch {
      setError('Backend connection unavailable.')
      return null
    }
  }

  useEffect(() => {
    loadState()

    return () => {
      clearTimeout(animationTimer.current)
      clearTimeout(ghostTimer.current)
    }
  }, [])

  const addLog = (type, message) => {
    setLog((previous) => [
      {
        id: Date.now() + Math.random(),
        type,
        message,
        time: new Date().toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      },
      ...previous.slice(0, 11)
    ])
  }

  const animate = (action, selectedKey = null) => {
    clearTimeout(animationTimer.current)
    setLastAction(action)
    setActiveKey(selectedKey)

    animationTimer.current = setTimeout(() => {
      setLastAction(null)
      setActiveKey(null)
    }, 900)
  }

  const showGhost = (entryKey, entryValue) => {
    clearTimeout(ghostTimer.current)
    setGhost({ key: entryKey, value: entryValue })

    ghostTimer.current = setTimeout(() => {
      setGhost(null)
    }, 950)
  }

  const put = async () => {
    const trimmedKey = key.trim()
    const trimmedValue = value.trim()

    if (!trimmedKey || !trimmedValue) {
      setError('Enter both a key and a value.')
      setResult('')
      return
    }

    setBusy(true)
    setError('')
    setResult('')

    const oldEntries = previousEntries.current
    const oldMap = new Map(
        oldEntries.map((entry) => {
          const [entryKey, entryValue] = Object.entries(entry)[0]
          return [entryKey, entryValue]
        })
    )

    try {
      const response = await fetch(
          `${API}/keys/${encodeURIComponent(trimmedKey)}`,
          {
            method: 'PUT',
            headers: { 'Content-Type': 'text/plain' },
            body: trimmedValue
          }
      )

      if (!response.ok) throw new Error('PUT operation failed.')

      const newState = await loadState()
      if (!newState) return

      const newKeys = new Set(
          newState.entries.map((entry) => Object.keys(entry)[0])
      )

      const evictedKey = [...oldMap.keys()].find(
          (oldKey) => !newKeys.has(oldKey)
      )

      if (evictedKey) {
        const evictedValue = oldMap.get(evictedKey)
        addLog('evict', `PUT ${trimmedKey} → EVICT ${evictedKey}`)
        setResult(
            `Inserted ${trimmedKey} · evicted ${evictedKey} as the LRU entry`
        )
        showGhost(evictedKey, evictedValue)
        animate('evict', trimmedKey)
      } else if (oldMap.has(trimmedKey)) {
        addLog('update', `PUT ${trimmedKey} → UPDATED`)
        setResult(`Updated ${trimmedKey} · moved to MRU`)
        animate('update', trimmedKey)
      } else {
        addLog('put', `PUT ${trimmedKey} → INSERTED`)
        setResult(`Inserted ${trimmedKey} · placed at MRU`)
        animate('put', trimmedKey)
      }

      setKey('')
      setValue('')
    } catch (err) {
      setError(err.message || 'PUT operation failed.')
    } finally {
      setBusy(false)
    }
  }

  const get = async () => {
    const trimmedKey = key.trim()

    if (!trimmedKey) {
      setError('Enter a key first.')
      setResult('')
      return
    }

    setBusy(true)
    setError('')
    setResult('')

    try {
      const response = await fetch(
          `${API}/keys/${encodeURIComponent(trimmedKey)}`
      )

      if (response.ok) {
        const foundValue = await response.text()

        addLog('hit', `GET ${trimmedKey} → HIT (${foundValue})`)
        setResult(`HIT · ${trimmedKey} found · moved to MRU`)
        animate('hit', trimmedKey)
      } else {
        addLog('miss', `GET ${trimmedKey} → MISS`)
        setResult(`MISS · ${trimmedKey} is not in the cache`)
        animate('miss', trimmedKey)
      }

      await loadState()
      setKey('')
    } catch {
      setError('GET operation failed.')
    } finally {
      setBusy(false)
    }
  }

  const remove = async () => {
    const trimmedKey = key.trim()

    if (!trimmedKey) {
      setError('Enter a key first.')
      setResult('')
      return
    }

    setBusy(true)
    setError('')
    setResult('')

    try {
      const response = await fetch(
          `${API}/keys/${encodeURIComponent(trimmedKey)}`,
          { method: 'DELETE' }
      )

      if (response.ok) {
        addLog('delete', `DELETE ${trimmedKey} → REMOVED`)
        setResult(`Removed ${trimmedKey} from the cache`)
        animate('delete', trimmedKey)
      } else {
        addLog('miss', `DELETE ${trimmedKey} → NOT FOUND`)
        setResult(`Nothing removed · ${trimmedKey} was not found`)
        animate('miss', trimmedKey)
      }

      await loadState()
      setKey('')
    } catch {
      setError('DELETE operation failed.')
    } finally {
      setBusy(false)
    }
  }

  const reset = async () => {
    setBusy(true)
    setError('')
    setResult('')

    try {
      const response = await fetch(`${API}/reset`, { method: 'POST' })
      if (!response.ok) throw new Error('RESET operation failed.')

      addLog('reset', 'RESET → CACHE CLEARED')
      setResult('Cache cleared · all nodes and statistics reset')
      setKey('')
      setValue('')
      setGhost(null)
      animate('reset')
      await loadState()
    } catch (err) {
      setError(err.message || 'RESET operation failed.')
    } finally {
      setBusy(false)
    }
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !busy) {
      put()
    }
  }

  if (!state) {
    return (
        <div className="loading-screen">
          <div className="loading-orb" />
          <span>INITIALISING CACHE LAB</span>
        </div>
    )
  }

  const hitRate =
      state.stats.hits + state.stats.misses === 0
          ? '0%'
          : `${(
              (state.stats.hits /
                  (state.stats.hits + state.stats.misses)) *
              100
          ).toFixed(1)}%`

  return (
      <div className="app">
        <div className="ambient ambient-one" />
        <div className="ambient ambient-two" />
        <div className="ambient ambient-three" />
        <div className="grid-overlay" />

        <header className="hero">
          <div className="brand-line">
            <span className="live-dot" />
            CACHE LAB
            <span className="slash">/</span>
            INTERACTIVE
          </div>

          <div className="hero-main">
            <div>
              <h1>
                LRU <span>Cache</span>
              </h1>
              <p className="hero-description">
                A live view of the two structures behind constant-time
                cache operations: a HashMap for lookup and a doubly linked
                list for recency.
              </p>
            </div>

            <div className="capacity-display">
              <span>CAPACITY</span>
              <strong>{state.capacity}</strong>
            </div>
          </div>
        </header>

        <section className="console glass-panel">
          <div className="section-label">
            <span>01</span>
            OPERATION CONSOLE
          </div>

          <div className="console-body">
            <div className="input-group">
              <label htmlFor="cache-key">KEY</label>
              <input
                  id="cache-key"
                  value={key}
                  onChange={(event) => {
                    setKey(event.target.value)
                    setError('')
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="e.g. user:42"
                  autoComplete="off"
              />
            </div>

            <div className="input-group">
              <label htmlFor="cache-value">VALUE</label>
              <input
                  id="cache-value"
                  value={value}
                  onChange={(event) => {
                    setValue(event.target.value)
                    setError('')
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="e.g. Preksha"
                  autoComplete="off"
              />
            </div>

            <div className="button-group">
              <button
                  className="primary-button"
                  onClick={put}
                  disabled={busy}
              >
                PUT
              </button>
              <button onClick={get} disabled={busy}>
                GET
              </button>
              <button onClick={remove} disabled={busy}>
                DELETE
              </button>
              <button
                  className="danger-button"
                  onClick={reset}
                  disabled={busy}
              >
                RESET
              </button>
            </div>
          </div>

          {(error || result) && (
              <div className={`console-feedback ${error ? 'is-error' : ''}`}>
                <span className="feedback-dot" />
                {error || result}
              </div>
          )}
        </section>

        <section className="metrics">
          <Metric label="SIZE" value={state.size} />
          <Metric label="HITS" value={state.stats.hits} />
          <Metric label="MISSES" value={state.stats.misses} />
          <Metric label="EVICTIONS" value={state.stats.evictions} />
          <Metric label="HIT RATE" value={hitRate} accent />
        </section>

        <div className={`flow-strip ${lastAction ? 'flow-active' : ''}`}>
          <span className="flow-step">HASHMAP</span>
          <span className="flow-line" />
          <span className="flow-step">NODE REFERENCE</span>
          <span className="flow-line" />
          <span className="flow-step">RECENCY UPDATE</span>
          <span className="flow-status">
{lastAction ? lastAction.toUpperCase() : 'READY'}
</span>
        </div>

        <main className="visual-grid">
          <section className="structure-panel glass-panel">
            <PanelHeader
                number="02"
                label="STRUCTURE"
                title="Recency Chain"
                description="MRU stays at the front. The tail is the next eviction candidate."
                badge="MRU → LRU"
            />

            <div className="chain-wrapper">
              {state.entries.length === 0 ? (
                  <EmptyState
                      symbol="∅"
                      title="Cache is empty"
                      text="Insert a key to create the first node."
                  />
              ) : (
                  <div className="linked-list">
                    <div className="endpoint head">HEAD</div>

                    {state.entries.map((entry, index) => {
                      const [entryKey, entryValue] =
                          Object.entries(entry)[0]

                      const isActive = activeKey === entryKey
                      const isFirst = index === 0
                      const isLast = index === state.entries.length - 1

                      return (
                          <div
                              className={`node-group
${isActive ? `active-node action-${lastAction}` : ''}
  ${isFirst ? 'node-mru' : ''}
  ${isLast ? 'node-lru' : ''}
  `}
                              key={entryKey}
                              style={{ '--node-delay': `${index * 45}ms` }}
                          >
                            <div className="cache-node-card">
                              <div className="node-top">
<span className="node-index">
NODE {String(index + 1).padStart(2, '0')}
</span>

                                {isFirst && (
                                    <span className="node-status mru">MRU</span>
                                )}

                                {isLast && (
                                    <span className="node-status lru">LRU</span>
                                )}
                              </div>

                              <div className="node-key">{entryKey}</div>
                              <div className="node-value">{entryValue}</div>

                              <div className="node-pointer">
                                <span>prev</span>
                                <span className="pointer-dot" />
                                <span>next</span>
                              </div>
                            </div>

                            {!isLast && (
                                <div className="chain-arrow" aria-hidden="true">
                                  <span>←</span>
                                  <i />
                                  <span>→</span>
                                </div>
                            )}
                          </div>
                      )
                    })}

                    {ghost && (
                        <div className="node-group ghost-node">
                          <div className="cache-node-card">
                            <div className="node-top">
                              <span className="node-index">EVICTED</span>
                              <span className="node-status lru">REMOVED</span>
                            </div>
                            <div className="node-key">{ghost.key}</div>
                            <div className="node-value">{ghost.value}</div>
                          </div>
                        </div>
                    )}

                    <div className="endpoint tail">TAIL</div>
                  </div>
              )}
            </div>
          </section>

          <section className="hash-panel glass-panel">
            <PanelHeader
                number="03"
                label="LOOKUP"
                title="HashMap View"
                description="Each key resolves directly to its cache node."
                badge="O(1)"
                badgeClass="complexity-badge"
            />

            <div className="hash-visual">
              {state.entries.length === 0 ? (
                  <EmptyState
                      small
                      symbol="#"
                      title="No mappings"
                      text="HashMap is currently empty."
                  />
              ) : (
                  <div className="hash-list">
                    {state.entries.map((entry, index) => {
                      const [entryKey, entryValue] =
                          Object.entries(entry)[0]

                      return (
                          <div
                              className={`hash-entry ${
                                  activeKey === entryKey ? 'hash-active' : ''
                              }`}
                              key={entryKey}
                          >
                            <div className="hash-key">
                            <span className="hash-index">
                            {String(index).padStart(2, '0')}
                            </span>
                              <strong>{entryKey}</strong>
                            </div>

                            <div className="hash-arrow">→</div>

                            <div className="hash-value">
                              <span className="node-dot" />
                              <span>{entryValue}</span>
                            </div>
                          </div>
                      )
                    })}
                  </div>
              )}
            </div>

            <div className="panel-footnote">
              <span className="legend-dot" />
              key → node reference
            </div>
          </section>

          <section className="trace-panel glass-panel">
            <PanelHeader
                number="04"
                label="TRACE"
                title="Execution Trace"
                description="Recent operations performed against the cache."
                badge={`${log.length} EVENTS`}
            />

            <div className="trace-list">
              {log.length === 0 ? (
                  <EmptyState
                      small
                      symbol=">_"
                      title="Waiting for input"
                      text="Run PUT, GET or DELETE to see the trace."
                  />
              ) : (
                  log.map((item) => (
                      <div
                          className={`trace-entry trace-${item.type}`}
                          key={item.id}
                      >
                        <div className="trace-marker">
                          {item.type === 'put' && '+'}
                          {item.type === 'update' && '↻'}
                          {item.type === 'hit' && '✓'}
                          {item.type === 'miss' && '×'}
                          {item.type === 'delete' && '−'}
                          {item.type === 'evict' && '⚡'}
                          {item.type === 'reset' && '↺'}
                        </div>

                        <div className="trace-content">
                          <span className="trace-time">{item.time}</span>
                          <strong>{item.message}</strong>
                        </div>
                      </div>
                  ))
              )}
            </div>
          </section>
        </main>

        <footer>
          <span>LRU CACHE VISUALIZER</span>
          <span>HASHMAP × DOUBLY LINKED LIST</span>
          <span>GET / PUT / DELETE</span>
        </footer>
      </div>
  )
}

function Metric({ label, value, accent }) {
  return (
      <div className={`metric ${accent ? 'metric-accent' : ''}`}>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
  )
}

function PanelHeader({
                       number,
                       label,
                       title,
                       description,
                       badge,
                       badgeClass = ''
                     }) {
  return (
      <div className="panel-header">
        <div>
          <div className="section-label">
            <span>{number}</span>
            {label}
          </div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>

        <div className={`panel-badge ${badgeClass}`}>{badge}</div>
      </div>
  )
}

function EmptyState({ symbol, title, text, small = false }) {
  return (
      <div className={`empty-visual ${small ? 'small' : ''}`}>
        <div className="empty-symbol">{symbol}</div>
        <strong>{title}</strong>
        <span>{text}</span>
      </div>
  )
}

export default App
