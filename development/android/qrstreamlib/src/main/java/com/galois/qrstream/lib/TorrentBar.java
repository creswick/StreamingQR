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
    private Paint mPaint;
    private RectF mBounds;

    public TorrentBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintInit();
    }

    private void paintInit() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLUE);
    }

    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        Log.d("qrstream", "onSizeChanged w:"+w+" h:"+h+" ow:"+oldw+" oh:"+oldh);
        mBounds = new RectF();
        int aspectHeight= w/10;
        mBounds.set(0,0,w,aspectHeight);

    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the shadow
        canvas.drawOval(
                mBounds,
                mPaint
        );
    }

    public void setProgress(int progressStatus) {
    }

    public int getMax() {
        return 100;
    }
}