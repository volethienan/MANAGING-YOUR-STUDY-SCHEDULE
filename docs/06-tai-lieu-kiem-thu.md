# Tài Liệu Kiểm Thử

## 1. Mục Tiêu Kiểm Thử

Đảm bảo ứng dụng Study Planner hoạt động đúng các luồng chính: xác thực, quản lý lịch, quản lý task, Pomodoro, thống kê, import ảnh và cấu hình dịch vụ ngoài.

## 2. Phạm Vi Kiểm Thử

### 2.1 Trong Phạm Vi

- Build Android app.
- Cài app lên emulator.
- Onboarding, đăng ký, đăng nhập, quên mật khẩu.
- Gửi OTP qua backend local.
- Google Sign-In qua Firebase.
- CRUD lịch học và task.
- Kiểm tra xung đột lịch.
- Pomodoro và thống kê.
- Import lịch từ ảnh bằng Gemini API.
- Cài đặt hồ sơ và cá nhân hóa.

### 2.2 Ngoài Phạm Vi

- Kiểm thử tải lớn nhiều người dùng đồng thời.
- Kiểm thử bảo mật chuyên sâu/pentest.
- Kiểm thử phát hành Google Play.
- Kiểm thử đồng bộ Google Calendar thật vì hiện mới có tùy chọn UI.

## 3. Môi Trường Kiểm Thử

| Thành phần | Giá trị |
| --- | --- |
| Hệ điều hành dev | Windows |
| IDE | Android Studio |
| Build tool | Gradle |
| Android minSdk | 24 |
| Android targetSdk | 36 |
| Emulator đã test | Pixel 9 Pro XL, Android API 36 |
| Java | Java 11+ |
| Backend OTP | Java HTTP server, port 8080 |
| Firebase | Project có Android app `com.example.cuoiky_qllichhoctap` |
| Gemini | API key trong `local.properties` |

## 4. Chiến Lược Kiểm Thử

| Loại kiểm thử | Mô tả |
| --- | --- |
| Unit test | Kiểm thử logic nhỏ, hiện project mới có test mẫu |
| Instrumented test | Kiểm thử context Android, hiện project mới có test mẫu |
| Manual test | Kiểm thử trực tiếp các luồng UI chính |
| Integration test | Kiểm thử app với Firebase, backend OTP, Gemini API |
| Regression test | Chạy lại các test case chính sau khi sửa lỗi |

## 5. Lệnh Kiểm Thử Kỹ Thuật

Build debug:

```powershell
.\gradlew.bat :app:assembleDebug
```

Cài app lên emulator:

```powershell
.\gradlew.bat :app:installDebug
```

Chạy unit test:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Chạy instrumented test:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Chạy backend OTP:

```powershell
.\gradlew.bat -p otp-backend run
```

Kiểm tra backend:

```powershell
curl http://localhost:8080/health
```

## 6. Test Case

| Mã | Chức năng | Tiền điều kiện | Bước thực hiện | Kết quả mong đợi | Trạng thái |
| --- | --- | --- | --- | --- | --- |
| TC-01 | Build app | Có JDK/Gradle | Chạy `.\gradlew.bat :app:assembleDebug` | Build success, tạo APK debug | Pass |
| TC-02 | Mở app lần đầu | App mới cài | Mở app | Hiển thị onboarding | Chưa test lại |
| TC-03 | Vào màn đăng nhập | Ở onboarding | Bấm "Bắt đầu ghi lịch" | Hiển thị màn đăng nhập | Chưa test lại |
| TC-04 | Đăng ký thiếu thông tin | Ở màn đăng ký | Bỏ trống tên/email/mật khẩu, bấm đăng ký | App báo cần nhập đủ thông tin | Chưa test lại |
| TC-05 | Đăng ký email sai định dạng | Ở màn đăng ký | Nhập email không hợp lệ | App báo email sai định dạng | Chưa test lại |
| TC-06 | Đăng ký mật khẩu ngắn | Ở màn đăng ký | Nhập mật khẩu dưới 6 ký tự | App báo mật khẩu cần ít nhất 6 ký tự | Chưa test lại |
| TC-07 | Đăng ký mật khẩu xác nhận sai | Ở màn đăng ký | Nhập confirm khác mật khẩu | App báo xác nhận chưa khớp | Chưa test lại |
| TC-08 | Đăng ký chưa đồng ý điều khoản | Ở màn đăng ký | Không tick điều khoản | App báo cần đồng ý điều khoản | Chưa test lại |
| TC-09 | Đăng ký hợp lệ | Backend OTP chạy, SMTP đúng | Nhập thông tin hợp lệ, bấm đăng ký | App gửi OTP và mở màn OTP | Cần test khi có SMTP |
| TC-10 | OTP sai | Có OTP active | Nhập OTP sai | App báo OTP chưa đúng, tăng attempts | Cần test |
| TC-11 | OTP đúng | Có OTP active | Nhập OTP đúng | Tài khoản verified, vào dashboard | Cần test |
| TC-12 | OTP hết hạn | OTP quá 5 phút | Nhập OTP | App báo OTP hết hạn | Cần test |
| TC-13 | Đăng nhập email chưa xác thực | Có user chưa verified | Nhập email/mật khẩu | App báo tài khoản chưa xác thực OTP | Cần test |
| TC-14 | Đăng nhập email sai mật khẩu | Có user verified | Nhập mật khẩu sai | App báo mật khẩu không đúng | Cần test |
| TC-15 | Đăng nhập email thành công | Có user verified | Nhập email/mật khẩu đúng | Vào dashboard | Cần test |
| TC-16 | Quên mật khẩu email không tồn tại | Ở forgot password | Nhập email không có trong DB | App báo không tìm thấy tài khoản đã xác thực | Cần test |
| TC-17 | Đặt lại mật khẩu | Có OTP reset | Nhập OTP và mật khẩu mới | Mật khẩu được cập nhật | Cần test |
| TC-18 | Google Sign-In thiếu/sai SHA-1 | Firebase SHA-1 không khớp | Bấm đăng nhập Google, chọn account | Google Sign-In không hoàn tất, log báo OAuth/SHA-1 | Fail cấu hình |
| TC-19 | Google Sign-In đúng cấu hình | Firebase SHA-1 đúng | Bấm đăng nhập Google, chọn account | Đăng nhập thành công, vào dashboard | Cần test sau cấu hình |
| TC-20 | Dashboard | Đã đăng nhập | Mở dashboard | Hiển thị tiến độ, task, deadline, lịch tiếp theo | Chưa test lại |
| TC-21 | Thêm task | Đã đăng nhập | Bấm thêm task, nhập thông tin hợp lệ | Task xuất hiện trong danh sách | Cần test |
| TC-22 | Sửa task | Có task | Mở action sửa, cập nhật title | Task cập nhật | Cần test |
| TC-23 | Xóa task | Có task | Mở action xóa | Task biến mất | Cần test |
| TC-24 | Hoàn thành task | Có task chưa xong | Đánh dấu hoàn thành | Task chuyển trạng thái đã xong, thống kê cập nhật | Cần test |
| TC-25 | Lọc task hôm nay | Có task deadline hôm nay | Bấm filter Hôm nay | Chỉ hiện task hôm nay | Cần test |
| TC-26 | Lọc task quá hạn | Có task quá hạn | Bấm filter Quá hạn | Chỉ hiện task quá hạn | Cần test |
| TC-27 | Thêm lịch hợp lệ | Đã đăng nhập | Bấm thêm lịch, nhập ngày giờ hợp lệ | Event được lưu và hiển thị | Cần test |
| TC-28 | Thêm lịch giờ sai | Nhập end <= start | Lưu event | App báo lỗi thời gian | Cần test |
| TC-29 | Xung đột lịch | Có event A | Tạo event B trùng giờ A | App cảnh báo xung đột | Cần test |
| TC-30 | Lọc lịch thi | Có event loại lịch thi | Bấm filter Thi | Chỉ hiện lịch thi | Cần test |
| TC-31 | Import ảnh thiếu Gemini key | `GEMINI_API_KEY` rỗng | Chọn ảnh import | App báo chưa cấu hình Gemini API key | Cần test |
| TC-32 | Import ảnh hợp lệ | Có Gemini key | Chọn ảnh lịch học | App parse danh sách event và cho lưu | Cần test |
| TC-33 | Chạy Pomodoro | Đã đăng nhập | Bấm bắt đầu Pomodoro | Timer chạy | Cần test |
| TC-34 | Hoàn thành Pomodoro | Timer kết thúc | Chờ hết phiên hoặc test duration ngắn | Focus stats tăng | Cần test |
| TC-35 | Xem thống kê | Có task/event/focus data | Mở Stats | Hiển thị biểu đồ và số liệu | Cần test |
| TC-36 | Sửa hồ sơ | Đã đăng nhập | Mở Settings, sửa profile | Profile mới hiển thị | Cần test |
| TC-37 | Cá nhân hóa | Đã đăng nhập | Chọn avatar/nền/màu/mascot | Dashboard cập nhật theo lựa chọn | Cần test |
| TC-38 | Đăng xuất | Đã đăng nhập | Bấm Đăng xuất | Trở về màn login, Firebase/Google sign out nếu có | Cần test |

## 7. Bug Report

### BUG-01: Google Sign-In Báo Không Hoàn Tất Do Sai SHA-1/OAuth

| Mục | Nội dung |
| --- | --- |
| Chức năng | Đăng nhập Google |
| Mức độ | Cao |
| Môi trường | Emulator Android API 36 |
| Cách tái hiện | Bấm "Đăng nhập với Google", chọn tài khoản Gmail |
| Kết quả thực tế | Flow đóng lại, app không đăng nhập được |
| Log chính | `This android application is not registered to use OAuth2.0... package name and SHA-1 certificate fingerprint match...` |
| Nguyên nhân | SHA-1 debug hiện tại không khớp SHA-1 trong Firebase/Google OAuth client |
| SHA-1 debug hiện tại | `24:46:D4:63:C7:3C:AF:03:64:4D:FE:10:9F:F4:5A:43:3D:0C:36:40` |
| Trạng thái | Chưa fix cấu hình Firebase; đã sửa thông báo trong app để không báo nhầm người dùng hủy |
| Hướng xử lý | Thêm SHA-1 vào Firebase Console, tải lại `google-services.json`, rebuild app |

## 8. Ma Trận Truy Vết Yêu Cầu - Test

| Yêu cầu | Test case |
| --- | --- |
| FR-01 | TC-02, TC-03 |
| FR-02, FR-03, FR-04 | TC-04 đến TC-12 |
| FR-05 | TC-13 đến TC-15 |
| FR-06, FR-07 | TC-16, TC-17 |
| FR-08 | TC-18, TC-19 |
| FR-09 | TC-38 |
| FR-10, FR-11 | TC-36, TC-37 |
| FR-12 | TC-20 |
| FR-13 đến FR-18 | TC-27 đến TC-30 |
| FR-19 | TC-31, TC-32 |
| FR-20 đến FR-25 | TC-21 đến TC-26 |
| FR-26 đến FR-29 | TC-33, TC-34 |
| FR-30 | TC-35 |

## 9. Kết Luận Kiểm Thử Hiện Tại

- App build debug thành công.
- App cài được lên emulator.
- Google Sign-In đã test và xác định lỗi do cấu hình SHA-1/OAuth, không phải lỗi người dùng hủy.
- Các test case nghiệp vụ còn lại cần chạy thủ công đầy đủ khi có backend OTP, SMTP và Gemini API key hợp lệ.
