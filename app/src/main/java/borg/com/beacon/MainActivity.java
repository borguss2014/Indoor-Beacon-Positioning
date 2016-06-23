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

package borg.com.beacon;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.provider.Settings;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Revision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import borg.com.trilateration.NonLinearLeastSquaresSolver;
import borg.com.trilateration.TrilaterationFunction;


@TargetApi(23)
public class MainActivity extends Activity {
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final int SCAN_PERIOD = 5000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    private int vvalue = 0;
    private boolean scanBegin = true;
    private int nrOfSets = 7 * 20;

    HashMap<String,Map<String, String>> foundBeacons = new LinkedHashMap<>();
    HashMap<String,Map<String, String>> completeBeaconData = new LinkedHashMap<>();

    Map<String, Double> distMap = new LinkedHashMap<>();
    //Map<String, Double> armaMeasurements = new LinkedHashMap<>();
    HashMap<String,Map<String, Double>> armaMeasurements = new LinkedHashMap<>();
    HashMap<String,Map<String, Integer>> powerLevelsFound = new LinkedHashMap<>();
    HashMap<String, Map<String, Double>> min_max_distances = new LinkedHashMap<>();

    String phoneRSSI;
    String beaconRSSI;
    String maj;
    String min;


    String[] storagePermissions = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE" };
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_STORAGE = 2;
    private static final int REQUEST_CHECK_SETTINGS = 3;


    WebView web;
    Manager manager ;
    Database database;

    final String key = "beacons";


    String TAG = "FUNCTION";

    boolean documentsReplicated = false;
    boolean fileLoaded = false;

    boolean pageLoaded = false;

    boolean firstTimeOpened = true;
    boolean locate_user = false;

    boolean scan = true;

    String beaconPositionsJSON;

    int nrBeaconsDetection = 0;

    double valueEstimate = 0;
    double distance = 0;

    double[][] positions = new double[][]{};
    double[] distances = new double[]{};

    int nrOfLevelsScanned = 0;
    boolean firstScan = true;



    HashMap<String,Map<String, Double>> errorEstimate = new LinkedHashMap<>();
    double errorMeasurement = 3;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState != null){
            firstTimeOpened = savedInstanceState.getBoolean("FirstTimeOpened");
        }

        Log.d("BooleanValue", String.valueOf(firstTimeOpened));

        //GET LOCATION AND STORAGE PERMISSIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs storage access");
                builder.setMessage("Please grant storage access so this app can log beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(storagePermissions, PERMISSION_REQUEST_STORAGE);
                    }
                });
                builder.show();
            }

        }
        //==================================================================================================================================


        //TELL USER TO START LOCATION SERVICE
        if(!isLocationEnabled(getApplicationContext())){
            displayLocationSettingsRequest(getApplicationContext());
        }
        //===========================================================


        //Check if BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        //=================================================================================

        //Use handler object so the scan thread communicates with the main thread
        mHandler = new Handler();

        //Initialize the bluetooth adapter variable
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //Create webview
        web = (WebView) findViewById(R.id.webView);
        web.setWebContentsDebuggingEnabled(true);
        web.addJavascriptInterface(this, "android");

        //Setting webview zoom and zoom controls
        web.setInitialScale(1);
        WebSettings settings = web.getSettings();
        settings.setSupportZoom(true);
        settings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
        settings.setDisplayZoomControls(true);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptEnabled(true);
        /////////////////////////////////////////



        //Try to get local database . If database not found, one will be created automatically
        try {
            manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase("testing");

        } catch (Exception e) {
            Log.e("Couch", "Error getting database", e);
            Toast.makeText(getApplicationContext(), "Error: Database couldn't be created", Toast.LENGTH_SHORT).show();
            return;
        }



    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_scan:
                if(scan) {
                    Toast.makeText(getApplicationContext(), "Locating ...", Toast.LENGTH_SHORT).show();
                    scan = false;
                    locateUser(true, SCAN_PERIOD);
                }
                else{
                    locateUser(false, SCAN_PERIOD);
                    scan = true;
                }
                break;

            case R.id.action_list:
                Intent beaconListIntent = new Intent(getApplicationContext(), BeaconList.class);

                startActivity(beaconListIntent);
                break;

            case R.id.delete_database:
                try {
                    database.delete();
                    Log.i(TAG, "DATABASE DELETED");
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                break;
        }
        return true;

    }


    @Override
     protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();

                if(firstTimeOpened){
                    Toast.makeText(getApplicationContext(), "Please wait, searching for beacons ...", Toast.LENGTH_LONG).show();
                    scanLeDevice(true, SCAN_PERIOD);
                    firstTimeOpened = false;
                }
            }
            //scanLeDevice(true);


        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("FirstTimeOpened", firstTimeOpened);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }

        if(requestCode == REQUEST_CHECK_SETTINGS){
            if(resultCode == Activity.RESULT_OK){
                Log.i("Location", "Location enabled");
            }
            else if(resultCode == Activity.RESULT_CANCELED){
                finish();
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void locateUser(final boolean enable, int scanPeriod){
        if(enable){
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

            } else {
                mLEScanner.startScan(filters, settings, beaconScanCallback);
            }
        }
        else{
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(beaconScanCallback);

                for (Map.Entry<String, Map<String, String>> beacon : completeBeaconData.entrySet()) {
                    Map<String, String> b = beacon.getValue();
                    String x = b.get("X");
                    String y = b.get("Y");
                    Log.d("BeaconPos", "UUID: " + beacon.getKey() + " " + " X: " + x + " " + " Y: " + y);
                }

            }
        }
    }

    private void scanLeDevice(final boolean enable, int scanPeriod) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    } else {

                        if(foundBeacons.size() == 0){
                            Toast.makeText(getApplicationContext(), "Your location couldn't be determined ! (maybe there are no beacons in the area?)", Toast.LENGTH_LONG).show();
                            scanLeDevice(false, 0);
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Scan finished succesfully, loading map..", Toast.LENGTH_SHORT).show();
                        mLEScanner.stopScan(mScanCallback);

                        //For each beacon found, search for couchdb document by UUID-key
                        outerloop:
                        for (Map.Entry<String, Map<String, String>> entry : foundBeacons.entrySet()) {

                            Map<String, String> beaconData = entry.getValue();
                            final String retrievedUUID = beaconData.get("UUID");

                            if(checkLocalDocument(database, key, retrievedUUID)){  //If document found in local database
                                Log.d(TAG, "Document found in local database, attempt loading...");
                                Query query = database.createAllDocumentsQuery();
                                query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
                                QueryEnumerator result;
                                try {
                                    result = query.run();
                                    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                                        QueryRow row = it.next();

                                        if(row.getDocument().getProperty(key) != null) {
                                            LinkedHashMap<String, Object> json =  (LinkedHashMap<String, Object>) row.getDocument().getProperties().get(key); //doc.beacons
                                            for (Map.Entry<String, Object> jsonBeaconObject : json.entrySet()) {
                                                Map<String, String> jsonBeaconProp = (Map<String, String>) jsonBeaconObject.getValue();
                                                if (jsonBeaconProp.containsKey("UUID")) {
                                                    if(jsonBeaconProp.get("UUID").equals(retrievedUUID)) {
                                                        Log.d(TAG, row.getDocumentId());

                                                        Document doc = row.getDocument();
                                                        Revision rev = doc.getCurrentRevision();
                                                        Attachment att = rev.getAttachment("test.svg");
                                                        if (att != null) {
                                                            InputStream is = att.getContent();
                                                            BufferedReader r = new BufferedReader(new InputStreamReader(is));
                                                            StringBuilder total = new StringBuilder();
                                                            String line;
                                                            while ((line = r.readLine()) != null) {
                                                                total.append(line).append('\n');
                                                            }
                                                            final String rs = total.toString();
                                                            final String svgFile = createHTML(rs);

                                                            //Preparing uuids to be sent to javascript for position processing
                                                            ArrayList<String> uuids = new ArrayList<>();

                                                            for (Map.Entry<String, Map<String, String>> c : foundBeacons.entrySet()) {
                                                                Map<String, String> beacon = c.getValue();
                                                                String uuid_data = beacon.get("UUID");
                                                                uuids.add(uuid_data);
                                                            }

                                                            JSONArray jsonArray = new JSONArray(uuids);
                                                            final String jsonData = jsonArray.toString();
                                                            //================================================================


                                                            //Load SVG here
                                                            web.post(new Runnable() {
                                                                @Override
                                                                public void run() {


                                                                    web.setWebViewClient(new WebViewClient() {
                                                                        @Override
                                                                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                                                            return false;
                                                                        }
                                                                    });
                                                                    web.loadUrl("about:blank");
                                                                    web.loadDataWithBaseURL("file:///android_asset/",svgFile, "text/html", "utf-8", "");

                                                                    Handler handler = new Handler();
                                                                    handler.post(new Runnable(){
                                                                        @Override
                                                                        public void run(){
                                                                            while(!pageLoaded) {
                                                                            }
                                                                            web.evaluateJavascript("getBeaconPositions(" + jsonData + ");", null);
                                                                            pageLoaded = false;
                                                                        }
                                                                    });

                                                                    Log.i(TAG, "File loaded");
                                                                }

                                                            });

                                                            for (Map.Entry<String, Map<String, String>> data : foundBeacons.entrySet()) {

                                                                Map<String, String> bD = data.getValue();

                                                                Map<String, String> beacon = new HashMap<>();
                                                                beacon.put("MAJOR", bD.get("MAJOR"));
                                                                beacon.put("MINOR", bD.get("MINOR"));


                                                                completeBeaconData.put(bD.get("UUID"), beacon);
                                                            }

                                                            foundBeacons.clear();

                                                            break outerloop;
                                                        }
                                                        else{
                                                            Toast.makeText(getApplicationContext(), "WARNING: Cannot load file", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (CouchbaseLiteException | IOException e) {
                                    e.printStackTrace();
                                }

                            }

                            //Check if document with beacon UUID-key is found in local database
                           else if(!checkLocalDocument(database, key, retrievedUUID)){  //If not found, do a filtered replication on the remote database
                                Log.d(TAG, "Document not found in local database, attempting to retrieve from remote database...");

                                // start pull replication
                                Replication replication = null;
                                try {
                                    String username = "admin";
                                    String password = "tardis2010";
                                    replication = database.createPullReplication(new URL("http://"+username+":"+password+"@borguss.ddns.net:5984/testing"));

                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }

                                if (replication != null) {
                                    replication.addChangeListener(new Replication.ChangeListener() {
                                        @Override
                                        public void changed(Replication.ChangeEvent event) {
                                            String TAGz = "Replication";

                                            Replication replicationStatus = event.getSource();

                                            Log.d(TAGz, "Replication : " + replicationStatus + " changed.");

                                            if (!replicationStatus.isRunning()) {
                                                String msg = String.format("Replicator %s not running", replicationStatus);
                                                Log.d(TAGz, msg);
                                                //If document was replicated and the SVG wasn't loaded , attempt to load it in the webview
                                                if(documentsReplicated && !fileLoaded){
                                                    //Check if document exists in local database (if it was replicated)
                                                    if(checkLocalDocument(database, key, retrievedUUID)){
                                                        Log.d(TAG, "Document retrieved and found in local DB . Attempting to load in webview...");
                                                        //Use a query on the lcoal database to retrieve the SVG
                                                        Query query = database.createAllDocumentsQuery();
                                                        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
                                                        QueryEnumerator result;
                                                        try {
                                                            result = query.run();
                                                            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                                                                QueryRow row = it.next();

                                                                if(row.getDocument().getProperty(key) != null) {
                                                                    LinkedHashMap<String, Object> json =  (LinkedHashMap<String, Object>) row.getDocument().getProperties().get(key); //doc.beacons
                                                                    for (Map.Entry<String, Object> jsonBeaconObject : json.entrySet()) {
                                                                        Map<String, String> jsonBeaconProp = (Map<String, String>) jsonBeaconObject.getValue();
                                                                        if (jsonBeaconProp.containsKey("UUID")) {
                                                                            if(jsonBeaconProp.get("UUID").equals(retrievedUUID)) {
                                                                                Document doc = row.getDocument();
                                                                                Revision rev = doc.getCurrentRevision();
                                                                                Attachment att = rev.getAttachment("test.svg");
                                                                                if (att != null) {
                                                                                    InputStream is = att.getContent();
                                                                                    BufferedReader r = new BufferedReader(new InputStreamReader(is));
                                                                                    StringBuilder total = new StringBuilder();
                                                                                    String line;
                                                                                    while ((line = r.readLine()) != null) {
                                                                                        total.append(line).append('\n');
                                                                                    }
                                                                                    final String rs = total.toString();
                                                                                    final String svgFile = createHTML(rs);

                                                                                    //Preparing uuids to be sent to javascript for position processing
                                                                                    ArrayList<String> uuids = new ArrayList<>();

                                                                                    for (Map.Entry<String, Map<String, String>> c : foundBeacons.entrySet()) {
                                                                                        Map<String, String> beacon = c.getValue();
                                                                                        String uuid_data = beacon.get("UUID");
                                                                                        uuids.add(uuid_data);
                                                                                    }

                                                                                    JSONArray jsonArray = new JSONArray(uuids);
                                                                                    final String jsonData = jsonArray.toString();
                                                                                    //================================================================

                                                                                    //Load SVG here
                                                                                    web.post(new Runnable() {
                                                                                        @Override
                                                                                        public void run() {


                                                                                            web.setWebViewClient(new WebViewClient() {
                                                                                                @Override
                                                                                                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                                                                                    return false;
                                                                                                }
                                                                                            });
                                                                                            web.loadUrl("about:blank");
                                                                                            web.loadDataWithBaseURL("file:///android_asset/",svgFile, "text/html", "utf-8", "");


                                                                                            Handler handler = new Handler();
                                                                                            handler.post(new Runnable(){
                                                                                                @Override
                                                                                                public void run(){
                                                                                                    while(!pageLoaded) {
                                                                                                    }
                                                                                                    web.evaluateJavascript("getBeaconPositions(" + jsonData + ");", null);
                                                                                                    pageLoaded = false;
                                                                                                }
                                                                                            });

                                                                                            Log.i(TAG, "File loaded");
                                                                                        }

                                                                                    });

                                                                                    for (Map.Entry<String, Map<String, String>> data : foundBeacons.entrySet()) {

                                                                                        Map<String, String> bD = data.getValue();

                                                                                        Map<String, String> beacon = new HashMap<>();
                                                                                        beacon.put("MAJOR", bD.get("MAJOR"));
                                                                                        beacon.put("MINOR", bD.get("MINOR"));

                                                                                        completeBeaconData.put(bD.get("UUID"), beacon);
                                                                                    }

                                                                                    foundBeacons.clear();

                                                                                    break;
                                                                                }
                                                                                else{
                                                                                    Toast.makeText(getApplicationContext(), "WARNING: Cannot load file", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                Log.d(TAG, row.getDocumentId());
                                                            }
                                                        } catch (CouchbaseLiteException | IOException e) {
                                                            e.printStackTrace();
                                                        }

                                                    }
                                                    else{
                                                        Log.d(TAG, "ERROR: Document couldn't be loaded");
                                                    }
                                                }
                                            } else {
                                                int processed = replicationStatus.getCompletedChangesCount();
                                                int total = replicationStatus.getChangesCount();
                                                String msg = String.format("Replicator processed %d / %d", processed, total);
                                                Log.d(TAGz, msg);

                                                if(processed < total){
                                                    documentsReplicated = false;
                                                }
                                                else if(processed == total && total != 0){
                                                    documentsReplicated = true;
                                                }
                                            }

                                        }
                                    });
                                }

                                getRemoteDocument(replication, "replicator/filterByBeacon", "beaconUUID", retrievedUUID);
                            }
                        }
                    }
                }
            }, scanPeriod);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);

            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }


    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {


            Log.i("result", result.toString());


            //if(result.getScanRecord().getManufacturerSpecificData().keyAt(0) == 89){

            //0x02 refers to beacon

            if (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[0] == 2) {

                String beaconAddress = result.getDevice().getAddress();


                if (!foundBeacons.containsKey(beaconAddress)) {


                    Map<String, String> beacon = new HashMap<>();


                    int startByte = 2;
                    byte[] uuidBytes = new byte[16];
                    System.arraycopy(result.getScanRecord().getManufacturerSpecificData().valueAt(0), startByte, uuidBytes, 0, 16);
                    String hexString = bytesToHex(uuidBytes);

                    //Here is your UUID
                    String uuidTemp = hexString.substring(0, 8) + "-" +
                            hexString.substring(8, 12) + "-" +
                            hexString.substring(12, 16) + "-" +
                            hexString.substring(16, 20) + "-" +
                            hexString.substring(20, 32);

                    String uuid = uuidTemp.toLowerCase();


                    maj = String.valueOf((result.getScanRecord().getManufacturerSpecificData().valueAt(0)[18] & 0xff) * 0x100 +
                            (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[19] & 0xff));

                    min = String.valueOf((result.getScanRecord().getManufacturerSpecificData().valueAt(0)[20] & 0xff) * 0x100 +
                            (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[21] & 0xff));

                    phoneRSSI = Integer.toString(result.getRssi());
                    beaconRSSI = Byte.toString(result.getScanRecord().getManufacturerSpecificData().valueAt(0)[22]);


                    beacon.put("UUID", uuid);
                    beacon.put("MAJOR", maj);
                    beacon.put("MINOR", maj);
                    foundBeacons.put(beaconAddress, beacon);

                }

            }


            //For testing purposes / future use . DO NOT DELETE
//            else {
//                if (!bDevices.contains(btDevice.getAddress())) {
//                    bDevices.add(btDevice.getAddress());
//                    listDataHeader.add(btDevice.getAddress());
//                }
//
//                String phoneRSSI = Integer.toString(result.getRssi());
//                String beaconRSSI = Byte.toString(result.getScanRecord().getManufacturerSpecificData().valueAt(0)[7]);
//
//                for (int i = 0; i < listDataHeader.size(); i++) {
//                    if (btDevice.getAddress().contains(listDataHeader.get(i))) {
//
//                        List<String> child = new ArrayList<String>();
//
//                        child.add("Phone RSSI: " + phoneRSSI);
//                        child.add("Received RSSI: " + beaconRSSI);
//
//                        listDataChild.put(listDataHeader.get(i), child);
//
//                    }
//                }
//
//            }

            //}
            //////////////////////////


        }




        private void writeBeaconData(String beaconData) {
            File file = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)) + "/testFile.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (file.exists()) {
                File readFile = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)) + "/testFile.txt");
                if (readFile.canRead()) {
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(readFile);
                        byte[] reader = new byte[fis.available()];
                        while (fis.read(reader) != -1) {
                        }
                        FileOutputStream fOut = null;
                        try {
                            fOut = new FileOutputStream(file, true);
                            fOut.write(beaconData.getBytes());
                        } finally {
                            if (fOut != null) {
                                try {
                                    fOut.flush();
                                    fOut.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    } catch (IOException e) {
                        Log.e("FILE", e.getMessage(), e);
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } else {
                    System.out.println("Unable to read/write sdcard file, see logcat output");
                }
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }


    };


    private ScanCallback beaconScanCallback = new ScanCallback() {



        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            //Log.i("BEACONresult", result.toString());
            //Log.d("DEBUG", "Found beacon");

            if (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[0] == 2) {

                //Log.d("DEBUG", "Beacon is NORDIC SEMICONDUCTOR");

                int startByte = 2;
                byte[] uuidBytes = new byte[16];
                System.arraycopy(result.getScanRecord().getManufacturerSpecificData().valueAt(0), startByte, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //Here is your UUID
                String uuidTemp = hexString.substring(0, 8) + "-" +
                        hexString.substring(8, 12) + "-" +
                        hexString.substring(12, 16) + "-" +
                        hexString.substring(16, 20) + "-" +
                        hexString.substring(20, 32);

                String uuid = uuidTemp.toLowerCase();

                phoneRSSI = Integer.toString(result.getRssi());
                beaconRSSI = Byte.toString(result.getScanRecord().getManufacturerSpecificData().valueAt(0)[22]);


                if(powerLevelsFound.get(uuid).get(beaconRSSI) == 0){
                    Log.d("DEBUG", "Scanning power level " + beaconRSSI + " for the first time, incrementing by one");
                    //First time scanned this power level
                    powerLevelsFound.get(uuid).remove(beaconRSSI);
                    powerLevelsFound.get(uuid).put(beaconRSSI, 1);
                    nrOfLevelsScanned++;
                }

                Log.d("DEBUG", "Power levels scanned for the first time: " + nrOfLevelsScanned);
                //Keep adding fresh filtered data until the set is finished
                if(nrOfLevelsScanned < (7 * completeBeaconData.size())){
                    Log.d("DEBUG", "Still not enough power levels scanned, adding arma filtered RSSI");

                    double armaPowerLevel = armaMeasurements.get(uuid).get(beaconRSSI);
                    Log.d("DEBUG", "For beacon " + uuid + ", previous filtered RSSI value: " + armaPowerLevel);

                    //Filter each RSSI in a separate manner
                    double estimateARMA = 0;
                    estimateARMA = armaFilter(0.1, armaPowerLevel, Double.parseDouble(phoneRSSI));

//                    double kalmanGain = errorEstimate.get(uuid).get(beaconRSSI) / (errorEstimate.get(uuid).get(beaconRSSI) + errorMeasurement);
//                    double kalmanEstimate = armaMeasurements.get(uuid).get(beaconRSSI) + kalmanGain*(Double.valueOf(phoneRSSI) - armaMeasurements.get(uuid).get(beaconRSSI));
//                    double kalmanErrorEstimate = (1 - kalmanGain) * errorEstimate.get(uuid).get(beaconRSSI);

                    Log.d("DEBUG", "For beacon " + uuid + ", current filtered RSSI value: " + estimateARMA);
                    armaMeasurements.get(uuid).remove(beaconRSSI);
                    armaMeasurements.get(uuid).put(beaconRSSI, estimateARMA);

//                    errorEstimate.get(uuid).remove(beaconRSSI);
//                    errorEstimate.get(uuid).put(beaconRSSI, kalmanErrorEstimate);

                }
                //If the set is completed for all beacons , start trilaterating
                else if(nrOfLevelsScanned == (7 * completeBeaconData.size())){
                    Log.d("DEBUG", "Enough power levels scanned , starting trilateration");

                    //Prepare ordered UUIDs to match positions with distances
                    Set<String> uuidKeys = armaMeasurements.keySet();
                    String[] uuidKeysArray = uuidKeys.toArray(new String[uuidKeys.size()]);

                    ArrayList<Double> rssiValuesOnDifferentPowerLevels = new ArrayList<>();

                    //Create a map with minimum and maximum distance for each beacon
                    for(int i=0; i < uuidKeysArray.length; i++){

                        //Get the RSSI for every power level , calculate distance and add it in an array .
                        // After that , extract the minimum and maximum distance from the array and
                        //add it to the map    min_max_distances
                        for (Map.Entry<String, Double> powerLevelWithFilteredValue : armaMeasurements.get(uuidKeysArray[i]).entrySet()) {
                            double filteredRSSIForPowerLevel = powerLevelWithFilteredValue.getValue();

                            //Calculate distance
                            distance = Math.pow(10,(filteredRSSIForPowerLevel - getReferenceRSSI(Integer.parseInt(beaconRSSI))) / (-10 * 2.7));
                            distance = (distance * 960) / 5.5;

                            rssiValuesOnDifferentPowerLevels.add(distance);
                        }

                        double minimumDistance = Collections.min(rssiValuesOnDifferentPowerLevels);
                        double maximumDistance = Collections.max(rssiValuesOnDifferentPowerLevels);

                        Log.d("DEBUG", "Min distance for beacon " + uuidKeysArray[i] + " is: " + minimumDistance);
                        Log.d("DEBUG", "Max distance for beacon " + uuidKeysArray[i] + " is: " + maximumDistance);


                        Map<String, Double> minMaxDist = new HashMap<>();
                        minMaxDist.put("min", minimumDistance);
                        minMaxDist.put("max", maximumDistance);

                        min_max_distances.put(uuidKeysArray[i], minMaxDist);

                        rssiValuesOnDifferentPowerLevels.clear();
                    }

                    //Create the minimum and maximum distance arrays for trilateration
                    double minimumDistancesArray[] = new double[uuidKeysArray.length];
                    double maximumDistancesArray[] = new double[uuidKeysArray.length];

                    for(int i=0; i<uuidKeysArray.length; i++) {
                        minimumDistancesArray[i] = min_max_distances.get(uuidKeysArray[i]).get("min");
                        maximumDistancesArray[i] = min_max_distances.get(uuidKeysArray[i]).get("max");
                    }


                    positions = new double[uuidKeysArray.length][2];

                    //Fill the positions array based on the order of the UUIDs stored
                    int x=0;
                    for(int i=0; i<uuidKeysArray.length; i++){
                        for(int j=0; j<2; j++){
                            if(j==0) {
                                positions[i][j] = Double.valueOf(completeBeaconData.get(uuidKeysArray[x]).get("X"));
                            }
                            else if(j==1){
                                positions[i][j] = Double.valueOf(completeBeaconData.get(uuidKeysArray[x]).get("Y"));
                            }
                        }
                        x++;
                    }


                    //Log.d("DEBUG", "Position for beacon " + uuidKeysArray[0] + " is " + positions[0][0] + " " + positions[0][1]);
                    //Log.d("DEBUG", "Position for beacon " + uuidKeysArray[1] + " is " + positions[1][0] + " " + positions[2][1]);
                    //Log.d("DEBUG", "Position for beacon " + uuidKeysArray[2] + " is " + positions[2][0] + " " + positions[2][1]);


                    //Preparing uuids to be sent to javascript for range-to-beacon processing
                    JSONArray jsonArrayUUIDS = null;
                    try {
                        jsonArrayUUIDS = new JSONArray(uuidKeysArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String jsonUUIDs = jsonArrayUUIDS.toString();


                    JSONArray jsonArrayMinDistances = null;
                    try {
                        jsonArrayMinDistances = new JSONArray(minimumDistancesArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String jsonMinDistances = jsonArrayMinDistances.toString();


                    JSONArray jsonArrayMaxDistances = null;
                    try {
                        jsonArrayMaxDistances = new JSONArray(maximumDistancesArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String jsonMaxDistances = jsonArrayMaxDistances.toString();


                    //Trilaterating based on beacon positions and distances to beacons

                    //Trilaterate for minimum distances
                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, minimumDistancesArray), new LevenbergMarquardtOptimizer());
                    LeastSquaresOptimizer.Optimum optimum = solver.solve();

                    //Resulted position after trilateration for minimum distances
                    double[] locationForMinDistances = optimum.getPoint().toArray();

                    double xPosMin = locationForMinDistances[0];
                    double yPosMin = locationForMinDistances[1];

                    //Trilaterate for maximum distances
                    solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, maximumDistancesArray), new LevenbergMarquardtOptimizer());
                    optimum = solver.solve();

                    //Resulted position after trilateration for maximum distances
                    double[] locationForMaxDistances = optimum.getPoint().toArray();

                    double xPosMax = locationForMaxDistances[0];
                    double yPosMax = locationForMaxDistances[1];

                    Log.d("DEBUG", "X MIN: " + xPosMin + " Y MIN: " + yPosMin + " X MAX: " + xPosMax + " Y MAX: " + yPosMax);

                    //Sending position data for drawing
                    web.evaluateJavascript("drawPosition(" + jsonUUIDs + " , " + xPosMin + " , " + yPosMin + " , " + jsonMinDistances + " , " + xPosMax + " , " + yPosMax + " , " + jsonMaxDistances + ");", null);


                    //Clean everything and prepare for the next data set
                    min_max_distances.clear();
                    powerLevelsFound.clear();
                    nrOfLevelsScanned = 0;

                    for(int i=0; i<uuidKeysArray.length; i++) {
                        Map<String, Integer> power_levels = new HashMap<>();
                        power_levels.put("4", 0);
                        power_levels.put("0", 0);
                        power_levels.put("-4", 0);
                        power_levels.put("-8", 0);
                        power_levels.put("-12", 0);
                        power_levels.put("-16", 0);
                        power_levels.put("-20", 0);
                        powerLevelsFound.put(uuidKeysArray[i], power_levels);
                    }

                }


            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

    };


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
    new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("onLeScan", device.toString());
                    //connectToDevice(device);
                }
            });
        }
    };

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }




    public static boolean isLocationEnabled(Context context){
        int locationMode = 0;
        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("Location", "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("Location", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("Location", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("Location", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Ceva", "Location permission granted");
                }
                else{
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Location access not granted");
                    builder.setMessage("This app won't be able to detect beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @TargetApi(Build.VERSION_CODES.M)
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }

            case PERMISSION_REQUEST_STORAGE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Ceva", "Storage permission granted");
                }
                else{
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Storage access not granted");
                    builder.setMessage("This app won't be able to log beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @TargetApi(Build.VERSION_CODES.M)
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    public boolean checkLocalDocument(Database db, String key, String value){
        //Get all documents in local DB
        Query query = db.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        QueryEnumerator result = null;
        try {
             result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();

                if(row.getDocument().getProperty(key) != null) {
                    LinkedHashMap<String, Object> json =  (LinkedHashMap<String, Object>) row.getDocument().getProperties().get(key); //doc.beacons
                    for (Map.Entry<String, Object> k : json.entrySet()) {
                        Map<String, String> j = (Map<String, String>) k.getValue();
                        if (j.containsKey("UUID")) {
                            if(j.get("UUID").equals(value)) {
                                return true;
                            }
                        }
                    }
                }
                Log.d(TAG, row.getDocumentId());
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void getRemoteDocument(Replication replication, String filter, String key, String value){

        replication.setFilter(filter);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(key, value);
        replication.setFilterParams(params);
        replication.setContinuous(false);
        replication.start();

    }

    public String createHTML(String svg){

        String upperHTML =
                        "<html>" + "\n"
                        + "<head>" + "\n"
                        + "<script src=\"file:///android_asset/snap.svg.js\" type=\"text/javascript\"></script>" + "\n"
                        + "<script src=\"file:///android_asset/beacon.data.js\" type=\"text/javascript\"></script>" + "\n"
                        + "</head>" + "\n"
                        + "<body>" + "\n";

        String lowerHTML =
                        "</body>" + "\n" + "</html>";


        return upperHTML + svg + lowerHTML;
    }

    public double armaFilter(double smoothingFactor, double previousArmaMeasurement , double rssi){
        double armaMeasurement = 0;
        armaMeasurement = previousArmaMeasurement - smoothingFactor * (previousArmaMeasurement - rssi);
        return armaMeasurement;
    }

    public int getReferenceRSSI(int refRSSI){

        int rssiAt1m = 0;

        switch(refRSSI){
            case -20 :
                rssiAt1m = -84;
                break;
            case -16:
                rssiAt1m = -81;
                break;
            case -12:
                rssiAt1m = -77;
                break;
            case -8:
                rssiAt1m = -72;
                break;
            case -4:
                rssiAt1m = -69;
                break;
            case 0:
                rssiAt1m = -65;
                break;
            case 4:
                rssiAt1m = -59;
                break;
        }

        return rssiAt1m;
    }

    @JavascriptInterface
    public void retrievedBeaconPositions(String json) throws JSONException{
        Log.d("JSONData", "Retrieved beacon positions from SVG");
        Log.d("JSONData", json);
        beaconPositionsJSON = json;

        try {

            if(beaconPositionsJSON != null) {
                JSONObject retrievedJSON = new JSONObject(beaconPositionsJSON);

                Log.d("TEST", "Trying to retrieve beacon positions from json data");

                for (Map.Entry<String, Map<String, String>> beacon : completeBeaconData.entrySet()) {
                    JSONArray uuidData = retrievedJSON.getJSONArray(beacon.getKey());
                    for(int i=0; i<uuidData.length(); i++){
                        JSONObject position = uuidData.getJSONObject(i);

                        String x = position.getString("x");
                        String y = position.getString("y");

                        Log.d("TEST", "X: " + x + " " + " Y: " + y);

                        completeBeaconData.get(beacon.getKey()).put("X", x);
                        completeBeaconData.get(beacon.getKey()).put("Y", y);

                        Log.d("TEST", "X': " + completeBeaconData.get(beacon.getKey()).get("X") + " " + " Y': " + completeBeaconData.get(beacon.getKey()).get("Y"));

                    }
                }

                Set<String> idKeys = completeBeaconData.keySet();
                String[] uuidKeysArray = idKeys.toArray(new String[idKeys.size()]);

                for(int i=0; i<uuidKeysArray.length; i++) {
                    Map<String, Double> power_levels = new HashMap<>();
                    power_levels.put("4", 0.0);
                    power_levels.put("0", 0.0);
                    power_levels.put("-4", 0.0);
                    power_levels.put("-8", 0.0);
                    power_levels.put("-12", 0.0);
                    power_levels.put("-16", 0.0);
                    power_levels.put("-20", 0.0);
                    armaMeasurements.put(uuidKeysArray[i], power_levels);
                }


//                for(int i=0; i<uuidKeysArray.length; i++) {
//                    Map<String, Double> power_levels = new HashMap<>();
//                    power_levels.put("4", 0.0);
//                    power_levels.put("0", 0.0);
//                    power_levels.put("-4", 0.0);
//                    power_levels.put("-8", 0.0);
//                    power_levels.put("-12", 0.0);
//                    power_levels.put("-16", 0.0);
//                    power_levels.put("-20", 0.0);
//                    errorEstimate.put(uuidKeysArray[i], power_levels);
//                }

                for(int i=0; i<uuidKeysArray.length; i++) {
                    Map<String, Integer> power_levels = new HashMap<>();
                    power_levels.put("4", 0);
                    power_levels.put("0", 0);
                    power_levels.put("-4", 0);
                    power_levels.put("-8", 0);
                    power_levels.put("-12", 0);
                    power_levels.put("-16", 0);
                    power_levels.put("-20", 0);
                    powerLevelsFound.put(uuidKeysArray[i], power_levels);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void notifyAndroidOnLibraryLoaded(){
        pageLoaded = true;
    }
}
