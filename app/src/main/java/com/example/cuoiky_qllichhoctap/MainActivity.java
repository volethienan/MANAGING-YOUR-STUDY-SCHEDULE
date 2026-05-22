package com.example.cuoiky_qllichhoctap;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.media.MediaPlayer;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.cuoiky_qllichhoctap.model.PomodoroSession;
import java.util.UUID;

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
import com.example.cuoiky_qllichhoctap.data.AuthRepository;
import com.example.cuoiky_qllichhoctap.data.StudyRepository;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.AuthUser;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;
import com.example.cuoiky_qllichhoctap.ui.ComparisonBarChartView;
import com.example.cuoiky_qllichhoctap.ui.DonutChartView;
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

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int SCREEN_DASHBOARD = 0;
    private static final int SCREEN_SCHEDULE = 1;
    private static final int SCREEN_TASKS = 2;
    private static final int SCREEN_POMODORO = 3;
    private static final int SCREEN_STATS = 4;
    private static final int GOOGLE_SIGN_IN_DEVELOPER_ERROR = 10;
    private static final String CALENDAR_DAY = "Ngày";
    private static final String CALENDAR_THREE_DAYS = "3 ngày";
    private static final String CALENDAR_WEEK = "Tuần";
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
    private static final String[] REMINDER_LABELS = {"5 phút", "10 phút", "15 phút", "30 phút", "1 giờ", "1 ngày"};
    private static final int[] REMINDER_MINUTES = {5, 10, 15, 30, 60, 1440};
    private static final String[] AVATAR_CHOICES = {"Chữ viết tắt", "Robot học tập", "Mèo học tập", "Quyển sách", "Bạn học tập"};
    private static final String[] MASCOT_CHOICES = {"Robot học tập", "Mèo học tập", "Quyển sách", "Bạn học tập"};
    private static final String[] DASHBOARD_BACKGROUNDS = {"Giấy sáng", "Xanh dịu", "Tím học tập", "Vàng note", "Hồng nhẹ"};
    private static final String[] THEME_COLORS = {"Hoa hồng", "Xanh biển", "Xanh lá", "Vàng", "Tím"};

    private StudyRepository repository;
    private AuthRepository authRepository;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private FrameLayout contentFrame;
    private LinearLayout bottomNav;
    private CountDownTimer pomodoroTimer;
    private long pomodoroRemainingMillis = 25L * 60L * 1000L;
    private boolean pomodoroRunning;
    private static final String POMODORO_MODE_FOCUS = "focus";
    private static final String POMODORO_MODE_SHORT_BREAK = "short_break";
    private static final String POMODORO_MODE_LONG_BREAK = "long_break";
    private String currentPomodoroMode = POMODORO_MODE_FOCUS;
    private int pomodoroCycleCount = 0;
    private StudyTask currentPomodoroTask = null;
    private MediaPlayer backgroundAudioPlayer;
    private AudioTrack whiteNoiseTrack;
    private Thread whiteNoiseThread;
    private volatile boolean generatedNoisePlaying = false;
    private boolean isMuted = false;
    private float pomodoroSoundVolume = 0.7f;
    private String pomodoroSoundName = "tiếng mưa";
    private long pomodoroSessionStartMillis = 0;
    
    private long scheduleWeekStartMillis = DateTimeUtils.startOfWeek(System.currentTimeMillis());
    private String scheduleFilter = "Tất cả";
    private String scheduleViewMode = CALENDAR_WEEK;
    private Uri pendingCameraUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
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
        firebaseAuth = FirebaseAuth.getInstance();
        contentFrame = findViewById(R.id.contentFrame);
        bottomNav = findViewById(R.id.bottomNav);
        drawerLayout = findViewById(R.id.drawerLayout);
        setupGoogleSignIn();
        setupImageLaunchers();
        setupBottomNav();
        setupSideMenu();

        AuthUser localUser = authRepository.getCurrentUser();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (repository.isFirstOpen()) {
            showOnboarding();
        } else if (localUser != null) {
            activateStudyRepository(localUser.getEmail());
            syncProfileFromAuthUser(localUser);
            repository.setLoggedIn(true);
            showDashboard();
        } else if (canEnterWithFirebaseUser(firebaseUser)) {
            activateStudyRepository(firebaseUser.getEmail());
            syncProfileFromFirebase(firebaseUser);
            repository.setLoggedIn(true);
            showDashboard();
        } else {
            showLogin();
        }
    }

    private void setupSideMenu() {
        if (drawerLayout == null) return;
        findViewById(R.id.btnCloseMenu).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.menuItemAll).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showTasks("Tất cả"); });
        findViewById(R.id.menuItemToday).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showTasks("Hôm nay"); });
        findViewById(R.id.menuItemDone).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showTasks("Đã hoàn thành"); });
        findViewById(R.id.menuBtnSettings).setOnClickListener(v -> { drawerLayout.closeDrawer(GravityCompat.START); showSettings(); });
        findViewById(R.id.menuBtnNewGoal).setOnClickListener(v -> toast("Tính năng Mục tiêu sẽ ra mắt sớm!"));
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
            if (result.getData() != null) {
                handleGoogleSignInResult(result.getData());
                return;
            }
            if (result.getResultCode() != RESULT_OK) {
                toast("Google Sign-In không hoàn tất. Kiểm tra SHA-1/OAuth nếu bạn không tự hủy.");
                return;
            }
            toast("Google không trả về dữ liệu đăng nhập");
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
            int color = getColor(active ? R.color.ink : R.color.muted);
            button.setTextColor(color);
            button.setIconTint(android.content.res.ColorStateList.valueOf(color));
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(active ? R.color.yellow : android.R.color.transparent)));
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
            registerWithOtp(textOf(name), textOf(email), textOf(password));
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
        try {
            AuthUser user = authRepository.login(email, password);
            activateStudyRepository(user.getEmail());
            syncProfileFromAuthUser(user);
            repository.finishOnboarding();
            repository.setLoggedIn(true);
            showDashboard();
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
        }
    }

    private void registerWithOtp(String name, String email, String password) {
        try {
            String code = authRepository.beginRegistration(name, email, password);
            deliverOtpEmail(email, code, "xác thực tài khoản");
            showOtpVerification(email);
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
        }
    }

    private void showOtpVerification(String email) {
        View screen = inflateScreen(R.layout.screen_otp, false, SCREEN_DASHBOARD);
        setText(screen, R.id.textOtpEmail, email);
        EditText otp = screen.findViewById(R.id.inputOtp);
        screen.findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            if (isBlank(otp)) {
                toast("Vui lòng nhập mã OTP");
                return;
            }
            try {
                AuthUser user = authRepository.verifyRegistrationOtp(email, textOf(otp));
                activateStudyRepository(user.getEmail());
                syncProfileFromAuthUser(user);
                repository.finishOnboarding();
                repository.setLoggedIn(true);
                showDashboard();
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.btnResendOtp).setOnClickListener(v -> {
            try {
                String code = authRepository.resendRegistrationOtp(email);
                deliverOtpEmail(email, code, "xác thực tài khoản");
                toast("Đã gửi lại mã OTP");
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBack).setOnClickListener(v -> showRegister());
    }

    private void sendPasswordResetEmail(String email) {
        try {
            String code = authRepository.beginPasswordReset(email);
            deliverOtpEmail(email, code, "đặt lại mật khẩu");
            showResetPassword(email);
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
        }
    }

    private void showResetPassword(String email) {
        View screen = inflateScreen(R.layout.screen_reset_password, false, SCREEN_DASHBOARD);
        setText(screen, R.id.textResetEmail, email);
        EditText otp = screen.findViewById(R.id.inputOtp);
        EditText password = screen.findViewById(R.id.inputPassword);
        EditText confirm = screen.findViewById(R.id.inputConfirmPassword);
        screen.findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            if (isBlank(otp) || isBlank(password) || isBlank(confirm)) {
                toast("Vui lòng nhập đủ mã OTP và mật khẩu mới");
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
                authRepository.resetPassword(email, textOf(otp), textOf(password));
                new AlertDialog.Builder(this)
                        .setTitle("Đã cập nhật mật khẩu")
                        .setMessage("Bạn có thể đăng nhập bằng mật khẩu mới.")
                        .setPositiveButton("Về đăng nhập", (dialog, which) -> showLogin())
                        .show();
            } catch (IllegalArgumentException exception) {
                toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBackLogin).setOnClickListener(v -> showLogin());
    }

    private void deliverOtpEmail(String email, String code, String purpose) {
        new Thread(() -> {
            String lastError = "";
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("code", code);
                payload.put("purpose", purpose);

                for (String baseUrl : otpBackendCandidates()) {
                    try {
                        int status = postOtp(baseUrl, payload);
                        if (status >= 200 && status < 300) {
                            runOnUiThread(() -> toast("Đã gửi OTP tới email"));
                            return;
                        }
                        lastError = baseUrl + " trả về HTTP " + status;
                    } catch (Exception exception) {
                        lastError = baseUrl + ": " + exception.getMessage();
                    }
                }
            } catch (Exception exception) {
                lastError = exception.getMessage();
            }
            String detail = lastError;
            runOnUiThread(() -> showOtpSendError(detail));
        }).start();
    }

    private List<String> otpBackendCandidates() {
        List<String> urls = new ArrayList<>();
        addOtpBackendUrl(urls, BuildConfig.OTP_BACKEND_URL);
        addOtpBackendUrl(urls, "http://10.0.2.2:8080");
        addOtpBackendUrl(urls, "http://127.0.0.1:8080");
        return urls;
    }

    private void addOtpBackendUrl(List<String> urls, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        String normalized = trimTrailingSlash(url.trim());
        if (!urls.contains(normalized)) {
            urls.add(normalized);
        }
    }

    private int postOtp(String baseUrl, JSONObject payload) throws Exception {
        URL url = new URL(trimTrailingSlash(baseUrl) + "/send-otp");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] body = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        int status = connection.getResponseCode();
        connection.disconnect();
        return status;
    }

    private void showOtpSendError(String detail) {
        new AlertDialog.Builder(this)
                .setTitle("Không gửi được OTP")
                .setMessage("App chưa gọi được backend OTP.\n\n"
                        + "Nếu dùng Emulator: backend phải chạy và app dùng http://10.0.2.2:8080.\n"
                        + "Nếu dùng điện thoại thật: cần mở Windows Firewall port 8080 hoặc dùng adb reverse.\n\n"
                        + "Chi tiết: " + (TextUtils.isEmpty(detail) ? "không rõ lỗi" : detail))
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private String trimTrailingSlash(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
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
            toast(googleSignInErrorMessage(exception));
        }
    }

    private String googleSignInErrorMessage(ApiException exception) {
        int statusCode = exception.getStatusCode();
        if (statusCode == GOOGLE_SIGN_IN_DEVELOPER_ERROR) {
            return "Google Sign-In lỗi cấu hình SHA-1/OAuth. Kiểm tra Firebase Console.";
        }
        return "Đăng nhập Google lỗi: " + statusCode;
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
                    if (user != null) {
                        activateStudyRepository(user.getEmail());
                    }
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
            setText(screen, R.id.textNextEventMeta, DateTimeUtils.formatDayLabel(nextEvent.getStartAt()) + " • " + DateTimeUtils.formatTime(nextEvent.getStartAt()) + " - " + DateTimeUtils.formatTime(nextEvent.getEndAt()) + " • " + nextEvent.getRoom());
        }

        StudyTask nextTask = findNearestDeadlineTask(tasks);
        if (nextTask == null) {
            setText(screen, R.id.textDeadline, "Không có deadline");
            setText(screen, R.id.textDeadlineMeta, "Bạn đang khá thoáng lịch");
        } else {
            setText(screen, R.id.textDeadline, nextTask.getTitle());
            setText(screen, R.id.textDeadlineMeta, deadlineMeta(nextTask));
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
        screen.findViewById(R.id.btnPomodoroQuick).setOnClickListener(v -> showPomodoro());
        tintButton(screen, R.id.btnPomodoroQuick, themeColorRes(repository.getThemeColorChoice()), themeButtonTextColorRes(repository.getThemeColorChoice()));
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
            scheduleWeekStartMillis = startForScheduleMode(scheduleViewMode, System.currentTimeMillis());
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
        weekCalendar.setOnEmptySlotClickListener(startAt -> showEventDialog(null, () -> showSchedule(scheduleFilter), startAt));
        weekCalendar.setOnEventMoveRequestListener((event, newStartAt) -> showMoveEventConfirmation(event, newStartAt));

        LinearLayout eventList = screen.findViewById(R.id.eventList);
        eventList.removeAllViews();

        for (StudyEvent event : visibleEvents) {
            View row = createEventRow(event, eventList);
            row.setOnClickListener(v -> showEventActions(event));
            eventList.addView(row);
        }
        if (eventList.getChildCount() == 0) {
            eventList.addView(emptyState(scheduleVisibleDays() == 1 ? "Hôm nay chưa có lịch nào." : "Chưa có lịch trong khoảng này."));
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
            scheduleViewMode = mode;
            scheduleWeekStartMillis = startForScheduleMode(mode, System.currentTimeMillis());
            showSchedule(scheduleFilter);
        });
    }

    private void setupScheduleFilters(View screen, String active) {
        bindFilter(screen, R.id.filterAll, "Tất cả", active, () -> showSchedule("Tất cả"));
        bindFilter(screen, R.id.filterStudy, StudyEvent.TYPE_STUDY, active, () -> showSchedule(StudyEvent.TYPE_STUDY));
        bindFilter(screen, R.id.filterExam, StudyEvent.TYPE_EXAM, active, () -> showSchedule(StudyEvent.TYPE_EXAM));
        bindFilter(screen, R.id.filterDeadline, StudyEvent.TYPE_DEADLINE, active, () -> showSchedule(StudyEvent.TYPE_DEADLINE));
        bindFilter(screen, R.id.filterPersonal, StudyEvent.TYPE_PERSONAL, active, () -> showSchedule(StudyEvent.TYPE_PERSONAL));
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

    private long startForScheduleMode(String mode, long millis) {
        return CALENDAR_WEEK.equals(mode)
                ? DateTimeUtils.startOfWeek(millis)
                : DateTimeUtils.startOfDay(millis);
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

        List<StudyTask> tasks = repository.getTasks();
        if (TASK_FILTER_MATRIX.equals(filter)) {
            addQuadrantSection(taskList, tasks, "Quan trọng và khẩn cấp", true, true);
            addQuadrantSection(taskList, tasks, "Quan trọng nhưng không khẩn cấp", true, false);
            addQuadrantSection(taskList, tasks, "Không quan trọng nhưng khẩn cấp", false, true);
            addQuadrantSection(taskList, tasks, "Không quan trọng và không khẩn cấp", false, false);
            screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, () -> showTasks(filter)));
            return;
        }

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
        }
        if (taskList.getChildCount() == 0) {
            taskList.addView(emptyState("Chưa có công việc phù hợp"));
        }
        screen.findViewById(R.id.btnAddTask).setOnClickListener(v -> showTaskDialog(null, () -> showTasks(filter)));
    }

    private void setupTaskFilters(View screen, String active) {
        bindFilter(screen, R.id.filterAll, TASK_FILTER_ALL, active, () -> showTasks(TASK_FILTER_ALL));
        bindFilter(screen, R.id.filterToday, TASK_FILTER_TODAY, active, () -> showTasks(TASK_FILTER_TODAY));
        bindFilter(screen, R.id.filterSoon, TASK_FILTER_SOON, active, () -> showTasks(TASK_FILTER_SOON));
        bindFilter(screen, R.id.filterOverdue, TASK_FILTER_OVERDUE, active, () -> showTasks(TASK_FILTER_OVERDUE));
        bindFilter(screen, R.id.filterDone, TASK_FILTER_DONE, active, () -> showTasks(TASK_FILTER_DONE));
        bindFilter(screen, R.id.filterMatrix, TASK_FILTER_MATRIX, active, () -> showTasks(TASK_FILTER_MATRIX));
        bindDynamicTaskFilter(screen, R.id.filterTag, active.startsWith(TASK_FILTER_TAG_PREFIX), this::showTagFilterDialog);
        bindDynamicTaskFilter(screen, R.id.filterPriority, active.startsWith(TASK_FILTER_PRIORITY_PREFIX), this::showPriorityFilterDialog);
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

    private void showPomodoro() {
        View screen = inflateScreen(R.layout.screen_pomodoro, true, SCREEN_POMODORO);
        TextView timer = screen.findViewById(R.id.textTimer);
        TextView modeText = screen.findViewById(R.id.textTaskName);
        TextView textStartPause = screen.findViewById(R.id.btnStartPause);
        View btnStartPause = textStartPause;
        View layoutActions = screen.findViewById(R.id.btnSkip);
        View btnStop = screen.findViewById(R.id.btnReset);
        View btnSkip = screen.findViewById(R.id.btnSkip);
        View btnSelectMode = screen.findViewById(R.id.btnSelectMode);
        
        ImageView tomato1 = screen.findViewById(R.id.tomato1);
        ImageView tomato2 = screen.findViewById(R.id.tomato2);
        ImageView tomato3 = screen.findViewById(R.id.tomato3);
        ImageView tomato4 = screen.findViewById(R.id.tomato4);

        if (currentPomodoroTask != null) {
            modeText.setText(currentPomodoroTask.getTitle());
        } else {
            modeText.setText(POMODORO_MODE_FOCUS.equals(currentPomodoroMode) ? "Tập trung tự do" : "Đang nghỉ ngơi");
        }

        updatePomodoroUi(timer, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);

        btnStartPause.setOnClickListener(v -> {
            if (pomodoroRunning) {
                pausePomodoro();
            } else {
                startPomodoro(timer, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
            }
            updatePomodoroUi(timer, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
        });
        
        btnStop.setOnClickListener(v -> {
            resetPomodoro();
            updatePomodoroUi(timer, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
        });

        btnSkip.setOnClickListener(v -> {
            pausePomodoro();
            handlePomodoroEnd(timer, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
        });

        btnSelectMode.setOnClickListener(v -> showTaskSelectionDialog(modeText));
        
        screen.findViewById(R.id.btnSound).setOnClickListener(v -> showPomodoroSoundPanel());
        screen.findViewById(R.id.btnHistory).setOnClickListener(v -> showPomodoroHistory());
    }

    private void startPomodoro(TextView timer, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        if (pomodoroRemainingMillis == 0) return;
        if (pomodoroSessionStartMillis == 0) {
            pomodoroSessionStartMillis = System.currentTimeMillis();
        }
        pomodoroRunning = true;
        pomodoroTimer = new CountDownTimer(pomodoroRemainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                pomodoroRemainingMillis = millisUntilFinished;
                updatePomodoroUi(timer, textStartPause, layoutActions, t1, t2, t3, t4);
            }

            @Override
            public void onFinish() {
                pomodoroRemainingMillis = 0;
                pomodoroRunning = false;
                handlePomodoroEnd(timer, textStartPause, layoutActions, t1, t2, t3, t4);
            }
        }.start();
        
        if (POMODORO_MODE_FOCUS.equals(currentPomodoroMode) && !isMuted) {
            playWhiteNoise();
        }
    }

    private void pausePomodoro() {
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }
        pomodoroRunning = false;
        pauseWhiteNoise();
    }

    private void resetPomodoro() {
        pausePomodoro();
        currentPomodoroMode = POMODORO_MODE_FOCUS;
        pomodoroRemainingMillis = 25L * 60L * 1000L;
        pomodoroSessionStartMillis = 0;
        stopWhiteNoise();
    }

    private void updatePomodoroUi(TextView timer, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        long totalSeconds = pomodoroRemainingMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        timer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        
        textStartPause.setText(pomodoroRunning ? "Tạm dừng" : "Bắt đầu");
        
        long durationMin = (POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) ? 25 : (POMODORO_MODE_SHORT_BREAK.equals(currentPomodoroMode) ? 5 : 15);
        boolean isInitial = (pomodoroRemainingMillis == durationMin * 60 * 1000L);
        layoutActions.setVisibility(isInitial ? View.GONE : View.VISIBLE);
        
        int filled = pomodoroCycleCount % 4;
        if (pomodoroCycleCount > 0 && filled == 0 && POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) filled = 4;
        
        t1.setImageResource(filled >= 1 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
        t2.setImageResource(filled >= 2 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
        t3.setImageResource(filled >= 3 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
        t4.setImageResource(filled >= 4 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
    }
    
    private void handlePomodoroEnd(TextView timer, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        stopWhiteNoise();
        long durationMin = (POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) ? 25 : (POMODORO_MODE_SHORT_BREAK.equals(currentPomodoroMode) ? 5 : 15);
        long completedMin = durationMin - (pomodoroRemainingMillis / 60000L);
        if (completedMin > 0) {
            String taskId = currentPomodoroTask != null ? currentPomodoroTask.getId() : "";
            String tag = currentPomodoroTask != null ? currentPomodoroTask.getTag() : "";
            PomodoroSession session = new PomodoroSession(UUID.randomUUID().toString(), taskId, tag, currentPomodoroMode, (int)durationMin, (int)completedMin, pomodoroSessionStartMillis, System.currentTimeMillis(), pomodoroRemainingMillis == 0, isMuted ? "none" : pomodoroSoundRawName());
            repository.savePomodoroSession(session);
            if (POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) {
                repository.addFocusSession((int)completedMin);
            }
        }
        
        if (pomodoroRemainingMillis == 0) {
            sendPomodoroNotification("Hết giờ!", POMODORO_MODE_FOCUS.equals(currentPomodoroMode) ? "Bạn đã hoàn thành phiên tập trung. Nghỉ ngơi nhé!" : "Hết giờ nghỉ, quay lại học thôi!");
        }

        if (POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) {
            pomodoroCycleCount++;
            if (pomodoroCycleCount > 0 && pomodoroCycleCount % 4 == 0) {
                currentPomodoroMode = POMODORO_MODE_LONG_BREAK;
                pomodoroRemainingMillis = 15L * 60L * 1000L;
                showPomodoroTransitionDialog("Bạn đã hoàn thành 4 phiên!", "Tuyệt vời! Bạn nên nghỉ dài 15 phút trước khi tiếp tục.", 15);
            } else {
                currentPomodoroMode = POMODORO_MODE_SHORT_BREAK;
                pomodoroRemainingMillis = 5L * 60L * 1000L;
                showPomodoroTransitionDialog("Hoàn thành phiên tập trung!", "Bạn đã học tập trung. Nghỉ 5 phút để lấy lại năng lượng nhé.", 5);
            }
        } else {
            currentPomodoroMode = POMODORO_MODE_FOCUS;
            pomodoroRemainingMillis = 25L * 60L * 1000L;
            showPomodoroTransitionDialog("Hết giờ nghỉ", "Quay lại học thôi!", 25);
        }
        pomodoroSessionStartMillis = 0;
        updatePomodoroUi(timer, textStartPause, layoutActions, t1, t2, t3, t4);
    }
    
    private void showPomodoroTransitionDialog(String title, String message, int nextDuration) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(nextDuration == 25 ? "Bắt đầu học" : "Bắt đầu nghỉ", (d, w) -> {
                showPomodoro();
                View screen = contentFrame.getChildAt(0);
                if (screen != null && screen.findViewById(R.id.btnStartPause) != null) {
                    screen.findViewById(R.id.btnStartPause).performClick();
                }
            })
            .setNegativeButton("Dừng lại", (d, w) -> {
                resetPomodoro();
                showPomodoro();
            })
            .show();
    }
    
    private void showTaskSelectionDialog(TextView modeText) {
        java.util.List<StudyTask> tasks = repository.getTasks();
        java.util.List<StudyTask> pending = new java.util.ArrayList<>();
        for (StudyTask t : tasks) {
            if (!t.isCompleted()) pending.add(t);
        }
        if (pending.isEmpty()) {
            toast("Không có công việc nào đang mở");
            return;
        }
        String[] titles = new String[pending.size() + 1];
        titles[0] = "Không chọn công việc (Tự do)";
        for (int i=0; i<pending.size(); i++) titles[i+1] = pending.get(i).getTitle();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn công việc")
            .setItems(titles, (d, w) -> {
                if (w == 0) {
                    currentPomodoroTask = null;
                    modeText.setText(POMODORO_MODE_FOCUS.equals(currentPomodoroMode) ? "Tập trung tự do" : "Đang nghỉ ngơi");
                } else {
                    currentPomodoroTask = pending.get(w - 1);
                    modeText.setText(currentPomodoroTask.getTitle());
                }
            }).show();
    }
    
    private void playWhiteNoise() {
        if (backgroundAudioPlayer == null) {
            try {
                int resId = getResources().getIdentifier(pomodoroSoundRawName(), "raw", getPackageName());
                if (resId != 0) {
                    backgroundAudioPlayer = MediaPlayer.create(this, resId);
                    if (backgroundAudioPlayer != null) {
                        backgroundAudioPlayer.setLooping(true);
                        backgroundAudioPlayer.setVolume(pomodoroSoundVolume, pomodoroSoundVolume);
                        backgroundAudioPlayer.start();
                    }
                } else {
                    startGeneratedWhiteNoise();
                }
            } catch (Exception e) {}
        } else if (!backgroundAudioPlayer.isPlaying()) {
            backgroundAudioPlayer.setVolume(pomodoroSoundVolume, pomodoroSoundVolume);
            backgroundAudioPlayer.start();
        }
    }
    
    private void pauseWhiteNoise() {
        if (backgroundAudioPlayer != null && backgroundAudioPlayer.isPlaying()) {
            backgroundAudioPlayer.pause();
        }
        stopGeneratedWhiteNoise();
    }
    
    private void stopWhiteNoise() {
        if (backgroundAudioPlayer != null) {
            backgroundAudioPlayer.stop();
            backgroundAudioPlayer.release();
            backgroundAudioPlayer = null;
        }
        stopGeneratedWhiteNoise();
    }

    private void startGeneratedWhiteNoise() {
        if (generatedNoisePlaying) {
            return;
        }
        generatedNoisePlaying = true;
        int sampleRate = 22050;
        int minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, sampleRate / 2);
        whiteNoiseTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            whiteNoiseTrack.setVolume(pomodoroSoundVolume);
        } else {
            whiteNoiseTrack.setStereoVolume(pomodoroSoundVolume, pomodoroSoundVolume);
        }
        whiteNoiseTrack.play();
        whiteNoiseThread = new Thread(() -> {
            Random random = new Random();
            short[] buffer = new short[1024];
            int smooth = 0;
            while (generatedNoisePlaying && whiteNoiseTrack != null) {
                for (int i = 0; i < buffer.length; i++) {
                    int raw = random.nextInt(Short.MAX_VALUE) - (Short.MAX_VALUE / 2);
                    if ("tiếng thư viện".equals(pomodoroSoundName)) {
                        smooth = (smooth * 7 + raw) / 8;
                        buffer[i] = (short) (smooth * 0.45f);
                    } else if ("tiếng sóng".equals(pomodoroSoundName)) {
                        smooth = (smooth * 3 + raw) / 4;
                        buffer[i] = (short) (smooth * 0.35f + random.nextInt(900));
                    } else {
                        buffer[i] = (short) (raw * 0.28f);
                    }
                }
                try {
                    whiteNoiseTrack.write(buffer, 0, buffer.length);
                } catch (Exception ignored) {
                    generatedNoisePlaying = false;
                }
            }
        });
        whiteNoiseThread.start();
    }

    private void stopGeneratedWhiteNoise() {
        generatedNoisePlaying = false;
        if (whiteNoiseThread != null) {
            whiteNoiseThread.interrupt();
            whiteNoiseThread = null;
        }
        if (whiteNoiseTrack != null) {
            try {
                whiteNoiseTrack.stop();
            } catch (IllegalStateException ignored) {
            }
            whiteNoiseTrack.release();
            whiteNoiseTrack = null;
        }
    }
    
    private void showPomodoroSettings() {
        toast("Settings: Có thể điều chỉnh trong bản cập nhật sau.");
    }

    private void showPomodoroSoundPanel() {
        Dialog dialog = new Dialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(14), dp(24), dp(24));
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.parseColor("#F7F7F7"));
        panelBg.setCornerRadii(new float[]{dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0});
        content.setBackground(panelBg);

        View handle = new View(this);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(60), dp(6));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, dp(24));
        handle.setLayoutParams(handleParams);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(Color.parseColor("#D8D8D8"));
        handleBg.setCornerRadius(dp(6));
        handle.setBackground(handleBg);
        content.addView(handle);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setText("Tập trung vào âm thanh");
        title.setTextColor(getColor(R.color.ink));
        title.setTextSize(21f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        Switch soundSwitch = new Switch(this);
        soundSwitch.setChecked(!isMuted);
        header.addView(title);
        header.addView(soundSwitch);
        content.addView(header);

        TextView nowPlaying = soundCard(pomodoroSoundName, isMuted ? "Đang tắt âm thanh" : pomodoroSoundSubtitle(pomodoroSoundName));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(90));
        cardParams.setMargins(0, dp(22), 0, dp(22));
        nowPlaying.setLayoutParams(cardParams);
        content.addView(nowPlaying);

        TextView volumeLabel = soundSectionLabel("Âm lượng");
        content.addView(volumeLabel);

        LinearLayout volumeBox = new LinearLayout(this);
        volumeBox.setGravity(android.view.Gravity.CENTER_VERTICAL);
        volumeBox.setOrientation(LinearLayout.HORIZONTAL);
        volumeBox.setPadding(dp(18), 0, dp(18), 0);
        GradientDrawable volumeBg = new GradientDrawable();
        volumeBg.setColor(Color.WHITE);
        volumeBg.setCornerRadius(dp(14));
        volumeBox.setBackground(volumeBg);
        LinearLayout.LayoutParams volumeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(74));
        volumeParams.setMargins(0, dp(10), 0, dp(24));
        volumeBox.setLayoutParams(volumeParams);

        TextView low = new TextView(this);
        low.setText(isMuted ? "🔇" : "🔈");
        low.setTextSize(22f);
        low.setGravity(android.view.Gravity.CENTER);
        volumeBox.addView(low, new LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.MATCH_PARENT));

        SeekBar volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(Math.round(pomodoroSoundVolume * 100f));
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        seekParams.setMargins(dp(12), 0, dp(12), 0);
        volumeBox.addView(volume, seekParams);

        TextView high = new TextView(this);
        high.setText("🔊");
        high.setTextSize(22f);
        high.setGravity(android.view.Gravity.CENTER);
        volumeBox.addView(high, new LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(volumeBox);

        TextView playlistLabel = soundSectionLabel("Danh sách phát");
        content.addView(playlistLabel);
        content.addView(soundChoiceRow("tiếng mưa", "Mưa nhẹ để giữ nhịp tập trung", dialog));
        content.addView(soundChoiceRow("tiếng sóng", "Sóng biển đều và thư giãn", dialog));
        content.addView(soundChoiceRow("tiếng củi cháy", "Âm lửa nhỏ ấm và chậm", dialog));
        content.addView(soundChoiceRow("tiếng rừng ban đêm", "Nền rừng dịu cho buổi tối", dialog));
        content.addView(soundChoiceRow("tiếng thư viện", "Không gian yên tĩnh khi học", dialog));

        soundSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            isMuted = !checked;
            nowPlaying.setText(soundCardText(pomodoroSoundName, isMuted ? "Đang tắt âm thanh" : pomodoroSoundSubtitle(pomodoroSoundName)));
            low.setText(isMuted ? "🔇" : "🔈");
            if (isMuted) {
                pauseWhiteNoise();
            } else if (pomodoroRunning && POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) {
                playWhiteNoise();
            }
        });
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pomodoroSoundVolume = Math.max(0f, progress / 100f);
                if (backgroundAudioPlayer != null) {
                    backgroundAudioPlayer.setVolume(pomodoroSoundVolume, pomodoroSoundVolume);
                }
                if (whiteNoiseTrack != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        whiteNoiseTrack.setVolume(pomodoroSoundVolume);
                    } else {
                        whiteNoiseTrack.setStereoVolume(pomodoroSoundVolume, pomodoroSoundVolume);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        dialog.setContentView(content);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setOnShowListener(d -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setGravity(android.view.Gravity.BOTTOM);
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private TextView soundSectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getColor(R.color.ink));
        label.setTextSize(20f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        return label;
    }

    private String pomodoroSoundRawName() {
        if ("tiếng sóng".equals(pomodoroSoundName)) {
            return "song_bien";
        }
        if ("tiếng củi cháy".equals(pomodoroSoundName)) {
            return "tieng_cui";
        }
        if ("tiếng rừng ban đêm".equals(pomodoroSoundName)) {
            return "night_forest";
        }
        if ("tiếng thư viện".equals(pomodoroSoundName)) {
            return "tieng_sach";
        }
        return "tieng_mua";
    }

    private String pomodoroSoundSubtitle(String name) {
        if ("tiếng sóng".equals(name)) {
            return "Sóng biển";
        }
        if ("tiếng củi cháy".equals(name)) {
            return "Lửa trại";
        }
        if ("tiếng rừng ban đêm".equals(name)) {
            return "Rừng đêm";
        }
        if ("tiếng thư viện".equals(name)) {
            return "Thư viện";
        }
        return "Danh sách phát";
    }

    private String pomodoroSoundIcon(String name) {
        if ("tiếng sóng".equals(name)) {
            return "≈";
        }
        if ("tiếng củi cháy".equals(name)) {
            return "♨";
        }
        if ("tiếng rừng ban đêm".equals(name)) {
            return "☾";
        }
        if ("tiếng thư viện".equals(name)) {
            return "▤";
        }
        return "☔";
    }

    private TextView soundCard(String name, String subtitle) {
        TextView view = new TextView(this);
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        view.setPadding(dp(18), 0, dp(16), 0);
        view.setText(soundCardText(name, subtitle));
        view.setTextColor(getColor(R.color.ink));
        view.setTextSize(16f);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FFFDEB"));
        bg.setStroke(dp(2), getColor(R.color.ink));
        bg.setCornerRadius(dp(8));
        view.setBackground(bg);
        return view;
    }

    private String soundCardText(String name, String subtitle) {
        return pomodoroSoundIcon(name) + "   " + name + "\n     " + subtitle + "                         ▶";
    }

    private TextView soundChoiceRow(String name, String subtitle, Dialog dialog) {
        TextView row = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70));
        params.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(params);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(18), 0);
        row.setText((pomodoroSoundName.equals(name) ? "●  " : "○  ") + pomodoroSoundIcon(name) + "  " + name + "\n   " + subtitle + "                                      ⋯");
        row.setTextColor(getColor(R.color.ink));
        row.setTextSize(15f);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        row.setOnClickListener(v -> {
            pomodoroSoundName = name;
            if (!isMuted && pomodoroRunning && POMODORO_MODE_FOCUS.equals(currentPomodoroMode)) {
                stopWhiteNoise();
                playWhiteNoise();
            }
            toast("Đã chọn " + name);
            dialog.dismiss();
        });
        return row;
    }
    
    private void showPomodoroHistory() {
        toast("Hôm nay đã tập trung: " + repository.getTodayFocusMinutes() + " phút.");
    }
    
    private void sendPomodoroNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "pomodoro_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Pomodoro Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_tomato_active)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify(1, builder.build());
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

        setText(screen, R.id.textStatsSubtitle, statsSubtitle(completion, overdue, repository.getTodayFocusMinutes()));
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
        compareBars.setData("So sánh nhanh", new String[]{"Xong", "Mở", "Lịch", "Tập trung"}, new int[]{completed, pending, events.size(), repository.getTodayFocusSessions()});

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
        setText(screen, R.id.textEventBreakdown, "Lịch học: " + studyEvents + " · Lịch thi: " + examEvents + " · Deadline: " + deadlineEvents);
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
        screen.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            authRepository.logout();
            firebaseAuth.signOut();
            if (googleSignInClient != null) {
                googleSignInClient.signOut();
            }
            repository.setLoggedIn(false);
            repository = new StudyRepository(this);
            resetPomodoro();
            showLogin();
        });
    }

    private void showTaskDialog(StudyTask editingTask, Runnable onSaved) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
        EditText title = dialogView.findViewById(R.id.inputTitle);
        if (editingTask != null) {
            title.setText(editingTask.getTitle());
            title.setSelection(title.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editingTask == null ? "Thêm việc cần làm" : "Sửa tên việc")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (isBlank(title)) {
                        toast("Vui lòng nhập tên việc cần làm");
                        return;
                    }
                    StudyTask task = editingTask == null
                            ? repository.newTask(textOf(title), "Cá nhân", DateTimeUtils.daysFromNow(0, 23, 59), StudyTask.PRIORITY_MEDIUM, "")
                            : editingTask;
                    task.setTitle(textOf(title));
                    if (editingTask == null) {
                        task.setImportant(false);
                        task.setUrgent(false);
                        task.setTag("Cá nhân");
                        task.setMarkerType("flag");
                        task.setMarkerValue("");
                    }
                    repository.saveTask(task);
                    dialog.dismiss();
                    onSaved.run();
                }));
        dialog.show();
        title.requestFocus();
    }

    private void showEventDialog(StudyEvent editingEvent, Runnable onSaved) {
        long defaultStart = editingEvent == null ? DateTimeUtils.daysFromNow(1, 9, 30) : editingEvent.getStartAt();
        showEventDialog(editingEvent, onSaved, defaultStart);
    }

    private void showEventDialog(StudyEvent editingEvent, Runnable onSaved, long preferredStartAt) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event, null);
        EditText title = dialogView.findViewById(R.id.inputTitle);
        EditText subject = dialogView.findViewById(R.id.inputSubject);
        EditText date = dialogView.findViewById(R.id.inputDate);
        EditText start = dialogView.findViewById(R.id.inputStart);
        EditText end = dialogView.findViewById(R.id.inputEnd);
        EditText room = dialogView.findViewById(R.id.inputRoom);
        EditText note = dialogView.findViewById(R.id.inputNote);
        CheckBox reminder = dialogView.findViewById(R.id.checkReminder);
        Spinner type = dialogView.findViewById(R.id.spinnerType);
        Spinner reminderBefore = dialogView.findViewById(R.id.spinnerReminderBefore);
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, EVENT_TYPES));
        reminderBefore.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, REMINDER_LABELS));

        long defaultStart = preferredStartAt > 0 ? preferredStartAt : DateTimeUtils.daysFromNow(1, 9, 30);
        long defaultEnd = defaultStart + 60L * 60L * 1000L;
        fillEventTimeInputs(date, start, end, defaultStart, defaultEnd);
        reminderBefore.setSelection(indexOf(REMINDER_LABELS, "15 phút"));
        if (editingEvent != null) {
            title.setText(editingEvent.getTitle());
            subject.setText(editingEvent.getSubject());
            room.setText(editingEvent.getRoom());
            note.setText(editingEvent.getNote());
            type.setSelection(indexOf(EVENT_TYPES, editingEvent.getType()));
            fillEventTimeInputs(date, start, end, editingEvent.getStartAt(), editingEvent.getEndAt());
            reminder.setChecked(editingEvent.isReminderEnabled());
            reminderBefore.setSelection(indexOfReminder(editingEvent.getReminderBeforeMinutes()));
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
                            : new StudyEvent(editingEvent.getId(), textOf(title), String.valueOf(type.getSelectedItem()), textOf(subject), startAt, endAt, textOf(room), textOf(note));
                    event.setReminderEnabled(reminder.isChecked());
                    event.setReminderBeforeMinutes(REMINDER_MINUTES[reminderBefore.getSelectedItemPosition()]);
                    List<StudyEvent> conflicts = repository.getConflicts(event);
                    if (!conflicts.isEmpty()) {
                        showConflictBeforeSave(event, conflicts, () -> {
                            repository.saveEvent(event);
                            dialog.dismiss();
                            onSaved.run();
                        });
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cá nhân hóa")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
                        if (task.isCompleted()) {
                            toast(encouragementMessage("task"));
                        }
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
        new AlertDialog.Builder(this)
                .setTitle(event.getTitle())
                .setMessage(eventDetailText(event))
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Sửa", (dialog, which) -> showEventDialog(event, () -> showSchedule(scheduleFilter)))
                .setNeutralButton("Xóa", (dialog, which) -> confirmDeleteEvent(event))
                .show();
    }

    private void confirmDeleteEvent(StudyEvent event) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch")
                .setMessage("Bạn có chắc muốn xóa lịch này không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    repository.deleteEvent(event.getId());
                    toast("Đã xóa lịch");
                    showSchedule(scheduleFilter);
                })
                .show();
    }

    private void showMoveEventConfirmation(StudyEvent event, long newStartAt) {
        long duration = Math.max(30L * 60L * 1000L, event.getEndAt() - event.getStartAt());
        StudyEvent moved = new StudyEvent(event.getId(), event.getTitle(), event.getType(), event.getSubject(), newStartAt, newStartAt + duration, event.getRoom(), event.getNote(), event.isReminderEnabled(), event.getReminderBeforeMinutes());
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật giờ?")
                .setMessage(event.getTitle() + "\nChuyển sang " + DateTimeUtils.formatDateTime(moved.getStartAt()) + " - " + DateTimeUtils.formatTime(moved.getEndAt()))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    repository.saveEvent(moved);
                    showSchedule(scheduleFilter);
                })
                .show();
    }

    private String eventDetailText(StudyEvent event) {
        String reminder = event.isReminderEnabled()
                ? "\nNhắc trước: " + reminderLabel(event.getReminderBeforeMinutes())
                : "\nNhắc nhở: Tắt";
        String room = TextUtils.isEmpty(event.getRoom()) ? "Chưa có địa điểm" : event.getRoom();
        String note = TextUtils.isEmpty(event.getNote()) ? "" : "\nGhi chú: " + event.getNote();
        return event.getType()
                + " • " + event.getSubject()
                + "\n" + DateTimeUtils.formatDateTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt())
                + "\nĐịa điểm: " + room
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
        TextView details = row.findViewById(R.id.textDetails);
        TextView marker = row.findViewById(R.id.textMarker);
        CheckBox done = row.findViewById(R.id.checkDone);
        TextView actionDone = row.findViewById(R.id.btnTaskDone);
        TextView actionEdit = row.findViewById(R.id.btnTaskEdit);
        TextView actionDelete = row.findViewById(R.id.btnTaskDelete);
        View taskActions = row.findViewById(R.id.taskActions);
        title.setText(task.getTitle());
        meta.setText(task.getTag() + " • Deadline " + DateTimeUtils.formatDateTime(task.getDueAt()));
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
            if (isChecked) {
                toast(encouragementMessage("task"));
            }
            if (onChanged != null) {
                onChanged.run();
            }
        });
        actionDone.setOnClickListener(v -> {
            task.setCompleted(!task.isCompleted());
            repository.saveTask(task);
            if (task.isCompleted()) {
                toast(encouragementMessage("task"));
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
            repository.deleteTask(task.getId());
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
        String status = task.isCompleted() ? "Đã hoàn thành" : "Đang làm";
        String quadrant = quadrantLabel(task);
        String pomodoro = task.getEstimatedPomodoro() > 0 ? " • " + task.getEstimatedPomodoro() + " Pomodoro" : "";
        String reminder = task.getReminderTime() > 0 ? " • Nhắc " + DateTimeUtils.formatDateTime(task.getReminderTime()) : "";
        String repeat = "Không lặp".equals(task.getRepeatOption()) ? "" : " • " + task.getRepeatOption();
        return status + " • " + quadrant + pomodoro + reminder + repeat;
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

    private String greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) {
            return "Chào buổi sáng";
        }
        if (hour >= 11 && hour < 18) {
            return "Chào buổi chiều";
        }
        return "Chào buổi tối";
    }

    private String dashboardSummary(int todayTotal, int todayCompleted, int todayRemaining) {
        if (todayTotal == 0) {
            return "Hôm nay chưa có việc học. Bạn có thể thêm một mục tiêu nhỏ để bắt nhịp.";
        }
        if (todayRemaining == 0) {
            return "Bạn đã hoàn thành toàn bộ việc học hôm nay. Rất gọn gàng.";
        }
        return "Hôm nay còn " + todayRemaining + " việc cần xử lý, đã xong " + todayCompleted + "/" + todayTotal + ".";
    }

    private String initialsOf(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return "SP";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.getDefault()));
            if (builder.length() == 2) {
                break;
            }
        }
        return builder.length() == 0 ? "SP" : builder.toString();
    }

    private String avatarMark(String avatarChoice, UserProfile profile) {
        if ("Chữ viết tắt".equals(avatarChoice)) {
            return initialsOf(profile.getName());
        }
        return mascotMark(avatarChoice);
    }

    private String mascotMark(String choice) {
        if ("Mèo học tập".equals(choice)) {
            return "Mèo";
        }
        if ("Quyển sách".equals(choice)) {
            return "Sách";
        }
        if ("Bạn học tập".equals(choice)) {
            return "Bạn";
        }
        return "Robot";
    }

    private String shortStatus(String status) {
        if (TextUtils.isEmpty(status)) {
            return "study";
        }
        String value = status.trim();
        String lower = value.toLowerCase(Locale.getDefault());
        if (lower.contains("sẵn sàng")) {
            return "Sẵn sàng";
        }
        if (lower.contains("học")) {
            return "Học tập";
        }
        return value.length() <= 10 ? value : value.substring(0, 10).trim();
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

    private String encouragementMessage(String type) {
        String mascot = mascotMark(repository.getMascotChoice());
        if ("pomodoro".equals(type)) {
            return mascot + ": Xong 25 phút rồi, nghỉ một nhịp nhé!";
        }
        return mascot + ": Tốt lắm, thêm một việc đã gọn!";
    }

    private void bindSpinner(Spinner spinner, String[] values, String selected) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(indexOf(values, selected));
    }

    private void tintButton(View root, int id, int backgroundColor, int textColor) {
        MaterialButton button = root.findViewById(id);
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(backgroundColor)));
        button.setTextColor(getColor(textColor));
    }

    private String deadlineMeta(StudyTask task) {
        String status = task.getDueAt() < System.currentTimeMillis() ? "Quá hạn" : "Đến hạn";
        return status + " · " + task.getSubject() + " · " + DateTimeUtils.formatDayLabel(task.getDueAt()) + " " + DateTimeUtils.formatTime(task.getDueAt());
    }

    private String statsSubtitle(int completion, int overdue, int todayFocusMinutes) {
        if (overdue > 0) {
            return "Có " + overdue + " việc quá hạn cần xử lý trước.";
        }
        if (completion >= 80 && todayFocusMinutes > 0) {
            return "Tiến độ tốt, bạn đang giữ nhịp học khá ổn.";
        }
        if (completion == 0) {
            return "Bắt đầu bằng một việc nhỏ để thống kê có đà.";
        }
        return "Theo dõi việc học, lịch và Pomodoro trong một màn hình.";
    }

    private String statsInsight(int totalTasks, int overdue, int todayTotal, int todayRemaining, int totalEvents, int todayFocusMinutes) {
        if (totalTasks == 0 && totalEvents == 0) {
            return "Gợi ý: thêm lịch học và việc đầu tiên để có thống kê trực quan hơn.";
        }
        if (overdue > 0) {
            return "Ưu tiên hôm nay: xử lý việc quá hạn trước, sau đó quay lại các việc sắp tới.";
        }
        if (todayTotal > 0 && todayRemaining == 0) {
            return "Hôm nay đã xong toàn bộ việc học. Có thể dùng Pomodoro để ôn lại hoặc chuẩn bị bài mới.";
        }
        if (todayFocusMinutes == 0) {
            return "Bạn chưa có phiên Pomodoro hôm nay. Một phiên 25 phút là đủ để bắt nhịp.";
        }
        return "Nhịp học đang ổn. Tiếp tục giữ lịch, việc học và Pomodoro cân bằng.";
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

    private StudyTask findNearestDeadlineTask(List<StudyTask> tasks) {
        for (StudyTask task : tasks) {
            if (!task.isCompleted()) {
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

    private int countAllTodayTasks(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (DateTimeUtils.isToday(task.getDueAt())) {
                count++;
            }
        }
        return count;
    }

    private int countCompletedTodayTasks(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (task.isCompleted() && DateTimeUtils.isToday(task.getDueAt())) {
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

    private int countPending(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (!task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    private int countEventsByType(List<StudyEvent> events, String type) {
        int count = 0;
        for (StudyEvent event : events) {
            if (type.equals(event.getType())) {
                count++;
            }
        }
        return count;
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

    private int percentOf(int value, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(value * 100f / total);
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

    private void activateStudyRepository(String accountEmail) {
        if (TextUtils.isEmpty(accountEmail)) {
            return;
        }
        repository = new StudyRepository(this, accountEmail);
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

    private void syncProfileFromAuthUser(AuthUser user) {
        if (user == null) {
            return;
        }
        UserProfile current = repository.getProfile();
        repository.saveProfile(new UserProfile(user.getName(), user.getEmail(), current.getGoal()));
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
