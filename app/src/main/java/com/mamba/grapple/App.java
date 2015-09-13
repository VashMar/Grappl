package com.mamba.grapple;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.*;

import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by vash on 4/17/15.
 */
public class App extends MultiDexApplication {

    private static MobileAnalyticsManager analytics;

    public void onCreate(){
        super.onCreate();
        Fabric.with(this, new Crashlytics());


        try {
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    this.getApplicationContext(),
                    "38b7a43f4d6c46c09badab0dd4f48219", //Amazon Mobile Analytics App ID
                    "us-east-1:6220c0ee-cc17-4a18-a095-30433b5f0ca4" //Amazon Cognito Identity Pool ID
            );
        } catch(InitializationException ex) {
            Log.e(this.getClass().getName(), "Failed to initialize Amazon Mobile Analytics", ex);
        }


        // adds roboto font
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        // register to be informed of activities starting up
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity,
                                          Bundle savedInstanceState) {

                // forces activity orientation to portrait
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.v("App", "Activity Resumed " + activity.getLocalClassName());
                if(analytics != null) {
                    analytics.getSessionClient().resumeSession();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.v("App", "Activity Paused " + activity.getLocalClassName());
                if(analytics != null) {
                    analytics.getSessionClient().pauseSession();
                    analytics.getEventClient().submitEvents();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }


        });


    }




}
