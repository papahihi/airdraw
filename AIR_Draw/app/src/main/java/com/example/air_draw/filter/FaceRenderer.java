package com.example.air_draw.filter;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import com.example.air_draw.CameraPreView;
import com.example.air_draw.ObjRenderer;
import com.google.ar.core.Session;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class FaceRenderer implements GLSurfaceView.Renderer {

    interface RenderCallBack{
        void preRender();
    }
    int width, height;
    RenderCallBack myCallBack;
    boolean viewportChange = false;
    ArrayList<ObjRenderer> objs;

    CameraPreView mCamera;
    FaceActivity faceActivity;

    public FaceRenderer(RenderCallBack myCallBack, Context context){
        this.myCallBack = myCallBack;
        mCamera = new CameraPreView();

        objs = new ArrayList<ObjRenderer>(){
            {
                add(new ObjRenderer(context, "faceObj/diffuse.obj", "faceObj/diffuse.jpg"));
                add(new ObjRenderer(context, "faceObj/sunflower.obj","faceObj/sunflower.jpg"));
                add(new ObjRenderer(context, "faceObj/beanie.obj", "faceObj/beanie.png"));
                add(new ObjRenderer(context, "faceObj/guy_mask.obj", "faceObj/guy_mask.png"));
                add(new ObjRenderer(context, "faceObj/deer_horn.obj", "faceObj/deer_horn.png"));
                add(new ObjRenderer(context, "faceObj/trump.obj", "faceObj/trump.png"));
                add(new ObjRenderer(context, "faceObj/goldstar.obj", "faceObj/goldstar.png"));
                add(new ObjRenderer(context, "faceObj/heart.obj", "faceObj/heart.png"));
                add(new ObjRenderer(context, "faceObj/mouth_sad.obj", "faceObj/mouth_sad.png"));
                add(new ObjRenderer(context, "faceObj/mouth_surprise.obj", "faceObj/mouth_surprise.png"));
                add(new ObjRenderer(context, "faceObj/partyhat.obj", "faceObj/partyhat.png"));
                add(new ObjRenderer(context, "faceObj/rebon.obj", "faceObj/rebon.png"));
                add(new ObjRenderer(context, "faceObj/smoke.obj", "faceObj/smoke.png"));
                add(new ObjRenderer(context, "faceObj/yeah.obj", "faceObj/yeah.png"));
                add(new ObjRenderer(context, "faceObj/pikachu.obj", "faceObj/pikachu.png"));
                add(new ObjRenderer(context, "faceObj/eve.obj", "faceObj/eve.png"));
            }
        };

        faceActivity = (FaceActivity)context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClearColor(0f,1f,1f,1f);
        mCamera.init();

        for (ObjRenderer obj: objs) {
            obj.init();
        }
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

        for (ObjRenderer obj: objs) {
            obj.draw();
        }
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

    void updateProjMatrix(float [] matrix){
        for (ObjRenderer obj :objs) {
            obj.setProjectionMatrix(matrix);
        }
    }

    void updateViewMatrix(float [] matrix){
        for (ObjRenderer obj :objs) {
            obj.setViewMatrix(matrix);
        }
    }
}
