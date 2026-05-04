import { useState } from 'react'
import { X, Save } from 'lucide-react'

export default function CreateFlagModal({ onClose, onSave, initial }) {
  const [form, setForm] = useState(initial || {
    flagKey: '', name: '', description: '',
    enabled: false, rolloutPercentage: 0,
    targetedUserIds: '[]',
    environments: '{"production":false,"staging":true,"development":true}',
    tags: '[]',
  })

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave(form)
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-900 border border-gray-700 rounded-2xl w-full max-w-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b border-gray-800">
          <h2 className="font-semibold">{initial ? 'Edit Flag' : 'Create Feature Flag'}</h2>
          <button onClick={onClose}><X className="w-5 h-5 text-gray-400" /></button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="text-xs text-gray-400 block mb-1">Flag Key * (lowercase, hyphens only)</label>
            <input required value={form.flagKey}
              onChange={e => set('flagKey', e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
              disabled={!!initial}
              placeholder="e.g. new-checkout-flow"
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:border-blue-500 disabled:opacity-50" />
          </div>

          <div>
            <label className="text-xs text-gray-400 block mb-1">Name *</label>
            <input required value={form.name} onChange={e => set('name', e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>

          <div>
            <label className="text-xs text-gray-400 block mb-1">Description</label>
            <textarea value={form.description} onChange={e => set('description', e.target.value)} rows={2}
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500 resize-none" />
          </div>

          <div className="flex items-center justify-between">
            <label className="text-sm">Globally Enabled</label>
            <button type="button" onClick={() => set('enabled', !form.enabled)}
              className={`relative w-11 h-6 rounded-full transition-colors ${form.enabled ? 'bg-blue-600' : 'bg-gray-700'}`}>
              <span className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-transform ${form.enabled ? 'translate-x-6' : 'translate-x-1'}`} />
            </button>
          </div>

          <div>
            <label className="text-xs text-gray-400 block mb-1">Rollout Percentage: {form.rolloutPercentage}%</label>
            <input type="range" min="0" max="100" value={form.rolloutPercentage}
              onChange={e => set('rolloutPercentage', +e.target.value)}
              className="w-full accent-blue-500" />
            <div className="flex justify-between text-xs text-gray-600 mt-0.5"><span>0%</span><span>50%</span><span>100%</span></div>
          </div>

          <div>
            <label className="text-xs text-gray-400 block mb-1">Targeted User IDs (JSON array)</label>
            <input value={form.targetedUserIds} onChange={e => set('targetedUserIds', e.target.value)}
              placeholder='["user-001","user-vip"]'
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:border-blue-500" />
          </div>

          <div>
            <label className="text-xs text-gray-400 block mb-1">Environments (JSON)</label>
            <input value={form.environments} onChange={e => set('environments', e.target.value)}
              className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:border-blue-500" />
          </div>

          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm border border-gray-700 rounded-lg hover:bg-gray-800">Cancel</button>
            <button type="submit"
              className="px-4 py-2 text-sm bg-blue-600 hover:bg-blue-700 rounded-lg font-medium flex items-center gap-2">
              <Save className="w-4 h-4" /> {initial ? 'Update' : 'Create Flag'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
