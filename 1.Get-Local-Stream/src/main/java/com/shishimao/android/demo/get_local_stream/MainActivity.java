package com.shishimao.android.demo.get_local_stream;

import android.Manifest;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.shishimao.sdk.Errors;
import com.shishimao.sdk.LocalStream;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.audio.RTCatAudioManager;
import com.shishimao.sdk.utils.RTCatLogging;
import com.shishimao.sdk.view.VideoPlayer;

import org.webrtc.RendererCommon;

public class MainActivity extends AppCompatActivity {

    RTCat cat;
    LocalStream localStream;
    VideoPlayer videoPlayer;
    ImageView iv;
    Button btGetStream;
    Button btSwitchCamera;
    Button btTakePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this,permissions, 111);

        iv=(ImageView)findViewById(R.id.img_pic);
        videoPlayer =(VideoPlayer)findViewById(R.id.video_player);
        videoPlayer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        btGetStream = (Button) findViewById(R.id.bt_get_local_stream);
        btSwitchCamera = (Button) findViewById(R.id.bt_switch_camera);
        btTakePicture = (Button) findViewById(R.id.bt_take_picture);


        cat = new RTCat(MainActivity.this,true,true,true,false, RTCatAudioManager.AudioDevice.SPEAKER_PHONE,RTCat.CodecSupported.H264, RTCatLogging.VERBOSE);
        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void error(Errors errors) {

            }

            @Override
            public void init() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btGetStream.setEnabled(true);
                    }
                });
            }
        });
        cat.init();
    }

    public void createStream(View view){
        btGetStream.setEnabled(false);
//        cat.initVideoPlayer(videoPlayer);

        localStream = cat.createStream(true,true,15, LocalStream.VideoFormat.Lv9,LocalStream.CameraFacing.FRONT);

        localStream.addObserver(new LocalStream.StreamObserver() {
            @Override
            public void afterSwitch(boolean b) {

            }

            @Override
            public void error(Errors errors) {

            }

            @Override
            public void accepted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btTakePicture.setEnabled(true);
                        btSwitchCamera.setEnabled(true);
                        localStream.play(videoPlayer);
                    }
                });
            }
        });

        localStream.init();
    }


    public void switchCamera(View view){
        localStream.switchCamera();
    }

    public void takePicture(View view)
    {
        localStream.takePicture(new LocalStream.CaptureCallback() {
            @Override
            public void onCapture(final Bitmap bm) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv.setImageBitmap(bm);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(localStream != null){
            localStream.release();
            localStream = null;
        }

        if(videoPlayer != null)
        {
            videoPlayer.release();
            videoPlayer = null;
        }

        if (cat !=null){
            cat.release();
            cat = null;
        }
    }
}
