# Mithra Assistant - APK Distribution

## APK Location
```
mithra-assistant\app\build\outputs\apk\debug\app-debug.apk
```

## To Generate a Working QR Code:

### Option 1: Host on a web server
1. Upload `app-debug.apk` to any HTTP server (GitHub Pages, Firebase Hosting, ngrok, etc.)
2. Replace the URL in the QR generator:
```python
python -c "
import qrcode
qr = qrcode.QRCode(version=1, box_size=10, border=5)
qr.add_data('https://your-server.com/app-debug.apk')
qr.make(fit=True)
img = qr.make_image(fill_color='black', back_color='white')
img.save('qr-apk.png')
"
```

### Option 2: Quick local server (for testing)
```bash
cd "mithra-assistant\app\build\outputs\apk\debug"
python -m http.server 8080
```
Then generate QR pointing to `http://<your-ip>:8080/app-debug.apk`

### Option 3: ADB install (direct to device)
```bash
adb install "mithra-assistant\app\build\outputs\apk\debug\app-debug.apk"
```

## Install via QR
1. Host APK on a URL
2. Generate QR code pointing to that URL
3. Scan QR from Android phone
4. Download and install APK (enable "Install from unknown sources")

## Build Commands
```bash
# Rebuild APK
.\gradlew.bat assembleDebug

# Clean build
.\gradlew.bat clean assembleDebug
```

## Logcat Monitoring
```bash
adb logcat -s VOICE_BOOT VOICE_WAKE VOICE_SESSION VOICE_STT VOICE_TTS VOICE_MEMORY VOICE_VERIFY
```
