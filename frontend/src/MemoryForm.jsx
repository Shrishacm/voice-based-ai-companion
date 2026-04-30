import { useEffect, useState } from 'react'
import { fetchPatientMemory, updatePatientMemory } from './api'

function normalizeDateTimeForInput(value) {
  if (!value) {
    return ''
  }

  return String(value).slice(0, 16)
}

function createEmptyMemory() {
  return {
    patient_name: '',
    age: 72,
    medications: [''],
    family_members: [{ name: '', relationship: '' }],
    appointments: [{ date: '', description: '' }],
  }
}

function MemoryForm({ userId }) {
  const [form, setForm] = useState(createEmptyMemory())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [status, setStatus] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadMemory() {
      setLoading(true)
      try {
        const data = await fetchPatientMemory(userId)
        if (!cancelled) {
          setForm({
            patient_name: data.patient_name || '',
            age: data.age || 72,
            medications: data.medications?.length ? data.medications : [''],
            family_members: data.family_members?.length ? data.family_members : [{ name: '', relationship: '' }],
            appointments: data.appointments?.length
              ? data.appointments.map((item) => ({
                  ...item,
                  date: normalizeDateTimeForInput(item.date),
                }))
              : [{ date: '', description: '' }],
          })
        }
      } catch (err) {
        if (!cancelled) {
          setStatus('Unable to load memory. Check the backend connection.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadMemory()
    return () => {
      cancelled = true
    }
  }, [userId])

  function setField(key, value) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function updateMedication(index, value) {
    const medications = [...form.medications]
    medications[index] = value
    setField('medications', medications)
  }

  function updateFamilyMember(index, key, value) {
    const familyMembers = [...form.family_members]
    familyMembers[index] = { ...familyMembers[index], [key]: value }
    setField('family_members', familyMembers)
  }

  function updateAppointment(index, key, value) {
    const appointments = [...form.appointments]
    appointments[index] = { ...appointments[index], [key]: value }
    setField('appointments', appointments)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setStatus('Saving changes...')
    try {
      await updatePatientMemory(userId, {
        ...form,
        medications: form.medications.filter(Boolean),
        family_members: form.family_members.filter((member) => member.name && member.relationship),
        appointments: form.appointments.filter((item) => item.date && item.description),
      })
      setStatus('Memory updated successfully.')
    } catch (err) {
      setStatus('Could not save changes. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <section className="panel"><p>Loading form...</p></section>
  }

  return (
    <section className="panel form-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Edit Patient Memory</p>
          <h2>Update the companion's context</h2>
        </div>
      </div>

      <form className="memory-form" onSubmit={handleSubmit}>
        <label>
          Patient Name
          <input value={form.patient_name} onChange={(event) => setField('patient_name', event.target.value)} required />
        </label>

        <label>
          Age
          <input
            type="number"
            min="1"
            value={form.age}
            onChange={(event) => setField('age', Number(event.target.value))}
            required
          />
        </label>

        <div className="form-section">
          <div className="section-title-row">
            <h3>Medications</h3>
            <button type="button" className="ghost-button" onClick={() => setField('medications', [...form.medications, ''])}>
              Add medication
            </button>
          </div>
          {form.medications.map((medication, index) => (
            <div key={`${medication}-${index}`} className="row-with-action">
              <input
                value={medication}
                onChange={(event) => updateMedication(index, event.target.value)}
                placeholder="aspirin 8am"
              />
              <button
                type="button"
                className="danger-button"
                onClick={() => setField('medications', form.medications.filter((_, itemIndex) => itemIndex !== index))}
              >
                Remove
              </button>
            </div>
          ))}
        </div>

        <div className="form-section">
          <div className="section-title-row">
            <h3>Family Members</h3>
            <button
              type="button"
              className="ghost-button"
              onClick={() => setField('family_members', [...form.family_members, { name: '', relationship: '' }])}
            >
              Add family member
            </button>
          </div>
          {form.family_members.map((member, index) => (
            <div key={`${member.name}-${member.relationship}-${index}`} className="two-column-row">
              <input
                value={member.name}
                onChange={(event) => updateFamilyMember(index, 'name', event.target.value)}
                placeholder="Priya"
              />
              <input
                value={member.relationship}
                onChange={(event) => updateFamilyMember(index, 'relationship', event.target.value)}
                placeholder="daughter"
              />
              <button
                type="button"
                className="danger-button full-width-mobile"
                onClick={() => setField('family_members', form.family_members.filter((_, itemIndex) => itemIndex !== index))}
              >
                Remove
              </button>
            </div>
          ))}
        </div>

        <div className="form-section">
          <div className="section-title-row">
            <h3>Appointments</h3>
            <button
              type="button"
              className="ghost-button"
              onClick={() => setField('appointments', [...form.appointments, { date: '', description: '' }])}
            >
              Add appointment
            </button>
          </div>
          {form.appointments.map((appointment, index) => (
            <div key={`${appointment.date}-${appointment.description}-${index}`} className="two-column-row">
              <input
                type="datetime-local"
                value={appointment.date}
                onChange={(event) => updateAppointment(index, 'date', event.target.value)}
              />
              <input
                value={appointment.description}
                onChange={(event) => updateAppointment(index, 'description', event.target.value)}
                placeholder="Doctor checkup"
              />
              <button
                type="button"
                className="danger-button full-width-mobile"
                onClick={() => setField('appointments', form.appointments.filter((_, itemIndex) => itemIndex !== index))}
              >
                Remove
              </button>
            </div>
          ))}
        </div>

        <div className="actions-row">
          <button type="submit" className="primary-button" disabled={saving}>
            {saving ? 'Saving...' : 'Save Memory'}
          </button>
          {status ? <p className="status-text">{status}</p> : null}
        </div>
      </form>
    </section>
  )
}

export default MemoryForm
