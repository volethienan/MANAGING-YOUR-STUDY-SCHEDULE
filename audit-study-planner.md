# Audit va ke hoach sua Study Planner

Ngay lap: 28/05/2026  
Vai tro danh gia: Senior Android Developer, Full-stack Developer, UI/UX Reviewer

## PHAN 1. Tong quan hien trang du an

Du an co 3 module chinh:

| Module | Hien trang |
|---|---|
| Android app `app` | Build duoc, chuc nang chinh da co: dang ky/OTP, Google login, lich, task, Pomodoro, thong ke, OCR lich bang Gemini, dong bo admin registry. Diem yeu nam o UX lich/task/deadline, reminder chua thuc su chay nen, card hien thi con thieu du lieu. |
| Admin web `admin-web` | Build duoc, co login admin, dashboard thong ke, quan ly tai khoan registry, thong bao, loi OTP/AI, audit log. Diem yeu la UI con don gian, thieu reset/cap lai tai khoan that su, chuc nang khoa phu thuoc app sync. |
| OTP backend `otp-backend` | Build duoc, co `/send-otp` va `/health`, gui mail qua SMTP env. Diem yeu bao mat: backend nhan OTP tu client, chua rate limit, CORS mo rong, parse JSON thu cong. |

Ket qua kiem tra build:

```text
.\gradlew.bat assembleDebug              SUCCESS
.\gradlew.bat -p admin-web build         SUCCESS
.\gradlew.bat -p otp-backend build       SUCCESS
```

## PHAN 2. Danh sach loi/chua on tim thay

| STT | Khu vuc | Van de | Muc do | File/component lien quan | Nguyen nhan du doan | Huong sua de xuat |
|---:|---|---|---|---|---|---|
| 1 | Lich Android | Card lich chua du thong tin, online link chua bam duoc | Cao | `MainActivity.java`, `item_event.xml` | `createEventRow()` chi render title/meta/note; `room` bi dung lan phong, dia diem, online | Nang `item_event.xml` thanh card nhieu dong: loai, gio, subject/noi dung, dia diem/link, ghi chu, reminder; neu `room` la URL thi set clickable mo browser |
| 2 | Lich Android | Deadline/ca nhan co filter nhung noi dung rong hoac khong dung ngu canh | Cao | `dialog_event.xml`, `MainActivity.showEventDialog()` | Form luon yeu cau "Mon hoc", label "Phong hoc", seed data khong co deadline/ca nhan mau | Doi label dong theo loai: lich hoc/thi dung mon + phong; deadline dung mon/noi dung + han nop/link; ca nhan dung noi dung + dia diem/link |
| 3 | Lich Android | Khong co field rieng cho link hoc online | Trung binh | `StudyEvent.java`, `StudyRepository.java` | Model chi co `room`, chua co `onlineLink` | Cach it pha cau truc: tam dung `room` cho "Phong/Dia diem/Link". Neu muon chuan hon, them `location` va `online_link`, tang `DB_VERSION`, dung `ALTER TABLE` an toan |
| 4 | Lich Android | Reminder lich chi luu du lieu, chua co nhac truoc lich that | Cao | `MainActivity.java`, `StudyRepository.java` | Khong thay `AlarmManager`, `BroadcastReceiver`, permission notification cho lich | Them scheduler nhe: `AlarmManager` + receiver + notification channel; huy/reschedule khi sua/xoa event |
| 5 | Lich Android | Canh bao trung lich dang tinh ca deadline nhu su kien chiem thoi gian | Trung binh | `StudyRepository.getConflicts()` | `getConflicts()` overlap moi loai; chi import OCR moi bo qua deadline | Quy dinh deadline co block lich hay khong. Khuyen nghi deadline khong chan lich, chi canh bao nhe neu cung khung gio |
| 6 | Lich Android | View ngay/3 ngay/tuan co nhung lich co dinh `1320dp`, de dai va chu bi cat | Trung binh | `screen_schedule.xml`, `WeekCalendarView.java` | Custom view ve text rat ngan, tuan nhieu cot nen title/room bi trim | Giu custom view nhung tang thong tin o list card ben duoi; voi day mode co the show full title/time/location trong block |
| 7 | Task | Form task qua thieu: chi nhap ten, trong khi model co deadline/priority/note/reminder/pomodoro | Cao | `dialog_task.xml`, `MainActivity.showTaskDialog()` | `showTaskDialog()` default subject "Ca nhan", due hom nay 23:59, priority trung binh | Mo rong dialog task: tieu de, mon/tag, deadline date-time, uu tien, note, nhac nho, so pomodoro du kien |
| 8 | Task UI | `item_task.xml` an meta/details nen task list chi con ten + marker | Trung binh | `item_task.xml` | `textMeta` va `textDetails` `visibility="gone"` du code co set text | Hien lai meta/details hoac lam card 2-3 dong: deadline, tag, priority, trang thai |
| 9 | Deadline | Deadline trong lich va deadline trong task dang tach roi | Cao | `StudyEvent.java`, `StudyTask.java` | Co `StudyEvent.TYPE_DEADLINE` va task `dueAt`, nhung khong sync nhau | Chon task la nguon chinh cua deadline; khi tao task co deadline thi tuy chon "Hien tren lich" tao event deadline lien ket bang `sourceTaskId` |
| 10 | Dashboard/Stats | Dashboard deadline chi lay task, lich deadline khong duoc tinh ro; stats bo qua ca nhan | Trung binh | `MainActivity.showDashboard()`, `MainActivity.showStats()` | Thong ke dang chia task/event rieng, event breakdown chi hoc/thi/deadline | Dashboard nen hien thi "Deadline gan nhat" tu ca task va event deadline; stats them ca nhan |
| 11 | Pomodoro | Co gan task, luu session, am nen; lich su chi toast tong phut | Trung binh | `MainActivity.handlePomodoroEnd()`, `MainActivity.showPomodoroHistory()` | Co bang `pomodoro_sessions` nhung khong co man/list lich su | Them repository query sessions gan day, dialog/bottom sheet lich su theo task/tag/thoi luong |
| 12 | Pomodoro notification | Android 13+ co the khong hien thong bao | Cao | `AndroidManifest.xml`, `MainActivity.sendPomodoroNotification()` | Thieu `POST_NOTIFICATIONS` va runtime request permission | Them permission va request truoc khi gui notification |
| 13 | UI Android | Font handwriting dung toan app de kho doc, nhieu card height co dinh de cat text | Trung binh | `themes.xml`, nhieu layout XML | Dung `@font/study_handwriting` toan cuc; nhieu `height=54dp/64dp/132dp` | Dung Mali/Inter-like cho body, handwriting chi lam diem nhan; doi card sang `wrap_content`, `minHeight`, `maxLines` hop ly |
| 14 | Admin web | Chua co reset/cap lai tai khoan thuc chat | Trung binh | `AdminWebServer.java`, `app.js` | Admin chi quan ly registry, khong truy cap SQLite auth tren thiet bi/Firebase Auth | Neu giu pham vi: ghi ro "registry only". Neu bo sung: them flow reset qua email/mobile sync, khong hua xoa tai khoan that |
| 15 | Admin web UI | Dashboard dung chuc nang nhung chua Studygram/professional | Thap | `index.html`, `styles.css` | UI dang la admin toi gian | Nang stat card, trang thai he thong, bo loc users/issues, empty state, tone pastel nhung gon |
| 16 | OTP backend | Backend nhan OTP code tu client, chua rate limit | Cao | `OtpMailServer.java` | App sinh OTP local roi gui backend mailer | Trong pham vi demo co the chap nhan, nhung nen them rate limit theo email/IP va validate email/code; tot hon la backend tu sinh OTP |
| 17 | OTP backend | CORS `*`, parse JSON bang regex, loi SMTP tra thong tin tho | Trung binh | `OtpMailServer.java` | Server Java nhe, khong framework | Gioi han origin neu deploy, che bot loi SMTP, dung JSON parser nho hoac kiem tra input chat hon |

## PHAN 3. Ke hoach sua theo tung giai doan

### Phase 1: Sua loi hien thi va loi chuc nang quan trong

1. Sua lich truoc: `dialog_event.xml`, `item_event.xml`, `MainActivity.createEventRow()`, `eventDetailText()`.
2. Cho `room` hoat dong nhu "Phong hoc / Dia diem / Link online"; neu la URL thi bam mo duoc.
3. Doi label/validation theo `type`: deadline/ca nhan khong bat buoc "mon hoc" theo nghia hoc phan.
4. Lam card lich du thong tin: tieu de, loai, gio, mon/noi dung, dia diem/link, ghi chu, reminder, canh bao trung.
5. Sua logic conflict: deadline khong nen mac dinh chan lich hoc.
6. Bo sung notification permission va alarm reminder that cho event.

### Phase 2: Nang cap trai nghiem nguoi dung Android

1. Mo rong `dialog_task.xml` va `showTaskDialog()` de nhap du deadline, uu tien, note, reminder, estimated pomodoro.
2. Hien lai meta/details trong `item_task.xml`.
3. Thong nhat deadline: task la nguon chinh; event deadline la ban hien thi tren lich.
4. Dashboard/stat lay deadline tu ca task va event deadline.
5. Ra soat font: body dung font de doc hon, handwriting chi dung cho heading/brand.
6. Chinh cac card co dinh height sang `wrap_content/minHeight` de tranh cat tieng Viet.

### Phase 3: Nang cap admin-web

1. UI: nang `styles.css` theo Studygram admin: pastel nhe, card thong ke ro, table de scan, trang thai bang badge.
2. Users: them filter "dang khoa", "chua xac thuc", "provider"; them confirm truoc xoa/khoa.
3. Announcements: them preview trang thai active/inactive ro hon.
4. Issues: loc OTP/AI/open/resolved.
5. Reset/cap lai tai khoan: chi nen lam neu xac dinh backend auth trung tam. Voi source hien tai, nen ghi la "khong reset truc tiep duoc vi tai khoan app luu local/Firebase".

### Phase 4: Kiem thu toan he thong

1. Build Android, admin-web, otp-backend.
2. Test dang ky email OTP, resend OTP, reset password.
3. Test Google login va sync admin.
4. Test them/sua/xoa lich theo du 4 loai.
5. Test link online, reminder, conflict.
6. Test task/deadline/Pomodoro/dashboard/stat.
7. Test admin lock/unlock/delete registry, announcements, issues, audit.
8. Test tren emulator va it nhat mot man nho.

## PHAN 4. Checklist test sau khi sua

- [ ] Dang ky tai khoan moi, nhan OTP, nhap sai OTP qua 5 lan, nhap OTP het han.
- [ ] Dang nhap email/password, dang nhap Google, logout/login lai.
- [ ] Tao lich hoc co phong hoc, sua gio, xoa lich.
- [ ] Tao lich online voi link Zoom/Meet, mo link tu card va dialog chi tiet.
- [ ] Tao lich thi, card hien thi phong, ghi chu, nhac nho.
- [ ] Tao deadline tren lich, khong bi card rong, khong bat nhap "mon hoc" vo ly.
- [ ] Tao viec ca nhan, card hien thi noi dung/dia diem/ghi chu phu hop.
- [ ] Kiem tra filter Tat ca/Lich hoc/Thi/Deadline/Ca nhan.
- [ ] Kiem tra view ngay, 3 ngay, tuan voi 0, 1, nhieu event va event trung gio.
- [ ] Tao 2 lich trung gio, thay canh bao truoc khi luu va badge trung tren card.
- [ ] Bat reminder, dong app, toi thoi diem nhac van co notification.
- [ ] Tao task co deadline/priority/note/pomodoro, kiem tra list task hien thi du.
- [ ] Kiem tra deadline task co xuat hien tren lich neu bat "Hien tren lich".
- [ ] Chay Pomodoro gan voi task, pause/resume/reset/skip, kiem tra am nen va notification ket thuc.
- [ ] Xem lich su Pomodoro, kiem tra so phut dashboard/stat cap nhat dung.
- [ ] Admin login, xem users, search, filter locked/verified.
- [ ] Khoa user tren admin, app login/sync phai bi chan ro rang.
- [ ] Tao/tat/xoa thong bao, app nhan thong bao active.
- [ ] Tao loi OTP/AI tu app, admin hien thi dung loai va trang thai.
- [ ] Goi `GET /health` OTP backend tra `{"ok":true}`.

## PHAN 5. De xuat cach sua code

Khong nen viet lai du an. Sua tap trung cac file sau:

| Muc sua | File nen sua | Ghi chu ky thuat |
|---|---|---|
| Card lich day du | `item_event.xml`, `MainActivity.createEventRow()` | Them TextView cho type/time/subject/location/note/reminder; detect URL bang `Patterns.WEB_URL` |
| Link hoc online | `MainActivity.createEventRow()`, `eventDetailText()` | Tam dung field `room`; neu bat dau bang `http://` hoac `https://`, mo `Intent.ACTION_VIEW` |
| Form lich theo loai | `dialog_event.xml`, `MainActivity.showEventDialog()` | Them listener cho spinner type de doi hint: "Mon hoc", "Noi dung deadline", "Dia diem/link" |
| Reminder that | `AndroidManifest.xml`, them receiver Java, `MainActivity`/helper | Them `POST_NOTIFICATIONS`, `AlarmManager`, cancel/reschedule theo event id |
| Deadline thong nhat | `StudyTask.java`, `StudyEvent.java`, `StudyRepository.java` | Neu them field: `events.source_task_id TEXT`, `tasks.show_on_calendar INTEGER DEFAULT 0`; tang `DB_VERSION` len 3 va `ALTER TABLE` bang `ensureColumn()` |
| Task form day du | `dialog_task.xml`, `MainActivity.showTaskDialog()` | Them date/time picker, priority spinner, note, reminder, estimated pomodoro |
| Task card | `item_task.xml`, `MainActivity.createTaskRow()` | Bo `visibility="gone"` cho meta/details, tang height thanh `wrap_content` |
| Dashboard/stat | `MainActivity.showDashboard()`, `showStats()` | Tinh deadline gan nhat tu task + event deadline; them count ca nhan |
| Pomodoro history | `StudyRepository.java`, `MainActivity.showPomodoroHistory()` | Them query `getRecentPomodoroSessions()`, hien thi dialog list thay vi toast |
| Admin UI | `admin-web/.../index.html`, `styles.css`, `app.js` | Them filter, confirm action, card thong ke dep hon |
| Admin reset | `AdminWebServer.java` | Chi them neu co backend auth trung tam; hien tai khong nen gia lap reset mat khau |
| OTP hardening | `OtpMailServer.java` | Validate email/code, rate limit don gian bang memory map, che loi SMTP chi tiet |

## Ket luan

Du an da co nen tot va build sach. Phan can uu tien nhat de bao ve thuyet phuc la lich/deadline/task: lam form dung ngu canh, card hien thi du thong tin, link online bam duoc, reminder chay that, va thong nhat deadline giua task voi lich.

## PHAN 6. Trang thai sua chua hien tai

Ngay cap nhat: 29/05/2026

### Da sua trong source

| Nhom | Trang thai | File chinh |
|---|---|---|
| Card lich day du thong tin | Da sua | `item_event.xml`, `MainActivity.java` |
| Nhan loai lich tieng Viet | Da sua spinner/card/notification de hien "Lich hoc", "Lich thi", "Deadline", "Ca nhan" thay vi ma noi bo `study/exam/personal` | `MainActivity.java` |
| Link hoc online bam mo duoc | Da sua bang cach dung tam field `room` nhu phong/dia diem/link | `MainActivity.createEventRow()` |
| Link trong dialog chi tiet lich | Da them hanh dong "Mo link online" khi su kien co URL, khong chi bam duoc tren card list | `MainActivity.showEventActions()` |
| Form lich theo loai | Da sua hint va validation co ban cho hoc/thi/deadline/ca nhan | `MainActivity.showEventDialog()` |
| Canh bao trung lich | Da sua de deadline khong chan lich hoc/lich thi | `StudyRepository.getConflicts()` |
| Reminder lich/task | Da them receiver, permission notification, alarm schedule/cancel/reschedule | `AndroidManifest.xml`, `EventReminderReceiver.java`, `MainActivity.java` |
| Validation thoi diem reminder | Da chan luu reminder task trong qua khu va reminder lich co thoi diem nhac truoc da qua, tranh UI bao da bat nhac nhung thuc te khong len alarm | `MainActivity.showTaskDialog()`, `MainActivity.showEventDialog()` |
| Quyen notification reminder | Da them thong bao ro khi nguoi dung tu choi quyen notification va nut mo cai dat thong bao app; neu cap quyen, app reschedule reminder | `MainActivity.setupNotificationPermissionLauncher()` |
| Notification reminder UX | Da them `contentIntent` de bam notification mo lai app va `BigTextStyle` de noi dung dai khong bi cat qua som | `EventReminderReceiver.java` |
| Deadline task hien tren lich | Da them `tasks.show_on_calendar` va `events.source_task_id`, co sync task -> event deadline | `StudyTask.java`, `StudyEvent.java`, `StudyRepository.java`, `MainActivity.java` |
| Xoa deadline lien ket task | Da dong bo nguoc: neu xoa event deadline tao tu task tren lich, task se tat `show_on_calendar` de khong bi tao lai ngoai y muon | `StudyRepository.java`, `MainActivity.confirmDeleteEvent()` |
| Migration SQLite | Da tang `DB_VERSION = 3` va them column an toan bang `ensureColumn`/`ensureTaskColumn`/`ensureEventColumn` | `StudyRepository.java` |
| Tuong thich loai lich cu | Da normalize cac ma `study/exam/deadline/personal` ve loai hien tai khi doc model va khi khoi tao repository, tranh filter/card bi sai tren may da co du lieu cu | `StudyEvent.java`, `StudyRepository.java`, `ExampleUnitTest.java` |
| Form task | Da mo rong field title, subject/tag, deadline, priority, note, important/urgent, reminder, estimated pomodoro, hien tren lich | `dialog_task.xml`, `MainActivity.showTaskDialog()` |
| Card task | Da hien meta/details va bo gioi han chieu cao de giam cat chu | `item_task.xml`, `MainActivity.createTaskRow()` |
| Dashboard/stat | Da tinh deadline gan nhat tu ca task va event deadline, them breakdown ca nhan | `MainActivity.java` |
| Deadline task hoan thanh | Da xoa/bo qua event deadline lien ket khi task da hoan thanh, tranh dashboard/lich van bao deadline da xong nhu viec sap toi | `StudyRepository.syncTaskDeadlineEvent()`, `MainActivity.findNearestDeadlineEvent()` |
| Dashboard status fallback | Da sua fallback `study` thanh "Hoc tap" khi trang thai ca nhan rong | `MainActivity.shortStatus()` |
| Pomodoro history | Da hien dialog lich su phien gan day | `StudyRepository.java`, `MainActivity.showPomodoroHistory()` |
| Font/theme Android | Da chuyen ve font de doc hon va sua target API lint cho theme | `themes.xml`, `values-night/themes.xml` |
| Chieu cao lich ngay/3 ngay/tuan | Da bo `1320dp` co dinh trong XML, `WeekCalendarView` tu do chieu cao theo che do xem de giam cat chu va giam khoang trong | `screen_schedule.xml`, `WeekCalendarView.java` |
| Dashboard Android label | Da sua label mau `study` thanh tieng Viet de tranh lo nhan ky thuat khi demo | `screen_dashboard.xml` |
| Dashboard Android card | Da doi cac card thong ke/deadline/lich tiep theo/Pomodoro tu chieu cao co dinh sang `wrap_content` + `minHeight`, giam nguy co cat chu tieng Viet tren man nho | `screen_dashboard.xml` |
| Du lieu mau de demo filter | Da them lich deadline, lich ca nhan va lich online co URL that de filter/card/link co du noi dung khi mo app moi | `StudyRepository.seedIfNeeded()` |
| Du lieu mau sau dang nhap | Da seed demo cho database theo email neu tai khoan moi chua co lich/task, tranh man hinh trong khi bao ve | `StudyRepository.seedDemoDataIfEmpty()`, `MainActivity.activateStudyRepository()` |
| Xin quyen notification | Da chan viec goi permission request lap lai khi reschedule nhieu reminder tren Android 13+ | `MainActivity.java` |
| Cau hinh backend local | Da doi `local.properties` sang `OTP_BACKEND_URL=http://10.0.2.2:8080` va them `ADMIN_BACKEND_URL=http://10.0.2.2:8090` de demo bang emulator | `local.properties` |
| Admin dashboard UI | Da nang style, stat card, list row, filter/select, confirm thao tac | `admin-web/src/main/resources/web/*` |
| Admin action feedback | Da them vung trang thai thao tac cho lam moi/khoa/reset/xoa/thong bao, giup admin biet thanh cong/loi ngay tren UI | `index.html`, `app.js`, `styles.css` |
| Admin API sanitize | Da gioi han name/email/provider khi mobile sync va gioi han title/body thong bao o server, khong chi dua vao `maxlength` phia web | `AdminWebServer.java` |
| Admin yeu cau reset mat khau | Da them co `passwordResetRequested`: admin danh dau/go, app nhan khi sync va dan nguoi dung den man Quen mat khau/OTP. Sau khi app reset thanh cong, mobile goi endpoint de admin tu go co. Admin dashboard co stat/filter rieng cho tai khoan can reset. Khong reset truc tiep mat khau local tren thiet bi. | `AdminWebServer.java`, `index.html`, `app.js`, `AdminPortalClient.java`, `MainActivity.java` |
| Admin announcement -> Android | Da them API client doc thong bao active moi nhat va show trong app | `AdminPortalClient.java`, `MainActivity.java` |
| Admin issue log | Da chuan hoa issue type `otp/ai/general`, gioi han email/message o ca app client va admin server de tranh log qua dai lam vo UI | `AdminPortalClient.java`, `AdminWebServer.java` |
| OTP loi tren Android | Da tach thong bao than thien cho nguoi dung va chi tiet ky thuat trong nut rieng; van gui loi ve admin issue log | `MainActivity.showOtpSendError()` |
| OTP backend hardening | Da them validate email/OTP, rate limit memory, gioi han purpose, loi SMTP chung cho client | `OtpMailServer.java` |

### Da kiem tra bang lenh

```text
.\gradlew.bat assembleDebug              SUCCESS
.\gradlew.bat testDebugUnitTest          SUCCESS
.\gradlew.bat lintDebug                  SUCCESS
.\gradlew.bat -p admin-web build         SUCCESS
.\gradlew.bat -p otp-backend build       SUCCESS
GET http://localhost:18080/health        200 {"ok":true}
GET http://localhost:18091/health        200 {"ok":true}
POST /api/auth/login admin-web           200 {"ok":true,"username":"admin"}
GET /api/stats admin-web sau login       200 JSON stats
Admin requestReset smoke test            sync lan 2 tra passwordResetRequested=true
Admin reset-complete smoke test          sync lan 3 tra passwordResetRequested=false
```

### Chua xac minh duoc tren moi truong hien tai

- Chua test runtime Android tren emulator/thiet bi that.
- Chua test notification khi app dong/man hinh khoa.
- Chua test SMTP that vi can bien moi truong SMTP that.
- Chua test Google login/Firebase tren thiet bi that.
- Chua test AI OCR doc lich voi API key/network that.
- Chua reset truc tiep mat khau tu admin-web vi source hien tai khong co auth trung tam cho tai khoan local; da co co yeu cau reset, app nhac nguoi dung tu reset bang OTP va bao admin go co sau khi reset thanh cong.

### Viec nen lam tiep neu co nguoi lam chung

1. Cai APK len emulator, test toan bo checklist PHAN 4.
2. Neu notification khong bat o Android 12+ do exact alarm policy, chuyen sang WorkManager hoac hien dialog huong dan bat quyen alarm.
3. Them field rieng `online_link` va `location` chi khi muon lam data model sach hon; ban hien tai da dung `room` de giam rui ro migration.
4. Neu muon admin reset mat khau truc tiep that, can chuyen auth local thanh backend auth trung tam hoac them endpoint mobile chap nhan reset token do admin phat hanh.
