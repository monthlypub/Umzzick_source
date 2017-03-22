package com.monpub.umzzick.video;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.monpub.umzzick.R;
import com.monpub.umzzick.etc.Util;

/**
 * Created by small-lab on 2017-03-05.
 */

public class SelectionView extends View implements View.OnTouchListener {
    private Rect focusBox;

    private GestureDetector gestureBoxDetector;
    private GestureDetector gestureOutlineDetector;

    private GestureDetector currnetGestureDetector;

    private BoxType boxType = BoxType.FREE;
    private boolean videoReady = false;

    private OnSelectionChangedListener onSelectionChangedListener;

    private Rect lastRect;

    public SelectionView(Context context) {
        super(context);
        init(context);
    }

    public SelectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public SelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        setOnTouchListener(this);

        gestureBoxDetector = new GestureDetector(context, boxGestureListener);
        gestureOutlineDetector = new GestureDetector(context, outlineGestureListener);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        onSelectionChangedListener = listener;
    }

    public void videoReady() {
        this.videoReady = true;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        setClickable(false);
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        super.setOnLongClickListener(l);
        setLongClickable(false);
    }

    private Drawable selectionDrawable;
    private Drawable cropDrawable;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (selectionDrawable == null) {
            selectionDrawable = getResources().getDrawable(R.drawable.shape_selection, null);
        }
        if (cropDrawable == null) {
            cropDrawable = getResources().getDrawable(R.drawable.shape_crop, null);
        }

        if (focusBox == null) {
            return;
        }

        canvas.drawColor(0x33000000);

        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(focusBox, paint);

        selectionDrawable.setBounds(focusBox);
        selectionDrawable.draw(canvas);

        int cropWidth = cropDrawable.getIntrinsicWidth();
        int cropHeight = cropDrawable.getIntrinsicHeight();
        cropDrawable.setBounds(new Rect(
                focusBox.right - cropWidth,
                focusBox.bottom - cropHeight,
                focusBox.right,
                focusBox.bottom
        ));
        cropDrawable.draw(canvas);
    }

    public RectF getSelection() {
        int width = getWidth();
        int height = getHeight();

        if (focusBox == null) {
            return null;
        }

        return new RectF(
                (float) focusBox.left / width,
                (float) focusBox.top / height,
                (float) focusBox.right / width,
                (float) focusBox.bottom / height
            );
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        int x = (int) event.getX();
        int y = (int) event.getY();



        if (action == MotionEvent.ACTION_DOWN) {

            Rect outlineRect = new Rect(
                    focusBox.right - cropDrawable.getIntrinsicWidth(),
                    focusBox.bottom - cropDrawable.getIntrinsicHeight(),
                    focusBox.right,
                    focusBox.bottom
            );

            if (outlineRect.contains(x, y) == true) {
                currnetGestureDetector = gestureOutlineDetector;
            } else if (focusBox.contains(x, y) == true) {
                currnetGestureDetector = gestureBoxDetector;
            } else {
                currnetGestureDetector = null;
            }

            if (currnetGestureDetector == null) {
                return false;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            currnetGestureDetector.onTouchEvent(event);
            currnetGestureDetector = null;

            if (focusBox != null && lastRect != null) {
                if (focusBox.equals(lastRect) == false) {
                    if (onSelectionChangedListener != null) {
                        onSelectionChangedListener.onSelectionChanged();
                    }
                 }
            }

            return true;
        }

        if (currnetGestureDetector != null) {
            currnetGestureDetector.onTouchEvent(event);
        }

        return true;
    }

    private GestureDetector.SimpleOnGestureListener boxGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            setClickable(true);
            performClick();
            setClickable(false);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            focusBox.offset((int) -distanceX, (int) -distanceY);

            if (focusBox.left < 0) {
                focusBox.offset(-focusBox.left, 0);
            }
            if (focusBox.right > getWidth()) {
                focusBox.offset(getWidth() - focusBox.right, 0);
            }
            if (focusBox.top < 0) {
                focusBox.offset(0, -focusBox.top);
            }
            if (focusBox.bottom > getHeight()) {
                focusBox.offset(0, getHeight() - focusBox.bottom);
            }

            postInvalidate();

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            lastRect = focusBox == null ? null : new Rect(focusBox);
            return true;
        }
    };

    @Override
    public void layout(@Px int l, @Px int t, @Px int r, @Px int b) {
        super.layout(l, t, r, b);

        if (videoReady == false) {
            return;
        }

        focusBox = new Rect(
                0,
                0,
                r - l,
                b - t
        );

        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged();
        }
    }

    private static final int MIN_SIZE = Util.dp2px(40f);

    private GestureDetector.SimpleOnGestureListener outlineGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            if (boxType == BoxType.SQUARE) {
                focusBox.right -= distanceX;
                focusBox.bottom -= distanceX;
            } else if (boxType == BoxType.FREE) {
                focusBox.right -= distanceX;
                focusBox.bottom -= distanceY;
            }

            if (focusBox.width() < MIN_SIZE) {
                focusBox.right = focusBox.left + MIN_SIZE;
            }

            if (focusBox.height() < MIN_SIZE) {
                focusBox.bottom = focusBox.top + MIN_SIZE;
            }

            if (focusBox.right > getWidth()) {
                focusBox.right = getWidth();
            }

            if (focusBox.bottom > getHeight()) {
                focusBox.bottom = getHeight();
            }


            postInvalidate();

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);

            setLongClickable(true);
            performLongClick();
            setLongClickable(false);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    public BoxType getBoxType() {
        return boxType;
    }

    public void setBoxType(BoxType boxType) {
        if (this.boxType == BoxType.FREE && boxType == BoxType.SQUARE) {
            if (focusBox.width() > focusBox.height()) {
                focusBox.right = focusBox.left + focusBox.height();
            } else if (focusBox.width() < focusBox.height()) {
                focusBox.bottom = focusBox.top + focusBox.width();
            }

            postInvalidate();
        }

        this.boxType = boxType;
    }

    public enum BoxType {
        SQUARE,
        FREE
    }

    public interface OnSelectionChangedListener {
        public void onSelectionChanged();
    }
}
