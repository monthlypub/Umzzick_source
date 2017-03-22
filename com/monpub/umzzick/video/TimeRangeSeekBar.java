package com.monpub.umzzick.video;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.monpub.umzzick.R;
import com.monpub.umzzick.etc.Util;

/**
 * Created by small-lab on 2017-03-18.
 */

public class TimeRangeSeekBar extends View {
    private int progressColor = Color.DKGRAY;
    private int progressRangeColor = getResources().getColor(R.color.main_item_head);
    private int progressRangeSelectedColor = Color.WHITE;

    private int progressStokeWidth = Util.dp2px(4f);

    private Drawable rangeStartDrawable;
    private Drawable rangeEndDrawable;

    private int max = 1000;
    private int progressFrom = 1000;
    private int progressTo = 0;

    private Rect lastStartRect;
    private Rect lastEndRect;
    private Rect lastProgressRect;

    private OnRangeChangedListner onRangeChangedListner;

    private RangeType lastRangeType = null;
    private boolean initialized = false;

    public TimeRangeSeekBar(Context context) {
        super(context);
        init(context);
    }

    public TimeRangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TimeRangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public TimeRangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        rangeStartDrawable = context.getDrawable(R.drawable.selector_seek_thumb_start);
        rangeEndDrawable = context.getDrawable(R.drawable.selector_seek_thumb_end);

        gestureProgressDetector = new GestureDetector(context, onGestureProgressListener);
        gestureStartDetector = new GestureDetector(context, onGestureStartListener);
        gestureEndDetector = new GestureDetector(context, onGestureEndListener);
    }

    public void init(int from, int to, int max) {
        initialized = true;
        setMax(max);
        setProgressTo(to);
        setProgressFrom(from);

        postInvalidate();
    }

    public void stepPrev(int amount) {
        if (lastRangeType == null) {
            return;
        }

        switch (lastRangeType) {
            case FROM:
                setProgressFrom(progressFrom - amount);
                break;
            case TO:
                setProgressTo(progressTo - amount);
                break;
            case BOTH:
                int availableAmount = Math.min(progressFrom, amount);

                setProgressFrom(progressFrom - availableAmount);
                setProgressTo(progressTo - availableAmount);
                break;
        }

        if (onRangeChangedListner != null) {
            onRangeChangedListner.onRangeChanged(lastRangeType);
        }

        postInvalidate();
    }

    public void stepNext(int amount) {
        if (lastRangeType == null) {
            return;
        }

        switch (lastRangeType) {
            case FROM:
                setProgressFrom(progressFrom + amount);
                break;
            case TO:
                setProgressTo(progressTo + amount);
                break;
            case BOTH:
                int availableAmount = Math.min(max - progressTo, amount);

                setProgressFrom(progressFrom + availableAmount);
                setProgressTo(progressTo + availableAmount);
                break;
        }

        if (onRangeChangedListner != null) {
            onRangeChangedListner.onRangeChanged(lastRangeType);
        }

        postInvalidate();
    }

    public void notifyRangeChanged() {
        if (onRangeChangedListner != null) {
            onRangeChangedListner.onRangeChanged(RangeType.BOTH);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int from = rangeStartDrawable.getIntrinsicWidth();
        int to = width - rangeEndDrawable.getIntrinsicWidth();

        int progressFrom = from + (int) ((to - from) * getProgressFromRatio());
        int progressTo = from + (int) ((to - from) * getProgressToRatio());

        Paint paint = new Paint();
        paint.setColor(progressColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(progressStokeWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);

        canvas.drawLine(from, height / 2, to, height / 2, paint);

        if (initialized == false) {
            return;
        }

        paint.setColor(lastRangeType == RangeType.BOTH ? progressRangeSelectedColor : progressRangeColor);

        lastProgressRect = new Rect(
                progressFrom,
                0,
                progressTo,
                getHeight());

        canvas.drawLine(progressFrom, height / 2, progressTo, height / 2, paint);

        lastStartRect = new Rect(
                progressFrom - rangeEndDrawable.getIntrinsicWidth(),
                0,
                progressFrom,
                rangeStartDrawable.getIntrinsicHeight());

        // draw start
        rangeStartDrawable.setBounds(lastStartRect);
        rangeStartDrawable.draw(canvas);

        lastEndRect = new Rect(
                progressTo,
                0,
                progressTo + rangeEndDrawable.getIntrinsicWidth(),
                rangeEndDrawable.getIntrinsicHeight());

        // draw end
        rangeEndDrawable.setBounds(lastEndRect);
        rangeEndDrawable.draw(canvas);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), Util.dp2px(36f));
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getProgressFrom() {
        return progressFrom;
    }

    public void setProgressFrom(int progressFrom) {
        this.progressFrom = progressFrom;
        if (this.progressFrom < 0) {
            this.progressFrom = 0;
        }
        if (this.progressFrom >= progressTo) {
            this.progressFrom = progressTo - 1;
        }
    }

    public int getProgressTo() {
        return progressTo;
    }

    public void setProgressTo(int progressTo) {
        this.progressTo = progressTo;
        if (this.progressTo <= progressFrom) {
            this.progressTo = progressFrom + 1;
        }
        if (this.progressTo > max) {
            this.progressTo = max;
        }

        if (onRangeChangedListner != null) {
            onRangeChangedListner.onRangeChanged(RangeType.TO);
        }
    }

    private float getProgressFromRatio() {
        return (float) progressFrom / max;
    }

    private void setProgressFromRatio(float ratio) {
        progressFrom = (int) (max * ratio);
    }

    private float getProgressToRatio() {
        return (float) progressTo / max;
    }

    private void setProgressToRatio(float ratio) {
        progressTo = (int) (max * ratio);
    }

    public OnRangeChangedListner getOnRangeChangedListner() {
        return onRangeChangedListner;
    }

    public void setOnRangeChangedListner(OnRangeChangedListner onRangeChangedListner) {
        this.onRangeChangedListner = onRangeChangedListner;
    }

    private GestureDetector currGestureDetector;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (lastProgressRect.contains(x, y) == true) {
                currGestureDetector = gestureProgressDetector;
            } else if (lastStartRect.contains(x, y) == true) {
                currGestureDetector = gestureStartDetector;
            } else if (lastEndRect.contains(x, y) == true) {
                currGestureDetector = gestureEndDetector;
            } else {
                return false;
            }

            currGestureDetector.onTouchEvent(event);
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (currGestureDetector != null) {
                currGestureDetector.onTouchEvent(event);
                currGestureDetector = null;
                return true;
            }
        }

        if (currGestureDetector != null) {
            currGestureDetector.onTouchEvent(event);
            return true;
        }

        return false;
    }

    private GestureDetector gestureStartDetector;
    private GestureDetector gestureProgressDetector;
    private GestureDetector gestureEndDetector;

    private GestureDetector.SimpleOnGestureListener onGestureStartListener = new GestureDetector.SimpleOnGestureListener() {
        private float downRatio;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int totalProgressFrom =  rangeStartDrawable.getIntrinsicWidth();
            int totalProgressTo = getWidth() - rangeEndDrawable.getIntrinsicWidth();
            float diffRatio = (float) (e2.getX() - e1.getX()) / (totalProgressTo - totalProgressFrom);

            float progressRatio = downRatio + diffRatio;

             if (progressRatio < 0f) {
                setProgressFromRatio(0f);
            } else if (progressRatio > getProgressToRatio()) {
                setProgressFrom(getProgressTo() - 1);
            } else {
                setProgressFromRatio(progressRatio);
            }

            postInvalidate();

            if (onRangeChangedListner != null) {
                onRangeChangedListner.onRangeChanged(RangeType.FROM);
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            downRatio = getProgressFromRatio();

            setSelected(rangeStartDrawable, true);
            setSelected(rangeEndDrawable, false);
            postInvalidate();
            lastRangeType = RangeType.FROM;
            return true;
        }
    };
    private GestureDetector.SimpleOnGestureListener onGestureEndListener = new GestureDetector.SimpleOnGestureListener() {
        private float downRatio;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int totalProgressFrom =  rangeStartDrawable.getIntrinsicWidth();
            int totalProgressTo = getWidth() - rangeEndDrawable.getIntrinsicWidth();
            float diffRatio = (float) (e2.getX() - e1.getX()) / (totalProgressTo - totalProgressFrom);

            float progressRatio = downRatio + diffRatio;

            if (progressRatio < getProgressFromRatio()) {
            } else if (progressRatio > 1f) {
                setProgressToRatio(1f);
            } else {
                setProgressToRatio(progressRatio);
            }

            postInvalidate();

            if (onRangeChangedListner != null) {
                onRangeChangedListner.onRangeChanged(RangeType.TO);
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            downRatio = getProgressToRatio();
            setSelected(rangeStartDrawable, false);
            setSelected(rangeEndDrawable, true);
            postInvalidate();
            lastRangeType = RangeType.TO;
            return true;
        }
    };

    private GestureDetector.SimpleOnGestureListener onGestureProgressListener = new GestureDetector.SimpleOnGestureListener() {
        private float downFromRatio, downToRatio;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int totalProgressFrom =  rangeStartDrawable.getIntrinsicWidth();
            int totalProgressTo = getWidth() - rangeEndDrawable.getIntrinsicWidth();
            float diffRatio = (float) (e2.getX() - e1.getX()) / (totalProgressTo - totalProgressFrom);

            if (diffRatio < -downFromRatio) {
                diffRatio = -downFromRatio;
            }
            if (diffRatio > 1 - downToRatio) {
                diffRatio = 1 - downToRatio;
            }

            float progressFromRatio = downFromRatio + diffRatio;
            float progressToRatio = downToRatio + diffRatio;

            setProgressFromRatio(progressFromRatio);
            setProgressToRatio(progressToRatio);

            postInvalidate();

            if (onRangeChangedListner != null) {
                onRangeChangedListner.onRangeChanged(RangeType.BOTH);
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            downFromRatio = getProgressFromRatio();
            downToRatio = getProgressToRatio();

            setSelected(rangeStartDrawable, true);
            setSelected(rangeEndDrawable, true);
            postInvalidate();
            lastRangeType = RangeType.BOTH;

            return true;
        }
    };

    public interface OnRangeChangedListner {
        public void onRangeChanged(RangeType type);
    }

    public enum RangeType {
        FROM, TO, BOTH
    }

    private boolean isSelected(Drawable drawable) {
        int[] states = drawable.getState();
        for (int state : states) {
            if (state == android.R.attr.state_selected) {
                return true;
            }
        }

        return false;
    }

    private void setSelected(Drawable drawable, boolean selected) {
        drawable.setState(selected == true ? new int[]{android.R.attr.state_selected} : new int[]{});
    }
}
