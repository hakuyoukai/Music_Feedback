/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * 
 * 
 * Last modified: 5/15/13
 * Author: Joey Huang
 * 
 * 
 * Notes:
 * Location Activity modified for use in music feedback. 
 * This activity has two modes: 
 * 1. Without feedback - logs speed, pace, time, average pace, and min/max paces
 * 2. With feedback - functionalities as mode without feedback. Also, if music from
 * external music player app is playing in background, if pace falls outside of
 * user-specified range, music will cut off. 
 * 
 * Two output files. One appends to log.txt file. Includes data such as time HH:MM:SS and pace (miles/min).
 * This file is printed in real time contains raw data.
 * 
 * Second output file is written on stop button press. It contains processed data stored in 
 * RunSession structure, including time in milliseconds and speed (m/s).
 * Outliers at beginning of run are removed, and temporary lapses of GPS provided data are removed 
 * and replaced by an interpolated average(e.g. GPS sent a sequence of speed readings of 2 m/s, 0 m/s,
 *  2 m/s, indicating an inaccurate measurement during that interval where most likely the speed was a constant 2 m/s. 
 *  
 *  See accompanied ReadMe.txt file for information on how to use the application.
 */

package com.example.android.location;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("NewApi")
public class LocationActivity extends FragmentActivity {
    private TextView mLatLng;
    private TextView mAddress;
    private TextView mSpeed;
    private TextView avgSpeedView;
    private TextView avgPaceView;
    private EditText mMinPace;
    private EditText mMaxPace;
    private EditText mNameField;
    private Button mFineProviderButton;
    private Button mBothProviderButton;
    private LocationManager mLocationManager;
    private Handler mHandler;
    private boolean mGeocoderAvailable;
    private boolean mUseFine;
    private boolean mUseBoth;
    private double currPace;
    private double minPace;
    private double maxPace;
    private ToggleButton tb;
    private static AudioManager amanager; 
    private int onVolume;
    private SeekBar volumeSlider;
    private Button logStartButton;
    private Button logStopButton;
    private double totalSpeeds;
    private double averageSpeed;
    private int measureCount;
    private boolean fileOpen;
    private boolean gotInfinity;
    RunSession rs = new RunSession();
    private ToggleButton feedbackButton;
    private List<Double> speedBuffer;
    boolean feedbackOn = false;

    private int runNum = 0;
    // Keys for maintaining UI states after rotation.
    private static final String KEY_FINE = "use_fine";
    private static final String KEY_BOTH = "use_both";
    // UI handler codes.
    private static final int UPDATE_ADDRESS = 1;
    private static final int UPDATE_LATLNG = 2;
    private static final int UPDATE_SPEED = 3;
    private static final int UPDATE_AVG_SPEED = 4;
    private static final int UPDATE_AVG_PACE = 5;

    private static final int TIMEINTERVAL = 1000;
    private static final int DISTANCEINTERVAL = 0;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    OutputStreamWriter osw;
    FileOutputStream fOut;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private Calendar cal;
    private int seconds;

    
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        fileOpen = false;
        // Restore apps state (if exists) after rotation.
        if (savedInstanceState != null) {
            mUseFine = savedInstanceState.getBoolean(KEY_FINE);
            mUseBoth = savedInstanceState.getBoolean(KEY_BOTH);
        } else {
            mUseFine = false;
            mUseBoth = false;
        }
        amanager = (AudioManager)getSystemService(AUDIO_SERVICE);
        onVolume = 10;
        volumeSlider = (SeekBar) findViewById(R.id.volumeSlider);
        volumeSlider.setMax(amanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeSlider.setOnSeekBarChangeListener(new volumeSliderChangeListener());
        feedbackButton = (ToggleButton) findViewById(R.id.feedbackONOFF);
        feedbackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				feedbackOn = !feedbackOn;
			}
        });
        tb = (ToggleButton)this.findViewById(R.id.tglSetStatus);
        tb.setChecked(fileOpen);
        mLatLng = (TextView) findViewById(R.id.latlng);
        mAddress = (TextView) findViewById(R.id.address);
        mSpeed = (TextView) findViewById(R.id.speed);
        avgSpeedView = (TextView) findViewById(R.id.avgspeedfield);
        avgPaceView = (TextView) findViewById(R.id.avgpacefield);
        mMinPace = (EditText) findViewById(R.id.minpaceIndicator);
        mMaxPace = (EditText) findViewById(R.id.maxpaceIndicator);
        mMaxPace.setText("0.0");
        mMinPace.setText("40.0");
        mNameField  = (EditText) findViewById(R.id.nameField);
        totalSpeeds = 0;
        measureCount = 0;
        averageSpeed = 0;

        maxPace = 30;
        logStartButton = (Button) findViewById(R.id.logStartButton);
        logStartButton.setOnClickListener(new StartButtonListener());
        
        logStopButton = (Button) findViewById(R.id.logStopButton);
        logStopButton.setOnClickListener(new StopButtonListener());
        
        // Receive location updates from the fine location provider (gps) only.
        mFineProviderButton = (Button) findViewById(R.id.provider_fine);
        // Receive location updates from both the fine (gps) and coarse (network) location
        // providers.
        mBothProviderButton = (Button) findViewById(R.id.provider_both);

        // The isPresent() helper method is only available on Gingerbread or above.
        mGeocoderAvailable =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent();

        // Handler for updating text fields on the UI like the lat/long and address.
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_ADDRESS:
                        mAddress.setText((String) msg.obj);
                        break;
                    case UPDATE_LATLNG:
                        mLatLng.setText((String) msg.obj);
                        break;
                    case UPDATE_SPEED:
                    	mSpeed.setText((String) msg.obj);
                    	break;
                    case UPDATE_AVG_SPEED:
                    	avgSpeedView.setText((String) msg.obj);
                    	break;
                    case UPDATE_AVG_PACE:
                    	avgPaceView.setText((String) msg.obj);
                    	break;
                }
            }
        };
        // Get a reference to the LocationManager object.
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        
        cal = Calendar.getInstance();
        seconds = (int)(cal.getTimeInMillis()/1000);
        speedBuffer = new ArrayList<Double>();
        speedBuffer.add(0.0);
        speedBuffer.add(0.0);
        speedBuffer.add(0.0);
        gotInfinity = false;
 
        //TODO: LOAD for debugging old save files
        //   loadData();
    }
        

    // Restores UI states after rotation.
    

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FINE, mUseFine);
        outState.putBoolean(KEY_BOTH, mUseBoth);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setup();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if the GPS setting is currently enabled on the device.
        // This verification should be done during onStart() because the system calls this method
        // when the user returns to the activity, which ensures the desired location provider is
        // enabled each time the activity resumes from the stopped state.
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            // Build an alert dialog here that requests that the user enable
            // the location services, then when the user clicks the "OK" button,
            // call enableLocationSettings()
            new EnableGpsDialogFragment().show(getSupportFragmentManager(), "enableGpsDialog");
        }
    }

    // Method to launch Settings
    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    // Stop receiving location updates whenever the Activity becomes invisible.
    @Override
    protected void onStop() {
        super.onStop();
        mLocationManager.removeUpdates(listener);
    }

    // Set up fine and/or coarse location providers depending on whether the fine provider or
    // both providers button is pressed.
    private void setup() {
    	
        Location gpsLocation = null;
        Location networkLocation = null;
        mLocationManager.removeUpdates(listener);
        mLatLng.setText(R.string.unknown);
        mAddress.setText(R.string.unknown);
        // Get fine location updates only.
        if (mUseFine) {
            mFineProviderButton.setBackgroundResource(R.drawable.button_active);
            mBothProviderButton.setBackgroundResource(R.drawable.button_inactive);
            // Request updates from just the fine (gps) provider.
            gpsLocation = requestUpdatesFromProvider(
                    LocationManager.GPS_PROVIDER, R.string.not_support_gps);
            // Update the UI immediately if a location is obtained.
            if (gpsLocation != null) updateUILocation(gpsLocation);
        } else if (mUseBoth) {
            // Get coarse and fine location updates.
            mFineProviderButton.setBackgroundResource(R.drawable.button_inactive);
            mBothProviderButton.setBackgroundResource(R.drawable.button_active);
            // Request updates from both fine (gps) and coarse (network) providers.
            gpsLocation = requestUpdatesFromProvider(
                    LocationManager.GPS_PROVIDER, R.string.not_support_gps);
            networkLocation = requestUpdatesFromProvider(
                    LocationManager.NETWORK_PROVIDER, R.string.not_support_network);

            // If both providers return last known locations, compare the two and use the better
            // one to update the UI.  If only one provider returns a location, use it.
            if (gpsLocation != null && networkLocation != null) {
                updateUILocation(getBetterLocation(gpsLocation, networkLocation));
            } else if (gpsLocation != null) {
                updateUILocation(gpsLocation);
            } else if (networkLocation != null) {
                updateUILocation(networkLocation);
            }
        }
        
    }

    /**
     * Method to register location updates with a desired location provider.  If the requested
     * provider is not available on the device, the app displays a Toast with a message referenced
     * by a resource id.
     *
     * @param provider Name of the requested provider.
     * @param errorResId Resource id for the string message to be displayed if the provider does
     *                   not exist on the device.
     * @return A previously returned {@link android.location.Location} from the requested provider,
     *         if exists.
     */
    private Location requestUpdatesFromProvider(final String provider, final int errorResId) {
        Location location = null;
        if (mLocationManager.isProviderEnabled(provider)) {
            mLocationManager.requestLocationUpdates(provider, TIMEINTERVAL, DISTANCEINTERVAL, listener);
            location = mLocationManager.getLastKnownLocation(provider);
        } else {
            Toast.makeText(this, errorResId, Toast.LENGTH_LONG).show();
        }
        return location;
    }

    // Callback method for the "fine provider" button.
    public void useFineProvider(View v) {
        mUseFine = true;
        mUseBoth = false;
        setup();
    }

    // Callback method for the "both providers" button.
    public void useCoarseFineProviders(View v) {
        mUseFine = false;
        mUseBoth = true;
        setup();
    }

    private void doReverseGeocoding(Location location) {
        // Since the geocoding API is synchronous and may take a while.  You don't want to lock
        // up the UI thread.  Invoking reverse geocoding in an AsyncTask.
        (new ReverseGeocodingTask(this)).execute(new Location[] {location});
    }

    private void updateUILocation(Location location) {
        // We're sending the update to a handler which then updates the UI with the new
        // location.
    	
    	
    	
    	// also updates displayed pace, and instant speed data stored in RunSession
    	cal = Calendar.getInstance();
    	double instantSpeed = location.getSpeed();
    	
    	// speedbuffer in order to process spotty GPS
    	// buffer of 3 measurements. if the center speed measurement == zero, surrounded by two
    	// nonzero measurements, then the GPS provided a poor measurement and the average of
    	// replaced at that point instead.
    	speedBuffer.add(instantSpeed);
    	speedBuffer.remove(0);
    	if (fileOpen)
    		rs.speedList.add(instantSpeed);
    	if (gotInfinity) {
    		if (instantSpeed > 0.01 && speedBuffer.get(0) >0.01) 
    			speedBuffer.set(1,(speedBuffer.get(0)+speedBuffer.get(2))/2);
    			if (fileOpen) {
    				if (rs.speedList.size() > 1) {
    					rs.speedList.set(rs.speedList.size()-2,(speedBuffer.get(0)+speedBuffer.get(2))/2);
    				}
    			}
    		gotInfinity = false;
    	}
    		
    	if (instantSpeed < 0.01 && speedBuffer.get(1) > 0.01) {
    		if (gotInfinity == false)
    			gotInfinity = true;
    	}
    	currPace = 1/(instantSpeed*0.0372); // m/s to miles/min
    	
        int s = (int)(cal.getTimeInMillis()/1000);
    	int timePassed = 0;
    	if (s > seconds)
    		timePassed = s - seconds;
    	else
    		timePassed = s - seconds + 60;
    	
    	seconds = s;
        if (fileOpen) {
        	rs.timeList.add(s);
        	
        	if (instantSpeed > rs.fastestSpeed)
        		rs.fastestSpeed = instantSpeed;
        	if (instantSpeed < rs.slowestSpeed)
        		rs.slowestSpeed = instantSpeed;
        totalSpeeds += instantSpeed*timePassed;
        measureCount+=timePassed;
        if (measureCount >0) {
        	averageSpeed = totalSpeeds/measureCount;
        	rs.averageSpeed = averageSpeed;
        }
        
        Message.obtain(mHandler,UPDATE_AVG_SPEED,averageSpeed+"").sendToTarget();
        }  	
        Message.obtain(mHandler,
        		UPDATE_LATLNG,
                location.getLatitude() + ", " + location.getLongitude()).sendToTarget();
        Message.obtain(mHandler,
                UPDATE_SPEED,
                currPace + "").sendToTarget();
        
        if (fileOpen) {
        if (feedbackOn) {
        	if (currPace > maxPace && currPace <= minPace)   
        	{
        		amanager.setStreamVolume(AudioManager.STREAM_MUSIC, onVolume, 0);
        	}
        	else if (currPace < maxPace || currPace > minPace) 
        	{
        		amanager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        	}  
        }
        else {
        	if (currPace > minPace && currPace <50)	
        	{	
        		minPace = currPace;
        		mMinPace.setText(minPace + "");
        	}


        	if (currPace < maxPace)
        	{
        		maxPace = currPace;
        		mMaxPace.setText(maxPace + "");
        	}       
        	
        	
        }
        writeToFile();       
        }
        // Bypass reverse-geocoding only if the Geocoder service is available on the device.
        if (mGeocoderAvailable) doReverseGeocoding(location);
    }
    
    // updates saved time and prints time and currents pace to file
    private void writeToFile() {    	
    	cal = Calendar.getInstance();
    	if (fileOpen) {
    	try {
    	osw.write(dateFormat.format(cal.getTime()) + " " + currPace + "\n");
    	//osw.write(currTimeInMillis + " " + currPace + "\n");
    	} catch (Exception e) {
    		printMessage("Problem writing to file.");
    	}
    	}
    }
    private final LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // A new location update is received.  Do something useful with it.  Update the UI with
            // the location update.
        	
            updateUILocation(location);
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
    };

    /** Determines whether one Location reading is better than the current Location fix.
      * Code taken from
      * http://developer.android.com/guide/topics/location/obtaining-user-location.html
      *
      * @param newLocation  The new Location that you want to evaluate
      * @param currentBestLocation  The current Location fix, to which you want to compare the new
      *        one
      * @return The better Location object based on recency and accuracy.
      */
    protected Location getBetterLocation(Location newLocation, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return newLocation;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved.
        if (isSignificantlyNewer) {
            return newLocation;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return currentBestLocation;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return newLocation;
        } else if (isNewer && !isLessAccurate) {
            return newLocation;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return newLocation;
        }
        return currentBestLocation;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    // AsyncTask encapsulating the reverse-geocoding API.  Since the geocoder API is blocked,
    // we do not want to invoke it from the UI thread.
    private class ReverseGeocodingTask extends AsyncTask<Location, Void, Void> {
        Context mContext;

        public ReverseGeocodingTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Location... params) {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

            Location loc = params[0];
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
                // Update address field with the exception.
                Message.obtain(mHandler, UPDATE_ADDRESS, e.toString()).sendToTarget();
            }
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                // Format the first line of address (if available), city, and country name.
                String addressText = String.format("%s, %s, %s",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                        address.getLocality(),
                        address.getCountryName());
                // Update address field on UI.
                Message.obtain(mHandler, UPDATE_ADDRESS, addressText).sendToTarget();
            }
            return null;
        }
    }
    
    
    // print any message as toasts (currently used for error messages/debugging)
	public void printMessage(String errmsg) {
		String msg = errmsg;
		Toast tempMessage = 
				Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		tempMessage.show();
		if(fileOpen) {
			try {
			osw.write(msg + "\n");
			} catch (Exception e) {
				
			}
		}
	}
	
	
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {						// not used
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    } 
    /**
     * Dialog to prompt users to enable GPS on the device.
     */
    private class EnableGpsDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.enable_gps)
                    .setMessage(R.string.enable_gps_dialog)
                    .setPositiveButton(R.string.enable_gps, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            enableLocationSettings();
                        }
                    })
                    .create();
        }
    }
    
    private class volumeSliderChangeListener implements OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser)
        {
        	onVolume = progress;
        		amanager.setStreamVolume(AudioManager.STREAM_MUSIC, onVolume, 0);
        }
        public void onStartTrackingTouch(SeekBar seekBar)
        {
        }

       	public void onStopTrackingTouch(SeekBar seekBar)
        {
        }
    }
    
    // handles pressing start button
    // opens log.txt file and writes header
    // reset initial data
    private class StartButtonListener implements OnClickListener {
    	
        	public void onClick(View v) {
                if (isExternalStorageWritable()==false) {
                	printMessage("external storage is not writable");
                } 
                else if (isExternalStorageReadable() == false) {
                	printMessage("external storage is not readable");
                }
                else {
        	                    
        	            // open output file
        	    	        try {
        	    	           	 File outfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"log.txt");
        	    	            // set to append to file if it already exists
        	    	            fOut = new FileOutputStream(outfile,true);
        	    	            osw = new OutputStreamWriter(fOut); 
        	    	            
        	    	            // reset values
        	    	            // display to 0.0
        	    	            Message.obtain(mHandler,UPDATE_AVG_PACE,"infinity").sendToTarget();
        	    	            Message.obtain(mHandler,UPDATE_AVG_SPEED,"0.00").sendToTarget();
        	    	            //
        	    	            averageSpeed = 0;
        	    	            totalSpeeds = 0;
        	    	            measureCount = 0;
        	    	            fileOpen = true;
        	    	            tb.setChecked(fileOpen);
        	    	            printMessage("Opened file");
        	    	            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        	    	            cal = Calendar.getInstance();
        	    	            seconds = (int)(cal.getTimeInMillis()/1000);
        	    	            
        	    	            // for feedback mode, read in pace values entered by user
        	    	            if (feedbackOn == true) {
        	    					minPace = Double.parseDouble(mMinPace.getText().toString());
        	    			        maxPace = Double.parseDouble(mMaxPace.getText().toString());
        	    			    		
        	    				}
        	    	            else {
        	    	            	minPace = 0;
        	    	            	maxPace = 1000;
        	    	            }
        	    	            osw.write("\n\n\n" + dateFormat.format(cal.getTime()) + "---------------------------\n\n");
        	    	            osw.write("minpace : " + mMinPace.getText() + " min/mile\n");
        	    	            osw.write("maxpace : " + mMaxPace.getText() + " min/mile\n\n\n");
        	    	            
        	    	            rs.thresholdPace = minPace;
        	    	        } catch(Exception e){
        	    	        	printMessage("Error opening file.");
        	    	        }
        	            }
                }
        	
    }
    
    // stop button
    // closes filestream
    // writes RunSessionData to specified filename
    // if feedback off, write min and max paces to displayed fields
    // displays average pace and speed
    private class StopButtonListener implements OnClickListener {
    	public void onClick(View v) {
    		if (fileOpen) {
    			try {
    				printMessage("Done recording");
    				double averagePace = 1/(averageSpeed*0.0372);
    				Message.obtain(mHandler,UPDATE_AVG_PACE,String.format("%.2f", averagePace)).sendToTarget();
    				osw.write("\nAverage Speed : " + averageSpeed + "\n");
    				osw.write("\nAverage Pace : " + averagePace + "\n---------------------------\n\n\n");
    				osw.close();
    				fOut.close();
    				fileOpen = false;
    				tb.setChecked(fileOpen);
    				printMessage("closed file");
    				if (!rs.speedList.isEmpty()) {
    				cleanData();
    				
    				averageSpeed = rs.averageSpeed;
    				averagePace = 1/(averageSpeed*0.0372);
    		        Message.obtain(mHandler,UPDATE_AVG_SPEED,String.format("%.2f", averageSpeed)).sendToTarget();
    		        Message.obtain(mHandler,UPDATE_AVG_PACE,String.format("%.2f", averagePace)).sendToTarget();
    				saveData();
    				printRunSessionData(rs);
    			  	 runNum++;
    				if (!feedbackOn && !rs.speedList.isEmpty()) {
    					minPace = 1/(rs.slowestSpeed*0.0372);
    					maxPace = 1/(rs.fastestSpeed*0.0372);
    					mMinPace.setText(minPace + "");
    					mMaxPace.setText(maxPace + "");
    				}
    				}
    				rs = new RunSession();
    				

    				
    			} catch (Exception e) {
    				printMessage("Error stop button close");
    			}
    			
    			
    		}
    	}
    }

    
    // saves RunSession data to a save file, named specified in name field
    public void saveData() {
        FileOutputStream f;
        ObjectOutputStream o;
        try {    
       	 File outfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),mNameField.getText().toString() + runNum +".sav");
         f = new FileOutputStream(outfile,true);
            o = new ObjectOutputStream(f);
            o.writeObject(rs);
            o.close();
        } catch (Exception e) {
            e.printStackTrace();
            printMessage("error in sav function");
        }
    }

    
    // cleans data stored in RunSession instance
    // initial conditions: RunSession must not be empty
    public void cleanData() {
    	int index = 0;
    	double max = 0;
    	double min = 1000;
    	
    	// identifies first good data point (speed ~ jogging, threshold 1.2 m/s)
    	for (int i = 0; i < rs.speedList.size();i++) {
    		if (rs.speedList.get(i) > 1.2) {
    			index = i;
    			break;
    		}
    	}
    	
    	// removes initial bad data points (due to GPS issues - outliers)
    	if (index >0 && index < rs.speedList.size()) {
    		for (int i = index-1;i >=0;i--) {
    			rs.speedList.remove(i);
    			rs.timeList.remove(i);
    		}
    	}
    	
    	if(!rs.speedList.isEmpty()) {
    	// recompute max and min speeds
    	double totalSum = 0;
    	double totalTimeSum = rs.timeList.get(0);
    	double initialTime = rs.timeList.get(0);
    	for (int i = 0; i < rs.speedList.size();i++) {
    		double speed = rs.speedList.get(i);
    		double instanttime = 1;
    		if (i != 0) {
    			instanttime = rs.timeList.get(i) - rs.timeList.get(i-1);
    		} 
    		totalSum += speed*instanttime;
    		if (speed > max)
    			max = speed;
    		if (speed < min)
    			min = speed;
    	}
    	
    	if (rs.speedList.size() >1) 
    		rs.averageSpeed = totalSum/(rs.timeList.get(rs.timeList.size()-1)-rs.timeList.get(0));
    	else if (rs.speedList.size() == 1)
    		rs.averageSpeed = rs.speedList.get(0);
    	rs.slowestSpeed = min;
    	rs.fastestSpeed = max;
    	
    	}  	
    }
    
    // prints run session data to a text file
    public void printRunSessionData(RunSession r) {
      	 File outfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),mNameField.getText().toString() + runNum +".txt");
     try {
		fOut = new FileOutputStream(outfile,true);
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	}
     
     osw = new OutputStreamWriter(fOut); 
    	try {
			osw.write("Time (miliseconds)\tSpeed(m/s)\n");

    	for (int i = 0; i < r.timeList.size(); i++) {
    		osw.write(r.timeList.get(i) + "\t" + r.speedList.get(i) + "\n");
    	}
    	
    	osw.write("slowestSpeed: " + r.slowestSpeed + "\n");
    	osw.write("averageSpeed: " + r.averageSpeed + "\n");
    	osw.write("fastestSpeed: " + r.fastestSpeed + "\n");
    	osw.close();
    	fOut.close();
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

    
    // load RunSession Data ('xxx.sav')
    // intended to print out RunSession class data
    // to implement, uncomment line following 'TODO:LOAD' (~line )
    public void loadData(){
    	FileInputStream f;
    	ObjectInputStream o;
    	try{    // loads previously saved player data
    		f = new FileInputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "log1366426731358.sav");
    		o = new ObjectInputStream(f);
    		RunSession temp = (RunSession) o.readObject();
    		printRunSessionData(temp);
    		System.out.println("Good");
    		o.close();
    	}catch(IOException e){
    		e.printStackTrace();
    	} catch(ClassNotFoundException c){
    		c.printStackTrace();
    	}
    }
}