# Study Planner Admin Web

Web quản trị dành cho hệ thống Study Planner.

## Chức năng

- Đăng nhập quản trị.
- Xem dashboard số tài khoản, tài khoản đã xác thực, tài khoản đang khóa, lỗi mở và thông báo đang bật.
- Xem, tìm kiếm, khóa, mở khóa hoặc xóa tài khoản đã đồng bộ từ app Android.
- Tạo, bật/tắt và xóa thông báo chung.
- Theo dõi lỗi OTP và lỗi AI đọc lịch được app gửi về.
- Lưu nhật ký thao tác quản trị gần đây.

## Phạm vi dữ liệu

App Android hiện lưu lịch học, task và Pomodoro trên từng thiết bị. Web quản trị không đọc trực tiếp dữ liệu học tập cá nhân đó.

App gửi bản ghi tài khoản về web khi người dùng đăng nhập hoặc hoàn tất xác thực OTP. Khi tài khoản bị khóa trên web, app chặn truy cập ở lần đồng bộ đăng nhập tiếp theo nếu backend quản trị đang kết nối được.

## Chạy local

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25'
$env:ADMIN_USERNAME='admin'
$env:ADMIN_PASSWORD='change-this-password'
..\gradlew.bat -p . run
```

Mặc định web chạy ở `http://localhost:8090`.

Nếu không đặt biến môi trường tài khoản quản trị, tài khoản dev mặc định là:

```text
admin / admin123
```

Đổi tài khoản mặc định trước khi demo trên máy dùng chung.

## Kết nối Android

Emulator Android gọi máy host qua `10.0.2.2`:

```properties
ADMIN_BACKEND_URL=http://10.0.2.2:8090
```

Nếu chạy trên điện thoại thật, đặt `ADMIN_BACKEND_URL` thành IP LAN của máy chạy web quản trị và mở firewall cho port tương ứng.

## API chính

| Endpoint | Mục đích |
| --- | --- |
| `POST /api/auth/login` | Đăng nhập web quản trị |
| `GET /api/stats` | Thống kê dashboard |
| `GET /api/users` | Danh sách tài khoản đã đồng bộ |
| `POST /api/users/action` | Khóa, mở khóa hoặc xóa tài khoản khỏi registry |
| `GET /api/announcements` | Danh sách thông báo |
| `POST /api/announcements` | Tạo thông báo |
| `GET /api/issues` | Danh sách lỗi OTP/AI |
| `POST /api/mobile/users/sync` | Android đồng bộ tài khoản và nhận trạng thái khóa |
| `POST /api/mobile/issues` | Android gửi lỗi OTP/AI |

## Dữ liệu runtime

Web lưu dữ liệu local trong `admin-web/data/admin-store.properties` khi chạy từ root project. Thư mục này được ignore khỏi git.
