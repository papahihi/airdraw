package com.example.air_draw;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.air_draw.drawer.DrawActivity;
import com.example.air_draw.sticker.StickerActivity;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class QRActivity extends AppCompatActivity {

    Session mSession;
    CameraPreView mCamera;
    GLSurfaceView mySurfaceView;

    RelativeLayout r_layout;
    ImageButton home;
    String mode,theme;


    ArrayList<String> mainThemeObjs01,mainThemeObjs02,mainThemeObjs03;

    QRRenderer qrRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hidStatusBarTitleBar();
        setContentView(R.layout.activity_qrsel);

        r_layout = (RelativeLayout)findViewById(R.id.r_Layout);
        ImageView gif = (ImageView)findViewById(R.id.imageView5);
        Glide.with(this).load(R.raw.searching_01).into(gif);
        home = (ImageButton) findViewById(R.id.home);

        Intent intent = getIntent();
        mode = intent.getStringExtra("??????");

        findViewById(R.id.qrModeSel).setVisibility(View.VISIBLE);
        mySurfaceView = (GLSurfaceView)findViewById(R.id.glSsurfaceview);


        home.setOnClickListener(view -> {

            Intent intent1 = new Intent(QRActivity.this, HomeActivity.class);
            startActivity(intent1);

        });
        //?????? ?????? ?????? --> ?????? ??????
        DisplayManager displayManager = (DisplayManager)getSystemService(DISPLAY_SERVICE);

        if(displayManager != null){

            //?????? ????????? ??????
            displayManager.registerDisplayListener(
                    new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {}
                        @Override
                        public void onDisplayRemoved(int displayId) {}
                        //????????? ??????????????????
                        @Override
                        public void onDisplayChanged(int displayId) {
                            //????????? --> ????????? ???????????? ????????? ??????
                            synchronized (this){
                                //?????? ????????? ????????????
                                qrRenderer.onDisplayChanged();
                            }
                        }
                    }, null);
        }
        QRRenderer.RenderCallBack mr = new QRRenderer.RenderCallBack() {
            @Override
            public void preRender() {

                if (qrRenderer.viewportChange) {
                    Display display = getWindowManager().getDefaultDisplay();

                    qrRenderer.updateSession(mSession, display.getRotation());
                }
                mSession.setCameraTextureName(qrRenderer.getTextureID());

                Frame frame = null;

                try {
                    frame = mSession.update(); //???????????? ????????? ??????????????????.
                } catch (
                        CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                qrRenderer.mCamera.transformDisplayGeometry(frame);


                Camera camera = frame.getCamera();

                float [] viewMatrix = new float[16];
                float [] projMatrix = new float[16];

                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f);
                camera.getViewMatrix(viewMatrix, 0);

                drawImages(frame);

            }

        };
        qrRenderer = new QRRenderer(mr, this);

        //pause ??? ?????? ????????? ???????????? ?????? ??????.
        mySurfaceView.setPreserveEGLContextOnPause(true);
        mySurfaceView.setEGLContextClientVersion(3); //?????? 3.0 ??????

        //????????? ??????
        mySurfaceView.setRenderer(qrRenderer);
        //????????? ?????? ??????
        mySurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //????????? ??????
        requestPermission();

        //ARCore session ?????? --> ????????? ??????
        if(mSession == null){

            try {

                Log.d("?????? ???????",
                        ArCoreApk.getInstance().requestInstall(this, true)+"");

                switch (ArCoreApk.getInstance().requestInstall(this, true)){
                    case INSTALLED:
                        //ARCore  ?????? ????????? ?????? ??????
                        mSession = new Session(this);

                        //ARCore ??????????????? Config
                        Config config = new Config(mSession);
                        //?????? ?????? ??????
                        config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
                        mSession.configure(config);

                        Log.d("?????? ???????","???????????????");
                        break;
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        Config mConfig = new Config(mSession);

        setImgDB(mConfig);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mySurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySurfaceView.onPause();
        mSession.pause();
    }

    void setImgDB(Config mConfig){
        //??????????????????????????? ??????
        AugmentedImageDatabase imgDB = new AugmentedImageDatabase(mSession);

        try {
            InputStream is = getAssets().open("camping_qr.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imgDB.addImage("Camping",bitmap);
            is.close();

            is = getAssets().open("spase.png");
            bitmap = BitmapFactory.decodeStream(is);
            imgDB.addImage("spase",bitmap);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //????????? ?????????????????? ??????
        mConfig.setAugmentedImageDatabase(imgDB);
    }
    //????????? ???????????? ?????? ?????? ?????????
    void drawImages(Frame frame){

        Collection<AugmentedImage> agImgs = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage img: agImgs){
            if(img.getTrackingState()== TrackingState.TRACKING) {
                Intent intent;

                switch (img.getName()){
                    case "Camping":
                        theme  = img.getName();
                        break;
                    case "spase":
                        theme  = img.getName();
                        break;
                }
                intent = new Intent(QRActivity.this, StickerActivity.class);
                intent.putExtra("??????", mode);
                intent.putExtra("??????", theme);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // ??????????????? ??? ????????? ?????????
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    void hidStatusBarTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }
    void requestPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.CAMERA},
                    0
            );
        }
    }

    public void onBtnClick(View view){

    }
}
