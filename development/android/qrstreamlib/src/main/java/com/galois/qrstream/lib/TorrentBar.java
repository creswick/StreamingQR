/**
 *    Copyright 2014 Galois, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.galois.qrstream.lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 *  Android UI Widget for a non-linear progress bar.
 */
public class TorrentBar extends View {
    private final Paint onPaint = new Paint();
    private final Paint offPaint = new Paint();
    private int width;
    private int height;
    private int cellWidth;
    private boolean[] toggles;

    public TorrentBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        brushSetup();
    }

    private void brushSetup() {
        onPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        onPaint.setColor(Color.YELLOW);
        offPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        offPaint.setColor(Color.BLUE);
    }

    /**
     * Set the number of total cells.
     * @param total
     */
    public void setCellCount(int total) {
        toggles = new boolean[total];
        recomputeCellWidth();
    }

    /**
     * When the layout width changes, adjust the size of the cells
     */
    public void recomputeCellWidth() {
        if(isConfigured()) {
            this.cellWidth = width / toggles.length;
        }
    }

    public boolean isConfigured() {
        return toggles != null;
    }

    public int getCellCount() {
        return toggles.length;
    }

    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        this.width = w;
        this.height= w/10;
        recomputeCellWidth();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isConfigured()) {
            for (int i = 0; i < toggles.length; i++) {
                drawCell(canvas, i, toggles[i]);
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
     * Mark the given cell as completed
     * @param cellId cell id (1-based counting)
     */
    public void setProgress(int cellId) {
        int cellIndex = cellId - 1;
        if(cellIndex < toggles.length) {
            toggles[cellIndex] = true;
            invalidate();
        } else {
            throw new IllegalArgumentException("cellId "+cellId+" is out of range for "+toggles.length);
        }
    }

    /**
     * Set all cells as completed
     */
    public void setComplete() {
        for(int i=0; i < toggles.length; i++) {
            toggles[i] = true;
        }
        invalidate();
    }

    public void reset() {
        toggles = null;
        invalidate();
    }

}