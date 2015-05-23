package com.mamba.grapple;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


public class Broadcast extends Fragment {


    private OnFragmentInteractionListener mListener;
    private int availableTime = 30;
    private double price = 10.00;
    private int distance = 1;
    String[] courses =  {"Comp Sci 302", "Physics 202"};

    SeekBar seekprice;
    SeekBar seektime;
    SeekBar seekdist;
    TextView priceView;
    TextView timeView;
    TextView distView;
    Button broadcastButton;



    public Broadcast() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_broadcast, container, false);
    }

    @Override
    public void onStart(){
       super.onStart();
       broadcastButton = (Button) getView().findViewById(R.id.broadcastButton);

       broadcastButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
                startBroadcast();
           }
       });

        // update user slides
        //Change price slider
        priceView = (TextView) getView().findViewById(R.id.priceView);
        seekprice = (SeekBar) getView().findViewById(R.id.seekPrice);

        seekprice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                price = 10 +  progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                priceView.setText("Price: $" + price);
            }
        });

        //change
        timeView = (TextView) getView().findViewById(R.id.timeAvailable);
        seektime = (SeekBar) getView().findViewById(R.id.seekTime);

        seektime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                availableTime =  30 + progress * 10;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                timeView.setText("Availability: " + availableTime + " min");
            }
        });

//        //change travel distance
//        distView = (TextView) getView().findViewById(R.id.travelDistance);
//        seekdist = (SeekBar) getView().findViewById(R.id.seekDistance);
//
//        seekdist.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                distance = progress;
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                distView.setText("Travel Distance: " + distance + " mi");
//            }
//        });
    }

    public void startBroadcast(){
        Log.v("Starting Broadcast..", "Broadcast initiated");
        ((Main)getActivity()).mService.startBroadcast(availableTime, distance, price, courses);
        ((Main) getActivity()).session.updateCurrentUserDistance(distance);
        //((Main)getActivity()).session.getCurrentUser().
        Intent intent = new Intent(getActivity(), Waiting.class);
        intent.putExtra("location",  ((Main)getActivity()).mService.getLocation());
        startActivity(intent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }




}
