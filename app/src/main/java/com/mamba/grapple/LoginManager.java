package com.mamba.grapple;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by vash on 5/8/15.
 */
public class LoginManager {
    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    SharedPreferences.Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

   // Sharedpref file name
    private static final String PREF_NAME = "SessionData";
    private static final String CURRENT_USER = "CurrentUser";
    private static final String FUTURE_BROADCAST = "FutureBroadcast";
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String AUTH_TOKEN = "Token";

    // Current User
    UserObject currentUser = null;


    Gson gson = new Gson();

    // Constructor
    public LoginManager(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    // Create a login session by storing auth token
    public void login(String token, String user){
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(AUTH_TOKEN, token);
        editor.putString(CURRENT_USER, user);
        editor.commit();
    }


    // clear the token and current user data
    public void logout(){
        Log.v("Logout", "Removing Session Token..");
        editor.clear();
        editor.commit();
    }

    // check if current user is logged in
    public boolean isLoggedIn(){
        Log.v("Checking Logged in", "" + pref.getBoolean(IS_LOGIN, false));
        return pref.getBoolean(IS_LOGIN, false);
    }

    // return auth token
    public String getToken(){
        return pref.getString(AUTH_TOKEN, null);
    }

    // returns the logged in user
    public UserObject getCurrentUser(){
        String user = pref.getString(CURRENT_USER, null);

        if(user != null){
            currentUser = gson.fromJson(user, UserObject.class);
        }

        return currentUser;
    }

    // saves changed current user
    public void saveUser(UserObject updatedUser){
        currentUser = updatedUser;
        String user = gson.toJson(currentUser);
        editor.putString(CURRENT_USER, user);
        editor.commit();
    }

    // keeps track if there is a broadcast for the future
    public void setFutureBroadcast(){
        editor.putBoolean(FUTURE_BROADCAST, true);
        editor.commit();
    }

    public void removeFutureBroadcast(){
        editor.putBoolean(FUTURE_BROADCAST, false);
        editor.commit();
    }

    public Boolean getFutureBroadcast(){
        return pref.getBoolean(FUTURE_BROADCAST, false);
    }

    public void storeSelectedCourses(ArrayList<String> selectedCourses){
        String selected = gson.toJson(selectedCourses);
        editor.putString("selectedCourses", selected);
        editor.commit();
    }

    public ArrayList<String> getSelectedCourses(){
        String json = pref.getString("selectedCourses", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> selectedCourses = gson.fromJson(json, type);
        return selectedCourses;
    }

    public void storeAvailString(String availString){
        editor.putString("availString", availString);
        editor.commit();
    }


    public String getAvailString(){
        return pref.getString("availString", null);
    }

    public void updateCurrentUserDistance(int distance) {
        currentUser.setTravelDistance(distance);
        String user = gson.toJson(currentUser);
        editor.putString(CURRENT_USER, user);
        editor.commit();
    }
}
