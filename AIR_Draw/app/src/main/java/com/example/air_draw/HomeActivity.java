package com.example.air_draw;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.air_draw.drawer.DrawActivity;
import com.example.air_draw.sticker.StickerActivity;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hidStatusBarTitleBar();
        setContentView(R.layout.activity_home);

        Button themeModeBtn = findViewById(R.id.homebutton);
        Button recogModeBtn = findViewById(R.id.homebutton2);
        Button freeModeBtn = findViewById(R.id.homebutton3);

        ArrayList<Button> btns = new ArrayList<>();
        btns.add(themeModeBtn);
        btns.add(recogModeBtn);
        btns.add(freeModeBtn);

        for (Button btn: btns) {
//            btn.getText();
            Class modeSel;
            switch (btn.getText().toString()){
                case "선택 모드":
                    modeSel = ThemeSelectActivity.class;
                    break;
                case "인식 모드":
                    modeSel = QRActivity.class;
                    break;
                case "자유 모드":
                    modeSel = DrawActivity.class;
                    break;

                default:
                    modeSel = HomeActivity.class;
            }
            btn.setOnClickListener(view -> {
                Intent intent = new Intent(HomeActivity.this, modeSel);
                intent.putExtra("모드", ((Button)view).getText().toString());
                startActivity(intent);
            });
        }

//        freeModeBtn.setOnClickListener(view -> {
//            Intent intent = new Intent(HomeActivity.this, DrawActivity.class);
//            startActivity(intent);
//        });
    }

    void hidStatusBarTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }
}
