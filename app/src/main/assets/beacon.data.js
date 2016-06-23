/* Copyright 2016 Spoiala Viorel Cristian
E-mail: kittelle92@gmail.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

var x = "Loaded beacon.data.js library";
console.log(x);

android.notifyAndroidOnLibraryLoaded();

function getBeaconPositions (beaconUUIDS) {

        console.log("Entered function");
        var beacons = beaconUUIDS;
        var json = '{';

        for (var i = 0; i < beacons.length - 1; i++) {

            var id = "#X" + beacons[i];
            var svg_beacon = Snap(id);

            json = json +
                '"' + beacons[i] + '" : [' +
                '{ "x" : "' + svg_beacon.getBBox().cx + '" , "y" : "' + svg_beacon.getBBox().cy + '" }' + '],';

        }

        var id = "#X" + beacons[beacons.length - 1];
        var svg_beacon = Snap(id);

        json = json +
            '"' + beacons[beacons.length - 1] + '" : [' +
            '{ "x" : "' + svg_beacon.getBBox().cx + '" , "y" : "' + svg_beacon.getBBox().cy + '" }' + ']}';

        android.retrievedBeaconPositions(json);

}

function drawPosition (uuids, xPosMin, yPosMin, minDistances, xPosMax, yPosMax, maxDistances){

    var beacons = uuids;

    var minDist = minDistances;
    var maxDist = maxDistances;

    var s = Snap("svg");

    for(var i=0; i<beacons.length; i++){
        var id = "#X" + beacons[i];
        var beacon = Snap(id);

        var idd = "beaconMinRange" + i;

        var newID = Snap("#beaconMinRange" + i);

        if(beacon){
            Snap(newID).remove();
            var beaconMinRange = s.circle(beacon.getBBox().cx, beacon.getBBox().cy, minDist[i]);
            beaconMinRange.attr({
                id: idd,
                "fill-opacity": '0.0',
                stroke: 'green',
                strokeWidth: '3'
            })
        }
        else{
            var beaconMinRange = s.circle(beacon.getBBox().cx, beacon.getBBox().cy, minDist[i]);
            beaconMinRange.attr({
                id: idd,
                "fill-opacity": '0.0',
                stroke: 'green',
                strokeWidth: '3'
            })
        }
    }

    for(var i=0; i<beacons.length; i++){
            var id = "#X" + beacons[i];
            var beacon = Snap(id);

            var idd = "beaconMaxRange" + i;

            var newID = Snap("#beaconMaxRange" + i);

            if(beacon){
                Snap(newID).remove();
                var beaconMaxRange = s.circle(beacon.getBBox().cx, beacon.getBBox().cy, maxDist[i]);
                beaconMaxRange.attr({
                    id: idd,
                    "fill-opacity": '0.0',
                    stroke: 'blue',
                    strokeWidth: '3'
                })
            }
            else{
                var beaconMaxRange = s.circle(beacon.getBBox().cx, beacon.getBBox().cy, maxDist[i]);
                beaconMaxRange.attr({
                    id: idd,
                    "fill-opacity": '0.0',
                    stroke: 'blue',
                    strokeWidth: '3'
                })
            }
    }



    var circleCenterX = (xPosMin + xPosMax) / 2;
    var circleCenterY = (yPosMin + yPosMax) / 2;

    var r = (xPosMax - circleCenterX)*(xPosMax - circleCenterX) + (yPosMax - circleCenterY)*(yPosMax - circleCenterY)
    var radius = Math.sqrt(r);

    var circleExist = Snap("#Position");


    if(circleExist){
        //console.log("Exists");
        Snap("#Position").remove();

        var circle = s.circle(circleCenterX, circleCenterY, radius);
        circle.attr({
            id:'Position',
            fill: 'red'
        })

    }
    else{
        //console.log("Doesnt exist");
        var circle = s.circle(circleCenterX, circleCenterY, radius);
        circle.attr({
            id:'Position',
            fill: 'red'
        })
    }

    console.log("Circle center: " + circleCenterX + " " + circleCenterY);
    console.log("Circle radius: " + radius);
    //console.log("UUIDs: " + beacons);
    //console.log("Minimum Distances: " + minDist);
    //console.log("Maximum Distances: " + maxDist);

}

// var clickFunction = function() {
//     console.log("Clicked element with position: " + circleFromDom1.getBBox().cx + " " + circleFromDom1.getBBox().cy);
// }
//
// var circleFromDom1 = Snap("#Xe20a39f4-73f5-4bc4-a12f-17d1ad07a961");
// var s = Snap("svg");
// s.append(s.circle(circleFromDom1.getBBox().cx, circleFromDom1.getBBox().cy, 10));
//
// circleFromDom1.click(clickFunction);
