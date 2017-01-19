package com.shishimao.android.demo.advanced_share_screen;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        requestPer();
    }

    public void requestPer(){
        if(Build.VERSION.SDK_INT >= 23) {
            String[] permssions = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            requestPermissions(permssions, 111);
        }
    }


    public void start(View view){
        startActivity(new Intent(this,MainActivity.class));
    }

}
