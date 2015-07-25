package com.mamba.grapple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferProgress;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.regions.Regions;
import com.google.android.gms.maps.MapFragment;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.lang.reflect.AccessibleObject;


public class Account extends Activity {

    // Amazon s3 related
    CognitoCachingCredentialsProvider credentialsProvider;
    TransferManager transferManager;


    LoginManager session;
    UserObject currentUser;
    PicManager picManager;

    private Location mLastLocation;

    // service related variables
    private boolean mBound = false;
    DBService mService;

    Button selectPhoto;
    ImageView profilePic;
    private Uri picUri;
    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            Log.v("Picasso", "Loaded Bitmap");
            picManager.storeImage(bitmap, currentUser.getPicKey());

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.v("Picasso", "Bitmap Failed");

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.v("Picasso", "Bitmap Loading..");
        }
    };


    // receiver intended for multicasts
    private BroadcastReceiver multicastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            // intent can contain any data
            Bundle extras = intent.getExtras();

            if(extras != null){
                String responseType = extras.getString("responseType");
                Log.v("responseType", responseType);
                Log.v("Main Activity", "received multicast: " + responseType);

                if (responseType.equals("updatedPic")){
                    currentUser.setProfilePic(extras.getString("profilePic"));
                    updateUserSession();
                    Log.v("Profile Pic Updated", currentUser.getProfilePic());
                    Picasso.with(getApplicationContext()).load(currentUser.getProfilePic()).into(mTarget);
                }
            }

        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        session = new LoginManager(getApplicationContext());
        currentUser = session.getCurrentUser();
        picManager = new PicManager((getApplicationContext()));

        selectPhoto = (Button) findViewById(R.id.btnSelectPhoto);
        profilePic = (ImageView) findViewById(R.id.profilePic);

        if(currentUser.hasProfilePic()){
            profilePic.setImageBitmap(picManager.getImage(currentUser.getPicKey()));
        }


        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // Context
                "us-east-1:6220c0ee-cc17-4a18-a095-30433b5f0ca4", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

       transferManager = new TransferManager(credentialsProvider);




    }

    public void onStart() {
        super.onStart();
        if (session.isLoggedIn()){
            createService();
        }

        // register global broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(multicastReceiver,
                new IntentFilter("multicastReceiver"));

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(multicastReceiver);
    }


    private void selectImage() {

        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(Account.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, 1);
                } else if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    /**
     * this function does the crop operation.
     */
    private void performCrop() {
        // take care of exceptions
        try {
            // call the standard crop action intent (the user device may not
            // support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            // set crop properties
            cropIntent.putExtra("crop", "true");
            // indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            // indicate output X and Y
            cropIntent.putExtra("outputX", 300);
            cropIntent.putExtra("outputY", 300);
            // retrieve data on return
            cropIntent.putExtra("return-data", true);

            File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp_CROP.jpg");
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, 3);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            Toast toast = Toast
                    .makeText(this, "This device doesn't support the crop action!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // handle taken picture
            if (requestCode == 1) {
                // get the file
                File file = new File(Environment.getExternalStorageDirectory()+File.separator + "temp.jpg");
                // get the Uri for the captured image
                picUri = Uri.fromFile(file);
                performCrop();

                // handle photo from gallery
            } else if (requestCode == 2) {
                  picUri = data.getData();
                  performCrop();
            } else if (requestCode == 3){
                final File cropFile = new File(Environment.getExternalStorageDirectory()+File.separator + "temp_CROP.jpg");

                //Upload to s3
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        String ref = "profilePic-" + currentUser.getId();
                        Upload upload = transferManager.upload("grappl-pics", ref, cropFile);
                        while(!upload.isDone()){
                            TransferProgress transferred = upload.getProgress();
                            Log.v("Percent Uploaded", transferred.getPercentTransferred()+"");
                        }

                        mService.updateProfilePic(ref);
                    }
                });
                thread.start();



                // get the returned data
                Bundle extras = data.getExtras();
                // get the cropped bitmap
                Bitmap thePic = extras.getParcelable("data");
                profilePic.setImageBitmap(thePic);


            }

        }

    }




    public void createService() {
        Log.v("Waiting Page", "Creating Service..");
        startService(new Intent(this, DBService.class));
        bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.v("Service Bound", "Results bound to new service");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_signin, menu);

        //return super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                //TODO
            case R.id.action_signout:

                Intent myIntent = new Intent(Account.this, SignIn.class);
                myIntent.putExtra("destroy_token","true");
                startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            mBound = true;
            mLastLocation = mService.getLocation();
        }
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void updateUserSession(){
        session.saveUser(currentUser);
        mService.setSession(session);
    }
}
