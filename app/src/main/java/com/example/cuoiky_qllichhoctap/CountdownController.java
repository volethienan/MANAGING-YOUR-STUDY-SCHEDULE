package com.example.cuoiky_qllichhoctap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cuoiky_qllichhoctap.model.CountdownMilestone;
import com.example.cuoiky_qllichhoctap.ui.StudyDialogFactory.StudyFormDialog;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class CountdownController {
    static final String FILTER_ALL = "Tất cả";
    static final String FILTER_UPCOMING = "Sắp tới";
    static final String FILTER_PAST = "Đã qua";

    private final MainActivity activity;

    CountdownController(MainActivity activity) {
        this.activity = activity;
    }

    void showCountdown(String filter) {
        View screen = activity.inflateScreen(R.layout.screen_countdown, true, -1);
        setupCountdownFilters(screen, filter);
        List<CountdownItem> items = buildCountdownItems();
        bindCountdownSummary(screen, items);

        LinearLayout list = screen.findViewById(R.id.countdownList);
        list.removeAllViews();
        for (CountdownItem item : items) {
            if (!matchesCountdownFilter(item, filter)) {
                continue;
            }
            list.addView(createCountdownRow(item, filter));
        }
        if (list.getChildCount() == 0) {
            list.addView(activity.emptyState("Chưa có sự kiện đếm ngược phù hợp"));
        }
        screen.findViewById(R.id.btnAddCountdown).setOnClickListener(v -> showCreateCountdownDialog(filter));
    }

    private void setupCountdownFilters(View screen, String active) {
        activity.bindFilter(screen, R.id.countdownFilterAll, FILTER_ALL, active, () -> showCountdown(FILTER_ALL));
        activity.bindFilter(screen, R.id.countdownFilterUpcoming, FILTER_UPCOMING, active, () -> showCountdown(FILTER_UPCOMING));
        activity.bindFilter(screen, R.id.countdownFilterOverdue, FILTER_PAST, active, () -> showCountdown(FILTER_PAST));
    }

    private List<CountdownItem> buildCountdownItems() {
        List<CountdownItem> items = new ArrayList<>();
        for (CountdownMilestone milestone : activity.repository.getCountdownMilestones()) {
            items.add(new CountdownItem(milestone));
        }
        Collections.sort(items, countdownComparator());
        return items;
    }

    private Comparator<CountdownItem> countdownComparator() {
        long today = DateTimeUtils.startOfDay(System.currentTimeMillis());
        return (first, second) -> {
            boolean firstPast = DateTimeUtils.startOfDay(first.targetAt) < today;
            boolean secondPast = DateTimeUtils.startOfDay(second.targetAt) < today;
            if (firstPast != secondPast) {
                return firstPast ? 1 : -1;
            }
            if (firstPast) {
                return Long.compare(second.targetAt, first.targetAt);
            }
            return Long.compare(first.targetAt, second.targetAt);
        };
    }

    private boolean matchesCountdownFilter(CountdownItem item, String filter) {
        long today = DateTimeUtils.startOfDay(System.currentTimeMillis());
        long targetDay = DateTimeUtils.startOfDay(item.targetAt);
        if (FILTER_UPCOMING.equals(filter)) {
            return targetDay >= today;
        }
        if (FILTER_PAST.equals(filter)) {
            return targetDay < today;
        }
        return true;
    }

    private void bindCountdownSummary(View screen, List<CountdownItem> items) {
        int past = 0;
        CountdownItem nearestUpcoming = null;
        long today = DateTimeUtils.startOfDay(System.currentTimeMillis());
        for (CountdownItem item : items) {
            if (DateTimeUtils.startOfDay(item.targetAt) < today) {
                past++;
            } else if (nearestUpcoming == null || item.targetAt < nearestUpcoming.targetAt) {
                nearestUpcoming = item;
            }
        }
        activity.setText(screen, R.id.textCountdownSubtitle, items.isEmpty()
                ? "Cài sự kiện đầu tiên để bắt đầu đếm ngược"
                : items.size() + " sự kiện đang theo dõi · " + past + " đã qua");
        activity.setText(screen, R.id.textCountdownNearest, nearestUpcoming == null
                ? "Gần nhất\nChưa có sự kiện sắp tới"
                : "Gần nhất\n" + countdownBadge(nearestUpcoming) + "\n" + nearestUpcoming.title);
        activity.setText(screen, R.id.textCountdownOverdue, "Đã qua\n" + past + " sự kiện\n" + (past == 0 ? "Sổ vẫn mới" : "Lưu kỷ niệm"));
    }

    private View createCountdownRow(CountdownItem item, String filter) {
        LinearLayout row = new LinearLayout(activity);
        row.setLayoutParams(cardLayoutParams());
        row.setBackgroundResource(countdownBackground(item));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(activity.dp(104));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(activity.dp(16), activity.dp(14), activity.dp(16), activity.dp(14));
        row.setRotation(item.title.hashCode() % 2 == 0 ? -0.8f : 0.8f);

        TextView badge = new TextView(activity);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(activity.dp(92), activity.dp(74));
        badgeParams.setMargins(0, 0, activity.dp(12), 0);
        badge.setLayoutParams(badgeParams);
        boolean past = DateTimeUtils.startOfDay(item.targetAt) < DateTimeUtils.startOfDay(System.currentTimeMillis());
        badge.setBackgroundResource(past ? R.drawable.bg_card_pink : R.drawable.bg_selected_pill);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setText(countdownBadge(item));
        badge.setTextColor(past ? activity.getColor(R.color.danger) : activity.getColor(R.color.ink));
        badge.setTextSize(13f);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setRotation(past ? -1.5f : 1.5f);

        LinearLayout content = new LinearLayout(activity);
        content.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText(item.title);
        title.setTextColor(activity.getColor(R.color.ink));
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView meta = new TextView(activity);
        meta.setText(countdownMeta(item));
        meta.setTextColor(activity.getColor(R.color.muted));
        meta.setTextSize(12f);
        meta.setMaxLines(2);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.setMargins(0, activity.dp(5), 0, 0);
        meta.setLayoutParams(metaParams);

        content.addView(title);
        content.addView(meta);

        LinearLayout actions = new LinearLayout(activity);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(activity.dp(58), ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.setMargins(activity.dp(10), 0, 0, 0);
        actions.setLayoutParams(actionsParams);
        actions.setOrientation(LinearLayout.VERTICAL);

        TextView edit = countdownActionButton("Sửa", R.drawable.bg_action_edit, R.color.ink);
        TextView delete = countdownActionButton("Xóa", R.drawable.bg_action_delete, R.color.rose);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.dp(36));
        deleteParams.setMargins(0, activity.dp(6), 0, 0);
        delete.setLayoutParams(deleteParams);

        edit.setOnClickListener(v -> showCountdownDialog(item.milestone, () -> showCountdown(filter)));
        delete.setOnClickListener(v -> activity.confirmDelete("Xóa sự kiện đếm ngược?", item.title, () -> {
            activity.repository.deleteCountdownMilestone(item.milestone.getId());
            activity.toast("Đã xóa sự kiện đếm ngược");
            showCountdown(filter);
        }));

        actions.addView(edit);
        actions.addView(delete);
        row.addView(badge);
        row.addView(content);
        row.addView(actions);
        row.setOnClickListener(v -> showCountdownActions(item.milestone, () -> showCountdown(filter)));
        return row;
    }

    private TextView countdownActionButton(String text, int backgroundRes, int colorRes) {
        TextView button = new TextView(activity);
        button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.dp(36)));
        button.setBackgroundResource(backgroundRes);
        button.setGravity(android.view.Gravity.CENTER);
        button.setText(text);
        button.setTextColor(activity.getColor(colorRes));
        button.setTextSize(12f);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        return button;
    }

    private LinearLayout.LayoutParams cardLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, activity.dp(12));
        return params;
    }

    private int countdownBackground(CountdownItem item) {
        if (DateTimeUtils.startOfDay(item.targetAt) < DateTimeUtils.startOfDay(System.currentTimeMillis())) {
            return R.drawable.bg_card;
        }
        int choice = Math.abs(item.title.hashCode()) % 4;
        if (choice == 0) return R.drawable.bg_card_lavender;
        if (choice == 1) return R.drawable.bg_card_pink;
        if (choice == 2) return R.drawable.bg_card_mint;
        return R.drawable.bg_card_yellow;
    }

    private String countdownBadge(CountdownItem item) {
        long today = DateTimeUtils.startOfDay(System.currentTimeMillis());
        long targetDay = DateTimeUtils.startOfDay(item.targetAt);
        long days = (targetDay - today) / (24L * 60L * 60L * 1000L);
        if (days < 0) {
            return "Qua\n" + Math.abs(days) + " ngày";
        }
        if (days == 0) {
            return "Hôm nay\nD-day";
        }
        return "Còn\n" + days + " ngày";
    }

    private String countdownMeta(CountdownItem item) {
        return "Ngày đếm ngược · " + DateTimeUtils.formatDate(item.targetAt);
    }

    private void showCreateCountdownDialog(String filter) {
        showCountdownDialog(null, () -> showCountdown(filter));
    }

    private void showCountdownActions(CountdownMilestone milestone, Runnable onChanged) {
        String[] actions = {"Sửa sự kiện", "Xóa sự kiện"};
        new AlertDialog.Builder(activity)
                .setTitle(milestone.getTitle())
                .setMessage(countdownDetailText(milestone))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showCountdownDialog(milestone, onChanged);
                    } else {
                        activity.confirmDelete("Xóa sự kiện đếm ngược?", milestone.getTitle(), () -> {
                            activity.repository.deleteCountdownMilestone(milestone.getId());
                            activity.toast("Đã xóa sự kiện đếm ngược");
                            onChanged.run();
                        });
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showCountdownDialog(CountdownMilestone editingMilestone, Runnable onSaved) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_countdown, null);
        EditText title = view.findViewById(R.id.inputTitle);
        EditText targetDate = view.findViewById(R.id.inputTargetDate);

        long defaultTarget = DateTimeUtils.startOfDay(DateTimeUtils.daysFromNow(7, 0, 0));
        targetDate.setText(DateTimeUtils.formatDate(defaultTarget));
        if (editingMilestone != null) {
            title.setText(editingMilestone.getTitle());
            targetDate.setText(DateTimeUtils.formatDate(editingMilestone.getTargetDate()));
        }
        targetDate.setOnClickListener(v -> activity.pickDate(targetDate));

        StudyFormDialog formDialog = activity.dialogFactory.createStudyFormDialog(
                editingMilestone == null ? "Cài sự kiện đếm ngược" : "Sửa sự kiện đếm ngược",
                view,
                "Lưu");
        Dialog dialog = formDialog.dialog;
        formDialog.positive.setOnClickListener(v -> {
            String titleValue = activity.textOf(title);
            if (TextUtils.isEmpty(titleValue)) {
                activity.toast("Vui lòng nhập tên sự kiện");
                return;
            }
            long target = DateTimeUtils.startOfDay(DateTimeUtils.combineDateAndTime(targetDate.getText().toString(), "00:00", defaultTarget));
            CountdownMilestone milestone = editingMilestone == null
                    ? activity.repository.newCountdownMilestone(titleValue, CountdownMilestone.TYPE_EVENT, target, "")
                    : editingMilestone;
            milestone.setTitle(titleValue);
            milestone.setType(CountdownMilestone.TYPE_EVENT);
            milestone.setTargetDate(target);
            milestone.setNote("");
            activity.repository.saveCountdownMilestone(milestone);
            activity.toast("Đã lưu sự kiện đếm ngược");
            dialog.dismiss();
            onSaved.run();
        });
        dialog.show();
    }

    private String countdownDetailText(CountdownMilestone milestone) {
        return "Ngày: " + DateTimeUtils.formatDate(milestone.getTargetDate())
                + "\n" + countdownBadge(new CountdownItem(milestone)).replace("\n", " ");
    }

    private static class CountdownItem {
        final String title;
        final long targetAt;
        final CountdownMilestone milestone;

        CountdownItem(CountdownMilestone milestone) {
            this.title = milestone.getTitle();
            this.targetAt = milestone.getTargetDate();
            this.milestone = milestone;
        }
    }
}
