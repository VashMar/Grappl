package com.mamba.grapple;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Rating;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by vash on 4/2/15.
 */
public class TutorsAdapter extends ArrayAdapter<UserObject> {

    private Location userLocation;



    public TutorsAdapter(Context context, ArrayList<UserObject> tutors) {
        super(context, 0, tutors);

    }

    public View getView(int position, View convertView, ViewGroup parent){

        // get the data item for this position
        UserObject tutor = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tutor_row, parent, false);
        }

        // Look up view for data population
        TextView tutorName = (TextView) convertView.findViewById(R.id.tutorName);
        TextView tutorAvailability = (TextView) convertView.findViewById(R.id.tutorAvailability);
        TextView tutorPrice = (TextView) convertView.findViewById(R.id.tutorPrice);
        TextView timeRange = (TextView) convertView.findViewById(R.id.timeRange);
        ImageView tutorPic = (ImageView) convertView.findViewById(R.id.profilePic);
        ImageView availableIcon = (ImageView) convertView.findViewById(R.id.availableIcon);
        ImageView unavailableIcon = (ImageView) convertView.findViewById(R.id.unavailableIcon);
        RatingBar tutorRating = (RatingBar) convertView.findViewById(R.id.ratingBar);

        // populate the data into the list item template
        tutorName.setText(tutor.getName());
        tutorPrice.setText("$" +  String.format("%.2f", tutor.getPrice()));
        tutorRating.setRating(tutor.getRating());


        // get the time data for the current time
        Log.v("Start Time", ""+ tutor.getSessionStart());
        Calendar cal = Calendar.getInstance();
        long startTime = tutor.getSessionStart();
        int currDate = cal.get(Calendar.DATE);


        // get the date data from the received start time
        cal.setTimeInMillis(startTime);
        int startHr = cal.get(Calendar.HOUR);
        int startMin = cal.get(Calendar.MINUTE);
        int endMin = startMin + tutor.sessionLength();
        int endHr = startHr;
        String startTOD = "AM";
        String startStr =  String.valueOf(startMin);;
        String endStr = endMin+"";
        String timeStr;


        // check if AM OR PM for start
        if(startHr >= 12 ){
            startTOD = "PM";
            if(startHr > 12){
                startHr -= 12;
            }
        }
        if(startMin < 10){
            startStr = "0" + startStr;
        }

        // build time string for start time
        startStr = startHr + ":" + startStr + startTOD;

        // deal with overflow when adding availability time
        if(endMin > 60){
            endHr += endMin/60;
            endMin = endMin%60;
            endStr = String.valueOf(endMin);
            if(endMin < 10){
                endStr = "0" +endStr;
            }
        }

        // handle AM or PM for availability end time and build the string
        if(endHr > 24){
            endStr = endHr - 24 + ":" + endMin + "AM";
        }else if(endHr >= 12){
            endStr =  (endHr - 12 == 12) ? endHr - 12 + ":" + endStr + "AM" : endHr - 12 + ":" + endStr + "PM";
        }else{
            endStr = endHr + ":" + endStr + "AM";
        }


        if(tutor.isAvailable()) {
            tutorAvailability.setText("Available");
            timeStr = "Now - " + endStr;
        }else{
             // show full range of begin/end
             timeStr = startStr + " - " + endStr;

            // change availability icon
            availableIcon.setVisibility(View.GONE);
            unavailableIcon.setVisibility(View.VISIBLE);

            // check if the session is tomorrow
            if(currDate < cal.get(Calendar.DATE) || cal.get(Calendar.DATE) == 1){
                tutorAvailability.setText("Tomorrow");
                tutorAvailability.setTextSize(13);
            } else {
                tutorAvailability.setText("Today," + timeRange);

            }
        }


        timeRange.setText(timeStr);
        timeRange.setTextSize(13);




        // TEMP DUMMY TUTORS
        switch (tutor.firstName()){
            case "Jess": tutorPic.setImageResource(R.drawable.jess);
                break;
            case "Eric": tutorPic.setImageResource(R.drawable.eric);
                break;
            case "Robert": tutorPic.setImageResource(R.drawable.robert);
                break;
            case "Nadia": tutorPic.setImageResource(R.drawable.nadia);
                break;
        }

        // return the completed view to render
        return convertView;
    }


    // use to add in profile picture
    public static Drawable LoadImageFromWebOperations(String url){
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "");
            return d;
        } catch (Exception e) {
            return null;
        }
    }


    public void setUserLocation(Location userLocation){
        if(userLocation != null)
            this.userLocation = userLocation;
    }

}


