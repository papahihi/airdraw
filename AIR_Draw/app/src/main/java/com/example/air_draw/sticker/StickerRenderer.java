package com.example.air_draw.sticker;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.View;

import com.example.air_draw.CameraPreView;
import com.example.air_draw.ObjRenderer;
import com.example.air_draw.PlaneRenderer;
import com.google.ar.core.Session;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class StickerRenderer implements GLSurfaceView.Renderer {

    interface RenderCallBack {
        void preRender();
    }

    int width, height;
    RenderCallBack myCallBack;
    boolean viewportChange = false, planeCreate = true;

    CameraPreView mCamera;
    PlaneRenderer mPlane;

    ArrayList<ObjRenderer> themeObjs;

    StickerActivity stickerActivity;

    StickerRenderer(RenderCallBack myCallBack, Context context, String theme) {
        this.myCallBack = myCallBack;
        stickerActivity = (StickerActivity) context;
        mCamera = new CameraPreView();
        mPlane = new PlaneRenderer(Color.GREEN, 0.7f);

        switch (theme) {
            case "pokemon":
                themeObjs = new ArrayList<ObjRenderer>() {
                    {
                        add(new ObjRenderer(context,"pokemonObj/beedrill.obj","pokemonObj/beedrill.png"));
                        add(new ObjRenderer(context,"pokemonObj/bellsprout.obj","pokemonObj/bellsprout.png"));
                        add(new ObjRenderer(context,"pokemonObj/blastoise.obj","pokemonObj/blastoise.png"));
                        add(new ObjRenderer(context,"pokemonObj/bulbasaur.obj","pokemonObj/bulbasaur.png"));
                        add(new ObjRenderer(context,"pokemonObj/butterfree.obj","pokemonObj/butterfree.png"));
                        add(new ObjRenderer(context,"pokemonObj/caterpie.obj","pokemonObj/caterpie.png"));
                        add(new ObjRenderer(context,"pokemonObj/charizard.obj","pokemonObj/charizard.png"));
                        add(new ObjRenderer(context,"pokemonObj/charmander.obj","pokemonObj/charmander.png"));
                        add(new ObjRenderer(context,"pokemonObj/doduo.obj","pokemonObj/doduo.png"));
                        add(new ObjRenderer(context,"pokemonObj/dratini.obj","pokemonObj/dratini.png"));
                        add(new ObjRenderer(context,"pokemonObj/drowzee.obj","pokemonObj/drowzee.png"));
                        add(new ObjRenderer(context,"pokemonObj/eevee.obj","pokemonObj/eevee.png"));
                        add(new ObjRenderer(context,"pokemonObj/ekans.obj","pokemonObj/ekans.png"));
                        add(new ObjRenderer(context,"pokemonObj/electabuzz.obj","pokemonObj/electabuzz.png"));
                        add(new ObjRenderer(context,"pokemonObj/exeggcute.obj","pokemonObj/exeggcute.png"));
                        add(new ObjRenderer(context,"pokemonObj/feraligatr.obj","pokemonObj/feraligatr.png"));
                        add(new ObjRenderer(context,"pokemonObj/geodude.obj","pokemonObj/geodude.png"));
                        add(new ObjRenderer(context,"pokemonObj/golem.obj","pokemonObj/golem.png"));
                        add(new ObjRenderer(context,"pokemonObj/hooh.obj","pokemonObj/hooh.png"));
                        add(new ObjRenderer(context,"pokemonObj/kabuto.obj","pokemonObj/kabuto.png"));
                        add(new ObjRenderer(context,"pokemonObj/lapras.obj","pokemonObj/lapras.png"));
                        add(new ObjRenderer(context,"pokemonObj/machamp.obj","pokemonObj/machamp.png"));
                        add(new ObjRenderer(context,"pokemonObj/machop.obj","pokemonObj/machop.png"));
                        add(new ObjRenderer(context,"pokemonObj/magnemite.obj","pokemonObj/magnemite.png"));
                        add(new ObjRenderer(context,"pokemonObj/mankey.obj","pokemonObj/mankey.png"));
                        add(new ObjRenderer(context,"pokemonObj/meowth.obj","pokemonObj/meowth.png"));
                        add(new ObjRenderer(context,"pokemonObj/mew.obj","pokemonObj/mew.png"));
                        add(new ObjRenderer(context,"pokemonObj/mewtwo.obj","pokemonObj/mewtwo.png"));
                        add(new ObjRenderer(context,"pokemonObj/nidoran.obj","pokemonObj/nidoran.png"));
                        add(new ObjRenderer(context,"pokemonObj/pikachu.obj", "pokemonObj/pikachu.png"));
                        add(new ObjRenderer(context,"pokemonObj/pinsir.obj","pokemonObj/pinsir.png"));
                        add(new ObjRenderer(context,"pokemonObj/poliwag.obj","pokemonObj/poliwag.png"));
                        add(new ObjRenderer(context,"pokemonObj/psyduck.obj","pokemonObj/psyduck.png"));
                        add(new ObjRenderer(context,"pokemonObj/raticate.obj","pokemonObj/raticate.png"));
                        add(new ObjRenderer(context,"pokemonObj/rattata.obj","pokemonObj/rattata.png"));
                        add(new ObjRenderer(context,"pokemonObj/rhyhorn.obj","pokemonObj/rhyhorn.png"));
                        add(new ObjRenderer(context,"pokemonObj/sandshrew.obj","pokemonObj/sandshrew.png"));
                        add(new ObjRenderer(context,"pokemonObj/scyther.obj","pokemonObj/scyther.png"));
                        add(new ObjRenderer(context,"pokemonObj/spearow.obj","pokemonObj/spearow.png"));
                        add(new ObjRenderer(context,"pokemonObj/squirtle.obj","pokemonObj/squirtle.png"));
                        add(new ObjRenderer(context,"pokemonObj/tauros.obj","pokemonObj/tauros.png"));
                        add(new ObjRenderer(context,"pokemonObj/tentacruel.obj","pokemonObj/tentacruel.png"));
                        add(new ObjRenderer(context,"pokemonObj/venusaur.obj","pokemonObj/venusaur.png"));
                        add(new ObjRenderer(context,"pokemonObj/voltorb.obj","pokemonObj/voltorb.png"));
                        add(new ObjRenderer(context,"pokemonObj/weedle.obj","pokemonObj/weedle.png"));
                        add(new ObjRenderer(context,"pokemonObj/weezing.obj","pokemonObj/weezing.png"));
                        add(new ObjRenderer(context,"pokemonObj/zubat.obj","pokemonObj/zubat.png"));
                    }
                };
                break;
            case "amongus":
                themeObjs = new ArrayList<ObjRenderer>() {
                    {
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_blackcolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_bluecolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_browncolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_cyancolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_greencolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_limecolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_orangecolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_pinkcolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_purplecolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_redcolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_whitecolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/amongus.obj", "amongusObj/amongus_yellowcolor.jpg"));
                        add(new ObjRenderer(context, "amongusObj/among_killer.obj", "amongusObj/among_killer.png"));
                    }
                };
                break;
            case "Camping":
                themeObjs = new ArrayList<ObjRenderer>() {
                    {
                        add(new ObjRenderer(context, "campingObj/Campfire.obj", "campingObj/Campfire.jpeg"));
                        add(new ObjRenderer(context, "campingObj/SleepingBag.obj", "campingObj/SleepingBag.png"));
                        add(new ObjRenderer(context, "campingObj/stone761.obj", "campingObj/stone761.png"));
                        add(new ObjRenderer(context, "campingObj/stump.obj", "campingObj/stump.png"));
                        add(new ObjRenderer(context, "campingObj/tent_green.obj", "campingObj/tent_green.png"));
                        add(new ObjRenderer(context, "campingObj/treeStump.obj", "campingObj/treeStump.png"));
                    }
                };
                break;
            case "free":
                themeObjs = new ArrayList<ObjRenderer>() {
                    {
                        add(new ObjRenderer(context, "freeObj/baseball_bat.obj", "freeObj/baseball_bat.png"));
                        add(new ObjRenderer(context, "freeObj/flower.obj", "freeObj/flower.png"));
                        add(new ObjRenderer(context, "freeObj/GiraffeCar.obj", "freeObj/GiraffeCar.png"));
                        add(new ObjRenderer(context, "freeObj/Hamster.obj", "freeObj/Hamster.png"));
                        add(new ObjRenderer(context, "freeObj/Microphone.obj", "freeObj/Microphone.png"));
                        add(new ObjRenderer(context, "freeObj/Mushroom.obj", "freeObj/Mushroom.png"));
                        add(new ObjRenderer(context, "freeObj/octopus.obj", "freeObj/octopus.png"));
                        add(new ObjRenderer(context, "freeObj/patrick.obj", "freeObj/patrick.png"));
                        add(new ObjRenderer(context, "freeObj/Robot_B01.obj", "freeObj/Robot_B01.png"));
                        add(new ObjRenderer(context, "freeObj/Shark.obj", "freeObj/Shark.jpg"));
                        add(new ObjRenderer(context, "freeObj/Umbreon.obj", "freeObj/Umbreon.png"));
                        add(new ObjRenderer(context, "freeObj/balloon.obj", "freeObj/balloon.png"));
                        add(new ObjRenderer(context, "freeObj/cake.obj", "freeObj/cake.png"));
                        add(new ObjRenderer(context, "freeObj/cheese.obj", "freeObj/cheese.png"));
                        add(new ObjRenderer(context, "freeObj/owl.obj", "freeObj/owl.png"));
                        add(new ObjRenderer(context, "freeObj/seagull.obj", "freeObj/seagull.png"));
                        add(new ObjRenderer(context, "freeObj/star.obj", "freeObj/star.png"));
                        add(new ObjRenderer(context, "freeObj/star2.obj", "freeObj/star2.png"));
                    }
                };
                break;
        }

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClearColor(0f, 1f, 1f, 1f);
        mCamera.init();
        mPlane.init();

        for (ObjRenderer obj : themeObjs) {
            obj.init();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
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


        if (stickerActivity.planeCreate) {
            mPlane.draw();
        }

        for (ObjRenderer obj : themeObjs) {
            obj.draw();
        }
    }

    //화면 변화 감지하면 내가 실행된다.
    void onDisplayChanged() {
        viewportChange = true;
    }

    int getTextureID() {  //카메라의 색칠하기 id를 리턴 한다.
        return mCamera == null ? -1 : mCamera.mTextures[0];
    }

    void updateSession(Session session, int rotation) {
        if (viewportChange) {
            session.setDisplayGeometry(rotation, width, height);

            viewportChange = false;
        }
    }

    void setModelMatrix(float x, float y, float z) {

        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, x, y, z);
    }

    void updateProjMatrix(float[] matrix) {
        mPlane.setProjectionMatrix(matrix);

        for (ObjRenderer obj : themeObjs) {
            obj.setProjectionMatrix(matrix);
        }
    }

    void updateViewMatrix(float[] matrix) {
        mPlane.setViewMatrix(matrix);

        for (ObjRenderer obj : themeObjs) {
            obj.setViewMatrix(matrix);
        }
    }
}
