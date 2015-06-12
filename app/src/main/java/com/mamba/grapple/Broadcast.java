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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class Broadcast extends Fragment {


    private OnFragmentInteractionListener mListener;
    private int availableTime = 30;
    private double price = 10.00;
    private int distance = 1;
    String[] courses =  {"Comp Sci 302", "Physics 202"};

    private LocationsAdapter locationsAdapter;
    private List<LocationObject> locationList;
    private ArrayList<LocationObject> selectedLocations;
    private LocationObject selectedLocation;

    SeekBar seekprice;
    SeekBar seektime;
    SeekBar seekdist;
    TextView priceView;
    TextView timeView;
    TextView distView;
    TextView locText;
    ImageButton locationButton;
    Button broadcastButton;
    View selected;


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

        broadcastButton = (Button) getView().findViewById(R.id.broadcastButton);
        locationButton = (ImageButton) getView().findViewById(R.id.locationButton);
        locText = (TextView) getView().findViewById(R.id.locationsText);
        dummyPopulate();


        broadcastButton.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v) {
                startBroadcast();
           }
        });


        locationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                showLocationList(v.getContext());
            }
        });

        // update user slides
        //Change price slider
        priceView = (TextView) getView().findViewById(R.id.priceView);
        seekprice = (SeekBar) getView().findViewById(R.id.seekPrice);

        seekprice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                price = 10 +  progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                priceView.setText("Price: $" + price);
            }
        });

        //change
        timeView = (TextView) getView().findViewById(R.id.timeAvailable);
        seektime = (SeekBar) getView().findViewById(R.id.seekTime);

        seektime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                availableTime = 30 + progress * 10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                timeView.setText("Availability: " + availableTime + " min");
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
            ((Main) getActivity()).mService.startBroadcast(availableTime, price, courses, selectedLocations);
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


    public void showLocationList(Context context){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);


        dialog.setCancelable(true);

        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.activity_addresslist, null);

        ListView list = (ListView) view.findViewById(R.id.addressList);
        Button suggestBtn = (Button) view.findViewById(R.id.suggestBtn);
        locationsAdapter = new LocationsAdapter(getActivity(), locationList);
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
                if (selectedLocations.contains(selectedLocation)){
                    view.setBackgroundColor(Color.TRANSPARENT);
                    selectedLocations.remove(selectedLocation);
//                    parent.getChildAt(0).setBackgroundColor(Color.TRANSPARENT);
                }else{
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
