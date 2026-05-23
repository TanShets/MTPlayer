package com.example.mtplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VisualizerView extends View {
    private byte[] bytes;
    private final Paint paint = new Paint();
    private int backgroundColor = Color.parseColor("#F5F5F5");
    private int playedColor = Color.parseColor("#6200EE");
    private int remainingColor = Color.parseColor("#BDBDBD");
    private float progress = 0f; // 0.0 to 1.0
    private OnSeekBarChangeListener listener;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(float progress, boolean fromUser);
    }

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bytes = null;
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        postInvalidate();
    }

    public void setProgress(float progress) {
        this.progress = progress;
        postInvalidate();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float newProgress = event.getX() / getWidth();
            newProgress = Math.max(0, Math.min(1, newProgress));
            if (listener != null) {
                listener.onProgressChanged(newProgress, true);
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                performClick();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        float width = (float) getWidth();
        float height = (float) getHeight();
        float halfHeight = height / 2f;

        // Draw background
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backgroundColor);
        canvas.drawRect(0, 0, width, height, paint);

        if (bytes == null || bytes.length == 0) {
            paint.setColor(remainingColor);
            paint.setStrokeWidth(2f);
            canvas.drawLine(0, halfHeight, width, halfHeight, paint);
            return;
        }

        // Configuration for vertical bars
        float barWidth = 6f;
        float gap = 4f;
        int numBars = (int) (width / (barWidth + gap));
        
        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < numBars; i++) {
            // Map the current bar to a sample in the bytes array
            int byteIndex = (int) ((float) i / numBars * (bytes.length - 1));
            
            // Get amplitude (absolute distance from center)
            int sample = Math.abs((bytes[byteIndex] & 0xFF) - 128);
            
            // Scale the height - make it more prominent but stay within bounds
            float barHeight = Math.max(4f, (sample / 128f) * halfHeight * 0.8f);
            
            float x = i * (barWidth + gap);
            
            // Determine color based on progress
            if (x < width * progress) {
                paint.setColor(playedColor);
            } else {
                paint.setColor(remainingColor);
            }
            
            // Draw bar centered vertically
            canvas.drawRect(x, halfHeight - barHeight, x + barWidth, halfHeight + barHeight, paint);
        }
        
        // Draw a thin progress indicator line
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3f);
        canvas.drawLine(width * progress, 0, width * progress, height, paint);
    }
}
