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
    private Paint onPaint, offPaint;
    private int width;
    private int height;
    private int cellWidth;
    private boolean[] onoffs;

    public TorrentBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintInit();
    }

    private void paintInit() {
        onPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        onPaint.setColor(Color.YELLOW);
        offPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        offPaint.setColor(Color.BLUE);
    }

    public void setCellCount(int total) {
        this.cellCount = total;
        onoffs = new boolean[total];
        recomputeCellWidth();
    }

    public void recomputeCellWidth() {
        if(this.cellCount > 0) {
            this.cellWidth = width / this.cellCount;
        }
    }

    public int getCellCount() {
        return this.cellCount;
    }

    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        this.width = w;
        this.height= w/10;
        recomputeCellWidth();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(onoffs != null) {
            for (int i = 0; i < cellCount; i++) {
                drawCell(canvas, i, onoffs[i]);
            }
        }
    }

    protected void drawCell(Canvas canvas, int idx, boolean onoff) {
        RectF cellBounds = cellBounds(idx);
        Paint paint;
        if(onoff) {
            paint = onPaint;
        } else {
            paint = offPaint;
        }
        canvas.drawRect(
                cellBounds,
                paint
        );
    }

    protected RectF cellBounds(int idx) {
        int top = 0;
        int left = idx * cellWidth;
        int right = left + this.width;
        int bottom = top + this.height;
        RectF bounds = new RectF();
        bounds.set(left, top, right, bottom);
        return bounds;
    }

    /**
     * Mark the given chunk as received
     * @param chunkId chunk id (1-based counting)
     */
    public void setProgress(int chunkId) {
        onoffs[chunkId-1] = true;
        invalidate();
    }

    public void setComplete() {
        for(int i=0; i < cellCount; i++) {
            onoffs[i] = true;
        }
        invalidate();
    }

    public void reset() {
        invalidate();
    }

}