// Copyright (c) 2015 GeoSpark
//
// Released under the MIT License (MIT)
// See the LICENSE file, or visit http://opensource.org/licenses/MIT

package com.geospark.scoperoid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WaveformRenderer implements GLSurfaceView.Renderer {
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private Context context;
    private byte[] waveform_data = null;
    private WaveformLine line;
    private WaveformGrid grid;

    public Handler handler;

    Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Bundle b = msg.getData();
            waveform_data = b.getByteArray("waveform");
            return true;
        }
    };

    public WaveformRenderer(Context context) {
        handler = new Handler(callback);
        this.context = context;
    }

    public static int loadShader(String vertexShaderCode, String fragmentShaderCode) {
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        int program = GLES20.glCreateProgram();

        GLES20.glShaderSource(vs, vertexShaderCode);
        GLES20.glShaderSource(fs, fragmentShaderCode);
        GLES20.glCompileShader(vs);
        GLES20.glCompileShader(fs);
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        return program;
    }

    public static int loadTexture(final Context context, final int resourceId) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        } else {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        int c = context.getResources().getColor(R.color.channel1High);
        float r = (float)Color.red(c) / 255.0f;
        float g = (float)Color.green(c) / 255.0f;
        float b = (float)Color.blue(c) / 255.0f;
        line = new WaveformLine(r, g, b);
        grid = new WaveformGrid(context);
        grid.setBrightness(0.75f);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.orthoM(mProjectionMatrix, 0, 0.0f, 1200.0f, 0.0f, 255.0f, 0.1f, 10.0f);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        grid.draw(mMVPMatrix);

        if (waveform_data != null) {
            line.setData(waveform_data);
            line.draw(mMVPMatrix);
        }
    }
}
