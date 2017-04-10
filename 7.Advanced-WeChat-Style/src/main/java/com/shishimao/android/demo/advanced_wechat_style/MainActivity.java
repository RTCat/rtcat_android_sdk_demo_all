package com.shishimao.android.demo.advanced_wechat_style;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.shishimao.android.demo.config.Config;
import com.shishimao.sdk.Errors;
import com.shishimao.sdk.LocalStream;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.RemoteStream;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.apprtc.AppRTCAudioManager;
import com.shishimao.sdk.audio.RTCatAudioManager;
import com.shishimao.sdk.http.RTCatRequests;
import com.shishimao.sdk.log.WebRTCLog;
import com.shishimao.sdk.utils.RTCatLogging;
import com.shishimao.sdk.view.VideoPlayer;
import com.shishimao.sdk.view.VideoPlayerLayout;

import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private final static String TAG  = "Advanced-WeChat-Sytle";

    VideoPlayerLayout localRenderLayout;
    VideoPlayer localVideoPlayer;
    VideoPlayerLayout remoteRenderLayout;
    VideoPlayer remoteVideoPlayer;
    //webrtc
    RTCat cat;
    LocalStream localStream;
    Session session;

    public String token;
    boolean isRemotePlay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        localVideoPlayer = (VideoPlayer) findViewById(R.id.local_video_render);
//        localVideoPlayer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localRenderLayout = (VideoPlayerLayout) findViewById(R.id.local_video_layout);
        localRenderLayout.setPosition(0,0,100,100);

        remoteVideoPlayer = (VideoPlayer) findViewById(R.id.remote_video_render);
        remoteRenderLayout = (VideoPlayerLayout) findViewById(R.id.remote_video_layout);
        remoteRenderLayout.setPosition(0,0,0,0);

        cat = new RTCat(MainActivity.this,true,true,true,false, RTCatAudioManager.AudioDevice.SPEAKER_PHONE ,RTCat.CodecSupported.VP8, RTCatLogging.VERBOSE);

        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void init() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
        cat.initVideoPlayer(localVideoPlayer);

        localStream = cat.createStream(true,true,15, LocalStream.VideoFormat.Lv0, LocalStream.CameraFacing.FRONT);

        localStream.addObserver(new LocalStream.StreamObserver() {
            @Override
            public void error(Errors errors) {

            }

            @Override
            public void afterSwitch(boolean isFrontCamera) {}

            @Override
            public void accepted() {
                localStream.play(localVideoPlayer);
                createSession();
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

                    session.addObserver(new Session.SessionObserver() {
                        @Override
                        public void in(String token) {
                            l(token + " is in");
                            l(String.valueOf(session.getWits().size()));

                            JSONObject attr = new JSONObject();
                            session.sendTo(localStream,true,attr, token);

                        }

                        @Override
                        public void close() {
                            finish();
                        }

                        @Override
                        public void out(final String token) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    l(token + " is out");
                                    remoteRenderLayout.setPosition(0,0,0,0);
                                    localRenderLayout.setPosition(0,0,100,100);
                                    remoteVideoPlayer.requestLayout();
                                    remoteVideoPlayer.release();
                                    isRemotePlay = false;
                                }
                            });
                        }

                        @Override
                        public void connected(final ArrayList wits) {
                            l("connected main");

                            JSONObject attr = new JSONObject();
                            if(wits.size() == 1)
                                session.send(localStream,true,attr);
                        }

                        @Override
                        public void remote(final Receiver receiver) {
                            l("get receiver");
                            try {
                                receiver.addObserver(new Receiver.ReceiverObserver() {
                                    @Override
                                    public void error(Errors errors) {

                                    }


                                    @Override
                                    public void log(WebRTCLog.ReceiverClientLog receiverClientLog) {

                                    }

                                    @Override
                                    public void receiveFile(String s) {

                                    }

                                    @Override
                                    public void receiveFileFinish(File file) {

                                    }

                                    @Override
                                    public void stream(final RemoteStream stream) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if(isRemotePlay)
                                                    return;
                                                t(receiver.getOpposite() + " stream");
                                                cat.initVideoPlayer(remoteVideoPlayer);
                                                remoteRenderLayout.setPosition(0,0,100,100);
                                                localRenderLayout.setPosition(60,0,40,40);
                                                localVideoPlayer.setZOrderMediaOverlay(true);
                                                localVideoPlayer.requestLayout();
                                                remoteVideoPlayer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                                                stream.play(remoteVideoPlayer);
                                                isRemotePlay = true;
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
                    });

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
        Toast.makeText(MainActivity.this, o,
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {

        if(localStream != null)
        {
            localStream.dispose();
        }

        if(session != null)
        {
            session.disconnect();
        }


        if(localVideoPlayer != null)
        {
            localVideoPlayer.release();
            localVideoPlayer = null;
        }

        remoteVideoPlayer.release();

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
}
