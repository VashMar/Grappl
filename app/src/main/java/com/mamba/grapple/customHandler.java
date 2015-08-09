package com.mamba.grapple;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.pushbots.push.PBNotificationIntent;
import com.pushbots.push.Pushbots;
import com.pushbots.push.utils.PBConstants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by vash on 7/25/15.
 */
public class customHandler extends BroadcastReceiver
{
    private static final String TAG = "customHandler";
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action.equals("com.pushbots.MSG_OPEN"))
        {
            Pushbots pushInstance = Pushbots.sharedInstance();
            if (!pushInstance.isInitialized().booleanValue())
            {
                Log.d(TAG, "Initializing Pushbots.");
                Pushbots.sharedInstance().init(context.getApplicationContext());
            }

            // only launch if notifications array is populated
            if (PBNotificationIntent.notificationsArray != null) {
                PBNotificationIntent.notificationsArray = null;

            HashMap<?, ?> PushdataOpen = (HashMap)intent.getExtras().get("com.pushbots.MSG_OPEN");
            if (Pushbots.sharedInstance().isAnalyticsEnabled()) {
                Pushbots.sharedInstance().reportPushOpened((String)PushdataOpen.get("PUSHANALYTICS"));
            }
            String packageName = context.getPackageName();
            Intent resultIntent = new Intent(context.getPackageManager().getLaunchIntentForPackage(packageName));
            resultIntent.setFlags(268468224);

            Bundle bundle = intent.getBundleExtra("pushData");
            if (bundle != null)
            {
                Set<String> keys = bundle.keySet();
                Iterator<String> it = keys.iterator();
                Log.d(TAG, "Dumping Intent start");
                while (it.hasNext())
                {
                    String key = (String)it.next();
                    Log.d(TAG, "[" + key + "=" + bundle.get(key) + "]");
                }
                Log.d(TAG, "Dumping Intent end");
            }
            if (null != bundle.getString("nextActivity")) {
                try
                {
                    Log.i(TAG, "Opening custom activity " + bundle.getString("nextActivity"));
                    resultIntent = new Intent(context, Class.forName(bundle.getString("nextActivity")));
                    resultIntent.setFlags(268468224);
                    Log.d(TAG, "we are now redirecting to intent: " + bundle.getString("nextActivity"));
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            if ((null != bundle.getString("openURL")) && ((bundle.getString("openURL").startsWith("http://")) || (bundle.getString("openURL").startsWith("https://"))))
            {
                resultIntent = new Intent("android.intent.action.VIEW", Uri.parse(bundle.getString("openURL")));
                resultIntent.setFlags(268468224);
                Log.d(TAG, "we are now opening url: " + bundle.getString("openURL"));
            }
            resultIntent.putExtras(intent.getBundleExtra("pushData"));

            Pushbots.sharedInstance().startActivity(resultIntent);

            } // end notifications array check
        }
        else if ("buttonOneClicked".equals(action))
        {
            Log.d(TAG, "buttonOneClicked");
        }
    }
}