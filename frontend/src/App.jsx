import { useState, useEffect, useCallback } from 'react'
import toast, { Toaster } from 'react-hot-toast'
import { Plus, Flag, Wifi, WifiOff, RefreshCw } from 'lucide-react'
import FlagCard from './components/FlagCard'
import CreateFlagModal from './components/CreateFlagModal'
import EvaluatePanel from './components/EvaluatePanel'
import { flagsApi } from './services/api'
import { useWebSocket } from './hooks/useWebSocket'

export default function App() {
  const [flags, setFlags] = useState([])
  const [showModal, setShowModal] = useState(false)
  const [editingFlag, setEditingFlag] = useState(null)
  const [wsConnected, setWsConnected] = useState(false)
  const [liveEvents, setLiveEvents] = useState([])

  useEffect(() => {
    flagsApi.getAll().then(setFlags).catch(() =>
      toast.error('Backend not reachable — start it on :8090'))
  }, [])

  const handleFlagUpdate = useCallback((event) => {
    setWsConnected(true)
    setLiveEvents(prev => [{ ...event, ts: new Date().toLocaleTimeString() }, ...prev].slice(0, 10))
    flagsApi.getAll().then(setFlags).catch(() => {})
    toast(`Flag ${event.action?.toLowerCase()}: ${event.flagKey}`, { icon: '🚩' })
  }, [])

  useWebSocket(handleFlagUpdate)

  const handleCreate = async (data) => {
    try {
      const flag = await flagsApi.create(data)
      setFlags(prev => [...prev, flag])
      toast.success('Flag created!')
      setShowModal(false)
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to create flag')
    }
  }

  const handleUpdate = async (data) => {
    try {
      const flag = await flagsApi.update(editingFlag.id, data)
      setFlags(prev => prev.map(f => f.id === flag.id ? flag : f))
      toast.success('Flag updated!')
      setEditingFlag(null)
    } catch {
      toast.error('Failed to update flag')
    }
  }

  const handleToggle = async (id) => {
    try {
      const flag = await flagsApi.toggle(id)
      setFlags(prev => prev.map(f => f.id === id ? flag : f))
    } catch { toast.error('Failed to toggle flag') }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this flag?')) return
    try {
      await flagsApi.delete(id)
      setFlags(prev => prev.filter(f => f.id !== id))
      toast.success('Flag deleted')
    } catch { toast.error('Failed to delete flag') }
  }

  const activeCount  = flags.filter(f => f.enabled).length
  const fullRollout  = flags.filter(f => f.enabled && f.rolloutPercentage === 100).length
  const partialRollout = flags.filter(f => f.enabled && f.rolloutPercentage > 0 && f.rolloutPercentage < 100).length

  return (
    <div className="min-h-screen bg-gray-950 p-6">
      <Toaster position="top-right" toastOptions={{
        style: { background: '#1f2937', color: '#f9fafb', border: '1px solid #374151' }
      }} />

      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-2">
              <Flag className="w-6 h-6 text-blue-400" /> Feature Flag Service
            </h1>
            <p className="text-gray-400 text-sm mt-1">
              Redis pub/sub → WebSocket real-time updates · Consistent percentage bucketing
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className={`text-xs px-3 py-1.5 rounded-full border flex items-center gap-1.5 ${wsConnected ? 'text-green-400 bg-green-500/10 border-green-500/30' : 'text-gray-500 bg-gray-800 border-gray-700'}`}>
              {wsConnected ? <Wifi className="w-3 h-3" /> : <WifiOff className="w-3 h-3" />}
              {wsConnected ? 'Live via Redis' : 'Connecting...'}
            </span>
            <button onClick={() => flagsApi.getAll().then(setFlags)}
              className="p-2 text-gray-400 hover:text-white hover:bg-gray-800 rounded-lg">
              <RefreshCw className="w-4 h-4" />
            </button>
            <button onClick={() => setShowModal(true)}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-sm font-medium px-4 py-2 rounded-lg">
              <Plus className="w-4 h-4" /> New Flag
            </button>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Total Flags',     value: flags.length,    color: 'text-blue-400' },
            { label: 'Active',          value: activeCount,     color: 'text-green-400' },
            { label: 'Full Rollout',    value: fullRollout,     color: 'text-purple-400' },
            { label: 'Partial Rollout', value: partialRollout,  color: 'text-yellow-400' },
          ].map(s => (
            <div key={s.label} className="bg-gray-900 border border-gray-800 rounded-xl p-4 text-center">
              <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
              <p className="text-xs text-gray-500 mt-1">{s.label}</p>
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Flags Grid */}
          <div className="lg:col-span-3">
            <h2 className="text-sm font-semibold text-gray-300 uppercase tracking-wide mb-3">
              Flags ({flags.length})
            </h2>
            {flags.length === 0 ? (
              <div className="flex flex-col items-center py-16 text-gray-500">
                <Flag className="w-12 h-12 mb-3 opacity-20" />
                <p className="text-sm">No flags yet.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {flags.map(f => (
                  <FlagCard key={f.id} flag={f}
                    onToggle={handleToggle}
                    onEdit={setEditingFlag}
                    onDelete={handleDelete} />
                ))}
              </div>
            )}
          </div>

          {/* Right Panel */}
          <div className="space-y-4">
            <EvaluatePanel flags={flags} />

            {/* Live Events */}
            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-4">
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                Live Events (Redis → WS)
              </h3>
              {liveEvents.length === 0 ? (
                <p className="text-xs text-gray-600">Toggle a flag to see real-time events here.</p>
              ) : (
                <div className="space-y-1.5">
                  {liveEvents.map((e, i) => (
                    <div key={i} className="text-xs flex items-center justify-between">
                      <span className={`font-mono ${e.enabled ? 'text-green-400' : 'text-gray-500'}`}>
                        {e.flagKey}
                      </span>
                      <span className="text-gray-600">{e.ts}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {showModal && <CreateFlagModal onClose={() => setShowModal(false)} onSave={handleCreate} />}
      {editingFlag && (
        <CreateFlagModal
          initial={{ ...editingFlag, flagKey: editingFlag.flagKey }}
          onClose={() => setEditingFlag(null)}
          onSave={handleUpdate} />
      )}
    </div>
  )
}
