package com.example.skincancerai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlayView extends View {

    private final Paint framePaint = new Paint();
    private final Paint cornerPaint = new Paint();
    private final Paint dimPaint = new Paint();
    private RectF focusRect;

    public CameraOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        framePaint.setColor(0xFF60A5FA);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(4f);
        framePaint.setAntiAlias(true);

        cornerPaint.setColor(0xFF2563EB);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(8f);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setAntiAlias(true);

        dimPaint.setColor(0xAA000000);
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) * 2 / 3;

        int left = (width - size) / 2;
        int top = (height - size) / 2;
        focusRect = new RectF(left, top, left + size, top + size);

        // Darken area outside focus square.
        canvas.drawRect(0, 0, width, focusRect.top, dimPaint);
        canvas.drawRect(0, focusRect.top, focusRect.left, focusRect.bottom, dimPaint);
        canvas.drawRect(focusRect.right, focusRect.top, width, focusRect.bottom, dimPaint);
        canvas.drawRect(0, focusRect.bottom, width, height, dimPaint);

        float radius = 20f;
        canvas.drawRoundRect(focusRect, radius, radius, framePaint);
        drawCornerGuides(canvas, focusRect);
    }

    private void drawCornerGuides(Canvas canvas, RectF r) {
        float len = r.width() * 0.12f;

        // top-left
        canvas.drawLine(r.left, r.top, r.left + len, r.top, cornerPaint);
        canvas.drawLine(r.left, r.top, r.left, r.top + len, cornerPaint);

        // top-right
        canvas.drawLine(r.right - len, r.top, r.right, r.top, cornerPaint);
        canvas.drawLine(r.right, r.top, r.right, r.top + len, cornerPaint);

        // bottom-left
        canvas.drawLine(r.left, r.bottom - len, r.left, r.bottom, cornerPaint);
        canvas.drawLine(r.left, r.bottom, r.left + len, r.bottom, cornerPaint);

        // bottom-right
        canvas.drawLine(r.right - len, r.bottom, r.right, r.bottom, cornerPaint);
        canvas.drawLine(r.right, r.bottom - len, r.right, r.bottom, cornerPaint);
    }

    public RectF getFocusRect() {
        return focusRect;
    }
}
