import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export const flagsApi = {
  getAll:   ()         => api.get('/flags').then(r => r.data),
  create:   (data)     => api.post('/flags', data).then(r => r.data),
  update:   (id, data) => api.put(`/flags/${id}`, data).then(r => r.data),
  toggle:   (id)       => api.patch(`/flags/${id}/toggle`).then(r => r.data),
  delete:   (id)       => api.delete(`/flags/${id}`),
  getLogs:  (key)      => api.get(`/flags/${key}/logs`).then(r => r.data),
  evaluate: (data)     => api.post('/flags/evaluate', data).then(r => r.data),
  getStats: ()         => api.get('/stats').then(r => r.data),
}
