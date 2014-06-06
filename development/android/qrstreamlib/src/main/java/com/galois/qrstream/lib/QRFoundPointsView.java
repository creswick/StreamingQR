package com.galois.qrstream.lib;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.*;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.jetbrains.annotations.NotNull;

/**
 * Surface overlay for drawing the location of QR finder patterns whenever
 * any QR code has been identified in the camera preview frame.
 *
 * Created by lcasburn on 6/4/14.
 */
public class QRFoundPointsView extends View {

    // Canvas drawPoints expects that the array being passed
    // has x followed by y available: [x1,y2,x2,y2...xi,yi...].
    private float[] points = new float[0];
    private final Paint paint = new Paint();

    // Dimension of incoming preview frame so that the QR result points can be
    // displayed in the correct location and orientation as the camera preview frame.
    private Size previewSize = null;
    private int cameraRotation = 0;

    public QRFoundPointsView(Context context) {
        super(context);
        initPaintStyles();
    }

    public QRFoundPointsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPaintStyles();
    }


    public QRFoundPointsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaintStyles();
    }

    private void initPaintStyles() {
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }

    /*
     * Saves dimension of incoming preview frame so that the QR result points can be
     * displayed in the correct location and orientation as the camera preview frame.
     */
    public void setCameraPreviewSize(Size cameraPreviewSize) {
        this.previewSize = cameraPreviewSize;
    }

    public void setCameraRotation(int rotation) {
        this.cameraRotation = rotation % 360;
    }

    public void setPoints(@NotNull float[] datapoints) {
        this.points = datapoints.clone();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Camera preview is always in landscape mode. To display the
        // QR finder points in the right location on the phone, we need to:
        //   1. center the frame around the origin,
        //   2. scale it to match the same width and height as the display,
        //   3. rotate it by the same degree as the application, and then
        //   4. translate it back to the start location.


        // The final points to display
        float [] dest = new float[points.length];

        // The transformation matrix to apply to the QR finder points
        Matrix mtx = new Matrix();

        // Desired display dimensions
        int imgWidth  = getWidth();
        int imgHeight = getHeight();

        // How much to scale the image to get to desired dimensions
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (previewSize != null) {
            // TODO what other rotations do we need to handle?
            if (cameraRotation == 90 ) {
                // Scale = desired dimension / actual dimension
                scaleX = imgHeight / (float) previewSize.width;
                scaleY = imgWidth / (float) previewSize.height;
            }
            mtx.setTranslate(-previewSize.width/2,-previewSize.height/2);
        } else {
            mtx.setTranslate(-imgWidth/2,-imgHeight/2);
        }
        mtx.postScale(scaleX,scaleY);
        mtx.postRotate(cameraRotation);
        mtx.postTranslate(imgWidth/2,imgHeight/2);
        mtx.mapPoints(dest, points);

        canvas.drawPoints(dest, paint);

    }
}
