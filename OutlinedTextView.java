package com.onecodeman.driply;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class OutlinedTextView extends androidx.appcompat.widget.AppCompatTextView {

    private Paint strokePaint;
    private Paint fillPaint;

    public OutlinedTextView(Context context) {
        super(context);
        init();
    }

    public OutlinedTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OutlinedTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Paint for the black stroke
        strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4); // Thickness of the outline
        strokePaint.setColor(0xFF000000); // Black outline color
        strokePaint.setAntiAlias(true);
        strokePaint.setTextAlign(Paint.Align.LEFT);

        // Paint for the white fill
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xFFFFFFFF); // White fill color
        fillPaint.setAntiAlias(true);
        fillPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the stroke first
        String text = getText().toString();
        canvas.drawText(text, getPaddingLeft(), getBaseline(), strokePaint);

        // Draw the fill on top of the stroke
        canvas.drawText(text, getPaddingLeft(), getBaseline(), fillPaint);
    }
}

