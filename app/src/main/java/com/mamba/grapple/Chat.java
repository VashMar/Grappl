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
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;


import java.util.List;

/**
 * Created by vash on 4/8/15.
 */
public class Chat extends Activity {

    private MessagesAdapter adapter;
    private List<MessageObject> messageList;
    private LocationObject selectedLocation;


    private UserObject otherUser;
    private UserObject currentUser;


    ListView messagesContainer;

    EditText chatInput;
    ImageButton sendButton;
    ImageButton mapButton;
    View selected;
//    ImageButton locationList;

    PicManager picManager;
    LoginManager session;


    // service related variables
    private boolean mBound = false;
    DBService mService;

    // service connection event handler
    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.connectSocket();
            otherUser = mService.grappledUser;
            getActionBar().setTitle(otherUser.getName());

            if(otherUser.hasProfilePic()){
                Resources res = getResources();
                BitmapDrawable icon = new BitmapDrawable(res, picManager.getImage(otherUser.getPicKey()));
                getActionBar().setIcon(icon);
            } else{
                getActionBar().setIcon(R.drawable.user_icon);
            }



            messageList = mService.retrieveConvo();
            adapter = new MessagesAdapter(Chat.this,messageList);
            messagesContainer.setAdapter(adapter);
            adapter.notifyDataSetChanged();

            RelativeLayout tutorPrompt = (RelativeLayout) findViewById(R.id.tutorPrompt);
            Log.v("Is tutor", currentUser.isTutor()+"");
            Log.v("Meetup State", mService.inMeetup()+"");
            if(currentUser.isTutor() && !mService.inMeetup()){
                Log.v("Chat Activity", "Tutor entered..");

                Button acceptTutoring = (Button) findViewById(R.id.acceptTutoring);
                Button declineTutoring = (Button) findViewById(R.id.declineTutoring);
                final TextView meetupQuestion = (TextView) findViewById(R.id.meetupQuestion);
                tutorPrompt.setVisibility(View.VISIBLE);
                meetupQuestion.setText("Do you want to meetup with " + otherUser.firstName() + " at " +  selectedLocation.getName()+ "?");
                acceptTutoring.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mService.setMeetup();
                        mService.startMeetup();
                        Intent meet = new Intent(Chat.this, Meetup.class);
                        meet.putExtra("meetingPoint", selectedLocation);
                        meet.putExtra("otherUser", otherUser);
                        startActivity(meet);
                    }
                });

                declineTutoring.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v){

                    }
                });

            }else{
                tutorPrompt.setVisibility(View.GONE);
            }

        }
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



    // receiver to handle server responses for this activity
    private BroadcastReceiver grapplReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Chat Activity", " grappl receiver received: " + responseType);
                // TODO: Change to switch statement
                // if there's a new message add it to the list and display
                if(responseType == "message"){
                    adapter.notifyDataSetChanged();
                }

                if(responseType.equals("startMeetup")){
                    Intent meet = new Intent(Chat.this, Meetup.class);
                    meet.putExtra("meetingPoint", selectedLocation);
                    startActivity(meet);
                }

                if(responseType.equals("grapplEnded")){
                    endGrappleAlert();
                }

                if(responseType.equals("sessionRequest")){
                    acceptRequestPrompt();
                }

                if(responseType.equals("startSession")){
                    sessionAccept();
                }
            }
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorchat);

        Log.v("Chat Activity", "Created");

        picManager = new PicManager(getApplicationContext());
        retrieveInfo(); // gets info about other chat member

        messagesContainer = (ListView) findViewById(R.id.list_view_messages);
        sendButton = (ImageButton) findViewById(R.id.btnSend);
        mapButton = (ImageButton) findViewById(R.id.mapicon);
        chatInput = (EditText)  findViewById(R.id.msgInput);

        sendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                // get message from input field
                String msgText = chatInput.getText().toString();

                // send message to server
                mService.sendMessage(currentUser.firstName(), currentUser.getId(), otherUser.getId(), msgText);

                // clear input field
                chatInput.setText("");

            }
        });


        mapButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent meet = new Intent(Chat.this, Meetup.class);
                meet.putExtra("meetingPoint", selectedLocation);
                meet.putExtra("otherUser", otherUser);
                startActivity(meet);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
    }


    @Override
    protected void onPause(){
        super.onPause();
    }



    @Override
    protected void onStart(){
        super.onStart();

        //get the latest session and user data
        session = new LoginManager(getApplicationContext());
        currentUser = session.getCurrentUser();

        // bind to service
        Intent intent = new Intent(this, DBService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(grapplReceiver,
                new IntentFilter("grapplReceiver"));
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Unbind from the service
        if (mBound){
            Log.v("Unbinding Service", "Chat Activity");
            unbindService(mConnection);
            mBound = false;
        }

        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(grapplReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Log.v("Chat Activity", "New Intent");
    }


    public void retrieveInfo(){
        // get the connected users data
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            selectedLocation = extras.getParcelable("meetingSpot");
        }
    }


    public void sessionAccept(){
        Intent intent = new Intent(Chat.this, InSession.class);
        startActivity(intent);
        finish();
    }


    public void endGrappleAlert(){
        new AlertDialog.Builder(this)
            .setTitle("Grappl Ended")
            .setMessage(otherUser.firstName() + " has cancelled the Grappl")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mService.resetStates();
                    returnHome();
                }
            })

            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }



    // launches alert dialog signalling grapple will end
    private void acceptRequestPrompt(){
        new AlertDialog.Builder(this)
                .setTitle(otherUser.firstName() + " wants to start the session")
                .setMessage("Are you ready to begin?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // send acceptance message and continue to session screen
                        mService.setMeetup();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }




    /**************************************************************************** Options Menu Management ******************************************************************/




    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_grapple, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_endGrapple:
                mService.cancelGrappl();
                returnHome();
                return true;
            case R.id.action_settings:
                //TODO
            case R.id.action_signout:
                Intent myIntent = new Intent(Chat.this, SignIn.class);
                myIntent.putExtra("destroy_token", "true");
                startActivity(myIntent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void returnHome(){
        // turn the tutoring switch off
        currentUser.tutorOff();
        session.saveUser(currentUser);
        Intent intent = new Intent(Chat.this, Main.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }


}
