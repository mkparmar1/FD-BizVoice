# BizVoice — Enterprise VoIP & Business Calling Android Application

**BizVoice** is a modern, enterprise-grade Voice over IP (VoIP) and business telecommunications Android application built with **Kotlin** and **Jetpack Compose (Material 3)**. It integrates seamlessly with custom **Laravel REST API backends** and **Twilio Voice SDK** for crystal-clear outbound and inbound voice calls, complete with contacts synchronization, live call management, detailed call logs, audio device routing, and user presence management.

---

## 📱 Key Features

### 1. 📞 Smart Dialpad & VoIP Calling
- **Interactive DTMF Keypad**: Standard numeric keypad with acoustic DTMF tone generation, haptic feedback, and continuous backspace support.
- **Dynamic Assigned Number Display**: Visual badge displaying the active company phone line / caller ID assigned to the logged-in agent.
- **Live In-Call Controls**:
  - **Mute / Unmute**: Toggle microphone input on the fly.
  - **Speakerphone / Earpiece / Bluetooth**: Dynamic audio routing using Android `AudioManager`.
  - **Hold / Resume**: Keep calls on hold with visual indicator state.
  - **In-Call Keypad**: Send DTMF tones during interactive IVR systems.
  - **Call Duration Timer**: Live ticker tracking call duration with formatted display.
- **Incoming Call Handling**: Dedicated full-screen / pop-up incoming call alert with Accept and Decline actions.

### 2. 📋 Comprehensive Call History (Recents)
- **Call Filtering**: Filter by **All**, **Missed**, **Incoming**, and **Outgoing** calls.
- **Call Search**: Instant search by contact name or phone number.
- **Rich Call Detail View**:
  - Caller ID, timestamp, duration, status (Completed, Missed, Busy, Failed).
  - One-tap callback and direct contact save/view shortcuts.
  - Call recording playback simulator with visual waveform and progress controls.
  - AI call summary & transcription preview.
  - Notes logging for CRM records.

### 3. 👥 Contacts Directory
- **Enterprise & Personal Contacts**: Unified address book with quick alpha sorting.
- **Instant Search & Filter**: Filter contacts by **All**, **Favorites**, and **Company**.
- **Contact Management**: Full CRUD support (Add, View, Edit, and Delete contacts).
- **Direct Calling**: One-tap calling directly from contact cards.

### 4. 👤 Profile & Presence Management
- **Presence Status Switcher**: Real-time status toggle (**Available / Active**, **Busy**, **Away**).
- **Assigned Line Information**: Shows the agent's assigned VoIP direct inward dialing (DID) number.
- **Profile Details Editor**: Manage full name, work email, job title, and organization/company.
- **Secure Log Out**: Unregisters device push tokens, clears cached sessions, and returns to authentication.

### 5. ⚙️ Enterprise Settings & Configuration
- **Live Laravel Server Switcher**: Seamlessly toggle between **Live Laravel API** and the offline **Sandbox Demo Mode**.
- **Custom API Endpoint**: Configure custom backend URLs (e.g., `https://api.yourcompany.com/api/v1`).
- **Audio Preferences**: Default to speakerphone, DTMF tone toggles, and microphone gain preferences.
- **Permissions Dashboard**: Visual audit of required runtime permissions (`RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `POST_NOTIFICATIONS`, etc.).

---

## 🏗️ Technical Architecture

BizVoice is architected according to modern Android MVVM clean architecture principles:

```
app/src/main/java/com/example/
├── data/
│   ├── local/                     # SharedPreferences & Room local storage
│   │   └── SessionManager.kt      # Auth token, user credentials, settings
│   ├── model/                     # Moshi JSON data models
│   │   └── Models.kt              # User, CallLog, Contact, ApiResponses
│   ├── remote/                    # Networking layer (Retrofit / OkHttp)
│   │   ├── ApiClient.kt           # Dynamic baseUrl client with AuthInterceptor
│   │   ├── LaravelApiService.kt   # Retrofit endpoint interfaces
│   │   └── MockLaravelBackend.kt  # In-memory sandbox backend for offline testing
│   └── repository/                # Single source of truth repository
│       └── BizVoiceRepository.kt  # Coordinates remote API, local cache, and state
├── telephony/                     # Audio, VoIP & Call state management
│   ├── AudioDeviceManager.kt      # Speaker, earpiece, and audio mode control
│   └── CallManager.kt             # Twilio Voice client wrapper & CallState flow
├── ui/                            # Jetpack Compose UI
│   ├── components/                # Reusable UI widgets & design system
│   │   └── BizVoiceComponents.kt  # MainTabHeader, BizAvatar, NumberBanner, etc.
│   ├── screens/                   # Modular feature screens
│   │   ├── auth/                  # LoginScreen & ForgotPasswordScreen
│   │   ├── calls/                 # ActiveCallScreen & IncomingCallScreen
│   │   ├── contacts/              # ContactsScreen, ContactDetail, AddEditContact
│   │   ├── dialer/                # DialerScreen (Keypad)
│   │   ├── main/                  # MainContainerScreen (Bottom navigation)
│   │   ├── profile/               # ProfileScreen & Editor
│   │   ├── recents/               # RecentsScreen & CallDetailScreen
│   │   ├── settings/              # SettingsScreen, BackendConfig, Permissions, About
│   │   └── splash/                # SplashScreen & Session initialization
│   └── theme/                     # Material 3 Color Schemes, Typography, Shapes
└── MainActivity.kt                # Jetpack Navigation host & lifecycle handler
```

---

## 🔌 Laravel Backend Integration

BizVoice is engineered to interface directly with standard Laravel RESTful APIs utilizing **Laravel Sanctum** or **Passport** for Bearer token authentication.

### Key API Endpoints

| Method | Endpoint | Description | Request Body / Params |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/login` | Authenticate user | `{"email": "...", "password": "..."}` |
| `POST` | `/api/logout` | Revoke user token | `Header: Bearer <token>` |
| `GET` | `/api/me` | Fetch authenticated user | `Header: Bearer <token>` |
| `PUT` | `/api/me` | Update user profile | `{"name": "...", "email": "...", ...}` |
| `GET` | `/api/phone-number` | Get assigned VoIP phone number | `Header: Bearer <token>` |
| `GET` | `/api/twilio/token` | Retrieve Twilio Voice Access Token | `Header: Bearer <token>` |
| `GET` | `/api/calls` | Fetch call history records | `Header: Bearer <token>` |
| `POST` | `/api/calls` | Log a new call | `{"to": "...", "direction": "...", ...}` |
| `PUT` | `/api/calls/{id}` | Update call status/notes | `{"notes": "...", "status": "..."}` |
| `GET` | `/api/contacts` | Fetch contact list | `Header: Bearer <token>` |
| `POST` | `/api/contacts` | Create a new contact | `{"name": "...", "phone": "...", ...}` |
| `PUT` | `/api/contacts/{id}` | Update contact | `{"name": "...", "phone": "...", ...}` |
| `DELETE` | `/api/contacts/{id}` | Delete contact | `Header: Bearer <token>` |
| `POST` | `/api/push-token` | Register Firebase FCM device token | `{"token": "..."}` |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Ladybug (2024.2+) or later
- **JDK 17** or **JDK 21**
- **Android SDK**: `minSdk: 26` (Android 8.0 Oreo), `targetSdk: 35` (Android 15)

### Build & Run
1. Open the project in Android Studio or use the command line:
   ```bash
   ./gradlew assembleDebug
   ```
2. Deploy to a connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```
3. To run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Default Sandbox Credentials
When operating in **Sandbox Demo Mode**, you can sign in using pre-configured test profiles:
- **Admin**: `alex.mitchell@bizvoice.io` / `businesspass123`
- **Agent**: `sarah.j@techflow.com` / `businesspass123`

---

## 🛡️ Permissions & Security

BizVoice follows strict Android security and permission best practices:
- **`RECORD_AUDIO`**: Captures microphone input during active phone calls.
- **`MODIFY_AUDIO_SETTINGS`**: Adjusts in-call audio stream volume, earpiece, and speakerphone modes.
- **`POST_NOTIFICATIONS`**: Alerts user of incoming phone calls and background call status.
- **`READ_PHONE_STATE` & `MANAGE_OWN_CALLS`**: Manages telecom audio focus with the Android OS.
- **`INTERNET` & `ACCESS_NETWORK_STATE`**: Secure TLS communications with Laravel and VoIP gateways.

---

## 🎨 UI/UX Design System
- Built on **Material 3 (Material You)** dynamic color system.
- Standardized 8dp grid spacing with uniform `MainTabHeader` navigation bars across all views.
- High-contrast typography and accessibility-compliant touch targets (minimum 48dp).
- Fully responsive across phones, foldables, and tablet screen sizes.
