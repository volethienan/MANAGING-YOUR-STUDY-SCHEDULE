# Kế Hoạch Dự Án

## 1. Mục Đích

Tài liệu này mô tả kế hoạch phát triển, phân công, công cụ, tiến độ, rủi ro và tiêu chí nghiệm thu cho dự án Study Planner.

## 2. Phạm Vi Công Việc

| Nhóm công việc | Nội dung |
| --- | --- |
| Phân tích | Khảo sát nhu cầu quản lý lịch học, task, Pomodoro và xác thực tài khoản |
| Thiết kế | Thiết kế UI Android, dữ liệu SQLite, luồng xác thực và luồng nhập lịch từ ảnh |
| Xây dựng app | Lập trình Android Java/XML, repository, model, màn hình và xử lý nghiệp vụ |
| Xây dựng backend | Tạo HTTP server Java gửi OTP qua SMTP |
| Tích hợp | Firebase Auth, Google Sign-In, Gemini API, SMTP |
| Kiểm thử | Kiểm thử đăng nhập, OTP, lịch, task, Pomodoro, thống kê, import ảnh |
| Tài liệu | Hoàn thiện bộ tài liệu theo tiêu chí Công nghệ phần mềm |

## 3. Nhân Sự Và Vai Trò

| Vai trò | Trách nhiệm |
| --- | --- |
| Quản lý dự án | Theo dõi tiến độ, phạm vi, rủi ro, tài liệu bàn giao |
| Phân tích viên | Xác định yêu cầu, use case, quy tắc nghiệp vụ |
| Lập trình viên Android | Xây dựng giao diện, xử lý nghiệp vụ, SQLite, Firebase |
| Lập trình viên backend | Xây dựng OTP backend và cấu hình SMTP |
| Kiểm thử viên | Viết test case, kiểm thử thủ công, ghi nhận lỗi |
| Người viết tài liệu | Viết SRS, thiết kế, triển khai, hướng dẫn sử dụng |

Với đồ án cá nhân hoặc nhóm nhỏ, một thành viên có thể đảm nhiệm nhiều vai trò.

## 4. Công Cụ Sử Dụng

| Công cụ | Mục đích |
| --- | --- |
| Android Studio | Phát triển và chạy ứng dụng Android |
| Gradle | Build app và backend |
| Java 11+ | Ngôn ngữ lập trình app và backend |
| XML Layout | Thiết kế giao diện Android |
| SQLite | Lưu trữ dữ liệu cục bộ |
| Firebase Console | Cấu hình Firebase Auth và Google Sign-In |
| Google Cloud OAuth | Quản lý OAuth client |
| Gemini API | Nhận diện lịch học từ ảnh |
| SMTP Gmail/App Password | Gửi OTP email |
| Git/GitHub | Quản lý mã nguồn và lịch sử thay đổi |
| Emulator/Thiết bị Android | Kiểm thử ứng dụng |

## 5. Tiến Độ Dự Kiến

| Giai đoạn | Công việc | Thời lượng dự kiến | Kết quả |
| --- | --- | --- | --- |
| 1 | Khởi tạo ý tưởng và phạm vi | 1 ngày | Đề cương dự án |
| 2 | Phân tích yêu cầu | 2 ngày | SRS, use case |
| 3 | Thiết kế hệ thống và dữ liệu | 2 ngày | Kiến trúc, sơ đồ, thiết kế SQLite |
| 4 | Xây dựng chức năng xác thực | 2 ngày | Đăng ký, OTP, đăng nhập, quên mật khẩu |
| 5 | Xây dựng lịch học và task | 3 ngày | CRUD lịch/task, lọc, xung đột |
| 6 | Xây dựng Pomodoro và thống kê | 2 ngày | Timer, lịch sử, thống kê |
| 7 | Tích hợp Gemini và Firebase | 2 ngày | Import ảnh, Google Sign-In |
| 8 | Kiểm thử và sửa lỗi | 2 ngày | Test case, bug report |
| 9 | Hoàn thiện tài liệu | 2 ngày | Bộ tài liệu Markdown |

## 6. Mốc Thời Gian

| Mốc | Mô tả | Điều kiện hoàn thành |
| --- | --- | --- |
| M1 | Chốt đề tài | Có đề cương và phạm vi |
| M2 | Chốt yêu cầu | Có SRS và use case |
| M3 | Chốt thiết kế | Có kiến trúc, dữ liệu, giao diện |
| M4 | Bản chạy được | App build và chạy trên emulator |
| M5 | Bản tích hợp | Firebase, OTP, Gemini được cấu hình |
| M6 | Bản nghiệm thu | Test pass các luồng chính và có tài liệu |

## 7. Rủi Ro Dự Án

| Mã | Rủi ro | Mức độ | Ảnh hưởng | Phương án xử lý |
| --- | --- | --- | --- | --- |
| R01 | Sai SHA-1 Firebase làm Google Sign-In lỗi | Cao | Không đăng nhập được Google | Lấy SHA-1 bằng `keytool`, thêm vào Firebase, tải lại `google-services.json` |
| R02 | Không cấu hình SMTP đúng | Cao | Không gửi được OTP | Dùng Gmail App Password, kiểm tra biến môi trường backend |
| R03 | Thiếu Gemini API key | Trung bình | Không tạo lịch từ ảnh | Cấu hình `GEMINI_API_KEY` trong `local.properties` |
| R04 | Dữ liệu chỉ lưu local | Trung bình | Đổi máy sẽ mất dữ liệu | Ghi rõ giới hạn, đề xuất cloud sync ở phiên bản sau |
| R05 | UI phụ thuộc kích thước màn hình | Trung bình | Một số màn hình nhỏ có thể cuộn nhiều | Test trên emulator nhiều kích thước |
| R06 | Google Sign-In legacy API bị deprecate | Thấp/Trung bình | Cần nâng cấp tương lai | Ghi nhận trong bảo trì, cân nhắc Credential Manager |

## 8. Chi Phí Dự Kiến

| Hạng mục | Chi phí |
| --- | --- |
| Android Studio, Gradle, Java | Miễn phí |
| Firebase Authentication | Có gói miễn phí, tùy giới hạn sử dụng |
| Gemini API | Có thể phát sinh chi phí theo API key |
| SMTP Gmail | Miễn phí nếu dùng tài khoản cá nhân, cần App Password |
| GitHub | Miễn phí cho repo cá nhân |

## 9. Kế Hoạch Kiểm Soát Chất Lượng

- Kiểm tra build Android bằng `.\gradlew.bat :app:assembleDebug`.
- Kiểm tra cài đặt app bằng `.\gradlew.bat :app:installDebug`.
- Kiểm tra backend OTP bằng endpoint `/health`.
- Kiểm thử thủ công các luồng chính theo tài liệu test case.
- Ghi nhận bug, nguyên nhân, trạng thái và cách khắc phục.
- Không commit file nhạy cảm như `local.properties`, `.env`, keystore, mật khẩu SMTP.

## 10. Tiêu Chí Nghiệm Thu

- App chạy được trên emulator Android.
- Có thể đăng ký tài khoản email và xác thực OTP khi backend SMTP hoạt động.
- Có thể đăng nhập bằng email/mật khẩu sau khi xác thực.
- Google Sign-In chạy được khi Firebase SHA-1/OAuth cấu hình đúng.
- Có thể thêm/sửa/xóa lịch học và task.
- Có thể phát hiện lịch bị trùng thời gian.
- Có thể chạy Pomodoro và ghi nhận thống kê.
- Có đủ 9 tài liệu Markdown trong thư mục `docs`.
