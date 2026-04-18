# 📖 Narrately — The Ethereal Library

> Turn any PDF or text file into an offline podcast. Listen to your documents anywhere, anytime.

**Created by Rahul**

---

## ✨ Features

| Feature | Description |
|---|---|
| 📄 **PDF & TXT Import** | Open any PDF or plain text file from your device |
| 🎧 **Text-to-Speech Playback** | Native Android TTS reads your documents aloud |
| ⏯ **Full Player Controls** | Play, Pause, Rewind 15s, Forward 15s |
| 🏎 **Speed & Pitch Control** | Adjust playback speed (0.5x - 2.0x) and voice pitch |
| 📚 **Persistent Library** | All imported books stay in your library permanently |
| 🔖 **Progress Memory** | App remembers exactly where you stopped reading |
| ✨ **Karaoke Highlighting** | Currently spoken text is highlighted in real time |
| 🔒 **100% Offline** | No internet required — everything runs on device |
| 🔔 **Background Playback** | Keeps reading when screen is off or in another app |
| 🎨 **Premium Dark UI** | Glassmorphism design inspired by Spotify + Apple Books |

---

## 📱 Screenshots

The app features a premium dark glassmorphism UI with:
- **Splash Screen** — Animated "NARRATELY" logo with purple glow
- **Library / Bookshelf** — Recently read carousel + full library list
- **Player / Now Playing** — Karaoke text highlighting + podcast-style controls

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVVM (ViewModel + StateFlow)
- **DI:** Hilt (Dagger)
- **Database:** Room (local SQLite)
- **PDF Parsing:** PdfBox-Android
- **Audio Session:** Media3 MediaSession (lock-screen controls)
- **TTS:** Native Android TextToSpeech API
- **CI/CD:** GitHub Actions (auto-builds APK on push)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Min SDK: Android 7.0 (API 24)

### Build Locally
```bash
git clone https://github.com/rahulkumargit1/narrately.git
cd narrately
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions (Auto-Build)
Push to `main` branch → GitHub Actions auto-builds → Download APK from the **Actions** tab → **Artifacts** section.

---

## 📂 Project Structure

```
app/src/main/java/com/example/pdfreader/
├── MainActivity.kt          # Entry point, navigation, screen transitions
├── ReaderApplication.kt     # App init (Hilt + PDFBox)
├── data/
│   ├── Entities.kt          # Room entities (DocumentEntity, ProgressEntity)
│   ├── ReaderDao.kt         # Database queries
│   └── ReaderDatabase.kt    # Room database instance
├── di/
│   └── DatabaseModule.kt    # Hilt dependency injection
├── parser/
│   └── DocumentParser.kt    # PDF & TXT extraction + chunking
├── service/
│   └── MediaLibraryService.kt  # Background audio foreground service
├── tts/
│   └── TTSManager.kt        # TTS engine wrapper (play/pause/speed/pitch)
└── ui/
    ├── ReaderViewModel.kt   # Main ViewModel (all app logic)
    ├── theme/
    │   ├── Color.kt         # Full color palette from Stitch design
    │   └── Theme.kt         # Material 3 dark theme
    └── screens/
        ├── SplashScreen.kt  # Animated splash with "Created by Rahul"
        ├── LibraryScreen.kt # Home bookshelf with carousel + list
        └── PlayerScreen.kt  # Now Playing with karaoke + controls
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

> Built with ❤️ by **Rahul**
