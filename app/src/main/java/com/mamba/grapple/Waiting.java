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
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class Waiting extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private MapFragment mapFragment;

    LoginManager session;
    UserObject currentUser;
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
    private String available = "";

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
                    UserObject user = extras.getParcelable("user");
                    String place = extras.getString("place");
                    LocationObject meetingSpot = findMeetingSpot(place);
                    mService.newConvo();
                    Intent chatIntent = new Intent(Waiting.this, Chat.class);
                    chatIntent.putExtra("user", user);
                    chatIntent.putExtra("meetingSpot", meetingSpot);
                    startActivity(chatIntent);
                    finish();
                }

            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            mBound = true;
            mLastLocation = mService.getLocation();
        }
        public void onServiceDisconnected(ComponentName arg0) {
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
        if (extras != null ){
            meetingSpots = extras.getParcelableArrayList("meetingSpots");
            selectedCourses = extras.getStringArrayList("selectedCourses");
            hrRate = extras.getDouble("hrRate");
            available = extras.getString("available");
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
        tutorAvailability = (TextView) findViewById(R.id.tutorAvailability);
        tutorPic = (ImageView) findViewById(R.id.profilePic);

        //update UI elements

        if(currentUser.hasProfilePic()){
            Log.v("Profile Pic", "Loading..");
            Bitmap img  = picManager.getImage(currentUser.getPicKey());
            tutorPic.setImageBitmap(img);
            //Picasso.with(getApplicationContext()).load(currentUser.getProfilePic()).into(tutorPic);
        }

        tutorName.setText(currentUser.getName());
        tutorPrice.setText("Hourly Rate: $" + String.format("%.2f", hrRate));
        tutorCourses.setText("Courses: " + courseString);
        tutorAvailability.setText("Available: " + available);


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
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(multicastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(waitingReceiver);
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
