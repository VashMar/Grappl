package com.mamba.grapple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by vash on 7/12/15.
 */
public class PicManager {

    // Context
    Context _context;

    // Constructor
    public PicManager(Context context){
        this._context = context;
    }


    public void storeImage(Bitmap image, String source){
            Log.v("PicManager", "Storing image..");
        try {
            // Use the compress method on the Bitmap object to write image to
            // the OutputStream
            FileOutputStream fos = _context.openFileOutput(source, Context.MODE_PRIVATE);

            // Writing the bitmap to the output stream
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.v("PicManager", "Image Stored");
        } catch (Exception e) {
            Log.e("saveToInternalStorage()", e.getMessage());
        }

    }

    // retrieves a stored image
    public Bitmap getImage(String source){
        Bitmap thumbnail = null;
        try {
            File filePath = _context.getFileStreamPath(source);
            FileInputStream fi = new FileInputStream(filePath);
            thumbnail = BitmapFactory.decodeStream(fi);
        } catch (Exception e) {
            Log.e("getImage", e.getMessage());
        }

        return thumbnail;
    }

    public void deleteImage(String source){
        _context.deleteFile(source);
        Log.v("Deleted Image", source);
    }






}
