package com.example.cuoiky_qllichhoctap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.example.cuoiky_qllichhoctap.R;

import java.util.Locale;

public class DonutChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] colors = {R.color.mint, R.color.yellow, R.color.rose};
    private String centerText = "0";
    private String centerLabel = "task";
    private String[] labels = {"Hoàn thành", "Đang làm", "Quá hạn"};
    private int[] values = {0, 0, 0};

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setMinimumHeight(dp(220));
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD));
    }

    public void setData(String centerText, String centerLabel, String[] labels, int[] values) {
        this.centerText = centerText;
        this.centerLabel = centerLabel;
        this.labels = labels;
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = Math.max(getHeight(), dp(220));
        int chartSize = Math.min(width - dp(150), height - dp(38));
        if (chartSize < dp(120)) {
            chartSize = Math.min(width, height) - dp(46);
        }
        float left = dp(8);
        float top = (height - chartSize) / 2f;
        RectF oval = new RectF(left, top, left + chartSize, top + chartSize);
        float stroke = Math.max(dp(22), chartSize * 0.18f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color(R.color.paper_light));
        canvas.drawArc(oval, -90, 360, false, paint);

        int total = total(values);
        float start = -90f;
        if (total == 0) {
            paint.setColor(color(R.color.line));
            canvas.drawArc(oval, -90, 360, false, paint);
        } else {
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) {
                    continue;
                }
                paint.setColor(color(colors[i % colors.length]));
                float sweep = values[i] * 360f / total;
                canvas.drawArc(oval, start + 2f, Math.max(2f, sweep - 4f), false, paint);
                start += sweep;
            }
        }

        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(color(R.color.ink));
        textPaint.setTextSize(sp(26));
        canvas.drawText(centerText, oval.centerX(), oval.centerY() - dp(2), textPaint);
        textPaint.setColor(color(R.color.muted));
        textPaint.setTextSize(sp(11));
        canvas.drawText(centerLabel, oval.centerX(), oval.centerY() + dp(18), textPaint);

        drawLegend(canvas, left + chartSize + dp(18), dp(30), width - left - chartSize - dp(28), total);
    }

    private void drawLegend(Canvas canvas, float x, float y, float width, int total) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(sp(12));
        for (int i = 0; i < labels.length; i++) {
            float rowTop = y + i * dp(48);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(colors[i % colors.length]));
            rect.set(x, rowTop, x + dp(14), rowTop + dp(14));
            canvas.drawRoundRect(rect, dp(4), dp(4), paint);

            textPaint.setColor(color(R.color.ink));
            canvas.drawText(labels[i], x + dp(22), rowTop + dp(12), textPaint);
            textPaint.setColor(color(R.color.muted));
            String percent = total == 0 ? "0%" : String.format(Locale.getDefault(), "%d%%", Math.round(values[i] * 100f / total));
            canvas.drawText(values[i] + " · " + percent, x + dp(22), rowTop + dp(30), textPaint);
        }
    }

    private int total(int[] items) {
        int result = 0;
        for (int item : items) {
            result += Math.max(0, item);
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
