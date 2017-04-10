package com.shishimao.android.demo.message;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.shishimao.android.demo.config.Config;
import com.shishimao.sdk.Errors;
import com.shishimao.sdk.RTCat;
import com.shishimao.sdk.Receiver;
import com.shishimao.sdk.RemoteStream;
import com.shishimao.sdk.Sender;
import com.shishimao.sdk.Session;
import com.shishimao.sdk.apprtc.AppRTCAudioManager;
import com.shishimao.sdk.http.RTCatRequests;
import com.shishimao.sdk.log.WebRTCLog;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final static String TAG  = "Message";

    //webrtc
    RTCat cat;
    Session session;

    HashMap<String,Sender> senders = new HashMap<>();

    public String token;

    public String messageToken;

    public EditText editText;
    public TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //webrtc

        editText = (EditText)findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);

        cat = new RTCat(this);
        cat.addObserver(new RTCat.RTCatObserver() {
            @Override
            public void init() {
                createSession(null);
            }

            @Override
            public void error(Errors errors) {

            }
        });
        cat.init();

    }

    public void sendMessage(View view){
        String message = editText.getText().toString().trim();
        textView.append("self :" + message + "\r\n");
        editText.setText("");
        for(Map.Entry<String, Sender> entry : senders.entrySet()) {
            String key = entry.getKey();
            Sender value = entry.getValue();
            value.sendMessage(message);
            // do what you have to do here
            // In your case, an other loop.
        }
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

        if(cat != null)
        {
            cat.release();
        }

        Log.d("Test","EXIT");

        super.onDestroy();

    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    class SessionHandler implements Session.SessionObserver {
        @Override
        public void in(String token) {
            messageToken = token;
            l(token + " is in");
            l(String.valueOf(session.getWits().size()));

            session.sendTo(null,true,null, token);

        }

        @Override
        public void close() {
        }

        @Override
        public void out(String token) {

        }

        @Override
        public void connected(ArrayList wits) {
            l("connected main");
            JSONObject attr = new JSONObject();
            session.send(null,true,attr);
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

                    }

                    @Override
                    public void message(final String message) {
                        Log.d(TAG,message);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.append(receiver.getOpposite().substring(0,7) + ":" + message + "\r\n");
                            }
                        });
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
            senders.put(sender.getId(), sender);
            sender.addObserver(new Sender.SenderObserver() {
                @Override
                public void log(WebRTCLog.SenderClientLog senderClientLog) {

                }

                @Override
                public void fileSendFinished() {

                }

                @Override
                public void close() {
                    senders.remove(sender);
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
