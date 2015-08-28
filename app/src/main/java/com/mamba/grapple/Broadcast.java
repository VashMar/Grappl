package com.mamba.grapple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.CountDownTimer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.internal.zzhl.runOnUiThread;


public class Broadcast extends Fragment {

    private OnFragmentInteractionListener mListener;
    private LocationsAdapter locationsAdapter;
    private List<LocationObject> locationList;
    private ArrayList<LocationObject> selectedLocations;
    private LocationObject selectedLocation;

    // settings view elements
    TextView timeLength;
    TextView availText;
    TextView locText;
    TextView courseText;
    TextView priceText;
    TextView courseTitle;
    TextView meetupTitle;
    TextView whenTitle;
    TextView priceTitle;
    TextView countdownTitle;
    TextView timeView;
    ImageButton locationButton;
    ImageButton timeButton;
    ImageButton priceButton;
    ImageButton courseButton;
    Button broadcastButton;
    Button cancelButton;
    ProgressBar spinner;
    LinearLayout courseContainer;
    SeekBar seekTime;
    Spinner dropdown;

    // broadcast variables
    private int availableTime;
    private double hrRate;
    private long broadcastMS;
    private ArrayList<String> selectedCourses;
    private String selectedCourse;

    //dialog variables (set to defaults)
    private String whenAvailable;
    private String lengthAvailable;
    private String availableString;
    private int lengthHr;
    private int lengthMin;
    private String priceString;
    private int broadcastDate;
    private int broadcastMonth;
    private int broadcastYear;
    private String startTime;
    private String endTime = "";
    private boolean inView = true;



    MapView mapView;
    private GoogleMap gMap;


    Intent countdownIntent;

    // Date/Time related
    TimePicker timePicker;
    Calendar rightNow;
    CounterClass timer;

    LoginManager session;
    UserObject currentUser;

    Bundle savedState;

    public Broadcast(){
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        //initialize  presets
        selectedLocations = new ArrayList<LocationObject>();
        selectedCourses = new ArrayList<String>();
        whenAvailable = "Now";
        lengthAvailable = "2 hours";
        availableString = whenAvailable+", "+lengthAvailable;
        availableTime = 120;
        hrRate = 10.00;
        lengthHr = 0;
        broadcastMS = 0;
        lengthMin = availableTime;

        savedState = savedInstanceState;

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_broadcast, container, false);
    }

    @Override
    public void onStart(){
        Log.v("Broadcast Settings", "Started");
        super.onStart();
        currentUser = ((Main) getActivity()).currentUser;
        session = ((Main) getActivity()).session;

        // grab UI elements
        broadcastButton = (Button) getView().findViewById(R.id.broadcastButton);
        cancelButton = (Button) getView().findViewById(R.id.cancelButton);
        spinner = (ProgressBar) getView().findViewById(R.id.spinner);
        courseButton  = (ImageButton) getView().findViewById(R.id.courseButton);
        locationButton = (ImageButton) getView().findViewById(R.id.locationButton);
        timeButton = (ImageButton) getView().findViewById(R.id.timeButton);
        priceButton = (ImageButton) getView().findViewById(R.id.priceButton);
        locText = (TextView) getView().findViewById(R.id.locationsText);
        courseText = (TextView) getView().findViewById(R.id.courseText);
        availText = (TextView) getView().findViewById(R.id.availText);
        priceText = (TextView) getView().findViewById(R.id.priceText);
        courseTitle = (TextView) getView().findViewById(R.id.courseTitle);
        meetupTitle = (TextView) getView().findViewById(R.id.meetupTitle);
        whenTitle = (TextView) getView().findViewById(R.id.whenTitle);
        priceTitle = (TextView) getView().findViewById(R.id.priceTitle);
        countdownTitle = (TextView) getView().findViewById(R.id.countdownTitle);
        timeView = (TextView) getView().findViewById(R.id.timeView);
        courseContainer = (LinearLayout) getView().findViewById(R.id.courseContainer);




        locText.setMovementMethod(new ScrollingMovementMethod());

        // give main control of spinner
        ((Main) getActivity()).setSpinner(spinner);

        // get meeting locations
        locPopulate();

        // handles broadcast button click
        broadcastButton.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v){

               if(((Main) getActivity()).network.getConnectivityStatus(getActivity().getApplicationContext()) != 0){
                   // if they hit the broadcast button and aren't logged in, redirect them to the login
                   if(!((Main) getActivity()).session.isLoggedIn()){
                       // transfer the user to the register page
                       Intent intent = new Intent(getActivity(), SignIn.class);

                       // we expect the auth response
                       startActivityForResult(intent, 1);
                   }else{
                       startBroadcast();
                   }
               }else{
                   ((Main) getActivity()).noConnectionDialog();
               }

           }
        });

        courseButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                showCourses(v.getContext());
            }
        });

        //shows location list on location button click
        locationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                spinner.setVisibility(View.VISIBLE);
                locationButton.setEnabled(false);
                showLocationList(v.getContext());
            }
        });

        // shows broadcast scheduling dialog on click
        timeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                broadcastSchedule(v.getContext());
            }
        });

        priceButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                setPrice(v.getContext());
            }
        });


        // if the user has a future broadcast pending show the timer
        if(currentUser != null && session.getFutureBroadcast()){
            selectedLocations = currentUser.getMeetingSpots();
            selectedCourses = session.getSelectedCourses();
            availableString = session.getAvailString();
            availableTime = currentUser.sessionLength();
            hrRate = currentUser.getPrice();
            priceString = String.format("%.2f", hrRate);
            broadcastMS = currentUser.getSessionStart();
            setAllTextViews();
            countdownView();
        }


    }

    @Override
    public void onResume(){
        Log.v("Broadcast Settings", "Resumed");
        super.onResume();
        inView = true;

        if(countdownIntent != null){
            startActivity(countdownIntent);
            countdownIntent = null;
        }

        // if the selected locations list isn't empty, show them
        if(selectedLocations.size() > 0){
            insertLocText();
        }

        broadcastButton.setEnabled(true);


    }

    @Override
    public void onPause(){
        super.onPause();
        Log.v("Broadcast Activity", "Paused");
        inView = false;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(timer != null){
            timer.cancel();
        }
    }




    public void startBroadcast() {
        if(selectedLocations.size() > 0 && selectedCourses.size() > 0){

            if(broadcastMS == 0 || broadcastMS <= rightNow.getTimeInMillis()){
                Log.v("Starting Broadcast..", "Broadcast initiated");
                Log.v("Broadcast MS", broadcastMS+"");
                Log.v("Available Time", availableTime+"");
                Log.v("Hourly Rate", hrRate+"");
                Log.v("Selected Course", selectedCourses + "");
                Log.v("Selected Locations", selectedLocations + "");
                spinner.setVisibility(View.VISIBLE);
                broadcastButton.setEnabled(false);

                Intent intent = new Intent(getActivity(), Waiting.class);
                intent.putParcelableArrayListExtra("meetingSpots", selectedLocations);
                intent.putExtra("hrRate", hrRate);
                intent.putStringArrayListExtra("selectedCourses", selectedCourses);
                intent.putExtra("available", availableTime);
                ((Main) getActivity()).setBroadCastIntent(intent);


            }else{
                countdownView();
                session.setFutureBroadcast();
            }

            // notify backend of the broadcast
            ((Main) getActivity()).mService.startBroadcast(broadcastMS, availableTime, hrRate, selectedCourses, selectedLocations);

            //store the available string
            session.storeAvailString(availableString);
            session.storeSelectedCourses(selectedCourses);



        }else{
            // we can only broadcast if the tutor has a selected a meeting spot
            if(selectedCourses.size() < 1){
                courseText.setError("You must choose a course!");
            }

            if(selectedLocations.size() < 1){
                locText.setError("You must choose a preferred meeting spot!");
            }

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void showCourses(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setCancelable(true);

        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.dialog_courses, null);

        ListView listView = (ListView) view.findViewById(R.id.courseList);
        Button selectBtn = (Button) view.findViewById(R.id.selectBtn);


        //add elements from array to list view, show previously selected items
        listView.setAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.row, ((Main) getActivity()).courseList) { // TODO swap COURSES with courseList

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final View renderer = super.getView(position, convertView, parent);
                if (selectedCourses.contains(((Main) getActivity()).courseList.get(position))) {
                    Log.v("Position", "" + position);
                    renderer.setBackgroundColor(Color.rgb(62, 175, 212));
                } else {
                    renderer.setBackgroundColor(Color.TRANSPARENT);
                }


                return renderer;
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.v("Item Click Position", "" + position);
                // get the selected course
                selectedCourse = ((Main) getActivity()).courseList.get(position);

                // If a previous item was selected unhighlight it and remove it from the list
                if (selectedCourses.contains(selectedCourse)) {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    selectedCourses.remove(selectedCourse);
                } else {
                    // highlight the selected item
                    view.setBackgroundColor(Color.rgb(62, 175, 212));
                    selectedCourses.add(selectedCourse);
                }

            }
        });



        TextView title = new TextView(getActivity());

        // title info
        title.setText("Select Your Courses For Tutoring");
        title.setGravity(Gravity.CENTER);
        title.setTextSize(20);
        title.setHeight(200);


        dialog.setCustomTitle(title);


        dialog.setView(view);
        final AlertDialog alert = dialog.show();

        // on set button click
        selectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // close dialog, take chosen courses and display them
                insertCourseText();
                alert.cancel();
            }
        });


    }

    public void setPrice(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setCancelable(true);


        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.dialog_priceset, null);
        final NumberPicker numPicker1 = (NumberPicker) view.findViewById(R.id.numberPicker);
        final NumberPicker numPicker2 = (NumberPicker) view.findViewById(R.id.numberPicker2);
        final NumberPicker numPicker3 = (NumberPicker) view.findViewById(R.id.numberPicker3);

        // initialize the num pickers
        numPicker1.setMinValue(10);
        numPicker1.setMaxValue(50);
        numPicker2.setMinValue(0);
        numPicker2.setMaxValue(9);
        numPicker3.setMinValue(0);
        numPicker3.setMaxValue(9);

        Button setBtn = (Button) view.findViewById(R.id.setBtn);


        TextView title = new TextView(getActivity());

        // title info
        title.setText("Set Your Hourly Price");
        title.setGravity(Gravity.CENTER);
        title.setTextSize(20);

        dialog.setCustomTitle(title);


        dialog.setView(view);
        final AlertDialog alert = dialog.show();

        // on set button click
        setBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                priceString = numPicker1.getValue() + "." + numPicker2.getValue() + "" + numPicker3.getValue();
                // display price, close dialog
                insertPriceText();
                alert.cancel();
            }
        });

    }

    public void broadcastSchedule(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setCancelable(true);
        rightNow = Calendar.getInstance();
        final int currHour = rightNow.get(Calendar.HOUR_OF_DAY);
        final int currMin = rightNow.get(Calendar.MINUTE);
        broadcastDate = rightNow.get(Calendar.DATE);
        broadcastMonth = rightNow.get(Calendar.MONTH);
        broadcastYear = rightNow.get(Calendar.YEAR);


        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.dialog_broadcastschedule, null);
        timeLength = (TextView) view.findViewById(R.id.time);
        final TextView timeError = (TextView) view.findViewById(R.id.timeError);
        dropdown = (Spinner) view.findViewById(R.id.spinner);
        seekTime  = (SeekBar) view.findViewById(R.id.seekTime);
        timePicker = (TimePicker) view.findViewById(R.id.timePicker);
        timePicker.setEnabled(false);




        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.broadcast_schedule, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        dropdown.setAdapter(adapter);



        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                if (position == 0) {
                    timePicker.setEnabled(false);
                } else {
                    timePicker.setEnabled(true);
                }

                ;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Log.v("Time", timePicker.getCurrentHour() + ":" + timePicker.getCurrentMinute());
            }

        });


        seekTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeLength.setText(getLengthAvailable(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        TextView title = new TextView(getActivity());

        // title info
        title.setText("Set Your Broadcast Time");
        title.setGravity(Gravity.CENTER);
        title.setTextSize(20);

        dialog.setCustomTitle(title);


        dialog.setView(view);
        final AlertDialog alert = dialog.show();


        Button setBtn = (Button) view.findViewById(R.id.setBtn);

        // on suggest button click
        setBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // close dialog, show when available
                whenAvailable = dropdown.getSelectedItem().toString();
                if (!whenAvailable.equals("Now")) {

                    if (whenAvailable.equals("Today")) {
                        // throw error if user picks a date in the past
                        if (timePicker.getCurrentHour() < currHour || (currHour == timePicker.getCurrentHour() && timePicker.getCurrentMinute() < currMin)) {
                            Log.v("Invalid Time", "Past time selected");
                            timeError.setVisibility(View.VISIBLE);
                            return;
                        }
                    } else {
                        // must be tomorrow so increment broadcast date
                        broadcastDate += 1;
                        // TODO: adjust broadcast date for end of the month
                    }


                    // get start time in ms
                    Calendar calendar = Calendar.getInstance();
                    Log.v("preset calendar", calendar.toString());
                    Log.v("Calendar Year", broadcastYear + "");
                    Log.v("Calendar Month", broadcastMonth + "");
                    Log.v("Calendar Date", broadcastDate + "");
                    Log.v("Current Hour", timePicker.getCurrentHour() + "");
                    Log.v("Current Minute", timePicker.getCurrentMinute() + "");

                    // create a calendar instance with the set timepicker time
                    calendar.set(broadcastYear, broadcastMonth, broadcastDate, timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                    broadcastMS = calendar.getTimeInMillis();
                    Log.v("MS Start Time:", broadcastMS + "");
                    Log.v("Calendar time", "" + calendar.getTime());
                    Log.v("Calendar", calendar.toString());


                    int endHr = timePicker.getCurrentHour() + lengthHr;
                    int endMin = timePicker.getCurrentMinute() + lengthMin;
                    int startMin = timePicker.getCurrentMinute();
                    String startMinStr = Integer.toString(startMin);
                    String endMinStr = Integer.toString(endMin);

                    //handle minute overflow
                    if (endMin > 60) {
                        endHr += endMin / 60;
                        endMin = endMin % 60;
                        endMinStr = Integer.toString(endMin);
                    }

                    // append 0 in front of min if under 10 for time format
                    if (startMin < 10) {
                        startMinStr = "0" + startMinStr;
                    }

                    if (endMin < 10) {
                        endMinStr = "0" + endMinStr;
                        Log.v("endMinStr", endMinStr);
                    }


                    // PM time
                    if (timePicker.getCurrentHour() > 12) {
                        startTime = timePicker.getCurrentHour() - 12 + ":" + startMinStr + "PM";
                        // if added time crosses 24 hour mark
                        if (endHr > 24) {
                            endTime = endHr - 24 + ":" + endMinStr + "AM";
                        } else {
                            endTime = (endHr - 12 == 12) ? endHr - 12 + ":" + endMinStr + "AM" : endHr - 12 + ":" + endMinStr + "PM";
                        }
                        //AM time
                    } else {
                        startTime = timePicker.getCurrentHour() + ":" + startMinStr + "AM";
                        // if time crosses 12 hour mark
                        if (endHr > 12) {
                            endTime = endHr - 12 + ":" + endMinStr + "PM";
                        } else {
                            endTime = (endHr == 12) ? endHr + ":" + endMinStr + "PM" : endHr + ":" + endMinStr + "AM";
                        }
                    }

                    lengthAvailable = startTime + "-" + endTime;

                } else {
                    broadcastMS = 0;
                    lengthAvailable = getLengthAvailable(seekTime.getProgress());
                }

                availableString = whenAvailable + ", " + lengthAvailable;
                insertAvailText();
                alert.cancel();
            }
        });


    }

    public void showLocationList(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        Log.v("Location List Size", locationList.size() + "");

        dialog.setCancelable(true);

        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.dialog_addresslist, null);
        final ListView list = (ListView) view.findViewById(R.id.addressList);
        final ImageView mapIcon = (ImageView) view.findViewById(R.id.mapIcon);
        final ImageView locationPin = (ImageView) view.findViewById(R.id.locationPin);
        Button suggestBtn = (Button) view.findViewById(R.id.suggestBtn);
        final Button showMap = (Button) view.findViewById(R.id.showMap);
        final Button showList = (Button) view.findViewById(R.id.showList);
        locationsAdapter = new LocationsAdapter(getActivity(), locationList, selectedLocations);
        TextView title = new TextView(getActivity());
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(null);


        // title info
        title.setText("Where can you meet?");
        title.setGravity(Gravity.CENTER);
        title.setTextSize(20);

        dialog.setCustomTitle(title);

        list.setAdapter(locationsAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // get the selected location
                selectedLocation = locationList.get(position);

                // If a previous item was selected unhighlight it and remove it from the list
                if (selectedLocations.contains(selectedLocation)) {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    selectedLocations.remove(selectedLocation);
//                    parent.getChildAt(0).setBackgroundColor(Color.TRANSPARENT);
                } else {
                    // highlight the selected item
                    view.setBackgroundColor(Color.rgb(62, 175, 212));
                    selectedLocations.add(selectedLocation);
                }


            }
        });

        dialog.setView(view);
        final AlertDialog alert = dialog.show();


        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                spinner.setVisibility(View.GONE);
                locationButton.setEnabled(true);
            }
        });

        new Thread(new Runnable() {
            public void run() {
                mapView.onResume();

//                // After sleep finished blocking, create a Runnable to run on the UI Thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap = mapView.getMap();
                        gMap.setMyLocationEnabled(true);

                        for (int i = 0; i < locationList.size(); i++) {
                            LatLng loc = new LatLng(locationList.get(i).lat, locationList.get(i).lon);
                            gMap.addMarker(new MarkerOptions()
                                    .position(loc)
                                    .title(locationList.get(i).getName())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.markersmall)));

                        }

                        // selects the meeting point and stores it for reference
                        gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                Log.v("Meeting map", "Marker Selected!");
                                String selectedName = marker.getTitle();
                                LocationObject selected = findMeetingSpot(selectedName);
                                if (!selectedLocations.contains(selected)) {
                                    // show as selected and to list
                                    marker.setIcon((BitmapDescriptorFactory.fromResource(R.drawable.markerselected)));
                                    selectedLocations.add(selected);
                                } else {
                                    // deselect and remove from list
                                    marker.setIcon((BitmapDescriptorFactory.fromResource(R.drawable.markersmall)));
                                    selectedLocations.remove(selected);
                                }

                                return false;
                            }
                        });


                        // chosen center coordinates
                        LatLng center = new LatLng(43.075618, -89.405192);

                        // show on map
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13.9f));




                    }
                });

            }

        }).start();


        // renders map location selection
        showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.setVisibility(View.GONE);
                mapView.setVisibility(View.VISIBLE);
                mapIcon.setVisibility(View.GONE);
                locationPin.setVisibility(View.VISIBLE);
                showMap.setVisibility(View.GONE);
                showList.setVisibility(View.VISIBLE);
                addSelectedMarkers();
            }
        });

        // renders list location selection
        showList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.setVisibility(View.VISIBLE);
                mapView.setVisibility(View.GONE);
                mapIcon.setVisibility(View.VISIBLE);
                locationPin.setVisibility(View.GONE);
                showMap.setVisibility(View.VISIBLE);
                showList.setVisibility(View.GONE);
                locationsAdapter.notifyDataSetChanged();
            }
        });



        // on suggest button click
        suggestBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // close dialog, take chosen locations and display them
                alert.cancel();
                insertLocText();
            }
        });




    }

    /********************************************************* Helpers **************************************************************/

    private LocationObject findMeetingSpot(String place){
        for(int i = 0; i < locationList.size(); i++){
            if(locationList.get(i).getName().equals(place)){
                return locationList.get(i);
            }
        }

        return null;
    }

    private void addSelectedMarkers(){
        //add all location markers
        for(int i =0; i < selectedLocations.size(); i++){
            LatLng loc = new LatLng(selectedLocations.get(i).lat, selectedLocations.get(i).lon);
            gMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(locationList.get(i).getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.markerselected)));

        }
    }


    public String getLengthAvailable(int progress){
        availableTime = 30 + 30 * progress;
        lengthMin = availableTime;
        lengthHr = lengthMin / 60;
        if (lengthHr > 0) {
            lengthMin = lengthMin % 60;
            lengthAvailable = (lengthMin > 0) ? lengthHr + " hours " + lengthMin + " min" : lengthHr + " hours ";
        } else {
            lengthAvailable = lengthMin + " min";
        }


        return lengthAvailable;
    }

    /******************************************************** Selection Insertion *******************************************************/
    public void insertLocText(){
        String locationString = "";
        int i = 1;
        for(LocationObject selectedLoc: selectedLocations){
            if(i < selectedLocations.size()){
                locationString += selectedLoc.getName() + ", ";
            }else{
                locationString += selectedLoc.getName();
            }

            i++;
        }

        locText.setText(locationString);
    }


    public void insertCourseText(){
        String coursesString = "";
        int i = 1;
        for(String selectedCourse: selectedCourses){
            if(i < selectedCourses.size()){
                coursesString += selectedCourse + ", ";
            }else{
                coursesString += selectedCourse;
            }

            i++;
        }

        courseText.setText(coursesString);
    }

    public void insertAvailText(){
        availText.setText(availableString);
    }

    public void insertPriceText(){
        priceText.setText("$" + priceString + "  an hour");
        hrRate = Double.parseDouble(priceString);
        Log.v("Hour Rate", hrRate + "");
    }





    public void setAllTextViews(){
        insertLocText();
        insertCourseText();
        insertAvailText();
        insertPriceText();
    }

 /*********************************************** Switch between views  ******************************************/
   // sets the view to countdown mode for a future broadcast
    public void countdownView(){
        hideTitles();
        showCountdown();
        initiateCountdown();
        showCancelButton();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) courseContainer.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.timeView);
    }


    public void hideTitles(){
        courseTitle.setVisibility(View.GONE);
        meetupTitle.setVisibility(View.GONE);
        whenTitle.setVisibility(View.GONE);
        priceTitle.setVisibility(View.GONE);
    }

    public void showCountdown(){
        countdownTitle.setVisibility(View.VISIBLE);
        timeView.setVisibility(View.VISIBLE);
    }

    public void showCancelButton(){
        broadcastButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.VISIBLE);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.cancel();
                // continue with ending broadcast
                ((Main) getActivity()).mService.endBroadcast();
                session.removeFutureBroadcast();
                Log.v("Ending Broadcast", "Tutoring Off");
                currentUser.tutorOff();
                session.saveUser(currentUser);
                broadcastView();
            }
        });
    }

    //sets the view to settings for a new broadcast
    public void broadcastView(){
        hideCountdown();
        showTitles();
        showBroadcastButton();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) courseContainer.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.courseTitle);
    }


    public void showTitles(){
        courseTitle.setVisibility(View.VISIBLE);
        meetupTitle.setVisibility(View.VISIBLE);
        whenTitle.setVisibility(View.VISIBLE);
        priceTitle.setVisibility(View.VISIBLE);
    }

    public void hideCountdown(){
        countdownTitle.setVisibility(View.GONE);
        timeView.setVisibility(View.GONE);
    }

    public void showBroadcastButton(){
        broadcastButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.GONE);

    }


    /******************************************* Countdown Management *************************************************************/

    public void initiateCountdown(){
        rightNow = Calendar.getInstance();
        long millis = broadcastMS - rightNow.getTimeInMillis();
        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

        timeView.setText(hms);
        timer = new CounterClass(millis,1000);
        timer.start();
    }


    public class CounterClass extends CountDownTimer{
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onFinish() {
            timeView.setText("Completed.");
            session.removeFutureBroadcast();
            Intent intent = new Intent(getActivity(), Waiting.class);
            intent.putParcelableArrayListExtra("meetingSpots", selectedLocations);
            intent.putExtra("hrRate", hrRate);
            intent.putStringArrayListExtra("selectedCourses", selectedCourses);
            intent.putExtra("available", availableTime);
            if(inView){
                getActivity().startActivity(intent);
                broadcastView();
            }else{
                countdownIntent = intent;
            }

        }

        @Override
        public void onTick(long millisUntilFinished) {
            long millis = millisUntilFinished;
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            System.out.println(hms);
            timeView.setText(hms);
        }
    }






    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener{
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    public void locPopulate(){

        locationList =  new ArrayList<LocationObject>();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){

                SharedPreferences pref = getActivity().getSharedPreferences("locations", 0);

                String locStr = pref.getString("locations", null);
                Log.v("Locations", locStr);

                Gson gson = new Gson();
                Type resultType = new TypeToken<ArrayList<LocationObject>>(){}.getType();
                locationList = gson.fromJson(locStr, resultType);
                Log.v("locations list", locationList.size()+"");

            }
        });


        thread.start();

    }




}
