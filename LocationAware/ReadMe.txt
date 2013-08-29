Exercise Performance Feedback: Music Feedback
Read Me

Last modified: 5/16/13
By: Joey Huang
-------------

Purpose: This Android application is intended to perform the function of exercise
performance feedback. Its purpose is to measure and log a runner's speed
and pace, and provide auditory feedback to attempt to encourage a 
consistent running pace. It can optionally be first run in No feedback mode
in order to ascertain a realistic minimum, maximum, and average pace
(miles/min) for the user. The main activity, Feedback Mode, takes in
a minimum and maximum pace entered by the user. During the run session,
with the user's phone playing music in the background, if the user's pace 
falls out of range of these threshold values, the music will be muted until
the user's pace returns to within range. 


Installing the Application
----------------------------
This is a working version of the Music Feedback App. To install on an Android
device, install Eclipse IDE with Android SDK. Import the project in the file
'LocationAware'. Connect your Android device to the computer and build and install
the project.

Alternatively, drag and drop the 'MenuActivity.apk' file into the appropriate App
file on your Android device.


Running the Application
----------------------------

Prior to starting this application, it is recommended that the user
initiates music playback using any music player installed on the phone.
Music is necessary for the music feedback mode of this application.

On the opening screen are two buttons: (1) Find Baseline and (2) Music
Feedback. The Find Baseline button should be greyed out; the current
version of this application has the functionality of both modes contained
in the Music Feedback selection.

There are two operating modes:

1. Without Feedback
		- Measures and updates instantaneous pace on the screen.
		- Updates maximum and minimum pace (miles/min) on the screen.
		- Additionally updates GPS address and lattitude/longitude.
		- Creates two output files.
			log.txt		Records time (HH:MM:SS), instantaneous pace, 
						average pace, minimum pace, and maximum pace
						(miles/min). Raw, unprocessed data. Saved to
						DOWNLOADS folder on Android phone.
						
			[Name].txt	Records time (m), speed (m/s), average speed,
						maximum speed, minimum speed (m/s). Minimal
						processing of data to remove outliers that
						occur from inaccurate GPS measurements.
						Specifically, initial outliers are removed,
						and instantaneous lapses (1 s) are replaced
						with interpolated averages. Saved to DOWNLOADS
						folder on Android phone.
						
2. With Feedback
		- Measures and updates instaneous pace on the screen.
		- Disables volume if instant pace falls out of range of a user
		- specified maximum and minimum pace (miles/min).
		- Additionally updates GPS address and lattitude/longitude.
		- Creates two output files.
			log.txt		Records time (HH:MM:SS), instantaneous pace, 
						average pace, minimum pace, and maximum pace
						(miles/min). Raw, unprocessed data.
						
			[Name].txt	Records time (m), speed (m/s), average speed,
						maximum speed, minimum speed (m/s). Minimal
						processing of data to remove outliers that
						occur from inaccurate GPS measurements.
						Specifically, initial outliers are removed,
						and instantaneous lapses (1 s) are replaced
						with interpolated averages.

						
Standard Operation : Without Feedback
---------------------------------------

To initiate either modes, select a GPS mode (fine-grain provider or  both
providers. Fine-grain provider receives location updates from the fine 
location provider (gps) only. Both providers  receives location updates 
from both the fine (gps) and coarse (network) location providers.

A file name should entered in the 'Name' field. It should be a single word
(like a person's name) without special characters or file extension.

Scroll down and adjust the volume to desired level. 

If the 'Feedback' button indictea 'On', press it to change back to no
feedback mode.

When beginning the run session, press the 'Start' button to start
recording. When finished running, pressed the 'Stop' button.

To view run data, open log.txt or [Name].txt (see operating mode details).

Standard Operation : With Feedback
-------------------------------------

To initiate either modes, select a GPS mode (fine-grain provider or  both
providers. Fine-grain provider receives location updates from the fine 
location provider (gps) only. Both providers  receives location updates 
from both the fine (gps) and coarse (network) location providers.

A file name should entered in the 'Name' field. It should be a single word
(like a person's name) without special characters or file extension.

Input desired minimum and maximum pace (miles/min) within which the user
intends to attempt to run consistently. These fields can only be set
before the start button or feedback button is pressed.

Scroll down and adjust the volume to desired level. 

Press the 'Feedback' button to initiate feedback (make sure it says 'On').

When beginning the run session, press the 'Start' button to start
recording. When finished running, pressed the 'Stop' button.

To view run data, open log.txt or [Name].txt (see operating mode details).
