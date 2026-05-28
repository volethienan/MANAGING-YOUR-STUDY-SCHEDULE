# Web Quản Trị Study Planner

## 1. Mục Tiêu

Web quản trị là thành phần dành cho quản trị viên, chạy tách khỏi ứng dụng Android. Thành phần này tập trung vào quản lý tài khoản đã đồng bộ, thông báo chung và theo dõi lỗi vận hành của OTP hoặc AI đọc lịch.

## 2. Phạm Vi

Ứng dụng Android hiện lưu lịch học, task và Pomodoro cục bộ theo thiết bị/tài khoản. Vì vậy web quản trị không quản lý trực tiếp dữ liệu lịch học cá nhân. Android đồng bộ thông tin tài khoản cơ bản về web khi người dùng đăng nhập hoặc hoàn tất xác thực OTP.

## 3. Yêu Cầu Chức Năng Của Người Quản Trị

### 3.1. Quản lý tài khoản người dùng

Người quản trị có thể xem danh sách tài khoản đã đồng bộ, tìm kiếm theo tên hoặc email, xem trạng thái xác thực, khóa tài khoản, mở khóa tài khoản và xóa bản ghi tài khoản khỏi registry quản trị.

### 3.2. Quản lý thông báo hệ thống

Người quản trị có thể tạo thông báo chung, bật hoặc tắt thông báo và xóa thông báo không còn sử dụng.

### 3.3. Theo dõi hoạt động hệ thống

Người quản trị có thể xem dashboard tổng quan gồm số tài khoản, số tài khoản đã xác thực, số tài khoản đang khóa, số lỗi chưa xử lý và số thông báo đang bật. Hệ thống cũng hiển thị nhật ký thao tác quản trị gần đây.

### 3.4. Theo dõi lỗi vận hành

Người quản trị có thể xem lỗi OTP và lỗi AI đọc lịch được app Android gửi về, đánh dấu lỗi đã xử lý, mở lại lỗi hoặc xóa lỗi khỏi danh sách theo dõi.

## 4. Liên Kết Với App Android

- `POST /api/mobile/users/sync`: app gửi email, tên, nguồn đăng nhập và trạng thái xác thực.
- Phản hồi của endpoint đồng bộ trả về trạng thái khóa để app chặn truy cập ở lần đăng nhập hoặc mở lại app tiếp theo.
- `POST /api/mobile/issues`: app gửi lỗi OTP hoặc lỗi AI về danh sách theo dõi của quản trị viên.
