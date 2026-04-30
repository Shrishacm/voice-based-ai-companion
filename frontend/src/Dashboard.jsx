import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchAlerts, fetchConversationLog, fetchPatientMemory } from './api'

function formatDateTime(value) {
  if (!value) {
    return 'Not available'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

function Dashboard({ userId }) {
  const [memory, setMemory] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [conversation, setConversation] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadDashboard() {
      setLoading(true)
      setError('')
      try {
        const [memoryData, alertsData, conversationData] = await Promise.all([
          fetchPatientMemory(userId),
          fetchAlerts(userId),
          fetchConversationLog(userId),
        ])
        if (!cancelled) {
          setMemory(memoryData)
          setAlerts(alertsData)
          setConversation(conversationData)
        }
      } catch (err) {
        if (!cancelled) {
          setError('Could not load patient information. Check that the backend is running.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadDashboard()
    return () => {
      cancelled = true
    }
  }, [userId])

  const appointments = [...(memory?.appointments || [])].sort((a, b) => a.date.localeCompare(b.date)).slice(0, 3)

  if (loading) {
    return <section className="panel"><p>Loading dashboard...</p></section>
  }

  if (error) {
    return <section className="panel error"><p>{error}</p></section>
  }

  return (
    <div className="grid-layout">
      <section className="panel spotlight">
        <div className="spotlight-header">
          <div>
            <p className="eyebrow">Patient Profile</p>
            <h2>{memory?.patient_name}</h2>
          </div>
          <div className="age-badge">Age {memory?.age}</div>
        </div>
        <p className="hero-copy">
          This profile feeds the voice companion context used in every conversation.
        </p>
        <div className="quick-actions">
          <Link to="/voice" className="action-button voice-button">
            <span>Voice Interface</span>
          </Link>
          <Link to="/qr" className="action-button qr-button">
            <span>QR Code</span>
          </Link>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h3>Medications</h3>
        </div>
        <ul className="stack-list">
          {memory?.medications?.map((medication) => (
            <li key={medication}>{medication}</li>
          ))}
        </ul>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h3>Family Members</h3>
        </div>
        <ul className="stack-list">
          {memory?.family_members?.map((member) => (
            <li key={`${member.name}-${member.relationship}`}>
              <strong>{member.name}</strong>
              <span>{member.relationship}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h3>Upcoming Appointments</h3>
        </div>
        <ul className="stack-list">
          {appointments.map((appointment) => (
            <li key={`${appointment.date}-${appointment.description}`}>
              <strong>{appointment.description}</strong>
              <span>{formatDateTime(appointment.date)}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="panel wide-panel">
        <div className="panel-header">
          <h3>Last 10 Conversations</h3>
        </div>
        <div className="log-list">
          {conversation.length === 0 ? <p>No conversations yet.</p> : null}
          {conversation.map((item) => (
            <article key={item.id} className={`log-entry ${item.role}`}>
              <div className="log-meta">
                <strong>{item.role === 'assistant' ? 'AI Companion' : 'Patient'}</strong>
                <span>{formatDateTime(item.created_at)}</span>
              </div>
              <p>{item.content}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="panel wide-panel">
        <div className="panel-header">
          <h3>Caregiver Alerts</h3>
        </div>
        <div className="alert-list">
          {alerts.length === 0 ? <p>No active alerts.</p> : null}
          {alerts.slice(0, 5).map((alert) => (
            <article key={alert.id} className={`alert-card ${alert.severity}`}>
              <strong>{alert.alert_type.split('_').join(' ')}</strong>
              <p>{alert.message}</p>
              <span>{formatDateTime(alert.created_at)}</span>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}

export default Dashboard
