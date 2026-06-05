package com.example.cuoiky_qllichhoctap.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import com.example.cuoiky_qllichhoctap.EventReminderReceiver;
import com.example.cuoiky_qllichhoctap.data.StudyRepository;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;

public class ReminderScheduler {
    public interface EventTypeLabelResolver {
        String labelFor(String type);
    }

    public interface NotificationPermissionGateway {
        boolean hasPermission();

        void requestOnce();
    }

    private final Context context;
    private final EventTypeLabelResolver eventTypeLabelResolver;
    private final NotificationPermissionGateway permissionGateway;
    private StudyRepository repository;

    public ReminderScheduler(
            Context context,
            StudyRepository repository,
            EventTypeLabelResolver eventTypeLabelResolver,
            NotificationPermissionGateway permissionGateway
    ) {
        this.context = context;
        this.repository = repository;
        this.eventTypeLabelResolver = eventTypeLabelResolver;
        this.permissionGateway = permissionGateway;
    }

    public void setRepository(StudyRepository repository) {
        this.repository = repository;
    }

    public void scheduleEventReminder(StudyEvent event) {
        cancelEventReminder(event);
        if (!event.isReminderEnabled()) {
            return;
        }
        if (!permissionGateway.hasPermission()) {
            permissionGateway.requestOnce();
            return;
        }
        long triggerAt = event.getStartAt() - event.getReminderBeforeMinutes() * 60L * 1000L;
        if (triggerAt <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = reminderPendingIntent(event, PendingIntent.FLAG_UPDATE_CURRENT);
        setReminderAlarm(alarmManager, triggerAt, pendingIntent);
    }

    public void cancelEventReminder(StudyEvent event) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = reminderPendingIntent(event, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public void scheduleTaskReminder(StudyTask task) {
        cancelTaskReminder(task);
        if (task.isCompleted() || task.getReminderTime() <= System.currentTimeMillis()) {
            return;
        }
        if (!permissionGateway.hasPermission()) {
            permissionGateway.requestOnce();
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = taskReminderPendingIntent(task, PendingIntent.FLAG_UPDATE_CURRENT);
        setReminderAlarm(alarmManager, task.getReminderTime(), pendingIntent);
    }

    public void cancelTaskReminder(StudyTask task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = taskReminderPendingIntent(task, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public void syncTaskCalendarEvent(StudyTask task) {
        StudyEvent previous = repository.getEventForTask(task.getId());
        StudyEvent synced = repository.syncTaskDeadlineEvent(task);
        if (previous != null && (synced == null || !previous.getId().equals(synced.getId()))) {
            cancelEventReminder(previous);
        }
        if (synced != null) {
            scheduleEventReminder(synced);
        }
    }

    public void deleteTaskAndLinkedCalendar(StudyTask task) {
        StudyEvent linked = repository.getEventForTask(task.getId());
        if (linked != null) {
            cancelEventReminder(linked);
        }
        repository.deleteTask(task.getId());
    }

    public void rescheduleAllReminders() {
        if (!permissionGateway.hasPermission()) {
            permissionGateway.requestOnce();
            return;
        }
        for (StudyEvent event : repository.getEvents()) {
            scheduleEventReminder(event);
        }
        for (StudyTask task : repository.getTasks()) {
            scheduleTaskReminder(task);
        }
    }

    private PendingIntent reminderPendingIntent(StudyEvent event, int modeFlag) {
        Intent intent = new Intent(context, EventReminderReceiver.class);
        intent.putExtra(EventReminderReceiver.EXTRA_EVENT_ID, event.getId());
        intent.putExtra(EventReminderReceiver.EXTRA_TITLE, event.getTitle());
        intent.putExtra(EventReminderReceiver.EXTRA_MESSAGE, eventReminderMessage(event));
        int flags = modeFlag | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, EventReminderReceiver.notificationId(event.getId()), intent, flags);
    }

    private String eventReminderMessage(StudyEvent event) {
        String message = eventTypeLabelResolver.labelFor(event.getType()) + " lúc " + DateTimeUtils.formatTime(event.getStartAt());
        if (!TextUtils.isEmpty(event.getRoom())) {
            message += " • " + event.getRoom();
        }
        return message;
    }

    private PendingIntent taskReminderPendingIntent(StudyTask task, int modeFlag) {
        Intent intent = new Intent(context, EventReminderReceiver.class);
        intent.putExtra(EventReminderReceiver.EXTRA_EVENT_ID, "task_" + task.getId());
        intent.putExtra(EventReminderReceiver.EXTRA_TITLE, task.getTitle());
        intent.putExtra(EventReminderReceiver.EXTRA_MESSAGE, "Deadline " + DateTimeUtils.formatDateTime(task.getDueAt()) + " • " + task.getTag());
        int flags = modeFlag | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, EventReminderReceiver.notificationId("task_" + task.getId()), intent, flags);
    }

    private void setReminderAlarm(AlarmManager alarmManager, long triggerAt, PendingIntent pendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }
}
