package com.mamba.grapple;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by vash on 4/15/15.
 */
public class MapDialog extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleMap meetMap;
    private UserObject otherUser;   // other person being connected to
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private MapFragment mapFragment;

    // received from Intent
    private LocationObject meetingPoint; // the suggested meeting location
    private boolean isSelf = false; // is owner of the message that this dialog was launched from

    // tutor coordinates
    private double otherUserLat;
    private double otherUserLon;

    // UI elements
    Button acceptBtn;
    Button declineBtn;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_map);

        // grab UI elements
        acceptBtn = (Button) findViewById(R.id.accept_button);
        declineBtn = (Button) findViewById(R.id.decline_button);

        // get current user  location
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        mLastLocation = locationManager.getLastKnownLocation(locationProvider);

        // load the google map
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // extract data sent from intent
        Bundle extras = getIntent().getExtras();

        if(extras != null){
            meetingPoint = extras.getParcelable("meetingPoint");
            otherUser = extras.getParcelable("otherUser");
            isSelf = extras.getBoolean("isSelf");
            otherUserLat = otherUser.getLatitude();
            otherUserLon = otherUser.getLongitude();
        }


        // bind UI listeners
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send the data to the meetup view and finish
                Intent intent = new Intent(MapDialog.this, Meetup.class);
                intent.putExtra("meetingPoint", meetingPoint);
                startActivity(intent);
                finish();
            }
        });


        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // just close for now
            }
        });

        // if currentUser sent this message then hide the buttons
//        if(isSelf){
//            acceptBtn.setVisibility(View.GONE);
//            declineBtn.setVisibility(View.GONE);
//        }



    }



    @Override
    public void onMapReady(GoogleMap map){
        Log.v("Google Map Ready", "Adding tutor marker");
        LatLng tutorLoc = new LatLng(otherUserLat, otherUserLon);
        LatLng meetPoint = new LatLng(meetingPoint.lat, meetingPoint.lon);
        int zoom;
        meetMap = map;
        map.setMyLocationEnabled(true);
        Marker tutorMarker = map.addMarker(new MarkerOptions()
                .position(tutorLoc)
                .title(otherUser.firstName())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.markersmall)));

        Marker meetMarker = map.addMarker(new MarkerOptions()
                .position(meetPoint)
                .title("Meeting Spot")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        meetMarker.showInfoWindow();

        if(mLastLocation != null ){
            LatLng userLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
//            Log.v("mLastLocation Exists", "Adding user marker");
//            Marker userMarker = map.addMarker(new MarkerOptions()
//                    .position(userLoc)
//                    .title("You")
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.markersmall)));

            Double meetDistance =  Double.parseDouble(otherUser.getDistance(userLoc));

            // if the distance is under a mile zoom 14, otherwise do 13
            zoom = (meetDistance <= 1) ? 14 : 13;

            map.moveCamera( CameraUpdateFactory.newLatLngZoom( meetPoint , zoom) );



        }

    }




    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        Log.v("gConnected", "Connected to google play services");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        Log.v("latitude", String.valueOf(mLastLocation.getLatitude()));

        meetMap.addMarker(new MarkerOptions()
                .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .title("You"));

    }

    @Override
    public void onConnectionSuspended(int cause){
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.v("fail", "Connection to Google Services Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the next section.
        Log.v("fail", "Connection to Google Services Failed");

    }


}
