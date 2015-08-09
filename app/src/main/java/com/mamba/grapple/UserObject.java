package com.mamba.grapple;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by vash on 5/8/15.
 */
public class UserObject implements Parcelable {

    // the attributes of each tutor
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private double tutorRating;
    private double studentRating;
    private int tutorSessionCount;
    private int studentSessionCount;
    private String profilePic = "";
    private float distance;
    private boolean tutoring = false;
    private LocationObject location;
    private TutorSession session;



    // rounds to two decimal places
    DecimalFormat twoDeci = new DecimalFormat("##.00");



    // constructor
    public UserObject(String firstName, String lastName, String id, String email, String profilePic,
                      LocationObject location, TutorSession session,
                      int tutorSessionCount, int studentSessionCount, double studentRating, double tutorRating){
        this.firstName = firstName;
        this.lastName = lastName;
        this.id = id;
        this.email = email;
        this.profilePic = profilePic;
        this.location = location;
        this.tutorSessionCount = tutorSessionCount;
        this.studentSessionCount = studentSessionCount;
        this.studentRating = studentRating;
        this.tutorRating = tutorRating;
        if(session != null){
            this.session = session;
            this.tutoring = true;
        }

        Log.v("Created User: ", firstName + " " + lastName);
    }



    /********************************************************************** Getters and Setters ****************************************************************************************/

    public String getDistance(Location userLocation){

        double lat1;
        double lon1;

        if(userLocation != null){
            lat1 = userLocation.getLatitude();
            lon1 = userLocation.getLongitude();
        }else{
            // create dummy lat/lon for emulator
            lat1 = 43.076592;
            lon1 = -89.412487;
        }


        double lat2 = this.location.lat;
        double lon2 = this.location.lon;

        Log.v("Calculating distance", "(" + lat1 + "," + lon1 + ") & " + "(" + lat2 + "," + lon2 + ")" );

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        return twoDeci.format(dist);
    }



    public String getDistance(LatLng location){

        double lat1 = location.latitude;
        double lon1 = location.longitude;
        double lat2 = this.location.lat;
        double lon2 = this.location.lon;

        Log.v("Calculating distance", "(" + lat1 + "," + lon1 + ") & " + "(" + lat2 + "," + lon2 + ")" );

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        return twoDeci.format(dist);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String ID){
        this.id = ID;
    }

    public void setTravelDistance(float distance){
        this.distance = distance;
    }

    public void setSession(TutorSession session){
        this.session = session;
        this.tutoring = true;
    }

    public void isStudent(){
        this.tutoring = false;
    }

    public float travelDistance(){
        return this.distance;
    }

    public String firstName(){ return this.firstName; }

    public String getName(){
        return this.firstName + " " + this.lastName;
    }

    public double getStudentRating(){
        return this.studentRating;
    }

    public double getTutorRating(){
        return this.tutorRating;
    }

    public double getPrice(){
        return this.session.price;
    }

    public int sessionLength(){
        return this.session.period;
    }

    public ArrayList<LocationObject> getMeetingSpots(){
        return this.session.meetingSpots;
    }



    public double getLatitude(){
        return this.location.lat;
    }

    public double getLongitude(){
        return this.location.lon;
    }

    public boolean isTutor(){ return this.tutoring; }

    public void setTutor(){
        this.tutoring = true;
    }

    public void tutorOff(){
        this.tutoring = false;
    }


    public boolean isAvailable(){return this.session.available;}

    public long getSessionStart(){ return this.session.startTime; }

    public void setStartTime(long startTime){
        this.session.startTime = startTime;
    }

    public boolean hasProfilePic(){
        if(this.profilePic != null && !this.profilePic.isEmpty()){
            return true;
        }
        return false;
    }

    public String getProfilePic(){
        return this.profilePic;
    }

    // returns the unique string for storing profile pic
    public String getPicKey(){
      return "profilePic-" + this.id;
    }

    public void setProfilePic(String profilePic){
        this.profilePic = profilePic;
    }

   /************************************  Helpers   ***************************************************************************/

    // conversions
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


//    public String toString(){
//        return "[id=" + id + " firstName=" + firstName + " lastName=" + lastName +
//                " rating=" + rating + " location=" + location.lat + "," + location.lon + "]" +
//                " session= {price: " + session.price + ", minLength: " + session.period + " , available: " + session.available + "}]";
//    }
//
//
//
//


    /********************************* Parcel Code  *************************************************************************/

    protected UserObject(Parcel in){
        id = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        email = in.readString();
        studentRating = in.readDouble();
        tutorRating = in.readDouble();
        tutorSessionCount = in.readInt();
        studentSessionCount = in.readInt();
        profilePic = in.readString();
        distance = in.readFloat();
        location = (LocationObject) in.readValue(LocationObject.class.getClassLoader());
        session = (TutorSession) in.readValue(TutorSession.class.getClassLoader());
        tutoring = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(email);
        dest.writeDouble(studentRating);
        dest.writeDouble(tutorRating);
        dest.writeInt(tutorSessionCount);
        dest.writeInt(studentSessionCount);
        dest.writeString(profilePic);
        dest.writeFloat(distance);
        dest.writeValue(location);
        dest.writeValue(session);
        dest.writeByte((byte) (tutoring ? 1 : 0));

    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<UserObject> CREATOR = new Parcelable.Creator<UserObject>() {
        @Override
        public UserObject createFromParcel(Parcel in) {
            return new UserObject(in);
        }

        @Override
        public UserObject[] newArray(int size) {
            return new UserObject[size];
        }
    };





}
