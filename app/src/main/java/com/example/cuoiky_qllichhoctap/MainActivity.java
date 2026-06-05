package com.example.cuoiky_qllichhoctap;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.os.Build;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.cuoiky_qllichhoctap.data.GeminiScheduleExtractor;
import com.example.cuoiky_qllichhoctap.data.AdminPortalClient;
import com.example.cuoiky_qllichhoctap.data.AuthRepository;
import com.example.cuoiky_qllichhoctap.data.OtpEmailSender;
import com.example.cuoiky_qllichhoctap.data.StudyRepository;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.AuthUser;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;
import com.example.cuoiky_qllichhoctap.util.ReminderScheduler;
import com.example.cuoiky_qllichhoctap.ui.StudyDialogFactory;
import com.example.cuoiky_qllichhoctap.ui.StudyDialogFactory.StudyFormDialog;
import com.example.cuoiky_qllichhoctap.ui.ComparisonBarChartView;
import com.example.cuoiky_qllichhoctap.ui.DonutChartView;
import com.example.cuoiky_qllichhoctap.ui.SwipeActionLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.example.cuoiky_qllichhoctap.util.StudyStats.*;

public class MainActivity extends AppCompatActivity {
    static final int SCREEN_DASHBOARD = 0;
    static final int SCREEN_SCHEDULE = 1;
    static final int SCREEN_TASKS = 2;
    static final int SCREEN_POMODORO = 3;
    static final int SCREEN_STATS = 4;
    private static final int GOOGLE_SIGN_IN_DEVELOPER_ERROR = 10;
    private static final String TASK_FILTER_ALL = "Tất cả";
    private static final String TASK_FILTER_TODAY = "Hôm nay";
    private static final String TASK_FILTER_SOON = "Sắp hạn";
    private static final String TASK_FILTER_OVERDUE = "Quá hạn";
    private static final String TASK_FILTER_DONE = "Đã hoàn thành";
    private static final String TASK_FILTER_MATRIX = "Ma trận ưu tiên";
    private static final String TASK_FILTER_TAG_PREFIX = "Môn: ";
    private static final String TASK_FILTER_PRIORITY_PREFIX = "Ưu tiên: ";
    private static final String[] REPEAT_OPTIONS = {"Không lặp", "Hằng ngày", "Hằng tuần", "Hằng tháng"};
    private static final String[] EVENT_TYPES = {StudyEvent.TYPE_STUDY, StudyEvent.TYPE_EXAM, StudyEvent.TYPE_DEADLINE, StudyEvent.TYPE_PERSONAL};
    private static final String[] EVENT_TYPE_LABELS = {"Lịch học", "Lịch thi", "Deadline", "Cá nhân"};
    private static final String[] REMINDER_LABELS = {"5 phút", "10 phút", "15 phút", "30 phút", "1 giờ", "1 ngày"};
    private static final int[] REMINDER_MINUTES = {5, 10, 15, 30, 60, 1440};
    private static final String[] AVATAR_CHOICES = {"Chữ viết tắt", "Robot học tập", "Mèo học tập", "Quyển sách", "Bạn học tập"};
    private static final String[] MASCOT_CHOICES = {"Robot học tập", "Mèo học tập", "Quyển sách", "Bạn học tập"};
    private static final String[] DASHBOARD_BACKGROUNDS = {"Giấy sáng", "Xanh dịu", "Tím học tập", "Vàng note", "Hồng nhẹ"};
    private static final String[] THEME_COLORS = {"Hoa hồng", "Xanh biển", "Xanh lá", "Vàng", "Tím"};

    StudyRepository repository;
    AuthRepository authRepository;
    AdminPortalClient adminPortalClient;
    OtpEmailSender otpEmailSender;
    ReminderScheduler reminderScheduler;
    StudyDialogFactory dialogFactory;
    FirebaseAuth firebaseAuth;
    private AuthController authController;
    FrameLayout contentFrame;
    private LinearLayout bottomNav;
    PomodoroController pomodoroController;
    private CountdownController countdownController;
    private ScheduleController scheduleController;
    
    private boolean showTodayScheduleInTasks = false;
    private final Set<String> checkedTodayScheduleIds = new HashSet<>();
    private String lastShownAnnouncementId = "";
    private Uri pendingCameraUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private boolean notificationPermissionRequestInFlight;
    private DrawerLayout drawerLayout;

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
        adminPortalClient = new AdminPortalClient();
        otpEmailSender = new OtpEmailSender();
        dialogFactory = new StudyDialogFactory(this);
        pomodoroController = new PomodoroController(this);
        countdownController = new CountdownController(this);
        scheduleController = new ScheduleController(this);
        reminderScheduler = new ReminderScheduler(this, repository, this::eventTypeLabel, new ReminderScheduler.NotificationPermissionGateway() {
            @Override
            public boolean hasPermission() {
                return hasNotificationPermission();
            }

            @Override
            public void requestOnce() {
                requestNotificationPermissionOnce();
            }
        });
        firebaseAuth = FirebaseAuth.getInstance();
        authController = new AuthController(this);
        contentFrame = findViewById(R.id.contentFrame);
        bottomNav = findViewById(R.id.bottomNav);
        drawerLayout = findViewById(R.id.drawerLayout);
        authController.setupGoogleSignIn();
        setupNotificationPermissionLauncher();
        setupImageLaunchers();
        setupBottomNav();
        setupSideMenu();

        authController.showInitialScreen();
    }

    private void setupSideMenu() {
        if (drawerLayout == null) return;
        findViewById(R.id.btnCloseMenu).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.menuItemAll).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showTasks("Tất cả"); });
        findViewById(R.id.menuItemToday).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showTasks("Hôm nay"); });
        findViewById(R.id.menuItemDone).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showTasks("Đã hoàn thành"); });
        findViewById(R.id.menuItemCountdown).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); countdownController.showCountdown(CountdownController.FILTER_ALL); });
        findViewById(R.id.menuItemReportIssue).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showReportIssueDialog(); });
    }

    private void showReportIssueDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_report_issue, null, false);
        EditText input = form.findViewById(R.id.inputIssueMessage);

        new AlertDialog.Builder(this)
                .setTitle("Báo lỗi / góp ý")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        toast("Vui lòng nhập nội dung phản ánh");
                        return;
                    }
                    String email = currentAccountEmail();
                    adminPortalClient.reportIssue("general", email, message);
                    toast("Đã gửi phản ánh đến web quản trị");
                })
                .show();
    }

    private String currentAccountEmail() {
        AuthUser localUser = authRepository == null ? null : authRepository.getCurrentUser();
        if (localUser != null && !TextUtils.isEmpty(localUser.getEmail())) {
            return localUser.getEmail();
        }
        FirebaseUser firebaseUser = firebaseAuth == null ? null : firebaseAuth.getCurrentUser();
        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getEmail())) {
            return firebaseUser.getEmail();
        }
        UserProfile profile = repository == null ? null : repository.getProfile();
        return profile == null ? "không rõ" : profile.getEmail();
    }

    private void updateSideMenuHeader(UserProfile profile) {
        if (drawerLayout == null) return;
        ((TextView) findViewById(R.id.menuTextAvatar)).setText(avatarMark(repository.getAvatarChoice(), profile));
        ((TextView) findViewById(R.id.menuTextName)).setText(profile.getName().isEmpty() ? "Student Planner" : profile.getName());
        List<StudyTask> tasks = repository.getTasks();
        ((TextView) findViewById(R.id.menuCountAll)).setText(String.valueOf(tasks.size()));
    }

    @Override
    protected void onDestroy() {
        if (pomodoroController != null) {
            pomodoroController.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        authController.syncLearningSnapshotToAdmin();
        super.onPause();
    }

    View inflateScreen(int layoutId, boolean showBottomNav, int selectedNav) {
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
        findViewById(R.id.navSchedule).setOnClickListener(v -> scheduleController.showSchedule("Tất cả"));
        findViewById(R.id.navTasks).setOnClickListener(v -> showTasks("Tất cả"));
        findViewById(R.id.navPomodoro).setOnClickListener(v -> pomodoroController.showPomodoro());
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
            int color = getColor(active ? R.color.ink : R.color.muted);
            button.setTextColor(color);
            button.setIconTint(android.content.res.ColorStateList.valueOf(color));
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(active ? R.color.yellow : android.R.color.transparent)));
        }
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            notificationPermissionRequestInFlight = false;
            if (granted) {
                toast("Đã bật quyền thông báo. Các nhắc nhở sắp tới sẽ được lên lịch lại.");
                reminderScheduler.rescheduleAllReminders();
            } else {
                authController.showNotificationPermissionDeniedMessage();
            }
        });
    }

    void showDashboard() {
        View screen = inflateScreen(R.layout.screen_dashboard, true, SCREEN_DASHBOARD);
        UserProfile profile = repository.getProfile();
        List<StudyTask> tasks = repository.getTasks();
        List<StudyEvent> events = repository.getEvents();
        authController.syncLearningSnapshotToAdmin();
        int themeColor = getColor(themeColorRes(repository.getThemeColorChoice()));

        int todayTotal = countAllTodayTasks(tasks);
        int todayCompleted = countCompletedTodayTasks(tasks);
        int todayRemaining = countTodayTasks(tasks);
        int todayProgress = todayTotal == 0 ? 0 : Math.round(todayCompleted * 100f / todayTotal);

        screen.findViewById(R.id.dashboardRoot).setBackgroundColor(getColor(dashboardBackgroundColorRes(repository.getDashboardBackgroundChoice())));
        View avatarCard = screen.findViewById(R.id.avatarCard);
        ((TextView) screen.findViewById(R.id.textAvatar)).setText(avatarMark(repository.getAvatarChoice(), profile));
        setText(screen, R.id.textAvatarLabel, shortStatus(repository.getStudyStatus()));
        
        avatarCard.setOnClickListener(v -> {
            if (drawerLayout != null) {
                updateSideMenuHeader(profile);
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        TextView greeting = screen.findViewById(R.id.textGreeting);
        greeting.setText(greetingForNow() + ", " + firstName(profile.getName()));
        greeting.setTextColor(themeColor);
        ((TextView) screen.findViewById(R.id.textTodaySummary)).setText(repository.getStudyStatus() + " · " + dashboardSummary(todayTotal, todayCompleted, todayRemaining));
        ((ProgressBar) screen.findViewById(R.id.progressToday)).setProgress(todayProgress);
        setText(screen, R.id.textProgressPercent, todayProgress + "%");
        setText(screen, R.id.textProgressMeta, todayTotal == 0
                ? "Chưa có việc học nào trong hôm nay"
                : todayCompleted + "/" + todayTotal + " việc đã hoàn thành");

        StudyEvent nextEvent = findNextEvent(events);
        if (nextEvent == null) {
            setText(screen, R.id.textNextEvent, "Chưa có lịch sắp tới");
            setText(screen, R.id.textNextEventMeta, "Thêm lịch học để bắt đầu theo dõi");
        } else {
            setText(screen, R.id.textNextEvent, nextEvent.getTitle());
            setText(screen, R.id.textNextEventMeta, eventTimeMeta(nextEvent));
        }

        StudyTask nextTask = findNearestDeadlineTask(tasks);
        StudyEvent nextDeadlineEvent = findNearestDeadlineEvent(events, repository);
        if (nextTask == null && nextDeadlineEvent == null) {
            setText(screen, R.id.textDeadline, "Không có deadline");
            setText(screen, R.id.textDeadlineMeta, "Bạn đang khá thoáng lịch");
        } else if (nextDeadlineEvent == null || (nextTask != null && nextTask.getDueAt() <= nextDeadlineEvent.getStartAt())) {
            setText(screen, R.id.textDeadline, nextTask.getTitle());
            setText(screen, R.id.textDeadlineMeta, deadlineMeta(nextTask));
        } else {
            setText(screen, R.id.textDeadline, nextDeadlineEvent.getTitle());
            setText(screen, R.id.textDeadlineMeta, deadlineEventMeta(nextDeadlineEvent));
        }
        setText(screen, R.id.textTodayTasks, todayRemaining + "/" + todayTotal);
        setText(screen, R.id.textTodayTasksMeta, todayTotal == 0
                ? "Chưa có việc học hôm nay"
                : todayCompleted + " xong, " + todayRemaining + " còn lại");
        setText(screen, R.id.textPomodoroToday, repository.getTodayFocusMinutes() + " phút");
        setText(screen, R.id.textPomodoroMeta, repository.getTodayFocusSessions() + " phiên hôm nay · tổng " + repository.getFocusMinutes() + " phút");

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
            priorityTasks.addView(emptyState("Chưa có việc ưu tiên. Thêm một việc nhỏ để bắt đầu ngày học."));
        }

        screen.findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());
        screen.findViewById(R.id.btnAddEvent).setOnClickListener(v -> showEventDialog(null, this::showDashboard));
        screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, this::showDashboard));
        screen.findViewById(R.id.btnOcr).setOnClickListener(v -> showImageImportOptions());
        screen.findViewById(R.id.btnPomodoroQuick).setOnClickListener(v -> pomodoroController.showPomodoro());
        tintButton(screen, R.id.btnPomodoroQuick, themeColorRes(repository.getThemeColorChoice()), themeButtonTextColorRes(repository.getThemeColorChoice()));
        fetchAndShowAdminAnnouncement();
    }

    private void fetchAndShowAdminAnnouncement() {
        adminPortalClient.fetchLatestAnnouncement((id, title, body, loaded, message) -> runOnUiThread(() -> {
            if (!loaded || TextUtils.isEmpty(id) || id.equals(lastShownAnnouncementId)) {
                return;
            }
            lastShownAnnouncementId = id;
            new AlertDialog.Builder(this)
                    .setTitle(TextUtils.isEmpty(title) ? "Thông báo chung" : title)
                    .setMessage(TextUtils.isEmpty(body) ? "Bạn có một thông báo mới từ quản trị viên." : body)
                    .setPositiveButton("Đã đọc", null)
                    .show();
        }));
    }

    private void showTasks(String filter) {
        View screen = inflateScreen(R.layout.screen_tasks, true, SCREEN_TASKS);
        setupTaskFilters(screen, filter);
        setupTaskScheduleToggle(screen, filter);
        LinearLayout taskList = screen.findViewById(R.id.taskList);
        taskList.removeAllViews();

        detachTasksFromCalendar();
        List<StudyTask> tasks = repository.getTasks();
        if (showTodayScheduleInTasks) {
            addTodaySchedulePreview(taskList);
        }
        if (TASK_FILTER_MATRIX.equals(filter)) {
            addQuadrantSection(taskList, tasks, "Quan trọng và khẩn cấp", true, true);
            addQuadrantSection(taskList, tasks, "Quan trọng nhưng không khẩn cấp", true, false);
            addQuadrantSection(taskList, tasks, "Không quan trọng nhưng khẩn cấp", false, true);
            addQuadrantSection(taskList, tasks, "Không quan trọng và không khẩn cấp", false, false);
            screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, () -> showTasks(filter)));
            return;
        }

        int taskRows = 0;
        for (StudyTask task : tasks) {
            if (!matchesTaskFilter(task, filter)) {
                continue;
            }
            Runnable refreshTasks = () -> showTasks(filter);
            View row = createTaskRow(task, true, taskList, refreshTasks);
            View foreground = row.findViewById(R.id.taskForeground);
            foreground.setOnClickListener(v -> {
                if (row instanceof SwipeActionLayout && ((SwipeActionLayout) row).isOpen()) {
                    ((SwipeActionLayout) row).close();
                    return;
                }
                showTaskActions(task, refreshTasks);
            });
            taskList.addView(row);
            taskRows++;
        }
        if (taskRows == 0) {
            taskList.addView(emptyState("Chưa có công việc phù hợp"));
        }
        screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, () -> showTasks(filter)));
    }

    private void detachTasksFromCalendar() {
        for (StudyTask task : repository.getTasks()) {
            if (!task.isShowOnCalendar()) {
                continue;
            }
            task.setShowOnCalendar(false);
            repository.saveTask(task);
            reminderScheduler.syncTaskCalendarEvent(task);
        }
    }

    private void setupTaskScheduleToggle(View screen, String filter) {
        TextView toggle = screen.findViewById(R.id.toggleTodaySchedule);
        toggle.setText(showTodayScheduleInTasks ? "Hiện lịch hôm nay: Bật" : "Hiện lịch hôm nay: Tắt");
        toggle.setBackgroundResource(showTodayScheduleInTasks ? R.drawable.bg_selected_pill : R.drawable.bg_outline_pill);
        toggle.setOnClickListener(v -> {
            showTodayScheduleInTasks = !showTodayScheduleInTasks;
            showTasks(filter);
        });
    }

    private void addTodaySchedulePreview(LinearLayout taskList) {
        List<StudyEvent> todayEvents = todayScheduleEvents();
        if (todayEvents.isEmpty()) {
            taskList.addView(taskSectionHeader("Lịch hôm nay"));
            taskList.addView(emptyState("Hôm nay chưa có lịch."));
            return;
        }
        taskList.addView(taskSectionHeader("Lịch hôm nay"));
        for (StudyEvent event : todayEvents) {
            taskList.addView(createTodayScheduleTaskRow(event, taskList));
        }
        taskList.addView(taskSectionHeader("To-do list"));
    }

    private View createTodayScheduleTaskRow(StudyEvent event, ViewGroup parent) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_task, parent, false);
        if (row instanceof SwipeActionLayout) {
            ((SwipeActionLayout) row).setSwipeEnabled(false);
        }

        TextView title = row.findViewById(R.id.textTitle);
        TextView meta = row.findViewById(R.id.textMeta);
        TextView details = row.findViewById(R.id.textDetails);
        TextView marker = row.findViewById(R.id.textMarker);
        CheckBox done = row.findViewById(R.id.checkDone);
        View taskActions = row.findViewById(R.id.taskActions);
        View foreground = row.findViewById(R.id.taskForeground);

        taskActions.setVisibility(View.GONE);
        details.setVisibility(View.GONE);
        marker.setVisibility(View.GONE);
        title.setText(event.getTitle());
        meta.setText(DateTimeUtils.formatTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt()));

        String eventId = event.getId();
        boolean checked = checkedTodayScheduleIds.contains(eventId);
        done.setChecked(checked);
        applyTodayScheduleTaskState(title, meta, checked);
        done.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkedTodayScheduleIds.add(eventId);
            } else {
                checkedTodayScheduleIds.remove(eventId);
            }
            applyTodayScheduleTaskState(title, meta, isChecked);
        });
        foreground.setOnClickListener(v -> done.setChecked(!done.isChecked()));
        return row;
    }

    private void applyTodayScheduleTaskState(TextView title, TextView meta, boolean checked) {
        float alpha = checked ? 0.55f : 1f;
        title.setAlpha(alpha);
        meta.setAlpha(alpha);
        if (checked) {
            title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            title.setPaintFlags(title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private List<StudyEvent> todayScheduleEvents() {
        List<StudyEvent> result = new ArrayList<>();
        for (StudyEvent event : repository.getEvents()) {
            if (!TextUtils.isEmpty(event.getSourceTaskId())) {
                continue;
            }
            if (DateTimeUtils.isToday(event.getStartAt())) {
                result.add(event);
            }
        }
        return result;
    }

    private void setupTaskFilters(View screen, String active) {
        bindFilter(screen, R.id.filterAll, TASK_FILTER_ALL, active, () -> showTasks(TASK_FILTER_ALL));
        bindFilter(screen, R.id.filterToday, TASK_FILTER_TODAY, active, () -> showTasks(TASK_FILTER_TODAY));
        bindFilter(screen, R.id.filterSoon, TASK_FILTER_SOON, active, () -> showTasks(TASK_FILTER_SOON));
        bindFilter(screen, R.id.filterOverdue, TASK_FILTER_OVERDUE, active, () -> showTasks(TASK_FILTER_OVERDUE));
        bindFilter(screen, R.id.filterDone, TASK_FILTER_DONE, active, () -> showTasks(TASK_FILTER_DONE));
        screen.findViewById(R.id.filterTag).setVisibility(View.GONE);
        screen.findViewById(R.id.filterPriority).setVisibility(View.GONE);
        screen.findViewById(R.id.filterMatrix).setVisibility(View.GONE);
    }

    private void addQuadrantSection(LinearLayout taskList, List<StudyTask> tasks, String title, boolean important, boolean urgent) {
        taskList.addView(taskSectionHeader(title));
        int before = taskList.getChildCount();
        Runnable refresh = () -> showTasks(TASK_FILTER_MATRIX);
        for (StudyTask task : tasks) {
            if (task.isImportant() == important && task.isUrgent() == urgent) {
                taskList.addView(createTaskRow(task, true, taskList, refresh));
            }
        }
        if (taskList.getChildCount() == before) {
            taskList.addView(emptyState("Chưa có việc học trong nhóm này"));
        }
    }

    private void bindDynamicTaskFilter(View screen, int id, boolean active, Runnable action) {
        TextView view = screen.findViewById(id);
        view.setBackgroundResource(active ? R.drawable.bg_selected_pill : R.drawable.bg_outline_pill);
        view.setOnClickListener(v -> action.run());
    }

    private void showTagFilterDialog() {
        List<String> tags = uniqueTaskTags();
        if (tags.isEmpty()) {
            toast("Chưa có môn học/tag để lọc");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Lọc theo môn học/tag")
                .setItems(tags.toArray(new String[0]), (dialog, which) -> showTasks(TASK_FILTER_TAG_PREFIX + tags.get(which)))
                .show();
    }

    private void showPriorityFilterDialog() {
        String[] priorities = {StudyTask.PRIORITY_HIGH, StudyTask.PRIORITY_MEDIUM, StudyTask.PRIORITY_LOW};
        new AlertDialog.Builder(this)
                .setTitle("Lọc theo ưu tiên")
                .setItems(priorities, (dialog, which) -> showTasks(TASK_FILTER_PRIORITY_PREFIX + priorities[which]))
                .show();
    }

    private void showStats() {
        View screen = inflateScreen(R.layout.screen_stats, true, SCREEN_STATS);
        List<StudyTask> tasks = repository.getTasks();
        List<StudyEvent> events = repository.getEvents();
        int totalTasks = tasks.size();
        int completed = countCompleted(tasks);
        int overdue = countOverdue(tasks);
        int pending = countPending(tasks);
        int pendingOnTrack = Math.max(0, pending - overdue);
        int completion = completionRate(tasks);
        int todayTotal = countAllTodayTasks(tasks);
        int todayDone = countCompletedTodayTasks(tasks);
        int todayRemaining = countTodayTasks(tasks);
        int todayProgress = todayTotal == 0 ? 0 : Math.round(todayDone * 100f / todayTotal);
        int studyEvents = countEventsByType(events, StudyEvent.TYPE_STUDY);
        int examEvents = countEventsByType(events, StudyEvent.TYPE_EXAM);
        int deadlineEvents = countEventsByType(events, StudyEvent.TYPE_DEADLINE);
        int personalEvents = countEventsByType(events, StudyEvent.TYPE_PERSONAL);

        setText(screen, R.id.textStatsSubtitle, statsSubtitle(completion, overdue, repository.getTodayFocusMinutes()));
        setText(screen, R.id.textCompletedMetric, completed + "\nviệc đã hoàn thành");
        setText(screen, R.id.textOpenMetric, pending + "\nviệc chưa hoàn thành");
        setText(screen, R.id.textCompletionPercent, completion + "%");
        setText(screen, R.id.textCompletionMeta, totalTasks == 0
                ? "Chưa có việc học để thống kê"
                : completed + "/" + totalTasks + " việc đã hoàn thành");
        ((ProgressBar) screen.findViewById(R.id.progressCompletion)).setProgress(completion);

        setText(screen, R.id.textTodayStatsMeta, todayTotal == 0
                ? "Hôm nay chưa có việc học nào"
                : todayDone + " xong · " + todayRemaining + " còn lại");
        setText(screen, R.id.textTodayProgressPercent, "Tiến độ hôm nay: " + todayProgress + "%");
        ((ProgressBar) screen.findViewById(R.id.progressTodayStats)).setProgress(todayProgress);

        DonutChartView taskDonut = screen.findViewById(R.id.chartTaskDonut);
        taskDonut.setData(totalTasks + "", "việc", new String[]{"Hoàn thành", "Đang làm", "Quá hạn"}, new int[]{completed, pendingOnTrack, overdue});
        ComparisonBarChartView compareBars = screen.findViewById(R.id.chartCompareBars);
        compareBars.setData("Số việc hôm nay", new String[]{"Xong", "Còn lại"}, new int[]{todayDone, todayRemaining});

        setText(screen, R.id.textTaskSummary, totalTasks + " việc · " + pending + " đang mở · " + overdue + " quá hạn");
        setText(screen, R.id.textCompletedTasks, "Hoàn thành: " + completed + " việc");
        setText(screen, R.id.textPendingTasks, "Đang làm: " + pending + " việc");
        setText(screen, R.id.textOverdueTasks, "Quá hạn: " + overdue + " việc");
        ((ProgressBar) screen.findViewById(R.id.progressCompletedTasks)).setProgress(percentOf(completed, totalTasks));
        ((ProgressBar) screen.findViewById(R.id.progressPendingTasks)).setProgress(percentOf(pending, totalTasks));
        ((ProgressBar) screen.findViewById(R.id.progressOverdueTasks)).setProgress(percentOf(overdue, totalTasks));

        setText(screen, R.id.textFocusToday, repository.getTodayFocusMinutes() + " phút\nhôm nay");
        setText(screen, R.id.textFocusTotal, repository.getFocusMinutes() + " phút\ntổng cộng");
        setText(screen, R.id.textFocusSessions, repository.getTodayFocusSessions() + " phiên hôm nay · " + repository.getFocusSessions() + " phiên tổng");

        setText(screen, R.id.textEventSummary, events.size() + " sự kiện trong lịch");
        setText(screen, R.id.textEventBreakdown, "Lịch học: " + studyEvents + " · Lịch thi: " + examEvents + " · Deadline: " + deadlineEvents + " · Cá nhân: " + personalEvents);
        setText(screen, R.id.textStatsInsight, statsInsight(totalTasks, overdue, todayTotal, todayRemaining, events.size(), repository.getTodayFocusMinutes()));
    }

    private void showSettings() {
        View screen = inflateScreen(R.layout.screen_settings, true, SCREEN_DASHBOARD);
        UserProfile profile = repository.getProfile();
        setText(screen, R.id.textProfile, avatarMark(repository.getAvatarChoice(), profile) + "  " + profile.getName() + "\n" + profile.getEmail() + "\n" + profile.getGoal());
        setText(screen, R.id.textPersonalizationPreview,
                mascotMark(repository.getMascotChoice()) + "  " + repository.getMascotChoice()
                        + "\nNền: " + repository.getDashboardBackgroundChoice()
                        + " · Màu: " + repository.getThemeColorChoice()
                        + "\nTrạng thái: " + repository.getStudyStatus());
        CheckBox notify = screen.findViewById(R.id.checkNotify);
        CheckBox sync = screen.findViewById(R.id.checkSync);
        notify.setChecked(repository.isNotifyEnabled());
        sync.setChecked(repository.isSyncEnabled());
        notify.setOnCheckedChangeListener((buttonView, isChecked) -> repository.setNotifyEnabled(isChecked));
        sync.setOnCheckedChangeListener((buttonView, isChecked) -> repository.setSyncEnabled(isChecked));
        screen.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showProfileDialog());
        screen.findViewById(R.id.btnPersonalize).setOnClickListener(v -> showPersonalizationDialog());
        screen.findViewById(R.id.btnLogout).setOnClickListener(v -> authController.logout());
    }

    private void showTaskDialog(StudyTask editingTask, Runnable onSaved) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task_simple, null);
        EditText title = dialogView.findViewById(R.id.inputTitle);
        long defaultDueAt = DateTimeUtils.daysFromNow(0, 23, 59);
        if (editingTask != null) {
            title.setText(editingTask.getTitle());
            title.setSelection(title.getText().length());
        }

        StudyFormDialog formDialog = dialogFactory.createStudyFormDialog(
                editingTask == null ? "Thêm việc cần làm" : "Sửa việc cần làm",
                dialogView,
                "Lưu");
        Dialog dialog = formDialog.dialog;
        formDialog.positive.setOnClickListener(v -> {
            if (isBlank(title)) {
                toast("Vui lòng nhập tên việc cần làm");
                return;
            }
            StudyTask task = editingTask == null
                    ? repository.newTask(textOf(title), "To-do", defaultDueAt, StudyTask.PRIORITY_MEDIUM, "")
                    : editingTask;
            task.setTitle(textOf(title));
            if (TextUtils.isEmpty(task.getSubject())) {
                task.setSubject("To-do");
            }
            if (TextUtils.isEmpty(task.getTag())) {
                task.setTag("To-do");
            }
            if (task.getDueAt() <= 0) {
                task.setDueAt(defaultDueAt);
            }
            if (TextUtils.isEmpty(task.getPriority())) {
                task.setPriority(StudyTask.PRIORITY_MEDIUM);
            }
            task.setShowOnCalendar(false);
            if (editingTask == null) {
                task.setMarkerType("flag");
                task.setMarkerValue("");
            }
            repository.saveTask(task);
            reminderScheduler.cancelTaskReminder(task);
            reminderScheduler.syncTaskCalendarEvent(task);
            dialog.dismiss();
            onSaved.run();
        });
        dialog.show();
        title.requestFocus();
    }

    void showEventDialog(StudyEvent editingEvent, Runnable onSaved) {
        long defaultStart = editingEvent == null ? DateTimeUtils.daysFromNow(1, 9, 30) : editingEvent.getStartAt();
        showEventDialog(editingEvent, onSaved, defaultStart);
    }

    void showEventDialog(StudyEvent editingEvent, Runnable onSaved, long preferredStartAt) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event, null);
        EditText title = dialogView.findViewById(R.id.inputTitle);
        EditText date = dialogView.findViewById(R.id.inputDate);
        EditText start = dialogView.findViewById(R.id.inputStart);
        EditText end = dialogView.findViewById(R.id.inputEnd);
        EditText room = dialogView.findViewById(R.id.inputRoom);
        EditText note = dialogView.findViewById(R.id.inputNote);
        CheckBox reminder = dialogView.findViewById(R.id.checkReminder);
        Spinner type = dialogView.findViewById(R.id.spinnerType);
        Spinner reminderBefore = dialogView.findViewById(R.id.spinnerReminderBefore);
        type.setAdapter(studySpinnerAdapter(EVENT_TYPE_LABELS));
        reminderBefore.setAdapter(studySpinnerAdapter(REMINDER_LABELS));

        long defaultStart = preferredStartAt > 0 ? preferredStartAt : DateTimeUtils.daysFromNow(1, 9, 30);
        long defaultEnd = defaultStart + 60L * 60L * 1000L;
        fillEventTimeInputs(date, start, end, defaultStart, defaultEnd);
        reminderBefore.setSelection(indexOf(REMINDER_LABELS, "15 phút"));
        if (editingEvent != null) {
            title.setText(editingEvent.getTitle());
            room.setText(editingEvent.getRoom());
            note.setText(editingEvent.getNote());
            type.setSelection(indexOf(EVENT_TYPES, editingEvent.getType()));
            fillEventTimeInputs(date, start, end, editingEvent.getStartAt(), editingEvent.getEndAt());
            reminder.setChecked(editingEvent.isReminderEnabled());
            reminderBefore.setSelection(indexOfReminder(editingEvent.getReminderBeforeMinutes()));
        }
        updateEventDialogHints(selectedEventType(type), title, room, note);
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateEventDialogHints(EVENT_TYPES[Math.max(0, Math.min(EVENT_TYPES.length - 1, position))], title, room, note);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        date.setOnClickListener(v -> pickDate(date));
        start.setOnClickListener(v -> pickTime(start));
        end.setOnClickListener(v -> pickTime(end));

        StudyFormDialog formDialog = dialogFactory.createStudyFormDialog(
                editingEvent == null ? "Thêm lịch / sự kiện" : "Sửa sự kiện",
                dialogView,
                "Lưu");
        Dialog dialog = formDialog.dialog;
        formDialog.positive.setOnClickListener(v -> {
            if (isBlank(title)) {
                toast("Vui lòng nhập tên sự kiện");
                return;
            }
            String selectedType = selectedEventType(type);
            long startAt = DateTimeUtils.combineDateAndTime(textOf(date), textOf(start), defaultStart);
            long endAt = DateTimeUtils.combineDateAndTime(textOf(date), textOf(end), defaultEnd);
            if (endAt <= startAt) {
                endAt = startAt + 60L * 60L * 1000L;
            }
            String subjectValue = textOf(title);
            StudyEvent event = editingEvent == null
                    ? repository.newEvent(textOf(title), selectedType, subjectValue, startAt, endAt, textOf(room), textOf(note))
                    : new StudyEvent(
                            editingEvent.getId(),
                            textOf(title),
                            selectedType,
                            subjectValue,
                            startAt,
                            endAt,
                            textOf(room),
                            textOf(note),
                            editingEvent.isReminderEnabled(),
                            editingEvent.getReminderBeforeMinutes(),
                            editingEvent.getSourceTaskId()
                    );
            event.setReminderEnabled(reminder.isChecked());
            event.setReminderBeforeMinutes(REMINDER_MINUTES[reminderBefore.getSelectedItemPosition()]);
            if (event.isReminderEnabled() && event.getStartAt() - event.getReminderBeforeMinutes() * 60L * 1000L <= System.currentTimeMillis()) {
                toast("Thời điểm nhắc trước lịch đã qua. Hãy chọn mức nhắc gần hơn hoặc tắt nhắc nhở.");
                return;
            }
            List<StudyEvent> conflicts = repository.getConflicts(event);
            if (!conflicts.isEmpty()) {
                showConflictBeforeSave(event, conflicts, () -> {
                    repository.saveEvent(event);
                    reminderScheduler.scheduleEventReminder(event);
                    dialog.dismiss();
                    onSaved.run();
                });
                return;
            }
            repository.saveEvent(event);
            reminderScheduler.scheduleEventReminder(event);
            dialog.dismiss();
            onSaved.run();
        });
        dialog.show();
    }

    private void updateEventDialogHints(String selectedType, EditText title, EditText room, EditText note) {
        if (StudyEvent.TYPE_DEADLINE.equals(selectedType)) {
            title.setHint("Tên deadline");
            room.setHint("Link nộp bài / nơi nộp");
            note.setHint("Yêu cầu, tài liệu cần nộp");
            return;
        }
        if (StudyEvent.TYPE_PERSONAL.equals(selectedType)) {
            title.setHint("Tên việc cá nhân");
            room.setHint("Địa điểm / link liên quan");
            note.setHint("Ghi chú cá nhân");
            return;
        }
        if (StudyEvent.TYPE_EXAM.equals(selectedType)) {
            title.setHint("Tên kỳ thi");
            room.setHint("Phòng thi / địa điểm");
            note.setHint("Ghi chú: giấy tờ, tài liệu, lưu ý");
            return;
        }
        title.setHint("Tên buổi học");
        room.setHint("Phòng học / địa điểm / link học online");
        note.setHint("Ghi chú");
    }

    private String selectedEventType(Spinner spinner) {
        int position = Math.max(0, Math.min(EVENT_TYPES.length - 1, spinner.getSelectedItemPosition()));
        return EVENT_TYPES[position];
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

        StudyFormDialog formDialog = dialogFactory.createStudyFormDialog("Sửa hồ sơ", dialogView, "Lưu");
        Dialog dialog = formDialog.dialog;
        formDialog.positive.setOnClickListener(v -> {
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
        });
        dialog.show();
    }

    private void showPersonalizationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_personalization, null);
        Spinner avatar = dialogView.findViewById(R.id.spinnerAvatar);
        Spinner mascot = dialogView.findViewById(R.id.spinnerMascot);
        Spinner background = dialogView.findViewById(R.id.spinnerBackground);
        Spinner theme = dialogView.findViewById(R.id.spinnerTheme);
        EditText status = dialogView.findViewById(R.id.inputStudyStatus);

        bindSpinner(avatar, AVATAR_CHOICES, repository.getAvatarChoice());
        bindSpinner(mascot, MASCOT_CHOICES, repository.getMascotChoice());
        bindSpinner(background, DASHBOARD_BACKGROUNDS, repository.getDashboardBackgroundChoice());
        bindSpinner(theme, THEME_COLORS, repository.getThemeColorChoice());
        status.setText(repository.getStudyStatus());

        StudyFormDialog formDialog = dialogFactory.createStudyFormDialog("Cá nhân hóa", dialogView, "Lưu");
        Dialog dialog = formDialog.dialog;
        formDialog.positive.setOnClickListener(v -> {
            String studyStatus = textOf(status);
            if (TextUtils.isEmpty(studyStatus)) {
                studyStatus = "Sẵn sàng học tập";
            }
            repository.savePersonalization(
                    String.valueOf(avatar.getSelectedItem()),
                    String.valueOf(background.getSelectedItem()),
                    String.valueOf(theme.getSelectedItem()),
                    String.valueOf(mascot.getSelectedItem()),
                    studyStatus
            );
            dialog.dismiss();
            toast("Đã lưu cá nhân hóa");
            showSettings();
        });
        dialog.show();
    }

    private void showTaskActions(StudyTask task) {
        showTaskActions(task, () -> showTasks("Tất cả"));
    }

    private void showTaskActions(StudyTask task, Runnable onChanged) {
        String[] actions = {"Sửa", task.isCompleted() ? "Đánh dấu chưa xong" : "Đánh dấu hoàn thành", "Xóa"};
        new AlertDialog.Builder(this)
                .setTitle(task.getTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showTaskDialog(task, onChanged);
                    } else if (which == 1) {
                        task.setCompleted(!task.isCompleted());
                        repository.saveTask(task);
                        reminderScheduler.scheduleTaskReminder(task);
                        reminderScheduler.syncTaskCalendarEvent(task);
                        if (task.isCompleted()) {
                            toast(encouragementMessage(repository.getMascotChoice(), "task"));
                        }
                        onChanged.run();
                    } else {
                        confirmDelete("Xóa công việc?", task.getTitle(), () -> {
                            reminderScheduler.deleteTaskAndLinkedCalendar(task);
                            reminderScheduler.cancelTaskReminder(task);
                            onChanged.run();
                        });
                    }
                })
                .show();
    }

    void showEventActions(StudyEvent event) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_detail, null);
        boolean hasOnlineLink = isWebUrl(event.getRoom());

        setText(dialogView, R.id.textEventType, eventTypeLabel(event.getType()));
        setText(dialogView, R.id.textEventTitle, event.getTitle());
        setText(dialogView, R.id.textEventTime,
                DateTimeUtils.formatDayLabel(event.getStartAt())
                        + " • " + DateTimeUtils.formatDateTime(event.getStartAt())
                        + " - " + DateTimeUtils.formatTime(event.getEndAt()));
        boolean hasLocation = !TextUtils.isEmpty(event.getRoom());
        TextView locationLabel = dialogView.findViewById(R.id.textEventLocationLabel);
        TextView location = dialogView.findViewById(R.id.textEventLocation);
        String locationValue = hasLocation ? event.getRoom() : "";
        locationLabel.setVisibility(hasLocation ? View.VISIBLE : View.GONE);
        location.setVisibility(hasLocation ? View.VISIBLE : View.GONE);
        locationLabel.setText(hasOnlineLink ? "LINK ONLINE" : "ĐỊA ĐIỂM");
        location.setText(locationValue);
        location.setTextColor(getColor(hasOnlineLink ? R.color.accent_blue : R.color.ink));
        location.setOnClickListener(v -> {
            if (hasOnlineLink) {
                openUrl(event.getRoom());
            }
        });
        setText(dialogView, R.id.textEventReminder, event.isReminderEnabled()
                ? "Nhắc trước: " + reminderLabel(event.getReminderBeforeMinutes())
                : "Nhắc nhở: Tắt");
        setText(dialogView, R.id.textEventNote, TextUtils.isEmpty(event.getNote())
                ? "Ghi chú: Chưa có ghi chú"
                : "Ghi chú: " + event.getNote());

        TextView openLink = dialogView.findViewById(R.id.btnOpenLink);
        openLink.setVisibility(hasOnlineLink ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        openLink.setOnClickListener(v -> openUrl(event.getRoom()));
        dialogView.findViewById(R.id.btnEditEvent).setOnClickListener(v -> {
            dialog.dismiss();
            showEventDialog(event, () -> scheduleController.showCurrent());
        });
        dialogView.findViewById(R.id.btnDeleteEvent).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteEvent(event);
        });
        dialogView.findViewById(R.id.btnCloseEvent).setOnClickListener(v -> dialog.dismiss());
        dialog.setOnShowListener(d -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void confirmDeleteEvent(StudyEvent event) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch")
                .setMessage("Bạn có chắc muốn xóa lịch này không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    repository.deleteEvent(event.getId());
                    if (!TextUtils.isEmpty(event.getSourceTaskId())) {
                        repository.setTaskCalendarVisibility(event.getSourceTaskId(), false);
                    }
                    reminderScheduler.cancelEventReminder(event);
                    toast("Đã xóa lịch");
                    scheduleController.showCurrent();
                })
                .show();
    }

    void showMoveEventConfirmation(StudyEvent event, long newStartAt) {
        long duration = Math.max(30L * 60L * 1000L, event.getEndAt() - event.getStartAt());
        StudyEvent moved = new StudyEvent(
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getSubject(),
                newStartAt,
                newStartAt + duration,
                event.getRoom(),
                event.getNote(),
                event.isReminderEnabled(),
                event.getReminderBeforeMinutes(),
                event.getSourceTaskId()
        );
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật giờ?")
                .setMessage(event.getTitle() + "\nChuyển sang " + DateTimeUtils.formatDateTime(moved.getStartAt()) + " - " + DateTimeUtils.formatTime(moved.getEndAt()))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    repository.saveEvent(moved);
                    reminderScheduler.scheduleEventReminder(moved);
                    scheduleController.showCurrent();
                })
                .show();
    }

    private String eventTimeMeta(StudyEvent event) {
        String meta = DateTimeUtils.formatDayLabel(event.getStartAt())
                + " • " + DateTimeUtils.formatTime(event.getStartAt())
                + " - " + DateTimeUtils.formatTime(event.getEndAt());
        if (!TextUtils.isEmpty(event.getRoom())) {
            meta += " • " + event.getRoom();
        }
        return meta;
    }

    private String eventDetailText(StudyEvent event) {
        String reminder = event.isReminderEnabled()
                ? "\nNhắc trước: " + reminderLabel(event.getReminderBeforeMinutes())
                : "\nNhắc nhở: Tắt";
        String room = TextUtils.isEmpty(event.getRoom()) ? "" : event.getRoom();
        String locationLine = TextUtils.isEmpty(room) ? "" : "\n" + (isWebUrl(room) ? "Link online: " : "Địa điểm: ") + room;
        String note = TextUtils.isEmpty(event.getNote()) ? "" : "\nGhi chú: " + event.getNote();
        String subject = TextUtils.isEmpty(event.getSubject()) ? "Chưa có nội dung liên quan" : event.getSubject();
        return eventTypeLabel(event.getType())
                + " • " + subject
                + "\n" + DateTimeUtils.formatDateTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt())
                + locationLine
                + reminder
                + note;
    }

    private void showConflictBeforeSave(StudyEvent event, List<StudyEvent> conflicts, Runnable onConfirmSave) {
        StringBuilder message = new StringBuilder();
        message.append("Lịch này đang bị trùng với:\n");
        for (StudyEvent conflict : conflicts) {
            message.append("- ")
                    .append(conflict.getTitle())
                    .append(" từ ")
                    .append(DateTimeUtils.formatTime(conflict.getStartAt()))
                    .append(" đến ")
                    .append(DateTimeUtils.formatTime(conflict.getEndAt()))
                    .append(".\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Phát hiện xung đột lịch")
                .setMessage(message.toString().trim())
                .setNegativeButton("Quay lại sửa", null)
                .setPositiveButton("Vẫn lưu", (dialog, which) -> onConfirmSave.run())
                .show();
    }

    void confirmDelete(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> onConfirm.run())
                .show();
    }

    void showImageImportOptions() {
        String[] actions = {"Chụp ảnh", "Tải ảnh từ máy"};
        new AlertDialog.Builder(this)
                .setTitle("Tạo lịch tự động")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        galleryLauncher.launch("image/*");
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
            if (intent.resolveActivity(getPackageManager()) == null) {
                toast("Thiết bị chưa có ứng dụng camera phù hợp");
                return;
            }
            cameraLauncher.launch(intent);
        } catch (Exception exception) {
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
                    UserProfile profile = repository.getProfile();
                    adminPortalClient.reportIssue("ai", profile.getEmail(), message);
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
            List<StudyEvent> conflicts = importConflicts(event, events);
            if (!conflicts.isEmpty()) {
                conflictEvents.add(event);
            }
            preview.append(conflicts.isEmpty() ? "" : "[Trùng] ")
                    .append(event.getTitle())
                    .append("\n")
                    .append(DateTimeUtils.formatDateTime(event.getStartAt()))
                    .append(" - ")
                    .append(DateTimeUtils.formatTime(event.getEndAt()))
                    .append("\n");
            if (!conflicts.isEmpty()) {
                preview.append("Trùng với: ")
                        .append(eventTitles(conflicts))
                        .append("\n");
            }
            preview.append("\n");
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
                    toast("Đã tạo " + events.size() + " lịch từ ảnh");
                    scheduleController.showAllAround(events.get(0).getStartAt());
                })
                .show();
    }

    private List<StudyEvent> importConflicts(StudyEvent candidate, List<StudyEvent> importedEvents) {
        List<StudyEvent> conflicts = new ArrayList<>(repository.getConflicts(candidate));
        if (StudyEvent.TYPE_DEADLINE.equals(candidate.getType())) {
            return conflicts;
        }
        for (StudyEvent event : importedEvents) {
            if (event.getId().equals(candidate.getId()) || StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
                continue;
            }
            if (DateTimeUtils.rangesOverlap(candidate.getStartAt(), candidate.getEndAt(), event.getStartAt(), event.getEndAt())) {
                conflicts.add(event);
            }
        }
        return conflicts;
    }

    private String eventTitles(List<StudyEvent> events) {
        StringBuilder builder = new StringBuilder();
        for (StudyEvent event : events) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(event.getTitle());
        }
        return builder.toString();
    }

    private View createTaskRow(StudyTask task, boolean interactive, ViewGroup parent) {
        return createTaskRow(task, interactive, parent, null);
    }

    private View createTaskRow(StudyTask task, boolean interactive, ViewGroup parent, Runnable onChanged) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_task, parent, false);
        TextView title = row.findViewById(R.id.textTitle);
        TextView meta = row.findViewById(R.id.textMeta);
        TextView details = row.findViewById(R.id.textDetails);
        TextView marker = row.findViewById(R.id.textMarker);
        CheckBox done = row.findViewById(R.id.checkDone);
        TextView actionDone = row.findViewById(R.id.btnTaskDone);
        TextView actionEdit = row.findViewById(R.id.btnTaskEdit);
        TextView actionDelete = row.findViewById(R.id.btnTaskDelete);
        View taskActions = row.findViewById(R.id.taskActions);
        title.setText(task.getTitle());
        meta.setText("To-do");
        details.setText(taskDetails(task));
        bindTaskMarker(marker, task);
        if (row instanceof SwipeActionLayout) {
            ((SwipeActionLayout) row).setSwipeEnabled(interactive);
        }
        taskActions.setVisibility(interactive ? View.VISIBLE : View.GONE);
        actionDone.setText(task.isCompleted() ? "Mở lại" : "Xong");
        done.setChecked(task.isCompleted());
        done.setEnabled(interactive);
        done.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!interactive) {
                return;
            }
            task.setCompleted(isChecked);
            repository.saveTask(task);
            reminderScheduler.scheduleTaskReminder(task);
            reminderScheduler.syncTaskCalendarEvent(task);
            if (isChecked) {
                toast(encouragementMessage(repository.getMascotChoice(), "task"));
            }
            if (onChanged != null) {
                onChanged.run();
            }
        });
        actionDone.setOnClickListener(v -> {
            task.setCompleted(!task.isCompleted());
            repository.saveTask(task);
            reminderScheduler.scheduleTaskReminder(task);
            reminderScheduler.syncTaskCalendarEvent(task);
            if (task.isCompleted()) {
                toast(encouragementMessage(repository.getMascotChoice(), "task"));
            }
            if (onChanged != null) {
                onChanged.run();
            }
        });
        actionEdit.setOnClickListener(v -> showTaskDialog(task, onChanged == null ? () -> {
        } : onChanged));
        marker.setEnabled(interactive);
        marker.setOnClickListener(v -> {
            if (interactive) {
                showTaskMarkerPopup(marker, task, onChanged);
            }
        });
        actionDelete.setOnClickListener(v -> confirmDelete("Xóa công việc?", task.getTitle(), () -> {
            reminderScheduler.deleteTaskAndLinkedCalendar(task);
            reminderScheduler.cancelTaskReminder(task);
            if (onChanged != null) {
                onChanged.run();
            }
        }));
        if (task.isCompleted()) {
            title.setAlpha(0.55f);
            meta.setAlpha(0.55f);
            marker.setAlpha(0.55f);
            title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            title.setPaintFlags(title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        return row;
    }

    private void bindTaskMarker(TextView marker, StudyTask task) {
        String type = task.getMarkerType();
        String value = task.getMarkerValue();
        marker.setText(markerText(type, value));
        marker.setTextColor(markerTextColor(type, value));
        marker.setBackground(markerBackground(type, value, isMarked(task) ? Color.TRANSPARENT : Color.parseColor("#E5E7EB")));
    }

    private void showTaskMarkerPopup(View anchor, StudyTask task, Runnable onChanged) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(10));
        background.setStroke(dp(1), Color.parseColor("#E5E7EB"));
        content.setBackground(background);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setText("Đánh dấu bằng ký hiệu");
        title.setTextColor(getColor(R.color.ink));
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView clear = new TextView(this);
        clear.setText("Xóa");
        clear.setTextColor(getColor(R.color.muted));
        clear.setTextSize(14f);
        clear.setPadding(dp(12), dp(8), dp(4), dp(8));
        titleRow.addView(title);
        titleRow.addView(clear);
        content.addView(titleRow);

        PopupWindow popup = new PopupWindow(content, dp(300), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(dp(6));
        }
        clear.setOnClickListener(v -> {
            task.setMarkerType("flag");
            task.setMarkerValue("");
            repository.saveTask(task);
            popup.dismiss();
            if (onChanged != null) {
                onChanged.run();
            }
        });

        addMarkerSection(content, popup, task, onChanged, "Lá cờ", "flag",
                new String[]{"#EC4899", "#FBBF24", "#A855F7", "#3B82F6", "#22C55E"});
        addMarkerSection(content, popup, task, onChanged, "Con số", "number",
                new String[]{"1", "2", "3", "4", "5"});
        addMarkerSection(content, popup, task, onChanged, "Tiến triển", "progress",
                new String[]{"1", "2", "3", "4", "5"});
        addMarkerSection(content, popup, task, onChanged, "Khí sắc", "mood",
                new String[]{"great", "good", "normal", "tired", "bad"});

        popup.showAsDropDown(anchor, -dp(252), -dp(4));
    }

    private void addMarkerSection(LinearLayout content, PopupWindow popup, StudyTask task, Runnable onChanged, String label, String type, String[] values) {
        TextView section = new TextView(this);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionParams.setMargins(0, dp(14), 0, dp(4));
        section.setLayoutParams(sectionParams);
        section.setText(label);
        section.setTextColor(getColor(R.color.muted));
        section.setTextSize(14f);
        section.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(section);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        for (String value : values) {
            TextView option = markerOption(type, value, task);
            option.setOnClickListener(v -> {
                task.setMarkerType(type);
                task.setMarkerValue(value);
                repository.saveTask(task);
                popup.dismiss();
                if (onChanged != null) {
                    onChanged.run();
                }
            });
            row.addView(option);
        }
        content.addView(row);
    }

    private TextView markerOption(String type, String value, StudyTask task) {
        TextView option = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
        params.setMargins(0, 0, dp(12), 0);
        option.setLayoutParams(params);
        option.setGravity(android.view.Gravity.CENTER);
        option.setText(markerText(type, value));
        option.setTextColor(markerTextColor(type, value));
        option.setTextSize("flag".equals(type) || "mood".equals(type) ? 24f : 16f);
        option.setTypeface(null, android.graphics.Typeface.BOLD);
        int stroke = type.equals(task.getMarkerType()) && value.equals(task.getMarkerValue())
                ? getColor(R.color.ink)
                : Color.TRANSPARENT;
        option.setBackground(markerBackground(type, value, stroke));
        return option;
    }

    private GradientDrawable markerBackground(String type, String value, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape("flag".equals(type) ? GradientDrawable.RECTANGLE : GradientDrawable.OVAL);
        drawable.setCornerRadius(dp(12));
        if ("number".equals(type)) {
            drawable.setColor(markerColor(type, value));
        } else if ("progress".equals(type)) {
            drawable.setColor(Color.parseColor("#DBEAFE"));
        } else if ("mood".equals(type)) {
            drawable.setColor(Color.parseColor("#FEF3C7"));
        } else {
            drawable.setColor(Color.TRANSPARENT);
        }
        drawable.setStroke(dp(strokeColor == Color.TRANSPARENT ? 0 : 2), strokeColor);
        return drawable;
    }

    private boolean isMarked(StudyTask task) {
        return !TextUtils.isEmpty(task.getMarkerValue());
    }

    private String markerText(String type, String value) {
        if ("number".equals(type)) {
            return TextUtils.isEmpty(value) ? "1" : value;
        }
        if ("progress".equals(type)) {
            if ("1".equals(value)) return "\u25D4";
            if ("2".equals(value)) return "\u25D1";
            if ("3".equals(value)) return "\u25D0";
            if ("4".equals(value)) return "\u25D5";
            return "\u25CF";
        }
        if ("mood".equals(type)) {
            if ("great".equals(value)) return "\uD83D\uDE03";
            if ("good".equals(value)) return "\uD83D\uDE0A";
            if ("normal".equals(value)) return "\uD83D\uDE10";
            if ("tired".equals(value)) return "\uD83D\uDE14";
            return "\uD83D\uDE16";
        }
        return "\u2691";
    }

    private int markerTextColor(String type, String value) {
        if ("number".equals(type)) {
            return Color.WHITE;
        }
        if ("progress".equals(type)) {
            return Color.parseColor("#3B82F6");
        }
        if ("mood".equals(type)) {
            return getColor(R.color.ink);
        }
        return TextUtils.isEmpty(value) ? Color.parseColor("#9CA3AF") : markerColor(type, value);
    }

    private int markerColor(String type, String value) {
        if ("flag".equals(type) && !TextUtils.isEmpty(value)) {
            return Color.parseColor(value);
        }
        if ("1".equals(value)) return Color.parseColor("#EC4899");
        if ("2".equals(value)) return Color.parseColor("#FBBF24");
        if ("3".equals(value)) return Color.parseColor("#A855F7");
        if ("4".equals(value)) return Color.parseColor("#3B82F6");
        if ("5".equals(value)) return Color.parseColor("#22C55E");
        return getColor(R.color.accent_blue);
    }

    View createEventRow(StudyEvent event, ViewGroup parent) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_event, parent, false);
        row.setBackgroundResource(eventBackground(event.getType()));
        setText(row, R.id.textTitle, event.getTitle());
        setText(row, R.id.textType, eventTypeLabel(event.getType()));
        setText(row, R.id.textTime, DateTimeUtils.formatDayLabel(event.getStartAt()) + " • " + DateTimeUtils.formatTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt()));
        setEventSubjectText(row, event);
        TextView location = row.findViewById(R.id.textLocation);
        String locationValue = TextUtils.isEmpty(event.getRoom()) ? "" : event.getRoom();
        location.setVisibility(TextUtils.isEmpty(locationValue) ? View.GONE : View.VISIBLE);
        location.setText(TextUtils.isEmpty(locationValue) ? "" : (isWebUrl(locationValue) ? "Link online: " : "Địa điểm: ") + locationValue);
        location.setOnClickListener(v -> {
            if (isWebUrl(locationValue)) {
                openUrl(locationValue);
            }
        });
        setText(row, R.id.textReminder, event.isReminderEnabled()
                ? "Nhắc trước: " + reminderLabel(event.getReminderBeforeMinutes())
                : "Nhắc nhở: Tắt");
        String conflict = repository.hasConflict(event) ? "[Trùng lịch] " : "";
        String note = TextUtils.isEmpty(event.getNote()) ? "Chưa có ghi chú" : event.getNote();
        setText(row, R.id.textNote, conflict + "Ghi chú: " + note);
        row.setRotation(event.getId().hashCode() % 2 == 0 ? -0.8f : 0.8f);
        return row;
    }

    private void setEventSubjectText(View row, StudyEvent event) {
        TextView subjectView = row.findViewById(R.id.textSubject);
        String subject = event.getSubject() == null ? "" : event.getSubject().trim();
        String title = event.getTitle() == null ? "" : event.getTitle().trim();
        if (TextUtils.isEmpty(subject) || subject.equalsIgnoreCase(title)) {
            subjectView.setVisibility(View.GONE);
            subjectView.setText("");
            return;
        }
        subjectView.setVisibility(View.VISIBLE);
        subjectView.setText(eventSubjectLabel(event));
    }

    private String eventSubjectLabel(StudyEvent event) {
        String subject = TextUtils.isEmpty(event.getSubject()) ? "Chưa có nội dung liên quan" : event.getSubject();
        if (StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
            return "Nội dung: " + subject;
        }
        if (StudyEvent.TYPE_PERSONAL.equals(event.getType())) {
            return "Nhóm việc: " + subject;
        }
        return "Môn học: " + subject;
    }

    private String eventTypeLabel(String type) {
        if (StudyEvent.TYPE_EXAM.equals(type)) {
            return "Lịch thi";
        }
        if (StudyEvent.TYPE_DEADLINE.equals(type)) {
            return "Deadline";
        }
        if (StudyEvent.TYPE_PERSONAL.equals(type)) {
            return "Cá nhân";
        }
        return "Lịch học";
    }

    private boolean isWebUrl(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String normalized = normalizeUrl(value);
        return Patterns.WEB_URL.matcher(normalized).matches();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(url))));
        } catch (Exception exception) {
            toast("Không mở được link này");
        }
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim();
        String lower = value.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
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

    private TextView taskSectionHeader(String title) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        params.setMargins(0, dp(10), 0, dp(8));
        view.setLayoutParams(params);
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        view.setBackgroundResource(R.drawable.bg_selected_pill);
        view.setText(title);
        view.setTextColor(getColor(R.color.ink));
        view.setTextSize(15f);
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

    private String taskDetails(StudyTask task) {
        return task.isCompleted() ? "Đã hoàn thành" : "Đang làm";
    }

    private String quadrantLabel(StudyTask task) {
        if (task.isImportant() && task.isUrgent()) {
            return "Quan trọng + khẩn cấp";
        }
        if (task.isImportant()) {
            return "Quan trọng";
        }
        if (task.isUrgent()) {
            return "Khẩn cấp";
        }
        return "Không khẩn cấp";
    }

    TextView emptyState(String message) {
        TextView view = new TextView(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
        view.setGravity(android.view.Gravity.CENTER);
        view.setText(message);
        view.setTextColor(getColor(R.color.muted));
        view.setBackgroundResource(R.drawable.bg_card);
        return view;
    }

    void bindFilter(View screen, int id, String value, String active, Runnable action) {
        TextView view = screen.findViewById(id);
        view.setBackgroundResource(value.equals(active) ? R.drawable.bg_selected_pill : R.drawable.bg_outline_pill);
        view.setOnClickListener(v -> action.run());
    }

    private void pickDateTime(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, R.style.StudyPickerDialog, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog timePicker = new TimePickerDialog(this, R.style.StudyPickerDialog, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                target.setText(DateTimeUtils.formatDateTime(calendar.getTimeInMillis()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePicker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    void pickDate(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, R.style.StudyPickerDialog, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            target.setText(DateTimeUtils.formatDate(calendar.getTimeInMillis()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, R.style.StudyPickerDialog, (view, hourOfDay, minute) -> target.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void fillEventTimeInputs(EditText date, EditText start, EditText end, long startAt, long endAt) {
        date.setText(DateTimeUtils.formatDate(startAt));
        start.setText(DateTimeUtils.formatTime(startAt));
        end.setText(DateTimeUtils.formatTime(endAt));
    }

    private boolean matchesTaskFilter(StudyTask task, String filter) {
        if (TASK_FILTER_TODAY.equals(filter)) {
            return DateTimeUtils.isToday(task.getDueAt());
        }
        if (TASK_FILTER_SOON.equals(filter)) {
            return DateTimeUtils.isSoon(task.getDueAt()) && !task.isCompleted();
        }
        if (TASK_FILTER_OVERDUE.equals(filter)) {
            return task.getDueAt() < System.currentTimeMillis() && !task.isCompleted();
        }
        if (TASK_FILTER_DONE.equals(filter)) {
            return task.isCompleted();
        }
        if (filter.startsWith(TASK_FILTER_TAG_PREFIX)) {
            return filter.substring(TASK_FILTER_TAG_PREFIX.length()).equals(task.getTag());
        }
        if (filter.startsWith(TASK_FILTER_PRIORITY_PREFIX)) {
            return filter.substring(TASK_FILTER_PRIORITY_PREFIX.length()).equals(task.getPriority());
        }
        return true;
    }

    private int dashboardBackgroundColorRes(String choice) {
        if ("Xanh dịu".equals(choice)) {
            return R.color.blue;
        }
        if ("Tím học tập".equals(choice)) {
            return R.color.lavender;
        }
        if ("Vàng note".equals(choice)) {
            return R.color.yellow_soft;
        }
        if ("Hồng nhẹ".equals(choice)) {
            return R.color.pink;
        }
        return R.color.paper_light;
    }

    private int themeColorRes(String choice) {
        if ("Xanh biển".equals(choice)) {
            return R.color.accent_blue;
        }
        if ("Xanh lá".equals(choice)) {
            return R.color.mint;
        }
        if ("Vàng".equals(choice)) {
            return R.color.yellow;
        }
        if ("Tím".equals(choice)) {
            return R.color.lavender;
        }
        return R.color.rose;
    }

    private int themeButtonTextColorRes(String choice) {
        if ("Xanh lá".equals(choice) || "Vàng".equals(choice) || "Tím".equals(choice)) {
            return R.color.ink;
        }
        return R.color.white;
    }

    private void bindSpinner(Spinner spinner, String[] values, String selected) {
        spinner.setAdapter(studySpinnerAdapter(values));
        spinner.setSelection(indexOf(values, selected));
    }

    private ArrayAdapter<String> studySpinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_study, values);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_study);
        return adapter;
    }

    private void tintButton(View root, int id, int backgroundColor, int textColor) {
        MaterialButton button = root.findViewById(id);
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(backgroundColor)));
        button.setTextColor(getColor(textColor));
    }

    private List<String> uniqueTaskTags() {
        List<String> tags = new ArrayList<>();
        for (StudyTask task : repository.getTasks()) {
            String tag = task.getTag();
            if (!TextUtils.isEmpty(tag) && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
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
            return R.drawable.bg_danger_soft;
        }
        if (StudyEvent.TYPE_DEADLINE.equals(type)) {
            return R.drawable.bg_card_lavender;
        }
        if (StudyEvent.TYPE_PERSONAL.equals(type)) {
            return R.drawable.bg_card_mint;
        }
        return R.drawable.bg_card_yellow;
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private int indexOfReminder(int minutes) {
        for (int i = 0; i < REMINDER_MINUTES.length; i++) {
            if (REMINDER_MINUTES[i] == minutes) {
                return i;
            }
        }
        return 2;
    }

    private String reminderLabel(int minutes) {
        int index = indexOfReminder(minutes);
        return REMINDER_LABELS[index];
    }

    boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationPermissionRequestInFlight) {
            return;
        }
        notificationPermissionRequestInFlight = true;
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private int parsePositiveInt(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return 0;
        }
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

    String textOf(EditText editText) {
        return editText.getText().toString().trim();
    }

    private boolean isValidEmail(String value) {
        return !TextUtils.isEmpty(value) && android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }

    void setText(View root, int id, String text) {
        ((TextView) root.findViewById(id)).setText(text);
    }

    int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
