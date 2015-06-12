package com.mamba.grapple;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by vash on 4/2/15.
 */

public class TutorSession implements Parcelable {
    public int price;
    public int maxLength;
    public boolean available;
    public ArrayList<LocationObject> meetingSpots;

    public TutorSession(int price, int maxLength, boolean available, ArrayList<LocationObject> meetingSpots){
        this.price = price;
        this.maxLength = maxLength;
        this.available = available;
        this.meetingSpots = meetingSpots;
    }


    protected TutorSession(Parcel in) {
        price = in.readInt();
        maxLength = in.readInt();
        available = in.readByte() != 0x00;
        meetingSpots = in.readArrayList(LocationObject.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(price);
        dest.writeInt(maxLength);
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