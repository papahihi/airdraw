package com.example.air_draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class ObjRenderer {
    private static final String TAG = ObjRenderer.class.getSimpleName();

    private final String vertexShaderString =
            "uniform mat4 uMvMatrix;\n" +
                    "uniform mat4 uMvpMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec3 aNormal;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "   vPosition = (uMvMatrix * aPosition).xyz;\n" +
                    "   vNormal = normalize((uMvMatrix * vec4(aNormal, 0.0)).xyz);\n" +
                    "   vTexCoord = aTexCoord;\n" +
                    "   gl_Position = uMvpMatrix * vec4(aPosition.xyz, 1.0);\n" +
                    "}";

    private final String fragmentShaderString =
            "precision mediump float;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "uniform vec4 uLighting;\n" +
                    "uniform vec4 uMaterial;\n" +
                    "uniform vec4 uColorCorrection;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    const float kGamma = 0.4545454;\n" +
                    "    const float kInverseGamma = 2.2;\n" +
                    "    const float kMiddleGrayGamma = 0.466;\n" +
                    "    vec3 viewLightDirection = uLighting.xyz;\n" +
                    "    vec3 colorShift = uColorCorrection.rgb;\n" +
                    "    float averagePixelIntensity = uColorCorrection.a;\n" +
                    "    float lightIntensity = uLighting.w;\n" +
                    "    float materialAmbient = uMaterial.x;\n" +
                    "    float materialDiffuse = uMaterial.y;\n" +
                    "    float materialSpecular = uMaterial.z;\n" +
                    "    float materialSpecularPower = uMaterial.w;\n" +
                    "    vec3 viewFragmentDirection = normalize(vPosition);\n" +
                    "    vec3 viewNormal = normalize(vNormal);\n" +
                    "    vec4 objectColor = texture2D(uTexture, vec2(vTexCoord.x, 1.0 - vTexCoord.y));\n" +
                    "    objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));\n" +
                    "    float ambient = materialAmbient;\n" +
                    "    float diffuse = lightIntensity * materialDiffuse * 0.5 * (dot(viewNormal, viewLightDirection) + 1.0);\n" +
                    "    vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);\n" +
                    "    float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));\n" +
                    "    float specular = lightIntensity * materialSpecular * pow(specularStrength, materialSpecularPower);\n" +
                    "    vec3 color = objectColor.rgb * (ambient + diffuse) + specular;\n" +
                    "    color.rgb = pow(color, vec3(kGamma));\n" +
                    "    color *= colorShift * (averagePixelIntensity / 0.5);\n" +
                    "    gl_FragColor.a = objectColor.a;\n" +
                    "    gl_FragColor.rgb = color;\n" +
                    "}";

    /*private final String fragmentShaderString =
            "precision mediump float;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "uniform vec4 uLighting;\n" +  //빛을 처리하는 변수
                    "uniform vec4 uMaterial;\n" +
                    "varying vec3 vPosition;\n" +
                    "varying vec3 vNormal;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    const float kGamma = 0.4545454;\n" +
                    "    const float kInverseGamma = 2.2;\n" +
                    "    vec3 viewLightDirection = uLighting.xyz;\n" +
                    "    float lightIntensity = uLighting.w;\n" +
                    "    float materialAmbient = uMaterial.x;\n" +
                    "    float materialDiffuse = uMaterial.y;\n" +
                    "    float materialSpecular = uMaterial.z;\n" +
                    "    float materialSpecularPower = uMaterial.w;\n" +
                    "    vec3 viewFragmentDirection = normalize(vPosition);\n" +
                    "    vec3 viewNormal = normalize(vNormal);\n" +
                    "    vec4 objectColor = texture2D(uTexture, vec2(vTexCoord.x, 1.0 - vTexCoord.y));\n" +
                    "    objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));\n" +
                    "    float ambient = materialAmbient;\n" +
                    "    float diffuse = lightIntensity * materialDiffuse * 0.5 * (dot(viewNormal, viewLightDirection) + 1.0);\n" +
                    "    vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);\n" +
                    "    float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));\n" +
                    "    float specular = lightIntensity * materialSpecular * pow(specularStrength, materialSpecularPower);\n" +
                    "    gl_FragColor.a = objectColor.a;\n" +
                    "    gl_FragColor.rgb = pow(objectColor.rgb * (ambient + diffuse) + specular, vec3(kGamma));\n" +
                    "}";*/

    private Context mContext;
    public String mObjName;
    public String mTextureName;

    private Obj mObj;

    private int mProgram;
    private int[] mTextures;
    private int[] mVbos;
    private int mVerticesBaseAddress;
    private int mTexCoordsBaseAddress;
    private int mNormalsBaseAddress;
    private int mIndicesCount;

    public float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    //빛의 세기 변수
    private float mLightIntensity;

    float [] mColorCorrection = {0.8f,0.8f,0.8f,0.8f};

    public ObjRenderer(Context context, String objName, String textureName) {
        mContext = context;
        mObjName = objName;
        mTextureName = textureName;
    }

    public void init() {
        try {
            InputStream is = mContext.getAssets().open(mObjName);
            Bitmap bmp = BitmapFactory.decodeStream(mContext.getAssets().open(mTextureName));
            mObj = ObjReader.read(is);
            mObj = ObjUtils.convertToRenderable(mObj);

            mTextures = new int[1];
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glGenTextures(1, mTextures, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextures[0]);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

            bmp.recycle();
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        if (mObj == null || mTextures[0] == -1) {
            Log.e(TAG, "Failed to init obj - " + mObjName + ", " + mTextureName);
        }

        ShortBuffer indices = ObjData.convertToShortBuffer(ObjData.getFaceVertexIndices(mObj, 3));
        FloatBuffer vertices = ObjData.getVertices(mObj);
        FloatBuffer texCoords = ObjData.getTexCoords(mObj, 2);
        FloatBuffer normals = ObjData.getNormals(mObj);

        mVbos = new int[2];
        GLES30.glGenBuffers(2, mVbos, 0);

        mVerticesBaseAddress = 0;
        mTexCoordsBaseAddress = mVerticesBaseAddress + 4 * vertices.limit();
        mNormalsBaseAddress = mTexCoordsBaseAddress + 4 * texCoords.limit();
        final int totalBytes = mNormalsBaseAddress + 4 * normals.limit();

        mIndicesCount = indices.limit();

        // vertexBufferId
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbos[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, totalBytes, null, GLES30.GL_STATIC_DRAW);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, mVerticesBaseAddress, 4 * vertices.limit(), vertices);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, mTexCoordsBaseAddress, 4 * texCoords.limit(), texCoords);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, mNormalsBaseAddress, 4 * normals.limit(), normals);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        // indexBufferId
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mVbos[1]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, 2 * mIndicesCount, indices, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);

        int vShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vShader, vertexShaderString);
        GLES30.glCompileShader(vShader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(vShader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile vertex shader.");
            GLES30.glDeleteShader(vShader);
        }

        int fShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fShader, fragmentShaderString);
        GLES30.glCompileShader(fShader);
        GLES30.glGetShaderiv(fShader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile fragment shader.");
            GLES30.glDeleteShader(fShader);
        }

        mProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(mProgram, vShader);
        GLES30.glAttachShader(mProgram, fShader);
        GLES30.glLinkProgram(mProgram);
        int[] linked = new int[1];
        GLES30.glGetProgramiv(mProgram, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Could not link program.");
        }
    }

    public void draw() {
        float[] mvMatrix = new float[16];
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0);

        GLES30.glUseProgram(mProgram);

        int mv = GLES30.glGetUniformLocation(mProgram, "uMvMatrix");
        int mvp = GLES30.glGetUniformLocation(mProgram, "uMvpMatrix");

        int position = GLES30.glGetAttribLocation(mProgram, "aPosition");
        int normal = GLES30.glGetAttribLocation(mProgram, "aNormal");
        int texCoord = GLES30.glGetAttribLocation(mProgram, "aTexCoord");

        int texture = GLES30.glGetUniformLocation(mProgram, "uTexture");

        int lighting = GLES30.glGetUniformLocation(mProgram, "uLighting");
        int material = GLES30.glGetUniformLocation(mProgram, "uMaterial");
        int colorCorrection = GLES30.glGetUniformLocation(mProgram, "uColorCorrection");




        float[] viewLightDirection = new float[4];
        float[] lightDirection = new float[] {0.250f, 0.866f, 0.433f, 0.0f};
        Matrix.multiplyMV(viewLightDirection, 0, mvMatrix, 0, lightDirection, 0);
        normalize(viewLightDirection);
        // 빛의 세기를 받아 재질을 표현한다.
        GLES30.glUniform4f(lighting, viewLightDirection[0], viewLightDirection[1], viewLightDirection[2], mLightIntensity);

        GLES30.glUniform4f(colorCorrection, mColorCorrection[0], mColorCorrection[1], mColorCorrection[2], mColorCorrection[3]);

        float ambient = 0.3f;
        float diffuse = 1.0f;
        float specular = 1.0f;
        float specularPower = 6.0f;
        GLES30.glUniform4f(material, ambient, diffuse, specular, specularPower);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextures[0]);
        GLES30.glUniform1i(texture, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbos[0]);
        GLES30.glVertexAttribPointer(position, 3, GLES30.GL_FLOAT, false, 0, mVerticesBaseAddress);
        GLES30.glVertexAttribPointer(normal, 3, GLES30.GL_FLOAT, false, 0, mNormalsBaseAddress);
        GLES30.glVertexAttribPointer(texCoord, 2, GLES30.GL_FLOAT, false, 0, mTexCoordsBaseAddress);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glUniformMatrix4fv(mv, 1, false, mvMatrix, 0);
        GLES30.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);

        GLES30.glEnableVertexAttribArray(position);
        GLES30.glEnableVertexAttribArray(normal);
        GLES30.glEnableVertexAttribArray(texCoord);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mVbos[1]);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mIndicesCount, GLES30.GL_UNSIGNED_SHORT, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES30.glDisableVertexAttribArray(position);
        GLES30.glDisableVertexAttribArray(normal);
        GLES30.glDisableVertexAttribArray(texCoord);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public void setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, mModelMatrix, 0, 16);
    }

    public void setProjectionMatrix(float[] projMatrix) {
        System.arraycopy(projMatrix, 0, mProjMatrix, 0, 16);
    }

    public void setViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16);
    }

    public void setLightIntensity(float lightIntensity) {
        mLightIntensity = lightIntensity;
    }

    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    public void setColorCorrection(float[] colorCorrection){
        mColorCorrection[0] = colorCorrection[0];
        mColorCorrection[1] = colorCorrection[1];
        mColorCorrection[2] = colorCorrection[2];
        mColorCorrection[3] = colorCorrection[3];
    }

    float[][] getPoint() {
        float[][] minMax = calcMinMax();
        float[] minPoint = new float[4];
        float[] maxPoint = new float[4];

        Matrix.multiplyMV(
                minPoint, 0,
                mModelMatrix, 0,
                new float[]{minMax[0][0], minMax[0][1], minMax[0][2], 1f}, 0);

        Matrix.multiplyMV(
                maxPoint, 0,
                mModelMatrix, 0,
                new float[]{minMax[1][0], minMax[1][1], minMax[1][2], 1f}, 0);

//        System.out.println("큐브 getPoint() 여: " + Arrays.toString(minPoint));
//        System.out.println("큐브 getPoint() 여: " + Arrays.toString(maxPoint));

        // 좌우가 뒤집어졌을때? 를 대비해서
        float[] minRes = new float[3];
        float[] maxRes = new float[3];

        minRes[0] = Math.min(minPoint[0], maxPoint[0]);
        minRes[1] = Math.min(minPoint[1], maxPoint[1]);
        minRes[2] = Math.min(minPoint[2], maxPoint[2]);

        maxRes[0] = Math.max(minPoint[0], maxPoint[0]);
        maxRes[1] = Math.max(minPoint[1], maxPoint[1]);
        maxRes[2] = Math.max(minPoint[2], maxPoint[2]);

        return new float[][]{minRes, maxRes};
    }

    float[][] calcMinMax() {
        float[] minPoint = new float[3];
        float[] maxPoint = new float[3];

        float[] vertices = ObjData.getVerticesArray(mObj);

        for (int i = 1; i < mObj.getNumVertices(); i++) {
            minPoint[0] = Math.min(minPoint[0], vertices[i * 3 + 0]);
            minPoint[1] = Math.min(minPoint[1], vertices[i * 3 + 1]);
            minPoint[2] = Math.min(minPoint[2], vertices[i * 3 + 2]);

            maxPoint[0] = Math.max(maxPoint[0], vertices[i * 3 + 0]);
            maxPoint[1] = Math.max(maxPoint[1], vertices[i * 3 + 1]);
            maxPoint[2] = Math.max(maxPoint[2], vertices[i * 3 + 2]);
        }

        return new float[][]{minPoint, maxPoint};
    }
}