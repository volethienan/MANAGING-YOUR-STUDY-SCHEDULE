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

import com.example.cuoiky_qllichhoctap.data.GeminiScheduleExtractor;
import com.example.cuoiky_qllichhoctap.data.StudyRepository;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;
import com.example.cuoiky_qllichhoctap.ui.SwipeActionLayout;
import com.example.cuoiky_qllichhoctap.ui.WeekCalendarView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;

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
    private static final String CALENDAR_DAY = "Ngày";
    private static final String CALENDAR_THREE_DAYS = "3 ngày";
    private static final String CALENDAR_WEEK = "Tuần";

    private StudyRepository repository;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private FrameLayout contentFrame;
    private LinearLayout bottomNav;
    private CountDownTimer pomodoroTimer;
    private long pomodoroRemainingMillis = 25L * 60L * 1000L;
    private boolean pomodoroRunning;
    private long scheduleWeekStartMillis = DateTimeUtils.startOfWeek(System.currentTimeMillis());
    private String scheduleFilter = "Tất cả";
    private String scheduleViewMode = CALENDAR_WEEK;
    private Uri pendingCameraUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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
        firebaseAuth = FirebaseAuth.getInstance();
        contentFrame = findViewById(R.id.contentFrame);
        bottomNav = findViewById(R.id.bottomNav);
        setupGoogleSignIn();
        setupImageLaunchers();
        setupBottomNav();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (repository.isFirstOpen()) {
            showOnboarding();
        } else if (canEnterWithFirebaseUser(firebaseUser)) {
            syncProfileFromFirebase(firebaseUser);
            repository.setLoggedIn(true);
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

    private void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                toast("Bạn đã hủy đăng nhập Google");
                return;
            }
            handleGoogleSignInResult(result.getData());
        });
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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        EditText email = screen.findViewById(R.id.inputEmail);
        EditText password = screen.findViewById(R.id.inputPassword);
        if (currentUser != null && !TextUtils.isEmpty(currentUser.getEmail())) {
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
            signInWithEmail(textOf(email), textOf(password));
        };
        screen.findViewById(R.id.btnLogin).setOnClickListener(login);
        screen.findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> startGoogleSignIn());
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
            registerWithEmailVerification(textOf(name), textOf(email), textOf(password));
        });
        screen.findViewById(R.id.textGoLogin).setOnClickListener(v -> showLogin());
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
            sendPasswordResetEmail(textOf(email));
        });
        screen.findViewById(R.id.textBackLogin).setOnClickListener(v -> showLogin());
    }

    private void signInWithEmail(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showAuthTaskError(task, "Không đăng nhập được");
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (!canEnterWithFirebaseUser(user)) {
                        showEmailVerificationRequired(user);
                        return;
                    }
                    syncProfileFromFirebase(user);
                    repository.finishOnboarding();
                    repository.setLoggedIn(true);
                    showDashboard();
                });
    }

    private void registerWithEmailVerification(String name, String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showAuthTaskError(task, "Không tạo được tài khoản");
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        toast("Không lấy được tài khoản Firebase");
                        return;
                    }
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    user.updateProfile(profileUpdate)
                            .addOnCompleteListener(profileTask -> sendRegistrationVerification(user, name, email));
                });
    }

    private void sendRegistrationVerification(FirebaseUser user, String name, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showAuthTaskError(task, "Không gửi được email xác thực");
                        return;
                    }
                    repository.saveProfile(new UserProfile(name, email, repository.getProfile().getGoal()));
                    repository.finishOnboarding();
                    firebaseAuth.signOut();
                    new AlertDialog.Builder(this)
                            .setTitle("Kiểm tra email")
                            .setMessage("Firebase đã gửi link xác thực đến " + email + ". Hãy mở email, bấm link xác thực rồi quay lại đăng nhập.")
                            .setPositiveButton("Về đăng nhập", (dialog, which) -> showLogin())
                            .show();
                });
    }

    private void sendPasswordResetEmail(String email) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        showAuthTaskError(task, "Không gửi được email đặt lại mật khẩu");
                        return;
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Đã gửi email")
                            .setMessage("Firebase đã gửi link đặt lại mật khẩu đến " + email + ". Hãy kiểm tra hộp thư rồi quay lại đăng nhập.")
                            .setPositiveButton("Về đăng nhập", (dialog, which) -> showLogin())
                            .show();
                });
    }

    private void startGoogleSignIn() {
        if (googleSignInClient == null || googleSignInLauncher == null) {
            toast("Google Sign-In chưa sẵn sàng");
            return;
        }
        toast("Đang mở đăng nhập Google...");
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || TextUtils.isEmpty(account.getIdToken())) {
                toast("Google không trả về ID token");
                return;
            }
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException exception) {
            toast("Đăng nhập Google lỗi: " + exception.getStatusCode());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        String message = task.getException() == null
                                ? "Không đăng nhập được bằng Google"
                                : task.getException().getMessage();
                        toast(message);
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    syncProfileFromFirebase(user);
                    repository.finishOnboarding();
                    repository.setLoggedIn(true);
                    toast("Đăng nhập Google thành công");
                    showDashboard();
                });
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
        normalizeScheduleStart();
        setupScheduleViewModes(screen);
        setupScheduleFilters(screen, filter);
        int visibleDays = scheduleVisibleDays();
        setText(screen, R.id.textWeekRange, DateTimeUtils.formatDayRange(scheduleWeekStartMillis, visibleDays));
        screen.findViewById(R.id.btnPrevWeek).setOnClickListener(v -> {
            scheduleWeekStartMillis = DateTimeUtils.addDays(scheduleWeekStartMillis, -visibleDays);
            showSchedule(scheduleFilter);
        });
        screen.findViewById(R.id.btnThisWeek).setOnClickListener(v -> {
            scheduleWeekStartMillis = CALENDAR_WEEK.equals(scheduleViewMode)
                    ? DateTimeUtils.startOfWeek(System.currentTimeMillis())
                    : DateTimeUtils.startOfDay(System.currentTimeMillis());
            showSchedule(scheduleFilter);
        });
        screen.findViewById(R.id.btnNextWeek).setOnClickListener(v -> {
            scheduleWeekStartMillis = DateTimeUtils.addDays(scheduleWeekStartMillis, visibleDays);
            showSchedule(scheduleFilter);
        });

        List<StudyEvent> visibleEvents = visibleRangeEvents(filter);
        WeekCalendarView weekCalendar = screen.findViewById(R.id.weekCalendar);
        weekCalendar.setRange(scheduleWeekStartMillis, visibleDays);
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

    private void setupScheduleViewModes(View screen) {
        bindCalendarMode(screen, R.id.modeDay, CALENDAR_DAY);
        bindCalendarMode(screen, R.id.modeThreeDays, CALENDAR_THREE_DAYS);
        bindCalendarMode(screen, R.id.modeWeek, CALENDAR_WEEK);
    }

    private void bindCalendarMode(View screen, int id, String mode) {
        TextView view = screen.findViewById(id);
        view.setBackgroundResource(mode.equals(scheduleViewMode) ? R.drawable.bg_selected_pill : R.drawable.bg_outline_pill);
        view.setOnClickListener(v -> {
            if (mode.equals(scheduleViewMode)) {
                return;
            }
            long focusedDay = DateTimeUtils.startOfDay(scheduleWeekStartMillis);
            scheduleViewMode = mode;
            scheduleWeekStartMillis = CALENDAR_WEEK.equals(mode) ? DateTimeUtils.startOfWeek(focusedDay) : focusedDay;
            showSchedule(scheduleFilter);
        });
    }

    private void setupScheduleFilters(View screen, String active) {
        bindFilter(screen, R.id.filterAll, "Tất cả", active, () -> showSchedule("Tất cả"));
        bindFilter(screen, R.id.filterStudy, StudyEvent.TYPE_STUDY, active, () -> showSchedule(StudyEvent.TYPE_STUDY));
        bindFilter(screen, R.id.filterExam, StudyEvent.TYPE_EXAM, active, () -> showSchedule(StudyEvent.TYPE_EXAM));
        bindFilter(screen, R.id.filterDeadline, StudyEvent.TYPE_DEADLINE, active, () -> showSchedule(StudyEvent.TYPE_DEADLINE));
    }

    private List<StudyEvent> visibleRangeEvents(String filter) {
        List<StudyEvent> result = new ArrayList<>();
        int visibleDays = scheduleVisibleDays();
        for (StudyEvent event : repository.getEvents()) {
            if (!DateTimeUtils.isInDayRange(event.getStartAt(), scheduleWeekStartMillis, visibleDays)) {
                continue;
            }
            if (!"Tất cả".equals(filter) && !event.getType().equals(filter)) {
                continue;
            }
            result.add(event);
        }
        return result;
    }

    private int scheduleVisibleDays() {
        if (CALENDAR_DAY.equals(scheduleViewMode)) {
            return 1;
        }
        if (CALENDAR_THREE_DAYS.equals(scheduleViewMode)) {
            return 3;
        }
        return 7;
    }

    private void normalizeScheduleStart() {
        scheduleWeekStartMillis = CALENDAR_WEEK.equals(scheduleViewMode)
                ? DateTimeUtils.startOfWeek(scheduleWeekStartMillis)
                : DateTimeUtils.startOfDay(scheduleWeekStartMillis);
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
            firebaseAuth.signOut();
            if (googleSignInClient != null) {
                googleSignInClient.signOut();
            }
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
                        onChanged.run();
                    } else {
                        confirmDelete("Xóa công việc?", task.getTitle(), () -> {
                            repository.deleteTask(task.getId());
                            onChanged.run();
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
                    scheduleWeekStartMillis = CALENDAR_WEEK.equals(scheduleViewMode)
                            ? DateTimeUtils.startOfWeek(events.get(0).getStartAt())
                            : DateTimeUtils.startOfDay(events.get(0).getStartAt());
                    toast("Đã tạo " + events.size() + " lịch từ ảnh");
                    showSchedule("Tất cả");
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
        TextView priority = row.findViewById(R.id.textPriority);
        CheckBox done = row.findViewById(R.id.checkDone);
        TextView actionDone = row.findViewById(R.id.btnTaskDone);
        TextView actionEdit = row.findViewById(R.id.btnTaskEdit);
        TextView actionDelete = row.findViewById(R.id.btnTaskDelete);
        View taskActions = row.findViewById(R.id.taskActions);
        title.setText(task.getTitle());
        meta.setText(task.getSubject() + " • " + DateTimeUtils.formatDateTime(task.getDueAt()));
        priority.setText(task.getPriority());
        priority.setBackgroundResource(priorityBackground(task.getPriority()));
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
            if (onChanged != null) {
                onChanged.run();
            }
        });
        actionDone.setOnClickListener(v -> {
            task.setCompleted(!task.isCompleted());
            repository.saveTask(task);
            if (onChanged != null) {
                onChanged.run();
            }
        });
        actionEdit.setOnClickListener(v -> showTaskDialog(task, onChanged == null ? () -> {
        } : onChanged));
        actionDelete.setOnClickListener(v -> confirmDelete("Xóa công việc?", task.getTitle(), () -> {
            repository.deleteTask(task.getId());
            if (onChanged != null) {
                onChanged.run();
            }
        }));
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

    private boolean canEnterWithFirebaseUser(FirebaseUser user) {
        if (user == null) {
            return false;
        }
        return user.isEmailVerified() || isGoogleUser(user);
    }

    private boolean isGoogleUser(FirebaseUser user) {
        for (UserInfo info : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private void showEmailVerificationRequired(FirebaseUser user) {
        String email = user == null || TextUtils.isEmpty(user.getEmail()) ? "email của bạn" : user.getEmail();
        new AlertDialog.Builder(this)
                .setTitle("Cần xác thực email")
                .setMessage("Tài khoản " + email + " chưa bấm link xác thực. Hãy mở email từ Firebase rồi đăng nhập lại.")
                .setNegativeButton("Đã hiểu", null)
                .setPositiveButton("Gửi lại link", (dialog, which) -> resendEmailVerification(user))
                .show();
    }

    private void resendEmailVerification(FirebaseUser user) {
        if (user == null) {
            toast("Không tìm thấy tài khoản để gửi lại link");
            return;
        }
        user.sendEmailVerification()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        toast("Đã gửi lại email xác thực");
                    } else {
                        showAuthTaskError(task, "Không gửi lại được email xác thực");
                    }
                });
    }

    private void showAuthTaskError(Task<?> task, String fallback) {
        String message = task.getException() == null ? fallback : task.getException().getMessage();
        toast(TextUtils.isEmpty(message) ? fallback : message);
    }

    private void syncProfileFromFirebase(FirebaseUser user) {
        if (user == null) {
            return;
        }
        UserProfile current = repository.getProfile();
        String name = TextUtils.isEmpty(user.getDisplayName()) ? firstNameFromEmail(user.getEmail()) : user.getDisplayName();
        String email = TextUtils.isEmpty(user.getEmail()) ? "google-user@firebase.local" : user.getEmail();
        repository.saveProfile(new UserProfile(name, email, current.getGoal()));
    }

    private String firstNameFromEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return "Google User";
        }
        int at = email.indexOf("@");
        return at > 0 ? email.substring(0, at) : email;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
