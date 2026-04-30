import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000',
})

export async function fetchPatientMemory(userId) {
  const response = await api.get(`/memory/${userId}`)
  return response.data
}

export async function updatePatientMemory(userId, payload) {
  const response = await api.put(`/memory/${userId}`, payload)
  return response.data
}

export async function fetchAlerts(userId) {
  const response = await api.get(`/alerts/${userId}`)
  return response.data
}

export async function fetchConversationLog(userId) {
  const response = await api.get(`/conversations/${userId}`)
  return response.data
}

export default api
