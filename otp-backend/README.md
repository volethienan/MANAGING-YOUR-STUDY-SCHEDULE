# OTP Mail Backend

Backend nhỏ để Android app gửi mã OTP qua email bằng SMTP.

## Chạy local

PowerShell:

```powershell
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your-email@gmail.com"
$env:SMTP_PASSWORD="your-app-password"
$env:SMTP_FROM="your-email@gmail.com"
$env:OTP_BACKEND_PORT="8080"
..\gradlew.bat -p . run
```

Với Gmail cần dùng App Password, không dùng mật khẩu đăng nhập thường.

## Endpoint

`POST /send-otp`

```json
{
  "email": "student@example.com",
  "code": "123456",
  "purpose": "xác thực tài khoản"
}
```

Android emulator gọi máy host qua `http://10.0.2.2:8080`. Nếu chạy trên điện thoại thật, đổi `OTP_BACKEND_URL` trong `local.properties` sang IP máy tính, ví dụ `http://192.168.1.10:8080`.
