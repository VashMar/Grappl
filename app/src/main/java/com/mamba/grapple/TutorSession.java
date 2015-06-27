package com.mamba.grapple;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by vash on 4/2/15.
 */

public class TutorSession implements Parcelable {
    public double price;
    public int period;
    public boolean available;
    public long startTime; // session start date and time in ms
    public ArrayList<LocationObject> meetingSpots;

    public TutorSession(int price, int period, boolean available, ArrayList<LocationObject> meetingSpots,long startTime){
        this.price = price;
        this.period = period;
        this.available = available;
        this.meetingSpots = meetingSpots;
        this.startTime = startTime;
    }


    protected TutorSession(Parcel in){
        price = in.readDouble();
        period = in.readInt();
        startTime = in.readLong();
        available = in.readByte() != 0x00;
        meetingSpots = in.readArrayList(LocationObject.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(price);
        dest.writeInt(period);
        dest.writeLong(startTime);
        dest.writeByte((byte) (available ? 0x01 : 0x00));
        dest.writeList(meetingSpots);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TutorSession> CREATOR = new Parcelable.Creator<TutorSession>() {
        @Override
        public TutorSession createFromParcel(Parcel in) {
            return new TutorSession(in);
        }

        @Override
        public TutorSession[] newArray(int size) {
            return new TutorSession[size];
        }
    };


}