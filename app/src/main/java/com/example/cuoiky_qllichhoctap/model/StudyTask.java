package com.example.cuoiky_qllichhoctap.model;

import org.json.JSONException;
import org.json.JSONObject;

public class StudyTask {
    public static final String PRIORITY_HIGH = "Cao";
    public static final String PRIORITY_MEDIUM = "Trung bình";
    public static final String PRIORITY_LOW = "Thấp";

    private final String id;
    private String title;
    private String subject;
    private long dueAt;
    private String priority;
    private String note;
    private boolean completed;
    private boolean important;
    private boolean urgent;
    private String tag;
    private long reminderTime;
    private String repeatOption;
    private int estimatedPomodoro;
    private String markerType;
    private String markerValue;

    public StudyTask(String id, String title, String subject, long dueAt, String priority, String note, boolean completed) {
        this(id, title, subject, dueAt, priority, note, completed, PRIORITY_HIGH.equals(priority), false, subject, 0L, "Không lặp", 0, "flag", "");
    }

    public StudyTask(String id, String title, String subject, long dueAt, String priority, String note, boolean completed,
                     boolean important, boolean urgent, String tag, long reminderTime, String repeatOption, int estimatedPomodoro) {
        this(id, title, subject, dueAt, priority, note, completed, important, urgent, tag, reminderTime, repeatOption, estimatedPomodoro, "flag", "");
    }

    public StudyTask(String id, String title, String subject, long dueAt, String priority, String note, boolean completed,
                     boolean important, boolean urgent, String tag, long reminderTime, String repeatOption, int estimatedPomodoro,
                     String markerType, String markerValue) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.dueAt = dueAt;
        this.priority = priority;
        this.note = note;
        this.completed = completed;
        this.important = important;
        this.urgent = urgent;
        this.tag = tag == null || tag.trim().isEmpty() ? subject : tag;
        this.reminderTime = reminderTime;
        this.repeatOption = repeatOption == null || repeatOption.trim().isEmpty() ? "Không lặp" : repeatOption;
        this.estimatedPomodoro = Math.max(0, estimatedPomodoro);
        this.markerType = markerType == null || markerType.trim().isEmpty() ? "flag" : markerType;
        this.markerValue = markerValue == null ? "" : markerValue;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getDueAt() {
        return dueAt;
    }

    public void setDueAt(long dueAt) {
        this.dueAt = dueAt;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getTag() {
        return tag == null || tag.trim().isEmpty() ? subject : tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getRepeatOption() {
        return repeatOption;
    }

    public void setRepeatOption(String repeatOption) {
        this.repeatOption = repeatOption;
    }

    public int getEstimatedPomodoro() {
        return estimatedPomodoro;
    }

    public void setEstimatedPomodoro(int estimatedPomodoro) {
        this.estimatedPomodoro = Math.max(0, estimatedPomodoro);
    }

    public String getMarkerType() {
        return markerType == null || markerType.trim().isEmpty() ? "flag" : markerType;
    }

    public void setMarkerType(String markerType) {
        this.markerType = markerType == null || markerType.trim().isEmpty() ? "flag" : markerType;
    }

    public String getMarkerValue() {
        return markerValue == null ? "" : markerValue;
    }

    public void setMarkerValue(String markerValue) {
        this.markerValue = markerValue == null ? "" : markerValue;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("subject", subject);
        json.put("dueAt", dueAt);
        json.put("priority", priority);
        json.put("note", note);
        json.put("completed", completed);
        json.put("important", important);
        json.put("urgent", urgent);
        json.put("tag", getTag());
        json.put("reminderTime", reminderTime);
        json.put("repeatOption", repeatOption);
        json.put("estimatedPomodoro", estimatedPomodoro);
        json.put("markerType", getMarkerType());
        json.put("markerValue", getMarkerValue());
        return json;
    }

    public static StudyTask fromJson(JSONObject json) {
        String subject = json.optString("subject");
        String priority = json.optString("priority", PRIORITY_MEDIUM);
        return new StudyTask(
                json.optString("id"),
                json.optString("title"),
                subject,
                json.optLong("dueAt"),
                priority,
                json.optString("note"),
                json.optBoolean("completed"),
                json.optBoolean("important", PRIORITY_HIGH.equals(priority)),
                json.optBoolean("urgent", false),
                json.optString("tag", subject),
                json.optLong("reminderTime", 0L),
                json.optString("repeatOption", "Không lặp"),
                json.optInt("estimatedPomodoro", 0),
                json.optString("markerType", "flag"),
                json.optString("markerValue", "")
        );
    }
}
