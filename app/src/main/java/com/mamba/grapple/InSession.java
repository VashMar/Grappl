package com.mamba.grapple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.android.gms.maps.MapFragment;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class InSession extends Activity {

    final int MS_IN_MIN = 60000; // ms in a minute

//    TextView textViewTime;
    Button btnPause;
    Button btnStop;
    TextView currentUsername;
    TextView otherUsername;

    ImageView currentUserPic;
    ImageView otherUserPic;

    UserObject otherUser;
    UserObject currentUser;
    UserObject tutor;

    private SessionCounter timer;
    private long sessionRemaining = MS_IN_MIN * 30;    // set from tutor's set max (default 30 min)
    private boolean sessionPaused = false;
    private long sessionLengthMS;
    int sessionLength; // length of entire session

    // management objects
    PicManager picManager;
    LoginManager session;


    // tracks background updates
    private List<String> updates;


    // service related variables
    private boolean mBound = false;
    DBService mService;

    ArcProgress arcProgress;

    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v("In session", "Service Connected");
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            mService.inView();
            mBound = true;

            updates = mService.getUpdates();

            if(timer == null){
               intializeSession();
           }


            // check for background updates
            if(!updates.isEmpty()){
                if(updates.contains("ENDED_SESSION")){
                    sessionEndedAlert(mService.getSeshTime());
                }
                mService.clearUpdates();
            }



        }
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    // receiver to handle server responses for this activity
    private BroadcastReceiver seshReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("In Session Activity", "received response: " + responseType);
                if(responseType.equals("sessionEnded")){
                    // notify the user
                    sessionEndedAlert(extras.getLong("seshTime"));
                }
            }
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insession);
        getActionBar().show();
        getActionBar().setTitle("Session In Progress..");

        picManager = new PicManager(getApplicationContext());

        // get the latest session data
        session = new LoginManager(getApplicationContext());

        btnStop = (Button) findViewById(R.id.endBtn);
        btnPause = (Button) findViewById(R.id.pauseBtn);
        arcProgress = (ArcProgress) findViewById(R.id.arcProgress);


        btnPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if the session is already paused resume it
                if (sessionPaused) {
                    startCountdown();
                    btnPause.setText("Pause Session");
                    sessionPaused = false;

                } else {
                    timer.cancel();
                    btnPause.setText("Resume Session");
                    sessionPaused = true;
                }
            }

        });


        btnStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                endSessionPrompt();
            }
        });


    }


    @Override
    public void onStart() {
        super.onStart();
        Log.v("In session", "Started");
        session = new LoginManager(getApplicationContext());
        currentUser = session.getCurrentUser();
        createService();


        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(seshReceiver,
                new IntentFilter("seshReceiver"));

    }


    @Override
    protected void onStop(){
        super.onStop();
        Log.v("In session", "Stopped");
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(seshReceiver);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.v("Meetup", "Out of View");
        mService.outOfView();
    }



    public void onBackPressed(){
        endSessionPrompt();
    }

    public void createService(){
        Log.v("Waiting Page", "Creating Service..");
        startService(new Intent(this, DBService.class));
        bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Results bound to new service");
    }


    private void intializeSession(){
        otherUser = mService.getGrappledUser();
        currentUser = session.getCurrentUser();

        otherUserPic = (ImageView) findViewById(R.id.other_pic);
        currentUserPic = (ImageView) findViewById(R.id.current_pic);
        otherUsername = (TextView) findViewById(R.id.other_username);
        currentUsername = (TextView) findViewById(R.id.current_username);

        if(otherUser.hasProfilePic()){
            otherUserPic.setImageBitmap(picManager.getImage(otherUser.getPicKey()));
        }

        if(currentUser.hasProfilePic()){
            currentUserPic.setImageBitmap(picManager.getImage(currentUser.getPicKey()));
        }

        otherUsername.setText(otherUser.firstName());
        currentUsername.setText(currentUser.firstName());


        // find out who the tutor is
        tutor =  (otherUser.isTutor()) ?  otherUser : currentUser;
        sessionLength = tutor.sessionLength();

        // get the session time in hours and minutes
        int hr = sessionLength/60;
        int min =sessionLength%60;
        String hrStr = (hr > 1) ? " hours" : " hour"; // deals with pluralization



        // display time accordingly
        if(hr > 0 && min > 0){
             arcProgress.setBottomText(hr + hrStr + min + " min" );
        }else if(hr > 0){
            arcProgress.setBottomText(hr + hrStr );
        }else{
            arcProgress.setBottomText(min + " min" );
        }


        // convert to long in ms
        sessionLengthMS = MS_IN_MIN * (long) sessionLength;

        // set our session remaining to tutors availability time (in ms)
        if (sessionLengthMS > sessionRemaining) {
            sessionRemaining = sessionLengthMS;
        }



        startCountdown();
    }


    private void startCountdown(){
//        mService.sessionStarted(sessionLengthMS);
        timer = new SessionCounter(sessionRemaining, 10000);
        timer.start();
    }





    // launches alert dialog signalling grapple will end
    private void endSessionPrompt(){
        new AlertDialog.Builder(this)
                .setTitle("End Session?")
                .setMessage("Are you sure you'd like to end the session?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // shut the timer off
                        timer.cancel();
                        timer.onFinish();

                        Long seshTime = sessionLengthMS - sessionRemaining;
                        //send the time passed to other user
                        mService.endSession(seshTime);

                        // go to receipt
                        Intent intent = new Intent(InSession.this, PostSession.class);
                        intent.putExtra("seshTime", seshTime);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void sessionEndedAlert(final long seshTime){
        new AlertDialog.Builder(this)
                .setTitle("Session Ended")
                .setMessage(otherUser.firstName() + " has ended the session")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // shut the timer off
                        timer.cancel();
                        timer.onFinish();


                        // go to receipt
                        Intent intent = new Intent(InSession.this, PostSession.class);
                        intent.putExtra("seshTime", seshTime);
                        startActivity(intent);
                        finish();
                    }
                })

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_insession, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                //TODO
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private class SessionCounter extends CountDownTimer {

        public SessionCounter(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            arcProgress.setBottomText("Completed.");
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.v("Timer Tick", "Session Remaining: " + sessionRemaining + ", Session Length: " + sessionLengthMS);

            sessionRemaining = millisUntilFinished;
            int percentage = (int)( 100 * (sessionLengthMS - sessionRemaining)/sessionLengthMS);
            arcProgress.setProgress(percentage);
        }


    }



}



