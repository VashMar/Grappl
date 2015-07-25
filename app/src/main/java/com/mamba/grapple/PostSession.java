package com.mamba.grapple;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * Created by vash on 4/18/15.
 */
public class PostSession extends Activity {

    TextView rateText;
    TextView sessionLength;
    TextView hourRate;
    TextView total;
    RatingBar rating;
    Button doneButton;
    ImageView ratePic;

    long seshLength;
    long seshMin;
    long seshHr;
    long totalCost;

    UserObject otherUser;
    UserObject currentUser;
    UserObject tutor;


    LoginManager session;
    PicManager picManager;

    // service related variables
    private boolean mBound = false;
    DBService mService;

    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            mBound = true;


            otherUser = mService.getGrappledUser();
            currentUser = session.getCurrentUser();


            // find whos the tutor
            tutor =  (otherUser.isTutor()) ?  otherUser : currentUser;

            // other user picture
            if(otherUser.hasProfilePic()){
                ratePic.setImageBitmap(picManager.getImage(otherUser.getPicKey()));
            }

            // other user rating prompt
            rateText.setText("Rate " + otherUser.firstName());


            // show the hourly rate
            hourRate.setText("Hourly Rate: $" + String.format("%.2f", tutor.getPrice()));

            // show total
            if(seshMin <= 30){
                total.setText("Total: $" + String.format("%.2f", tutor.getPrice()/2));
            }else{
                double totalCost  = seshHr * tutor.getPrice() + (seshMin/60) * tutor.getPrice();
                total.setText("Total: $" + String.format("%.2f", totalCost));
            }

        }
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postsession);

        picManager = new PicManager(getApplicationContext());

        rateText = (TextView) findViewById(R.id.rateTutor);
        sessionLength = (TextView) findViewById(R.id.session_length);
        hourRate = (TextView) findViewById(R.id.cost_title);
        total = (TextView) findViewById(R.id.total_cost);
        rating = (RatingBar) findViewById(R.id.ratingBar2);
        doneButton = (Button) findViewById(R.id.doneBtn);
        ratePic = (ImageView) findViewById(R.id.profilePic);

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float starRating = rating.getRating();
                // send rating of other user to the server
                mService.updateRating(otherUser.isTutor() , starRating);
                currentUser.tutorOff();
                session.saveUser(currentUser);
                // return to search and finish
                Intent intent = new Intent(PostSession.this, Main.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // remove the stored data of the other users profile pic
                picManager.deleteImage(otherUser.getPicKey());
                finish();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("tutor")){
             seshLength = extras.getParcelable("seshTime");

             long seshHr =  TimeUnit.MILLISECONDS.toHours(seshLength);
             long seshMin = TimeUnit.MILLISECONDS.toMinutes(seshLength);
             String totalTime = String.format("%d hours, %d min ",
                    seshHr, seshMin);

             sessionLength.setText("Session Length: " + totalTime);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        session = new LoginManager(getApplicationContext());
        if (session.isLoggedIn()) {
            createService();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Unbind from the service
        if (mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void createService() {
        Log.v("Waiting Page", "Creating Service..");
        startService(new Intent(this, DBService.class));
        bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Results bound to new service");
    }


}
