# Indoor-Beacon-Positioning (Work-In-Progress)

![Alt text](/6.jpg?raw=true "Indoor Location Detection")

Android application which uses Nordic Semiconductor beacons (nRF51822) to track a user's position indoors.

This app will scan for beacons in the area when its first opened , after which the user can trigger location detection .
The minimum and maximum distance to each beacon is displayed with a green circle , and respectively a blue circle . 
User position , beacons and other elements in the map are displayed in a WebView and manipulated with Snap.svg: 

    https://github.com/adobe-webplatform/Snap.svg

Trilateration is done using :

    https://github.com/lemmingapex/Trilateration

# Usage

This application requires a CouchDB database to store its maps in SVG format . The map must be created with a modified version of Method-Draw .  CouchDB acts as a web server for the map editor and as a regular database for the SVG maps .

A CouchDB filter is also required so that the application can find the maps based on the beacons detected in the area. This filter was used , stored as a CouchDB document under the name "_design/replicator" in the same database where the maps are stored :


    function(doc, req) {
        if (typeof doc.beacons !== 'undefined') {
            for (var prop in doc.beacons) {
                if (typeof doc.beacons[prop] !== 'undefined' && doc.beacons[prop].UUID == req.query.beaconUUID) {
                    return true;
                }
            }
        }
        return false;
    }
    
The name of the database used by this application and the map editor is "testing". If a different database name is required , it must be changed in both . Also, the IP of the CouchDB database must be provided/changed so it can properly replicate the maps.  

Further information about this map editor can be found at : https://github.com/borguss2014/SVG-Editor


-----------------------------------------------

Copyright(C) 2016 under Apache version 2.0

Made by: Spoiala Viorel Cristian (student at Politehnica University of Bucharest - Faculty of engineering in foreign languages)

E-mail: kittelle92@gmail.com

(This application serves as my fourth year license project)
