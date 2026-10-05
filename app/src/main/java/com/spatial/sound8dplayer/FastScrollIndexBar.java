package com.spatial.sound8dplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class FastScrollIndexBar extends View {

    public interface OnLetterTouchListener {
        void onLetterTouch(String letter, int index);
    }

    private static final String[] ALPHABETS = {
            "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
    };

    private Paint textPaint;
    private Paint highlightPaint;
    private int selectedIndex = -1;
    private OnLetterTouchListener listener;

    public FastScrollIndexBar(Context context) {
        super(context);
        init();
    }

    public FastScrollIndexBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FastScrollIndexBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#9CA3AF"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(24f);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#00E5FF"));
        highlightPaint.setTextAlign(Paint.Align.CENTER);
        highlightPaint.setTextSize(28f);
        highlightPaint.setFakeBoldText(true);
    }

    public void setOnLetterTouchListener(OnLetterTouchListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int width = getWidth();
        if (height <= 0 || width <= 0) return;

        float singleHeight = (float) height / ALPHABETS.length;
        for (int i = 0; i < ALPHABETS.length; i++) {
            float xPos = width / 2f;
            float yPos = singleHeight * i + singleHeight * 0.75f;
            if (i == selectedIndex) {
                canvas.drawText(ALPHABETS[i], xPos, yPos, highlightPaint);
            } else {
                canvas.drawText(ALPHABETS[i], xPos, yPos, textPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float y = event.getY();
        int oldSelected = selectedIndex;
        int currentPos = (int) (y / getHeight() * ALPHABETS.length);

        if (currentPos < 0) currentPos = 0;
        if (currentPos >= ALPHABETS.length) currentPos = ALPHABETS.length - 1;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                selectedIndex = currentPos;
                if (oldSelected != currentPos && listener != null) {
                    listener.onLetterTouch(ALPHABETS[currentPos], currentPos);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                selectedIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
