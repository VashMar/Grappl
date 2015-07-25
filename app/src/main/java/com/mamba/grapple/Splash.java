package com.mamba.grapple;

// *android imports*

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mamba.grapple.DBService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Splash extends Activity {

    /**
     * Duration of wait *
     */
    private final int SPLASH_DISPLAY_LENGTH = 2000;
    public final static String EXTRA_MESSAGE = "com.mamba.grapple.MESSAGE";

    private String token;
    private boolean mBound = false;
    DBService mService;


    LoginManager session;
    SharedPreferences sharedPref;

    String LOC_PATH = "http://protected-dawn-4244.herokuapp.com/locations";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        // check to see if the user is logged in here
        session = new LoginManager(getApplicationContext());
        sharedPref = getSharedPreferences("locations", 0);
//        loginCheck();

        if (session.isLoggedIn()) {
            // start the background networking thread and open up socket connection
            Log.v("DBService", "Binding DBService from Splash..");
            // we must start and bind the service so we have control of its lifecycle
            startService(new Intent(this, DBService.class));
            bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        }

        loadLocations();

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent mainIntent = new Intent(Splash.this, Main.class);
                Splash.this.startActivity(mainIntent);
                Splash.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setSession(session);
            mService.connectSocket();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



    public void loadLocations(){
        //  send the data in a http request
        ConnectivityManager conMgr = (ConnectivityManager)
               getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
        // if there is a network connection create a request thread
        if(networkInfo != null && networkInfo.isConnected()){
            new LocationRetrieval().execute(LOC_PATH);
        }else{
            Log.v("no connection", "Failed to connect to internet");
        }
    }




    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class LocationRetrieval extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls){

            // params comes from the execute() call: params[0] is the url.
            try {
                 getLocs(urls[0]);
                return "";

            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPreExecute(){

        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result){

        }
    }



    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private void getLocs(String myurl) throws IOException {
        InputStream is = null;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder;
        String line;

        try {
            Log.v("url", myurl);
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.v("url", String.valueOf(url));
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);


            // Starts the query
            conn.connect();

            int response = conn.getResponseCode();

            Log.v("response", String.valueOf(response));

            is = conn.getInputStream();

            // Convert the InputStream into a JSON string
            bufferedReader = new BufferedReader(new InputStreamReader(is));
            stringBuilder = new StringBuilder();


            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + '\n');
            }


            String result = stringBuilder.toString();

            Log.v("Returned Locations", result);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("locations", result);
            editor.commit();

            Log.v("Preferences", "Locations Changed");


            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }



}
