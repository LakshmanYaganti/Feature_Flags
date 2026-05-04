import { useState } from 'react'
import { Play, CheckCircle, XCircle } from 'lucide-react'
import { flagsApi } from '../services/api'

const REASON_COLORS = {
  TARGETED_USER:     'text-purple-400',
  FULL_ROLLOUT:      'text-green-400',
  PERCENTAGE_ROLLOUT:'text-blue-400',
  FLAG_DISABLED:     'text-gray-400',
  ENV_DISABLED:      'text-yellow-400',
  ZERO_ROLLOUT:      'text-gray-400',
  FLAG_NOT_FOUND:    'text-red-400',
  NO_USER_ID:        'text-gray-400',
}

export default function EvaluatePanel({ flags }) {
  const [form, setForm] = useState({ flagKey: '', userId: '', environment: 'production' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const handleEvaluate = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await flagsApi.evaluate(form)
      setResult(res)
    } catch {
      setResult(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-5">
      <h2 className="text-sm font-semibold mb-4 uppercase tracking-wide text-gray-300">SDK Evaluator</h2>

      <form onSubmit={handleEvaluate} className="space-y-3">
        <div>
          <label className="text-xs text-gray-400 block mb-1">Flag Key</label>
          <select value={form.flagKey} onChange={e => set('flagKey', e.target.value)} required
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
            <option value="">Select a flag...</option>
            {flags.map(f => <option key={f.id} value={f.flagKey}>{f.flagKey}</option>)}
          </select>
        </div>

        <div>
          <label className="text-xs text-gray-400 block mb-1">User ID</label>
          <input value={form.userId} onChange={e => set('userId', e.target.value)}
            placeholder="e.g. user-vip-001"
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
        </div>

        <div>
          <label className="text-xs text-gray-400 block mb-1">Environment</label>
          <select value={form.environment} onChange={e => set('environment', e.target.value)}
            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
            <option value="production">production</option>
            <option value="staging">staging</option>
            <option value="development">development</option>
          </select>
        </div>

        <button type="submit" disabled={loading}
          className="w-full bg-blue-600 hover:bg-blue-700 text-sm font-medium py-2 rounded-lg transition-colors flex items-center justify-center gap-2">
          <Play className="w-4 h-4" /> Evaluate
        </button>
      </form>

      {result && (
        <div className={`mt-4 p-3 rounded-xl border ${result.enabled ? 'bg-green-900/20 border-green-500/30' : 'bg-gray-800 border-gray-700'}`}>
          <div className="flex items-center gap-2 mb-2">
            {result.enabled
              ? <CheckCircle className="w-5 h-5 text-green-400" />
              : <XCircle className="w-5 h-5 text-gray-400" />}
            <span className={`font-bold ${result.enabled ? 'text-green-400' : 'text-gray-400'}`}>
              {result.enabled ? 'ENABLED' : 'DISABLED'}
            </span>
          </div>
          <div className="space-y-1 text-xs">
            <div className="flex justify-between">
              <span className="text-gray-500">Reason</span>
              <span className={REASON_COLORS[result.reason] || 'text-gray-300'}>{result.reason}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Flag</span>
              <span className="font-mono text-gray-300">{result.flagKey}</span>
            </div>
            {result.userId && (
              <div className="flex justify-between">
                <span className="text-gray-500">User</span>
                <span className="text-gray-300">{result.userId}</span>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="mt-4 pt-4 border-t border-gray-800">
        <p className="text-xs text-gray-500 font-mono">POST /api/flags/evaluate</p>
        <pre className="text-xs text-gray-600 mt-1">{`{
  "flagKey": "new-checkout-flow",
  "userId":  "user-123",
  "environment": "production"
}`}</pre>
      </div>
    </div>
  )
}
