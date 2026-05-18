package com.example.cuoiky_qllichhoctap.model;

public class PomodoroSession {
    private String id;
    private String taskId;
    private String subjectTag;
    private String mode; // "focus", "short_break", "long_break"
    private int durationMinutes;
    private int completedMinutes;
    private long startedAt;
    private long endedAt;
    private boolean isCompleted;
    private String soundType;

    public PomodoroSession(String id, String taskId, String subjectTag, String mode, int durationMinutes, int completedMinutes, long startedAt, long endedAt, boolean isCompleted, String soundType) {
        this.id = id;
        this.taskId = taskId;
        this.subjectTag = subjectTag;
        this.mode = mode;
        this.durationMinutes = durationMinutes;
        this.completedMinutes = completedMinutes;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.isCompleted = isCompleted;
        this.soundType = soundType;
    }

    public String getId() { return id; }
    public String getTaskId() { return taskId; }
    public String getSubjectTag() { return subjectTag; }
    public String getMode() { return mode; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getCompletedMinutes() { return completedMinutes; }
    public long getStartedAt() { return startedAt; }
    public long getEndedAt() { return endedAt; }
    public boolean isCompleted() { return isCompleted; }
    public String getSoundType() { return soundType; }
}
