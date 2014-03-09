# uninotts-android #

An Android app for student timetables at the University of Nottingham.

## Dependencies ##
In order to build the app the following dependencies are required - they need to be imported into the same Eclipse workspace - verify that the libraries are configured in the project preferences.

- **CardsUI Library**
	- This library is used to create the Google Now/Play style cards, includes stacking and swiping cards, also embedded drawables
	- My fork includes extra features required by the project
	- `https://github.com/mherod/cardsui-for-android`
- **Crouton**
	- This library is used to provide toasts which drop down from the Action Bar
	- `https://github.com/keyboardsurfer/Crouton`
- **Google Play Services**
	- This library is used for better location monitoring, for directions and realtime notifications
	- Located in the **android-sdk**

Other libraries used are included in the `libs` folder

- Android Support v13
- Apache HTTP
- Joda Time
- JSON.simple

Also included as library are the set of API libraries for the University of Nottingham information, which includes the **Student Now** framework. 