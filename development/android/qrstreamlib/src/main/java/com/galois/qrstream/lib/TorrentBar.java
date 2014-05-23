package com.galois.qrstream.lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TorrentBar extends View {
    private int cellCount;
    private Paint mPaint;
    private int width;
    private int height;
    private int cellWidth;

    public TorrentBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintInit();
    }

    public void setTotalChunks(int total) {
        this.cellCount = total;
        this.cellWidth = width / total;
    }

    private void paintInit() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLUE);
    }

    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        Log.d("qrstream", "onSizeChanged w:"+w+" h:"+h+" ow:"+oldw+" oh:"+oldh);
        this.width = w;
        this.height= w/10;
    }

    protected void onDraw(Canvas canvas) {
        Log.d("qrstream", "onDraw total "+cellCount);
        super.onDraw(canvas);
        for(int i=0; i < cellCount; i++) {
            drawCell(canvas, i, true);
        }
    }

    protected void drawCell(Canvas canvas, int idx, boolean onoff) {
        RectF cellBounds = cellBounds(idx);
        canvas.drawOval(
                cellBounds,
                mPaint
        );
    }

    protected RectF cellBounds(int idx) {
        int top = 0;
        int left = idx * cellWidth;
        int right = left + this.height;
        int bottom = top + this.height;
        RectF bounds = new RectF();
        bounds.set(left, top, right, bottom);
        return bounds;
    }

    public void setProgress(int progressStatus) {
        invalidate();
    }

    public int getMax() {
        return 100;
    }
}