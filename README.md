<div align="center">

# 🎧 DIMEN SHON 8D

### *Your music, in an immersive 360° spatial dimension.*

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Size](https://img.shields.io/badge/App%20Size-1.86%20MB-blueviolet?style=flat-square)](dimen_shon.apk)
[![Engine](https://img.shields.io/badge/DSP-Binaural%208D-ff007f?style=flat-square)](#-spatial-8d-audio-engine)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

**Developed with ❤️ by No Coder Pro**

---

</div>

## 🌌 Overview

**DIMEN SHON 8D** is an ultra-lightweight, high-performance Android music player and real-time on-device spatial audio processor. It transforms any standard stereo track into an immersive **360° 8D Audio Experience** using real-time binaural panning, Interaural Time Difference (ITD), head-shadow acoustic filtering, and interactive visual feedback.

> 🎧 **Best experienced with headphones / earphones.**

---

## ✨ Key Features

### 🌀 Spatial 8D Audio Engine
* **360° Binaural Orbiting**: Continuous spatial panning revolving smoothly around the listener's head.
* **Acoustic ITD & Head Shadow**: Sub-millisecond acoustic delay filters for realistic spatial depth.
* **Custom DSP Controls**: Real-time adjustment for **Orbit Speed**, **Spatial Intensity**, and **Bass Boost**.
* **Radar Visualizer**: Real-time 2D orbital radar showing the exact position of the moving sound source.

### 🌐 Online Search, Stream & Download
* **Global Search**: Search millions of online songs, trending playlists, and artists.
* **Lossless Streaming**: High-speed audio streaming with intelligent memory caching.
* **Multi-Format Downloader**: Fast multi-threaded downloader with automatic ID3 tag & album art embedding.

### 🎛️ Modern Cyberpunk Player
* **Fast-Scroll Index Bar**: Instant alphabetical navigation through large local music libraries.
* **Floating Mini Player**: Seamless background playback with Android Media notification controls.
* **Smart Queue & Favorites**: Manage your active queue, track playback, and favorite songs with one tap.

---

## 💻 Build Without Android Studio (CLI)

You can build the complete production APK directly from the terminal without installing or opening Android Studio.

### 📋 Prerequisites
* **JDK 17+** installed and available in PATH.
* **Android SDK** (Command-line tools or SDK platform `android-34`).
* *Set your SDK path in `local.properties`:*
  ```properties
  # Windows
  sdk.dir=C\:/Users/<YourUsername>/AppData/Local/Android/Sdk

  # Linux / macOS
  sdk.dir=/home/<YourUsername>/Android/Sdk
  ```

---

### 🚀 Option 1: One-Click Build & Auto-Cleanup (Python)

Runs full compilation, R8 minification, copies the release APK to root as `dimen_shon.apk`, and cleans all temporary build cache:

```bash
python build.py
```

* Output APK: **`dimen_shon.apk`** *(~1.86 MB in root folder)*

---

### ⚡ Option 2: Build using Gradle Wrapper

#### **Windows (Command Prompt / PowerShell):**
```cmd
.\gradlew.bat assembleRelease
```

#### **Linux / macOS:**
```bash
chmod +x gradlew
./gradlew assembleRelease
```

* Output APK path: `app/build/outputs/apk/release/app-release.apk`

---

## 🛠️ Tech Stack

* **Platform:** Android SDK (Java 17 / API 26+)
* **Audio Engine:** AndroidX Media3 / ExoPlayer + Custom Binaural AudioProcessor
* **UI/UX:** Cyberpunk Dark Theme, Custom Radar Visualizer, Material Design

---

<div align="center">

Made with passion for spatial sound enthusiasts.  
⭐ **Star this repository if you like the project!**

</div>
