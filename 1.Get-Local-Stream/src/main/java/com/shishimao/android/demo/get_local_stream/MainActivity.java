package com.shishimao.android.demo.get_local_stream;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.shishimao.sdk.Errors;
import com.shishimao.sdk.RTCat;

public class MainActivity extends AppCompatActivity {

    RTCat cat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cat = new RTCat(this);
        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void init() {

            }

            @Override
            public void error(Errors errors) {

            }
        });
        cat.init();
    }
}
