# Hướng Dẫn Cài Đặt Và Triển Khai

## 1. Mục Đích

Tài liệu này hướng dẫn cài đặt môi trường, cấu hình Firebase, Gemini API, OTP backend, build và chạy ứng dụng Study Planner.

## 2. Yêu Cầu Môi Trường

| Thành phần | Yêu cầu |
| --- | --- |
| OS dev | Windows |
| JDK | Java 11 hoặc cao hơn |
| Android Studio | Bản hỗ trợ Android Gradle Plugin hiện tại |
| Android SDK | compileSdk 36 |
| Gradle Wrapper | Dùng `gradlew.bat` có sẵn trong repo |
| Emulator/Device | Android API 24 trở lên, có Google Play Services nếu dùng Google Sign-In |
| Internet | Cần cho Firebase, Gemini, SMTP |

## 3. Clone Và Mở Project

```powershell
git clone <repository-url>
cd MANAGING-YOUR-STUDY-SCHEDULE
```

Mở thư mục project bằng Android Studio, chờ Gradle sync hoàn tất.

## 4. Cấu Hình Android App

### 4.1 Package Name

Package/applicationId hiện tại:

```text
com.example.cuoiky_qllichhoctap
```

Firebase Android app phải dùng đúng package này.

### 4.2 File `google-services.json`

Tải file từ Firebase Console và đặt tại:

```text
app/google-services.json
```

Nếu thiếu file này, Gradle sẽ lỗi ở task:

```text
:app:processDebugGoogleServices
```

### 4.3 Cấu Hình SHA-1 Cho Google Sign-In

Lấy SHA-1 debug:

```powershell
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
& $keytool -list -v -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -keypass android
```

SHA-1 đang dùng trên máy kiểm thử hiện tại:

```text
24:46:D4:63:C7:3C:AF:03:64:4D:FE:10:9F:F4:5A:43:3D:0C:36:40
```

Thêm SHA-1 này vào Firebase Console:

```text
Project settings -> Your apps -> Android app -> Add fingerprint
```

Sau đó tải lại `google-services.json` và thay file trong `app/`.

## 5. Cấu Hình `local.properties`

File `local.properties` nằm ở root project và không commit lên Git.

Ví dụ:

```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
OTP_BACKEND_URL=http://10.0.2.2:8080
```

Giải thích:

| Key | Mục đích |
| --- | --- |
| `GEMINI_API_KEY` | Dùng cho chức năng tạo lịch từ ảnh |
| `OTP_BACKEND_URL` | URL backend gửi OTP; emulator dùng `http://10.0.2.2:8080` |

## 6. Build Android App

Build debug:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Cài lên emulator/device:

```powershell
.\gradlew.bat :app:installDebug
```

## 7. Chạy OTP Backend

### 7.1 Cấu Hình Biến Môi Trường

Với Gmail, cần dùng App Password thay vì mật khẩu đăng nhập thường.

```powershell
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your-email@gmail.com"
$env:SMTP_PASSWORD="your-app-password"
$env:SMTP_FROM="your-email@gmail.com"
$env:SMTP_STARTTLS="true"
$env:OTP_BACKEND_PORT="8080"
```

### 7.2 Khởi Động Backend

```powershell
.\gradlew.bat -p otp-backend run
```

Khi chạy thành công:

```text
OTP mail backend started on http://localhost:8080
```

### 7.3 Kiểm Tra Backend

```powershell
curl http://localhost:8080/health
```

Response:

```json
{"ok": true}
```

Test gửi OTP:

```powershell
curl -Method POST http://localhost:8080/send-otp `
  -ContentType "application/json" `
  -Body '{"email":"student@example.com","code":"123456","purpose":"xác thực tài khoản"}'
```

## 8. Cấu Hình Firebase Authentication

Trong Firebase Console:

1. Vào Authentication.
2. Chọn Sign-in method.
3. Bật Email/Password nếu dùng Firebase email trong tương lai.
4. Bật Google provider.
5. Đảm bảo Android app có đúng package name và SHA-1.
6. Tải lại `google-services.json` sau khi thêm SHA-1.

Lưu ý: App hiện có hệ thống email/password local bằng SQLite và OTP backend riêng. Firebase chủ yếu dùng cho Google Sign-In.

## 9. Cấu Hình Gemini API

1. Tạo Gemini API key từ Google AI Studio/Google Cloud.
2. Thêm vào `local.properties`:

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

3. Rebuild app:

```powershell
.\gradlew.bat :app:assembleDebug
```

Nếu không cấu hình key, chức năng tạo lịch từ ảnh sẽ báo lỗi chưa cấu hình `GEMINI_API_KEY`.

## 10. Chạy Trên Thiết Bị Thật

Nếu dùng điện thoại thật thay vì emulator:

- `10.0.2.2` không trỏ về máy tính.
- Đổi `OTP_BACKEND_URL` sang IP LAN của máy tính, ví dụ:

```properties
OTP_BACKEND_URL=http://192.168.1.10:8080
```

- Mở Windows Firewall cho port 8080 nếu cần.
- Điện thoại và máy tính phải cùng mạng.

## 11. Xử Lý Lỗi Thường Gặp

| Lỗi | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `google-services.json is missing` | Chưa đặt file Firebase config | Tải file và đặt vào `app/google-services.json` |
| Google Sign-In không hoàn tất | Sai SHA-1/OAuth | Thêm SHA-1 debug/release vào Firebase, tải lại config |
| OTP không gửi được | Backend chưa chạy hoặc SMTP sai | Chạy backend, kiểm tra biến môi trường SMTP |
| App không gọi được backend trên emulator | URL sai | Dùng `http://10.0.2.2:8080` |
| App không gọi được backend trên điện thoại thật | IP/firewall sai | Dùng IP LAN, mở firewall |
| Gemini API lỗi 401/403 | API key sai hoặc chưa bật quyền | Kiểm tra key và quyền API |
| Gemini API lỗi timeout | Mạng chậm/ảnh lớn | Thử ảnh nhỏ hơn hoặc mạng ổn định hơn |

## 12. Backup Và Restore Dữ Liệu

Hiện dữ liệu chính nằm trong SQLite private app storage. Với bản debug/phát triển:

- Backup đơn giản nhất là export dữ liệu qua chức năng tương lai hoặc dùng Android Studio Device Explorer.
- Khi uninstall app, dữ liệu local có thể bị xóa.
- Chưa có cơ chế backup/restore chính thức trong phiên bản 1.0.

## 13. Release Notes Phiên Bản 1.0

### Tính năng

- Onboarding và login/register UI.
- Đăng ký email với OTP.
- Đăng nhập email local.
- Quên mật khẩu qua OTP.
- Google Sign-In qua Firebase.
- Dashboard học tập.
- Quản lý lịch học/task.
- Pomodoro và thống kê.
- Import lịch từ ảnh bằng Gemini.
- Backend gửi OTP qua SMTP.

### Hạn chế

- Dữ liệu chưa đồng bộ cloud.
- Google Calendar sync mới có cấu hình UI.
- Google Sign-In phụ thuộc cấu hình SHA-1 thủ công.
- Backend OTP phù hợp demo/local, chưa có rate limit production.
