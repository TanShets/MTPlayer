package com.example.mtplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VisualizerView extends View {
    private byte[] bytes;
    private float[] points;
    private final Rect rect = new Rect();
    private final Paint paint = new Paint();

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
        paint.setStrokeWidth(4f);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#6200EE")); // Primary color
        paint.setStyle(Paint.Style.STROKE);
    }

    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null) {
            return;
        }

        if (points == null || points.length < bytes.length * 4) {
            points = new float[bytes.length * 4];
        }

        rect.set(0, 0, getWidth(), getHeight());
        float width = (float) rect.width();
        float height = (float) rect.height();
        float halfHeight = height / 2f;

        for (int i = 0; i < bytes.length - 1; i++) {
            points[i * 4] = width * i / (bytes.length - 1);
            points[i * 4 + 1] = halfHeight
                    + ((byte) (bytes[i] + 128)) * (halfHeight) / 128;
            points[i * 4 + 2] = width * (i + 1) / (bytes.length - 1);
            points[i * 4 + 3] = halfHeight
                    + ((byte) (bytes[i + 1] + 128)) * (halfHeight) / 128;
        }

        canvas.drawLines(points, paint);
    }
}
