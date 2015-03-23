// Copyright (c) 2015 GeoSpark
//
// Released under the MIT License (MIT)
// See the LICENSE file, or visit http://opensource.org/licenses/MIT

package com.geospark.scoperoid;

import android.content.Context;
import android.content.res.TypedArray;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;


public class WaveformView extends GLSurfaceView {
    private int mAspectRatioWidth;
    private int mAspectRatioHeight;
    private final WaveformRenderer mRenderer;
    private static final double VIEW_ASPECT_RATIO = 1.5;
    private ViewAspectRatioMeasurer varm = new ViewAspectRatioMeasurer(VIEW_ASPECT_RATIO);

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaveformView);

        mAspectRatioWidth = a.getInt(R.styleable.WaveformView_aspectRatioWidth, 1);
        mAspectRatioHeight = a.getInt(R.styleable.WaveformView_aspectRatioHeight, 1);

        a.recycle();

        setEGLContextClientVersion(2);
        mRenderer = new WaveformRenderer(context);

        setRenderer(mRenderer);
    }

    public void setWaveformData(byte[] data) {
        // Bit of an assumption, ideally we read in the first two bytes to determine the length
        // of the rest of the header, but meh.
        byte[] hdr = Arrays.copyOfRange(data, 2, 11);
        int data_len;

        try {
            String s = new String(hdr, "US-ASCII");
            data_len = Integer.parseInt(s);
        } catch (UnsupportedEncodingException e) {
            return;
        }

        Message msg = Message.obtain(mRenderer.handler);
        Bundle b = new Bundle();
        b.putByteArray("waveform", Arrays.copyOfRange(data, 11, Math.min(data.length, data_len + 12)));
        msg.setData(b);
        msg.sendToTarget();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        varm.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(varm.getMeasuredWidth(), varm.getMeasuredHeight());
        //        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
//        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
//        int calculatedHeight = originalWidth * mAspectRatioHeight / mAspectRatioWidth;
//
//        int finalWidth, finalHeight;
//
//        if (calculatedHeight > originalHeight) {
//            finalWidth = originalHeight * mAspectRatioWidth / mAspectRatioHeight;
//            finalHeight = originalHeight;
//        } else {
//            finalWidth = originalWidth;
//            finalHeight = calculatedHeight;
//        }
//
////        float ar = (float)finalWidth / (float)finalHeight;
////        Log.d("GRID", String.valueOf(finalWidth) + " x " + String.valueOf(finalHeight) + " (" + String.valueOf(ar) + ")");
//
//        super.onMeasure(
//                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }
}
