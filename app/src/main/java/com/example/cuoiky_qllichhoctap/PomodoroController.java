package com.example.cuoiky_qllichhoctap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.example.cuoiky_qllichhoctap.model.PomodoroSession;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;
import com.example.cuoiky_qllichhoctap.util.PomodoroSoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

class PomodoroController {
    private static final String MODE_FOCUS = "focus";
    private static final String MODE_SHORT_BREAK = "short_break";
    private static final String MODE_LONG_BREAK = "long_break";

    private final MainActivity activity;
    private final PomodoroSoundManager soundManager;
    private CountDownTimer timer;
    private long remainingMillis = 25L * 60L * 1000L;
    private boolean running;
    private String currentMode = MODE_FOCUS;
    private int cycleCount = 0;
    private StudyTask currentTask = null;
    private long sessionStartMillis = 0;

    PomodoroController(MainActivity activity) {
        this.activity = activity;
        this.soundManager = new PomodoroSoundManager(activity);
    }

    void showPomodoro() {
        View screen = activity.inflateScreen(R.layout.screen_pomodoro, true, MainActivity.SCREEN_POMODORO);
        TextView timerText = screen.findViewById(R.id.textTimer);
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

        if (currentTask != null) {
            modeText.setText(currentTask.getTitle());
        } else {
            modeText.setText(MODE_FOCUS.equals(currentMode) ? "Tập trung tự do" : "Đang nghỉ ngơi");
        }

        updatePomodoroUi(timerText, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);

        btnStartPause.setOnClickListener(v -> {
            if (running) {
                pausePomodoro();
            } else {
                startPomodoro(timerText, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
            }
            updatePomodoroUi(timerText, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
        });

        btnStop.setOnClickListener(v -> {
            resetPomodoro();
            updatePomodoroUi(timerText, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4);
        });

        btnSkip.setOnClickListener(v -> skipPomodoroSession(timerText, textStartPause, layoutActions, tomato1, tomato2, tomato3, tomato4));
        btnSelectMode.setOnClickListener(v -> showTaskSelectionDialog(modeText));
        screen.findViewById(R.id.btnSound).setOnClickListener(v -> showPomodoroSoundPanel());
        screen.findViewById(R.id.btnHistory).setOnClickListener(v -> showPomodoroHistory());
    }

    void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        soundManager.stop();
    }

    void reset() {
        resetPomodoro();
    }

    private void startPomodoro(TextView timerText, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        if (remainingMillis == 0) {
            return;
        }
        if (sessionStartMillis == 0) {
            sessionStartMillis = System.currentTimeMillis();
        }
        running = true;
        timer = new CountDownTimer(remainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updatePomodoroUi(timerText, textStartPause, layoutActions, t1, t2, t3, t4);
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                running = false;
                handlePomodoroEnd(timerText, textStartPause, layoutActions, t1, t2, t3, t4);
            }
        }.start();

        if (MODE_FOCUS.equals(currentMode) && !soundManager.isMuted()) {
            soundManager.play();
        }
    }

    private void pausePomodoro() {
        if (timer != null) {
            timer.cancel();
        }
        running = false;
        soundManager.pause();
    }

    private void resetPomodoro() {
        pausePomodoro();
        currentMode = MODE_FOCUS;
        remainingMillis = 25L * 60L * 1000L;
        sessionStartMillis = 0;
        soundManager.stop();
    }

    private void updatePomodoroUi(TextView timerText, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        long totalSeconds = remainingMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        textStartPause.setText(running ? "Tạm dừng" : "Bắt đầu");

        long durationMin = MODE_FOCUS.equals(currentMode) ? 25 : (MODE_SHORT_BREAK.equals(currentMode) ? 5 : 15);
        boolean isInitial = remainingMillis == durationMin * 60 * 1000L;
        layoutActions.setVisibility(isInitial ? View.GONE : View.VISIBLE);

        int filled = cycleCount % 4;
        if (cycleCount > 0 && filled == 0 && MODE_FOCUS.equals(currentMode)) {
            filled = 4;
        }

        t1.setImageResource(filled >= 1 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
        t2.setImageResource(filled >= 2 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
        t3.setImageResource(filled >= 3 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
        t4.setImageResource(filled >= 4 ? R.drawable.ic_tomato_active : R.drawable.ic_tomato_inactive);
    }

    private void handlePomodoroEnd(TextView timerText, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        soundManager.stop();
        long durationMin = MODE_FOCUS.equals(currentMode) ? 25 : (MODE_SHORT_BREAK.equals(currentMode) ? 5 : 15);
        long completedMin = durationMin - (remainingMillis / 60000L);
        if (completedMin > 0) {
            String taskId = currentTask != null ? currentTask.getId() : "";
            String tag = currentTask != null ? currentTask.getTag() : "";
            PomodoroSession session = new PomodoroSession(UUID.randomUUID().toString(), taskId, tag, currentMode, (int) durationMin, (int) completedMin, sessionStartMillis, System.currentTimeMillis(), remainingMillis == 0, soundManager.isMuted() ? "none" : soundManager.rawName());
            activity.repository.savePomodoroSession(session);
            if (MODE_FOCUS.equals(currentMode)) {
                activity.repository.addFocusSession((int) completedMin);
            }
        }

        if (remainingMillis == 0) {
            sendPomodoroNotification("Hết giờ!", MODE_FOCUS.equals(currentMode) ? "Bạn đã hoàn thành phiên tập trung. Nghỉ ngơi nhé!" : "Hết giờ nghỉ, quay lại học thôi!");
        }

        if (MODE_FOCUS.equals(currentMode)) {
            cycleCount++;
            if (cycleCount > 0 && cycleCount % 4 == 0) {
                currentMode = MODE_LONG_BREAK;
                remainingMillis = 15L * 60L * 1000L;
                showPomodoroTransitionDialog("Bạn đã hoàn thành 4 phiên!", "Tuyệt vời! Bạn nên nghỉ dài 15 phút trước khi tiếp tục.", 15);
            } else {
                currentMode = MODE_SHORT_BREAK;
                remainingMillis = 5L * 60L * 1000L;
                showPomodoroTransitionDialog("Hoàn thành phiên tập trung!", "Bạn đã học tập trung. Nghỉ 5 phút để lấy lại năng lượng nhé.", 5);
            }
        } else {
            currentMode = MODE_FOCUS;
            remainingMillis = 25L * 60L * 1000L;
            showPomodoroTransitionDialog("Hết giờ nghỉ", "Quay lại học thôi!", 25);
        }
        sessionStartMillis = 0;
        updatePomodoroUi(timerText, textStartPause, layoutActions, t1, t2, t3, t4);
    }

    private void skipPomodoroSession(TextView timerText, TextView textStartPause, View layoutActions, ImageView t1, ImageView t2, ImageView t3, ImageView t4) {
        pausePomodoro();
        sessionStartMillis = 0;
        if (MODE_FOCUS.equals(currentMode)) {
            currentMode = MODE_SHORT_BREAK;
            remainingMillis = 5L * 60L * 1000L;
            updatePomodoroUi(timerText, textStartPause, layoutActions, t1, t2, t3, t4);
            showPomodoroTransitionDialog("Đã bỏ qua phiên tập trung", "Phiên này không được tính vào thống kê. Bạn có thể nghỉ ngắn 5 phút rồi quay lại.", 5);
        } else {
            currentMode = MODE_FOCUS;
            remainingMillis = 25L * 60L * 1000L;
            updatePomodoroUi(timerText, textStartPause, layoutActions, t1, t2, t3, t4);
            showPomodoroTransitionDialog("Đã bỏ qua giờ nghỉ", "Quay lại phiên tập trung 25 phút.", 25);
        }
    }

    private void showPomodoroTransitionDialog(String title, String message, int nextDuration) {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(nextDuration == 25 ? "Bắt đầu học" : "Bắt đầu nghỉ", (d, w) -> {
                    showPomodoro();
                    View screen = activity.contentFrame.getChildAt(0);
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
        List<StudyTask> tasks = activity.repository.getTasks();
        List<StudyTask> pending = new ArrayList<>();
        for (StudyTask task : tasks) {
            if (!task.isCompleted()) {
                pending.add(task);
            }
        }
        if (pending.isEmpty()) {
            activity.toast("Không có công việc nào đang mở");
            return;
        }
        String[] titles = new String[pending.size() + 1];
        titles[0] = "Không chọn công việc (Tự do)";
        for (int i = 0; i < pending.size(); i++) {
            titles[i + 1] = pending.get(i).getTitle();
        }

        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Chọn công việc")
                .setItems(titles, (d, w) -> {
                    if (w == 0) {
                        currentTask = null;
                        modeText.setText(MODE_FOCUS.equals(currentMode) ? "Tập trung tự do" : "Đang nghỉ ngơi");
                    } else {
                        currentTask = pending.get(w - 1);
                        modeText.setText(currentTask.getTitle());
                    }
                }).show();
    }

    private void showPomodoroSoundPanel() {
        Dialog dialog = new Dialog(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(activity.dp(24), activity.dp(14), activity.dp(24), activity.dp(24));
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.parseColor("#F7F7F7"));
        panelBg.setCornerRadii(new float[]{activity.dp(22), activity.dp(22), activity.dp(22), activity.dp(22), 0, 0, 0, 0});
        content.setBackground(panelBg);

        View handle = new View(activity);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(activity.dp(60), activity.dp(6));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, activity.dp(24));
        handle.setLayoutParams(handleParams);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(Color.parseColor("#D8D8D8"));
        handleBg.setCornerRadius(activity.dp(6));
        handle.setBackground(handleBg);
        content.addView(handle);

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(activity);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setText("Tập trung vào âm thanh");
        title.setTextColor(activity.getColor(R.color.ink));
        title.setTextSize(21f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        Switch soundSwitch = new Switch(activity);
        soundSwitch.setChecked(!soundManager.isMuted());
        header.addView(title);
        header.addView(soundSwitch);
        content.addView(header);

        TextView nowPlaying = soundCard(
                soundManager.getSoundName(),
                soundManager.isMuted() ? "Đang tắt âm thanh" : PomodoroSoundManager.subtitle(soundManager.getSoundName())
        );
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.dp(90));
        cardParams.setMargins(0, activity.dp(22), 0, activity.dp(22));
        nowPlaying.setLayoutParams(cardParams);
        content.addView(nowPlaying);

        content.addView(soundSectionLabel("Âm lượng"));

        LinearLayout volumeBox = new LinearLayout(activity);
        volumeBox.setGravity(android.view.Gravity.CENTER_VERTICAL);
        volumeBox.setOrientation(LinearLayout.HORIZONTAL);
        volumeBox.setPadding(activity.dp(18), 0, activity.dp(18), 0);
        GradientDrawable volumeBg = new GradientDrawable();
        volumeBg.setColor(Color.WHITE);
        volumeBg.setCornerRadius(activity.dp(14));
        volumeBox.setBackground(volumeBg);
        LinearLayout.LayoutParams volumeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.dp(74));
        volumeParams.setMargins(0, activity.dp(10), 0, activity.dp(24));
        volumeBox.setLayoutParams(volumeParams);

        TextView low = new TextView(activity);
        low.setText(soundManager.isMuted() ? "🔇" : "🔈");
        low.setTextSize(22f);
        low.setGravity(android.view.Gravity.CENTER);
        volumeBox.addView(low, new LinearLayout.LayoutParams(activity.dp(34), ViewGroup.LayoutParams.MATCH_PARENT));

        SeekBar volume = new SeekBar(activity);
        volume.setMax(100);
        volume.setProgress(Math.round(soundManager.getVolume() * 100f));
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        seekParams.setMargins(activity.dp(12), 0, activity.dp(12), 0);
        volumeBox.addView(volume, seekParams);

        TextView high = new TextView(activity);
        high.setText("🔊");
        high.setTextSize(22f);
        high.setGravity(android.view.Gravity.CENTER);
        volumeBox.addView(high, new LinearLayout.LayoutParams(activity.dp(34), ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(volumeBox);

        content.addView(soundSectionLabel("Danh sách phát"));
        content.addView(soundChoiceRow("tiếng mưa", "Mưa nhẹ để giữ nhịp tập trung", dialog));
        content.addView(soundChoiceRow("tiếng sóng", "Sóng biển đều và thư giãn", dialog));
        content.addView(soundChoiceRow("tiếng củi cháy", "Âm lửa nhỏ ấm và chậm", dialog));
        content.addView(soundChoiceRow("tiếng rừng ban đêm", "Nền rừng dịu cho buổi tối", dialog));
        content.addView(soundChoiceRow("tiếng thư viện", "Không gian yên tĩnh khi học", dialog));

        soundSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            soundManager.setMuted(!checked);
            String soundName = soundManager.getSoundName();
            nowPlaying.setText(PomodoroSoundManager.cardText(soundName, soundManager.isMuted() ? "Đang tắt âm thanh" : PomodoroSoundManager.subtitle(soundName)));
            low.setText(soundManager.isMuted() ? "🔇" : "🔈");
            if (soundManager.isMuted()) {
                soundManager.pause();
            } else if (running && MODE_FOCUS.equals(currentMode)) {
                soundManager.play();
            }
        });
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                soundManager.setVolume(progress / 100f);
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
        TextView label = new TextView(activity);
        label.setText(text);
        label.setTextColor(activity.getColor(R.color.ink));
        label.setTextSize(20f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        return label;
    }

    private TextView soundCard(String name, String subtitle) {
        TextView view = new TextView(activity);
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        view.setPadding(activity.dp(18), 0, activity.dp(16), 0);
        view.setText(PomodoroSoundManager.cardText(name, subtitle));
        view.setTextColor(activity.getColor(R.color.ink));
        view.setTextSize(16f);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FFFDEB"));
        bg.setStroke(activity.dp(2), activity.getColor(R.color.ink));
        bg.setCornerRadius(activity.dp(8));
        view.setBackground(bg);
        return view;
    }

    private TextView soundChoiceRow(String name, String subtitle, Dialog dialog) {
        TextView row = new TextView(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.dp(70));
        params.setMargins(0, activity.dp(10), 0, 0);
        row.setLayoutParams(params);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(activity.dp(18), 0, activity.dp(18), 0);
        row.setText((soundManager.getSoundName().equals(name) ? "●  " : "○  ") + PomodoroSoundManager.icon(name) + "  " + name + "\n   " + subtitle + "                                      ⋯");
        row.setTextColor(activity.getColor(R.color.ink));
        row.setTextSize(15f);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(activity.dp(14));
        row.setBackground(bg);
        row.setOnClickListener(v -> {
            soundManager.setSoundName(name);
            if (!soundManager.isMuted() && running && MODE_FOCUS.equals(currentMode)) {
                soundManager.stop();
                soundManager.play();
            }
            activity.toast("Đã chọn " + name);
            dialog.dismiss();
        });
        return row;
    }

    private void showPomodoroHistory() {
        List<PomodoroSession> sessions = activity.repository.getRecentPomodoroSessions(12);
        if (sessions.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Lịch sử Pomodoro")
                    .setMessage("Chưa có phiên Pomodoro nào. Hãy bắt đầu một phiên tập trung để ghi nhận lịch sử.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Hôm nay: ")
                .append(activity.repository.getTodayFocusMinutes())
                .append(" phút • ")
                .append(activity.repository.getTodayFocusSessions())
                .append(" phiên\n\n");
        for (PomodoroSession session : sessions) {
            builder.append("- ")
                    .append(pomodoroModeLabel(session.getMode()))
                    .append(" • ")
                    .append(session.getCompletedMinutes())
                    .append("/")
                    .append(session.getDurationMinutes())
                    .append(" phút");
            if (!TextUtils.isEmpty(session.getSubjectTag())) {
                builder.append(" • ").append(session.getSubjectTag());
            }
            builder.append(" • ")
                    .append(DateTimeUtils.formatDateTime(session.getStartedAt()))
                    .append(session.isCompleted() ? " • xong" : " • dở dang")
                    .append("\n");
        }
        new AlertDialog.Builder(activity)
                .setTitle("Lịch sử Pomodoro")
                .setMessage(builder.toString().trim())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private String pomodoroModeLabel(String mode) {
        if (MODE_SHORT_BREAK.equals(mode)) {
            return "Nghỉ ngắn";
        }
        if (MODE_LONG_BREAK.equals(mode)) {
            return "Nghỉ dài";
        }
        return "Tập trung";
    }

    private void sendPomodoroNotification(String title, String message) {
        if (!activity.hasNotificationPermission()) {
            activity.requestNotificationPermissionOnce();
            return;
        }
        NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "pomodoro_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Pomodoro Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, channelId)
                .setSmallIcon(R.drawable.ic_tomato_active)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify(1, builder.build());
    }
}
