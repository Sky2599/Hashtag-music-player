# Hashtag Music Player 🎵💎

An audiophile-grade, offline music player for Android built entirely with **Kotlin** and **Jetpack Compose**. Featuring Material Design 3, dynamic Monet/Palette theming, and a **Liquid Glass** theme with interactive specular reflections and fluid animations.

---

## ✨ Features

- **💎 Liquid Glass Design Language**:
  - Translucent glassmorphism with dynamic ambient light and refraction borders.
  - Smooth-moving reflective navigation pill across bottom tabs and settings categories.
  - Interactive specular sheen and reflection on button/card tap.
  - Dynamic palette background engine adapting subtly to the current album artwork.

- **🎚️ 10-Band Equalizer & Audiophile DSP**:
  - 10-band parametric EQ with audiophile presets (Rock, Pop, Jazz, Electronic, Bass Boost, Vocal, Acoustic).
  - Bass boost, 3D audio spatial virtualizer, and multi-environment reverb effects.
  - High-precision audio engine with bit-perfect sample rate & bit-depth badges (FLAC, ALAC, WAV, AAC, MP3).

- **🖼️ High-Resolution Artwork & Tag Parsing**:
  - Multi-tier embedded ID3/FLAC master artwork extraction with LruCache.
  - Automatic folder artwork resolution with generic folder pollution guard.
  - Fast, non-blocking MediaStore indexing.

- **📜 Synchronized Real-Time Lyrics**:
  - Synchronized time-stamped lyrics viewer with instant seek-to-lyric line support.

- **📊 Sonic DNA & Listening Analytics**:
  - Interactive listening dashboard showing top artists, top albums, listening habits, and undiscovered gems.

- **🎛️ Audiophile Standby Mode**:
  - Full-screen landscape desk display mode with dynamic audio spectrum visualizers and queue management.

- **📱 Native Home Screen Widgets**:
  - Audiophile full player widget, compact player widget, and quick queue widget.

---

## 📥 Downloading the APK

### Via GitHub Actions (Continuous Integration)
Every commit to `main` automatically builds a fresh debug APK.
1. Navigate to the **[Actions](../../actions)** tab of this repository.
2. Click on the latest workflow run (**Build Android APK**).
3. Scroll down to the **Artifacts** section at the bottom.
4. Download the **`Hashtag-Music-Player-debug`** zip archive, extract it, and install the `.apk` on your Android device!

### Via Releases
When a version tag (e.g. `v1.0.0`) is published, the APK is automatically attached to the **[Releases](../../releases)** page.

---

## 🛠️ Building From Source

### Prerequisites
- **JDK 21** or higher
- **Android SDK** (API Level 36, minSdk 24)
- **Git**

### Clone & Build
```bash
# Clone repository
git clone https://github.com/your-username/hashtag-music-player.git
cd hashtag-music-player

# Build Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

To install directly to a connected Android device or emulator:
```bash
./gradlew installDebug
```

---

## 🏗️ Architecture & Tech Stack

- **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Audio Engine**: Android Media3 / ExoPlayer / AudioTrack / Android Equalizer DSP
- **Persistence**: Room Database & DataStore Preferences
- **Image Loading**: Coil 2.x
- **Palette**: AndroidX Palette for dynamic palette extraction
- **Testing**: Robolectric & Compose UI Testing with Roborazzi
- **CI/CD**: GitHub Actions

---

## 📄 License
This project is open-source under the Apache License 2.0.
