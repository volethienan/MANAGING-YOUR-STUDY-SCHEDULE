# Phân Công Nhiệm Vụ Thành Viên

## 1. Mục Đích

Tài liệu này mô tả phạm vi công việc của từng thành viên trong dự án **Study Planner - Managing Your Study Schedule**. Việc phân công được trình bày theo module chức năng để thể hiện rõ phần chịu trách nhiệm chính và phần phối hợp giữa các thành viên.

## 2. Thành Viên

| Thành viên | Vai trò tổng quát |
| --- | --- |
| THAI HO PHU GIA | Phụ trách luồng chức năng chính của ứng dụng Android, xử lý dữ liệu local, lịch học, Pomodoro, reminder và tích hợp dữ liệu vào các màn hình người dùng. |
| VO LE THIEN AN | Phụ trách giao diện người dùng, Task/To-do list, Firebase, Gemini AI, OTP backend, admin web, countdown, thống kê và các chức năng đồng bộ/quản trị. |

## 3. Bảng Phân Công Theo Module

| Module/Hạng mục | THAI HO PHU GIA | VO LE THIEN AN |
| --- | --- | --- |
| Dashboard | Xử lý logic tổng quan, lấy dữ liệu từ task, lịch học, Pomodoro và hiển thị tiến độ học tập. | Thiết kế giao diện dashboard, card thông tin, bố cục và trải nghiệm hiển thị. |
| UI tổng thể ứng dụng | Gắn sự kiện, điều hướng màn hình và kiểm tra luồng thao tác. | Thiết kế XML layout, drawable, theme, icon, bottom navigation, side menu và phong cách giao diện chung. |
| Lịch học | Phụ trách logic tạo, sửa, xóa lịch học/lịch thi/deadline, lọc theo ngày/tuần và reminder. | Hỗ trợ giao diện lịch học và tích hợp kết quả nhập lịch từ Gemini AI. |
| Task/To-do list | Tích hợp dữ liệu task vào dashboard, reminder, Pomodoro và repository. | Phụ trách màn Task/To-do list, form task, card task, trạng thái hoàn thành và thao tác thêm/sửa/xóa. |
| Pomodoro | Phụ trách logic chạy phiên tập trung, chọn task liên quan, lưu phiên học và cập nhật tiến độ. | Hỗ trợ giao diện và dữ liệu phục vụ thống kê Pomodoro. |
| SQLite local | Thiết kế lưu trữ local bằng SQLite, repository, model và các thao tác CRUD dữ liệu học tập. | Hỗ trợ phần đồng bộ dữ liệu từ local lên Firebase. |
| Notification reminder | Phụ trách alarm/notification nhắc lịch học, task và deadline trên thiết bị. | Hỗ trợ kiểm tra giao diện và luồng thao tác liên quan đến reminder. |
| Đăng nhập email/OTP trên app | Xây dựng luồng đăng ký, đăng nhập, nhập OTP, đặt lại mật khẩu và điều hướng sau xác thực. | Phụ trách backend gửi OTP qua email và hỗ trợ xử lý lỗi OTP. |
| Countdown | Hỗ trợ liên kết deadline/task khi cần hiển thị mốc sắp đến. | Phụ trách chức năng countdown, màn hình countdown và lọc mốc sắp tới/quá hạn. |
| Thống kê | Cung cấp dữ liệu đầu vào từ task, lịch học và Pomodoro. | Phụ trách màn thống kê, biểu đồ và tổng hợp số liệu học tập. |
| Google Sign-In | Hỗ trợ luồng UI và điều hướng sau đăng nhập. | Phụ trách Firebase Authentication và cấu hình đăng nhập Google. |
| Firebase Realtime Database | Gọi đồng bộ dữ liệu từ app/repository khi có thay đổi dữ liệu học tập. | Phụ trách cấu hình Firebase Realtime Database, cấu trúc dữ liệu cloud và xử lý đồng bộ. |
| Gemini AI | Hỗ trợ màn chọn ảnh và đưa kết quả trích xuất vào lịch học. | Phụ trách gọi Gemini API, xử lý ảnh thời khóa biểu và parse dữ liệu lịch học. |
| OTP backend | Gọi API OTP từ ứng dụng Android. | Phụ trách dịch vụ Java HTTP Server gửi OTP qua SMTP. |
| Admin web | Gửi snapshot dữ liệu học tập từ app lên admin API. | Phụ trách web quản trị, giao diện admin, registry tài khoản, thông báo, lỗi hệ thống và dashboard quản trị. |
| API admin/mobile sync | Gọi API từ ứng dụng Android và xử lý phản hồi phía app. | Phụ trách thiết kế API, lưu dữ liệu quản trị và đồng bộ thông tin giữa app với admin web. |

## 4. Phạm Vi Công Việc Của THAI HO PHU GIA

- Xây dựng luồng chức năng chính trong ứng dụng Android.
- Quản lý lịch học, lịch thi, deadline và sự kiện cá nhân.
- Xử lý lưu trữ dữ liệu học tập local bằng SQLite.
- Xây dựng repository và các thao tác CRUD cho dữ liệu học tập.
- Xây dựng reminder local bằng alarm và notification.
- Xử lý Pomodoro, lưu phiên học và cập nhật tiến độ học tập.
- Tích hợp dữ liệu task, lịch học và Pomodoro vào dashboard.
- Hỗ trợ kết nối ứng dụng với Firebase, Gemini AI, OTP backend và admin web.

## 5. Phạm Vi Công Việc Của VO LE THIEN AN

- Thiết kế giao diện tổng thể của ứng dụng Android.
- Xây dựng các layout XML, drawable, theme, icon, bottom navigation và side menu.
- Phụ trách chức năng Task/To-do list gồm danh sách task, form task, card task và thao tác thêm/sửa/xóa/hoàn thành.
- Xây dựng countdown và màn thống kê học tập.
- Phụ trách Firebase Authentication, Google Sign-In và Firebase Realtime Database.
- Phụ trách Gemini AI để đọc lịch học từ ảnh thời khóa biểu.
- Xây dựng OTP backend gửi mã xác thực qua email.
- Xây dựng admin web và API admin/mobile sync.

## 6. Nguyên Tắc Phối Hợp

Các module trong dự án có liên kết chặt chẽ giữa giao diện, xử lý dữ liệu và backend. Vì vậy, một số chức năng có sự phối hợp giữa hai thành viên:

- Module giao diện do VO LE THIEN AN phụ trách chính, THAI HO PHU GIA hỗ trợ gắn sự kiện và tích hợp logic.
- Module dữ liệu local và luồng chức năng trong app do THAI HO PHU GIA phụ trách chính, VO LE THIEN AN hỗ trợ phần giao diện, đồng bộ cloud hoặc backend liên quan.
- Các module backend, Firebase, Gemini AI và admin web do VO LE THIEN AN phụ trách chính, THAI HO PHU GIA hỗ trợ phần gọi API từ ứng dụng Android.
