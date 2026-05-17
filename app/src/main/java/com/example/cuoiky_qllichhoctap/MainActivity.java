package com.example.cuoiky_qllichhoctap;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cuoiky_qllichhoctap.data.AuthRepository;
import com.example.cuoiky_qllichhoctap.data.GeminiScheduleExtractor;
import com.example.cuoiky_qllichhoctap.data.StudyRepository;
import com.example.cuoiky_qllichhoctap.model.AuthUser;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;
import com.example.cuoiky_qllichhoctap.ui.WeekCalendarView;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int SCREEN_DASHBOARD = 0;
    private static final int SCREEN_SCHEDULE = 1;
    private static final int SCREEN_TASKS = 2;
    private static final int SCREEN_POMODORO = 3;
    private static final int SCREEN_STATS = 4;

    private StudyRepository repository;
    private AuthRepository authRepository;
    private FrameLayout contentFrame;
    private LinearLayout bottomNav;
    private CountDownTimer pomodoroTimer;
    private long pomodoroRemainingMillis = 25L * 60L * 1000L;
    private boolean pomodoroRunning;
    private long scheduleWeekStartMillis = DateTimeUtils.startOfWeek(System.currentTimeMillis());
    private String scheduleFilter = "Tất cả";
    private Uri pendingCameraUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new StudyRepository(this);
        authRepository = new AuthRepository(this);
        contentFrame = findViewById(R.id.contentFrame);
        bottomNav = findViewById(R.id.bottomNav);
        setupImageLaunchers();
        setupBottomNav();

        if (repository.isFirstOpen()) {
            showOnboarding();
        } else if (authRepository.isLoggedIn()) {
            syncProfileFromAuth(authRepository.getCurrentUser());
            showDashboard();
        } else {
            showLogin();
        }
    }

    @Override
    protected void onDestroy() {
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }
        super.onDestroy();
    }

    private View inflateScreen(int layoutId, boolean showBottomNav, int selectedNav) {
        contentFrame.removeAllViews();
        View screen = getLayoutInflater().inflate(layoutId, contentFrame, false);
        contentFrame.addView(screen);
        bottomNav.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        if (showBottomNav) {
            updateSelectedNav(selectedNav);
        }
        return screen;
    }

    private void setupBottomNav() {
        findViewById(R.id.navDashboard).setOnClickListener(v -> showDashboard());
        findViewById(R.id.navSchedule).setOnClickListener(v -> showSchedule("Tất cả"));
        findViewById(R.id.navTasks).setOnClickListener(v -> showTasks("Tất cả"));
        findViewById(R.id.navPomodoro).setOnClickListener(v -> showPomodoro());
        findViewById(R.id.navStats).setOnClickListener(v -> showStats());
    }

    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && pendingCameraUri != null) {
                processScheduleImage(pendingCameraUri);
            }
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                processScheduleImage(uri);
            }
        });
    }

    private void updateSelectedNav(int selected) {
        int[] ids = {R.id.navDashboard, R.id.navSchedule, R.id.navTasks, R.id.navPomodoro, R.id.navStats};
        for (int i = 0; i < ids.length; i++) {
            MaterialButton button = findViewById(ids[i]);
            boolean active = i == selected;
            button.setTextColor(getColor(active ? R.color.ink : R.color.muted));
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(active ? R.color.yellow : android.R.color.transparent)));
        }
    }

    private void showOnboarding() {
        View screen = inflateScreen(R.layout.screen_onboarding, false, SCREEN_DASHBOARD);
        screen.findViewById(R.id.btnStart).setOnClickListener(v -> {
            repository.finishOnboarding();
            showLogin();
        });
    }

    private void showLogin() {
        View screen = inflateScreen(R.layout.screen_login, false, SCREEN_DASHBOARD);
        AuthUser currentUser = authRepository.getCurrentUser();
        EditText email = screen.findViewById(R.id.inputEmail);
        EditText password = screen.findViewById(R.id.inputPassword);
        if (currentUser != null) {
            email.setText(currentUser.getEmail());
        }

        View.OnClickListener login = v -> {
            if (isBlank(email) || isBlank(password)) {
                toast("Vui lòng nhập email và mật khẩu");
                return;
            }
            if (!isValidEmail(textOf(email))) {
                toast("Email chưa đúng định dạng");
                return;
            }
            if (textOf(password).length() < 6) {
                toast("Mật khẩu cần ít nhất 6 ký tự");
                return;
            }
            try {
                AuthUser user = authRepository.login(textOf(email), textOf(password));
                syncProfileFromAuth(user);
                repository.setLoggedIn(true);
                showDashboard();
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        };
        screen.findViewById(R.id.btnLogin).setOnClickListener(login);
        screen.findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> showGoogleSetupDialog());
        screen.findViewById(R.id.textForgotPassword).setOnClickListener(v -> showForgotPassword());
        screen.findViewById(R.id.textGoRegister).setOnClickListener(v -> showRegister());
    }

    private void showRegister() {
        View screen = inflateScreen(R.layout.screen_register, false, SCREEN_DASHBOARD);
        EditText name = screen.findViewById(R.id.inputName);
        EditText email = screen.findViewById(R.id.inputEmail);
        EditText password = screen.findViewById(R.id.inputPassword);
        EditText confirm = screen.findViewById(R.id.inputConfirmPassword);
        CheckBox terms = screen.findViewById(R.id.checkTerms);

        screen.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            if (isBlank(name) || isBlank(email) || isBlank(password)) {
                toast("Vui lòng nhập đủ thông tin");
                return;
            }
            if (!isValidEmail(textOf(email))) {
                toast("Email chưa đúng định dạng");
                return;
            }
            if (textOf(password).length() < 6) {
                toast("Mật khẩu cần ít nhất 6 ký tự");
                return;
            }
            if (!password.getText().toString().equals(confirm.getText().toString())) {
                toast("Mật khẩu xác nhận chưa khớp");
                return;
            }
            if (!terms.isChecked()) {
                toast("Bạn cần đồng ý điều khoản sử dụng");
                return;
            }
            try {
                String otp = authRepository.beginRegistration(textOf(name), textOf(email), textOf(password));
                showRegisterOtp(textOf(email), textOf(name), textOf(password), otp);
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textGoLogin).setOnClickListener(v -> showLogin());
    }

    private void showRegisterOtp(String email, String name, String password, String otp) {
        View screen = inflateScreen(R.layout.screen_otp, false, SCREEN_DASHBOARD);
        setText(screen, R.id.textOtpSubtitle, "Nhập mã 6 số để kích hoạt tài khoản");
        setText(screen, R.id.textOtpEmail, email);
        EditText inputOtp = screen.findViewById(R.id.inputOtp);
        showDemoOtpDialog(otp, "đăng ký");

        screen.findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            if (isBlank(inputOtp) || textOf(inputOtp).length() != 6) {
                toast("Vui lòng nhập đủ 6 số OTP");
                return;
            }
            try {
                AuthUser user = authRepository.verifyRegistrationOtp(email, textOf(inputOtp));
                syncProfileFromAuth(user);
                repository.finishOnboarding();
                repository.setLoggedIn(true);
                toast("Đăng ký thành công");
                showDashboard();
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });

        screen.findViewById(R.id.btnResendOtp).setOnClickListener(v -> {
            try {
                String newOtp = authRepository.beginRegistration(name, email, password);
                showDemoOtpDialog(newOtp, "đăng ký");
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBack).setOnClickListener(v -> showRegister());
    }

    private void showForgotPassword() {
        View screen = inflateScreen(R.layout.screen_forgot_password, false, SCREEN_DASHBOARD);
        EditText email = screen.findViewById(R.id.inputEmail);
        screen.findViewById(R.id.btnSendOtp).setOnClickListener(v -> {
            if (isBlank(email)) {
                toast("Vui lòng nhập email");
                return;
            }
            if (!isValidEmail(textOf(email))) {
                toast("Email chưa đúng định dạng");
                return;
            }
            try {
                String otp = authRepository.beginPasswordReset(textOf(email));
                showResetPassword(textOf(email), otp);
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBackLogin).setOnClickListener(v -> showLogin());
    }

    private void showResetPassword(String email, String otp) {
        View screen = inflateScreen(R.layout.screen_reset_password, false, SCREEN_DASHBOARD);
        setText(screen, R.id.textResetEmail, email);
        EditText inputOtp = screen.findViewById(R.id.inputOtp);
        EditText password = screen.findViewById(R.id.inputPassword);
        EditText confirm = screen.findViewById(R.id.inputConfirmPassword);
        showDemoOtpDialog(otp, "đặt lại mật khẩu");

        screen.findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            if (isBlank(inputOtp) || isBlank(password) || isBlank(confirm)) {
                toast("Vui lòng nhập đủ OTP và mật khẩu mới");
                return;
            }
            if (textOf(password).length() < 6) {
                toast("Mật khẩu cần ít nhất 6 ký tự");
                return;
            }
            if (!textOf(password).equals(textOf(confirm))) {
                toast("Mật khẩu xác nhận chưa khớp");
                return;
            }
            try {
                authRepository.resetPassword(email, textOf(inputOtp), textOf(password));
                toast("Đã cập nhật mật khẩu");
                showLogin();
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBackLogin).setOnClickListener(v -> showLogin());
    }

    private void showDashboard() {
        View screen = inflateScreen(R.layout.screen_dashboard, true, SCREEN_DASHBOARD);
        UserProfile profile = repository.getProfile();
        List<StudyTask> tasks = repository.getTasks();
        List<StudyEvent> events = repository.getEvents();

        ((TextView) screen.findViewById(R.id.textGreeting)).setText("Chào " + firstName(profile.getName()));
        ((TextView) screen.findViewById(R.id.textTodaySummary)).setText("Hôm nay bạn có " + countTodayTasks(tasks) + " việc cần ưu tiên");

        StudyEvent nextEvent = findNextEvent(events);
        if (nextEvent == null) {
            setText(screen, R.id.textNextEvent, "Chưa có lịch sắp tới");
            setText(screen, R.id.textNextEventMeta, "Thêm lịch học để bắt đầu theo dõi");
        } else {
            setText(screen, R.id.textNextEvent, nextEvent.getTitle());
            setText(screen, R.id.textNextEventMeta, DateTimeUtils.formatDayLabel(nextEvent.getStartAt()) + " • " + DateTimeUtils.formatTime(nextEvent.getStartAt()) + " - " + DateTimeUtils.formatTime(nextEvent.getEndAt()) + " • " + nextEvent.getRoom());
        }

        StudyTask nextTask = findNextTask(tasks);
        setText(screen, R.id.textDeadline, nextTask == null ? "Không có" : nextTask.getTitle());
        setText(screen, R.id.textTodayTasks, countTodayTasks(tasks) + " task");

        LinearLayout quickStats = screen.findViewById(R.id.quickStats);
        quickStats.removeAllViews();
        quickStats.addView(statBlock("Hoàn thành", String.valueOf(countCompleted(tasks))));
        quickStats.addView(statBlock("Quá hạn", String.valueOf(countOverdue(tasks))));
        quickStats.addView(statBlock("Tập trung", repository.getFocusMinutes() + "m"));

        LinearLayout priorityTasks = screen.findViewById(R.id.priorityTasks);
        priorityTasks.removeAllViews();
        for (StudyTask task : topPriorityTasks(tasks)) {
            priorityTasks.addView(createTaskRow(task, false, priorityTasks));
        }
        if (priorityTasks.getChildCount() == 0) {
            priorityTasks.addView(emptyState("Không còn task ưu tiên. Trang vở hôm nay khá nhẹ."));
        }

        screen.findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());
        screen.findViewById(R.id.btnAddEvent).setOnClickListener(v -> showEventDialog(null, this::showDashboard));
        screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, this::showDashboard));
        screen.findViewById(R.id.btnOcr).setOnClickListener(v -> showImageImportOptions());
    }

    private void showSchedule(String filter) {
        scheduleFilter = filter;
        View screen = inflateScreen(R.layout.screen_schedule, true, SCREEN_SCHEDULE);
        setupScheduleFilters(screen, filter);
        setText(screen, R.id.textWeekRange, DateTimeUtils.formatWeekRange(scheduleWeekStartMillis));
        screen.findViewById(R.id.btnPrevWeek).setOnClickListener(v -> {
            scheduleWeekStartMillis = DateTimeUtils.addDays(scheduleWeekStartMillis, -7);
            showSchedule(scheduleFilter);
        });
        screen.findViewById(R.id.btnThisWeek).setOnClickListener(v -> {
            scheduleWeekStartMillis = DateTimeUtils.startOfWeek(System.currentTimeMillis());
            showSchedule(scheduleFilter);
        });
        screen.findViewById(R.id.btnNextWeek).setOnClickListener(v -> {
            scheduleWeekStartMillis = DateTimeUtils.addDays(scheduleWeekStartMillis, 7);
            showSchedule(scheduleFilter);
        });

        List<StudyEvent> visibleEvents = visibleWeekEvents(filter);
        WeekCalendarView weekCalendar = screen.findViewById(R.id.weekCalendar);
        weekCalendar.setWeekStartMillis(scheduleWeekStartMillis);
        weekCalendar.setEvents(visibleEvents, conflictIds(visibleEvents));
        weekCalendar.setOnEventClickListener(this::showEventActions);

        LinearLayout eventList = screen.findViewById(R.id.eventList);
        eventList.removeAllViews();

        for (StudyEvent event : visibleEvents) {
            View row = createEventRow(event, eventList);
            row.setOnClickListener(v -> showEventActions(event));
            eventList.addView(row);
        }
        if (eventList.getChildCount() == 0) {
            eventList.addView(emptyState("Chưa có sự kiện phù hợp"));
        }
        screen.findViewById(R.id.btnAddEvent).setOnClickListener(v -> showEventDialog(null, () -> showSchedule(filter)));
        screen.findViewById(R.id.btnImportImage).setOnClickListener(v -> showImageImportOptions());
    }

    private void setupScheduleFilters(View screen, String active) {
        bindFilter(screen, R.id.filterAll, "Tất cả", active, () -> showSchedule("Tất cả"));
        bindFilter(screen, R.id.filterStudy, StudyEvent.TYPE_STUDY, active, () -> showSchedule(StudyEvent.TYPE_STUDY));
        bindFilter(screen, R.id.filterExam, StudyEvent.TYPE_EXAM, active, () -> showSchedule(StudyEvent.TYPE_EXAM));
        bindFilter(screen, R.id.filterDeadline, StudyEvent.TYPE_DEADLINE, active, () -> showSchedule(StudyEvent.TYPE_DEADLINE));
    }

    private List<StudyEvent> visibleWeekEvents(String filter) {
        List<StudyEvent> result = new ArrayList<>();
        for (StudyEvent event : repository.getEvents()) {
            if (!DateTimeUtils.isSameWeek(event.getStartAt(), scheduleWeekStartMillis)) {
                continue;
            }
            if (!"Tất cả".equals(filter) && !event.getType().equals(filter)) {
                continue;
            }
            result.add(event);
        }
        return result;
    }

    private Set<String> conflictIds(List<StudyEvent> events) {
        Set<String> ids = new HashSet<>();
        for (StudyEvent event : events) {
            if (repository.hasConflict(event)) {
                ids.add(event.getId());
            }
        }
        return ids;
    }

    private void showTasks(String filter) {
        View screen = inflateScreen(R.layout.screen_tasks, true, SCREEN_TASKS);
        setupTaskFilters(screen, filter);
        LinearLayout taskList = screen.findViewById(R.id.taskList);
        taskList.removeAllViews();

        for (StudyTask task : repository.getTasks()) {
            if (!matchesTaskFilter(task, filter)) {
                continue;
            }
            View row = createTaskRow(task, true, taskList);
            row.setOnClickListener(v -> showTaskActions(task));
            taskList.addView(row);
        }
        if (taskList.getChildCount() == 0) {
            taskList.addView(emptyState("Chưa có công việc phù hợp"));
        }
        screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, () -> showTasks(filter)));
    }

    private void setupTaskFilters(View screen, String active) {
        bindFilter(screen, R.id.filterAll, "Tất cả", active, () -> showTasks("Tất cả"));
        bindFilter(screen, R.id.filterToday, "Hôm nay", active, () -> showTasks("Hôm nay"));
        bindFilter(screen, R.id.filterSoon, "Sắp hạn", active, () -> showTasks("Sắp hạn"));
        bindFilter(screen, R.id.filterOverdue, "Quá hạn", active, () -> showTasks("Quá hạn"));
    }

    private void showPomodoro() {
        View screen = inflateScreen(R.layout.screen_pomodoro, true, SCREEN_POMODORO);
        TextView timer = screen.findViewById(R.id.textTimer);
        TextView mode = screen.findViewById(R.id.textMode);
        MaterialButton startPause = screen.findViewById(R.id.btnStartPause);
        updatePomodoroUi(timer, mode, startPause);

        startPause.setOnClickListener(v -> {
            if (pomodoroRunning) {
                pausePomodoro();
            } else {
                startPomodoro(timer, mode, startPause);
            }
            updatePomodoroUi(timer, mode, startPause);
        });
        screen.findViewById(R.id.btnReset).setOnClickListener(v -> {
            resetPomodoro();
            updatePomodoroUi(timer, mode, startPause);
        });
        setText(screen, R.id.textSessions, repository.getFocusSessions() + " phiên\nhoàn thành");
        setText(screen, R.id.textFocusMinutes, repository.getFocusMinutes() + " phút\ntập trung");
    }

    private void startPomodoro(TextView timer, TextView mode, MaterialButton startPause) {
        pomodoroRunning = true;
        pomodoroTimer = new CountDownTimer(pomodoroRemainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                pomodoroRemainingMillis = millisUntilFinished;
                updatePomodoroUi(timer, mode, startPause);
            }

            @Override
            public void onFinish() {
                repository.addFocusSession(25);
                pomodoroRunning = false;
                pomodoroRemainingMillis = 25L * 60L * 1000L;
                toast("Hoàn thành một phiên Pomodoro");
                showPomodoro();
            }
        }.start();
    }

    private void pausePomodoro() {
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }
        pomodoroRunning = false;
    }

    private void resetPomodoro() {
        pausePomodoro();
        pomodoroRemainingMillis = 25L * 60L * 1000L;
    }

    private void updatePomodoroUi(TextView timer, TextView mode, MaterialButton startPause) {
        long totalSeconds = pomodoroRemainingMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        timer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        mode.setText(pomodoroRunning ? "Đang tập trung" : "Học tập");
        startPause.setText(pomodoroRunning ? "Tạm dừng" : "Bắt đầu");
    }

    private void showStats() {
        View screen = inflateScreen(R.layout.screen_stats, true, SCREEN_STATS);
        LinearLayout stats = screen.findViewById(R.id.statsContent);
        List<StudyTask> tasks = repository.getTasks();
        stats.addView(bigStat("Tỷ lệ hoàn thành", completionRate(tasks) + "%", R.drawable.bg_card_mint));
        stats.addView(bigStat("Task quá hạn", countOverdue(tasks) + " task", R.drawable.bg_danger_soft));
        stats.addView(bigStat("Thời gian tập trung", repository.getFocusMinutes() + " phút", R.drawable.bg_card_yellow));
        stats.addView(bigStat("Số phiên Pomodoro", repository.getFocusSessions() + " phiên", R.drawable.bg_card_pink));
        stats.addView(bigStat("Tổng lịch học", repository.getEvents().size() + " sự kiện", R.drawable.bg_card_lavender));
    }

    private void showSettings() {
        View screen = inflateScreen(R.layout.screen_settings, true, SCREEN_DASHBOARD);
        UserProfile profile = repository.getProfile();
        setText(screen, R.id.textProfile, profile.getName() + "\n" + profile.getEmail() + "\n" + profile.getGoal());
        CheckBox notify = screen.findViewById(R.id.checkNotify);
        CheckBox sync = screen.findViewById(R.id.checkSync);
        notify.setChecked(repository.isNotifyEnabled());
        sync.setChecked(repository.isSyncEnabled());
        notify.setOnCheckedChangeListener((buttonView, isChecked) -> repository.setNotifyEnabled(isChecked));
        sync.setOnCheckedChangeListener((buttonView, isChecked) -> repository.setSyncEnabled(isChecked));
        screen.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showProfileDialog());
        screen.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            authRepository.logout();
            repository.setLoggedIn(false);
            resetPomodoro();
            showLogin();
        });
    }

    private void showTaskDialog(StudyTask editingTask, Runnable onSaved) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
        EditText title = dialogView.findViewById(R.id.inputTitle);
        EditText subject = dialogView.findViewById(R.id.inputSubject);
        EditText due = dialogView.findViewById(R.id.inputDue);
        EditText note = dialogView.findViewById(R.id.inputNote);
        Spinner priority = dialogView.findViewById(R.id.spinnerPriority);
        String[] priorities = {StudyTask.PRIORITY_HIGH, StudyTask.PRIORITY_MEDIUM, StudyTask.PRIORITY_LOW};
        priority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities));

        long defaultDue = DateTimeUtils.daysFromNow(0, 21, 0);
        due.setText(DateTimeUtils.formatDateTime(defaultDue));
        if (editingTask != null) {
            title.setText(editingTask.getTitle());
            subject.setText(editingTask.getSubject());
            due.setText(DateTimeUtils.formatDateTime(editingTask.getDueAt()));
            note.setText(editingTask.getNote());
            priority.setSelection(indexOf(priorities, editingTask.getPriority()));
        }
        due.setOnClickListener(v -> pickDateTime(due));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editingTask == null ? "Thêm công việc" : "Sửa công việc")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (isBlank(title) || isBlank(subject)) {
                        toast("Vui lòng nhập tên công việc và môn học");
                        return;
                    }
                    long dueAt = DateTimeUtils.parseDateTime(textOf(due), defaultDue);
                    StudyTask task = editingTask == null
                            ? repository.newTask(textOf(title), textOf(subject), dueAt, String.valueOf(priority.getSelectedItem()), textOf(note))
                            : editingTask;
                    task.setTitle(textOf(title));
                    task.setSubject(textOf(subject));
                    task.setDueAt(dueAt);
                    task.setPriority(String.valueOf(priority.getSelectedItem()));
                    task.setNote(textOf(note));
                    repository.saveTask(task);
                    dialog.dismiss();
                    onSaved.run();
                }));
        dialog.show();
    }

    private void showEventDialog(StudyEvent editingEvent, Runnable onSaved) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event, null);
        EditText title = dialogView.findViewById(R.id.inputTitle);
        EditText subject = dialogView.findViewById(R.id.inputSubject);
        EditText date = dialogView.findViewById(R.id.inputDate);
        EditText start = dialogView.findViewById(R.id.inputStart);
        EditText end = dialogView.findViewById(R.id.inputEnd);
        EditText room = dialogView.findViewById(R.id.inputRoom);
        EditText note = dialogView.findViewById(R.id.inputNote);
        Spinner type = dialogView.findViewById(R.id.spinnerType);
        String[] types = {StudyEvent.TYPE_STUDY, StudyEvent.TYPE_EXAM, StudyEvent.TYPE_DEADLINE};
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        long defaultStart = DateTimeUtils.daysFromNow(1, 9, 30);
        long defaultEnd = DateTimeUtils.daysFromNow(1, 11, 30);
        fillEventTimeInputs(date, start, end, defaultStart, defaultEnd);
        if (editingEvent != null) {
            title.setText(editingEvent.getTitle());
            subject.setText(editingEvent.getSubject());
            room.setText(editingEvent.getRoom());
            note.setText(editingEvent.getNote());
            type.setSelection(indexOf(types, editingEvent.getType()));
            fillEventTimeInputs(date, start, end, editingEvent.getStartAt(), editingEvent.getEndAt());
        }
        date.setOnClickListener(v -> pickDate(date));
        start.setOnClickListener(v -> pickTime(start));
        end.setOnClickListener(v -> pickTime(end));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editingEvent == null ? "Thêm lịch / sự kiện" : "Sửa sự kiện")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (isBlank(title) || isBlank(subject)) {
                        toast("Vui lòng nhập tên sự kiện và môn học");
                        return;
                    }
                    long startAt = DateTimeUtils.combineDateAndTime(textOf(date), textOf(start), defaultStart);
                    long endAt = DateTimeUtils.combineDateAndTime(textOf(date), textOf(end), defaultEnd);
                    if (endAt <= startAt) {
                        endAt = startAt + 60L * 60L * 1000L;
                    }
                    StudyEvent event = editingEvent == null
                            ? repository.newEvent(textOf(title), String.valueOf(type.getSelectedItem()), textOf(subject), startAt, endAt, textOf(room), textOf(note))
                            : editingEvent;
                    event.setTitle(textOf(title));
                    event.setType(String.valueOf(type.getSelectedItem()));
                    event.setSubject(textOf(subject));
                    event.setStartAt(startAt);
                    event.setEndAt(endAt);
                    event.setRoom(textOf(room));
                    event.setNote(textOf(note));
                    List<StudyEvent> conflicts = repository.getConflicts(event);
                    if (!conflicts.isEmpty()) {
                        showConflictBeforeSave(event, conflicts, onSaved);
                        dialog.dismiss();
                        return;
                    }
                    repository.saveEvent(event);
                    dialog.dismiss();
                    onSaved.run();
                }));
        dialog.show();
    }

    private void showProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null);
        EditText name = dialogView.findViewById(R.id.inputName);
        EditText email = dialogView.findViewById(R.id.inputEmail);
        EditText goal = dialogView.findViewById(R.id.inputGoal);
        UserProfile profile = repository.getProfile();
        name.setText(profile.getName());
        email.setText(profile.getEmail());
        goal.setText(profile.getGoal());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Sửa hồ sơ")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (isBlank(name) || isBlank(email)) {
                        toast("Vui lòng nhập tên và email");
                        return;
                    }
                    if (!isValidEmail(textOf(email))) {
                        toast("Email chưa đúng định dạng");
                        return;
                    }
                    repository.saveProfile(new UserProfile(textOf(name), textOf(email), textOf(goal)));
                    dialog.dismiss();
                    showSettings();
                }));
        dialog.show();
    }

    private void showTaskActions(StudyTask task) {
        String[] actions = {"Sửa", task.isCompleted() ? "Đánh dấu chưa xong" : "Đánh dấu hoàn thành", "Xóa"};
        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showTaskDialog(task, () -> showTasks("Tất cả"));
                    } else if (which == 1) {
                        task.setCompleted(!task.isCompleted());
                        repository.saveTask(task);
                        showTasks("Tất cả");
                    } else {
                        confirmDelete("Xóa công việc?", task.getTitle(), () -> {
                            repository.deleteTask(task.getId());
                            showTasks("Tất cả");
                        });
                    }
                })
                .show();
    }

    private void showEventActions(StudyEvent event) {
        String[] actions = {"Sửa", "Xóa"};
        new AlertDialog.Builder(this)
                .setTitle(event.getTitle())
                .setMessage(event.getType() + " • " + event.getSubject() + "\n" + DateTimeUtils.formatDateTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt()) + "\n" + event.getRoom())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showEventDialog(event, () -> showSchedule("Tất cả"));
                    } else {
                        confirmDelete("Xóa sự kiện?", event.getTitle(), () -> {
                            repository.deleteEvent(event.getId());
                            showSchedule("Tất cả");
                        });
                    }
                })
                .show();
    }

    private void showConflictBeforeSave(StudyEvent event, List<StudyEvent> conflicts, Runnable onSaved) {
        StringBuilder message = new StringBuilder();
        message.append(event.getTitle())
                .append("\n")
                .append(DateTimeUtils.formatDateTime(event.getStartAt()))
                .append(" - ")
                .append(DateTimeUtils.formatTime(event.getEndAt()))
                .append("\n\nTrùng với:\n");
        for (StudyEvent conflict : conflicts) {
            message.append("- ")
                    .append(conflict.getTitle())
                    .append(" (")
                    .append(DateTimeUtils.formatTime(conflict.getStartAt()))
                    .append(" - ")
                    .append(DateTimeUtils.formatTime(conflict.getEndAt()))
                    .append(")\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Phát hiện xung đột lịch")
                .setMessage(message.toString().trim())
                .setNegativeButton("Quay lại sửa", null)
                .setPositiveButton("Vẫn lưu", (dialog, which) -> {
                    repository.saveEvent(event);
                    onSaved.run();
                })
                .show();
    }

    private void confirmDelete(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> onConfirm.run())
                .show();
    }

    private void confirmOcrImport() {
        new AlertDialog.Builder(this)
                .setTitle("Tạo lịch từ hình ảnh")
                .setMessage("Demo OCR sẽ thêm 2 sự kiện và 1 deadline mẫu vào dữ liệu của bạn.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo lịch", (dialog, which) -> {
                    repository.addOcrSampleEvents();
                    toast("Đã đồng bộ dữ liệu từ ảnh mẫu");
                    showSchedule("Tất cả");
                })
                .show();
    }

    private void showImageImportOptions() {
        String[] actions = {"Chụp ảnh", "Tải ảnh từ máy", "Dùng dữ liệu mẫu"};
        new AlertDialog.Builder(this)
                .setTitle("Tạo lịch tự động")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        galleryLauncher.launch("image/*");
                    } else {
                        confirmOcrImport();
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists() && !dir.mkdirs()) {
                toast("Không tạo được thư mục ảnh tạm");
                return;
            }
            File image = File.createTempFile("study_schedule_", ".jpg", dir);
            pendingCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", image);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (IOException exception) {
            toast("Không mở được camera: " + exception.getMessage());
        }
    }

    private void processScheduleImage(Uri uri) {
        AlertDialog loading = new AlertDialog.Builder(this)
                .setTitle("Gemini đang đọc lịch")
                .setMessage("Đang trích xuất thời gian, môn học, phòng học và deadline từ ảnh...")
                .setCancelable(false)
                .create();
        loading.show();
        new GeminiScheduleExtractor(this).extract(uri, new GeminiScheduleExtractor.Callback() {
            @Override
            public void onSuccess(List<StudyEvent> events, String rawJson) {
                runOnUiThread(() -> {
                    loading.dismiss();
                    handleExtractedEvents(events, rawJson);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    loading.dismiss();
                    toast(message);
                });
            }
        });
    }

    private void handleExtractedEvents(List<StudyEvent> events, String rawJson) {
        if (events.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Không tìm thấy lịch")
                    .setMessage("Gemini không trích xuất được sự kiện nào từ ảnh này.\n\nJSON nhận được:\n" + rawJson)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        StringBuilder preview = new StringBuilder();
        List<StudyEvent> conflictEvents = new ArrayList<>();
        for (StudyEvent event : events) {
            List<StudyEvent> conflicts = repository.getConflicts(event);
            if (!conflicts.isEmpty()) {
                conflictEvents.add(event);
            }
            preview.append(conflicts.isEmpty() ? "" : "[Trùng] ")
                    .append(event.getTitle())
                    .append("\n")
                    .append(DateTimeUtils.formatDateTime(event.getStartAt()))
                    .append(" - ")
                    .append(DateTimeUtils.formatTime(event.getEndAt()))
                    .append("\n\n");
        }
        String title = conflictEvents.isEmpty()
                ? "Tạo " + events.size() + " lịch từ ảnh?"
                : "Có " + conflictEvents.size() + " lịch bị trùng";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(preview.toString().trim())
                .setNegativeButton("Hủy", null)
                .setPositiveButton(conflictEvents.isEmpty() ? "Lưu lịch" : "Vẫn lưu", (dialog, which) -> {
                    for (StudyEvent event : events) {
                        repository.saveEvent(event);
                    }
                    scheduleWeekStartMillis = DateTimeUtils.startOfWeek(events.get(0).getStartAt());
                    toast("Đã tạo " + events.size() + " lịch từ ảnh");
                    showSchedule("Tất cả");
                })
                .show();
    }

    private View createTaskRow(StudyTask task, boolean interactive, ViewGroup parent) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_task, parent, false);
        TextView title = row.findViewById(R.id.textTitle);
        TextView meta = row.findViewById(R.id.textMeta);
        TextView priority = row.findViewById(R.id.textPriority);
        CheckBox done = row.findViewById(R.id.checkDone);
        title.setText(task.getTitle());
        meta.setText(task.getSubject() + " • " + DateTimeUtils.formatDateTime(task.getDueAt()));
        priority.setText(task.getPriority());
        priority.setBackgroundResource(priorityBackground(task.getPriority()));
        done.setChecked(task.isCompleted());
        done.setEnabled(interactive);
        done.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!interactive) {
                return;
            }
            task.setCompleted(isChecked);
            repository.saveTask(task);
        });
        if (task.isCompleted()) {
            title.setAlpha(0.55f);
            meta.setAlpha(0.55f);
        }
        row.setRotation(task.getId().hashCode() % 2 == 0 ? -0.7f : 0.7f);
        return row;
    }

    private View createEventRow(StudyEvent event, ViewGroup parent) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_event, parent, false);
        row.setBackgroundResource(eventBackground(event.getType()));
        setText(row, R.id.textTitle, event.getTitle());
        setText(row, R.id.textMeta, event.getType() + " • " + event.getSubject() + " • " + DateTimeUtils.formatDayLabel(event.getStartAt()) + " " + DateTimeUtils.formatTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt()));
        String conflict = repository.hasConflict(event) ? "[Trùng lịch] " : "";
        setText(row, R.id.textNote, conflict + event.getRoom() + (TextUtils.isEmpty(event.getNote()) ? "" : " • " + event.getNote()));
        row.setRotation(event.getId().hashCode() % 2 == 0 ? -0.8f : 0.8f);
        return row;
    }

    private TextView statBlock(String label, String value) {
        TextView view = new TextView(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        view.setGravity(android.view.Gravity.CENTER);
        view.setText(value + "\n" + label);
        view.setTextColor(getColor(R.color.ink));
        view.setTextSize(14f);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView bigStat(String label, String value, int background) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(88));
        params.setMargins(0, 0, 0, dp(12));
        view.setLayoutParams(params);
        view.setBackgroundResource(background);
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        view.setText(label + "\n" + value);
        view.setTextColor(getColor(R.color.ink));
        view.setTextSize(18f);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setRotation(label.hashCode() % 2 == 0 ? -0.8f : 0.8f);
        return view;
    }

    private TextView emptyState(String message) {
        TextView view = new TextView(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
        view.setGravity(android.view.Gravity.CENTER);
        view.setText(message);
        view.setTextColor(getColor(R.color.muted));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    private void bindFilter(View screen, int id, String value, String active, Runnable action) {
        TextView view = screen.findViewById(id);
        view.setBackgroundResource(value.equals(active) ? R.drawable.bg_selected_pill : R.drawable.bg_outline_pill);
        view.setOnClickListener(v -> action.run());
    }

    private void pickDateTime(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                target.setText(DateTimeUtils.formatDateTime(calendar.getTimeInMillis()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePicker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void pickDate(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            target.setText(DateTimeUtils.formatDate(calendar.getTimeInMillis()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> target.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void fillEventTimeInputs(EditText date, EditText start, EditText end, long startAt, long endAt) {
        date.setText(DateTimeUtils.formatDate(startAt));
        start.setText(DateTimeUtils.formatTime(startAt));
        end.setText(DateTimeUtils.formatTime(endAt));
    }

    private boolean matchesTaskFilter(StudyTask task, String filter) {
        if ("Hôm nay".equals(filter)) {
            return DateTimeUtils.isToday(task.getDueAt());
        }
        if ("Sắp hạn".equals(filter)) {
            return DateTimeUtils.isSoon(task.getDueAt()) && !task.isCompleted();
        }
        if ("Quá hạn".equals(filter)) {
            return task.getDueAt() < System.currentTimeMillis() && !task.isCompleted();
        }
        return true;
    }

    private StudyEvent findNextEvent(List<StudyEvent> events) {
        long now = System.currentTimeMillis();
        for (StudyEvent event : events) {
            if (event.getStartAt() >= now) {
                return event;
            }
        }
        return null;
    }

    private StudyTask findNextTask(List<StudyTask> tasks) {
        for (StudyTask task : tasks) {
            if (!task.isCompleted() && task.getDueAt() >= System.currentTimeMillis()) {
                return task;
            }
        }
        return null;
    }

    private List<StudyTask> topPriorityTasks(List<StudyTask> tasks) {
        List<StudyTask> result = new ArrayList<>();
        for (StudyTask task : tasks) {
            if (!task.isCompleted()) {
                result.add(task);
            }
            if (result.size() == 3) {
                break;
            }
        }
        return result;
    }

    private int countTodayTasks(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (!task.isCompleted() && DateTimeUtils.isToday(task.getDueAt())) {
                count++;
            }
        }
        return count;
    }

    private int countCompleted(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    private int countOverdue(List<StudyTask> tasks) {
        int count = 0;
        long now = System.currentTimeMillis();
        for (StudyTask task : tasks) {
            if (!task.isCompleted() && task.getDueAt() < now) {
                count++;
            }
        }
        return count;
    }

    private int completionRate(List<StudyTask> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }
        return Math.round(countCompleted(tasks) * 100f / tasks.size());
    }

    private int priorityBackground(String priority) {
        if (StudyTask.PRIORITY_HIGH.equals(priority)) {
            return R.drawable.bg_card_pink;
        }
        if (StudyTask.PRIORITY_LOW.equals(priority)) {
            return R.drawable.bg_card_mint;
        }
        return R.drawable.bg_card_yellow;
    }

    private int eventBackground(String type) {
        if (StudyEvent.TYPE_EXAM.equals(type)) {
            return R.drawable.bg_card_pink;
        }
        if (StudyEvent.TYPE_DEADLINE.equals(type)) {
            return R.drawable.bg_card_yellow;
        }
        return R.drawable.bg_card_mint;
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private String firstName(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return "bạn";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private boolean isBlank(EditText editText) {
        return TextUtils.isEmpty(editText.getText().toString().trim());
    }

    private String textOf(EditText editText) {
        return editText.getText().toString().trim();
    }

    private boolean isValidEmail(String value) {
        return !TextUtils.isEmpty(value) && android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }

    private void setText(View root, int id, String text) {
        ((TextView) root.findViewById(id)).setText(text);
    }

    private void syncProfileFromAuth(AuthUser user) {
        if (user == null) {
            return;
        }
        UserProfile current = repository.getProfile();
        repository.saveProfile(new UserProfile(user.getName(), user.getEmail(), current.getGoal()));
    }

    private void showDemoOtpDialog(String otp, String purpose) {
        new AlertDialog.Builder(this)
                .setTitle("OTP " + purpose)
                .setMessage("Mã OTP thử nghiệm: " + otp + "\n\nTrong bản production, mã này sẽ được gửi qua Email/SMS backend hoặc Firebase.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void showGoogleSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Google Sign-In cần cấu hình Firebase")
                .setMessage("Mình đã bỏ cơ chế đăng nhập Google giả. Để bật đăng nhập Google thật cần thêm google-services.json, SHA-1 của máy build và OAuth client trong Firebase Console.")
                .setPositiveButton("OK", null)
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
