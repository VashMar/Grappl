package com.mamba.grapple;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.Image;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;


public class Results extends Activity {

    ArrayList<UserObject> tutorList;
    ListView listView;
    TextView emptyMsg;
    ImageView sadface;

    // service related variables
    private boolean mBound = false;
    DBService mService;

    // current user data
    LoginManager session;
    UserObject currentUser;
    PicManager picManager;

    TutorsAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        session = new LoginManager(getApplicationContext());
        picManager = new PicManager(getApplicationContext());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            String course = extras.getString("course");
            getActionBar().setTitle("Tutors for " + course);

            // get the tutor list from previous activity
            tutorList = extras.getParcelableArrayList("tutorList");
            Log.v("tutorList", String.valueOf(tutorList));

            // populate the list view
            adapter = new TutorsAdapter(this, tutorList);

            listView = (ListView) findViewById(R.id.listView);
            emptyMsg = (TextView) findViewById(R.id.empty_message);
            sadface = (ImageView) findViewById(R.id.sadface);

            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {

                if (!session.isLoggedIn()) {
                    // transfer the user to the register page
                    Intent intent = new Intent(Results.this, SignIn.class);
                    // we expect the auth response
                    startActivityForResult(intent, 1);
                } else {
                    Log.v("Login status", "Logged in user");
                    UserObject selectedTutor = tutorList.get(position);
                    Log.v("selected tutor", String.valueOf(selectedTutor));
                    selectedTutor.setTutor();

                    // store the users profile pic (temporary)
                    ImageView profPic = (ImageView)view.findViewById(R.id.profilePic);
                    Bitmap bitmap = ((BitmapDrawable)profPic.getDrawable()).getBitmap();
                    picManager.storeImage(bitmap, selectedTutor.getPicKey());

                    // takes the user out of tutor mode
                    currentUser.tutorOff();
                    session.saveUser(currentUser);

                    // transition to specific tutors page
                    Intent intent = new Intent(Results.this, Meetup.class);
                    intent.putExtra("otherUser", selectedTutor);
                    startActivity(intent);
                }
                }
            });

            // if there are no tutors display the message
            if(tutorList.size() < 1){
                emptyMsg.setVisibility(View.VISIBLE);
                sadface.setVisibility(View.VISIBLE);
            }
        }
    }

    // check login status every time the activity gets shown
    public void onResume(){
        super.onResume();

        // reset session
        session = new LoginManager(getApplicationContext());

        // reset action bar menu
        invalidateOptionsMenu();

        if(session.isLoggedIn()){
            currentUser = session.getCurrentUser();
            Log.v("Search Login Status", currentUser.getName() +  " has been logged in");
            createService();
        }
    }

    protected void onPause(){
        super.onPause();
        // Unbind from the service
        if (mBound) {
            Log.v("Unbinding Service", "Results Activity");
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    // handles the result of login/registration
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Log.v("Reached", "Auth Activity Result");

        }
    }

    public void createService(){
        Log.v("Login Status", "User has been logged in");
        startService(new Intent(this, DBService.class));
        bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Results bound to new service");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        if(session.isLoggedIn()){
            Log.v("checking login", "user logged in");
            getMenuInflater().inflate(R.menu.menu_account, menu);
        }else{
            Log.v("checking login", "user not logged in");
            getMenuInflater().inflate(R.menu.menu_signin, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                //TODO
            case R.id.action_signout:
                Intent myIntent = new Intent(Results.this, SignIn.class);
                session.logout();
                mService.endConnection();
                startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            adapter.setUserLocation(mService.getLocation());
            mBound = true;
            mService.resetStates();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
