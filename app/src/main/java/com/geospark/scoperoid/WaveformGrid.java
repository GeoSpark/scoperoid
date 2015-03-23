// Copyright (c) 2015 GeoSpark
//
// Released under the MIT License (MIT)
// See the LICENSE file, or visit http://opensource.org/licenses/MIT

package com.geospark.scoperoid;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class WaveformGrid {
    private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec2 vPosition;" +
            "attribute vec2 aTexcoord;" +
            "varying vec2 vTexcoord;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vec4(vPosition, 0.3, 1.0);" +
            "  vTexcoord = aTexcoord;" +
            "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform float brightness;" +
            "uniform sampler2D texture;" +
            "varying vec2 vTexcoord;" +
            "void main() {" +
            "  vec4 gridColour = texture2D(texture, vTexcoord);" +
            "  gridColour.a *= brightness;" +
            "  gl_FragColor = gridColour;" +
            "}";
    private final int COORDS_PER_VERTEX = 2;
    private final int program;
    private final int vertexCount = 4;
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private float[] brightness = {0.5f};
    private FloatBuffer vertexBuffer;
    private FloatBuffer coordBuffer;
    private int grid_tex;

    public WaveformGrid(Context context) {
        grid_tex = WaveformRenderer.loadTexture(context, R.drawable.grid);
        program = WaveformRenderer.loadShader(vertexShaderCode, fragmentShaderCode);

        ByteBuffer bb = ByteBuffer.allocateDirect(vertexCount * vertexStride);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(0.0f);
        vertexBuffer.put(255.0f);
        vertexBuffer.put(0.0f);
        vertexBuffer.put(0.0f);
        vertexBuffer.put(1200.0f);
        vertexBuffer.put(255.0f);
        vertexBuffer.put(1200.0f);
        vertexBuffer.put(0.0f);
        vertexBuffer.flip();

        ByteBuffer bbtex = ByteBuffer.allocateDirect(vertexCount * vertexStride);
        bbtex.order(ByteOrder.nativeOrder());
        coordBuffer = bbtex.asFloatBuffer();
        coordBuffer.clear();
        coordBuffer.put(0.0f);
        coordBuffer.put(1.0f);
        coordBuffer.put(0.0f);
        coordBuffer.put(0.0f);
        coordBuffer.put(1.0f);
        coordBuffer.put(1.0f);
        coordBuffer.put(1.0f);
        coordBuffer.put(0.0f);
        coordBuffer.flip();
    }

    public void setBrightness(float brightness) {
        this.brightness[0] = brightness;
    }

    public void draw(float[] matrix) {
        GLES20.glUseProgram(program);

        int MVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, matrix, 0);

        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int coordHandle = GLES20.glGetAttribLocation(program, "aTexcoord");
        GLES20.glEnableVertexAttribArray(coordHandle);
        GLES20.glVertexAttribPointer(coordHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, coordBuffer);

        int brightnessHandle = GLES20.glGetUniformLocation(program, "brightness");
        GLES20.glUniform1fv(brightnessHandle, 1, brightness, 0);

        int textureHandle = GLES20.glGetUniformLocation(program, "texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, grid_tex);
        GLES20.glUniform1i(textureHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
