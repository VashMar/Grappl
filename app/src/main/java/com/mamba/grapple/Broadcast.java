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

import java.util.ArrayList;
import java.util.List;


public class Broadcast extends Fragment {


    private OnFragmentInteractionListener mListener;
    private int availableTime = 30;
    private double price = 10.00;
    private int distance = 1;

    private LocationsAdapter locationsAdapter;
    private List<LocationObject> locationList;
    private ArrayList<LocationObject> selectedLocations;
    private LocationObject selectedLocation;

    private ArrayList<String> selectedCourses;
    private String selectedCourse;

    private String whenAvailable;
    private String lengthAvailable;

    SeekBar seekprice;
    SeekBar seektime;
    SeekBar seekdist;
    TextView priceView;
    TextView availText;
    TextView distView;
    TextView locText;
    TextView courseText;
    ImageButton locationButton;
    ImageButton timeButton;
    ImageButton priceButton;
    ImageButton courseButton;
    Button broadcastButton;
    View selected;

    TimePicker timePicker;


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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_broadcast, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        selectedLocations = new ArrayList<LocationObject>();
        selectedCourses = new ArrayList<String>();
        broadcastButton = (Button) getView().findViewById(R.id.broadcastButton);
        courseButton  = (ImageButton) getView().findViewById(R.id.courseButton);
        locationButton = (ImageButton) getView().findViewById(R.id.locationButton);
        timeButton = (ImageButton) getView().findViewById(R.id.timeButton);
        priceButton = (ImageButton) getView().findViewById(R.id.priceButton);
        locText = (TextView) getView().findViewById(R.id.locationsText);
        courseText = (TextView) getView().findViewById(R.id.courseText);
        availText = (TextView) getView().findViewById(R.id.availText);
        dummyPopulate();

        // handles broadcast button click
        broadcastButton.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v) {
                startBroadcast();
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
        super.onResume();
        // if the selected locations list isn't empty, show them
        if(selectedLocations.size() > 0){
            insertLocText();
        }
    }

    public void startBroadcast() {
        if (selectedLocations.size() > 0) {
            Log.v("Starting Broadcast..", "Broadcast initiated");
            ((Main) getActivity()).mService.startBroadcast(availableTime, price, selectedCourses, selectedLocations);
            ((Main) getActivity()).session.updateCurrentUserDistance(distance);
            Intent intent = new Intent(getActivity(), Waiting.class);
            intent.putParcelableArrayListExtra("meetingSpots", selectedLocations);
            startActivity(intent);
        }else {
            // we can only broadcast if the tutor has a selected a meeting spot
            locText.setError("You must choose a preferred meeting spot!");
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
                R.layout.row, ((Main) getActivity()).COURSES){ // TODO swap COURSES with courseList

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                final View renderer = super.getView(position, convertView, parent);
                if(selectedCourses.contains(((Main)getActivity()).COURSES[position])){
                    Log.v("Position", "" + position);
                    renderer.setBackgroundColor(Color.rgb(62, 175, 212));
                }else{
                    renderer.setBackgroundColor(Color.TRANSPARENT);
                }


                return renderer;
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.v("Item Click Position", ""+position);
                // get the selected course
                selectedCourse = ((Main) getActivity()).COURSES[position];

                // If a previous item was selected unhighlight it and remove it from the list
                if (selectedCourses.contains(selectedCourse)){
                    view.setBackgroundColor(Color.TRANSPARENT);
                    selectedCourses.remove(selectedCourse);
                }else{
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
        NumberPicker numPicker1 = (NumberPicker) view.findViewById(R.id.numberPicker);
        NumberPicker numPicker2 = (NumberPicker) view.findViewById(R.id.numberPicker2);
        NumberPicker numPicker3 = (NumberPicker) view.findViewById(R.id.numberPicker3);

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
                // close dialog, take chosen locations and display them
                alert.cancel();
            }
        });

    }

    public void broadcastSchedule(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setCancelable(true);

        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.dialog_broadcastschedule, null);

        Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.broadcast_schedule, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        timePicker = (TimePicker) view.findViewById(R.id.timePicker);
        timePicker.setEnabled(false);

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
