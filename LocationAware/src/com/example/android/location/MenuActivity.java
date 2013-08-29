/*
 * 
 * Last modified: 5/15/13
 * Author: Joey Huang
 * 
 * Notes: Opening menu.
 *  options:
 *  1. calibrate (not implemented, function included in main task)
 *  2. main task (implemented)
 */

package com.example.android.location;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MenuActivity extends Activity {
	private Button mCalibrateButton;
	private Button mMainButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		mCalibrateButton = (Button) findViewById(R.id.calibrateButton);
		mCalibrateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
			
			
			
		});
		mMainButton = (Button) findViewById(R.id.mainButton);
		mMainButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(getApplicationContext(), LocationActivity.class);
                startActivity(myIntent);
				
			}
			
		});

		mCalibrateButton.setEnabled(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_menu, menu);
		return true;
	}

}
