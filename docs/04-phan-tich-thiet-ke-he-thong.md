# Phân Tích Và Thiết Kế Hệ Thống

## 1. Phân Tích Hiện Trạng

Sinh viên thường quản lý việc học bằng nhiều công cụ khác nhau: lịch điện thoại, ghi chú, giấy, ứng dụng task hoặc nhắc nhở. Việc phân tán dữ liệu dẫn tới các vấn đề:

- Khó nhìn tổng quan lịch học, deadline và task trong một màn hình.
- Dễ tạo lịch trùng giờ.
- Khó biết hôm nay cần ưu tiên việc gì.
- Không đo được thời gian tập trung thực tế.
- Nhập lịch thủ công từ ảnh thời khóa biểu mất thời gian.

Study Planner giải quyết bằng một ứng dụng Android tập trung vào lịch học cá nhân, task, Pomodoro, thống kê và hỗ trợ nhập lịch từ ảnh.

## 2. Kiến Trúc Tổng Thể

```mermaid
flowchart TB
    subgraph Android["Android App"]
        UI["XML Screens + MainActivity"]
        Repo["Repository Layer"]
        Models["Model Layer"]
        SQLite["SQLiteOpenHelper"]
        Prefs["SharedPreferences"]
        GeminiClient["GeminiScheduleExtractor"]
        FirebaseClient["Firebase Auth / Google Sign-In"]
    end

    UI --> Repo
    UI --> GeminiClient
    UI --> FirebaseClient
    Repo --> Models
    Repo --> SQLite
    Repo --> Prefs
    GeminiClient --> Gemini["Gemini API"]
    UI --> OtpBackend["OTP Backend"]
    OtpBackend --> SMTP["SMTP Server"]
```

## 3. Thành Phần Chính

| Thành phần | File/Module | Vai trò |
| --- | --- | --- |
| MainActivity | `app/src/main/java/.../MainActivity.java` | Điều phối màn hình, sự kiện UI, xác thực, Pomodoro, CRUD |
| AuthRepository | `data/AuthRepository.java` | Quản lý tài khoản email, mật khẩu hash, OTP, session |
| StudyRepository | `data/StudyRepository.java` | Quản lý profile, task, event, settings, thống kê, Pomodoro |
| GeminiScheduleExtractor | `data/GeminiScheduleExtractor.java` | Gửi ảnh tới Gemini API và parse event |
| Model | `model/*.java` | Định nghĩa dữ liệu nghiệp vụ |
| Custom UI | `ui/*.java` | Biểu đồ, calendar view, swipe action, nền giấy |
| OTP Backend | `otp-backend` | HTTP server gửi OTP qua SMTP |

## 4. Thiết Kế Module Android

```mermaid
flowchart LR
    MainActivity --> AuthRepository
    MainActivity --> StudyRepository
    MainActivity --> GeminiScheduleExtractor
    MainActivity --> FirebaseAuth
    MainActivity --> GoogleSignInClient
    StudyRepository --> StudyEvent
    StudyRepository --> StudyTask
    StudyRepository --> UserProfile
    StudyRepository --> PomodoroSession
    AuthRepository --> AuthUser
```

## 5. Sơ Đồ Use Case

```mermaid
flowchart TB
    Student["Sinh viên"]
    Student --> UC1["Đăng ký tài khoản"]
    Student --> UC2["Xác thực OTP"]
    Student --> UC3["Đăng nhập email"]
    Student --> UC4["Đăng nhập Google"]
    Student --> UC5["Đặt lại mật khẩu"]
    Student --> UC6["Quản lý lịch học"]
    Student --> UC7["Tạo lịch từ ảnh"]
    Student --> UC8["Quản lý task"]
    Student --> UC9["Chạy Pomodoro"]
    Student --> UC10["Xem thống kê"]
    Student --> UC11["Cài đặt cá nhân"]

    UC2 --> Backend["OTP Backend"]
    UC4 --> Firebase["Firebase Auth"]
    UC7 --> Gemini["Gemini API"]
```

## 6. Luồng Xử Lý Chính

### 6.1 Đăng Ký Và Xác Thực OTP

```mermaid
sequenceDiagram
    actor User as Sinh viên
    participant App as Android App
    participant Auth as AuthRepository
    participant OTP as OTP Backend
    participant SMTP as SMTP Server

    User->>App: Nhập tên, email, mật khẩu
    App->>Auth: beginRegistration()
    Auth->>Auth: Lưu user chưa verified, tạo OTP hash
    Auth-->>App: Trả OTP plaintext
    App->>OTP: POST /send-otp
    OTP->>SMTP: Gửi email OTP
    SMTP-->>User: Email chứa OTP
    User->>App: Nhập OTP
    App->>Auth: verifyRegistrationOtp()
    Auth->>Auth: Kiểm tra hash, hạn dùng, số lần nhập
    Auth-->>App: User verified
    App->>App: Vào Dashboard
```

### 6.2 Đăng Nhập Google

```mermaid
sequenceDiagram
    actor User as Sinh viên
    participant App as Android App
    participant Google as Google Play Services
    participant Firebase as Firebase Auth

    User->>App: Bấm Đăng nhập với Google
    App->>Google: Mở Google Sign-In Intent
    Google-->>User: Chọn tài khoản
    Google-->>App: Trả GoogleSignInAccount hoặc lỗi
    App->>Firebase: signInWithCredential(idToken)
    Firebase-->>App: FirebaseUser
    App->>App: Tạo repository theo email, sync profile, vào Dashboard
```

Ghi chú: luồng này phụ thuộc SHA-1/OAuth trong Firebase Console. Nếu SHA-1 không khớp, Google Play Services trả lỗi ứng dụng chưa đăng ký OAuth2.

### 6.3 Tạo Lịch Từ Ảnh

```mermaid
sequenceDiagram
    actor User as Sinh viên
    participant App as Android App
    participant Gemini as Gemini API
    participant Repo as StudyRepository

    User->>App: Chọn camera hoặc thư viện
    App->>Gemini: Gửi ảnh base64 + prompt JSON schema
    Gemini-->>App: JSON danh sách events
    App->>App: Parse StudyEvent
    App->>User: Hiển thị danh sách lịch trích xuất
    User->>App: Xác nhận lưu
    App->>Repo: saveEvent()
```

### 6.4 Lưu Lịch Và Kiểm Tra Xung Đột

```mermaid
flowchart TD
    A["Người dùng nhập lịch"] --> B["Validate dữ liệu"]
    B --> C{"Thời gian hợp lệ?"}
    C -- "Không" --> D["Hiển thị lỗi"]
    C -- "Có" --> E["Kiểm tra xung đột"]
    E --> F{"Có trùng lịch?"}
    F -- "Có" --> G["Hiển thị cảnh báo xung đột"]
    G --> H{"Người dùng vẫn lưu?"}
    H -- "Không" --> I["Quay lại chỉnh sửa"]
    H -- "Có" --> J["Lưu event"]
    F -- "Không" --> J
```

## 7. Thiết Kế Giao Diện

| Màn hình | File layout | Chức năng |
| --- | --- | --- |
| Onboarding | `screen_onboarding.xml` | Giới thiệu app |
| Login | `screen_login.xml` | Đăng nhập email/Google, quên mật khẩu |
| Register | `screen_register.xml` | Đăng ký tài khoản |
| OTP | `screen_otp.xml` | Nhập/gửi lại OTP |
| Reset Password | `screen_reset_password.xml` | Đặt mật khẩu mới |
| Dashboard | `screen_dashboard.xml` | Tổng quan tiến độ, task, lịch, Pomodoro |
| Schedule | `screen_schedule.xml` | Quản lý lịch học |
| Tasks | `screen_tasks.xml` | Quản lý task |
| Pomodoro | `screen_pomodoro.xml` | Timer tập trung |
| Stats | `screen_stats.xml` | Thống kê học tập |
| Settings | `screen_settings.xml` | Hồ sơ, cá nhân hóa, đăng xuất |

## 8. Thiết Kế Lớp

```mermaid
classDiagram
    class AuthUser {
        String email
        String name
        boolean verified
        long createdAt
        long updatedAt
    }

    class UserProfile {
        String name
        String email
        String goal
    }

    class StudyTask {
        String id
        String title
        String subject
        long dueAt
        String priority
        boolean completed
        boolean important
        boolean urgent
        String tag
    }

    class StudyEvent {
        String id
        String title
        String type
        String subject
        long startAt
        long endAt
        String room
        boolean reminderEnabled
    }

    class PomodoroSession {
        String id
        String taskId
        String mode
        int durationMinutes
        int completedMinutes
        boolean isCompleted
    }

    class AuthRepository
    class StudyRepository

    AuthRepository --> AuthUser
    StudyRepository --> UserProfile
    StudyRepository --> StudyTask
    StudyRepository --> StudyEvent
    StudyRepository --> PomodoroSession
```

## 9. Thiết Kế API Backend OTP

### 9.1 `GET /health`

Kiểm tra backend còn chạy.

Response:

```json
{"ok": true}
```

### 9.2 `POST /send-otp`

Gửi OTP qua email.

Request:

```json
{
  "email": "student@example.com",
  "code": "123456",
  "purpose": "xác thực tài khoản"
}
```

Response thành công:

```json
{"ok": true}
```

Response lỗi:

```json
{
  "ok": false,
  "error": "Missing email or code"
}
```

## 10. Cấu Trúc Thư Mục

```text
MANAGING-YOUR-STUDY-SCHEDULE/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/example/cuoiky_qllichhoctap/
│       │   ├── MainActivity.java
│       │   ├── data/
│       │   ├── model/
│       │   ├── ui/
│       │   └── util/
│       └── res/
│           ├── layout/
│           ├── drawable/
│           ├── values/
│           └── raw/
├── otp-backend/
│   └── src/main/java/com/example/otpbackend/OtpMailServer.java
├── gradle/
├── docs/
└── README.md
```

## 11. Hạn Chế Thiết Kế Hiện Tại

- `MainActivity` đang xử lý nhiều trách nhiệm UI và nghiệp vụ, có thể tách ViewModel/Controller ở phiên bản sau.
- Google Sign-In đang dùng API legacy của `play-services-auth`; có thể nâng cấp Credential Manager.
- Đồng bộ Google Calendar mới có cấu hình UI, chưa có tích hợp Calendar API đầy đủ.
- Dữ liệu học tập lưu local, chưa có cloud backup/sync.
