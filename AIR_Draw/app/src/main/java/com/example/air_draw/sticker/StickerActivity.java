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

    // 이동, 회전 -> 한 손가락 이벤트
    GestureDetector mGestureDetector;
    // 크기 조절 -> 두 손가락 이벤트
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
        mode = intent.getStringExtra("모드");
        theme = intent.getStringExtra("테마");

        if(mode.equals("자유 모드")) {
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

        // GLSurfaceView 사진 찍기
        drawCapture.setOnClickListener(view -> {

            shutterImg.setVisibility(View.VISIBLE);

            captureBitmap(new BitmapReadyCallbacks() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera/";

                    File file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                        Toast.makeText(StickerActivity.this, "폴더가 생성되었습니다.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(StickerActivity.this, "촬영 완료. 갤러리를 확인해주세요 ", Toast.LENGTH_SHORT).show();
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    shutterImg.setVisibility(View.INVISIBLE);
                }
            });
        });

        // 디텍터 리스너 정의
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

            // 이동 // 두번 누르면
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mTouched = true;
                modelInit = false;
                planeCreate = false;
                displayX = e.getX();
                displayY = e.getY();

                Log.d("onDoubleTap 여", displayX + "," + displayY);
                return true;
            }

            class MyGo extends Thread {
                @Override
                public void run() {

                    if(!moving) {
                        moving = true;
                        // 배열 딥카피
                        float[] bufMatrix = modelMatrix.clone();

                        for (int i = 0; i < 200; i++) {
                            Matrix.translateM(modelMatrix, 0, 0, 0, -0.05f); // -10f까지

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

            // 회전 // 드래그 하면
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
                    Log.d("onScroll 여", xMove + "," + yMove);
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

                Log.d("onScale 여", detector.getScaleFactor() + "");

                // 스케일값을 전역변수로 기억
                mScale *= detector.getScaleFactor();
                // 스케일 조절 // 비율임
                Matrix.scaleM(modelMatrix, 0,
                        detector.getScaleFactor(), // x
                        detector.getScaleFactor(), // y
                        detector.getScaleFactor()); // z
                return true;
            }
        });

        //화면 변화 감지 --> 회전 등등
        DisplayManager displayManager = (DisplayManager)getSystemService(DISPLAY_SERVICE);

        if(displayManager != null){

            //화면 리스너 실행
            displayManager.registerDisplayListener(
                    new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {}
                        @Override
                        public void onDisplayRemoved(int displayId) {}
                        //화면이 변경되었다면
                        @Override
                        public void onDisplayChanged(int displayId) {
                            //동기화 --> 변환시 한번되고 난뒤에 작업
                            synchronized (this){
                                //화면 변화를 알려준다
                                mRenderer.onDisplayChanged();
                            }
                        }
                    }, null);
        }

        StickerRenderer.RenderCallBack mr = new StickerRenderer.RenderCallBack() {
            //StickerRenderer의 onDrawFrame() -- 그리기 할때 마다 호출
            //MainActivity에서 카메라 화면 정보를 얻기 위해서 이다.
            @Override
            public void preRender() {

                if(mRenderer.viewportChange){
                    Display display = getWindowManager().getDefaultDisplay();
                    mRenderer.updateSession(mSession, display.getRotation());
                }

                //session의 카메라 텍스처 이름을  StickerRenderer의 카메라의 텍스처 번호로 지정
                // session 카메라 : 입력정보  --> StickerRenderer의 카메라 :  화면에 뿌리는 출력정보
                mSession.setCameraTextureName(mRenderer.getTextureID());
                Frame frame = null;

                try {
                    frame = mSession.update(); //카메라의 화면을 업데이트한다.
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                mRenderer.mCamera.transformDisplayGeometry(frame);

                // 밝기 관련된 코드 // if 안에있던거 밖으로 뺏음 문제있으면 다시 넣기
                LightEstimate estimate = frame.getLightEstimate();
                float lightyIntensity = estimate.getPixelIntensity();

                if(mTouched) {
                    List<HitResult> results = frame.hitTest(displayX, displayY);

                    for (HitResult hr : results) {
                        Pose pose = hr.getHitPose();
                        Trackable trackable = hr.getTrackable();

                        // 클릭 좌표 추적이 Plane이고 Plane의 도형 안에 있어?
                        if(trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(pose)){

                            // 모델 매트릭스 초기화를 한번만 하기위함
                            if(!modelInit) {
                                modelInit = true;

                                pose.toMatrix(modelMatrix, 0);
                                // 다시 생성하면  기억해두었던 회전값을 준다
                                Matrix.rotateM(modelMatrix, 0, mRotate,  0f, 100f, 0f);
                                // 다시 생성하면 기억해두었던 스케일값을 준다
                                Matrix.scaleM(modelMatrix, 0, mScale, mScale, mScale);
                            }

                            // obj 변형
//                            Matrix.translateM(modelMatrix, 0, 0f, 0.3f, 0f); // y축으로 0.3f 이동
//                            Matrix.rotateM(modelMatrix, 0, 45f,  0f, 100f, 0f); // y축으로 45도 회전 // 100f는 큰의미없음
//                            Matrix.rotateM(modelMatrix, 0, 45f,  10f, 0f, 0f); // 이건 x축으로 회전. 인사하는것처럼
//                            Matrix.scaleM(modelMatrix, 0, 1f, 3f, 1f); // 스케일 조절 // 비율임

                            // 밝을때와 어두울때의 재질의 차이가 생김 광택?
                            mRenderer.themeObjs.get(selectedIndex).setLightIntensity(lightyIntensity);

                            // 조명 색상 지정
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
                            // 평면 찾았다
                            mRenderer.mPlane.update(plane);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        Log.d("TrackingState", "평면 찾음");
                                    }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        Log.d("TrackingState", "어딨어");
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

        //pause 시 관련 데이터 사라지지 않게 한다.
        mySurfaceView.setPreserveEGLContextOnPause(true);
        mySurfaceView.setEGLContextClientVersion(3); //버전 3.0 사용

        //렌더링 지정
        mySurfaceView.setRenderer(mRenderer);
        //렌더링 계속 호출
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
            // 안드로이드 풀 스크린 체인지
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
        //퍼미션 요청
        requestPermission();

        //ARCore session 유무 --> 없으면 생성
        if(mSession == null){

            try {

                Log.d("세션 돼냐?",
                        ArCoreApk.getInstance().requestInstall(this, true)+"");

                switch (ArCoreApk.getInstance().requestInstall(this, true)){
                    case INSTALLED:
                        //ARCore  정상 설치후 세션 생성
                        mSession = new Session(this);

                        //ARCore 환경설정용 Config
                        Config config = new Config(mSession);
                        //평면 배치 인식
                        config.setInstantPlacementMode(Config.InstantPlacementMode.LOCAL_Y_UP);
                        mSession.configure(config);

                        Log.d("세션 생성?","생성됐으요");
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

    // 퍼미션
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

        mGestureDetector.onTouchEvent(event); // 이벤트 위임
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
                    // 선택한 이미지에서 비트맵 생성
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    Bitmap img = BitmapFactory.decodeStream(in);
                    in.close();
                    // 이미지 표시
                    //  imageView.setImageBitmap(img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void onBtnClick(View view){
        String toolType = ((Button)view).getText().toString();

        Log.d("하단 클릭임",toolType+"");

        Intent intent;

        switch (toolType){
            case "드로우":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(StickerActivity.this, DrawActivity.class);
                intent.putExtra("모드", mode);
                intent.putExtra("테마", theme);
                startActivity(intent);
                break;
            case "스티커":
                break;
            case "페이스":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(StickerActivity.this, FaceActivity.class);
                intent.putExtra("모드", mode);
                intent.putExtra("테마", theme);
                startActivity(intent);
                break;

            default:
                Toast.makeText(StickerActivity.this,"다시 선택해 주세요",Toast.LENGTH_SHORT).show();
        }
    }
}
