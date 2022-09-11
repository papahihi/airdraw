package com.example.air_draw;

import android.content.Intent;

import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.air_draw.drawer.DrawActivity;
import com.example.air_draw.sticker.StickerActivity;
import com.google.ar.core.Session;

import java.util.ArrayList;

public class ThemeSelectActivity  extends AppCompatActivity {

    RelativeLayout r_layout;
    FrameLayout loading;
    Button pokemon, amongus;

    String mode,theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hidStatusBarTitleBar();
        setContentView(R.layout.activity_themesel);

        r_layout = (RelativeLayout)findViewById(R.id.r_Layout);
        loading = (FrameLayout) findViewById(R.id.loading);
        pokemon = (Button)findViewById(R.id.pokemon);
        amongus = (Button)findViewById(R.id.amongus);

        Intent intent = getIntent();
        mode = intent.getStringExtra("모드");

        findViewById(R.id.themeModeSel).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    void hidStatusBarTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    public void onBtnClick(View view){
        theme  = view.getResources().getResourceEntryName(view.getId());
        loading.setVisibility(View.VISIBLE);
        Intent intent;
        intent = new Intent(ThemeSelectActivity.this, StickerActivity.class);
        intent.putExtra("모드", mode);
        intent.putExtra("테마", theme);
        startActivity(intent);
    }
}
