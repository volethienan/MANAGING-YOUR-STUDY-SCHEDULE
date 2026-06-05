package com.example.cuoiky_qllichhoctap.util;

import android.text.TextUtils;

import com.example.cuoiky_qllichhoctap.data.StudyRepository;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.UserProfile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class StudyStats {
    private StudyStats() {
    }

    public static String greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) {
            return "Chào buổi sáng";
        }
        if (hour >= 11 && hour < 18) {
            return "Chào buổi chiều";
        }
        return "Chào buổi tối";
    }

    public static String dashboardSummary(int todayTotal, int todayCompleted, int todayRemaining) {
        if (todayTotal == 0) {
            return "Hôm nay chưa có việc học. Bạn có thể thêm một mục tiêu nhỏ để bắt nhịp.";
        }
        if (todayRemaining == 0) {
            return "Bạn đã hoàn thành toàn bộ việc học hôm nay. Rất gọn gàng.";
        }
        return "Hôm nay còn " + todayRemaining + " việc cần xử lý, đã xong " + todayCompleted + "/" + todayTotal + ".";
    }

    public static String avatarMark(String avatarChoice, UserProfile profile) {
        if ("Chữ viết tắt".equals(avatarChoice)) {
            return initialsOf(profile.getName());
        }
        return mascotMark(avatarChoice);
    }

    public static String mascotMark(String choice) {
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

    public static String shortStatus(String status) {
        if (TextUtils.isEmpty(status)) {
            return "Học tập";
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

    public static String encouragementMessage(String mascotChoice, String type) {
        String mascot = mascotMark(mascotChoice);
        if ("pomodoro".equals(type)) {
            return mascot + ": Xong 25 phút rồi, nghỉ một nhịp nhé!";
        }
        return mascot + ": Tốt lắm, thêm một việc đã gọn!";
    }

    public static String deadlineMeta(StudyTask task) {
        String status = task.getDueAt() < System.currentTimeMillis() ? "Quá hạn" : "Đến hạn";
        return status + " · " + task.getSubject() + " · " + DateTimeUtils.formatDayLabel(task.getDueAt()) + " " + DateTimeUtils.formatTime(task.getDueAt());
    }

    public static String deadlineEventMeta(StudyEvent event) {
        String status = event.getStartAt() < System.currentTimeMillis() ? "Quá hạn" : "Đến hạn";
        String subject = TextUtils.isEmpty(event.getSubject()) ? "Nội dung deadline" : event.getSubject();
        return status + " · " + subject + " · " + DateTimeUtils.formatDayLabel(event.getStartAt()) + " " + DateTimeUtils.formatTime(event.getStartAt());
    }

    public static String statsSubtitle(int completion, int overdue, int todayFocusMinutes) {
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

    public static String statsInsight(int totalTasks, int overdue, int todayTotal, int todayRemaining, int totalEvents, int todayFocusMinutes) {
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

    public static StudyEvent findNextEvent(List<StudyEvent> events) {
        long now = System.currentTimeMillis();
        for (StudyEvent event : events) {
            if (event.getStartAt() >= now) {
                return event;
            }
        }
        return null;
    }

    public static StudyTask findNearestDeadlineTask(List<StudyTask> tasks) {
        for (StudyTask task : tasks) {
            if (!task.isCompleted()) {
                return task;
            }
        }
        return null;
    }

    public static StudyEvent findNearestDeadlineEvent(List<StudyEvent> events, StudyRepository repository) {
        long now = System.currentTimeMillis();
        StudyEvent fallbackOverdue = null;
        for (StudyEvent event : events) {
            if (!StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
                continue;
            }
            if (!TextUtils.isEmpty(event.getSourceTaskId())) {
                StudyTask sourceTask = repository.getTask(event.getSourceTaskId());
                if (sourceTask != null && sourceTask.isCompleted()) {
                    continue;
                }
            }
            if (event.getStartAt() >= now) {
                return event;
            }
            fallbackOverdue = event;
        }
        return fallbackOverdue;
    }

    public static List<StudyTask> topPriorityTasks(List<StudyTask> tasks) {
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

    public static int countTodayTasks(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (!task.isCompleted() && DateTimeUtils.isToday(task.getDueAt())) {
                count++;
            }
        }
        return count;
    }

    public static int countAllTodayTasks(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (DateTimeUtils.isToday(task.getDueAt())) {
                count++;
            }
        }
        return count;
    }

    public static int countCompletedTodayTasks(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (task.isCompleted() && DateTimeUtils.isToday(task.getDueAt())) {
                count++;
            }
        }
        return count;
    }

    public static int countCompleted(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public static int countOverdue(List<StudyTask> tasks) {
        int count = 0;
        long now = System.currentTimeMillis();
        for (StudyTask task : tasks) {
            if (!task.isCompleted() && task.getDueAt() < now) {
                count++;
            }
        }
        return count;
    }

    public static int countPending(List<StudyTask> tasks) {
        int count = 0;
        for (StudyTask task : tasks) {
            if (!task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public static int countEventsByType(List<StudyEvent> events, String type) {
        int count = 0;
        for (StudyEvent event : events) {
            if (type.equals(event.getType())) {
                count++;
            }
        }
        return count;
    }

    public static int percentOf(int value, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(value * 100f / total);
    }

    public static int completionRate(List<StudyTask> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }
        return Math.round(countCompleted(tasks) * 100f / tasks.size());
    }

    private static String initialsOf(String fullName) {
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
}
