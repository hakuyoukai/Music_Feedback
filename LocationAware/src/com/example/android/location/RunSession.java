package com.example.android.location;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RunSession implements Serializable{
	public List<Double> speedList;
	public List<Integer> timeList;
	public double slowestSpeed;
	public double averageSpeed;
	public double fastestSpeed;
	public double thresholdPace;
	
	public RunSession() {
		speedList = new ArrayList<Double>();
		timeList = new ArrayList<Integer>();
		slowestSpeed = 1000;
		averageSpeed = 0;
		fastestSpeed = 0;
		thresholdPace = 0;
	}
	
}