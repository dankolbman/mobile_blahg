package com.dankolbman.travelblahg;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/*
GPS tracking service
 */
public class GpsTracker extends Service implements LocationListener {

    private final Context mContext;
    protected LocationManager locmgr;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    Location location = null; // location
    double latitude; // latitude
    double longitude; // longitude

    private static final long UPDATE_DIST = 10; // meters
    private static final long UPDATE_FREQ = 1000*60; // milliseconds

    String path = "";
    File datafile = null;

    public GpsTracker(Context context, String filename) {
        this.mContext = context;
        this.path = context.getFilesDir().getAbsolutePath();
        this.path += "/" + filename;
        this.datafile = new File(this.path);
    }

    /*
    Start gps recording
     */
    public Location startRec() {
        try {
            locmgr = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // update status
            isGPSEnabled = locmgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locmgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                CharSequence msg = "Position not available!!";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(mContext, msg, duration);
                toast.show();
                this.canGetLocation = false;
            } else {
                this.canGetLocation = true;

                if (isGPSEnabled) {
                    if (location == null) {
                        locmgr.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                UPDATE_FREQ, UPDATE_DIST, this);
                        CharSequence msg = "Using GPS";
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(mContext, msg, duration);
                        toast.show();
                        if (locmgr != null) {
                            location = locmgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                            writeLocation(location);
                        }
                    }
                } else if (isNetworkEnabled) { // Prefer satellite to towers
                    locmgr.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            UPDATE_FREQ, UPDATE_DIST, this);
                    CharSequence msg = "Using Network";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(mContext, msg, duration);
                    toast.show();
                    if (locmgr != null) {
                        location = locmgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                        writeLocation(location);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    public void stopRec(){
        if(locmgr != null){
            locmgr.removeUpdates(GpsTracker.this);
            CharSequence msg = "Stopping GPS";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(mContext, msg, duration);
            toast.show();
            locmgr = null;
            location = null;
        }
    }

    /*
    Getters
     */

    public Location getLocation() {
        return this.location;
    }
    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude(){
       return this.longitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        CharSequence msg = "Location changed!";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(mContext, msg, duration);
        toast.show();
        this.location = location;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        writeLocation(location);
    }

    /*
    Writes location to datafile
     */
    public void writeLocation(Location location) {
        try {

            FileOutputStream filewriter = new FileOutputStream(this.datafile, true);
            String out = String.format("%d, %f, %f, %f\n",
                    location.getTime(),
                    location.getAccuracy(),
                    location.getLatitude(),
                    location.getLongitude());

            filewriter.write(out.getBytes());
            filewriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}