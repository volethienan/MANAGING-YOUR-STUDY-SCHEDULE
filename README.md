# Managing Your Study Schedule - Study Planner

Ứng dụng Android hỗ trợ sinh viên quản lý lịch học, lịch thi, deadline, task, Pomodoro và tiến độ học tập. Dự án gồm app Android, OTP backend gửi email, admin web quản trị và đồng bộ dữ liệu học tập theo tài khoản lên Firebase Realtime Database.

## Tổng quan

Study Planner tập trung vào nhu cầu học tập hằng ngày của sinh viên:

- Xem tổng quan lịch học, task, deadline và thời gian tập trung.
- Tạo, sửa, xóa lịch học, lịch thi, deadline và sự kiện cá nhân.
- Quản lý task theo ngày, trạng thái, mức ưu tiên, tag và ma trận quan trọng/khẩn cấp.
- Tạo lịch từ ảnh thời khóa biểu bằng Gemini API.
- Chạy Pomodoro, lưu lịch sử phiên học và thống kê thời gian tập trung.
- Nhắc nhở lịch/task bằng notification local.
- Đăng nhập Google bằng Firebase Authentication.
- Lưu dữ liệu local bằng SQLite và đồng bộ dữ liệu theo tài khoản lên Firebase Realtime Database.
- Có web admin để quản lý tài khoản, khóa/mở khóa user, thông báo, thống kê học tập và lỗi OTP/AI.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| App mobile | Android Java/XML |
| UI | XML layout, Material Components, custom View |
| Local database | SQLiteOpenHelper |
| Local preferences | SharedPreferences |
| Đăng nhập Google | Firebase Authentication + Google Sign-In |
| Cloud sync | Firebase Realtime Database |
| Đọc lịch từ ảnh | Gemini API |
| OTP backend | Java HTTP Server + Jakarta Mail |
| Admin web | Java HTTP Server + HTML/CSS/JavaScript |
| Build | Gradle Kotlin DSL |

## Cấu trúc thư mục

```text
MANAGING-YOUR-STUDY-SCHEDULE/
├── app/                         # Ứng dụng Android chính
│   ├── google-services.json     # Cấu hình Firebase Android
│   ├── build.gradle.kts         # Dependency Android/Firebase/Gemini/backend URL
│   └── src/main/
│       ├── AndroidManifest.xml  # Activity, permission, receiver, FileProvider
│       ├── java/com/example/cuoiky_qllichhoctap/
│       │   ├── MainActivity.java
│       │   ├── EventReminderReceiver.java
│       │   ├── data/
│       │   ├── model/
│       │   ├── ui/
│       │   └── util/
│       └── res/
│           ├── layout/
│           ├── drawable/
│           ├── values/
│           ├── raw/
│           └── xml/
├── admin-web/                   # Web quản trị chạy port 8090
├── otp-backend/                 # Backend gửi OTP chạy port 8080
├── docs/                        # Tài liệu phân tích, thiết kế, báo cáo
├── build.gradle.kts             # Cấu hình Gradle cấp root
├── settings.gradle.kts          # Khai báo module
├── local.properties             # Cấu hình local, không commit
├── .env                         # Cấu hình backend local, không commit nếu chứa secret
└── README.md
```

## Vai trò các file Android chính

| File/thư mục | Vai trò |
| --- | --- |
| `MainActivity.java` | Điều phối màn hình, xử lý đăng nhập, lịch, task, countdown, Pomodoro, thống kê, reminder và gọi backend |
| `EventReminderReceiver.java` | Nhận alarm và hiển thị notification nhắc lịch/task |
| `data/AuthRepository.java` | Quản lý tài khoản email/password local, password hash, OTP hash, session |
| `data/StudyRepository.java` | Quản lý dữ liệu học tập local bằng SQLite và gọi đồng bộ Firebase |
| `data/FirebaseStudyStore.java` | Đồng bộ profile, tasks, events, countdowns, Pomodoro, settings lên Firebase Realtime Database |
| `data/AdminPortalClient.java` | Gọi admin backend để sync user, gửi snapshot học tập, nhận thông báo, báo lỗi OTP/AI |
| `data/GeminiScheduleExtractor.java` | Gửi ảnh lên Gemini API và parse kết quả thành lịch học |
| `model/StudyTask.java` | Model task/deadline học tập |
| `model/StudyEvent.java` | Model lịch học/lịch thi/deadline/sự kiện cá nhân |
| `model/CountdownMilestone.java` | Model mốc đếm ngược riêng |
| `model/PomodoroSession.java` | Model phiên Pomodoro |
| `ui/WeekCalendarView.java` | Custom View vẽ lịch ngày/3 ngày/tuần |
| `ui/SwipeActionLayout.java` | Layout hỗ trợ vuốt task để hiện hành động |
| `ui/DonutChartView.java` | Biểu đồ donut thống kê |
| `ui/ComparisonBarChartView.java` | Biểu đồ cột so sánh |
| `util/DateTimeUtils.java` | Hàm tiện ích xử lý ngày giờ |

## Tính năng chính

### Xác thực và tài khoản

- Đăng ký bằng email/password.
- Gửi OTP qua email bằng `otp-backend`.
- Xác thực OTP đăng ký.
- Đặt lại mật khẩu bằng OTP.
- Đăng nhập Google qua Firebase Authentication.
- Sync registry tài khoản với admin web.
- Admin có thể khóa/mở khóa tài khoản hoặc yêu cầu user reset mật khẩu.

### Lịch học

- Thêm, sửa, xóa lịch.
- Hỗ trợ loại lịch: lịch học, lịch thi, deadline, công việc cá nhân.
- Xem lịch theo ngày, 3 ngày hoặc tuần.
- Lọc lịch theo loại.
- Mở link học online nếu trường địa điểm là URL.
- Cảnh báo khi lịch bị trùng thời gian.
- Hẹn notification nhắc trước lịch.

### Task và deadline

- Thêm, sửa, xóa task.
- Đánh dấu hoàn thành/chưa hoàn thành.
- Lọc theo hôm nay, sắp hạn, quá hạn, đã xong, tag, ưu tiên, ma trận quan trọng/khẩn cấp.
- Gắn marker cho task.
- Đặt reminder cho task.
- Bật tùy chọn hiển thị task deadline trên lịch.

### Đếm ngược

- Tạo mốc đếm ngược riêng.
- Hỗ trợ loại mốc: ngày thi, sinh nhật, sự kiện quan trọng, khác.
- Xem mốc sắp tới hoặc quá hạn.
- Sửa/xóa mốc đếm ngược.

### Pomodoro và thống kê

- Timer Pomodoro cho phiên tập trung/nghỉ.
- Chọn task/môn học cho phiên Pomodoro.
- Âm thanh nền/white noise.
- Lưu lịch sử phiên học.
- Thống kê tổng phút tập trung, số phiên, tiến độ task, số lượng lịch theo loại.

### AI đọc lịch từ ảnh

- Chọn ảnh từ camera hoặc thư viện.
- App gửi ảnh lên Gemini API.
- Gemini trả JSON danh sách lịch.
- App parse thành `StudyEvent` và cho người dùng xác nhận lưu.
- Nếu lỗi AI, app có thể gửi issue về admin web.

### Admin web

- Đăng nhập quản trị.
- Dashboard tài khoản/thống kê/lỗi/thông báo.
- Quản lý user registry.
- Khóa/mở khóa/xóa user khỏi registry.
- Yêu cầu user reset mật khẩu.
- Nhận snapshot học tập từ app.
- Tạo/bật/tắt/xóa thông báo.
- Theo dõi lỗi OTP/AI/general.
- Lưu audit log.

## Lưu trữ dữ liệu

### Local Android

App vẫn ghi dữ liệu vào SQLite trước để làm cache local và giúp app dùng được ổn định:

```text
study_auth.db
study_planner.db
study_planner_<hash_email>.db
```

Trong đó:

- `study_auth.db`: user email/password local, OTP hash.
- `study_planner*.db`: profile, tasks, events, countdowns, Pomodoro, settings.

### Firebase Realtime Database

Sau khi đăng nhập theo tài khoản, app đồng bộ dữ liệu học tập lên Firebase Realtime Database qua `FirebaseStudyStore`.

Vào Firebase Console > Realtime Database > Data và kiểm tra các node:

```text
study_users/{encodedEmail}/meta/profile
study_users/{encodedEmail}/meta/focus_stats
study_users/{encodedEmail}/meta/personalization
study_users/{encodedEmail}/tasks/{encodedTaskId}
study_users/{encodedEmail}/events/{encodedEventId}
study_users/{encodedEmail}/countdowns/{encodedCountdownId}
study_users/{encodedEmail}/pomodoro_sessions/{encodedSessionId}
study_users/{encodedEmail}/focus_day_stats/{encodedDayKey}
study_users/{encodedEmail}/settings/{encodedKey}
```

Ghi chú:

- `{encodedEmail}` là email được encode dạng Base64 URL-safe.
- SQLite vẫn là nơi ghi local trước.
- Firebase Realtime Database dùng để xem/sync dữ liệu theo tài khoản khi có kết nối mạng.

### Admin web

Admin web lưu dữ liệu runtime trong:

```text
admin-web/data/admin-store.properties
```

Đây là dữ liệu quản trị local, không phải SQLite.

## Cấu hình local

File `local.properties` cần có:

```properties
sdk.dir=C\:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
OTP_BACKEND_URL=http://10.0.2.2:8080
ADMIN_BACKEND_URL=http://10.0.2.2:8090
```

Giải thích:

- `10.0.2.2` là địa chỉ emulator dùng để gọi về máy host.
- Nếu chạy trên điện thoại thật, đổi `10.0.2.2` thành IP LAN của máy tính.
- Không commit `local.properties` nếu chứa API key thật.

## Cấu hình Firebase

App cần file:

```text
app/google-services.json
```

*(File này đã được đính kèm sẵn trong repository, bạn có thể sử dụng trực tiếp để demo).*

Firebase đang dùng:

- Firebase Authentication cho Google Sign-In.
- Firebase Realtime Database cho đồng bộ dữ liệu học tập.

Nếu muốn tự cấu hình Firebase riêng, cần kiểm tra trong Firebase Console:

1. Tạo Android app với package:

```text
com.example.cuoiky_qllichhoctap
```

2. Thêm SHA-1/SHA-256 cho debug/release keystore nếu dùng Google Sign-In.
3. Tải `google-services.json` về và đặt tại `app/google-services.json`.
4. Bật Authentication provider Google.
5. Bật Realtime Database.
6. Cấu hình rule Realtime Database phù hợp cho demo hoặc triển khai thật.

## Chạy OTP backend

OTP backend dùng SMTP để gửi mã OTP qua email.

Từ thư mục gốc dự án:

```powershell
cd otp-backend
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your-email@gmail.com"
$env:SMTP_PASSWORD="your-app-password"
$env:SMTP_FROM="your-email@gmail.com"
$env:SMTP_STARTTLS="true"
$env:OTP_BACKEND_PORT="8080"
..\gradlew.bat -p . run
```

Kiểm tra:

```text
http://localhost:8080/health
```

Endpoint chính:

```text
POST /send-otp
```

Ví dụ request:

```json
{
  "email": "student@example.com",
  "code": "123456",
  "purpose": "xác thực tài khoản"
}
```

Với Gmail cần dùng **App Password**, không dùng mật khẩu đăng nhập Gmail thông thường.

## Chạy admin web

Từ thư mục gốc dự án:

```powershell
cd admin-web
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="admin123"
$env:ADMIN_WEB_PORT="8090"
..\gradlew.bat -p . run
```

Mở trình duyệt:

```text
http://localhost:8090
```

Tài khoản mặc định nếu không cấu hình env:

```text
admin / admin123
```

## Build Android app

Từ thư mục gốc dự án:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK debug nằm tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Kiểm tra nhanh khi demo

1. Chạy OTP backend ở port `8080`.
2. Chạy admin web ở port `8090`.
3. Kiểm tra:

```text
http://localhost:8080/health
http://localhost:8090/health
```

4. Mở app trên emulator.
5. Đăng nhập Google hoặc đăng ký email/password.
6. Tạo/sửa task hoặc lịch.
7. Vào Firebase Console > Realtime Database để xem node `study_users`.
8. Vào admin web để xem user registry, learning snapshot, thông báo và lỗi.

## Câu hỏi thường gặp

### Dữ liệu có lưu trên Firebase không?

Có. Bản hiện tại đã có `FirebaseStudyStore`, đồng bộ dữ liệu học tập theo tài khoản lên Firebase Realtime Database. Tuy nhiên app vẫn ghi SQLite trước để làm local cache.

### SQLite còn dùng để làm gì?

SQLite vẫn là nơi lưu local chính để app hoạt động ổn định, đọc/ghi nhanh và có dữ liệu trên thiết bị. Firebase dùng để đồng bộ/xem dữ liệu theo tài khoản.

### Admin web có xem toàn bộ dữ liệu học tập không?

Admin web nhận registry tài khoản và learning snapshot từ app. Dữ liệu học tập chi tiết theo node đầy đủ có thể xem trong Firebase Realtime Database.

### Vì sao Google Sign-In lỗi `DEVELOPER_ERROR`?

Thường do thiếu SHA-1/SHA-256 trong Firebase Console hoặc `google-services.json` chưa đúng với package/debug keystore hiện tại.

### Nếu OTP không gửi được thì kiểm tra gì?

- OTP backend đã chạy chưa.
- `OTP_BACKEND_URL` đúng chưa.
- SMTP username/password/App Password đúng chưa.
- Gmail đã bật App Password chưa.
- Firewall/port 8080 có bị chặn không nếu dùng điện thoại thật.

## Ghi chú bảo mật

- Không commit `local.properties` nếu chứa API key thật.
- Không commit `.env` nếu chứa SMTP password thật.
- Đổi tài khoản admin mặc định trước khi demo trên máy dùng chung hoặc triển khai thật.
- Rule Firebase Realtime Database cần siết lại nếu đưa lên môi trường thật.

