package com.mamba.grapple;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

// *socket.io imports*
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Manager;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Url;
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
import com.pushbots.push.Pushbots;


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
    private boolean inGrapple = false;  // flag goes up when user is in a grapple
    private boolean inMeetup = false;   // flag goes up when user is going to meeting point
    private  boolean inSesh = false;    // flag goes up when session has begun
    private boolean inView = false;     // tracks if the app is open or not
    private String deviceID = "";
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
        Log.v("Service", "Connecting Socket..");

        // set up socket connection
        if (socket == null || !socket.connected()){
            try {
                String url = "http://protected-dawn-4244.herokuapp.com" + "?token=" + session.getToken();
                Log.v("socket url", url);
                IO.Options options = new IO.Options();
                if(socket == null){
                    options.forceNew = true;
                }


                socket = IO.socket(url, options);
                // create listeners
                socket.on("message", message);
                socket.on("locationUpdate", locationUpdate);
                socket.on("meetingSuggestion", meetingSuggestion);
                socket.on("startSessionRequest", startSessionRequest);
                socket.on("grapple", grapple);
                socket.on("removeAvailableDone", removeAvailableDone);
                socket.on("sessionUpdated", sessionUpdated);
                socket.on("updatedPic", updatedPic);
                socket.on("grapplFail", grapplFail);
                socket.on("grapplSuccess", grapplSuccess);
                socket.on("grapplEnded", grapplEnded);
                socket.on("startMeetup", startMeetup);
                socket.on("sessionRequest", sessionRequest);
                socket.on("startSession", startSession);
                socket.on("sessionEnded", sessionEnded);
                socket.on(Socket.EVENT_DISCONNECT, reconnect);
//                socket.on(Socket.EVENT_CONNECT, sendDevID);
                socket.connect();

            } catch (URISyntaxException e){
                Log.e("Bad URI", e.getMessage());
            }
        }


    }

    public void endConnection(){
        Log.v("Service", "Ending Connection..");
        currentUser = null;
        socket.disconnect();
        socket = null;
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

    public void inView(){
        inView = true;
    }

    public void outOfView(){
        inView = false;
    }


    // sends updated socket data to UI
    private void clientBroadcast(Intent intent){
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /*********************************************************** Grappl State Management **************************************************/

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
        inMeetup = false;
        grappledUser = null;
    }

    public UserObject getGrappledUser(){
        Log.v("Service", "returning grappled user: " + String.valueOf(grappledUser));
        return grappledUser;
    }

    public boolean inMeetup(){
        return inMeetup;
    }

    public void setMeetup(){
        inMeetup = true;
    }

    public boolean inSesh(){
        return inSesh;
    }

    public void setSesh(){
        inSesh = true;
    }

    public void resetStates(){
        inSesh = false;
        inGrapple = false;
        inMeetup = false;
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


    private LocationObject findMeetingSpot(String place){
        for(int i = 0; i < currentUser.getMeetingSpots().size(); i++){
            if(currentUser.getMeetingSpots().get(i).getName().equals(place)){
                return currentUser.getMeetingSpots().get(i);
            }
        }

        return null;
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

        if (socket == null || !socket.connected()){
            Log.v("Socket is null", "Reconnecting..");
            connectSocket();

        }

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

    public void setDeviceID(String id){
        if(!id.equals(deviceID)){
            deviceID = id;
            Log.v("Emitting Device ID ", deviceID);
            socket.emit("deviceID", deviceID);
        }

    }

    // stops tutor from broadcasting their availability
    public void endBroadcast(){
        Log.v("Emitting..", "Remove Available");
        socket.emit("removeAvailable");
    }

    // initiates the grappling of a tutor
    public void startGrapple(UserObject user, String selectedLocation){
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
            idAndLocJson.put("place", selectedLocation);
            idAndLocJson.put("lat", lat);
            idAndLocJson.put("lon", lon);
            Log.v("Emitting..", "Grapple User");
            socket.emit("grapple", idAndLocJson);
        }
        catch(JSONException ex){
            Log.e("grapple", "could build and send json");
        }
    }


    public void cancelGrappl(){
        socket.emit("cancelGrappl");
        endGrapple();
    }

    public void grapplSuccess(String id){
        try{
            JSONObject data = new JSONObject();
            data.put("id", id);
            Log.v("Emitting..", "grapplSuccess");
            socket.emit("grapplSuccess", data);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    public void startMeetup(){
        socket.emit("startMeetup");
    }

    public void sessionRequest(){
        socket.emit("sessionRequest");
    }

    public void sessionAccept(){
        Log.v("Emitting..", "sessionAccept");
        socket.emit("sessionAccept");
        setSesh();
    }

    public void endSession(long ms){
        try{
            JSONObject data = new JSONObject();
            data.put("time", ms);
            socket.emit("endSession", data);
            resetStates();
        }catch(JSONException e){

        }

    }

//    public void sessionStarted(long ms){
//        try{
//            JSONObject data = new JSONObject();
//            data.put("sessionTime", ms);
//            Log.v("Emitting..", "sessionStarted");
//            socket.emit("sessionStarted", data);
//        }catch(JSONException e){
//            e.printStackTrace();
//        }
//    }


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


    // broadcast used to update the user's rating
    public void updateRating(boolean isTutor, float starRating){

        JSONObject broadcastInfo = new JSONObject();

        try{
            broadcastInfo.put("isTutor", isTutor);
            broadcastInfo.put("rating", starRating);
            Log.v("Emitting..", "Update Rating");
            socket.emit("updateRating", broadcastInfo);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    public void updateProfilePic(String ref){
        JSONObject data = new JSONObject();
        try{
            data.put("ref", ref);
            socket.emit("updateProfilePic", data);
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
                Intent chatIntent = new Intent("grapplReceiver");
                chatIntent.putExtra("responseType", "message");
                chatIntent.putExtra("msg", messageObject);
                clientBroadcast(chatIntent);


            }catch (JSONException e){
                e.printStackTrace();
            }



        }
    };

    private Emitter.Listener grapplFail = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.v("Emit Received..", "grapplFail");
            Intent intent = new Intent("grapplReceiver");
            intent.putExtra("responseType", "grapplFail");
            clientBroadcast(intent);
        }
    };


    private Emitter.Listener grapplSuccess = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.v("Emit Received..", "grapplSuccess");
            Intent intent = new Intent("grapplReceiver");
            intent.putExtra("responseType", "grapplSuccess");
            clientBroadcast(intent);
        }
    };

    private Emitter.Listener grapplEnded = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            Log.v("Emit Received..", "grapplEnded");
            Intent intent = new Intent("grapplReceiver");
            intent.putExtra("responseType", "grapplEnded");
            clientBroadcast(intent);
        }
    };

    private Emitter.Listener sessionRequest = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            Log.v("Emit Received..", "sessionRequest");
            Intent intent = new Intent("grapplReceiver");
            intent.putExtra("responseType", "sessionRequest");
            clientBroadcast(intent);
        }
    };



    private Emitter.Listener startSession = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            Log.v("Emit Received..", "startSession");
            Intent intent = new Intent("grapplReceiver");
            intent.putExtra("responseType", "startSession");
            setSesh();
            clientBroadcast(intent);
        }
    };



    private Emitter.Listener startMeetup = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.v("Emit Received..", "startMeetup");
            Intent intent = new Intent("grapplReceiver");
            intent.putExtra("responseType", "startMeetup");
            clientBroadcast(intent);
            setMeetup();
        }
    };

    private Emitter.Listener reconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if(currentUser != null){
                Log.v("Socket Disconnected", "Reconnecting..");
                socket.connect();
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



    private Emitter.Listener updatedPic = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            // parse data and broadcast
            // LocationObject location = gson.fromJson();

            try{
                String profilePic = data.getString("profilePic");
                Log.v("Updated Prof Pic", profilePic);
                Intent intent = new Intent("multicastReceiver");
                intent.putExtra("responseType", "updatedPic");
                intent.putExtra("profilePic", profilePic);
                clientBroadcast(intent);

            }catch (JSONException e){
                e.printStackTrace();
            }

        }
    };

    private Emitter.Listener sessionUpdated = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.v("Emit Received..", "Session Updated");
            JSONObject data = (JSONObject) args[0];
            // parse data and broadcast
            try{
                String updatedSession = data.getString("session");
                Log.v("Updated Session", updatedSession);
                TutorSession session = gson.fromJson(updatedSession, TutorSession.class);
                Intent intent = new Intent("mainReceiver");
                intent.putExtra("responseType", "updatedSession");
                intent.putExtra("session", session);
                clientBroadcast(intent);

            }catch(JSONException e){
                e.printStackTrace();
            }

        }
    };

    private Emitter.Listener sessionEnded = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.v("Emit Received..", "Session Ended");
            JSONObject data = (JSONObject) args[0];
            // parse data and broadcast
            try{
                String sessionTime = data.getString("time");
                Log.v("sessionTime", sessionTime);
                long seshTime = Long.parseLong(sessionTime);
                Intent intent = new Intent("seshReceiver");
                intent.putExtra("responseType", "sessionEnded");
                intent.putExtra("seshTime", seshTime);
                clientBroadcast(intent);

            }catch(JSONException e){
                e.printStackTrace();
            }

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
                String grappledUserString = data.getString("user");
                Log.v("Grappled", session.currentUser.getName() + " just got grappled by " + grappledUser);
                grappledUser = gson.fromJson(grappledUserString, UserObject.class);

                // set grapple state and track user
                setGrapple(grappledUser);

                // emit a grapplSuccess to grapple
                grapplSuccess(grappledUser.getId());

                // if the user has the app open redirect to chat
                if(inView){
                    // send to the waiting receiver on grapple
                    Intent intent = new Intent("waitingReceiver");
                    intent.putExtra("responseType", "grapple");
                    intent.putExtra("user", grappledUser);
                    intent.putExtra("place", data.getString("place"));
                    clientBroadcast(intent);

                }else{ // send a notification
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(DBService.this)
                                    .setSmallIcon(R.drawable.notification_icon)
                                    .setContentTitle("You've been Grappled")
                                    .setContentText(grappledUser.firstName() + " has grappled you")
                                    .setVisibility(1)
                                    .setVibrate(new long[2])
                                    .setPriority(Notification.PRIORITY_MAX);

                            int mId = 12345;

                            LocationObject meetingSpot = findMeetingSpot(data.getString("place"));
                            // Creates an explicit intent for an Activity in your app
                            Intent resultIntent = new Intent(DBService.this, Chat.class);
                                    resultIntent.putExtra("responseType", "grapple");
                                    resultIntent.putExtra("user", grappledUser);
                                    resultIntent.putExtra("meetingSpot", meetingSpot);

                            // The stack builder object will contain an artificial back stack for the
                            // started Activity.
                            // This ensures that navigating backward from the Activity leads out of
                            // your application to the Home screen.
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(DBService.this);
                            // Adds the back stack for the Intent (but not the Intent itself)
                            stackBuilder.addParentStack(Main.class);
                            // Adds the Intent that starts the Activity to the top of the stack
                            stackBuilder.addNextIntent(resultIntent);
                            PendingIntent resultPendingIntent =
                                    stackBuilder.getPendingIntent(
                                            2,
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    );

                            if(Build.VERSION.SDK_INT < 21){
                                mBuilder.setFullScreenIntent(resultPendingIntent, true);
                            }else{
                                mBuilder.setContentIntent(resultPendingIntent);
                            }


                            NotificationManager mNotificationManager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            // mId allows you to update the notification later on.
                            mNotificationManager.notify(mId, mBuilder.build());
                }

                // stop broadcasting
                endBroadcast();



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