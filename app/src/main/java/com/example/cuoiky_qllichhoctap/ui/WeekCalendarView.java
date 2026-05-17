package com.example.cuoiky_qllichhoctap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.cuoiky_qllichhoctap.R;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WeekCalendarView extends View {
    public interface OnEventClickListener {
        void onEventClick(StudyEvent event);
    }

    private static final int START_HOUR = 6;
    private static final int END_HOUR = 22;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final List<StudyEvent> events = new ArrayList<>();
    private final List<EventHitBox> hitBoxes = new ArrayList<>();
    private final Set<String> conflictIds = new HashSet<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private long rangeStartMillis = DateTimeUtils.startOfWeek(System.currentTimeMillis());
    private int visibleDayCount = 7;
    private OnEventClickListener listener;

    public WeekCalendarView(Context context) {
        super(context);
        init();
    }

    public WeekCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setMinimumHeight(dp(860));
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
    }

    public void setWeekStartMillis(long weekStartMillis) {
        setRange(weekStartMillis, 7);
    }

    public void setRange(long startMillis, int dayCount) {
        this.visibleDayCount = Math.max(1, Math.min(7, dayCount));
        this.rangeStartMillis = this.visibleDayCount == 7
                ? DateTimeUtils.startOfWeek(startMillis)
                : DateTimeUtils.startOfDay(startMillis);
        invalidate();
    }

    public void setEvents(List<StudyEvent> newEvents, Set<String> newConflictIds) {
        events.clear();
        events.addAll(newEvents);
        conflictIds.clear();
        conflictIds.addAll(newConflictIds);
        invalidate();
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        hitBoxes.clear();

        int leftGutter = dp(42);
        int headerHeight = dp(58);
        int width = getWidth();
        int height = Math.max(getHeight(), dp(860));
        float colWidth = (width - leftGutter - dp(8)) / (float) visibleDayCount;
        float hourHeight = (height - headerHeight - dp(22)) / (float) (END_HOUR - START_HOUR);

        paint.setColor(color(R.color.paper_light));
        canvas.drawRoundRect(new RectF(dp(4), dp(4), width - dp(4), height - dp(4)), dp(18), dp(18), paint);

        drawHeaders(canvas, leftGutter, colWidth, headerHeight);
        drawGrid(canvas, leftGutter, headerHeight, colWidth, hourHeight, width);
        drawEvents(canvas, leftGutter, headerHeight, colWidth, hourHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP || listener == null) {
            return true;
        }
        for (EventHitBox hitBox : hitBoxes) {
            if (hitBox.rect.contains(event.getX(), event.getY())) {
                listener.onEventClick(hitBox.event);
                return true;
            }
        }
        return true;
    }

    private void drawHeaders(Canvas canvas, int leftGutter, float colWidth, int headerHeight) {
        String[] dayNames = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        Calendar calendar = Calendar.getInstance();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(12));
        for (int i = 0; i < visibleDayCount; i++) {
            calendar.setTimeInMillis(DateTimeUtils.addDays(rangeStartMillis, i));
            float cx = leftGutter + i * colWidth + colWidth / 2f;
            float chipHalf = Math.min(dp(28), colWidth / 2f - dp(4));
            paint.setColor(isToday(calendar.getTimeInMillis()) ? color(R.color.yellow) : color(R.color.paper));
            canvas.drawRoundRect(new RectF(cx - chipHalf, dp(12), cx + chipHalf, headerHeight - dp(8)), dp(14), dp(14), paint);
            textPaint.setColor(color(R.color.ink));
            canvas.drawText(dayNames[normalizedDayIndex(calendar.get(Calendar.DAY_OF_WEEK))], cx, dp(29), textPaint);
            textPaint.setColor(color(R.color.muted));
            canvas.drawText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), cx, dp(47), textPaint);
        }
    }

    private void drawGrid(Canvas canvas, int leftGutter, int headerHeight, float colWidth, float hourHeight, int width) {
        paint.setStrokeWidth(dp(1));
        paint.setColor(color(R.color.line));
        textPaint.setTextSize(sp(10));
        textPaint.setColor(color(R.color.muted));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
            float y = headerHeight + (hour - START_HOUR) * hourHeight;
            canvas.drawLine(leftGutter, y, width - dp(8), y, paint);
            if (hour < END_HOUR) {
                canvas.drawText(String.format(Locale.getDefault(), "%02d:00", hour), leftGutter - dp(6), y + dp(4), textPaint);
            }
        }
        for (int day = 0; day <= visibleDayCount; day++) {
            float x = leftGutter + day * colWidth;
            canvas.drawLine(x, headerHeight, x, headerHeight + (END_HOUR - START_HOUR) * hourHeight, paint);
        }
        drawNowLine(canvas, leftGutter, headerHeight, colWidth, hourHeight);
    }

    private void drawEvents(Canvas canvas, int leftGutter, int headerHeight, float colWidth, float hourHeight) {
        Calendar calendar = Calendar.getInstance();
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(sp(10));
        for (StudyEvent event : events) {
            int dayIndex = dayIndexOf(event.getStartAt());
            if (dayIndex < 0 || dayIndex >= visibleDayCount) {
                continue;
            }
            calendar.setTimeInMillis(event.getStartAt());
            float startHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f;
            calendar.setTimeInMillis(event.getEndAt());
            float endHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f;
            if (endHour <= startHour) {
                endHour = startHour + 1f;
            }
            int laneCount = Math.max(1, overlapCount(event));
            int laneIndex = overlapIndex(event);
            float laneWidth = (colWidth - dp(6)) / laneCount;
            float left = leftGutter + dayIndex * colWidth + dp(3) + laneIndex * laneWidth;
            float right = left + laneWidth - dp(3);
            float top = headerHeight + Math.max(0, startHour - START_HOUR) * hourHeight + dp(2);
            float bottom = headerHeight + Math.min(END_HOUR - START_HOUR, endHour - START_HOUR) * hourHeight - dp(2);
            if (bottom <= top) {
                bottom = top + dp(34);
            }
            RectF rect = new RectF(left, top, right, bottom);
            paint.setColor(eventColor(event));
            canvas.drawRoundRect(rect, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(conflictIds.contains(event.getId()) ? 3 : 1));
            paint.setColor(color(conflictIds.contains(event.getId()) ? R.color.rose : R.color.ink));
            canvas.drawRoundRect(rect, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(color(R.color.ink));
            canvas.drawText(trim(event.getTitle(), Math.max(8, (int) (laneWidth / dp(7)))), left + dp(6), top + dp(16), textPaint);
            textPaint.setColor(conflictIds.contains(event.getId()) ? color(R.color.rose) : color(R.color.muted));
            String timeText = visibleDayCount == 1
                    ? DateTimeUtils.formatTime(event.getStartAt()) + " - " + DateTimeUtils.formatTime(event.getEndAt())
                    : DateTimeUtils.formatTime(event.getStartAt());
            canvas.drawText(trim(timeText, Math.max(8, (int) (laneWidth / dp(7)))), left + dp(6), top + dp(31), textPaint);
            hitBoxes.add(new EventHitBox(rect, event));
        }
    }

    private void drawNowLine(Canvas canvas, int leftGutter, int headerHeight, float colWidth, float hourHeight) {
        long now = System.currentTimeMillis();
        int todayIndex = dayIndexOf(now);
        if (todayIndex < 0 || todayIndex >= visibleDayCount) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        float currentHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f;
        if (currentHour < START_HOUR || currentHour > END_HOUR) {
            return;
        }
        float y = headerHeight + (currentHour - START_HOUR) * hourHeight;
        float left = leftGutter + todayIndex * colWidth;
        float right = left + colWidth;
        paint.setColor(color(R.color.rose));
        paint.setStrokeWidth(dp(2));
        canvas.drawLine(left + dp(4), y, right - dp(4), y, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(left + dp(5), y, dp(4), paint);
    }

    private int dayIndexOf(long millis) {
        long dayStart = DateTimeUtils.startOfDay(millis);
        long start = DateTimeUtils.startOfDay(rangeStartMillis);
        return (int) ((dayStart - start) / DAY_MS);
    }

    private int overlapCount(StudyEvent target) {
        int count = 0;
        for (StudyEvent event : events) {
            if (DateTimeUtils.isSameDay(event.getStartAt(), target.getStartAt())
                    && DateTimeUtils.rangesOverlap(target.getStartAt(), target.getEndAt(), event.getStartAt(), event.getEndAt())) {
                count++;
            }
        }
        return count;
    }

    private int overlapIndex(StudyEvent target) {
        int index = 0;
        for (StudyEvent event : events) {
            if (event.getId().equals(target.getId())) {
                return index;
            }
            if (DateTimeUtils.isSameDay(event.getStartAt(), target.getStartAt())
                    && DateTimeUtils.rangesOverlap(target.getStartAt(), target.getEndAt(), event.getStartAt(), event.getEndAt())) {
                index++;
            }
        }
        return 0;
    }

    private int normalizedDayIndex(int dayOfWeek) {
        return dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - Calendar.MONDAY;
    }

    private boolean isToday(long millis) {
        return DateTimeUtils.isToday(millis);
    }

    private int eventColor(StudyEvent event) {
        if (StudyEvent.TYPE_EXAM.equals(event.getType())) {
            return color(R.color.pink);
        }
        if (StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
            return color(R.color.yellow_soft);
        }
        return color(R.color.mint);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + ".";
    }

    private int color(int resId) {
        return getResources().getColor(resId, getContext().getTheme());
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static class EventHitBox {
        final RectF rect;
        final StudyEvent event;

        EventHitBox(RectF rect, StudyEvent event) {
            this.rect = rect;
            this.event = event;
        }
    }
}
