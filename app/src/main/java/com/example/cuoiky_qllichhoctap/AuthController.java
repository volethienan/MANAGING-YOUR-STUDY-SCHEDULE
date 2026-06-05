package com.example.cuoiky_qllichhoctap;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.cuoiky_qllichhoctap.model.AuthUser;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Locale;

class AuthController {
    private static final int GOOGLE_SIGN_IN_DEVELOPER_ERROR = 10;

    private final MainActivity activity;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    AuthController(MainActivity activity) {
        this.activity = activity;
    }

    void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, options);
        googleSignInLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        handleGoogleSignInResult(result.getData());
                    } else {
                        activity.toast("Đã hủy đăng nhập Google");
                    }
                });
    }

    void showInitialScreen() {
        AuthUser localUser = activity.authRepository.getCurrentUser();
        FirebaseUser firebaseUser = activity.firebaseAuth.getCurrentUser();
        if (activity.repository.isFirstOpen()) {
            showOnboarding();
        } else if (canEnterWithFirebaseUser(firebaseUser)) {
            if (localUser != null && !localUser.getEmail().equalsIgnoreCase(firebaseUser.getEmail())) {
                activity.authRepository.logout();
            }
            activateStudyRepository(firebaseUser.getEmail());
            syncProfileFromFirebase(firebaseUser);
            enterWithAdminSync(firebaseUser.getEmail(), activity.repository.getProfile().getName(), isGoogleUser(firebaseUser) ? "google" : "firebase", activity::showDashboard);
        } else if (firebaseUser != null) {
            showEmailVerificationRequired(firebaseUser);
        } else if (localUser != null) {
            activateStudyRepository(localUser.getEmail());
            syncProfileFromAuthUser(localUser);
            enterWithAdminSync(localUser.getEmail(), localUser.getName(), "email", activity::showDashboard);
        } else if (activity.repository.isLoggedIn()) {
            activity.showDashboard();
        } else {
            showLogin();
        }
    }

    void showOnboarding() {
        View screen = activity.inflateScreen(R.layout.screen_onboarding, false, -1);
        screen.findViewById(R.id.btnStart).setOnClickListener(v -> {
            activity.repository.finishOnboarding();
            showLogin();
        });
    }

    void showLogin() {
        View screen = activity.inflateScreen(R.layout.screen_login, false, -1);
        EditText email = screen.findViewById(R.id.inputEmail);
        EditText password = screen.findViewById(R.id.inputPassword);
        screen.findViewById(R.id.btnLogin).setOnClickListener(v -> signInWithEmail(textOf(email), textOf(password)));
        screen.findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> startGoogleSignIn());
        screen.findViewById(R.id.textGoRegister).setOnClickListener(v -> showRegister());
        screen.findViewById(R.id.textForgotPassword).setOnClickListener(v -> showForgotPassword(textOf(email)));
    }

    void logout() {
        activity.authRepository.logout();
        activity.firebaseAuth.signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
        activity.repository.setLoggedIn(false);
        activity.repository = new com.example.cuoiky_qllichhoctap.data.StudyRepository(activity);
        activity.reminderScheduler.setRepository(activity.repository);
        activity.pomodoroController.reset();
        showLogin();
    }

    private void showRegister() {
        View screen = activity.inflateScreen(R.layout.screen_register, false, -1);
        EditText name = screen.findViewById(R.id.inputName);
        EditText email = screen.findViewById(R.id.inputEmail);
        EditText password = screen.findViewById(R.id.inputPassword);
        EditText confirm = screen.findViewById(R.id.inputConfirmPassword);
        android.widget.CheckBox terms = screen.findViewById(R.id.checkTerms);
        screen.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            if (isBlank(name) || isBlank(email) || isBlank(password)) {
                activity.toast("Vui lòng nhập đủ thông tin");
                return;
            }
            if (!isValidEmail(textOf(email))) {
                activity.toast("Email chưa đúng định dạng");
                return;
            }
            if (textOf(password).length() < 6) {
                activity.toast("Mật khẩu cần ít nhất 6 ký tự");
                return;
            }
            if (!textOf(password).equals(textOf(confirm))) {
                activity.toast("Mật khẩu xác nhận chưa khớp");
                return;
            }
            if (!terms.isChecked()) {
                activity.toast("Bạn cần đồng ý điều khoản sử dụng");
                return;
            }
            registerWithOtp(textOf(name), textOf(email), textOf(password));
        });
        screen.findViewById(R.id.textGoLogin).setOnClickListener(v -> showLogin());
    }

    private void showForgotPassword() {
        showForgotPassword("");
    }

    private void showForgotPassword(String presetEmail) {
        View screen = activity.inflateScreen(R.layout.screen_forgot_password, false, -1);
        EditText email = screen.findViewById(R.id.inputEmail);
        email.setText(presetEmail);
        screen.findViewById(R.id.btnSendOtp).setOnClickListener(v -> {
            String value = textOf(email);
            if (!isValidEmail(value)) {
                activity.toast("Email không hợp lệ");
                return;
            }
            sendFirebasePasswordResetEmail(value);
        });
        screen.findViewById(R.id.textBackLogin).setOnClickListener(v -> showLogin());
    }

    private void signInWithEmail(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            activity.toast("Vui lòng nhập email và mật khẩu");
            return;
        }
        activity.firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = activity.firebaseAuth.getCurrentUser();
                        if (!canEnterWithFirebaseUser(firebaseUser)) {
                            showEmailVerificationRequired(firebaseUser);
                            return;
                        }
                        String name = firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getDisplayName())
                                ? firebaseUser.getDisplayName()
                                : firstNameFromEmail(email);
                        activateStudyRepository(email);
                        syncProfileFromFirebase(firebaseUser);
                        activity.repository.finishOnboarding();
                        activity.repository.setLoggedIn(true);
                        enterWithAdminSync(email, name, "firebase", activity::showDashboard);
                    } else {
                        signInWithLocalEmailFallback(email, password, task.getException() == null ? "" : task.getException().getMessage());
                    }
                });
    }

    private void signInWithLocalEmailFallback(String email, String password, String firebaseError) {
        try {
            AuthUser user = activity.authRepository.login(email, password);
            activateStudyRepository(user.getEmail());
            syncProfileFromAuthUser(user);
            enterWithAdminSync(user.getEmail(), user.getName(), "email", activity::showDashboard);
        } catch (IllegalArgumentException exception) {
            String message = TextUtils.isEmpty(firebaseError) ? exception.getMessage() : firebaseError;
            if (!TextUtils.isEmpty(message) && message.toLowerCase(Locale.ROOT).contains("configuration_not_found")) {
                message = "Firebase chưa cấu hình Email/Password. App sẽ dùng tài khoản cục bộ nếu đã đăng ký.";
            }
            activity.toast(TextUtils.isEmpty(message) ? "Không đăng nhập được" : message);
        }
    }

    private void registerWithFirebaseEmail(String name, String email, String password) {
        activity.firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = activity.firebaseAuth.getCurrentUser();
                        if (user != null) {
                            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                                    .addOnCompleteListener(profileTask -> sendFirebaseVerificationEmail(user));
                        } else {
                            registerWithOtp(name, email, password);
                        }
                    } else {
                        registerWithOtp(name, email, password);
                    }
                });
    }

    private void sendFirebaseVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        showFirebaseVerificationSent(user);
                    } else {
                        showAuthTaskError(task, "Không gửi được email xác thực Firebase");
                    }
                });
    }

    private void showFirebaseVerificationSent(FirebaseUser user) {
        String email = user == null || TextUtils.isEmpty(user.getEmail()) ? "email của bạn" : user.getEmail();
        new AlertDialog.Builder(activity)
                .setTitle("Kiểm tra email")
                .setMessage("Firebase đã gửi link xác thực tới " + email + ". Hãy bấm link trong email rồi quay lại đăng nhập.")
                .setPositiveButton("Về đăng nhập", (dialog, which) -> {
                    activity.firebaseAuth.signOut();
                    showLogin();
                })
                .show();
    }

    private void registerWithOtp(String name, String email, String password) {
        try {
            String code = activity.authRepository.beginRegistration(name, email, password);
            activity.adminPortalClient.syncRegisteredUser(email, name, "email", false);
            deliverOtpEmail(email, code, "xác thực tài khoản");
            showOtpVerification(email);
        } catch (IllegalArgumentException exception) {
            activity.toast(exception.getMessage());
        }
    }

    private void showOtpVerification(String email) {
        View screen = activity.inflateScreen(R.layout.screen_otp, false, -1);
        activity.setText(screen, R.id.textOtpEmail, email);
        EditText otp = screen.findViewById(R.id.inputOtp);
        screen.findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            try {
                AuthUser user = activity.authRepository.verifyRegistrationOtp(email, textOf(otp));
                activateStudyRepository(user.getEmail());
                syncProfileFromAuthUser(user);
                activity.repository.finishOnboarding();
                activity.repository.setLoggedIn(true);
                enterWithAdminSync(user.getEmail(), user.getName(), "email", activity::showDashboard);
            } catch (IllegalArgumentException exception) {
                activity.toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.btnResendOtp).setOnClickListener(v -> {
            try {
                String code = activity.authRepository.resendRegistrationOtp(email);
                deliverOtpEmail(email, code, "xác thực tài khoản");
                activity.toast("Đã gửi lại mã OTP");
            } catch (IllegalArgumentException exception) {
                activity.toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBack).setOnClickListener(v -> showRegister());
    }

    private void sendFirebasePasswordResetEmail(String email) {
        activity.firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        showAdminPasswordResetNotice(email);
                    } else {
                        sendPasswordResetEmail(email);
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        try {
            String code = activity.authRepository.beginPasswordReset(email);
            deliverOtpEmail(email, code, "đặt lại mật khẩu");
            showResetPassword(email);
        } catch (IllegalArgumentException exception) {
            activity.toast(exception.getMessage());
        }
    }

    private void showResetPassword(String email) {
        View screen = activity.inflateScreen(R.layout.screen_reset_password, false, -1);
        activity.setText(screen, R.id.textResetEmail, email);
        EditText otp = screen.findViewById(R.id.inputOtp);
        EditText password = screen.findViewById(R.id.inputPassword);
        EditText confirm = screen.findViewById(R.id.inputConfirmPassword);
        screen.findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            if (isBlank(otp) || isBlank(password) || isBlank(confirm)) {
                activity.toast("Vui lòng nhập đủ mã OTP và mật khẩu mới");
                return;
            }
            if (textOf(password).length() < 6) {
                activity.toast("Mật khẩu cần ít nhất 6 ký tự");
                return;
            }
            if (!textOf(password).equals(textOf(confirm))) {
                activity.toast("Mật khẩu xác nhận chưa khớp");
                return;
            }
            try {
                activity.authRepository.resetPassword(email, textOf(otp), textOf(password));
                activity.adminPortalClient.notifyPasswordResetComplete(email);
                new AlertDialog.Builder(activity)
                        .setTitle("Đã cập nhật mật khẩu")
                        .setMessage("Bạn có thể đăng nhập bằng mật khẩu mới.")
                        .setPositiveButton("Về đăng nhập", (dialog, which) -> showLogin())
                        .show();
            } catch (IllegalArgumentException exception) {
                activity.toast(exception.getMessage());
            }
        });
        screen.findViewById(R.id.textBackLogin).setOnClickListener(v -> showLogin());
    }

    private void deliverOtpEmail(String email, String code, String purpose) {
        activity.otpEmailSender.sendAsync(email, code, purpose, new com.example.cuoiky_qllichhoctap.data.OtpEmailSender.Callback() {
            @Override
            public void onSent() {
                activity.runOnUiThread(() -> activity.toast("Đã gửi OTP tới email"));
            }

            @Override
            public void onError(String detail) {
                activity.runOnUiThread(() -> {
                    activity.adminPortalClient.reportIssue("otp", email, detail);
                    showOtpSendError(detail);
                });
            }
        });
    }

    private void showOtpSendError(String detail) {
        AlertDialog otpDialog = new AlertDialog.Builder(activity)
                .setTitle("Không gửi được OTP")
                .setMessage("App chưa gửi được email OTP thật. Nguyên nhân thường là backend chưa chạy, sai URL backend, Windows Firewall chặn port 8080, hoặc SMTP Gmail chưa cấu hình đúng.\n\n"
                        + "Backend hiện tại: " + BuildConfig.OTP_BACKEND_URL)
                .setNeutralButton("Chi tiết kỹ thuật", (dialog, which) -> showTechnicalError("Chi tiết lỗi OTP", detail))
                .setPositiveButton("Đã hiểu", null)
                .create();
        otpDialog.setOnShowListener(shown -> activity.dialogFactory.styleStudyDialog(otpDialog));
        otpDialog.show();
    }

    private void showTechnicalError(String title, String detail) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(TextUtils.isEmpty(detail) ? "Không rõ lỗi" : detail)
                .setPositiveButton("OK", null)
                .create();
        dialog.setOnShowListener(shown -> activity.dialogFactory.styleStudyDialog(dialog));
        dialog.show();
    }

    private void startGoogleSignIn() {
        if (googleSignInClient == null || googleSignInLauncher == null) {
            activity.toast("Google Sign-In chưa sẵn sàng");
            return;
        }
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                activity.toast("Không lấy được Google ID token");
                return;
            }
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException exception) {
            activity.toast(googleSignInErrorMessage(exception));
        }
    }

    private String googleSignInErrorMessage(ApiException exception) {
        if (exception.getStatusCode() == GOOGLE_SIGN_IN_DEVELOPER_ERROR) {
            return "Google Sign-In chưa khớp SHA-1/OAuth client. Kiểm tra Firebase project và google-services.json.";
        }
        return "Đăng nhập Google thất bại: " + exception.getStatusCode();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        activity.firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        showAuthTaskError(task, "Không đăng nhập được Google");
                        return;
                    }
                    FirebaseUser firebaseUser = activity.firebaseAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        activity.toast("Không lấy được tài khoản Google");
                        return;
                    }
                    String email = TextUtils.isEmpty(firebaseUser.getEmail()) ? "google-user@firebase.local" : firebaseUser.getEmail();
                    String name = TextUtils.isEmpty(firebaseUser.getDisplayName()) ? firstNameFromEmail(email) : firebaseUser.getDisplayName();
                    activateStudyRepository(email);
                    syncProfileFromFirebase(firebaseUser);
                    activity.repository.finishOnboarding();
                    activity.repository.setLoggedIn(true);
                    enterWithAdminSync(email, name, "google", activity::showDashboard);
                });
    }

    void showNotificationPermissionDeniedMessage() {
        new AlertDialog.Builder(activity)
                .setTitle("Thông báo đang bị tắt")
                .setMessage("Bạn có thể bật lại quyền thông báo trong cài đặt ứng dụng để nhận nhắc lịch và Pomodoro.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Mở cài đặt", (dialog, which) -> openAppNotificationSettings())
                .show();
    }

    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    private void enterWithAdminSync(String email, String name, String provider, Runnable onAllowed) {
        activity.adminPortalClient.syncUserAccess(email, name, provider, (allowed, synced, passwordResetRequested, message) -> activity.runOnUiThread(() -> {
            if (!allowed) {
                activity.authRepository.logout();
                activity.firebaseAuth.signOut();
                new AlertDialog.Builder(activity)
                        .setTitle("Tài khoản bị khóa")
                        .setMessage(TextUtils.isEmpty(message) ? "Tài khoản này đã bị quản trị viên vô hiệu hóa. Vui lòng liên hệ quản trị." : message)
                        .setPositiveButton("Đã hiểu", (dialog, which) -> showLogin())
                        .show();
                return;
            }
            if (!synced && !TextUtils.isEmpty(message)) {
                activity.toast("Web quản trị chưa đồng bộ: " + message);
            }
            onAllowed.run();
            syncFcmTokenToAdmin(email);
            if (passwordResetRequested) {
                showAdminPasswordResetNotice(email);
            }
        }));
    }

    private void syncFcmTokenToAdmin(String email) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (!TextUtils.isEmpty(token)) {
                        activity.adminPortalClient.syncFcmToken(email, token);
                    }
                });
    }

    private void showAdminPasswordResetNotice(String email) {
        new AlertDialog.Builder(activity)
                .setTitle("Đã gửi email đặt lại mật khẩu")
                .setMessage("Nếu tài khoản " + email + " tồn tại trên Firebase, bạn sẽ nhận được email đặt lại mật khẩu. Yêu cầu cũng đã được ghi nhận cho quản trị viên.")
                .setPositiveButton("Về đăng nhập", (dialog, which) -> showLogin())
                .show();
    }

    void syncLearningSnapshotToAdmin() {
        AuthUser user = activity.authRepository == null ? null : activity.authRepository.getCurrentUser();
        String email = user != null ? user.getEmail() : activity.repository.getProfile().getEmail();
        if (TextUtils.isEmpty(email)) {
            return;
        }
        activity.adminPortalClient.syncLearningSnapshot(
                email,
                activity.repository.getProfile().getName(),
                "email",
                activity.repository.getTasks(),
                activity.repository.getEvents(),
                activity.repository.getFocusMinutes(),
                activity.repository.getFocusSessions(),
                activity.repository.getTodayFocusMinutes(),
                activity.repository.getTodayFocusSessions(),
                activity.repository.getRecentPomodoroSessions(20));
    }

    boolean canEnterWithFirebaseUser(FirebaseUser user) {
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
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Cần xác thực email")
                .setMessage("Tài khoản " + email + " chưa bấm link xác thực. Hãy mở email từ Firebase rồi đăng nhập lại.")
                .setNegativeButton("Đã hiểu", null)
                .setPositiveButton("Gửi lại link", (d, which) -> resendEmailVerification(user))
                .create();
        dialog.setOnShowListener(shown -> activity.dialogFactory.styleStudyDialog(dialog));
        dialog.show();
    }

    private void resendEmailVerification(FirebaseUser user) {
        if (user == null) {
            activity.toast("Không tìm thấy tài khoản để gửi lại link");
            return;
        }
        user.sendEmailVerification()
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        activity.toast("Đã gửi lại email xác thực");
                    } else {
                        showAuthTaskError(task, "Không gửi lại được email xác thực");
                    }
                });
    }

    private void showAuthTaskError(Task<?> task, String fallback) {
        String message = task.getException() == null ? fallback : task.getException().getMessage();
        if (!TextUtils.isEmpty(message)) {
            String lower = message.toLowerCase(Locale.ROOT);
            if (lower.contains("operation not allowed")
                    || lower.contains("operation is not allowed")
                    || lower.contains("sign-in provider is disabled")
                    || lower.contains("password sign-in is disabled")
                    || lower.contains("configuration_not_found")) {
                message = "Firebase Console chưa bật đăng nhập Email/Password. Vào Authentication > Sign-in method và bật Email/Password.";
            }
        }
        activity.toast(TextUtils.isEmpty(message) ? fallback : message);
    }

    private void activateStudyRepository(String accountEmail) {
        if (TextUtils.isEmpty(accountEmail)) {
            return;
        }
        activity.repository = new com.example.cuoiky_qllichhoctap.data.StudyRepository(activity, accountEmail);
        activity.reminderScheduler.setRepository(activity.repository);
        activity.repository.syncSnapshotToFirebase();
        activity.reminderScheduler.rescheduleAllReminders();
    }

    private void syncProfileFromFirebase(FirebaseUser user) {
        if (user == null) {
            return;
        }
        UserProfile current = activity.repository.getProfile();
        String name = TextUtils.isEmpty(user.getDisplayName()) ? firstNameFromEmail(user.getEmail()) : user.getDisplayName();
        String email = TextUtils.isEmpty(user.getEmail()) ? "google-user@firebase.local" : user.getEmail();
        activity.repository.saveProfile(new UserProfile(name, email, current.getGoal()));
    }

    private void syncProfileFromAuthUser(AuthUser user) {
        if (user == null) {
            return;
        }
        UserProfile current = activity.repository.getProfile();
        activity.repository.saveProfile(new UserProfile(user.getName(), user.getEmail(), current.getGoal()));
    }

    private String firstNameFromEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return "Google User";
        }
        int at = email.indexOf("@");
        return at > 0 ? email.substring(0, at) : email;
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
}
