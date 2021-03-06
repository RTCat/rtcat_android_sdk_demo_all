package com.shishimao.android.demo.advanced_share_screen;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;
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
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Advanced-Share-Screen";
    private static final int MAX_NUMBER = 4;
    private ArrayList<VideoPlayer> videoPlayers = new ArrayList<>();

    private LayoutEmpty[] layoutEmpties = new LayoutEmpty[MAX_NUMBER];
    RelativeLayout videoContainer;

    //rtcat
    RTCat cat;
    LocalStream localStream;
    String token;
    Session session;

    Intent screenIntent;

    private static final int PERMISSION_REQUEST_CODE = 777;
    private static final int SCREEN_REQUEST_CODE = 751;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        for(int i = 0; i < layoutEmpties.length;i++){
            layoutEmpties[i] = new LayoutEmpty();
        }

        videoContainer = (RelativeLayout)findViewById(R.id.video_container);

        String[] permissions = {
                android.Manifest.permission.RECORD_AUDIO,
        };

        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT >= 23) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }else {
            start();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode== PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SCREEN_REQUEST_CODE){
            Log.i(TAG,"result " + resultCode);
            if(resultCode == RESULT_OK){
                screenIntent = data;
                cat = new RTCat(this,true,true,true,true, AppRTCAudioManager.AudioDevice.SPEAKER_PHONE, RTCat.CodecSupported.H264, L.DEBUG);
                cat.addObserver(new RTCat.RTCatObserver() {
                    @Override
                    public void init() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                createScreenStream();
                            }
                        });
                    }
                    @Override
                    public void error(Errors error) {

                    }
                });
                cat.init();
            }
        }
    }

    private void start(){
        MediaProjectionManager manager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, SCREEN_REQUEST_CODE);
    }

    public void createScreenStream(){
        localStream = cat.createStream(true,true,15,1024,1024,screenIntent);
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
                        createVideoPlayer(localStream,"self");
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

    private void createVideoPlayer(AbstractStream stream, String id){
        int ePos = -1;
        for(int i = 0 ; i < layoutEmpties.length ; i ++){
            if(layoutEmpties[i].isEmpty){
                ePos = i;
                break;
            }
        }

        VideoPlayerLayout videoLayout = new VideoPlayerLayout(this);
        VideoPlayer videoPlayer = new VideoPlayer(this);
        cat.initVideoPlayer(videoPlayer);

        if(ePos >= 0){
            PosSize ps = getPosition(ePos);
            l("create video player in " + ePos + " pos : " + ps);
            videoLayout.setPosition(ps.x,ps.y,ps.w,ps.h);
            videoLayout.addView(videoPlayer);
            videoContainer.addView(videoLayout);
            stream.play(videoPlayer);

            videoPlayers.add(videoPlayer);
//            renderMap.put(id,videoLayout);
            layoutEmpties[ePos].layout = videoLayout;
            layoutEmpties[ePos].token = id;
            layoutEmpties[ePos].isEmpty = false;
        }else {
            t("布局空间已满");
        }
    }

    private PosSize getPosition(int n){
        int w,h;
        int x,y;
        w = h = 50;
        y = n / 2 * h;
        x = n % 2 * w;
        return new PosSize(x,y,w,h);
    }

    private void removeVideoPlayer(String token){
        for(int i = 0 ;i < layoutEmpties.length;i++){
            if(layoutEmpties[i].token != null && layoutEmpties[i].token.equals(token)){
                l("remove video player in " + i);
                videoContainer.removeView(layoutEmpties[i].layout);
                layoutEmpties[i] = new LayoutEmpty();
                break;
            }
        }
    }


    private void t(String s){
        Toast.makeText(this,s,Toast.LENGTH_LONG).show();
    }

    private void l(String s){
        Log.i(TAG,s);
    }



    class LayoutEmpty{
        VideoPlayerLayout layout = null;
        boolean isEmpty = true;
        String token;
    }


    class PosSize{
        int x;
        int y;
        int w;
        int h;
        PosSize(int x,int y,int w,int h){
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        @Override
        public String toString() {
            return "x :" + x + " y: " + y + " width: " + w + " height: " + h;
        }
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


        for (VideoPlayer renderer:videoPlayers)
        {
            renderer.release();
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    removeVideoPlayer(token);
                }
            });

        }

        @Override
        public void connected(ArrayList wits) {
            l("connected main");

            if(wits.size() >= MAX_NUMBER ){
                finish();
            }

            JSONObject attr = new JSONObject();
            session.send(localStream,true,attr);
        }

        @Override
        public void remote(final Receiver receiver) {
            try {
                receiver.addObserver(new Receiver.ReceiverObserver() {
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
                    public void error(Errors errors) {

                    }

                    @Override
                    public void stream(final RemoteStream stream) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                createVideoPlayer(stream,receiver.getSenderToken());
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

    }

}
