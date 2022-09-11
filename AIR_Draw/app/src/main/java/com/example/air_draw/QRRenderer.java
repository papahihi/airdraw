package com.example.air_draw;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Session;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class QRRenderer implements GLSurfaceView.Renderer {

    interface RenderCallBack{
        void preRender();
    }
    int width, height;
    RenderCallBack myCallBack;
    boolean viewportChange = false;

    CameraPreView mCamera;

    QRRenderer(RenderCallBack myCallBack, Context context){
        this.myCallBack = myCallBack;
        mCamera = new CameraPreView();

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClearColor(0f,1f,1f,1f);
        mCamera.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        GLES30.glViewport(0,0,width, height);
        viewportChange = true;
        this.width = width;
        this.height = height;

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        //mainActivity 로부터 카메라 화면 정보를 받기 위해 메소드 실행
        myCallBack.preRender();

        GLES30.glDepthMask(false);
        mCamera.draw();
        GLES30.glDepthMask(true);

    }

    //화면 변화 감지하면 내가 실행된다.
    void onDisplayChanged(){
        viewportChange = true;
    }


    int getTextureID(){  //카메라의 색칠하기 id를 리턴 한다.
        return mCamera == null ? -1 : mCamera.mTextures[0];
    }

    void updateSession(Session session, int rotation){
        if(viewportChange){
            session.setDisplayGeometry(rotation, width, height);

            viewportChange = false;
        }
    }
}
