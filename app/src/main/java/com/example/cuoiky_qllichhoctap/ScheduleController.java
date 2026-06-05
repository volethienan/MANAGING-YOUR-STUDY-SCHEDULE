package com.example.cuoiky_qllichhoctap;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.ui.WeekCalendarView;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ScheduleController {
    private static final String CALENDAR_DAY = "Ngày";
    private static final String CALENDAR_THREE_DAYS = "3 ngày";
    private static final String CALENDAR_WEEK = "Tuần";

    private final MainActivity activity;
    private long weekStartMillis = DateTimeUtils.startOfWeek(System.currentTimeMillis());
    private String filter = "Tất cả";
    private String viewMode = CALENDAR_WEEK;

    ScheduleController(MainActivity activity) {
        this.activity = activity;
    }

    void showSchedule(String filter) {
        this.filter = filter;
        View screen = activity.inflateScreen(R.layout.screen_schedule, true, MainActivity.SCREEN_SCHEDULE);
        normalizeScheduleStart();
        setupScheduleViewModes(screen);
        setupScheduleFilters(screen, filter);
        int visibleDays = scheduleVisibleDays();
        activity.setText(screen, R.id.textWeekRange, DateTimeUtils.formatDayRange(weekStartMillis, visibleDays));
        screen.findViewById(R.id.btnPrevWeek).setOnClickListener(v -> {
            weekStartMillis = DateTimeUtils.addDays(weekStartMillis, -visibleDays);
            showCurrent();
        });
        screen.findViewById(R.id.btnThisWeek).setOnClickListener(v -> {
            weekStartMillis = startForScheduleMode(viewMode, System.currentTimeMillis());
            showCurrent();
        });
        screen.findViewById(R.id.btnNextWeek).setOnClickListener(v -> {
            weekStartMillis = DateTimeUtils.addDays(weekStartMillis, visibleDays);
            showCurrent();
        });

        List<StudyEvent> visibleEvents = visibleRangeEvents(filter);
        Set<String> visibleConflictIds = conflictIds(visibleEvents);
        WeekCalendarView weekCalendar = screen.findViewById(R.id.weekCalendar);
        weekCalendar.setRange(weekStartMillis, visibleDays);
        weekCalendar.setEvents(visibleEvents, visibleConflictIds);
        weekCalendar.setOnEventClickListener(activity::showEventActions);
        weekCalendar.setOnEmptySlotClickListener(startAt -> activity.showEventDialog(null, this::showCurrent, startAt));
        weekCalendar.setOnEventMoveRequestListener((event, newStartAt) -> activity.showMoveEventConfirmation(event, newStartAt));

        TextView conflictTitle = screen.findViewById(R.id.textConflictTitle);
        LinearLayout eventList = screen.findViewById(R.id.eventList);
        eventList.removeAllViews();

        List<StudyEvent> conflictEvents = conflictEvents(visibleEvents, visibleConflictIds);
        conflictTitle.setVisibility(conflictEvents.isEmpty() ? View.GONE : View.VISIBLE);
        eventList.setVisibility(conflictEvents.isEmpty() ? View.GONE : View.VISIBLE);
        conflictTitle.setText("Kiểm tra xung đột (" + conflictEvents.size() + ")");

        for (StudyEvent event : conflictEvents) {
            View row = activity.createEventRow(event, eventList);
            row.setOnClickListener(v -> activity.showEventActions(event));
            eventList.addView(row);
        }
        screen.findViewById(R.id.btnAddEvent).setOnClickListener(v -> activity.showEventDialog(null, () -> showSchedule(filter)));
        screen.findViewById(R.id.btnImportImage).setOnClickListener(v -> activity.showImageImportOptions());
    }

    void showCurrent() {
        showSchedule(filter);
    }

    void showAllAround(long startAt) {
        weekStartMillis = CALENDAR_WEEK.equals(viewMode)
                ? DateTimeUtils.startOfWeek(startAt)
                : DateTimeUtils.startOfDay(startAt);
        showSchedule("Tất cả");
    }

    private void setupScheduleViewModes(View screen) {
        bindCalendarMode(screen, R.id.modeDay, CALENDAR_DAY);
        bindCalendarMode(screen, R.id.modeThreeDays, CALENDAR_THREE_DAYS);
        bindCalendarMode(screen, R.id.modeWeek, CALENDAR_WEEK);
    }

    private void bindCalendarMode(View screen, int id, String mode) {
        TextView view = screen.findViewById(id);
        view.setBackgroundResource(mode.equals(viewMode) ? R.drawable.bg_selected_pill : R.drawable.bg_outline_pill);
        view.setOnClickListener(v -> {
            if (mode.equals(viewMode)) {
                return;
            }
            viewMode = mode;
            weekStartMillis = startForScheduleMode(mode, System.currentTimeMillis());
            showCurrent();
        });
    }

    private void setupScheduleFilters(View screen, String active) {
        activity.bindFilter(screen, R.id.filterAll, "Tất cả", active, () -> showSchedule("Tất cả"));
        activity.bindFilter(screen, R.id.filterStudy, StudyEvent.TYPE_STUDY, active, () -> showSchedule(StudyEvent.TYPE_STUDY));
        activity.bindFilter(screen, R.id.filterExam, StudyEvent.TYPE_EXAM, active, () -> showSchedule(StudyEvent.TYPE_EXAM));
        activity.bindFilter(screen, R.id.filterDeadline, StudyEvent.TYPE_DEADLINE, active, () -> showSchedule(StudyEvent.TYPE_DEADLINE));
        activity.bindFilter(screen, R.id.filterPersonal, StudyEvent.TYPE_PERSONAL, active, () -> showSchedule(StudyEvent.TYPE_PERSONAL));
    }

    private List<StudyEvent> visibleRangeEvents(String filter) {
        List<StudyEvent> result = new ArrayList<>();
        int visibleDays = scheduleVisibleDays();
        for (StudyEvent event : activity.repository.getEvents()) {
            if (!DateTimeUtils.isInDayRange(event.getStartAt(), weekStartMillis, visibleDays)) {
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
        if (CALENDAR_DAY.equals(viewMode)) {
            return 1;
        }
        if (CALENDAR_THREE_DAYS.equals(viewMode)) {
            return 3;
        }
        return 7;
    }

    private void normalizeScheduleStart() {
        weekStartMillis = CALENDAR_WEEK.equals(viewMode)
                ? DateTimeUtils.startOfWeek(weekStartMillis)
                : DateTimeUtils.startOfDay(weekStartMillis);
    }

    private long startForScheduleMode(String mode, long millis) {
        return CALENDAR_WEEK.equals(mode)
                ? DateTimeUtils.startOfWeek(millis)
                : DateTimeUtils.startOfDay(millis);
    }

    private Set<String> conflictIds(List<StudyEvent> events) {
        Set<String> ids = new HashSet<>();
        for (StudyEvent event : events) {
            if (activity.repository.hasConflict(event)) {
                ids.add(event.getId());
            }
        }
        return ids;
    }

    private List<StudyEvent> conflictEvents(List<StudyEvent> events, Set<String> conflictIds) {
        List<StudyEvent> result = new ArrayList<>();
        for (StudyEvent event : events) {
            if (conflictIds.contains(event.getId())) {
                result.add(event);
            }
        }
        return result;
    }
}
