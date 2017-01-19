package com.shishimao.android.demo.log_and_error;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.shishimao.android.sdk.Config;
import com.shishimao.sdk.AbstractStream;
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

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Multi-Party";
    private static final int MAX_NUMBER = 2;

    //rtcat
    RTCat cat;
    LocalStream localStream;
    String token;
    Session session;


    VideoPlayer localVideoPlayer;
    VideoPlayer remoteVideoPlayer;
    TextView localLogText;
    TextView remoteLogText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        localVideoPlayer = (VideoPlayer)findViewById(R.id.local_video_player);
        remoteVideoPlayer = (VideoPlayer)findViewById(R.id.remote_video_player);
        localLogText = (TextView)findViewById(R.id.local_log_text);
        remoteLogText = (TextView)findViewById(R.id.remote_log_text);


        cat = new RTCat(MainActivity.this,true,true,true,false, AppRTCAudioManager.AudioDevice.SPEAKER_PHONE, RTCat.CodecSupported.VP8, L.VERBOSE);

        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void init() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cat.initVideoPlayer(localVideoPlayer);
                        cat.initVideoPlayer(remoteVideoPlayer);
                        createLocalStream();
                    }
                });
            }

            @Override
            public void error(Errors errors) {

            }
        });

        cat.init();
    }

    public void createLocalStream(){
        localStream = cat.createStream(true,true,10,120,90, LocalStream.CameraFacing.FRONT);
        localStream.addObserver(new LocalStream.StreamObserver() {

            @Override
            public void error(Errors errors) {

            }

            @Override
            public void afterSwitch(boolean isFrontCamera) {}

            @Override
            public void accepted() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        localStream.play(localVideoPlayer);
                        createSession();
                    }
                });

            }
        });

        localStream.init();
    }



    public void createSession()
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


    private void t(String s){
        Toast.makeText(this,s,Toast.LENGTH_LONG).show();
    }

    private void l(String s){
        Log.i(TAG,s);
    }

    private String handleLog(WebRTCLog.ClientLog log){
        String rv = "\n";
        try{
            for(Field field:log.getClass().getFields()){
                rv = rv + field.getName() + ": " + field.get(log) + "\n";
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }

        return  rv;
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


        if(cat != null)
        {
            cat.release();
        }

        l("EXIT");

        super.onDestroy();

    }

    class SessionHandler implements Session.SessionObserver {
        @Override
        public void in(String token) {
            l(token + " is in");
            JSONObject attr = new JSONObject();
            session.sendTo(localStream,true,attr, token);
        }

        @Override
        public void close() {
            finish();
        }

        @Override
        public void out(final String token) {


        }

        @Override
        public void connected(ArrayList wits) {
            l("connected main");

            if(wits.size() >= MAX_NUMBER ){
                finish();
                t("会话内人数已满");
            }

            JSONObject attr = new JSONObject();
            session.send(localStream,true,attr);
        }

        @Override
        public void remote(final Receiver receiver) {
            try {
                receiver.addObserver(new Receiver.ReceiverObserver() {
                    @Override
                    public void log(final WebRTCLog.ReceiverClientLog receiverClientLog) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                remoteLogText.setText(handleLog(receiverClientLog));
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
                            stream.play(remoteVideoPlayer);
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
                public void log(final WebRTCLog.SenderClientLog senderClientLog) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            localLogText.setText(handleLog(senderClientLog));
                        }
                    });
                }

                @Override
                public void fileSendFinished() {

                }

                @Override
                public void close() {
                }

                @Override
                public void error(Errors errors) {

                }
            });
        }

        @Override
        public void message(String token, String message) {
        }

        @Override
        public void error(Errors errors) {

        }

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
}
