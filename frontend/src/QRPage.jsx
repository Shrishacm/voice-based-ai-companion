import { QRCodeSVG } from 'qrcode.react'

const FRONTEND_URL = 'https://yummy-ghosts-invent.loca.lt/voice'

function QRPage() {
  return (
    <div style={{ padding: '40px', textAlign: 'center', fontFamily: 'system-ui' }}>
      <h1 style={{ color: '#17324d', fontSize: '32px', marginBottom: '20px' }}>Phone Access</h1>
      
      <p style={{ color: '#666', marginBottom: '30px', maxWidth: '400px', margin: '0 auto 30px' }}>
        Scan this QR code with your phone to open the voice interface.
      </p>
      
      <div style={{ padding: '20px', background: 'white', borderRadius: '16px', display: 'inline-block', boxShadow: '0 4px 20px rgba(0,0,0,0.1)' }}>
        <QRCodeSVG value={FRONTEND_URL} size={280} level="H" />
      </div>
      
      <p style={{ marginTop: '20px', color: '#888', fontSize: '14px', wordBreak: 'break-all', maxWidth: '400px', margin: '20px auto 0' }}>
        {FRONTEND_URL}
      </p>

      <div style={{ marginTop: '40px', padding: '20px', background: '#fff7ed', borderRadius: '12px', maxWidth: '400px', margin: '40px auto 0', textAlign: 'left' }}>
        <h3 style={{ color: '#c2410c', margin: '0 0 10px' }}>Important:</h3>
        <ul style={{ color: '#9a3412', fontSize: '14px', paddingLeft: '20px', lineHeight: '1.8', margin: 0 }}>
          <li>Use <strong>Chrome on Android</strong> (not Safari)</li>
          <li>Tap the QR or open the URL directly</li>
          <li>Tap <strong>Allow</strong> when asked for microphone</li>
          <li>Speak clearly to the phone</li>
        </ul>
      </div>
    </div>
  )
}

export default QRPage