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
import android.graphics.Matrix;
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

    /*
     * The transformation matrix that will bring the QR finder location points into the camera
     * coordinate view. This is necessary since the camera preview frame assumes landscape
     * mode and may have different dimensions than that of the on-screen display.
     * Initially this starts off as the identity matrix but gets replaced
     * with the right transformation matrix once the camera previewSize and rotation are known.
     */
    private Matrix cameraTransformationMtx = new Matrix();

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

    public void setPoints(@NotNull float[] datapoints) {
        this.points = datapoints.clone();
    }

    /*
     * Saves dimension of incoming preview frame so that the QR result points can be
     * displayed in the correct location and orientation as the camera preview frame.
     *
     * Once this is known, we can also create the transformation matrix that will
     * bring the QR finder location points into the camera coordinate view.
     */
    public void setCameraParameters(final Size previewSize, int rotation) {
        int cameraRotation = rotation % 360;

        // Reset the transformation matrix to apply to the QR finder points
        cameraTransformationMtx = buildCameraTransformationMatrix(previewSize, cameraRotation);
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

        cameraTransformationMtx.mapPoints(dest, points);
        canvas.drawPoints(dest, paint);
    }

    /*
 * Camera preview is always in landscape mode. To display the
 * QR finder points in the right location on the phone, we need to:
 *    1. center the frame around the origin,
 *    2. scale it to match the same width and height as the display,
 *    3. rotate it by the same degree as the application, and then
 *    4. translate it back to the start location.
 */
    private Matrix buildCameraTransformationMatrix(final Size previewSize, int rotation) {
        // Reset the transformation matrix to apply to the QR finder points
        Matrix mtx = new Matrix();

        // Desired display dimensions (i.e. the dimensions of this view)
        int displayWidth  = getWidth();
        int displayHeight = getHeight();

        // How much to scale the image to get to desired dimensions
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        // This moves the preview image to the origin so that a rotation
        // can be applied correctly without moving the frame out of view.
        if (previewSize != null) {
            // The ReceiveFragment always sets the camera to portrait mode
            // so we shouldn't need to handle other cases
            if (rotation == 90 ) {
                // Scale = desired dimension / actual dimension
                scaleX = displayHeight / (float) previewSize.width;
                scaleY = displayWidth / (float) previewSize.height;
            } else {
                // TODO what other rotations do we need to handle?
                Log.e(Constants.APP_TAG, "buildCameraTransformationMatrix received rotation, "+
                        rotation + ", but expected 90.");
            }
            mtx.setTranslate(-previewSize.width/2,-previewSize.height/2);
        } else {
            mtx.setTranslate(-displayWidth/2,-displayHeight/2);
        }
        mtx.postScale(scaleX, scaleY);
        mtx.postRotate(rotation);
        mtx.postTranslate(displayWidth/2,displayHeight/2);

        return mtx;
    }

}
