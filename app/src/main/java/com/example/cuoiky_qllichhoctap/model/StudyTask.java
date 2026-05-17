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

    public StudyTask(String id, String title, String subject, long dueAt, String priority, String note, boolean completed) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.dueAt = dueAt;
        this.priority = priority;
        this.note = note;
        this.completed = completed;
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

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("subject", subject);
        json.put("dueAt", dueAt);
        json.put("priority", priority);
        json.put("note", note);
        json.put("completed", completed);
        return json;
    }

    public static StudyTask fromJson(JSONObject json) {
        return new StudyTask(
                json.optString("id"),
                json.optString("title"),
                json.optString("subject"),
                json.optLong("dueAt"),
                json.optString("priority", PRIORITY_MEDIUM),
                json.optString("note"),
                json.optBoolean("completed")
        );
    }
}
