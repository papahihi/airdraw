package com.example.air_draw.drawer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;

import com.example.air_draw.HomeActivity;
import com.example.air_draw.filter.FaceActivity;
import com.example.air_draw.R;
import com.example.air_draw.sticker.StickerActivity;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class DrawActivity extends AppCompatActivity implements GLSurfaceView.Renderer, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{
    private static final String TAG = DrawActivity.class.getSimpleName();

    private GLSurfaceView mSurfaceView;

    private Config mDefaultConfig;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private LineShaderRenderer mLineShaderRenderer = new LineShaderRenderer();
    private Frame mFrame;

    private float[] projmtx = new float[16];
    private float[] viewmtx = new float[16];
    private float[] mZeroMatrix = new float[16];

    private boolean mPaused = false;

    private float mScreenWidth = 0;
    private float mScreenHeight = 0;

    private BiquadFilter biquadFilter;
    private Vector3f mLastPoint;
    private AtomicReference<Vector2f> lastTouch = new AtomicReference<>();

    private GestureDetectorCompat mDetector;

    private LinearLayout mSettingsUI;

    private SeekBar mLineWidthBar;
    private SeekBar mLineDistanceScaleBar;
    private SeekBar mSmoothingBar;


    private float mLineWidthMax = 0.33f;
    private float mDistanceScale = 0.0f;
    private float mLineSmoothing = 0.1f;

    private float[] mLastFramePosition;

    private AtomicBoolean bIsTracking = new AtomicBoolean(true);
    private AtomicBoolean bReCenterView = new AtomicBoolean(false);
    private AtomicBoolean bTouchDown = new AtomicBoolean(false);
    private AtomicBoolean bClearDrawing = new AtomicBoolean(false);
    private AtomicBoolean bLineParameters = new AtomicBoolean(false);
    private AtomicBoolean bUndo = new AtomicBoolean(false);
    private AtomicBoolean bNewStroke = new AtomicBoolean(false);

    private ArrayList<ArrayList<Vector3f>> mStrokes;

    private DisplayRotationHelper mDisplayRotationHelper;
    private Snackbar mMessageSnackbar;

    private boolean bInstallRequested;

    private TrackingState mState;
    MediaPlayer mp;
    RelativeLayout r_layout,mToolKit;
    FrameLayout centerBar, loading;
    LinearLayout bottomBar;
    TableLayout tableLayout;
    ImageButton drawcapture, home,topbutton2;
    FrameLayout shutterImg;
    float SScale = 1f;

    String mode, theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        setContentView(R.layout.activity_draw);

        mSurfaceView = findViewById(R.id.surfaceview);
        mSettingsUI = findViewById(R.id.strokeUI);
        mToolKit = (RelativeLayout)findViewById(R.id.ToolKit);
        r_layout = (RelativeLayout)findViewById(R.id.r_Layout);
        centerBar = (FrameLayout)findViewById(R.id.centerBar);
        loading = (FrameLayout) findViewById(R.id.loading);
        bottomBar = (LinearLayout)findViewById(R.id.bottomBar);
        tableLayout = (TableLayout)findViewById(R.id.pallete);
        drawcapture = (ImageButton) findViewById(R.id.drowCapture);
        home = (ImageButton) findViewById(R.id.home);

        mLineDistanceScaleBar = findViewById(R.id.distanceScale);
        mLineWidthBar = findViewById(R.id.lineWidth);
        mSmoothingBar = findViewById(R.id.smoothingSeekBar);
        mp = MediaPlayer.create(DrawActivity.this, R.raw.capture_sound);

        Intent intent = getIntent();
        mode = intent.getStringExtra("모드");
        theme = intent.getStringExtra("테마");

        topbutton2 = (ImageButton)  findViewById(R.id.topbutton2);
        shutterImg = (FrameLayout) findViewById(R.id.shutterImg);

        centerBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);

        mLineDistanceScaleBar.setProgress(sharedPref.getInt("mLineDistanceScale", 1));
        mLineWidthBar.setProgress(sharedPref.getInt("mLineWidth", 10));
        mSmoothingBar.setProgress(sharedPref.getInt("mSmoothing", 50));

        mDistanceScale = LineUtils.map((float) mLineDistanceScaleBar.getProgress(), 0, 100, 1, 200, true);
        mLineWidthMax = LineUtils.map((float) mLineWidthBar.getProgress(), 0f, 100f, 0.1f, 5f, true);
        mLineSmoothing = LineUtils.map((float) mSmoothingBar.getProgress(), 0, 100, 0.01f, 0.2f, true);

        mLineShaderRenderer.setColor(new Vector3f(0f, 0f, 0f));
        topbutton2.setOnClickListener(view -> {
            Intent intent2 = new Intent(Intent.ACTION_VIEW);
            intent2.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            //intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivity(intent2);
        });
        home.setOnClickListener(view -> {

            Intent intent1 = new Intent(DrawActivity.this, HomeActivity.class);
            startActivity(intent1);

        });

        // 팔레트 색깔별로 클릭 리스너 삽입
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow parentRow = (TableRow) tableLayout.getChildAt(i);

            for (int j = 0; j < parentRow.getChildCount(); j++) {
                View pick = parentRow.getChildAt(j);

                pick.setOnClickListener(view -> {
                    String layoutColor = Integer.toHexString(((ColorDrawable)pick.getBackground()).getColor());
                    String hexColor = "#" + layoutColor.substring(2);

                    int r = Integer.parseInt(hexColor.substring(1,3), 16);
                    int g = Integer.parseInt(hexColor.substring(3,5), 16);
                    int b = Integer.parseInt(hexColor.substring(5,7), 16);

                    Vector3f newColor = new Vector3f(r / 255.f, g / 255.f, b / 255.f);

                    AppSettings.setColor(newColor);
                    mLineShaderRenderer.setColor(AppSettings.getColor());
                });
            }
        }

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor editor = sharedPref.edit();

                if (seekBar == mLineDistanceScaleBar) {
                    editor.putInt("mLineDistanceScale", progress);
                    mDistanceScale = LineUtils.map((float) progress, 0f, 100f, 1f, 200f, true);
                } else if (seekBar == mLineWidthBar) {
                    editor.putInt("mLineWidth", progress);
                    mLineWidthMax = LineUtils.map((float) progress, 0f, 100f, 0.1f, 5f, true);
                } else if (seekBar == mSmoothingBar) {
                    editor.putInt("mSmoothing", progress);
                    mLineSmoothing = LineUtils.map((float) progress, 0, 100, 0.01f, 0.2f, true);
                }
                mLineShaderRenderer.bNeedsUpdate.set(true);

                editor.apply();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        mLineDistanceScaleBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mLineWidthBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mSmoothingBar.setOnSeekBarChangeListener(seekBarChangeListener);

        mSettingsUI.setVisibility(View.GONE);

        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
        Matrix.setIdentityM(mZeroMatrix, 0);

        mLastPoint = new Vector3f(0, 0, 0);

        bInstallRequested = false;

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);
        mStrokes = new ArrayList<>();

        drawcapture.setOnClickListener(view -> {

            shutterImg.setVisibility(View.VISIBLE);

            captureBitmap(new DrawActivity.BitmapReadyCallbacks() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera/";

                    File file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                        Toast.makeText(DrawActivity.this, "폴더가 생성되었습니다.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(DrawActivity.this, "촬영 완료. 갤러리를 확인해주세요 ", Toast.LENGTH_SHORT).show();
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    shutterImg.setVisibility(View.INVISIBLE);
                }
            });
        });

    }
    private Bitmap snapshotBitmap;

    private interface BitmapReadyCallbacks {
        void onBitmapReady(Bitmap bitmap);
    }

    private void captureBitmap(final DrawActivity.BitmapReadyCallbacks bitmapReadyCallbacks) {
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                EGL10 egl = (EGL10) EGLContext.getEGL();
                GL10 gl = (GL10) egl.eglGetCurrentContext().getGL();


                snapshotBitmap = createBitmapFromGLSurface(0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight(), gl);

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

    private void addStroke(Vector2f touchPoint) {
        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx);
        addStroke(newPoint);
    }

    private void addPoint(Vector2f touchPoint) {
        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, mScreenWidth, mScreenHeight, projmtx, viewmtx);
        addPoint(newPoint);
    }

    private void addStroke(Vector3f newPoint) {
        biquadFilter = new BiquadFilter(mLineSmoothing);
        for (int i = 0; i < 1500; i++) {
            biquadFilter.update(newPoint);
        }
        Vector3f p = biquadFilter.update(newPoint);
        mLastPoint = new Vector3f(p);
        mStrokes.add(new ArrayList<Vector3f>());
        mStrokes.get(mStrokes.size() - 1).add(mLastPoint);
    }

    private void addPoint(Vector3f newPoint) {
        if (LineUtils.distanceCheck(newPoint, mLastPoint)) {
            Vector3f p = biquadFilter.update(newPoint);
            mLastPoint = new Vector3f(p);
            mStrokes.get(mStrokes.size() - 1).add(mLastPoint);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSession == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !bInstallRequested)) {
                    case INSTALL_REQUESTED:
                        bInstallRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                if (!PermissionHelper.hasCameraPermission(this)) {
                    PermissionHelper.requestCameraPermission(this);
                    return;
                }

                mSession = new Session(/* context= */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

//            // 전면 카메라 사용하기
//            CameraConfigFilter filter =
//                    new CameraConfigFilter(mSession).setFacingDirection(CameraConfig.FacingDirection.FRONT);
//            CameraConfig cameraConfig = mSession.getSupportedCameraConfigs(filter).get(0);
//            mSession.setCameraConfig(cameraConfig);

            Config config = new Config(mSession);
            if (!mSession.isSupported(config)) {
                Log.e(TAG, "Exception creating session Device Does Not Support ARCore", exception);
            }
            mSession.configure(config);
        }
        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mSurfaceView.onResume();
        mDisplayRotationHelper.onResume();
        mPaused = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mDisplayRotationHelper.onPause();
            mSurfaceView.onPause();
            mSession.pause();
        }

        mPaused = false;


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenHeight = displayMetrics.heightPixels;
        mScreenWidth = displayMetrics.widthPixels;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        if (mSession == null) {
            return;
        }

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        mBackgroundRenderer.createOnGlThread(/*context=*/this);

        try {

            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
            mLineShaderRenderer.createOnGlThread(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        mScreenWidth = width;
        mScreenHeight = height;
    }

    private void update() {

        if (mSession == null) {
            return;
        }

        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {

            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

            mFrame = mSession.update();
            Camera camera = mFrame.getCamera();

            mState = camera.getTrackingState();

            if (mState == TrackingState.TRACKING && !bIsTracking.get()) {
                bIsTracking.set(true);
            } else if (mState== TrackingState.STOPPED && bIsTracking.get()) {
                bIsTracking.set(false);
                bTouchDown.set(false);
            }

            camera.getProjectionMatrix(projmtx, 0, AppSettings.getNearClip(), AppSettings.getFarClip());
            camera.getViewMatrix(viewmtx, 0);

            float[] position = new float[3];
            camera.getPose().getTranslation(position, 0);

            if (mLastFramePosition != null) {
                Vector3f distance = new Vector3f(position[0], position[1], position[2]);
                distance.sub(new Vector3f(mLastFramePosition[0], mLastFramePosition[1], mLastFramePosition[2]));

                if (distance.length() > 0.15) {
                    bTouchDown.set(false);
                }
            }
            mLastFramePosition = position;

            Matrix.multiplyMM(viewmtx, 0, viewmtx, 0, mZeroMatrix, 0);

            if (bNewStroke.get()) {
                bNewStroke.set(false);
                addStroke(lastTouch.get());
                mLineShaderRenderer.bNeedsUpdate.set(true);
            } else if (bTouchDown.get()) {
                addPoint(lastTouch.get());
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (bReCenterView.get()) {
                bReCenterView.set(false);
                mZeroMatrix = getCalibrationMatrix();
            }

            if (bClearDrawing.get()) {
                bClearDrawing.set(false);
                clearDrawing();
                mLineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (bUndo.get()) {
                bUndo.set(false);
                if (mStrokes.size() > 0) {
                    mStrokes.remove(mStrokes.size() - 1);
                    mLineShaderRenderer.bNeedsUpdate.set(true);
                }
            }
            mLineShaderRenderer.setDrawDebug(bLineParameters.get());
            if (mLineShaderRenderer.bNeedsUpdate.get()) {
                mLineShaderRenderer.setColor(AppSettings.getColor());
                mLineShaderRenderer.mDrawDistance = AppSettings.getStrokeDrawDistance();
                mLineShaderRenderer.setDistanceScale(mDistanceScale);
                mLineShaderRenderer.setLineWidth(mLineWidthMax);
                mLineShaderRenderer.clear();
                mLineShaderRenderer.updateStrokes(mStrokes);
                mLineShaderRenderer.upload();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mPaused) return;

        update();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mFrame == null) {
            return;
        }

        mBackgroundRenderer.draw(mFrame);

        if (mFrame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            mLineShaderRenderer.draw(viewmtx, projmtx, mScreenWidth, mScreenHeight, AppSettings.getNearClip(), AppSettings.getFarClip());
        }
    }

    public float[] getCalibrationMatrix() {
        float[] t = new float[3];
        float[] m = new float[16];

        mFrame.getCamera().getPose().getTranslation(t, 0);
        float[] z = mFrame.getCamera().getPose().getZAxis();
        Vector3f zAxis = new Vector3f(z[0], z[1], z[2]);
        zAxis.y = 0;
        zAxis.normalize();

        double rotate = Math.atan2(zAxis.x, zAxis.z);

        Matrix.setIdentityM(m, 0);
        Matrix.translateM(m, 0, t[0], t[1], t[2]);
        Matrix.rotateM(m, 0, (float) Math.toDegrees(rotate), 0, 1, 0);
        return m;
    }

    public void clearDrawing() {
        mStrokes.clear();
        mLineShaderRenderer.clear();
    }

    public void onClickUndo(View button) {
        bUndo.set(true);
    }

    public void onClickLineDebug(View button) {
        bLineParameters.set(!bLineParameters.get());
    }

    public void onClickSettings(View button) {
        Button settingsButton = findViewById(R.id.settingsButton);

        if (mSettingsUI.getVisibility() == View.GONE) {
            mSettingsUI.setVisibility(View.VISIBLE);
            mLineDistanceScaleBar = findViewById(R.id.distanceScale);
            mLineWidthBar = findViewById(R.id.lineWidth);

//            settingsButton.setColorFilter(getResources().getColor(R.color.active));
        } else {
            mSettingsUI.setVisibility(View.GONE);
//            settingsButton.setColorFilter(getResources().getColor(R.color.gray));
        }
    }

    public void onClickClear(View button) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("모든 그림을 지우시겠습니까?");

        builder.setPositiveButton("네 ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bClearDrawing.set(true);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onClickRecenter(View button) {
        bReCenterView.set(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent tap) {
        this.mDetector.onTouchEvent(tap);

        if (tap.getAction() == MotionEvent.ACTION_DOWN ) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            bTouchDown.set(true);
            bNewStroke.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_MOVE || tap.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            bTouchDown.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_UP || tap.getAction() == MotionEvent.ACTION_CANCEL) {
            bTouchDown.set(false);
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            return true;
        }

        return super.onTouchEvent(tap);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
//        if (mButtonBar.getVisibility() == View.GONE) {
//            mButtonBar.setVisibility(View.VISIBLE);
//        } else {
//            mButtonBar.setVisibility(View.GONE);
//        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent tap) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void onBtnClick(View view){
        String toolType = ((Button)view).getText().toString();

        Intent intent;

        switch (toolType){
            case "드로우":
                break;
            case "스티커":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(DrawActivity.this, StickerActivity.class);
                intent.putExtra("모드", mode);
                intent.putExtra("테마", theme);
                startActivity(intent);
                break;
            case "페이스":
                loading.setVisibility(View.VISIBLE);
                intent = new Intent(DrawActivity.this, FaceActivity.class);
                intent.putExtra("모드", mode);
                intent.putExtra("테마", theme);
                startActivity(intent);
                break;

            default:
                Toast.makeText(DrawActivity.this,"다시 선택해 주세요",Toast.LENGTH_SHORT).show();
        }
    }

}

