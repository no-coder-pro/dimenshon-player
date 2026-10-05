package com.spatial.sound8dplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SpatialVisualizerView extends View {

    private Paint ringPaint;
    private Paint secondaryRingPaint;
    private Paint headPaint;
    private Paint headphoneBandPaint;
    private Paint earLeftPaint;
    private Paint earRightPaint;
    private Paint soundOrbMainPaint;
    private Paint soundOrbSecondaryPaint;
    private Paint orbGlowPaint;
    private Paint pulsePaint;
    private Paint labelPaint;

    private float angle = 0f;
    private boolean isPlaying = false;
    private Spatial8DEngine.AudioMode mode = Spatial8DEngine.AudioMode.MODE_8D;
    private float pulseRadius = 0f;

    public SpatialVisualizerView(Context context) {
        super(context);
        init();
    }

    public SpatialVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpatialVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#3D4566"));
        ringPaint.setStrokeWidth(3.5f);

        secondaryRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondaryRingPaint.setStyle(Paint.Style.STROKE);
        secondaryRingPaint.setColor(Color.parseColor("#21253B"));
        secondaryRingPaint.setStrokeWidth(2f);

        headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headPaint.setStyle(Paint.Style.FILL);
        headPaint.setColor(Color.parseColor("#1B1E2E"));

        headphoneBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headphoneBandPaint.setStyle(Paint.Style.STROKE);
        headphoneBandPaint.setColor(Color.parseColor("#8A2BE2"));
        headphoneBandPaint.setStrokeWidth(5f);
        headphoneBandPaint.setStrokeCap(Paint.Cap.ROUND);

        earLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        earLeftPaint.setStyle(Paint.Style.FILL);
        earLeftPaint.setColor(Color.parseColor("#00E5FF"));

        earRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        earRightPaint.setStyle(Paint.Style.FILL);
        earRightPaint.setColor(Color.parseColor("#FF007F"));

        soundOrbMainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        soundOrbMainPaint.setStyle(Paint.Style.FILL);
        soundOrbMainPaint.setColor(Color.parseColor("#00E5FF"));

        soundOrbSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        soundOrbSecondaryPaint.setStyle(Paint.Style.FILL);
        soundOrbSecondaryPaint.setColor(Color.parseColor("#FF007F"));

        orbGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orbGlowPaint.setStyle(Paint.Style.FILL);

        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
    }

    public void updateAngle(float angle, boolean isPlaying, Spatial8DEngine.AudioMode mode) {
        this.angle = angle;
        this.isPlaying = isPlaying;
        this.mode = mode;

        if (isPlaying) {
            pulseRadius += 1.5f;
            if (pulseRadius > 50f) {
                pulseRadius = 0f;
            }
        } else {
            pulseRadius = 0f;
        }

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        if (cx <= 0 || cy <= 0) return;

        float rx = cx * 0.78f;
        float ry = cy * 0.62f;

        RectF outerOrbitRect = new RectF(cx - rx, cy - ry, cx + rx, cy + ry);
        canvas.drawOval(outerOrbitRect, ringPaint);

        RectF innerOrbitRect = new RectF(cx - rx * 0.6f, cy - ry * 0.6f, cx + rx * 0.6f, cy + ry * 0.6f);
        canvas.drawOval(innerOrbitRect, secondaryRingPaint);

        canvas.drawLine(cx - rx - 15f, cy, cx + rx + 15f, cy, secondaryRingPaint);
        canvas.drawLine(cx, cy - ry - 15f, cx, cy + ry + 15f, secondaryRingPaint);

        if (isPlaying && pulseRadius > 0) {
            int alpha = (int) (200 * (1f - (pulseRadius / 50f)));
            pulsePaint.setColor(Color.argb(Math.max(0, alpha), 138, 43, 226));
            canvas.drawCircle(cx, cy, (rx * 0.28f) + pulseRadius, pulsePaint);
        }

        float cosA = (float) Math.cos(angle);
        float sinA = (float) Math.sin(angle);

        float orbX = cx + rx * cosA;
        float orbY = cy + ry * sinA;

        float zDepth = 0.7f + 0.5f * ((sinA + 1.0f) / 2.0f);
        float mainOrbSize = (isPlaying ? 16f : 12f) * zDepth;

        if (sinA < 0 && mode != Spatial8DEngine.AudioMode.OFF) {
            drawOrb(canvas, orbX, orbY, mainOrbSize, soundOrbMainPaint, 150);
        }

        float headR = rx * 0.26f;
        canvas.drawCircle(cx, cy, headR, headPaint);

        ringPaint.setColor(Color.parseColor("#4B5563"));
        canvas.drawCircle(cx, cy, headR, ringPaint);
        ringPaint.setColor(Color.parseColor("#3D4566"));

        RectF bandRect = new RectF(cx - headR - 6f, cy - headR - 10f, cx + headR + 6f, cy + headR);
        canvas.drawArc(bandRect, 190, 160, false, headphoneBandPaint);

        float earW = 10f;
        float earH = headR * 0.9f;
        RectF leftEarRect = new RectF(cx - headR - earW, cy - earH / 2f, cx - headR + 2f, cy + earH / 2f);
        canvas.drawRoundRect(leftEarRect, 6f, 6f, earLeftPaint);

        RectF rightEarRect = new RectF(cx + headR - 2f, cy - earH / 2f, cx + headR + earW, cy + earH / 2f);
        canvas.drawRoundRect(rightEarRect, 6f, 6f, earRightPaint);

        labelPaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawText("L", cx - headR - 16f, cy + 8f, labelPaint);

        labelPaint.setColor(Color.parseColor("#FF007F"));
        canvas.drawText("R", cx + headR + 16f, cy + 8f, labelPaint);

        labelPaint.setColor(Color.parseColor("#D1D5DB"));
        labelPaint.setTextSize(18f);
        canvas.drawText("HEAD", cx, cy + 6f, labelPaint);
        labelPaint.setTextSize(22f);

        if (mode == Spatial8DEngine.AudioMode.OFF) {
            drawOrb(canvas, cx - rx * 0.75f, cy, 12f, soundOrbMainPaint, 220);
            drawOrb(canvas, cx + rx * 0.75f, cy, 12f, soundOrbSecondaryPaint, 220);
            return;
        }

        if (sinA >= 0) {
            drawOrb(canvas, orbX, orbY, mainOrbSize, soundOrbMainPaint, 255);
        }

        if (mode == Spatial8DEngine.AudioMode.MODE_16D) {
            float cos2 = (float) Math.cos(-angle * 1.8f);
            float sin2 = (float) Math.sin(-angle * 1.8f);
            float orb2X = cx + (rx * 0.85f) * cos2;
            float orb2Y = cy + (ry * 0.85f) * sin2;
            float z2 = 0.7f + 0.5f * ((sin2 + 1.0f) / 2.0f);
            drawOrb(canvas, orb2X, orb2Y, 13f * z2, soundOrbSecondaryPaint, 240);
        } else if (mode == Spatial8DEngine.AudioMode.MODE_8D) {
            float cosTrail = (float) Math.cos(angle - 0.4f);
            float sinTrail = (float) Math.sin(angle - 0.4f);
            float trailX = cx + rx * cosTrail;
            float trailY = cy + ry * sinTrail;
            drawOrb(canvas, trailX, trailY, mainOrbSize * 0.6f, soundOrbSecondaryPaint, 120);
        }
    }

    private void drawOrb(Canvas canvas, float x, float y, float size, Paint paint, int alpha) {
        orbGlowPaint.setColor(paint.getColor());
        orbGlowPaint.setAlpha(alpha / 3);
        canvas.drawCircle(x, y, size * 1.8f, orbGlowPaint);

        paint.setAlpha(alpha);
        canvas.drawCircle(x, y, size, paint);

        orbGlowPaint.setColor(Color.WHITE);
        orbGlowPaint.setAlpha(Math.min(255, alpha + 30));
        canvas.drawCircle(x, y, size * 0.35f, orbGlowPaint);
    }
}
