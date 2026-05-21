# Đề Cương Dự Án

## 1. Thông Tin Chung

| Mục | Nội dung |
| --- | --- |
| Tên dự án | Managing Your Study Schedule |
| Tên ứng dụng | Study Planner |
| Loại phần mềm | Ứng dụng Android quản lý lịch học cá nhân |
| Nền tảng | Android Java/XML |
| Backend phụ trợ | Java HTTP server gửi OTP email |
| Cơ sở dữ liệu | SQLite cục bộ trên thiết bị Android |
| Dịch vụ bên ngoài | Firebase Authentication, Google Sign-In, Gemini API, SMTP email |
| Phiên bản hiện tại | 1.0 |

## 2. Lý Do Thực Hiện

Sinh viên thường phải quản lý nhiều loại thông tin học tập cùng lúc như lịch học, lịch thi, deadline, công việc cần làm, thời gian tự học và tiến độ hoàn thành. Nếu dùng ghi chú thủ công hoặc nhiều ứng dụng rời rạc, người dùng dễ bỏ sót deadline, trùng lịch hoặc không theo dõi được hiệu quả học tập.

Ứng dụng Study Planner được xây dựng để gom các nhu cầu này vào một phần mềm di động đơn giản, trực quan và phù hợp với thói quen học tập hằng ngày.

## 3. Mục Tiêu Dự Án

- Xây dựng ứng dụng Android cho phép sinh viên quản lý lịch học, lịch thi, deadline và công việc học tập.
- Cung cấp chức năng đăng ký, đăng nhập bằng email/mật khẩu kèm xác thực OTP qua email.
- Cung cấp đăng nhập Google qua Firebase Authentication.
- Cho phép tạo lịch học thủ công hoặc trích xuất lịch từ ảnh bằng Gemini API.
- Hỗ trợ Pomodoro để quản lý thời gian tập trung.
- Hiển thị thống kê tiến độ học tập, task, lịch và thời gian tập trung.
- Lưu dữ liệu cục bộ theo từng tài khoản để người dùng có thể sử dụng lại sau khi đăng nhập.

## 4. Phạm Vi Dự Án

### 4.1 Trong Phạm Vi

- Onboarding lần đầu mở ứng dụng.
- Đăng ký tài khoản bằng email, mật khẩu và OTP.
- Đăng nhập bằng email/mật khẩu.
- Đặt lại mật khẩu bằng OTP.
- Đăng nhập Google bằng Firebase Auth.
- Quản lý hồ sơ người dùng.
- Quản lý lịch học, lịch thi, deadline và công việc cá nhân.
- Kiểm tra xung đột thời gian giữa các lịch.
- Quản lý task học tập, ưu tiên, tag, deadline, nhắc nhở và trạng thái hoàn thành.
- Chế độ lọc task theo hôm nay, sắp hạn, quá hạn, đã xong, môn học, ưu tiên và ma trận 4 phần tư.
- Pomodoro với phiên tập trung, nghỉ ngắn, nghỉ dài, âm thanh nền và lịch sử.
- Thống kê tiến độ học tập.
- Nhập lịch từ ảnh thông qua camera/thư viện và Gemini API.
- Backend local gửi OTP qua SMTP.

### 4.2 Ngoài Phạm Vi

- Không có hệ thống quản trị web riêng.
- Không có đồng bộ dữ liệu cloud đầy đủ cho lịch và task.
- Không có phân quyền nhiều vai trò như admin, giáo viên, sinh viên.
- Không có hệ thống thông báo push từ server.
- Không có phát hành chính thức lên Google Play Store trong phạm vi đồ án.

## 5. Đối Tượng Sử Dụng

| Đối tượng | Mô tả |
| --- | --- |
| Sinh viên | Người dùng chính, cần quản lý lịch học, task, deadline và thời gian tập trung |
| Người kiểm thử/giảng viên | Cài đặt, chạy thử app, kiểm tra chức năng và tài liệu |
| Nhà phát triển | Bảo trì mã nguồn Android, backend OTP và cấu hình dịch vụ |

## 6. Lợi Ích Kỳ Vọng

- Giảm tình trạng quên lịch học, lịch thi và deadline.
- Giúp sinh viên theo dõi tiến độ hoàn thành task.
- Cải thiện thói quen tập trung bằng Pomodoro.
- Rút ngắn thời gian nhập lịch nhờ chức năng tạo lịch từ ảnh.
- Cung cấp minh chứng quy trình phát triển phần mềm theo tiêu chí Công nghệ phần mềm.

## 7. Ràng Buộc Và Giả Định

### 7.1 Ràng Buộc

- App yêu cầu Android minSdk 24.
- App cần `google-services.json` hợp lệ để dùng Firebase/Google Sign-In.
- Google Sign-In yêu cầu SHA-1 debug/release đúng trong Firebase Console.
- Chức năng OTP email yêu cầu backend local hoặc server có cấu hình SMTP.
- Chức năng đọc lịch từ ảnh yêu cầu `GEMINI_API_KEY`.
- Dữ liệu lịch/task hiện lưu cục bộ trên thiết bị, không phải cloud database.

### 7.2 Giả Định

- Người dùng có thiết bị Android hoặc emulator có Google Play Services.
- Người dùng có kết nối Internet khi đăng nhập Google, gửi OTP hoặc dùng Gemini API.
- Người dùng cho phép app truy cập camera/thư viện khi tạo lịch từ ảnh.

## 8. Sản Phẩm Bàn Giao

- Mã nguồn ứng dụng Android.
- Mã nguồn backend OTP.
- File APK debug.
- Bộ tài liệu dự án trong thư mục `docs`.
- README hướng dẫn cài đặt và chạy project.

## 9. Tiêu Chí Thành Công

- App build thành công bằng Gradle.
- Người dùng có thể đăng ký, xác thực OTP và đăng nhập email.
- Người dùng có thể thêm, sửa, xóa lịch học và task.
- App phát hiện được xung đột lịch.
- Người dùng có thể chạy Pomodoro và xem thống kê.
- Chức năng Google Sign-In hoạt động khi Firebase SHA-1/OAuth được cấu hình đúng.
- Chức năng tạo lịch từ ảnh hoạt động khi có Gemini API key hợp lệ.
