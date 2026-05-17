package com.example.cuoiky_qllichhoctap.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateTimeUtils {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("dd/MM/yyyy HH:mm", VI);
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM/yyyy", VI);
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm", VI);
    private static final SimpleDateFormat DAY_LABEL = new SimpleDateFormat("EEE, dd/MM", VI);

    private DateTimeUtils() {
    }

    public static long parseDateTime(String value, long fallback) {
        try {
            Date date = DATE_TIME.parse(value);
            return date == null ? fallback : date.getTime();
        } catch (ParseException exception) {
            return fallback;
        }
    }

    public static long combineDateAndTime(String dateValue, String timeValue, long fallback) {
        return parseDateTime(dateValue + " " + timeValue, fallback);
    }

    public static String formatDateTime(long millis) {
        return DATE_TIME.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return DATE.format(new Date(millis));
    }

    public static String formatTime(long millis) {
        return TIME.format(new Date(millis));
    }

    public static String formatDayLabel(long millis) {
        return DAY_LABEL.format(new Date(millis));
    }

    public static boolean isToday(long millis) {
        Calendar current = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        return current.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && current.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    public static long startOfWeek(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long addDays(long millis, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTimeInMillis();
    }

    public static boolean isSameWeek(long millis, long weekStartMillis) {
        long weekEnd = addDays(weekStartMillis, 7);
        return millis >= weekStartMillis && millis < weekEnd;
    }

    public static String formatWeekRange(long weekStartMillis) {
        return DATE.format(new Date(weekStartMillis)) + " - " + DATE.format(new Date(addDays(weekStartMillis, 6)));
    }

    public static boolean isSoon(long millis) {
        long now = System.currentTimeMillis();
        long threeDays = 3L * 24L * 60L * 60L * 1000L;
        return millis >= now && millis <= now + threeDays;
    }

    public static long daysFromNow(int days, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static boolean rangesOverlap(long startA, long endA, long startB, long endB) {
        return startA < endB && startB < endA;
    }
}
