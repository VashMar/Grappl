package com.mamba.grapple;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Meetup extends FragmentActivity implements OnMapReadyCallback {

    private MapFragment mapFragment;
    private GoogleMap gMap;


    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private UserObject otherUser;
    private UserObject currentUser;

    // service related variables
    private boolean mBound = false;
    DBService mService;

    private LocationObject meetingPoint;

    Marker tutorMarker;

    GoogleMap.InfoWindowAdapter iwadapter;

    LoginManager session;

    ArrayList<LocationObject> meetingSpots;
    LocationObject meetingSpot;
    String selectedSpot;
    Button grappleButton;

    // receiver to handle server responses for this activity
    private BroadcastReceiver meetupReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Meetup Activity", "received response: " + responseType);

                // if there's a new message add it to the list and display
                if(responseType.equals("locationUpdate")){
                    //

                }

                if(responseType.equals("grapplFail")){
                    grapplFail();
                }

                if(responseType.equals("grapplSuccess")){
                    grapplSuccess();
                }
            }
        }
    };

    // callback for service connection
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mLastLocation = mService.getLocation();


            // render map if it doesn't exist
            if(gMap == null){
               mapFragment.getMapAsync(Meetup.this);
            }


            // if the user has been grappled, adjust view accordingly
            if(mService.grappleState()){

                invalidateOptionsMenu();

                // grab dynamic layout items
                ImageButton chatButton = (ImageButton) findViewById(R.id.chatButton);

                // hide the grapple button and show the session/chat buttons
                grappleButton.setVisibility(View.GONE);
                chatButton.setVisibility(View.VISIBLE);


//                // change to session infowindow
//                iwadapter = new SessionWindowAdapter();
//                gMap.setInfoWindowAdapter(iwadapter);
//                iwadapter.getInfoWindow(tutorMarker);
//                tutorMarker.showInfoWindow();

//                // send otherUser data along
//                gMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
//                    @Override
//                    public void onInfoWindowClick(Marker marker) {
//                        Intent intent = new Intent(Meetup.this, InSession.class);
//                        intent.putExtra("otherUser", otherUser);
//                        startActivity(intent);
//                        finish();
//                    }
//                });


                chatButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(Meetup.this, Chat.class);
                        intent.putExtra("user", otherUser);
                        intent.putExtra("beenGrappled", true);
                        startActivity(intent);
                    }
                });

            }
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorselect);


        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        grappleButton = (Button) findViewById(R.id.grappleButton);
        grappleButton.setEnabled(false);


        // get the other user data
        retrieveInfo();





    }

    public void onStart() {
        super.onStart();
        Log.v("Tutor View", "Resumed");
        Intent intent = new Intent(this, DBService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Tutor select bound to service");

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(meetupReceiver,
                new IntentFilter("meetupReceiver"));


        session = new LoginManager(getApplicationContext());
        currentUser = session.getCurrentUser();
    }

    protected void onStop(){
        super.onStop();
        // Unbind from the service
        if (mBound){
            Log.v("Unbinding Service", "Meetup Activity");
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(meetupReceiver);
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        if(mService.grappleState()){
            endGrapplePrompt();
        }else{
            super.onBackPressed();
        }

    }

    // response when meeting point is accepted
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.v("Tutor View", "new intent received ");
            meetingPoint = extras.getParcelable("meetingPoint");
            if (meetingPoint != null) {
                Log.v("Tutor View", "Meeting Point Found");
                gMap.clear();
                routeToMP();

            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.v("Google Map Ready", "Adding tutor marker");


        int zoom;
        gMap = map;
        map.setMyLocationEnabled(true);
        CameraPosition cameraPosition;

        if(mService.inMeetup()){
            routeToMP();

            cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(meetingPoint.lat, meetingPoint.lon)).zoom(14).build();
        }else{
            List<LatLng> coordinates = new ArrayList<LatLng>();
            meetingSpots = otherUser.getMeetingSpots();
            // add markers for all the potential meeting spots
            for(int i =0; i < meetingSpots.size(); i++){
                LatLng meetingLoc = new LatLng(meetingSpots.get(i).lat, meetingSpots.get(i).lon);
                coordinates.add(meetingLoc);
                tutorMarker = gMap.addMarker(new MarkerOptions()
                        .position(meetingLoc)
                        .title(meetingSpots.get(i).getName())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.markersmall)));

            }



        //        iwadapter = new TutorWindowAdapter();
        //        gMap.setInfoWindowAdapter(iwadapter);
        //        iwadapter.getInfoWindow(tutorMarker);

            LatLng center = findCenter(coordinates);

             cameraPosition = new CameraPosition.Builder()
                    .target(center).zoom(14).build();



            // selects the meeting point and stores it for reference
            gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(!mService.grappleState()){
                        grappleButton.setEnabled(true);
                        grappleButton.setTextColor(Color.WHITE);
                        selectedSpot =  marker.getTitle();
                    }

                    return false;
                }
            });
        }


        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
//                tutorMarker.showInfoWindow();
            }

            @Override
            public void onCancel() {

            }
        });
    }



    // enters the chat with the tutor
    public void grappleTutor(View view){
        Log.v("grappleEvent", "tutor id = " + otherUser.getId());
        startGrapplePrompt();
    }

    public void retrieveInfo() {
        // get the tutor data
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            otherUser = extras.getParcelable("otherUser");

            // get + set name and rating from both perspectives
            TextView othersName = (TextView) findViewById(R.id.tutorName);
            RatingBar othersRating = (RatingBar) findViewById(R.id.ratingBar);

            othersName.setText(otherUser.getName());
            othersRating.setRating(otherUser.getRating());

            if(otherUser.isTutor()){
                Log.v("Other User", "Is a tutor");
                // if tutor we show the price
                TextView tutorPrice = (TextView) findViewById(R.id.tutorPrice);
                tutorPrice.setText("$" +  String.format("%.2f", otherUser.getPrice()));
            }else{
                meetingPoint = extras.getParcelable("meetingPoint");
            }
        }
    }

    // creates marker from meeting point and draws a route to it
    public void routeToMP(){
        final LatLng mP = new LatLng(meetingPoint.lat, meetingPoint.lon);
        gMap.addMarker(new MarkerOptions()
                .position(mP)
                .title(meetingPoint.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        addRoute(meetingPoint.lat, meetingPoint.lon);
    }


    public void grapplSuccess(){
        Intent intent = new Intent(Meetup.this, Chat.class);
        intent.putExtra("user", otherUser);
        intent.putExtra("meetingSpot", meetingSpot);
        startActivity(intent);
    }

    /************************************  Alert Dialogs  ***************************************************************************/

    private void grapplFail(){
        new AlertDialog.Builder(this)
                .setTitle("Unable to Grappl" + otherUser.firstName())
                .setMessage(otherUser.firstName() + "is no longer broadcasting")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    // launches alert dialog signalling grapple will end
    private void startGrapplePrompt(){
        new AlertDialog.Builder(this)
                .setTitle("Grappl " + otherUser.firstName() + " ?")
                .setMessage("Request to meetup with " + otherUser.firstName() + " at " + selectedSpot + "?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // continue with ending broadcast
                        mService.startGrapple(otherUser, selectedSpot);
                        // mark this user as student
                        currentUser.isStudent();
                        // store
                        session.saveUser(currentUser);
                        // update service
                        mService.setSession(session);
                        // get the location for selectedSpot
                        meetingSpot = findMeetingSpot(selectedSpot);

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


    // launches alert dialog signalling grapple will end
    private void endGrapplePrompt(){
        new AlertDialog.Builder(this)
                .setTitle("Stop Grapple?")
                .setMessage("Are you sure you want to ungrapple " + otherUser.firstName() + " ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with ending broadcast
                        mService.endGrapple();
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


    /************************************  Locating  ***************************************************************************/

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


    /**************************************************************************** Options Menu Management ******************************************************************/


    // prepares new option menu on state change
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if(mService.grappleState()) {
            Log.v("Meetup", "Adding menu for grapple state");
            getMenuInflater().inflate(R.menu.menu_grapple, menu);
        }else{
            getMenuInflater().inflate(R.menu.menu_account, menu);
        }
        return super.onPrepareOptionsMenu(menu);
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
            case R.id.action_endGrapple:
                mService.endGrapple();
                finish();
                return true;
            case R.id.action_settings:
                //TODO
            case R.id.action_signout:
                Intent myIntent = new Intent(Meetup.this, SignIn.class);
                myIntent.putExtra("destroy_token", "true");
                startActivity(myIntent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    /**************************************************************************** Map Route Marking ******************************************************************/

    public void addRoute(double meetLat, double meetLong) {

        // we will treat it as though the meeting point is the way point between the tutor and student
        double originLat = mLastLocation.getLatitude();
        double originLong = mLastLocation.getLongitude();
//        double destLat = otherUser.getLatitude();
//        double destLong = otherUser.getLongitude();

        // Origin of route
        String str_origin = "origin=" + originLat + "," + originLong;

        // Destination of route
        String str_dest = "destination=" + meetLat + "," + meetLong;

//        // Waypoint (meeting point)
//        String waypoints = "waypoints=" + meetLat + "," + meetLong;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        // do http request for route in separate thread
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);

    }


    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Download Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service

            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<LatLng>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<LatLng> doInBackground(String... jsonData) {

            JSONObject result;
            JSONArray routes;
            List<LatLng> lines = new ArrayList<LatLng>();

            try {

                result = new JSONObject(jsonData[0]);
                routes = result.getJSONArray("routes");

                // route distance
                long distanceForSegment = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("value");

                JSONArray studentSteps = routes.getJSONObject(0).getJSONArray("legs")
                        .getJSONObject(0).getJSONArray("steps");

//                JSONArray tutorSteps = routes.getJSONObject(0).getJSONArray("legs")
//                        .getJSONObject(1).getJSONArray("steps");

                for (int i = 0; i < studentSteps.length(); i++) {
                    String polyline = studentSteps.getJSONObject(i).getJSONObject("polyline").getString("points");

                    for (LatLng p : decodePolyline(polyline)) {
                        lines.add(p);
                    }
                }

//                for (int i = 0; i < tutorSteps.length(); i++) {
//                    String polyline = tutorSteps.getJSONObject(i).getJSONObject("polyline").getString("points");
//
//                    for (LatLng p : decodePolyline(polyline)) {
//                        lines.add(p);
//                    }
//                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            return lines;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<LatLng> lines) {
            gMap.addPolyline(new PolylineOptions().addAll(lines).width(4).color(Color.CYAN));
        }

        /**
         * POLYLINE DECODER - http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java *
         */
        private List<LatLng> decodePolyline(String encoded) {

            List<LatLng> poly = new ArrayList<LatLng>();

            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                poly.add(p);
            }

            return poly;
        }

    }


    class TutorWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        TutorWindowAdapter() {
            myContentsView = getLayoutInflater().inflate(R.layout.tutorinfowindow, null);
        }


        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            TextView tutorName = (TextView) myContentsView.findViewById(R.id.tutorName);
            ImageView profilePic = (ImageView) myContentsView.findViewById(R.id.profilePic);

            tutorName.setText(otherUser.getName());
            int x = R.drawable.jess;
            profilePic.setImageResource(x);
            return myContentsView;
        }
    }


    class SessionWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        SessionWindowAdapter() {
            myContentsView = getLayoutInflater().inflate(R.layout.sessionstart_infowindow, null);
        }


        @Override
        public View getInfoWindow(Marker marker) {
            return null;

        }

        @Override
        public View getInfoContents(Marker marker) {
            ImageView profilePic = (ImageView) myContentsView.findViewById(R.id.profilePic);
            int x = R.drawable.jess;
            profilePic.setImageResource(x);
            return myContentsView;
        }
    }


}
