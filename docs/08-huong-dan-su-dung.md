# Hướng Dẫn Sử Dụng

## 1. Giới Thiệu

Study Planner là ứng dụng Android giúp sinh viên quản lý lịch học, deadline, task, Pomodoro và thống kê học tập. Giao diện được thiết kế theo phong cách trang vở học tập.

## 2. Mở Ứng Dụng Lần Đầu

1. Mở app Study Planner.
2. Ở màn hình giới thiệu, bấm **Bắt đầu ghi lịch**.
3. App chuyển sang màn hình đăng nhập.

## 3. Đăng Ký Tài Khoản

1. Ở màn hình đăng nhập, bấm **Chưa có tài khoản? Đăng ký ngay**.
2. Nhập:
   - Họ tên.
   - Email.
   - Mật khẩu.
   - Xác nhận mật khẩu.
3. Tick **Đồng ý điều khoản sử dụng**.
4. Bấm **Đăng ký và nhận OTP**.
5. Kiểm tra email để lấy mã OTP.
6. Nhập OTP 6 số ở màn hình xác thực.
7. Bấm **Xác nhận**.

Lưu ý:

- Mật khẩu cần ít nhất 6 ký tự.
- OTP có hiệu lực 5 phút.
- Nếu không nhận được OTP, bấm **Gửi lại mã**.
- Backend OTP phải đang chạy và cấu hình SMTP đúng.

## 4. Đăng Nhập Bằng Email

1. Nhập email và mật khẩu.
2. Bấm **Đăng nhập**.
3. Nếu tài khoản đã xác thực OTP và mật khẩu đúng, app mở Dashboard.

Các lỗi thường gặp:

| Thông báo | Nguyên nhân |
| --- | --- |
| Email chưa đúng định dạng | Email nhập sai format |
| Mật khẩu cần ít nhất 6 ký tự | Mật khẩu quá ngắn |
| Tài khoản chưa xác thực OTP | Đăng ký chưa hoàn tất OTP |
| Mật khẩu không đúng | Sai mật khẩu |

## 5. Đăng Nhập Bằng Google

1. Ở màn hình đăng nhập, bấm **Đăng nhập với Google**.
2. Chọn tài khoản Google.
3. Nếu Firebase cấu hình đúng, app đăng nhập và mở Dashboard.

Nếu app báo Google Sign-In không hoàn tất:

- Kiểm tra kết nối Internet.
- Kiểm tra thiết bị/emulator có Google Play Services.
- Nếu người dùng không tự hủy đăng nhập, nguyên nhân thường là Firebase SHA-1/OAuth chưa đúng.

## 6. Quên Mật Khẩu

1. Ở màn hình đăng nhập, bấm **Quên mật khẩu?**.
2. Nhập email đã đăng ký.
3. Bấm **Gửi mã OTP**.
4. Nhập OTP, mật khẩu mới và xác nhận mật khẩu.
5. Bấm **Cập nhật mật khẩu**.
6. Quay lại đăng nhập bằng mật khẩu mới.

## 7. Dashboard

Dashboard hiển thị nhanh:

- Lời chào và hồ sơ người dùng.
- Tiến độ hôm nay.
- Số việc hôm nay.
- Deadline gần nhất.
- Lịch học tiếp theo.
- Pomodoro hôm nay.
- 3 việc ưu tiên.

Các nút thêm nhanh:

| Nút | Chức năng |
| --- | --- |
| `+ Việc` | Thêm task học tập |
| `+ Lịch` | Thêm lịch học/lịch thi/deadline |
| `Từ ảnh` | Tạo lịch từ ảnh |
| `Tập trung` | Mở Pomodoro |

## 8. Quản Lý Lịch Học

### 8.1 Xem Lịch

1. Mở tab **Lịch học**.
2. Chọn chế độ xem:
   - Ngày.
   - 3 ngày.
   - Tuần.
3. Dùng nút điều hướng để chuyển tuần/ngày.
4. Dùng bộ lọc:
   - Tất cả.
   - Lịch học.
   - Thi.
   - Deadline.
   - Cá nhân.

### 8.2 Thêm Lịch

1. Bấm **Thêm lịch**.
2. Nhập:
   - Tiêu đề.
   - Loại lịch.
   - Môn học.
   - Ngày.
   - Giờ bắt đầu.
   - Giờ kết thúc.
   - Phòng học.
   - Ghi chú.
3. Có thể bật nhắc nhở và chọn thời gian nhắc trước.
4. Bấm lưu.

Nếu lịch trùng với lịch khác, app hiển thị cảnh báo xung đột. Người dùng có thể quay lại sửa hoặc vẫn lưu.

### 8.3 Sửa/Xóa Lịch

1. Chọn một lịch trong danh sách.
2. Chọn thao tác sửa hoặc xóa.
3. Xác nhận thay đổi.

## 9. Tạo Lịch Từ Ảnh

1. Ở Dashboard hoặc màn Lịch học, bấm **Từ ảnh** hoặc **Tạo từ ảnh**.
2. Chọn:
   - Chụp ảnh bằng camera.
   - Chọn ảnh từ thư viện.
3. App gửi ảnh tới Gemini API để đọc lịch.
4. Kiểm tra danh sách lịch được trích xuất.
5. Xác nhận lưu các lịch phù hợp.

Lưu ý:

- Cần cấu hình `GEMINI_API_KEY`.
- Ảnh nên rõ chữ, đủ sáng, không bị cắt mất ngày/giờ.

## 10. Quản Lý Việc Học

### 10.1 Xem Và Lọc Task

Mở tab **Việc học**. Có thể lọc theo:

- Tất cả.
- Hôm nay.
- Sắp hạn.
- Quá hạn.
- Đã xong.
- Môn học.
- Ưu tiên.
- 4 phần tư.

### 10.2 Thêm Task

1. Bấm **Thêm công việc** hoặc `+ Việc`.
2. Nhập tên task, môn học, deadline, ưu tiên và ghi chú.
3. Lưu task.

### 10.3 Hoàn Thành, Sửa, Xóa Task

1. Chọn task trong danh sách.
2. Chọn thao tác:
   - Đánh dấu hoàn thành.
   - Sửa thông tin.
   - Xóa task.
   - Chọn marker.

## 11. Pomodoro

1. Mở tab **Pomodoro** hoặc bấm **Tập trung** từ Dashboard.
2. Chọn chế độ/task nếu cần.
3. Bấm **Bắt đầu**.
4. Có thể tạm dừng, đặt lại hoặc bỏ qua phiên.
5. Khi hoàn thành phiên focus, app ghi nhận vào thống kê.

Tính năng Pomodoro gồm:

- Timer 25 phút mặc định.
- Nghỉ ngắn/nghỉ dài.
- Hiển thị biểu tượng phiên.
- Chọn âm thanh nền.
- Xem lịch sử Pomodoro.

## 12. Thống Kê

Mở tab **Thống kê học tập** để xem:

- Tổng quan hoàn thành task.
- Tiến độ hôm nay.
- Biểu đồ task.
- Số task đã xong, đang chờ, quá hạn.
- Phút tập trung hôm nay và tổng thời gian tập trung.
- Thống kê lịch theo loại.

## 13. Cài Đặt

Mở biểu tượng cài đặt từ Dashboard hoặc tab Settings.

### 13.1 Sửa Hồ Sơ

1. Bấm **Sửa hồ sơ**.
2. Cập nhật tên, email, mục tiêu học tập.
3. Lưu thay đổi.

### 13.2 Cá Nhân Hóa Giao Diện

1. Bấm **Cá nhân hóa giao diện**.
2. Chọn:
   - Ảnh đại diện.
   - Mascot.
   - Nền Dashboard.
   - Màu chủ đề.
   - Trạng thái học tập.
3. Lưu thay đổi.

### 13.3 Thông Báo Và Đồng Bộ

- Bật/tắt **Thông báo nhắc nhở**.
- Bật/tắt **Đồng bộ Google Calendar**.

Lưu ý: Google Calendar sync hiện là tùy chọn giao diện, chưa phải đồng bộ Calendar API đầy đủ.

### 13.4 Đăng Xuất

1. Bấm **Đăng xuất**.
2. App xóa trạng thái đăng nhập local.
3. Nếu đăng nhập Google, app gọi sign out Google/Firebase.

## 14. Xử Lý Lỗi Thường Gặp

| Tình huống | Cách xử lý |
| --- | --- |
| Không nhận được OTP | Kiểm tra backend OTP, SMTP, email nhập đúng |
| App báo không gọi được backend OTP | Emulator dùng `10.0.2.2:8080`; điện thoại thật dùng IP LAN |
| Google Sign-In không hoàn tất | Thêm đúng SHA-1 vào Firebase và tải lại `google-services.json` |
| Không tạo lịch từ ảnh | Kiểm tra `GEMINI_API_KEY`, Internet và chất lượng ảnh |
| Dữ liệu không thấy sau đăng nhập tài khoản khác | Dữ liệu được tách theo từng email |
| Build lỗi thiếu `google-services.json` | Đặt file vào `app/google-services.json` |
