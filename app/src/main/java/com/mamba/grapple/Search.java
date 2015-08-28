
package com.mamba.grapple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;


// *json imports*
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;


public class Search extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean loggedIn = false;
    SharedPreferences sharedPreferences;


    // UI Elements
    ListView listView;
    SeekBar seekBar;
    TextView distanceView;
    Button search;
    View selected = null;
    ProgressBar spinner;

    // Request Params
    int distance = 0;
    String course;
    String currLat;
    String currLong;



    // current url path for tutor list retrieval
    static final String TUTOR_PATH = "http://protected-dawn-4244.herokuapp.com/tutors";


    public Search(){
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        // grab all the view items and set defaults
        initialize();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.v("List Item", position + "selected");

                // If a previous item was selected unhighlight it
                if (selected != null) {
                    selected.setBackgroundColor(Color.TRANSPARENT);
                    parent.getChildAt(0).setBackgroundColor(Color.TRANSPARENT);
                }

                // highlight the selected item
                selected = view;
                selected.setBackgroundColor(Color.rgb(62, 175, 212));

            }
        });




        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        mLastLocation = locationManager.getLastKnownLocation(locationProvider);



    }

//    @Override
//    public void onStart(){
//        super.onStart();
//        // grab all the view items and set defaults
//        initialize();
//    }
//



    // A private method to help us initialize our default variables and settings
    private void initialize() {
        listView = (ListView) getView().findViewById(R.id.list);
        search = (Button) getView().findViewById(R.id.search_button);
        spinner = (ProgressBar) getView().findViewById(R.id.spinner);


        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Search", "Search Selected");
                if (((Main) getActivity()).network.getConnectivityStatus(getActivity().getApplicationContext()) != 0) {
                    tutorSearch(v);
                } else {
                    ((Main) getActivity()).noConnectionDialog();
                }
            }
        });



        //add elements from array to list view
        listView.setAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.row, ((Main)getActivity()).courseList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                final View renderer = super.getView(position, convertView, parent);
                if (position == 0) {
                    // highlight the first list item by default
                    if(selected == null){
                        selected = renderer;
                        selected.setBackgroundColor(Color.rgb(62, 175, 212));
                    }

                }
                return renderer;
            }
        });



    }


    // on search button click get the relevant  tutor list and show results
    public void tutorSearch(View view){

        course = String.valueOf(((TextView) selected).getText());
        Log.v("distance", "" + distance);
        Log.v("course", course);


        // make sure we have the GPS location
        if(mLastLocation != null) {

            currLat = String.valueOf(mLastLocation.getLatitude());
            currLong = String.valueOf(mLastLocation.getLongitude());

        }else {
            currLat = "43.076592";
            currLong = "-89.412487";
        }


        // log the current coordinates
        Log.v("currentLocation", "(" + currLat + "," + currLong + ")");


        //  send the data in a http request
        ConnectivityManager conMgr = (ConnectivityManager)
            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
        // if there is a network connection create a request thread
        if(networkInfo != null && networkInfo.isConnected()){
           new TutorRetrieval().execute(TUTOR_PATH);
        }else{
            Log.v("no connection", "Failed to connect to internet");
        }




    }



    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        Log.v("gConnected", "Connected to google play services");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

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



    private ArrayList<String> getCourseList(){
        ArrayList<String> classList = null;
        try {
            Gson gson = new Gson();
            String dummyUrl = null; // TODO remove this

            String json = downloadUrl("");//urls[0]); // TODO
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            classList = gson.fromJson(json, type);
            return classList;
        } catch (IOException e) {
            Log.e("IO","Unable to retrieve web page. URL may be invalid.");
            return classList;
        }
    }


    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class TutorRetrieval extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);

            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPreExecute(){
            spinner.setVisibility(View.VISIBLE);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result){
            Log.v("postResult", result);
            Gson gson = new Gson();

            ArrayList<UserObject> tutorList = new ArrayList<>();
            Type resultType = new TypeToken<ArrayList<UserObject>>(){}.getType();
            tutorList = gson.fromJson(result, resultType);

            Intent intent = new Intent(getActivity(), Results.class);

            Log.v("tutorList", String.valueOf(tutorList.size()));

            if(tutorList.size() > 0){
                Log.v("Tutor: " , tutorList.get(0).toString());
            }

            // send the tutorList along with login status on to the results activity
            intent.putParcelableArrayListExtra("tutorList", tutorList);
            intent.putExtra("course", course);
            startActivity(intent);
            spinner.setVisibility(View.GONE);
        }
    }



    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder;
        String line;

        // add all tutor request data to params list and build url query
        Uri.Builder builder = new Uri.Builder()
                .appendQueryParameter("course", course )
                .appendQueryParameter("distance", String.valueOf(distance))
                .appendQueryParameter("lat", currLat )
                .appendQueryParameter("lon", currLong);
        String query = builder.build().getEncodedQuery();

        myurl += "?" + query;           // append encoded query to URL
        Log.v("queriedURL", myurl);
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
            String jsonString = stringBuilder.toString();
            return jsonString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }








}

//////////////////// EXPANDABLE LIST CODE //////////////////////////////////////////////////////////////////////
//  private ArrayList<String> parentItems = new ArrayList<String>();
//  private ArrayList<Object> childItems = new ArrayList<Object>();
///
//       //  ON CREATE /////////////////////
//
//        ExpandableListView expandableList = getExpandableListView();
//
//        expandableList.setDividerHeight(2);
//        expandableList.setGroupIndicator(null);
//        expandableList.setClickable(true);
//
//        setGroupParents();
//        setChildData();
//
//        ExpandableAdapter adapter = new ExpandableAdapter(parentItems, childItems);
//
//        adapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), this);
//        expandableList.setAdapter(adapter);
//        expandableList.setOnChildClickListener(this);
//
//        //////////////////////////////////

//    public void setGroupParents() {
//        parentItems.add("Computer Sciences");
//        parentItems.add("Mathematics");
//        parentItems.add("Physics");
//        parentItems.add("etc.");
//    }
//
//    public void setChildData() {
//
//        // Computer Sciences
//        ArrayList<String> child = new ArrayList<String>();
//        child.add("CS302- Intro to Java");
//        //child.add("");
//        child.add("CS540- Intro to AI");
//        child.add("CS577- Into to Algorithms");
//        childItems.add(child);
//
//        // Mathematics
//        child = new ArrayList<String>();
//        child.add("MATH221-Calc I");
//        childItems.add(child);
//
//        // Physics
//        child = new ArrayList<String>();
//        child.add("PHYS201- Physics I");
//        childItems.add(child);
//
//        // etc.
//        child = new ArrayList<String>();
//        child.add("test");
//        childItems.add(child);
//    }

/*
public class Search extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    SharedPreferences sharedPreferences;

    // UI Elements
    ListView listView;
    SeekBar seekBar;
    TextView distanceView;
    Button search;
    View selected = null;

    // Request Params
    int distance = 0;
    String course;
    String currLat;
    String currLong;

//    // service related variables
//    private boolean mBound = false;
//    DBService mService;

    // temporary until DB load setup (use SimpleCursorAdapter for DB)
    static final String[] COURSES = {"Chemistry 103", "Comp Sci 302", "French 4", "Math 234", "Physics 202"};
    // current url path for tutor list retrieval
    static final String TUTOR_PATH = "http://protected-dawn-4244.herokuapp.com/tutors";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        // grab all the view items and set defaults
        initialize();

        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        mLastLocation = locationManager.getLastKnownLocation(locationProvider);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // If a previous item was selected unhighlight it
                if(selected != null){
                    selected.setBackgroundColor(Color.TRANSPARENT);
                    parent.getChildAt(0).setBackgroundColor(Color.TRANSPARENT);
                }

                // highlight the selected item
                selected = view;
                selected.setBackgroundColor(Color.rgb(62, 175, 212));

            }
        });


        // update distance as user slides
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distance = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                distanceView.setText("Travel Distance: " + distance + " mi");
            }
        });


    }


    // check login status every time the activity gets shown
    public void onResume(){
        super.onResume();

    }


    // A private method to help us initialize our default variables and settings
    private void initialize() {
        seekBar = (SeekBar) getView().findViewById(R.id.seekBar2);
        listView = (ListView) getView().findViewById(R.id.list);
        distanceView = (TextView) getView().findViewById(R.id.textView5);
        search = (Button) getView().findViewById(R.id.button);

        //add elements from array to list view
        listView.setAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.row, COURSES){

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                final View renderer = super.getView(position, convertView, parent);
                if (position == 0)
                {
                    // highlight the first list item by default
                    renderer.setBackgroundColor(Color.rgb(62, 175, 212));
                }
                return renderer;
            }
        });


        // select first list item
        selected = listView.getAdapter().getView(0, null, listView);


        // set initial distance
        distance = seekBar.getProgress();
        distanceView.setText("Travel Distance: " + distance + " mi");

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorSearch(v);
            }
        });

    }


    // on search button click get the relevant  tutor list and show results
    public void tutorSearch(View view){

        course = String.valueOf(((TextView) selected).getText());
        Log.v("distance", "" + distance);
        Log.v("course", course);


        // make sure we have the GPS location
        if(mLastLocation != null) {

            currLat = String.valueOf(mLastLocation.getLatitude());
            currLong = String.valueOf(mLastLocation.getLongitude());

        }else {
            currLat = "43.076592";
            currLong = "-89.412487";
        }


            // log the current coordinates
            Log.v("currentLocation", "(" + currLat + "," + currLong + ")");


            //  send the data in a http request
            ConnectivityManager conMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
            // if there is a network connection create a request thread
            if(networkInfo != null && networkInfo.isConnected()){
               new TutorRetrieval().execute(TUTOR_PATH);
            }else{
                Log.v("no connection", "Failed to connect to internet");
            }




    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        Log.v("gConnected", "Connected to google play services");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

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


    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class TutorRetrieval extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);

            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result){
            Log.v("postResult", result);
            Gson gson = new Gson();

            ArrayList<TutorObject> tutorList = new ArrayList<>();
            Type resultType = new TypeToken<ArrayList<TutorObject>>(){}.getType();
            tutorList = gson.fromJson(result, resultType);

            Intent intent = new Intent(getActivity(), Results.class);

            Log.v("tutorList", String.valueOf(tutorList.size()));

            // dummy populate the empty list for now
            if(tutorList.size() < 1){
                dummyPopulate(tutorList);
            }

            // send the tutorList along with login status on to the results activity
            intent.putParcelableArrayListExtra("tutorList", tutorList);
            intent.putExtra("distance", distance);
            startActivity(intent);

        }
    }



    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder;
        String line;

        // add all tutor request data to params list and build url query
        Uri.Builder builder = new Uri.Builder()
                .appendQueryParameter("course", course )
                .appendQueryParameter("distance", String.valueOf(distance))
                .appendQueryParameter("lat", currLat )
                .appendQueryParameter("lon", currLong);
        String query = builder.build().getEncodedQuery();

        myurl += "?" + query;           // append encoded query to URL
        Log.v("queriedURL", myurl);
        try {
            Log.v("url", myurl);
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.v("url", String.valueOf(url));
            conn.setReadTimeout(10000 // milliseconds );
            conn.setConnectTimeout(15000 // milliseconds );
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
            String jsonString = stringBuilder.toString();
            return jsonString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

}
*/

//////////////////// EXPANDABLE LIST CODE //////////////////////////////////////////////////////////////////////
//  private ArrayList<String> parentItems = new ArrayList<String>();
//  private ArrayList<Object> childItems = new ArrayList<Object>();
///
//       //  ON CREATE /////////////////////
//
//        ExpandableListView expandableList = getExpandableListView();
//
//        expandableList.setDividerHeight(2);
//        expandableList.setGroupIndicator(null);
//        expandableList.setClickable(true);
//
//        setGroupParents();
//        setChildData();
//
//        ExpandableAdapter adapter = new ExpandableAdapter(parentItems, childItems);
//
//        adapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), this);
//        expandableList.setAdapter(adapter);
//        expandableList.setOnChildClickListener(this);
//
//        //////////////////////////////////

//    public void setGroupParents() {
//        parentItems.add("Computer Sciences");
//        parentItems.add("Mathematics");
//        parentItems.add("Physics");
//        parentItems.add("etc.");
//    }
//
//    public void setChildData() {
//
//        // Computer Sciences
//        ArrayList<String> child = new ArrayList<String>();
//        child.add("CS302- Intro to Java");
//        //child.add("");
//        child.add("CS540- Intro to AI");
//        child.add("CS577- Into to Algorithms");
//        childItems.add(child);
//
//        // Mathematics
//        child = new ArrayList<String>();
//        child.add("MATH221-Calc I");
//        childItems.add(child);
//
//        // Physics
//        child = new ArrayList<String>();
//        child.add("PHYS201- Physics I");
//        childItems.add(child);
//
//        // etc.
//        child = new ArrayList<String>();
//        child.add("test");
//        childItems.add(child);
//    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////