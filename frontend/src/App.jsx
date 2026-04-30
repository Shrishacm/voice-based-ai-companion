import { NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { VoiceInterface, QRPage } from './VoiceInterface'
import Dashboard from './Dashboard'
import MemoryForm from './MemoryForm'

const USER_ID = 'user_001'

function App() {
  const location = useLocation()
  const isFamilyPage = location.pathname === '/' || location.pathname === '/edit'

  return (
    <div className="shell">
      {isFamilyPage && (
        <header className="hero">
          <div>
            <p className="eyebrow">Voice-Based AI Companion</p>
            <h1>Family Companion Portal</h1>
            <p className="hero-copy">Keep the companion updated with medications, family details, and appointments.</p>
          </div>
          <nav className="tabs">
            <NavLink to="/" end className={({ isActive }) => `tab ${isActive ? 'active' : ''}`}>Dashboard</NavLink>
            <NavLink to="/edit" className={({ isActive }) => `tab ${isActive ? 'active' : ''}`}>Edit Memory</NavLink>
          </nav>
        </header>
      )}

      <main>
        <Routes>
          <Route path="/" element={<Dashboard userId={USER_ID} />} />
          <Route path="/edit" element={<MemoryForm userId={USER_ID} />} />
          <Route path="/voice" element={<VoiceInterface />} />
          <Route path="/qr" element={<QRPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App