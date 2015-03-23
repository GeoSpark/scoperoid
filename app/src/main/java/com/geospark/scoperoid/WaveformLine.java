// Copyright (c) 2015 GeoSpark
//
// Released under the MIT License (MIT)
// See the LICENSE file, or visit http://opensource.org/licenses/MIT

package com.geospark.scoperoid;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class WaveformLine {
    private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec2 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vec4(vPosition, 0.2, 1.0);" +
            "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private final int COORDS_PER_VERTEX = 2;
    private final int program;
    private final int vertexCount = 1200;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private float[] colour;

    private FloatBuffer vertexBuffer;

    public WaveformLine(float r, float g, float b) {
        colour = new float[4];
        colour[0] = r;
        colour[1] = g;
        colour[2] = b;
        colour[3] = 1.0f;
        program = WaveformRenderer.loadShader(vertexShaderCode, fragmentShaderCode);
        ByteBuffer bb = ByteBuffer.allocateDirect(vertexCount * vertexStride);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
    }

    public void setData(byte[] data) {
        vertexBuffer.clear();

        for (int i = 0; i < Math.min(vertexCount, data.length); ++i) {
            vertexBuffer.put((float)i);
            float y = (float)(data[i] & 0xff);
            // Empirically derived numbers to make the waveform fit the grid.
            vertexBuffer.put((y * 1.285f) - 35.0f);
        }

        vertexBuffer.flip();
    }

    public void draw(float[] matrix) {
        GLES20.glUseProgram(program);

        int MVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, matrix, 0);

        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, colour, 0);

        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
