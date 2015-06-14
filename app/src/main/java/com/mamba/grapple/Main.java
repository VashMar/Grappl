package com.mamba.grapple;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

import java.util.ArrayList;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by vash on 4/29/15.
 */
public class Main extends FragmentActivity {

    private TabHost mTabHost;

    ActionBar actionBar;
    ViewPager viewPager;
    FragmentPagerAdapter fragPageAdapter;


    // service related variables
    private boolean mBound = false;
    DBService mService;

    // current user data
    LoginManager session;
    UserObject currentUser;

    // temporary until DB load setup (use SimpleCursorAdapter for DB)
    static final String[] COURSES = {"Chemistry 103", "Comp Sci 302", "French 4", "Math 234", "Physics 202"};
    ArrayList<String> courseList;

    // receiver intended for this activity
    private BroadcastReceiver mainReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent){
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Main Activity", "received response: " + responseType);

            }
        }
    };

    // receiver intended for multicasts
    private BroadcastReceiver multicastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Main Activity", "received multicast: " + responseType);

            }

        }
    };



    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new LoginManager(getApplicationContext());

        mTabHost = (TabHost) findViewById(R.id.tabHost);
        mTabHost.setup();

        TabHost.TabSpec ts = mTabHost.newTabSpec("student");
        ts.setContent(R.id.tab1);
        ts.setIndicator("Student");

        mTabHost.addTab(ts);

        ts = mTabHost.newTabSpec("tutor");
        ts.setContent(R.id.tab2);
        ts.setIndicator("Tutor");
        mTabHost.addTab(ts);
        mTabHost.setBackgroundColor(Color.WHITE);

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                Log.v("tab switch ", tabId);
            }
        });

        session = new LoginManager(getApplicationContext());

        // register broadcast receiver for activity
        LocalBroadcastManager.getInstance(this).registerReceiver(mainReceiver,
                new IntentFilter("mainReceiver"));

        // register global broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(multicastReceiver,
                new IntentFilter("multicastReceiver"));

    }

    // check login status every time the activity gets shown
    protected void onResume(){
        super.onResume();
        session = new LoginManager(getApplicationContext());
        if(session.isLoggedIn()){
            currentUser = session.getCurrentUser();
            Log.v("Search Login Status", currentUser.getName() +  " has been logged in");
            createService();
        }
    }

    protected void onPause(){
        super.onPause();
        // Unbind from the service
        if (mBound){
            Log.v("Unbinding Service", "Search Activity");
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(multicastReceiver);
    }



    // handles the result of login/registration
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == 1 && resultCode == RESULT_OK && data != null){
            Log.v("Reached", "Auth Activity Result");
//            session = new LoginManager(getApplicationContext());
            invalidateOptionsMenu();
        }
    }

    public void createService(){
        Log.v("Login Status", "User has been logged in");
        startService(new Intent(this, DBService.class));
        bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Results bound to new service");
    }

    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service){
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            mBound = true;

        }

        public void onServiceDisconnected(ComponentName arg0){
            mBound = false;
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
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
                Intent logoffIntent = new Intent(Main.this, SignIn.class);
                session.logout();
                startActivityForResult(logoffIntent, 1);
                return true;
            case R.id.action_signin:
                Intent loginIntent = new Intent(Main.this, SignIn.class);
                startActivityForResult(loginIntent, 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}


