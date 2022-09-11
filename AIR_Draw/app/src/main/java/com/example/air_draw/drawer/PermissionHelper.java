package com.example.air_draw.drawer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
  private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
  private static final int CAMERA_PERMISSION_CODE = 0;

  public static boolean hasCameraPermission(Activity activity) {
    return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED;

  }

  public static void requestCameraPermission(Activity activity) {
    ActivityCompat.requestPermissions(activity, new String[]{CAMERA_PERMISSION},
            CAMERA_PERMISSION_CODE);
  }
}
