package com.example.skincancerai;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class RiskGaugeView extends View {

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float riskPercent = 25f; // 0 -> 100

    public RiskGaugeView(Context context) {
        super(context);
        init();
    }

    public RiskGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RiskGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dp(16));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint.setColor(Color.parseColor("#2F4CCB"));
        tickPaint.setStrokeWidth(dp(3));

        textPaint.setColor(Color.parseColor("#2F4CCB"));
        textPaint.setTextSize(sp(14));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        needlePaint.setColor(Color.parseColor("#2F4CCB"));
        needlePaint.setStrokeWidth(dp(4));
        needlePaint.setStrokeCap(Paint.Cap.ROUND);

        centerPaint.setColor(Color.WHITE);
        centerPaint.setStyle(Paint.Style.FILL);
    }

    public void setRiskPercent(float percent) {
        if (percent < 0f) percent = 0f;
        if (percent > 100f) percent = 100f;
        this.riskPercent = percent;
        invalidate();
    }

    public float getRiskPercent() {
        return riskPercent;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float cx = w / 2f;
        float cy = h * 0.88f;
        float radius = Math.min(w * 0.38f, h * 0.58f);

        RectF arcRect = new RectF(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius
        );

        // Vẽ cung nhiều màu bằng sweep gradient
        SweepGradient gradient = new SweepGradient(
                cx,
                cy,
                new int[]{
                        Color.parseColor("#7ED321"),
                        Color.parseColor("#D6E800"),
                        Color.parseColor("#FFB300"),
                        Color.parseColor("#FF6A21")
                },
                new float[]{0.00f, 0.35f, 0.70f, 1.00f}
        );

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.preRotate(180, cx, cy);
        gradient.setLocalMatrix(rotateMatrix);
        arcPaint.setShader(gradient);

        // Nửa vòng trên: từ 180 đến 360
        canvas.drawArc(arcRect, 180, 180, false, arcPaint);

        // Tick 25 / 50 / 75
        drawTick(canvas, cx, cy, radius, 25, "25");
        drawTick(canvas, cx, cy, radius, 50, "50");
        drawTick(canvas, cx, cy, radius, 75, "75");

        // Text Low / High
        textPaint.setTextSize(sp(16));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Low", cx - radius + dp(18), cy - dp(6), textPaint);
        canvas.drawText("High", cx + radius - dp(46), cy - dp(6), textPaint);

        // Kim chỉ
        float angle = 180f + (riskPercent / 100f) * 180f;
        double rad = Math.toRadians(angle);

        float needleLen = radius - dp(18);
        float nx = (float) (cx + needleLen * Math.cos(rad));
        float ny = (float) (cy + needleLen * Math.sin(rad));

        canvas.drawLine(cx, cy, nx, ny, needlePaint);
        canvas.drawCircle(cx, cy, dp(10), centerPaint);
        canvas.drawCircle(cx, cy, dp(10), needlePaint);

        // Đầu kim
        Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setColor(Color.WHITE);
        tipPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(nx, ny, dp(9), tipPaint);

        Paint tipStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipStroke.setColor(Color.parseColor("#2F4CCB"));
        tipStroke.setStyle(Paint.Style.STROKE);
        tipStroke.setStrokeWidth(dp(2));
        canvas.drawCircle(nx, ny, dp(9), tipStroke);
    }

    private void drawTick(Canvas canvas, float cx, float cy, float radius, int value, String label) {
        float angle = 180f + (value / 100f) * 180f;
        double rad = Math.toRadians(angle);

        float r1 = radius - dp(2);
        float r2 = radius - dp(18);

        float x1 = (float) (cx + r1 * Math.cos(rad));
        float y1 = (float) (cy + r1 * Math.sin(rad));
        float x2 = (float) (cx + r2 * Math.cos(rad));
        float y2 = (float) (cy + r2 * Math.sin(rad));

        canvas.drawLine(x1, y1, x2, y2, tickPaint);

        float textR = radius - dp(40);
        float tx = (float) (cx + textR * Math.cos(rad));
        float ty = (float) (cy + textR * Math.sin(rad));

        textPaint.setTextSize(sp(16));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Rect bounds = new Rect();
        textPaint.getTextBounds(label, 0, label.length(), bounds);
        canvas.drawText(label, tx - bounds.width() / 2f, ty + bounds.height() / 2f, textPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}