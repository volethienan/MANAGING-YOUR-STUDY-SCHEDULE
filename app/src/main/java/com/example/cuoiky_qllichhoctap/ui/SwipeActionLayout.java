package com.example.cuoiky_qllichhoctap.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.example.cuoiky_qllichhoctap.R;

public class SwipeActionLayout extends FrameLayout {
    private View foreground;
    private View actions;
    private float downX;
    private float downY;
    private float startTranslationX;
    private int touchSlop;
    private int actionWidth;
    private boolean dragging;
    private boolean swipeEnabled = true;

    public SwipeActionLayout(Context context) {
        super(context);
        init();
    }

    public SwipeActionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        foreground = findViewById(R.id.taskForeground);
        actions = findViewById(R.id.taskActions);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!swipeEnabled || foreground == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                startTranslationX = foreground.getTranslationX();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!swipeEnabled || foreground == null) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                startTranslationX = foreground.getTranslationX();
                return true;
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                float dx = event.getX() - downX;
                foreground.setTranslationX(clamp(startTranslationX + dx, -getActionWidth(), 0));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                settle();
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnabled = enabled;
        if (!enabled) {
            close(false);
        }
    }

    public boolean isOpen() {
        return foreground != null && foreground.getTranslationX() < -getActionWidth() * 0.35f;
    }

    public void close() {
        close(true);
    }

    private void close(boolean animate) {
        moveTo(0, animate);
    }

    private void settle() {
        int width = getActionWidth();
        boolean shouldOpen = Math.abs(foreground.getTranslationX()) > width * 0.45f;
        moveTo(shouldOpen ? -width : 0, true);
    }

    private void moveTo(float target, boolean animate) {
        if (foreground == null) {
            return;
        }
        if (animate) {
            foreground.animate().translationX(target).setDuration(180).start();
        } else {
            foreground.setTranslationX(target);
        }
    }

    private int getActionWidth() {
        if (actionWidth <= 0 && actions != null) {
            actionWidth = actions.getWidth();
        }
        if (actionWidth <= 0) {
            actionWidth = dp(232);
        }
        return actionWidth;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
