package com.example.cuoiky_qllichhoctap.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cuoiky_qllichhoctap.R;

public class StudyDialogFactory {
    private final Context context;

    public StudyDialogFactory(Context context) {
        this.context = context;
    }

    public StudyFormDialog createStudyFormDialog(String titleText, View formView, String positiveText) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(context);
        shell.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(context.getColor(R.color.paper_light));
        shell.setPadding(dp(16), dp(16), dp(16), dp(14));

        TextView title = new TextView(context);
        title.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        title.setText(titleText);
        title.setTextColor(context.getColor(R.color.ink));
        title.setTextSize(24f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(dp(4), dp(2), dp(4), dp(12));
        shell.addView(title);

        shell.addView(formView);

        LinearLayout actions = new LinearLayout(context);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48));
        actionsParams.setMargins(0, dp(12), 0, 0);
        actions.setLayoutParams(actionsParams);
        actions.setBaselineAligned(false);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView negative = dialogActionButton("Hủy", R.drawable.bg_outline_pill, R.color.rose);
        TextView positive = dialogActionButton(positiveText, R.drawable.bg_selected_pill, R.color.ink);
        actions.addView(negative);
        actions.addView(positive);
        shell.addView(actions);

        dialog.setContentView(shell);
        dialog.setOnShowListener(shown -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
        negative.setOnClickListener(v -> dialog.dismiss());
        return new StudyFormDialog(dialog, positive, negative);
    }

    public void styleStudyDialog(AlertDialog dialog) {
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(context.getColor(R.color.paper_light)));
        }
        setDialogPanelBackground(dialog, "parentPanel");
        setDialogPanelBackground(dialog, "topPanel");
        setDialogPanelBackground(dialog, "contentPanel");
        setDialogPanelBackground(dialog, "customPanel");
        setDialogPanelBackground(dialog, "buttonPanel");

        TextView title = findAndroidDialogText(dialog, "alertTitle");
        if (title != null) {
            title.setTextColor(context.getColor(R.color.ink));
        }
        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) {
            message.setTextColor(context.getColor(R.color.muted));
        }
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), R.color.accent_blue);
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), R.color.rose);
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), R.color.muted);
    }

    private TextView dialogActionButton(String text, int backgroundRes, int textColorRes) {
        TextView button = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(dp(6), 0, dp(6), 0);
        button.setLayoutParams(params);
        button.setBackgroundResource(backgroundRes);
        button.setGravity(android.view.Gravity.CENTER);
        button.setText(text);
        button.setTextColor(context.getColor(textColorRes));
        button.setTextSize(15f);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        return button;
    }

    private void setDialogPanelBackground(AlertDialog dialog, String idName) {
        int id = context.getResources().getIdentifier(idName, "id", "android");
        if (id == 0) {
            return;
        }
        View panel = dialog.findViewById(id);
        if (panel != null) {
            panel.setBackgroundColor(context.getColor(R.color.paper_light));
        }
    }

    private TextView findAndroidDialogText(AlertDialog dialog, String idName) {
        int id = context.getResources().getIdentifier(idName, "id", "android");
        if (id == 0) {
            return null;
        }
        return dialog.findViewById(id);
    }

    private void styleDialogButton(android.widget.Button button, int colorRes) {
        if (button == null) {
            return;
        }
        button.setTextColor(context.getColor(colorRes));
        button.setBackgroundColor(Color.TRANSPARENT);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static class StudyFormDialog {
        public final Dialog dialog;
        public final TextView positive;
        public final TextView negative;

        public StudyFormDialog(Dialog dialog, TextView positive, TextView negative) {
            this.dialog = dialog;
            this.positive = positive;
            this.negative = negative;
        }
    }
}
