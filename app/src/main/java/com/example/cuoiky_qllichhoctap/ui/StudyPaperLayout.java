package com.example.cuoiky_qllichhoctap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.example.cuoiky_qllichhoctap.R;

public class StudyPaperLayout extends LinearLayout {
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StudyPaperLayout(Context context) {
        super(context);
        init();
    }

    public StudyPaperLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StudyPaperLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        backgroundPaint.setColor(getResources().getColor(R.color.paper, getContext().getTheme()));
        dotPaint.setColor(getResources().getColor(R.color.dot_grid, getContext().getTheme()));
        dotPaint.setAlpha(90);
        linePaint.setColor(getResources().getColor(R.color.line, getContext().getTheme()));
        linePaint.setAlpha(55);
        linePaint.setStrokeWidth(dp(1));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        float dotGap = dp(18);
        float dotRadius = dp(0.9f);
        for (float x = dotGap; x < getWidth(); x += dotGap) {
            for (float y = dotGap; y < getHeight(); y += dotGap) {
                canvas.drawCircle(x, y, dotRadius, dotPaint);
            }
        }

        float lineGap = dp(38);
        for (float y = dp(92); y < getHeight(); y += lineGap) {
            canvas.drawLine(dp(22), y, getWidth() - dp(22), y, linePaint);
        }
        super.onDraw(canvas);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
