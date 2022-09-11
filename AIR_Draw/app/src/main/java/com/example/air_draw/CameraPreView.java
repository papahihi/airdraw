package com.example.air_draw;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraPreView {

    //GPU 를 이용하여 고속 계산 하여 화면 처리 하기 위한 코드
    String vertexShaderCode =
            "attribute vec4 vPosition ;" + //vec4 -> 3차원 좌표
                    "attribute vec2 aTexCoord ;" +
                    "varying vec2 vTexCoord ;" +

                    "void main () {" +
                    "   vTexCoord =  aTexCoord ; "+
                    "  gl_Position =  vPosition ; "+

                    // gl_Position : OpenGL 에 있는 변수  ::> 계산식   uMVPMatrix * vPosition
                    "}";

    String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require \n"+
                    "precision mediump float;"+
                    "uniform samplerExternalOES sTexture; " +
                    "varying vec2 vTexCoord; " +
                    "void main() { "+
                    "   gl_FragColor = texture2D(sTexture , vTexCoord); "+
                    "}";





    //직사각형 점의 좌표
    static float [] QUARD_COORDS = {
            //x  ,   y,    z
            -1.0f,  -1.0f,  0.0f,
            -1.0f,  1.0f,  0.0f,
            1.0f,  -1.0f,  0.0f,
            1.0f,  1.0f,  0.0f

    };

    static float [] QUARD_TEXCOORDS = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,

    };

    public int [] mTextures;
    FloatBuffer mVertices; //점정보
    FloatBuffer mTexCoords; //텍스쳐좌표
    FloatBuffer mTextCoordsTransformed;

    int mProgram;

    public CameraPreView(){
        mVertices = ByteBuffer.allocateDirect(QUARD_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(QUARD_COORDS);
        mVertices.position(0);

        mTexCoords = ByteBuffer.allocateDirect(QUARD_TEXCOORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoords.put(QUARD_TEXCOORDS);
        mTexCoords.position(0);


        mTextCoordsTransformed = ByteBuffer.allocateDirect(QUARD_TEXCOORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();


    }

    //카메라 초기화
    public void init(){

        //텍스처 생성성
        mTextures = new int[1];
        GLES30.glGenTextures(1,mTextures,0);

        //텍스처 바인딩 -> 외부에서의 텍스처를 지정위치에 binding
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,mTextures[0]);

        // glTexParameteri: 축소, 확대 필터을 설정, 점의 경계를 부드럽게 보느냐 반복시킬것인가 등등 설정
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);


        //점쉐이더 생성
        int vShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vShader, vertexShaderCode);

        //컴파일
        GLES30.glCompileShader(vShader);
//        int[] compiled = new int[1];
//        GLES30.glGetShaderiv(vShader, GLES30.GL_COMPILE_STATUS, compiled, 0);

        //텍스처
        int fShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fShader, fragmentShaderCode);

        //컴파일
        GLES30.glCompileShader(fShader);
        //GLES30.glGetShaderiv(vShader, GLES30.GL_COMPILE_STATUS, compiled, 0);


        mProgram = GLES30.glCreateProgram();
        //점위치 계산식 합치기
        GLES30.glAttachShader(mProgram,vShader);
        //색상 계산식 합치기
        GLES30.glAttachShader(mProgram,fShader);
        GLES30.glLinkProgram(mProgram);  //도형 렌더링 계산식 정보 넣는다.



    }

    //카메라로 그리기
    public void draw(){
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,mTextures[0]);

        GLES30.glUseProgram(mProgram);

        //점,색 계산방식
        int position = GLES30.glGetAttribLocation(mProgram, "vPosition");

        int texCoord = GLES30.glGetAttribLocation(mProgram, "aTexCoord");

        //점, 색 좌표개산
        GLES30.glVertexAttribPointer(position, 3, GLES30.GL_FLOAT, false,0, mVertices);

        GLES30.glVertexAttribPointer(texCoord, 2, GLES30.GL_FLOAT, false,0, mTextCoordsTransformed);


        //GPU 활성화
        GLES30.glEnableVertexAttribArray(position);
        GLES30.glEnableVertexAttribArray(texCoord);

        //그린다
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0,4);

        //GPU 비활성화
        GLES30.glDisableVertexAttribArray(position);
        GLES30.glDisableVertexAttribArray(texCoord);
    }

    public void transformDisplayGeometry(Frame frame){

        // x, y, z  --> 객체의 좌표 (이동, 회전, 크기)
        // u, v, ww --> 맵핑(이미지)의 좌표 (이동, 회전, 크기)
        frame.transformDisplayUvCoords(mTexCoords, mTextCoordsTransformed);
    }
}
