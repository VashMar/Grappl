package com.mamba.grapple;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

// *socket.io imports*
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.lang.reflect.Type;
import java.net.*; // for URIexception
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DBService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Socket socket;
    private String token;
    private final IBinder myBinder = new LocalBinder();
    private final Gson gson = new Gson();
    private static final long INTERVAL = 10000 * 10;
    private static final long FASTEST_INTERVAL = 10000 * 5;
    private boolean inGrapple = false;      // flag goes up when user is in a grapple

    List<MessageObject> conversation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

    Activity boundActivity;

    LoginManager session;
    UserObject currentUser;
    UserObject grappledUser;


    /****************************************************************************** Service Related Methods  *********************************************************************/
    @Override
    public void onCreate() {
        System.out.println("DBService Created");
        super.onCreate();

        if (!isGooglePlayServicesAvailable()){
            Log.e("Google Play Services", "Could not connect");
        }else{
            Log.v("Google Play Services", "Connected");
        }

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        conversation = new ArrayList<MessageObject>();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        mGoogleApiClient.connect();
        return START_STICKY;
    }

    public void connectSocket(){
        // set up socket connection
        if (socket == null || !socket.connected()){
            try {
                String url = "http://protected-dawn-4244.herokuapp.com" + "?token=" + session.getToken();
                Log.v("socket url", url);
                socket = IO.socket(url);
            } catch (URISyntaxException e){
                Log.e("Bad URI", e.getMessage());
            }
        }

        // create listeners
        socket.on("message", message);
        socket.on("locationUpdate", locationUpdate);
        socket.on("meetingSuggestion", meetingSuggestion);
        socket.on("startSessionRequest", startSessionRequest);
        socket.on("grapple", grapple);
        socket.on("removeAvailableDone", removeAvailableDone);


        socket.connect();
    }


    public Location getLocation(){
        return mCurrentLocation;
    }





    public IBinder onBind(Intent intent) {
       return myBinder;
    }



    public class LocalBinder extends Binder {
        public DBService getService() {
            System.out.println("I am in Localbinder ");
            return DBService.this;

        }
    }


    // sends updated socket data to UI
    private void clientBroadcast(Intent intent){
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /*********************************************************** Grapple State Management **************************************************/

    private void setGrapple(UserObject user)
    {
       inGrapple = true;
        grappledUser = user;
    }

    public boolean grappleState(){
       return inGrapple;
    }

    public void endGrapple(){
        inGrapple = false;
        grappledUser = null;
    }

    public UserObject getGrappledUser(){
        Log.v("Service", "returning grappled user: " + String.valueOf(grappledUser));
        return grappledUser;
    }

    /****************************************************************************** Chat *********************************************************************/

    public void storeConversation(List<MessageObject> convo){
       conversation = convo;
    }

    public void newConvo(){
       conversation = new ArrayList<MessageObject>();
    }

    public void addMessage(MessageObject msg){
       Log.v("Storing Message", msg+"");
       conversation.add(msg);
    }


    public List<MessageObject> retrieveConvo(){
       return conversation;
    }






    /****************************************************************************** User Session Management *********************************************************************/
    // gets the latest session from activity, also gets updated current user data
    public void setSession(LoginManager session){
        Log.v("Service Session", ""+session);
        this.session = session;
        this.currentUser = session.getCurrentUser();
    }






    /****************************************************************************** Socket Emits *********************************************************************/

    // lets a tutor broadcast their availability
    public void startBroadcast(long startTime, int period, double price, ArrayList<String> courses, ArrayList<LocationObject> selectedLocs){

        JSONObject broadcastInfo = new JSONObject();
        JSONArray tutorCourses = new JSONArray();
        JSONArray meetingSpots = new JSONArray();

        try{

            for(String course : courses){
                tutorCourses.put(course);
            }
            

            for(LocationObject loc : selectedLocs){
                meetingSpots.put(gson.toJson(loc));
            }


            broadcastInfo.put("startTime", startTime);
            broadcastInfo.put("period", period);
            broadcastInfo.put("price", price);
            broadcastInfo.put("courses", tutorCourses);
            broadcastInfo.put("meetingSpots", meetingSpots);
            broadcastInfo.put("lat", mCurrentLocation.getLatitude());
            broadcastInfo.put("lon", mCurrentLocation.getLongitude());
            Log.v("Emitting..", "Set Available");
            socket.emit("setAvailable",  broadcastInfo); //

        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    // stops tutor from broadcasting their availability
    public void endBroadcast(){
        Log.v("Emitting..", "Remove Available");
        socket.emit("removeAvailable");
    }

    // initiates the grappling of a tutor
    public void startGrapple(UserObject user) {
        Log.v("Service", "Setting grapple state to true");
        setGrapple(user); // set grapple state flag and track user

        // serialize tutor.id, lat, and long
        Location loc = getLocation();
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        String id = user.getId();

        if(id == null){
            id = session.currentUser.getId();
        }
        Log.v("grappleEvent", "id passed in is: " + id);
        try {
            JSONObject idAndLocJson = new JSONObject();
            idAndLocJson.put("id", id);
            idAndLocJson.put("lat", lat);
            idAndLocJson.put("lon", lon);
            Log.v("Emitting..", "Grapple User");
            socket.emit("grapple", idAndLocJson);
        }
        catch(JSONException ex){
            Log.e("grapple", "could build and send json");
        }
    }


    // send text message
    public void sendMessage(String senderName, String senderID, String recipID, String message){

        try{
            JSONObject messageData = new JSONObject();
            messageData.put("senderName", senderName);
            messageData.put("senderID", senderID);
            messageData.put("recipID", recipID);
            messageData.put("message", message);
            Log.v("Emitting..", "Message");
            socket.emit("message" , messageData);

        }catch(JSONException e){
            e.printStackTrace();
        }


    }

    // send location message
    public void sendMessage(String senderName, String senderID, String recipID, String message, double latitude, double longitude){

        try{
            JSONObject messageData = new JSONObject();
            messageData.put("senderName", senderName);
            messageData.put("senderID", senderID);
            messageData.put("recipID", recipID);
            messageData.put("message", message);
            messageData.put("lat", latitude);
            messageData.put("lon", longitude);
            Log.v("Emitting..", " Location Message");
            socket.emit("message" , messageData);

        }catch(JSONException e){
            e.printStackTrace();
        }


    }



    // broadcast used to update the user's rating
    public void updateRating(String userID, int updatedRating){

        JSONObject broadcastInfo = new JSONObject();
        JSONArray tutorCourses = new JSONArray();

        try{
            broadcastInfo.put("id", userID);
            broadcastInfo.put("rating", updatedRating);
            Log.v("Emitting..", "Update Rating");
            socket.emit("updateRating", broadcastInfo);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }





    /****************************************************************************** Socket Listeners *********************************************************************/

    private Emitter.Listener message = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            // parse data and create message object
            try{
                Log.v("Emit Received..", "Message" + data.toString());

                String message = data.getString("message");
                String senderID = data.getString("senderID");
                String recipID = data.getString("recipID");
                String senderName = data.getString("senderName");
                Boolean isSelf  = senderID.equals(currentUser.getId());
                LocationObject location = null;
                Log.v("Message Received" , message);

                // check if location message
                if(data.has("lat")  && data.has("lon")){
                    location = new LocationObject(Double.parseDouble(data.getString("lat")), Double.parseDouble(data.getString("lon")));
                }

                MessageObject messageObject = new MessageObject(senderName, message, senderID, recipID, isSelf, location);
                addMessage(messageObject);

                // broadcast to chat
                Intent intent = new Intent("chatReceiver");
                intent.putExtra("responseType", "message");
                intent.putExtra("msg", messageObject);
                clientBroadcast(intent);


            }catch (JSONException e){
                e.printStackTrace();
            }



        }
    };

    private Emitter.Listener locationUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            // parse data and broadcast
            // LocationObject location = gson.fromJson();


            Intent intent = new Intent("locationUpdate");
            // You can also include some extra data.
            intent.putExtra("message", "This is my message!");
        }
    };

    private Emitter.Listener meetingSuggestion = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            // parse data and broadcast
        }
    };


    private Emitter.Listener startSessionRequest = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONArray data = (JSONArray) args[0];
            // parse data and broadcast
        }
    };


    private Emitter.Listener grapple = new Emitter.Listener(){
        @Override
        public void call(final Object... args){

            Log.v("Emit Received..", "Grapple");
            JSONObject data = (JSONObject) args[0];

            try{
                String grappledUserString = data.getString("id");
                Log.v("Grappled", session.currentUser.getName() + " just got grappled by " + grappledUser);
                grappledUser = gson.fromJson(grappledUserString, UserObject.class);

                // set grapple state and track user
                setGrapple(grappledUser);

                // send to the waiting receiver on grapple
                Intent intent = new Intent("waitingReceiver");
                intent.putExtra("responseType", "grapple");
                intent.putExtra("user", grappledUser);
                clientBroadcast(intent);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    };


    private Emitter.Listener removeAvailableDone = new Emitter.Listener(){

        @Override
        public void call(final Object... args) {
            Log.v("Received response", "End Broadcast");

            JSONObject data = (JSONObject) args[0];

            try{
                // get responses from server and multicast them out
                String responseType = data.getString("responseType");
                Intent intent = new Intent("multicastReceiver");
                intent.putExtra("responseType", responseType);
                clientBroadcast(intent);

            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    };



    /****************************************************************************** Location Services *********************************************************************/
    @Override
    public void onConnected(Bundle bundle) {
        Log.v("Location Api Connected" , String.valueOf(mGoogleApiClient.isConnected()));
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location Changed", "Firing onLocationChanged...");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.d("New Location: ", "("  + mCurrentLocation.getLatitude() + " , " + mCurrentLocation.getLongitude() + " )");
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("Connection Failed", ""+connectionResult);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            return false;
        }
    }

    protected void startLocationUpdates(){
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }



}