package com.shishimao.android.demo.advanced_codec;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.shishimao.android.sdk.Config;
import com.shishimao.sdk.Configs;
import com.shishimao.sdk.Errors;
import com.shishimao.sdk.LocalStream;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.RemoteStream;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.WebRTCLog;
import com.shishimao.sdk.apprtc.AppRTCAudioManager;
import com.shishimao.sdk.http.RTCatRequests;
import com.shishimao.sdk.tools.L;
import com.shishimao.sdk.view.VideoPlayer;
import com.shishimao.sdk.view.VideoPlayerLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Advanced-Codec";

    //layout
    VideoPlayerLayout localRenderLayout;
    VideoPlayer localVideoRenderer;

    VideoPlayerLayout remoteRenderLayout;
    VideoPlayer remoteVideoRenderer;

    TextView textVP8;
    TextView textVP9;
    TextView textH264;

    RadioButton rbtVP8;
    RadioButton rbtVP9;
    RadioButton rbtH264;

    RadioGroup radioGroup;
    Button btConnect;

    TextView textCodec;

    //rtCat
    RTCat.CodecSupported codec = RTCat.CodecSupported.VP8;
    RTCat cat;
    LocalStream localStream;
    String token;
    Session session;
    String messageToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permssions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
        };
        ActivityCompat.requestPermissions(this,permssions, 111);

        localRenderLayout = (VideoPlayerLayout)findViewById(R.id.local_video_layout);
        remoteRenderLayout = (VideoPlayerLayout)findViewById(R.id.remote_video_layout);
        localRenderLayout.setPosition(0,0,50,100);
        remoteRenderLayout.setPosition(50,0,50,100);
        localVideoRenderer = (VideoPlayer)findViewById(R.id.local_video_render);
        remoteVideoRenderer = (VideoPlayer)findViewById(R.id.remote_video_render);

        textVP8 = (TextView)findViewById(R.id.text_vp8);
        textVP9 = (TextView)findViewById(R.id.text_vp9);
        textH264 = (TextView)findViewById(R.id.text_h264);
        textCodec = (TextView)findViewById(R.id.text_codec);

        rbtVP8 = (RadioButton)findViewById(R.id.rbt_vp8);
        rbtVP9 = (RadioButton)findViewById(R.id.rbt_vp9);
        rbtH264 = (RadioButton)findViewById(R.id.rbt_h264);

        radioGroup = (RadioGroup)findViewById(R.id.rgp_codec);
        btConnect = (Button)findViewById(R.id.bt_connect);

        if(!RTCat.isVp8HwEncodeSupported()){
            textVP8.setText("VP8:否");
            radioGroup.removeView(rbtVP8);
            rbtVP9.setChecked(true);
            codec = RTCat.CodecSupported.VP9;
        }

        if(!RTCat.isVp9HwEncodeSupported()){
            textVP9.setText("VP9:否");
            radioGroup.removeView(rbtVP9);
            if(rbtVP9.isChecked()){
                rbtH264.setChecked(true);
                codec = RTCat.CodecSupported.H264;
            }
        }

        if(!RTCat.isH264HwEncodeSupported()){
            textH264.setText("H264:否");
            radioGroup.removeView(rbtH264);
            if(rbtH264.isChecked()){
                codec = RTCat.CodecSupported.VP8;
            }
        }

    }


    public void onRadioButtonClick(View view){
        boolean checked = ((RadioButton) view).isChecked();

        if(checked){
            switch (view.getId()){
                case R.id.rbt_vp8:
                    codec = RTCat.CodecSupported.VP8;
                    break;
                case R.id.rbt_vp9:
                    codec = RTCat.CodecSupported.VP9;
                    break;
                case R.id.rbt_h264:
                    codec = RTCat.CodecSupported.H264;
                    break;
            }
            t("You choose " + codec.toString());
        }
    }

    public void onClickConnect(View view){
        rbtVP8.setEnabled(false);
        rbtVP9.setEnabled(false);
        rbtH264.setEnabled(false);
        btConnect.setEnabled(false);

        cat = new RTCat(this,true,true,true,false, AppRTCAudioManager.AudioDevice.SPEAKER_PHONE,codec, L.VERBOSE);
        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void error(Errors errors) {

            }

            @Override
            public void init() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createLocalStream();
                    }
                });
            }
        });

        cat.init();
    }

    public void createLocalStream(){
        cat.initVideoPlayer(localVideoRenderer);
        cat.initVideoPlayer(remoteVideoRenderer);

        localStream = cat.createStream(true,true,15,RTCat.VideoFormat.Lv3, LocalStream.CameraFacing.FRONT);
        localStream.addObserver(new LocalStream.StreamObserver() {

            @Override
            public void error(Errors errors) {

            }

            @Override
            public void afterSwitch(boolean isFrontCamera) {}

            @Override
            public void accepted() {
                localStream.play(localVideoRenderer);
                createSession(null);
            }
        });

        localStream.init();
    }


    public void createSession(View view)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    RTCatRequests requests = new RTCatRequests(Config.APIKEY, Config.SECRET);
                    token = requests.getToken(Config.SESSION, "pub");
                    l("token is " + token);
                    session = cat.createSession(token, Session.SessionType.P2P);

                    SessionHandler sh = new SessionHandler();

                    session.addObserver(sh);

                    session.connect();

                } catch (Exception e) {
                    l(e.toString());
                }
            }
        }).start();

    }

    public void l(String o)
    {

        Log.d(TAG, o);
    }


    public void t(String o)
    {
        Toast.makeText(this, o,
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {

        if(session != null)
        {
            session.disconnect();
        }


        if(localStream != null)
        {
            localStream.dispose();
        }

        if(localVideoRenderer != null)
        {
            localVideoRenderer.release();
            localVideoRenderer = null;
        }

        if(remoteVideoRenderer != null){
            remoteVideoRenderer.release();
            remoteRenderLayout = null;
        }

        if(cat != null)
        {
            cat.release();
        }

        Log.d("Test","EXIT");

        super.onDestroy();

    }

    @Override
    protected void onStop() {
        if(localStream != null)
        {
            localStream.stop();
        }

        super.onStop();
    }

    @Override
    protected void onResume() {
        if(localStream != null)
        {
            localStream.start();
        }
        super.onResume();
    }

    class SessionHandler implements Session.SessionObserver {
        @Override
        public void in(String token) {
            messageToken = token;
            l(token + " is in");
            l(String.valueOf(session.getWits().size()));

            if (session.getWits().size() == 1)
            {
                JSONObject attr = new JSONObject();
                session.sendTo(localStream,true,attr, token);
            }
        }

        @Override
        public void close() {
            finish();
        }

        @Override
        public void out(String token) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //TODO
                    remoteVideoRenderer.release();
                }
            });
        }

        @Override
        public void connected(ArrayList wits) {
            l("connected main");

            JSONObject attr = new JSONObject();

            if(wits.size() == 1)
                session.send(localStream,true,attr);
        }

        @Override
        public void remote(final Receiver receiver) {
            try {
                receiver.addObserver(new Receiver.ReceiverObserver() {
                    @Override
                    public void log(WebRTCLog.ReceiverClientLog receiverClientLog) {
                        Log.d("Receiver Log ->",receiverClientLog.toString());
                        final String codecName = receiverClientLog.videoCodecName;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textCodec.setText("对方编码: " + codecName);
                            }
                        });
                    }

                    @Override
                    public void receiveFile(String s) {

                    }

                    @Override
                    public void receiveFileFinish(File file) {

                    }

                    @Override
                    public void error(Errors errors) {

                    }

                    @Override
                    public void stream(final RemoteStream stream) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                t(receiver.getFrom() + " stream");

                                stream.play(remoteVideoRenderer);
                            }
                        });

                    }

                    @Override
                    public void message(String message) {

                    }

                    @Override
                    public void close() {

                    }
                });

                receiver.response();
            } catch (Exception e) {
                l(e.toString());
            }


        }

        @Override
        public void local(final Sender sender) {

            sender.addObserver(new Sender.SenderObserver() {
                @Override
                public void log(WebRTCLog.SenderClientLog senderClientLog) {

                }

                @Override
                public void fileSendFinished() {

                }

                @Override
                public void close() {
                    if(session.getState() == Configs.ConnectState.CONNECTED)
                    {
                        session.sendTo(localStream,false,null,sender.getTo());
                    }
                }

                @Override
                public void error(Errors errors) {

                }
            });
        }

        @Override
        public void message(String token, String message) {
            l(token + ":" +message);
        }

        @Override
        public void error(Errors errors) {

        }
    }

}
