package com.example.cuoiky_qllichhoctap;

import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void task_preservesCalendarSyncFlag() {
        StudyTask task = new StudyTask("task-1", "Nộp báo cáo", "Mobile", 1_800_000L,
                StudyTask.PRIORITY_HIGH, "Nộp PDF", false);
        task.setShowOnCalendar(true);
        task.setReminderTime(900_000L);

        assertTrue(task.isShowOnCalendar());
        assertEquals("task-1", task.getId());
        assertEquals(900_000L, task.getReminderTime());
    }

    @Test
    public void event_preservesSourceTaskId() {
        StudyEvent event = new StudyEvent("event-1", "Nộp báo cáo", StudyEvent.TYPE_DEADLINE,
                "Mobile", 1_800_000L, 3_600_000L, "", "Nộp trước 22h", true, 15, "task-1");

        assertEquals("task-1", event.getSourceTaskId());
        assertEquals(StudyEvent.TYPE_DEADLINE, event.getType());
        assertTrue(event.isReminderEnabled());
    }

    @Test
    public void event_normalizesLegacyTypeCodes() {
        assertEquals(StudyEvent.TYPE_STUDY, new StudyEvent("1", "Học", "study", "", 0, 1, "", "").getType());
        assertEquals(StudyEvent.TYPE_EXAM, new StudyEvent("2", "Thi", "exam", "", 0, 1, "", "").getType());
        assertEquals(StudyEvent.TYPE_DEADLINE, new StudyEvent("3", "Nộp bài", "deadline", "", 0, 1, "", "").getType());
        assertEquals(StudyEvent.TYPE_PERSONAL, new StudyEvent("4", "Cá nhân", "personal", "", 0, 1, "", "").getType());
    }
}
