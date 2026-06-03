package com.example.cuoiky_qllichhoctap.model;

public class CountdownMilestone {
    public static final String TYPE_EXAM = "Ngày thi";
    public static final String TYPE_BIRTHDAY = "Sinh nhật";
    public static final String TYPE_EVENT = "Sự kiện quan trọng";
    public static final String TYPE_OTHER = "Khác";

    private final String id;
    private String title;
    private String type;
    private long targetDate;
    private String note;

    public CountdownMilestone(String id, String title, String type, long targetDate, String note) {
        this.id = id;
        this.title = title;
        this.type = normalizeType(type);
        this.targetDate = targetDate;
        this.note = note == null ? "" : note;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = normalizeType(type);
    }

    public long getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(long targetDate) {
        this.targetDate = targetDate;
    }

    public String getNote() {
        return note == null ? "" : note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note;
    }

    public static String normalizeType(String type) {
        if (type == null) {
            return TYPE_EVENT;
        }
        String value = type.trim();
        String lower = value.toLowerCase();
        if (TYPE_EXAM.equals(value) || lower.contains("thi")) {
            return TYPE_EXAM;
        }
        if (TYPE_BIRTHDAY.equals(value) || lower.contains("sinh nhật") || lower.contains("sinh nhat")) {
            return TYPE_BIRTHDAY;
        }
        if (TYPE_OTHER.equals(value) || lower.contains("khác") || lower.contains("khac")) {
            return TYPE_OTHER;
        }
        return TYPE_EVENT;
    }
}
