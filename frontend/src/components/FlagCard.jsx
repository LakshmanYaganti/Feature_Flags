import { Edit, Trash2, Users, Zap } from 'lucide-react'

export default function FlagCard({ flag, onToggle, onEdit, onDelete }) {
  const targeted = (() => {
    try { return JSON.parse(flag.targetedUserIds || '[]') } catch { return [] }
  })()

  const envs = (() => {
    try { return JSON.parse(flag.environments || '{}') } catch { return {} }
  })()

  return (
    <div className={`bg-gray-900 border rounded-xl p-4 transition-all ${flag.enabled ? 'border-blue-500/30' : 'border-gray-800'}`}>
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <span className="font-mono text-sm font-semibold text-blue-400 truncate">{flag.flagKey}</span>
          </div>
          <p className="text-xs text-gray-400 truncate">{flag.name}</p>
          {flag.description && <p className="text-xs text-gray-600 mt-0.5 truncate">{flag.description}</p>}
        </div>

        {/* Toggle */}
        <button onClick={() => onToggle(flag.id)}
          className={`relative flex-shrink-0 w-11 h-6 rounded-full transition-colors ${flag.enabled ? 'bg-blue-600' : 'bg-gray-700'}`}>
          <span className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${flag.enabled ? 'translate-x-6' : 'translate-x-1'}`} />
        </button>
      </div>

      {/* Rollout bar */}
      <div className="mb-3">
        <div className="flex justify-between text-xs text-gray-500 mb-1">
          <span>Rollout</span>
          <span className="font-mono">{flag.rolloutPercentage}%</span>
        </div>
        <div className="h-1.5 bg-gray-800 rounded-full overflow-hidden">
          <div className="h-full bg-blue-500 rounded-full transition-all"
            style={{ width: `${flag.rolloutPercentage}%` }} />
        </div>
      </div>

      {/* Environments */}
      <div className="flex flex-wrap gap-1 mb-3">
        {Object.entries(envs).map(([env, val]) => (
          <span key={env} className={`text-xs px-2 py-0.5 rounded-full border font-mono ${
            val ? 'bg-green-500/10 text-green-400 border-green-500/30'
                : 'bg-gray-800 text-gray-600 border-gray-700'}`}>
            {env}
          </span>
        ))}
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3 text-xs text-gray-500">
          {targeted.length > 0 && (
            <span className="flex items-center gap-1">
              <Users className="w-3 h-3" /> {targeted.length} targeted
            </span>
          )}
          <span className="flex items-center gap-1">
            <Zap className="w-3 h-3" />
            {new Date(flag.updatedAt).toLocaleDateString()}
          </span>
        </div>

        <div className="flex items-center gap-1">
          <button onClick={() => onEdit(flag)}
            className="p-1.5 text-gray-400 hover:text-white hover:bg-gray-800 rounded-lg transition-colors">
            <Edit className="w-3.5 h-3.5" />
          </button>
          <button onClick={() => onDelete(flag.id)}
            className="p-1.5 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors">
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  )
}
