package com.example.air_draw.filter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.air_draw.HomeActivity;
import com.example.air_draw.ObjRenderer;
import com.example.air_draw.R;
import com.example.air_draw.drawer.DrawActivity;
import com.example.air_draw.sticker.StickerActivity;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;


public class FaceActivity extends AppCompatActivity {
    Session mSession;

    GLSurfaceView mySurfaceView;
    RelativeLayout r_layout;
    FrameLayout centerBar, loading;

    LinearLayout bottomBar, objList;
    SlidingDrawer slide;

    ImageButton drawCapture,home, topbutton2,topbutton3;
    FrameLayout shutterImg;
    FaceRenderer mfaceRenderer;
    MediaPlayer mp;
    String mode, theme;
    SeekBar zoomSeekBar;

    float[] faceMatrix,leftMatrix,rightMatirx;
    float scale= 0.5f;
    float SScale = 1f;
    float scalePlus = 0.1f;
    float displayX, displayY, mRotate = 0f;

    boolean mTouched = false, modelInit = false, moving = false;

    float xMove , yMove;

    int selectedIndex = 0;

    // ??????, ?????? -> ??? ????????? ?????????
    GestureDetector mGestureDetector;
    // ?????? ?????? -> ??? ????????? ?????????
    ScaleGestureDetector mScaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hidStatusBarTitleBar();
        setContentView(R.layout.activity_face);


        mySurfaceView = (GLSurfaceView) findViewById(R.id.glSsurfaceview);
        slide = (SlidingDrawer) findViewById(R.id.slide);
        r_layout = (RelativeLayout) findViewById(R.id.r_Layout);
        centerBar = (FrameLayout) findViewById(R.id.centerBar);
        loading = (FrameLayout) findViewById(R.id.loading);
        bottomBar = (LinearLayout) findViewById(R.id.bottomBar);
        objList = (LinearLayout) findViewById(R.id.ObjList);
        topbutton3 = (ImageButton)  findViewById(R.id.topbutton3);
        topbutton2 = (ImageButton)  findViewById(R.id.topbutton2);
        home = (ImageButton) findViewById(R.id.home);
        drawCapture = (ImageButton) findViewById(R.id.drowCapture);
        zoomSeekBar = (SeekBar) findViewById(R.id.zoomSeekBar);
        topbutton2.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            //intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivity(intent);
        });

        mp = MediaPlayer.create(FaceActivity.this, R.raw.capture_sound);

        Intent intent = getIntent();
        mode = intent.getStringExtra("??????");
        theme = intent.getStringExtra("??????");

        shutterImg = (FrameLayout) findViewById(R.id.shutterImg);

        centerBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        slide.setVisibility(View.VISIBLE);
        home.setOnClickListener(view -> {

            Intent intent1 = new Intent(FaceActivity.this, HomeActivity.class);
            startActivity(intent1);

        });

        // GLSurfaceView ?????? ??????
        drawCapture.setOnClickListener(view -> {

            shutterImg.setVisibility(View.VISIBLE);

            captureBitmap(new FaceActivity.BitmapReadyCallbacks() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera/";

                    File file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                        Toast.makeText(FaceActivity.this, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                    }

                    SimpleDateFormat day = new SimpleDateFormat("yyyyMMddHHmmss");
                    Date date = new Date();

                    Bitmap captureview = bitmap;

                    Bitmap cropedBitmap = cropBitmap(captureview, (int) (captureview.getWidth() / SScale), (int) (captureview.getHeight() / SScale));

                    FileOutputStream fos;

                    try {
                        mp.start();

                        fos = new FileOutputStream(path + "/Capture" + day.format(date) + ".jpeg");
                        cropedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path + "/Capture" + day.format(date) + ".JPEG")));
                        Toast.makeText(FaceActivity.this, "?????? ??????. ???????????? ?????????????????? ", Toast.LENGTH_SHORT).show();
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    shutterImg.setVisibility(View.INVISIBLE);
                }
            });
        });


        //?????? ?????? ?????? --> ?????? ??????
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);

        if (displayManager != null) {

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
                            synchronized (this) {
                                //?????? ????????? ????????????
                                mfaceRenderer.onDisplayChanged();
                            }
                        }
                    }, null);
        }
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            class MyGo extends Thread {
                @Override
                public void run() {

                    if (!moving) {
                        moving = true;
                        // ?????? ?????????
                        float[] bufMatrix = faceMatrix.clone();


                        for (int i = 0; i < 200; i++) {
                            Matrix.scaleM(faceMatrix, 0, -0.05f, 0.05f, -0.05f); // -10f??????

                            try {
                                sleep(10);
                            } catch (Exception e) {

                            }
                        }

                        faceMatrix = bufMatrix;
                        moving = false;
                    }

                }
            }

            // ?????? // ????????? ??????
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

                mTouched = true;

                if (modelInit && !moving) {

                    if (distanceY > -50 && distanceY < 50) {
                        mRotate += (-distanceX / 5);

                        Matrix.rotateM(faceMatrix, 0, -distanceX / 5, 0f, 100f, 0f);
                    } else {

                        new MyGo().start();

                    }
                    Log.d("onScroll ???", xMove + "," + yMove);
                }

                return true;
            }
        });
        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                SScale = ((i / 20f) +1);
                mySurfaceView.setScaleX(SScale);
                mySurfaceView.setScaleY(SScale);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                // ??????????????? ??????????????? ??????
                scale *= detector.getScaleFactor();

                Log.d("scale ", scale + "");

                // ????????? ?????? // ?????????
                Matrix.scaleM(faceMatrix, 0,
                        detector.getScaleFactor(), // x
                        detector.getScaleFactor(), // y
                        detector.getScaleFactor()); // z
                return true;
            }
        });

        FaceRenderer.RenderCallBack mr = new FaceRenderer.RenderCallBack() {

            //MainRenderer??? onDrawFrame() -- ????????? ?????? ?????? ??????
            //MainActivity?????? ????????? ?????? ????????? ?????? ????????? ??????.
            @Override
            public void preRender() {
                if( mfaceRenderer.viewportChange){
                    Display display = getWindowManager().getDefaultDisplay();
                    mfaceRenderer.updateSession(mSession, display.getRotation());
                }
                //session??? ????????? ????????? ?????????  mainRenderer??? ???????????? ????????? ????????? ??????
                // session ????????? : ????????????  --> mainRenderer??? ????????? :  ????????? ????????? ????????????
                mSession.setCameraTextureName(mfaceRenderer.getTextureID());
                Frame frame = null;
                try {
                    frame = mSession.update(); //???????????? ????????? ??????????????????.
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }
                mfaceRenderer.mCamera.transformDisplayGeometry(frame);

                Camera camera = frame.getCamera();
                float [] viewMatrix = new float[16];
                float [] projMatrix = new float[16];

                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f);
                camera.getViewMatrix(viewMatrix, 0);

                mfaceRenderer.updateProjMatrix(projMatrix);
                mfaceRenderer.updateViewMatrix(viewMatrix);

                // ?????? ?????? ?????? ??????
                Collection<AugmentedFace> faces = mSession.getAllTrackables(AugmentedFace.class);

                for (AugmentedFace face : faces) {

                    if (face.getTrackingState() == TrackingState.TRACKING) {
                        FloatBuffer uvs = face.getMeshTextureCoordinates();
                        ShortBuffer indices = face.getMeshTriangleIndices();

                        // Center and region poses, mesh vertices, and normals are updated each frame.
                        Pose facePose = face.getCenterPose();
                        FloatBuffer faceVertices = face.getMeshVertices();
                        FloatBuffer faceNormals = face.getMeshNormals();

                        faceMatrix = new float[16];
                        leftMatrix = new float[16];
                        rightMatirx = new float[16];

                        if(selectedIndex == 0) {
                            face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT).toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0.05f, 0.06f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale, scale , scale);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 1) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, -0.015f, -0.04f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale, scale , scale);
                            Matrix.rotateM(faceMatrix,0,90f,30f,180f,0f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        }else if(selectedIndex == 2) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.05f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale * 2, scale * 2, scale * 2);
                            Matrix.rotateM(faceMatrix,0,90f,15f,60f,0f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);
                        } else if(selectedIndex == 3) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, -0.1f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale / 63, scale / 63, scale / 63) ;
                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 4) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.05f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale / 56, scale / 56, scale / 56) ;
                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 5) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, -0.1f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale / 56, scale / 56, scale / 56) ;

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);
                        } else if(selectedIndex == 6) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.08f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/25f, scale/25f, scale/25f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 7) {

                            face.getCenterPose().toMatrix(faceMatrix, 0);
                            Matrix.translateM(faceMatrix, 0, 0f, 0.06f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/10f, scale/10f, scale/10f);


                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 8) {

                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, -0.05f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/50f, scale/50f, scale/50f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 9) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, -0.05f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/50f, scale/50f, scale/50f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 10) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.1f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/17f, scale/17f, scale/17f);
                            Matrix.rotateM(faceMatrix, 0, 20f, 10f, 0f, 0f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);
                        } else if(selectedIndex == 11) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.07f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/35f, scale/35f, scale/35f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 12) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0.078f, -0.05f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/27f, scale/27f, scale/27f);
                            Matrix.rotateM(faceMatrix, 0, 50f, 0f, -30f, 0f);
                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 13) {

                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.18f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/16f, scale/16f, scale/16f);
                            Matrix.rotateM(faceMatrix, 0, 180f, 0f, 1f, 0f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        } else if(selectedIndex == 14) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0f, 0.2f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale/2f, scale/2f, scale/2f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        }else if(selectedIndex == 15) {
                            face.getCenterPose().toMatrix(faceMatrix, 0);

                            Matrix.translateM(faceMatrix, 0, 0.05f, 0.18f, 0f);
                            Matrix.scaleM(faceMatrix, 0, scale / 1.5f, scale / 1.5f, scale / 1.5f);
                            Matrix.rotateM(faceMatrix, 0, 50f, 10f, 0f, 0f);

                            mfaceRenderer.objs.get(selectedIndex).setModelMatrix(faceMatrix);

                        }

                    }

                }
            }
        };


        mfaceRenderer = new FaceRenderer(mr, this);
        InputStream is = null;

        for (int i = 0; i < mfaceRenderer.objs.size(); i++) {

            Bitmap bm = null;

            String[] str = (mfaceRenderer.objs.get(i).mTextureName).split("/");
            String btnName = (str[1].split("[.]"))[0];
            String fileName = "faceBtn/" + btnName + ".png";

            try {
                is = getResources().getAssets().open(fileName);

                bm = BitmapFactory.decodeStream(is) ;

                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ImageButton btn = new ImageButton(this);
            btn.setImageBitmap(bm);
            btn.setBackgroundColor(Color.WHITE);

            int finalI = i;

            btn.setOnClickListener(view -> {
                selectedIndex = finalI;
                for (ObjRenderer obj : mfaceRenderer.objs) {
                    obj.mModelMatrix = new float[16];
                }
            });

//            if(btnName.equals("ear_fur")){
//                continue;
//            }

            objList.addView(btn);
        }

        //pause ??? ?????? ????????? ???????????? ?????? ??????.
        mySurfaceView.setPreserveEGLContextOnPause(true);
        mySurfaceView.setEGLContextClientVersion(3); //?????? 3.0 ??????

        //????????? ??????
        mySurfaceView.setRenderer(mfaceRenderer);
        //????????? ?????? ??????
        mySurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private Bitmap snapshotBitmap;

    private interface BitmapReadyCallbacks {
        void onBitmapReady(Bitmap bitmap);
    }

    private void captureBitmap(final FaceActivity.BitmapReadyCallbacks bitmapReadyCallbacks) {
        mySurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                EGL10 egl = (EGL10) EGLContext.getEGL();
                GL10 gl = (GL10) egl.eglGetCurrentContext().getGL();


                snapshotBitmap = createBitmapFromGLSurface(0, 0, mySurfaceView.getWidth(), mySurfaceView.getHeight(), gl);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bitmapReadyCallbacks.onBitmapReady(snapshotBitmap);
                    }
                });

            }
        });
    }

    private Bitmap cropBitmap(Bitmap bitmap, int width, int height) {
        int originWidth = bitmap.getWidth();
        int originHeight = bitmap.getHeight();

        int x = 0;
        int y = 0;

        if(originWidth > width) {
            x = (originWidth - width) / 2;
        }

        if(originHeight > height) {
            y = (originHeight - height) / 2;
        }

        Bitmap cropedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
        return cropedBitmap;
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl) throws OutOfMemoryError {
        int bitmapBuffer[] = new int[(w * h)];
        int bitmapSource[] = new int[(w * h)];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);

        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (Exception e) {
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
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

                        // ?????? ????????? ????????????
                        CameraConfigFilter filter =
                                new CameraConfigFilter(mSession).setFacingDirection(CameraConfig.FacingDirection.FRONT);
                        CameraConfig cameraConfig = mSession.getSupportedCameraConfigs(filter).get(0);
                        mSession.setCameraConfig(cameraConfig);

                        topbutton3.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mSession.pause();

                                if (filter.getFacingDirection() != CameraConfig.FacingDirection.FRONT) {
                                    filter.setFacingDirection(CameraConfig.FacingDirection.FRONT);
                                    CameraConfig cameraConfig2 = mSession.getSupportedCameraConfigs(filter).get(0);
                                    mSession.setCameraConfig(cameraConfig2);
                                } else if ((filter.getFacingDirection() != CameraConfig.FacingDirection.BACK)) {
                                    filter.setFacingDirection(CameraConfig.FacingDirection.BACK);
                                    CameraConfig cameraConfig3 = mSession.getSupportedCameraConfigs(filter).get(0);
                                    mSession.setCameraConfig(cameraConfig3);
                                }

                                try {
                                    mSession.resume();
                                } catch (CameraNotAvailableException e) {
                                    e.printStackTrace();
                                }

                                Log.d("??????????", "");

                            }
                        });


                        //ARCore ??????????????? Config
                        Config config = new Config(mSession);
                        //?????? ?????? ??????
                        //config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);

                        // ???????????? ????????? ?????? ?????? ????????? ??????
                        //config.setGeospatialMode(Config.GeospatialMode.ENABLED);

                        // ?????? ???????????? ??????
                        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);

                        mSession.configure(config);

                        Log.d("?????? ???????","???????????????");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    // ?????????
    void requestPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.CAMERA},
                    0
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mGestureDetector.onTouchEvent(event); // ????????? ??????
        mScaleGestureDetector.onTouchEvent(event);

        displayX = event.getX();
        displayY = event.getY();

        if(event.getAction()==MotionEvent.ACTION_MOVE) {
            Log.d("onTouchEvent ???", event.getAction() + "");
        }

        mTouched = true;

        return true;
    }



    public void onBtnClick(View view){
        String toolType = ((Button)view).getText().toString();

        Intent intent;

        switch (toolType){

            case "?????????":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(FaceActivity.this, DrawActivity.class);
                intent.putExtra("??????", mode);
                intent.putExtra("??????", theme);
                startActivity(intent);
                break;
            case "?????????":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(FaceActivity.this, StickerActivity.class);
                intent.putExtra("??????", mode);
                intent.putExtra("??????", theme);
                startActivity(intent);
                break;
            case "?????????":
                break;
            case "??????":
                for (ObjRenderer obj : mfaceRenderer.objs) {
                    obj.mModelMatrix = new float[16];
                }

                selectedIndex--;

                if(selectedIndex < 0) {
                    selectedIndex = 0;
                }
                break;
            case "??????":
                for (ObjRenderer obj : mfaceRenderer.objs) {
                    obj.mModelMatrix = new float[16];
                }

                selectedIndex++;

                if(selectedIndex > mfaceRenderer.objs.size() - 1) {
                    selectedIndex = mfaceRenderer.objs.size() - 1;
                }
                break;

            default:
                Toast.makeText(FaceActivity.this, "?????? ????????? ?????????", Toast.LENGTH_SHORT).show();
        }
    }


    void hidStatusBarTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

}