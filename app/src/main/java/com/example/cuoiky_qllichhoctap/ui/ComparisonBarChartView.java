package com.example.cuoiky_qllichhoctap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.example.cuoiky_qllichhoctap.R;

public class ComparisonBarChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] colors = {R.color.mint, R.color.yellow, R.color.pink, R.color.blue};
    private String[] labels = {"Xong", "Còn lại"};
    private int[] values = {0, 0};
    private String caption = "Số việc hôm nay";

    public ComparisonBarChartView(Context context) {
        super(context);
        init();
    }

    public ComparisonBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setMinimumHeight(dp(230));
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
    }

    public void setData(String caption, String[] labels, int[] values) {
        this.caption = caption;
        this.labels = labels;
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = Math.max(getHeight(), dp(230));
        int left = dp(34);
        int right = width - dp(12);
        int top = dp(34);
        int bottom = height - dp(46);
        int max = Math.max(1, max(values));

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(sp(13));
        textPaint.setColor(color(R.color.ink));
        canvas.drawText(caption, dp(2), dp(18), textPaint);

        paint.setStrokeWidth(dp(1));
        paint.setColor(color(R.color.line));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(sp(10));
        textPaint.setColor(color(R.color.muted));
        for (int i = 0; i <= 4; i++) {
            float y = bottom - (bottom - top) * i / 4f;
            int axisValue = Math.round(max * i / 4f);
            canvas.drawLine(left, y, right, y, paint);
            canvas.drawText(String.valueOf(axisValue), left - dp(8), y + dp(4), textPaint);
        }
        canvas.drawLine(left, top, left, bottom, paint);

        int count = Math.max(1, values.length);
        float slot = (right - left) / (float) count;
        float barWidth = Math.min(dp(38), slot * 0.44f);
        for (int i = 0; i < count; i++) {
            float center = left + slot * i + slot / 2f;
            float barHeight = (bottom - top) * Math.max(0, values[i]) / (float) max;
            float barTop = bottom - Math.max(dp(8), barHeight);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(colors[i % colors.length]));
            rect.set(center - barWidth / 2f, barTop, center + barWidth / 2f, bottom);
            canvas.drawRoundRect(rect, dp(10), dp(10), paint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(sp(12));
            textPaint.setColor(color(R.color.ink));
            canvas.drawText(String.valueOf(values[i]), center, barTop - dp(8), textPaint);
            textPaint.setTextSize(sp(11));
            textPaint.setColor(color(R.color.muted));
            canvas.drawText(labels[i], center, bottom + dp(22), textPaint);
        }
    }

    private int max(int[] items) {
        int result = 0;
        for (int item : items) {
            result = Math.max(result, item);
        }
        return result;
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
}
