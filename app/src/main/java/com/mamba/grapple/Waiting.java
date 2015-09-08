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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.pushbots.push.PBNotification;
import com.pushbots.push.PBNotificationIntent;
import com.pushbots.push.Pushbots;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class Waiting extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private MapFragment mapFragment;

    LoginManager session;
    UserObject currentUser;
    UserObject otherUser;
    PicManager picManager;

    private Location mLastLocation;

    // service related variables
    private boolean mBound = false;
    DBService mService;


    TextView tutorName;
    TextView tutorPrice;
    TextView tutorCourses;
    TextView tutorAvailability;
    ImageView tutorPic;
    Button cancelButton;

    // list of meeting spots on map
    private ArrayList<LocationObject> meetingSpots;
    private ArrayList<String> selectedCourses;
    private double hrRate;
    private int availableTime;
    private String endTime;




    // receiver to handle server responses for this activity
    private BroadcastReceiver waitingReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Waiting Activity", "received response: " + responseType);
                if(responseType == "grapple"){
                    // take them to chat view with user
                    otherUser = extras.getParcelable("user");
                    String place = extras.getString("place");
                    LocationObject meetingSpot = findMeetingSpot(place);

                    // start a new chat and store the meeting spot
                    mService.newConvo();
                    mService.setMeetingPoint(meetingSpot);

                    Intent chatIntent = new Intent(Waiting.this, Chat.class);
                    chatIntent.putExtra("user", otherUser);
                    chatIntent.putExtra("meetingSpot", meetingSpot);
                    startActivity(chatIntent);
                    finish();

                    // remove pending notification from launching this activity
                    if (PBNotificationIntent.notificationsArray != null) {
                        Log.v("Clearing Notifcations", PBNotificationIntent.notificationsArray+"");
                        PBNotificationIntent.notificationsArray = null;
                    }
                }

            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            mService.inView();
            mBound = true;
            mLastLocation = mService.getLocation();

            if(mService.getUpdates().contains("GRAPPLE")){
                Intent intent = new Intent(Waiting.this, Chat.class);
                startActivity(intent);
                mService.clearUpdates();
                finish();
            }

        }
        public void onServiceDisconnected(ComponentName arg0){
            mBound = false;
        }
    };



    // receiver to handle multicast responses
    private BroadcastReceiver multicastReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {

            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Waiting Activity", "received response: " + responseType);

                if (responseType.equals("removeAvailableDone")) {
                    //Clear Notification array
                    if(PBNotificationIntent.notificationsArray != null){
                        Log.v("Notifications", PBNotificationIntent.notificationsArray+"");
                        PBNotificationIntent.notificationsArray = null;
                    }
                    ((Activity) Waiting.this).finish();
                }
            }

        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState){
        Log.v("Waiting", "Created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // track user session data
        session = new LoginManager(getApplicationContext());
        currentUser = session.getCurrentUser();
        picManager = new PicManager(getApplicationContext());

        //set current user as tutor
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("meetingSpots") && extras.containsKey("selectedCourses") ){
            Log.v("Waiting Activity", "Unpackaging Broadcast Data..");
            meetingSpots = extras.getParcelableArrayList("meetingSpots");
            selectedCourses = extras.getStringArrayList("selectedCourses");
            hrRate = extras.getDouble("hrRate");
            availableTime = extras.getInt("available");

            if(meetingSpots == null && selectedCourses == null ){
                Log.v("Waiting Activity", "Converting String Data");
                String mSpots = extras.getString("meetingSpots");
                String selCourses = extras.getString("selectedCourses");

                Gson gson = new Gson();
                Type resultType = new TypeToken<ArrayList<LocationObject>>(){}.getType();
                meetingSpots = gson.fromJson(mSpots, resultType);

                resultType = new TypeToken<ArrayList<String>>(){}.getType();
                selectedCourses = gson.fromJson(selCourses, resultType);

                hrRate = Double.parseDouble(extras.getString("hrRate"));
                availableTime = Integer.parseInt(extras.getString("available"));
                Log.v("mSpots", mSpots);
                Log.v("selCourses", selCourses);
            }



//            Calendar cal = Calendar.getInstance();
//            // get the date data from the received start time
//            cal.setTimeInMillis(currentUser.getSessionStart());
//            int startMin = cal.get(Calendar.MINUTE);
//            Log.v("startMin", startMin+"");
//            int endMin = startMin + availableTime;
//            Log.v("endMin", endMin+"");
//            int endHr = cal.get(Calendar.HOUR_OF_DAY);
//            Log.v("endHr", endHr+"");
//            endTime = endMin+"";
//
//            // deal with overflow when adding availability time
//            if(endMin > 60){
//                endHr += endMin/60;
//                endMin = endMin%60;
//                endTime = String.valueOf(endMin);
//                if(endMin < 10){
//                    endTime = "0" +endTime;
//                }
//            }
//
//            Log.v("endHr", endHr+"");
//            // handle AM or PM for availability end time and build the string
//            if(endHr > 24){
//                endTime = endHr - 24 + ":" + endMin + "AM";
//            }else if(endHr >= 12){
//                endTime =  (endHr - 12 == 12) ? endHr - 12 + ":" + endTime + "AM" : endHr - 12 + ":" + endTime + "PM";
//            }else{
//                endTime = endHr + ":" + endTime + "AM";
//            }


        }

        getActionBar().setTitle("Waiting...");

        // create readable string from courses list
        String courseString = "";
        for(int i = 0; i < selectedCourses.size(); i++){
            courseString += selectedCourses.get(i);
            if(i < selectedCourses.size() - 1){
                courseString += ", ";
            }
        }

        // load ui elements
        cancelButton = (Button) findViewById(R.id.cancelButton);
        tutorName = (TextView) findViewById(R.id.tutorName);
        tutorPrice = (TextView) findViewById(R.id.tutorPrice);
        tutorCourses = (TextView) findViewById(R.id.tutorCourses);
        tutorPic = (ImageView) findViewById(R.id.profilePic);

        //update UI elements

        if(currentUser.hasProfilePic()){
            Log.v("Profile Pic", "Loading..");
            Bitmap img  = picManager.getImage(currentUser.getPicKey());
            if(img != null){
                tutorPic.setImageBitmap(img);
            }else{
                Picasso.with(getApplicationContext()).load(currentUser.getProfilePic()).into(tutorPic);
            }
            
        }

        tutorName.setText(currentUser.getName());
        tutorPrice.setText("Hourly Rate: $" + String.format("%.2f", hrRate));
        tutorCourses.setText("Courses: " + courseString);



        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endBroadcastPrompt();
            }
        });

        //create map
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }


    public void onStart(){
        super.onStart();
        if (session.isLoggedIn()) {
            createService();
        }

        // register broadcast receiver for this activity
        LocalBroadcastManager.getInstance(this).registerReceiver(waitingReceiver,
                new IntentFilter("waitingReceiver"));


        // register broadcast receiver for this activity
        LocalBroadcastManager.getInstance(this).registerReceiver(multicastReceiver,
                new IntentFilter("multicastReceiver"));

        Pushbots.sharedInstance().setPushEnabled(false);
        Pushbots.sharedInstance().unRegister();

    }

    @Override
    public void onPause(){
        super.onPause();
        Log.v("Waiting", "Out of View");
        if(mBound){
            mService.outOfView();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.v("Waiting", "In View");

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(multicastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(waitingReceiver);

        Pushbots.sharedInstance().setPushEnabled(true);
        Pushbots.sharedInstance().register();


    }


    @Override
    public void onBackPressed(){
        endBroadcastPrompt();
    }

    public void createService(){
        Log.v("Waiting Page", "Creating Service..");
        startService(new Intent(this, DBService.class));
        bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Results bound to new service");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_signin, menu);

        //return super.onCreateOptionsMenu(menu);
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
            case R.id.action_signout:
                Intent myIntent = new Intent(Waiting.this, SignIn.class);
                myIntent.putExtra("destroy_token", "true");
                startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
//        LatLng userLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 11));

        List<LatLng> coordinates = new ArrayList<LatLng>();
        // loop through all added meeting spots and mark them on map
        for(LocationObject meetingSpot : meetingSpots){
            LatLng meetLoc = new LatLng(meetingSpot.lat, meetingSpot.lon);
            coordinates.add(meetLoc);
            googleMap.addMarker(new MarkerOptions()
                    .position(meetLoc)
                    .title(meetingSpot.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.markersmall)));

        }

        LatLng center = findCenter(coordinates);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 14));



    }




    private void endBroadcastPrompt(){
        new AlertDialog.Builder(this)
                .setTitle("Stop Broadcast")
                .setMessage("Are you sure you want to stop broadcasting?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with ending broadcast
                        mService.endBroadcast();
                        Log.v("Waiting Closing", "Tutoring Ending");
                        // turn the tutoring switch off
                        currentUser.tutorOff();
                        session.saveUser(currentUser);
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

    private LocationObject findMeetingSpot(String place){
        for(int i = 0; i < meetingSpots.size(); i++){
            if(meetingSpots.get(i).getName().equals(place)){
                return meetingSpots.get(i);
            }
        }

        return null;
    }

    private LatLng findCenter(List<LatLng> coordinates){

        if(coordinates.size() == 1){
          return coordinates.get(0);
        }

        double x = 0;
        double y = 0;
        double z = 0;


        for(LatLng coordinate : coordinates){
            double latitude = deg2rad(coordinate.latitude);
            double longitude = deg2rad(coordinate.longitude);

            x += Math.cos(latitude) * Math.cos(longitude);
            y += Math.cos(latitude) * Math.sin(longitude);
            z += Math.sin(latitude);

        }

       double total = coordinates.size();

        x = x / total;
        y = y / total;
        z = z / total;

        double centralLongitude = Math.atan2(y, x);
        double centralSquareRoot = Math.sqrt(x * x + y * y);
        double centralLatitude = Math.atan2(z, centralSquareRoot);

        return new LatLng(rad2deg(centralLatitude), rad2deg(centralLongitude));

    }


    /************************************  Helpers   ***************************************************************************/

    // conversions
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


}
