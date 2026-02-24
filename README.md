# 📱 DigiCheck – Smart Attendance Verification App

DigiCheck is an Android-based smart attendance system built using **Kotlin and Jetpack Compose**.  
The application verifies student attendance using **QR session validation, geofence proximity checks, selfie photo proof, and local authentication**, following an **offline-first architecture** powered by Room database.

---

## 🚀 Features

### 🔐 Local Authentication (Offline)
- Create account with username + password
- Secure password hashing using **PBKDF2 (salted)**
- Session persistence using SharedPreferences
- Fully local authentication (no Firebase dependency)

---

### 📷 QR Session Scanning
- Real-time QR detection using **ML Kit (Barcode Scanning)**
- Camera preview powered by **CameraX**
- Session payload validation before check-in

---

### 📍 Location Geofence Validation
- Haversine-based distance calculation
- Configurable radius per session
- Ensures student is physically present at session location

---

### 🤳 Camera Proof Capture
- Front-camera selfie capture using **CameraX**
- Stored locally with timestamp metadata
- Preview integration using **Coil**

---

### 🗄 Local Room Database (Offline-First)
- SQLite persistence using **Room**
- Entities:
  - `UserEntity`
  - `GroupEntity`
  - `SessionEntity`
  - `AttendanceEntity`
  - `LeaveRequestEntity`
  - `LocalAccountEntity`
- Reactive `Flow` queries
- Sync status tracking (`PENDING`, `SYNCED`, `FAILED`)
- Thread-safe singleton database initialization

---

### ⚡ Reactive UI & Concurrency
- Kotlin Coroutines for non-blocking operations
- Flow → StateFlow → Compose reactive updates
- UI automatically recomposes on database changes

---

## 🏗 Architecture

The application follows the **Model–View–ViewModel (MVVM)** pattern.

### Layer Overview

| Layer | Responsibility |
|-------|---------------|
| UI (Compose) | Renders screens and collects user input |
| ViewModel | Holds UI state and coordinates logic |
| Repository | Business rules and data orchestration |
| Data Layer (Room) | Local persistence and structured queries |
| Infrastructure | CameraX, ML Kit, Location Services |

### Data Flow
