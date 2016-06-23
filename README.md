# Indoor-Beacon-Positioning (Work-In-Progress)

Android application which uses Nordic Semiconductor beacons (nRF51822) to track a user's position indoors.

This app will scan for beacons in the area when its first opened , after which the user can trigger location detection .
User position , beacons and other elements in the map are displayed in a WebView and manipulated with Snap.svg : https://github.com/adobe-webplatform/Snap.svg

Trilateration is done using : https://github.com/lemmingapex/Trilateration

# Usage

This application requires a CouchDB database to store its maps in SVG format . The map must be created with a modified version of Method-Draw .  CouchDB acts as a web server for the map editor and as a regular database for the SVG maps .

Further information about this map editor can be found at : https://github.com/borguss2014/SVG-Editor


-----------------------------------------------

Copyright(C) 2016 under Apache version 2.0

Made by: Spoiala Viorel Cristian (student at Politehnica University of Bucharest - Faculty of engineering in foreign languages)

E-mail: kittelle92@gmail.com

(This application serves as my fourth year license project)
