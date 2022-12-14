package com.example.air_draw.sticker;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
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
import android.widget.ImageView;
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
import com.example.air_draw.drawer.DrawActivity;
import com.example.air_draw.R;
import com.example.air_draw.filter.FaceActivity;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class StickerActivity extends AppCompatActivity {

    Session mSession;
    GLSurfaceView mySurfaceView;
    RelativeLayout r_layout,mToolKit;
    FrameLayout centerBar;
    LinearLayout bottomBar, objList;
    SlidingDrawer slide;
    ImageView handle;
    Button  toolbutton3;
    FrameLayout shutterImg, loading;
    String mode, theme;

    StickerRenderer mRenderer;
    //ScaleGestureDetector ScreenscaleGestureDetector;
    ImageButton drawCapture,topbutton2, home;
    MediaPlayer mp;
    SeekBar zoomSeekBar;

    float displayX, displayY, mRotate = 0f, mScale = 1f;

    boolean mTouched = false, modelInit = false, moving = false, planeCreate = false;

    float[] modelMatrix = new float[16];
    float[] campMatrix = new float[16];

    float xMove , yMove;
    float SScale = 1f;

    int selectedIndex = 0;

    // ??????, ?????? -> ??? ????????? ?????????
    GestureDetector mGestureDetector;
    // ?????? ?????? -> ??? ????????? ?????????
    ScaleGestureDetector mScaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hidStatusBarTitleBar();
        setContentView(R.layout.activity_sticker);

        mySurfaceView = (GLSurfaceView)findViewById(R.id.glSsurfaceview);
        mToolKit = (RelativeLayout)findViewById(R.id.ToolKit);
        slide = (SlidingDrawer)findViewById(R.id.slide);
        r_layout = (RelativeLayout)findViewById(R.id.r_Layout);
        centerBar = (FrameLayout) findViewById(R.id.centerBar);
        bottomBar = (LinearLayout)findViewById(R.id.bottomBar);
        slide = (SlidingDrawer) findViewById(R.id.slide);
        objList = (LinearLayout)findViewById(R.id.ObjList);
        handle = (ImageView) findViewById(R.id.handle);
        drawCapture = (ImageButton) findViewById(R.id.drowCapture);
        shutterImg = (FrameLayout) findViewById(R.id.shutterImg);
        loading = (FrameLayout) findViewById(R.id.loading);
        zoomSeekBar = (SeekBar) findViewById(R.id.zoomSeekBar);
        toolbutton3 = (Button) findViewById(R.id.toolbtn03);
        home = (ImageButton) findViewById(R.id.home);

        toolbutton3.setOnClickListener(view -> {
            mRenderer.themeObjs.get(selectedIndex).mModelMatrix = new float[16];
        });

        topbutton2 = (ImageButton) findViewById(R.id.topbutton2);
        topbutton2.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            //intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivity(intent);
        });
        home.setOnClickListener(view -> {

            Intent intent1 = new Intent(StickerActivity.this, HomeActivity.class);
            startActivity(intent1);

        });

        mp = MediaPlayer.create(StickerActivity.this, R.raw.capture_sound);

        Intent intent = getIntent();
        mode = intent.getStringExtra("??????");
        theme = intent.getStringExtra("??????");

        if(mode.equals("?????? ??????")) {
            theme = "free";
        }

        centerBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        slide.setVisibility(View.VISIBLE);

        if(theme.equals("pokemon")) {
            handle.setImageDrawable(getResources().getDrawable(R.drawable.ic_pokeball));
        } else if(theme.equals("amongus")) {
            handle.setImageDrawable(getResources().getDrawable(R.drawable.ic_amonghatch));
        } else if(theme.equals("Camping")) {
            slide.setVisibility(View.INVISIBLE);
            handle.setImageDrawable(getResources().getDrawable(R.drawable.ic_camp));
        } else if(theme.equals("free")) {
            handle.setImageDrawable(getResources().getDrawable(R.drawable.ic_excite));
        }

        // GLSurfaceView ?????? ??????
        drawCapture.setOnClickListener(view -> {

            shutterImg.setVisibility(View.VISIBLE);

            captureBitmap(new BitmapReadyCallbacks() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera/";

                    File file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                        Toast.makeText(StickerActivity.this, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                    }

                    SimpleDateFormat day = new SimpleDateFormat("yyyyMMddHHmmss");
                    Date date = new Date();

                    Bitmap captureview = bitmap;

                    Bitmap cropedBitmap = cropBitmap(captureview, (int)(captureview.getWidth() / SScale), (int)(captureview.getHeight() / SScale));

                    FileOutputStream fos;

                    try {
                        mp.start();

                        fos = new FileOutputStream(path + "/Capture" + day.format(date) + ".jpeg");
                        cropedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path + "/Capture" + day.format(date) + ".JPEG")));
                        Toast.makeText(StickerActivity.this, "?????? ??????. ???????????? ?????????????????? ", Toast.LENGTH_SHORT).show();
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    shutterImg.setVisibility(View.INVISIBLE);
                }
            });
        });

        // ????????? ????????? ??????
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            // ?????? // ?????? ?????????
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mTouched = true;
                modelInit = false;
                planeCreate = false;
                displayX = e.getX();
                displayY = e.getY();

                Log.d("onDoubleTap ???", displayX + "," + displayY);
                return true;
            }

            class MyGo extends Thread {
                @Override
                public void run() {

                    if(!moving) {
                        moving = true;
                        // ?????? ?????????
                        float[] bufMatrix = modelMatrix.clone();

                        for (int i = 0; i < 200; i++) {
                            Matrix.translateM(modelMatrix, 0, 0, 0, -0.05f); // -10f??????

                            try {
                                sleep(10);
                            } catch (Exception e) {

                            }
                        }
                        modelMatrix = bufMatrix;
                        moving = false;

                    }

                }
            }

            // ?????? // ????????? ??????
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

                mTouched = true;

                if(modelInit && !moving) {

                    if(distanceY > -50 && distanceY < 50) {
                        mRotate += (-distanceX / 5);

                        Matrix.rotateM(modelMatrix, 0, -distanceX / 5,  0f, 100f, 0f);
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

        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                Log.d("onScale ???", detector.getScaleFactor() + "");

                // ??????????????? ??????????????? ??????
                mScale *= detector.getScaleFactor();
                // ????????? ?????? // ?????????
                Matrix.scaleM(modelMatrix, 0,
                        detector.getScaleFactor(), // x
                        detector.getScaleFactor(), // y
                        detector.getScaleFactor()); // z
                return true;
            }
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
                                mRenderer.onDisplayChanged();
                            }
                        }
                    }, null);
        }

        StickerRenderer.RenderCallBack mr = new StickerRenderer.RenderCallBack() {
            //StickerRenderer??? onDrawFrame() -- ????????? ?????? ?????? ??????
            //MainActivity?????? ????????? ?????? ????????? ?????? ????????? ??????.
            @Override
            public void preRender() {

                if(mRenderer.viewportChange){
                    Display display = getWindowManager().getDefaultDisplay();
                    mRenderer.updateSession(mSession, display.getRotation());
                }

                //session??? ????????? ????????? ?????????  StickerRenderer??? ???????????? ????????? ????????? ??????
                // session ????????? : ????????????  --> StickerRenderer??? ????????? :  ????????? ????????? ????????????
                mSession.setCameraTextureName(mRenderer.getTextureID());
                Frame frame = null;

                try {
                    frame = mSession.update(); //???????????? ????????? ??????????????????.
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                mRenderer.mCamera.transformDisplayGeometry(frame);

                // ?????? ????????? ?????? // if ??????????????? ????????? ?????? ??????????????? ?????? ??????
                LightEstimate estimate = frame.getLightEstimate();
                float lightyIntensity = estimate.getPixelIntensity();

                if(mTouched) {
                    List<HitResult> results = frame.hitTest(displayX, displayY);

                    for (HitResult hr : results) {
                        Pose pose = hr.getHitPose();
                        Trackable trackable = hr.getTrackable();

                        // ?????? ?????? ????????? Plane?????? Plane??? ?????? ?????? ???????
                        if(trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(pose)){

                            // ?????? ???????????? ???????????? ????????? ????????????
                            if(!modelInit) {
                                modelInit = true;

                                pose.toMatrix(modelMatrix, 0);
                                // ?????? ????????????  ?????????????????? ???????????? ??????
                                Matrix.rotateM(modelMatrix, 0, mRotate,  0f, 100f, 0f);
                                // ?????? ???????????? ?????????????????? ??????????????? ??????
                                Matrix.scaleM(modelMatrix, 0, mScale, mScale, mScale);
                            }

                            // obj ??????
//                            Matrix.translateM(modelMatrix, 0, 0f, 0.3f, 0f); // y????????? 0.3f ??????
//                            Matrix.rotateM(modelMatrix, 0, 45f,  0f, 100f, 0f); // y????????? 45??? ?????? // 100f??? ???????????????
//                            Matrix.rotateM(modelMatrix, 0, 45f,  10f, 0f, 0f); // ?????? x????????? ??????. ?????????????????????
//                            Matrix.scaleM(modelMatrix, 0, 1f, 3f, 1f); // ????????? ?????? // ?????????

                            // ???????????? ??????????????? ????????? ????????? ?????? ???????
                            mRenderer.themeObjs.get(selectedIndex).setLightIntensity(lightyIntensity);

                            // ?????? ?????? ??????
                            float[] colorArr = {1.0f, 1.0f, 1.0f, 0.6f};

                            mRenderer.themeObjs.get(selectedIndex).setColorCorrection(colorArr);
                            mRenderer.themeObjs.get(selectedIndex).setModelMatrix(modelMatrix);

                        }
                    }
                    mTouched = false;
                }

                if(planeCreate){
                    Collection<Plane> planes = mSession.getAllTrackables(Plane.class);

                    for (Plane plane: planes) {
                        if(plane.getTrackingState() == TrackingState.TRACKING && plane.getSubsumedBy() == null) {
                            // ?????? ?????????
                            mRenderer.mPlane.update(plane);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        Log.d("TrackingState", "?????? ??????");
                                    }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        Log.d("TrackingState", "?????????");
                                    }
                            });
                        }

                    }
                }

                Camera camera = frame.getCamera();

                if(theme.equals("Camping")) {

                    if(!modelInit) {
                        modelInit = true;

                        for (ObjRenderer obj: mRenderer.themeObjs) {

                            camera.getPose().toMatrix(campMatrix, 0);

                            if(obj.mObjName.split("/")[1].equals("Campfire.obj")) {
                                Matrix.translateM(campMatrix, 0, 1f, 0f, -2f);
                                Matrix.rotateM(campMatrix, 0, 90,  0f, 0f, 1f);
                                Matrix.scaleM(campMatrix, 0, 0.01f, 0.01f, 0.01f);

                                obj.setModelMatrix(campMatrix);
                            }

                            if(obj.mObjName.split("/")[1].equals("SleepingBag.obj")) {
                                Matrix.translateM(campMatrix, 0, 2f, 2f, -4f);
                                Matrix.rotateM(campMatrix, 0, 90,  0f, 0f, 1f);
                                Matrix.scaleM(campMatrix, 0, 0.2f, 0.2f, 0.2f);

                                obj.setModelMatrix(campMatrix);
                            }

                            if(obj.mObjName.split("/")[1].equals("stone761.obj")) {
                                Matrix.translateM(campMatrix, 0, 2f, -4f, -8f);
                                Matrix.rotateM(campMatrix, 0, 90,  0f, 0f, 1f);

                                obj.setModelMatrix(campMatrix);
                            }

                            if(obj.mObjName.split("/")[1].equals("stump.obj")) {
                                Matrix.translateM(campMatrix, 0, 1f, -3f, -2f);
                                Matrix.rotateM(campMatrix, 0, 90,  0f, 0f, 1f);

                                obj.setModelMatrix(campMatrix);
                            }

                            if(obj.mObjName.split("/")[1].equals("tent_green.obj")) {
                                Matrix.translateM(campMatrix, 0, 2f, 0f, -10f);
                                Matrix.rotateM(campMatrix, 0, 90,  0f, 0f, 1f);
                                Matrix.scaleM(campMatrix, 0, 3f, 3f, 3f);

                                obj.setModelMatrix(campMatrix);
                            }

                            if(obj.mObjName.split("/")[1].equals("treeStump.obj")) {
                                Matrix.translateM(campMatrix, 0, 2f, 0f, -1f);
                                Matrix.rotateM(campMatrix, 0, 90,  0f, 0f, 1f);
                                Matrix.scaleM(campMatrix, 0, 0.5f, 0.5f, 0.5f);

                                obj.setModelMatrix(campMatrix);
                            }
                        }

                    }
                }

                float [] viewMatrix = new float[16];
                float [] projMatrix = new float[16];

                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 500f);
                camera.getViewMatrix(viewMatrix, 0);

                mRenderer.updateProjMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);
            }
        };

        mRenderer = new StickerRenderer(mr, this, theme);
        InputStream is = null;

        if(theme.equals("pokemon")) {
            for (int i = 0; i < mRenderer.themeObjs.size(); i++) {

                Bitmap bm = null;

                String[] str = (mRenderer.themeObjs.get(i).mObjName).split("/");
                String btnName = (str[1].split("[.]"))[0];
                String fileName = "pokemonBtn/" + btnName + ".png";

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
                    planeCreate = true;
                });

                objList.addView(btn);
            }
        } else if(theme.equals("amongus")) {
            for (int i = 0; i < mRenderer.themeObjs.size(); i++) {

                Bitmap bm = null;

                String[] str = (mRenderer.themeObjs.get(i).mTextureName).split("/");
                String btnName = (str[1].split("[.]"))[0];
                String fileName = "amongusBtn/" + btnName + ".png";

                try {
                    is = getResources().getAssets().open(fileName);

                    bm = BitmapFactory.decodeStream(is) ;

                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ImageButton btn = new ImageButton(this);
                btn.setBackgroundColor(Color.WHITE);
                btn.setImageBitmap(bm);

                int finalI = i;

                btn.setOnClickListener(view -> {
                    selectedIndex = finalI;
                    planeCreate = true;
                });

                objList.addView(btn);
            }
        } else if(theme.equals("Camping")) {
            for (int i = 0; i < mRenderer.themeObjs.size(); i++) {

                Bitmap bm = null;

                String[] str = (mRenderer.themeObjs.get(i).mTextureName).split("/");
                String btnName = (str[1].split("[.]"))[0];
                String fileName = "campBtn/" + btnName + ".png";

                try {
                    is = getResources().getAssets().open(fileName);

                    bm = BitmapFactory.decodeStream(is) ;

                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ImageButton btn = new ImageButton(this);
                btn.setBackgroundColor(Color.WHITE);
                btn.setImageBitmap(bm);

                int finalI = i;

                btn.setOnClickListener(view -> {
                    selectedIndex = finalI;
                    planeCreate = true;
                });

                objList.addView(btn);
            }
        } else if(theme.equals("free")) {
            for (int i = 0; i < mRenderer.themeObjs.size(); i++) {

                Bitmap bm = null;

                String[] str = (mRenderer.themeObjs.get(i).mTextureName).split("/");
                String btnName = (str[1].split("[.]"))[0];
                String fileName = "freeBtn/" + btnName + ".png";

                try {
                    is = getResources().getAssets().open(fileName);

                    bm = BitmapFactory.decodeStream(is) ;

                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ImageButton btn = new ImageButton(this);
                btn.setBackgroundColor(Color.WHITE);
                btn.setImageBitmap(bm);

                int finalI = i;

                btn.setOnClickListener(view -> {
                    selectedIndex = finalI;
                    planeCreate = true;
                });

                objList.addView(btn);
            }
        } else {
            for (int i = 0; i < mRenderer.themeObjs.size(); i++) {

                String[] str = (mRenderer.themeObjs.get(i).mTextureName).split("/");
                String btnName = (str[1].split("[.]"))[0];

                Button btn = new Button(this);
                btn.setText(btnName);

                int finalI = i;

                btn.setOnClickListener(view -> {
                    selectedIndex = finalI;
                    planeCreate = true;
                });

                objList.addView(btn);
            }
        }

        //pause ??? ?????? ????????? ???????????? ?????? ??????.
        mySurfaceView.setPreserveEGLContextOnPause(true);
        mySurfaceView.setEGLContextClientVersion(3); //?????? 3.0 ??????

        //????????? ??????
        mySurfaceView.setRenderer(mRenderer);
        //????????? ?????? ??????
        mySurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private Bitmap snapshotBitmap;

    private interface BitmapReadyCallbacks {
        void onBitmapReady(Bitmap bitmap);
    }

    private void captureBitmap(final BitmapReadyCallbacks bitmapReadyCallbacks) {
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

    void hidStatusBarTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mGestureDetector.onTouchEvent(event); // ????????? ??????
        mScaleGestureDetector.onTouchEvent(event);
        //ScreenscaleGestureDetector.onTouchEvent(event);

        return true;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                try {
                    // ????????? ??????????????? ????????? ??????
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    Bitmap img = BitmapFactory.decodeStream(in);
                    in.close();
                    // ????????? ??????
                    //  imageView.setImageBitmap(img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void onBtnClick(View view){
        String toolType = ((Button)view).getText().toString();

        Log.d("?????? ?????????",toolType+"");

        Intent intent;

        switch (toolType){
            case "?????????":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(StickerActivity.this, DrawActivity.class);
                intent.putExtra("??????", mode);
                intent.putExtra("??????", theme);
                startActivity(intent);
                break;
            case "?????????":
                break;
            case "?????????":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(StickerActivity.this, FaceActivity.class);
                intent.putExtra("??????", mode);
                intent.putExtra("??????", theme);
                startActivity(intent);
                break;

            default:
                Toast.makeText(StickerActivity.this,"?????? ????????? ?????????",Toast.LENGTH_SHORT).show();
        }
    }
}
