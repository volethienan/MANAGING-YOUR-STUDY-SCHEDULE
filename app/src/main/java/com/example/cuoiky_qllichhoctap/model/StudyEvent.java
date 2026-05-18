package com.example.cuoiky_qllichhoctap.model;

import org.json.JSONException;
import org.json.JSONObject;

public class StudyEvent {
    public static final String TYPE_STUDY = "Lịch học";
    public static final String TYPE_EXAM = "Lịch thi";
    public static final String TYPE_DEADLINE = "Deadline";
    public static final String TYPE_PERSONAL = "Công việc cá nhân";

    private final String id;
    private String title;
    private String type;
    private String subject;
    private long startAt;
    private long endAt;
    private String room;
    private String note;
    private boolean reminderEnabled;
    private int reminderBeforeMinutes;

    public StudyEvent(String id, String title, String type, String subject, long startAt, long endAt, String room, String note) {
        this(id, title, type, subject, startAt, endAt, room, note, false, 15);
    }

    public StudyEvent(String id, String title, String type, String subject, long startAt, long endAt, String room, String note,
                      boolean reminderEnabled, int reminderBeforeMinutes) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.subject = subject;
        this.startAt = startAt;
        this.endAt = endAt;
        this.room = room;
        this.note = note;
        this.reminderEnabled = reminderEnabled;
        this.reminderBeforeMinutes = reminderBeforeMinutes <= 0 ? 15 : reminderBeforeMinutes;
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
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getStartAt() {
        return startAt;
    }

    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }

    public long getEndAt() {
        return endAt;
    }

    public void setEndAt(long endAt) {
        this.endAt = endAt;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public int getReminderBeforeMinutes() {
        return reminderBeforeMinutes;
    }

    public void setReminderBeforeMinutes(int reminderBeforeMinutes) {
        this.reminderBeforeMinutes = reminderBeforeMinutes <= 0 ? 15 : reminderBeforeMinutes;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("type", type);
        json.put("subject", subject);
        json.put("startAt", startAt);
        json.put("endAt", endAt);
        json.put("room", room);
        json.put("note", note);
        json.put("reminderEnabled", reminderEnabled);
        json.put("reminderBeforeMinutes", reminderBeforeMinutes);
        return json;
    }

    public static StudyEvent fromJson(JSONObject json) {
        return new StudyEvent(
                json.optString("id"),
                json.optString("title"),
                json.optString("type", TYPE_STUDY),
                json.optString("subject"),
                json.optLong("startAt"),
                json.optLong("endAt"),
                json.optString("room"),
                json.optString("note"),
                json.optBoolean("reminderEnabled", false),
                json.optInt("reminderBeforeMinutes", 15)
        );
    }
}
