import { useState, useEffect, useRef } from 'react'
import { QRCodeSVG } from 'qrcode.react'

const USER_ID = 'user_001'

// Auto-detect backend URL
const getBackendUrl = () => {
  const { protocol, hostname } = window.location
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:8000'
  }
  return `${protocol}//${hostname}:8000`.replace(':5173', ':8000').replace(':443', ':8000')
}

const BACKEND_URL = getBackendUrl()
const FRONTEND_URL = window.location.origin + '/voice'

console.log('Backend URL:', BACKEND_URL)
console.log('Frontend URL:', FRONTEND_URL)

const SpeechRecognition = typeof window !== 'undefined' 
  ? window.SpeechRecognition || window.webkitSpeechRecognition 
  : null

function VoiceInterface() {
  const [status, setStatus] = useState('idle')
  const [transcript, setTranscript] = useState('')
  const [aiReply, setAiReply] = useState('')
  const [error, setError] = useState('')
  const recognitionRef = useRef(null)
  const synthRef = useRef(null)
  const transcriptRef = useRef('')

  useEffect(() => {
    synthRef.current = window.speechSynthesis
    return () => {
      if (recognitionRef.current) recognitionRef.current.abort()
    }
  }, [])

  const startListening = () => {
    console.log('Starting to listen...')
    setError('')

    if (!SpeechRecognition) {
      setError('Speech recognition not supported. Use Chrome on Android.')
      return
    }

    if (window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
      setError('Requires HTTPS. Open via secure URL.')
      return
    }

    try {
      const recognition = new SpeechRecognition()
      recognition.lang = 'en-IN'
      recognition.continuous = false
      recognition.interimResults = true

      recognition.onstart = () => {
        setStatus('listening')
        setTranscript('')
      }

      recognition.onresult = (event) => {
        let text = ''
        for (let i = 0; i < event.results.length; i++) {
          text += event.results[i][0].transcript
        }
        setTranscript(text)
        transcriptRef.current = text
      }

      recognition.onerror = (event) => {
        console.error('Mic error:', event.error)
        if (event.error === 'not-allowed') {
          setError('Microphone denied. Tap Allow when prompted.')
        } else if (event.error === 'no-speech') {
          setError('No speech detected. Try again.')
        } else {
          setError('Error: ' + event.error)
        }
        setStatus('idle')
      }

      recognition.onend = () => {
        const text = transcriptRef.current.trim()
        if (text) {
          sendToBackend(text)
        } else {
          setStatus('idle')
        }
      }

      recognitionRef.current = recognition
      recognition.start()
    } catch (e) {
      setError('Could not start microphone')
    }
  }

  const sendToBackend = async (message) => {
    console.log('Sending:', message)
    setStatus('thinking')
    setError('')
    
    try {
      const response = await fetch(BACKEND_URL + '/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user_id: USER_ID, message })
      })
      
      if (!response.ok) {
        throw new Error('Server error: ' + response.status)
      }
      
      const data = await response.json()
      console.log('Response:', data)
      
      const reply = data.reply || 'No response'
      setAiReply(reply)
      setStatus('speaking')
      speakReply(reply)
    } catch (err) {
      console.error('Fetch error:', err)
      setError('Cannot connect to server: ' + err.message)
      setStatus('idle')
    }
  }

  const speakReply = (text) => {
    if (!synthRef.current) return
    
    synthRef.current.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'en-IN'
    utterance.rate = 0.9
    
    utterance.onend = () => setStatus('idle')
    synthRef.current.speak(utterance)
  }

  const stopAll = () => {
    if (recognitionRef.current) recognitionRef.current.abort()
    if (synthRef.current) synthRef.current.cancel()
    setStatus('idle')
  }

  return (
    <div style={{ padding: '20px', minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#f5f1ea', fontFamily: 'system-ui' }}>
      <h2 style={{ color: '#17324d', fontSize: '28px' }}>Voice Companion</h2>
      
      <div style={{ margin: '20px', color: '#666', fontSize: '18px', minHeight: '30px' }}>
        {status === 'idle' && 'Tap to speak'}
        {status === 'listening' && 'Listening...'}
        {status === 'thinking' && 'Thinking...'}
        {status === 'speaking' && 'Speaking...'}
      </div>

      {error && <div style={{ color: '#b91c1c', padding: '15px', background: '#fef2f2', borderRadius: '12px', margin: '10px', maxWidth: '320px', textAlign: 'center' }}>{error}</div>}

      {transcript && (
        <div style={{ padding: '20px', background: 'white', borderRadius: '16px', margin: '10px', maxWidth: '350px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
          <div style={{ color: '#666', fontSize: '14px' }}>You said:</div>
          <div style={{ fontSize: '22px', marginTop: '5px' }}>{transcript}</div>
        </div>
      )}

      {aiReply && (
        <div style={{ padding: '20px', background: '#17324d', borderRadius: '16px', margin: '10px', maxWidth: '350px', boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}>
          <div style={{ color: '#9ca3af', fontSize: '14px' }}>AI says:</div>
          <div style={{ fontSize: '22px', color: 'white', marginTop: '5px', lineHeight: '1.4' }}>{aiReply}</div>
        </div>
      )}

      <button onClick={status === 'idle' ? startListening : stopAll} style={{ width: '140px', height: '140px', borderRadius: '50%', border: 'none', background: status === 'listening' ? '#dc2626' : '#17324d', color: 'white', fontSize: '56px', cursor: 'pointer', marginTop: '20px', boxShadow: '0 8px 24px rgba(0,0,0,0.3)' }}>
        🎤
      </button>

      {status !== 'idle' && <button onClick={stopAll} style={{ marginTop: '20px', padding: '12px 40px', borderRadius: '25px', border: 'none', background: '#666', color: 'white', fontSize: '16px', cursor: 'pointer' }}>Stop</button>}
    </div>
  )
}

function QRPage() {
  return (
    <div style={{ padding: '40px', textAlign: 'center', fontFamily: 'system-ui' }}>
      <h1 style={{ color: '#17324d', fontSize: '32px', marginBottom: '20px' }}>Phone Access</h1>
      <p style={{ color: '#666', marginBottom: '30px' }}>Scan this QR code with your phone to open the voice interface.</p>
      <div style={{ padding: '20px', background: 'white', borderRadius: '16px', display: 'inline-block', boxShadow: '0 4px 20px rgba(0,0,0,0.1)' }}>
        <QRCodeSVG value={FRONTEND_URL} size={280} level="H" />
      </div>
      <p style={{ marginTop: '20px', color: '#888', fontSize: '14px', wordBreak: 'break-all', maxWidth: '400px', margin: '20px auto 0' }}>{FRONTEND_URL}</p>
      <div style={{ marginTop: '40px', padding: '20px', background: '#fff7ed', borderRadius: '12px', maxWidth: '400px', margin: '40px auto 0', textAlign: 'left' }}>
        <h3 style={{ color: '#c2410c', margin: '0 0 10px' }}>Important:</h3>
        <ul style={{ color: '#9a3412', fontSize: '14px', paddingLeft: '20px', lineHeight: '1.8', margin: 0 }}>
          <li>Use <strong>Chrome on Android</strong> (not Safari)</li>
          <li>Open via HTTPS URL</li>
          <li>Tap <strong>Allow</strong> when prompted for microphone</li>
        </ul>
      </div>
    </div>
  )
}

export { VoiceInterface, QRPage }