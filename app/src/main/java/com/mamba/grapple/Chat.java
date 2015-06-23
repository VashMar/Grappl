package com.mamba.grapple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
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
    private GoogleApiClient mGoogleApiClient;

    private LocationsAdapter locationsAdapter;
    private List<LocationObject> locationList;
    private LocationObject selectedLocation;


    private UserObject otherUser;
    private UserObject currentUser;


    ListView messagesContainer;

    EditText chatInput;
    ImageButton sendButton;
    ImageButton mapButton;
    View selected;
//    ImageButton locationList;


    private boolean grappled = false;
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

            // create a new conversation if this is the first time
            if(!grappled){
               mService.newConvo();
            }
            messageList = mService.retrieveConvo();
            adapter = new MessagesAdapter(Chat.this,messageList);
            messagesContainer.setAdapter(adapter);
            adapter.notifyDataSetChanged();

            if(currentUser.isTutor() && !mService.inMeetup()){
                Log.v("Chat Activity", "Tutor entered..");
                RelativeLayout tutorPrompt = (RelativeLayout) findViewById(R.id.tutorPrompt);
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


                declineTutoring.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v){

                    }
                });

            }

        }
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    // receiver to handle server responses for this activity
    private BroadcastReceiver chatReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Chat Activity", "received response: " + responseType);

                // if there's a new message add it to the list and display
                if(responseType == "message"){
                    adapter.notifyDataSetChanged();
                }

                if(responseType.equals("startMeetup")){
                    Intent meet = new Intent(Chat.this, Meetup.class);
                    meet.putExtra("meetingPoint", selectedLocation);
                    startActivity(meet);
                }
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorchat);

        Log.v("Chat Activity", "Created");

        retrieveInfo(); // gets info about other chat member

        messagesContainer = (ListView) findViewById(R.id.list_view_messages);
        sendButton = (ImageButton) findViewById(R.id.btnSend);
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
    }

    @Override
    protected void onResume(){
        super.onResume();
        //get the latest session and user data
        session = new LoginManager(getApplicationContext());
        currentUser = session.getCurrentUser();
    }


    @Override
    protected void onPause(){
        super.onPause();
    }



    @Override
    protected void onStart(){
        super.onStart();
        // bind to service
        Intent intent = new Intent(this, DBService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(chatReceiver,
                new IntentFilter("chatReceiver"));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(chatReceiver);
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
            otherUser = extras.getParcelable("user");
            selectedLocation = extras.getParcelable("meetingSpot");
            getActionBar().setTitle(otherUser.getName());
            getActionBar().setIcon(R.drawable.user_icon);

            // mark that grapple already happened
            if(extras.containsKey("beenGrappled")){
                grappled = true;
            }

        }
    }




    /**************************************************************************** Options Menu Management ******************************************************************/




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                mService.endGrapple();
                finish();
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



//
//    public void sendDummyMsg(){
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // display locally
//                MessageObject msg  = new MessageObject(tutor.firstName(), "Hi!", false, null);
//                messageList.add(msg);
//                adapter.notifyDataSetChanged();
//            }
//        }, 2000);
//
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // display locally
//                MessageObject msg = new MessageObject(tutor.firstName(), "Can you meet here? I've got a table reserved: ", false, null);
//                messageList.add(msg);
//                adapter.notifyDataSetChanged();
//
//            }
//        }, 3200);
//
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // display locally
//                LocationObject loc = new LocationObject(43.071394, -89.408676);
//                MessageObject msg = new MessageObject(tutor.firstName(), "215 N Randall Ave, Madison, WI 53706" , false, loc);
//                messageList.add(msg);
//                adapter.notifyDataSetChanged();
//            }
//        }, 4000);
//
//
//
//
//
//    }
//





}
