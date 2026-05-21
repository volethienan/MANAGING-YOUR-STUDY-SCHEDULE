# README Mã Nguồn

## 1. Tổng Quan

Study Planner là ứng dụng Android Java/XML hỗ trợ quản lý lịch học, deadline, task, Pomodoro, thống kê học tập, đăng ký OTP qua email, đăng nhập Google và nhập lịch từ ảnh bằng Gemini API.

Repo gồm hai phần chính:

- `app`: ứng dụng Android.
- `otp-backend`: backend Java nhỏ dùng để gửi OTP qua SMTP.

## 2. Công Nghệ Sử Dụng

| Nhóm | Công nghệ |
| --- | --- |
| Mobile | Android Java, XML Layout |
| UI | AppCompat, Material Components, ConstraintLayout |
| Local database | SQLiteOpenHelper |
| Local state | SharedPreferences |
| Authentication | Firebase Auth, Google Sign-In, email/password local |
| AI | Gemini API |
| Backend OTP | Java HTTP Server |
| Email | Jakarta Mail, SMTP |
| Build | Gradle Kotlin DSL |
| Test | JUnit, AndroidX Test, Espresso |

## 3. Cấu Trúc Thư Mục

```text
MANAGING-YOUR-STUDY-SCHEDULE/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/cuoiky_qllichhoctap/
│       │   │   ├── MainActivity.java
│       │   │   ├── data/
│       │   │   │   ├── AuthRepository.java
│       │   │   │   ├── GeminiScheduleExtractor.java
│       │   │   │   └── StudyRepository.java
│       │   │   ├── model/
│       │   │   ├── ui/
│       │   │   └── util/
│       │   └── res/
│       │       ├── layout/
│       │       ├── drawable/
│       │       ├── values/
│       │       └── raw/
│       ├── test/
│       └── androidTest/
├── otp-backend/
│   ├── build.gradle.kts
│   └── src/main/java/com/example/otpbackend/OtpMailServer.java
├── docs/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 4. Module Android `app`

### 4.1 `MainActivity.java`

Điều phối hầu hết UI và nghiệp vụ:

- Onboarding.
- Login/register/OTP/reset password.
- Google Sign-In.
- Dashboard.
- Schedule.
- Tasks.
- Pomodoro.
- Stats.
- Settings.
- Import ảnh.

### 4.2 Package `data`

| File | Vai trò |
| --- | --- |
| `AuthRepository.java` | Quản lý user local, mật khẩu hash, OTP hash, session |
| `StudyRepository.java` | Quản lý profile, task, event, settings, Pomodoro, thống kê |
| `GeminiScheduleExtractor.java` | Gửi ảnh tới Gemini và parse JSON event |

### 4.3 Package `model`

| File | Vai trò |
| --- | --- |
| `AuthUser.java` | Dữ liệu tài khoản email local |
| `UserProfile.java` | Hồ sơ người dùng |
| `StudyTask.java` | Task học tập |
| `StudyEvent.java` | Lịch học/lịch thi/deadline/cá nhân |
| `PomodoroSession.java` | Phiên Pomodoro |

### 4.4 Package `ui`

| File | Vai trò |
| --- | --- |
| `StudyPaperLayout.java` | Nền giao diện dạng giấy học tập |
| `WeekCalendarView.java` | Lịch tuần/ngày |
| `SwipeActionLayout.java` | Hành động vuốt |
| `DonutChartView.java` | Biểu đồ tròn |
| `ComparisonBarChartView.java` | Biểu đồ cột so sánh |

## 5. Module `otp-backend`

Backend Java đơn giản dùng `com.sun.net.httpserver.HttpServer`.

Endpoint:

| Method | Path | Mô tả |
| --- | --- | --- |
| GET | `/health` | Kiểm tra backend còn chạy |
| POST | `/send-otp` | Gửi OTP qua email |

Biến môi trường cần có:

| Biến | Mô tả |
| --- | --- |
| `SMTP_HOST` | SMTP host, ví dụ `smtp.gmail.com` |
| `SMTP_PORT` | SMTP port, ví dụ `587` |
| `SMTP_USERNAME` | Tài khoản email gửi |
| `SMTP_PASSWORD` | App Password |
| `SMTP_FROM` | Email người gửi |
| `SMTP_STARTTLS` | Bật STARTTLS, mặc định true |
| `OTP_BACKEND_PORT` | Port backend, mặc định 8080 |

## 6. Cài Đặt Nhanh

### 6.1 Cấu Hình Firebase

Đặt file:

```text
app/google-services.json
```

Package name phải là:

```text
com.example.cuoiky_qllichhoctap
```

Thêm SHA-1 debug vào Firebase nếu dùng Google Sign-In.

### 6.2 Cấu Hình `local.properties`

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
OTP_BACKEND_URL=http://10.0.2.2:8080
```

### 6.3 Build App

```powershell
.\gradlew.bat :app:assembleDebug
```

### 6.4 Cài App

```powershell
.\gradlew.bat :app:installDebug
```

### 6.5 Chạy Backend OTP

```powershell
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your-email@gmail.com"
$env:SMTP_PASSWORD="your-app-password"
$env:SMTP_FROM="your-email@gmail.com"
$env:OTP_BACKEND_PORT="8080"

.\gradlew.bat -p otp-backend run
```

## 7. Lệnh Phát Triển Thường Dùng

Build toàn bộ:

```powershell
.\gradlew.bat build
```

Build Android debug:

```powershell
.\gradlew.bat :app:assembleDebug
```

Cài Android debug:

```powershell
.\gradlew.bat :app:installDebug
```

Chạy unit test:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Chạy Android test:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Chạy OTP backend:

```powershell
.\gradlew.bat -p otp-backend run
```

## 8. Quy Ước Code

- Java source dùng package theo `com.example.cuoiky_qllichhoctap`.
- Model chỉ giữ dữ liệu và chuyển đổi JSON khi cần.
- Repository chịu trách nhiệm đọc/ghi dữ liệu.
- Không hardcode API key, SMTP password hoặc thông tin bí mật.
- Không commit `local.properties`, `.env`, keystore, file cấu hình riêng tư.
- Với tính năng mới, cập nhật tài liệu trong `docs` và test case tương ứng.

## 9. Trạng Thái Kiểm Thử Hiện Tại

- Build debug đã chạy thành công.
- Cài debug lên emulator đã chạy thành công.
- Đã xác định lỗi Google Sign-In khi SHA-1/OAuth chưa khớp.
- Code đã cập nhật để thông báo Google Sign-In không hoàn tất rõ hơn.

## 10. Hướng Bảo Trì Và Mở Rộng

| Hạng mục | Đề xuất |
| --- | --- |
| Kiến trúc Android | Tách `MainActivity` thành nhiều Activity/Fragment hoặc MVVM |
| Database | Chuyển SQLiteOpenHelper sang Room |
| Google Sign-In | Nâng cấp từ legacy Google Sign-In sang Credential Manager |
| Cloud sync | Thêm Firestore hoặc backend riêng |
| OTP backend | Thêm rate limit, logging, template email HTML |
| Test | Bổ sung unit test cho repository và instrumented test cho UI |
| Release | Thêm signing config release và quy trình phát hành |

## 11. Liên Kết Tài Liệu Trong Repo

| Tài liệu | File |
| --- | --- |
| Đề cương dự án | `docs/01-de-cuong-du-an.md` |
| Kế hoạch dự án | `docs/02-ke-hoach-du-an.md` |
| SRS | `docs/03-srs-dac-ta-yeu-cau.md` |
| Phân tích thiết kế | `docs/04-phan-tich-thiet-ke-he-thong.md` |
| Thiết kế dữ liệu | `docs/05-thiet-ke-du-lieu.md` |
| Kiểm thử | `docs/06-tai-lieu-kiem-thu.md` |
| Triển khai | `docs/07-huong-dan-trien-khai.md` |
| Hướng dẫn sử dụng | `docs/08-huong-dan-su-dung.md` |
| README mã nguồn | `docs/09-readme-ma-nguon.md` |
