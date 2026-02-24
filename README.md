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

## 🗂 Database

- **Database Name:** `attendance.db`
- **Storage Location:** Internal app storage (private sandbox)
- /data/data/sg.edu.sit.attendance/databases/attendance.db
- The database is local-only and removed when the app is uninstalled.

---

## 🛠 Tech Stack

| Technology | Purpose |
|------------|----------|
| Kotlin | Primary programming language |
| Jetpack Compose | Declarative UI framework |
| Room (SQLite) | Local persistence layer |
| Kotlin Coroutines | Asynchronous programming |
| StateFlow / Flow | Reactive state management |
| CameraX | Camera preview & capture |
| ML Kit (Barcode) | QR code detection |
| Coil | Image loading |
| WorkManager (optional) | Background sync tasks |

---

## 🔒 Security Design

- Passwords hashed using **PBKDF2WithHmacSHA256**
- Unique salt generated per account
- No plaintext password storage
- Local session stored securely in SharedPreferences

---

## 📶 Offline-First Design

The app is designed to function without internet connectivity:

- Sessions stored locally
- Attendance saved immediately
- Leave requests persisted locally
- Authentication handled locally

Future extension: remote synchronization via Firebase or backend API.

---

## 🧪 Attendance Verification Flow

1. Validate geofence proximity  
2. Validate QR session payload or PIN  
3. Capture selfie photo proof  
4. Persist attendance record in Room  
5. Reactive UI update  

---

## 📌 Future Improvements

- Remote sync integration
- Professor analytics dashboard
- Biometric authentication
- Cloud backup
- Role-based access control

---

## 📜 License

For academic and educational use. Created by students from Singapore Institute of Technology (SIT). All Rights Reserved 2026.

