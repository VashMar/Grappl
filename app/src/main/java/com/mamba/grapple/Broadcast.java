package com.mamba.grapple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import org.w3c.dom.Text;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


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
    ImageButton locationButton;
    ImageButton timeButton;
    ImageButton priceButton;
    ImageButton courseButton;
    Button broadcastButton;



    // broadcast variables
    private int availableTime;
    private double hrRate;
    private long broadcastMS;
    private ArrayList<String> selectedCourses;
    private String selectedCourse;


    //dialog variables (set to defaults)
    private String whenAvailable;
    private String lengthAvailable;
    private int lengthHr;
    private int lengthMin;
    private String priceString;
    private int broadcastDate;
    private int broadcastMonth;
    private int broadcastYear;



    // Date/Time related
    TimePicker timePicker;
    Calendar rightNow;


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
        availableTime = 120;
        hrRate = 10.00;
        lengthAvailable = "2 hours";
        lengthHr = 0;
        lengthMin = availableTime;

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_broadcast, container, false);
    }

    @Override
    public void onStart(){
        Log.v("Broadcast Settings", "Started");
        super.onStart();

        broadcastButton = (Button) getView().findViewById(R.id.broadcastButton);
        courseButton  = (ImageButton) getView().findViewById(R.id.courseButton);
        locationButton = (ImageButton) getView().findViewById(R.id.locationButton);
        timeButton = (ImageButton) getView().findViewById(R.id.timeButton);
        priceButton = (ImageButton) getView().findViewById(R.id.priceButton);
        locText = (TextView) getView().findViewById(R.id.locationsText);
        courseText = (TextView) getView().findViewById(R.id.courseText);
        availText = (TextView) getView().findViewById(R.id.availText);
        priceText = (TextView) getView().findViewById(R.id.priceText);
        dummyPopulate();

        // handles broadcast button click
        broadcastButton.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v){
               // if they hit the broadcast button and aren't logged in, redirect them to the login
               if(!((Main) getActivity()).session.isLoggedIn()){
                   // transfer the user to the register page
                   Intent intent = new Intent(getActivity(), SignIn.class);

                   // we expect the auth response
                   startActivityForResult(intent, 1);
               }else{
                   startBroadcast();
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



    }

    @Override
    public void onResume(){
        Log.v("Broadcast Settings", "Resumed");
        super.onResume();
        // if the selected locations list isn't empty, show them
        if(selectedLocations.size() > 0){
            insertLocText();
        }
    }





    public void startBroadcast() {
        if (selectedLocations.size() > 0 && selectedCourses.size() > 0) {
            Log.v("Starting Broadcast..", "Broadcast initiated");
            Log.v("Broadcast MS", broadcastMS+"");
            Log.v("Available Time", availableTime+"");
            Log.v("Hourly Rate", hrRate+"");
            Log.v("Selected Course", selectedCourses+"");
            Log.v("Selected Locations", selectedLocations+"");


            if(whenAvailable.equals("Now")){
                rightNow = Calendar.getInstance();
                broadcastMS = rightNow.getTimeInMillis(); // get the latest time before broadcasting
            }

            ((Main) getActivity()).mService.startBroadcast(broadcastMS, availableTime, hrRate, selectedCourses, selectedLocations);
//            ((Main) getActivity()).session.updateCurrentUserDistance(distance);
            Intent intent = new Intent(getActivity(), Waiting.class);
            intent.putParcelableArrayListExtra("meetingSpots", selectedLocations);
            startActivity(intent);
        }else {
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
                R.layout.row, ((Main) getActivity()).COURSES) { // TODO swap COURSES with courseList

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final View renderer = super.getView(position, convertView, parent);
                if (selectedCourses.contains(((Main) getActivity()).COURSES[position])) {
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
                selectedCourse = ((Main) getActivity()).COURSES[position];

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
        Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
        SeekBar seekTime = (SeekBar) view.findViewById(R.id.seekTime);
        timePicker = (TimePicker) view.findViewById(R.id.timePicker);
        timePicker.setEnabled(false);




        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.broadcast_schedule, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);



        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                if (position == 0) {
                    timePicker.setEnabled(false);
                } else {
                    timePicker.setEnabled(true);
                }

                whenAvailable = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
                whenAvailable = "Now";
            }

        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Log.v("Time", timePicker.getCurrentHour() + ":" + timePicker.getCurrentMinute());
            }

        });


        seekTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                availableTime = 30 + 30 * progress;
                lengthMin = availableTime;
                lengthHr = lengthMin / 60;
                if (lengthHr > 0) {
                    lengthMin = lengthMin % 60;
                    lengthAvailable = (lengthMin > 0) ? lengthHr + " hours " + lengthMin + " min" : lengthHr + " hours ";
                } else {
                    lengthAvailable = lengthMin + " min";
                }


                timeLength.setText(lengthAvailable);

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
        setBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                // close dialog, show when available

                if(!whenAvailable.equals("Now")){

                    if(whenAvailable.equals("Today")){
                        // throw error if user picks a date in the past
                        if(timePicker.getCurrentHour() < currHour || (currHour == timePicker.getCurrentHour() &&  timePicker.getCurrentMinute() < currMin)){
                            Log.v("Invalid Tie", "Past time selected");
                            timeError.setVisibility(View.VISIBLE);
                            return;
                        }
                    }else{
                        // must be tomorrow so increment broadcast date
                        broadcastDate += 1;

                    }


                    // get start time in ms
                    Calendar calendar = Calendar.getInstance();
                    Log.v("preset calendar", calendar.toString());
                    Log.v("Calendar Year", broadcastYear+"" );
                    Log.v("Calendar Month", broadcastMonth+"");
                    Log.v("Calendar Date", broadcastDate+"");
                    Log.v("Current Hour",  timePicker.getCurrentHour()+"");
                    Log.v("Current Minute", timePicker.getCurrentMinute()+"");

                    calendar.set(broadcastYear, broadcastMonth, broadcastDate, timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                    broadcastMS = calendar.getTimeInMillis();
                    Log.v("MS Start Time:", broadcastMS+"");
                    Log.v("Calendar time", ""+calendar.getTime());
                    Log.v("Calendar", calendar.toString());


                    String startTime;
                    String endTime;
                    int endHr = timePicker.getCurrentHour() + lengthHr;
                    int endMin = timePicker.getCurrentMinute() + lengthMin;
                    int startMin = timePicker.getCurrentMinute();
                    String startMinStr = Integer.toString(startMin);
                    String endMinStr = Integer.toString(endMin);

                    //handle minute overflow
                    if(endMin > 60){
                        endHr += endMin/60;
                        endMin = endMin%60;
                        endMinStr = Integer.toString(endMin);
                    }

                    // append 0 in front of min if under 10 for time format
                    if(startMin < 10){
                        startMinStr = "0" + startMinStr;
                    }

                    if(endMin < 10){
                        endMinStr = "0" + endMinStr;
                        Log.v("endMinStr", endMinStr);
                    }


                    // PM time
                    if(timePicker.getCurrentHour() > 12){
                        startTime = timePicker.getCurrentHour() - 12 + ":" + startMinStr + "PM";
                        // if added time crosses 24 hour mark
                        if(endHr > 24){
                            endTime = endHr - 24 + ":" + endMinStr + "AM";
                        }else{
                            endTime =  (endHr - 12 == 12) ? endHr - 12 + ":" + endMinStr + "AM" : endHr - 12 + ":" + endMinStr + "PM";
                        }
                    //AM time
                    }else{
                        startTime = timePicker.getCurrentHour()  + ":" + startMinStr + "AM";
                        // if time crosses 12 hour mark
                        if(endHr > 12){
                            endTime = endHr - 12 + ":" + endMinStr +"PM";
                        }else{
                            endTime =  (endHr == 12) ? endHr + ":" + endMinStr + "PM" : endHr + ":" + endMinStr + "AM";
                        }
                    }

                    lengthAvailable = startTime + "-" + endTime;
                }

                insertAvailText();
                alert.cancel();
            }
        });


    }

    public void showLocationList(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);


        dialog.setCancelable(true);

        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.activity_addresslist, null);

        ListView list = (ListView) view.findViewById(R.id.addressList);
        Button suggestBtn = (Button) view.findViewById(R.id.suggestBtn);
        locationsAdapter = new LocationsAdapter(getActivity(), locationList, selectedLocations);
        TextView title = new TextView(getActivity());

        // title info
        title.setText("Input or Choose a Meeting Location");
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

        // on suggest button click
        suggestBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // close dialog, take chosen locations and display them
                alert.cancel();
                insertLocText();
                // get the coordinates of any of the locations we haven't gotten
                geoCodeSelected();
            }
        });



    }

    public void geoCodeSelected(){
        for(LocationObject selectedLoc: selectedLocations){
            if(selectedLoc.lat == 0.0 || selectedLoc.lon == 0.0){
                selectedLoc.geoCode(getActivity().getApplicationContext());
                Log.v("Selected Location", String.valueOf(selectedLocation.lat) + "," + String.valueOf(selectedLocation.lon));
            }

        }
    }

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
        availText.setText(whenAvailable+", "+lengthAvailable);
    }

    public void insertPriceText(){
        priceText.setText(priceString + "  an hour");
        hrRate = Double.parseDouble(priceString);
        Log.v("Hour Rate", hrRate+"");
    }


    public void dummyPopulate(){

        locationList =  new ArrayList<LocationObject>();
        final Context context = getActivity().getApplicationContext();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                // create dummy location objects for now
                final LocationObject loc1 = new LocationObject("College Library", "600 N Park St, Madison, WI", context);
                final LocationObject loc2 = new LocationObject("Union South", "1308 W Dayton St, Madison, WI", context);
                final LocationObject loc3 = new LocationObject("Chemistry Building", "1101 University Ave, Madison, WI", context);
                final LocationObject loc4 = new LocationObject("Grainger Hall", "975 University Ave, Madison, WI", context);

                locationList.add(loc1);
                locationList.add(loc2);
                locationList.add(loc3);
                locationList.add(loc4);


            }
        });


        thread.start();

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




}
