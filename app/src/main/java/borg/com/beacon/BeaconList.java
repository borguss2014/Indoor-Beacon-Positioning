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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Borg on 5/4/2016.
 */
public class BeaconList extends Activity{

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    private List<String> bDevices = new ArrayList<String>();
    private List<String> bData = new ArrayList<String>();

    private boolean expRet = false;

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 25000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;


    private boolean firstTime = true;

    private boolean collectIsEnabled;
    private String distance;
    private String path;
    private String sset;

    private int value = 0;
    private boolean scanBegin = true;
    private int nrOfSets = 7 * 20;

    String phoneRSSI;
    String beaconRSSI;
    String maj;
    String min;
    double dist;

    ArrayList<Double> filteredRSSIValues;
    ArrayList<Double> rawRSSIValues;
    ArrayList<Double> distances;

    double valueEstimate = 0;
    double errorEstimate = 40;
    double errorMeasurement = 3;

    double kGain = 0;

    boolean firstScan = true;


    String[] storagePermissions = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE" };
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_STORAGE = 2;

    private static final int REQUEST_CHECK_SETTINGS = 3;


    double armaMeasurement = -75;

    ArrayList<Double> firstLevel = new ArrayList<>();
    ArrayList<Double> secondLevel = new ArrayList<>();
    ArrayList<Double> thirdLevel = new ArrayList<>();
    ArrayList<Double> fourthLevel = new ArrayList<>();
    ArrayList<Double> fifthLevel = new ArrayList<>();
    ArrayList<Double> sixthLevel = new ArrayList<>();
    ArrayList<Double> seventhLevel = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.beacon_list);

        filteredRSSIValues = new ArrayList<>();
        rawRSSIValues = new ArrayList<>();
        distances = new ArrayList<>();

        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        listAdapter = new ExpandableListAdapter(getApplicationContext(), listDataHeader, listDataChild);


        // Initialize the expandable list view variable
        expListView = (ExpandableListView) findViewById(R.id.listExp);
        expListView.setAdapter(listAdapter);

        // Listview Group click listener
        expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                // Toast.makeText(getApplicationContext(),
                // "Group Clicked " + listDataHeader.get(groupPosition),
                // Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        // Listview Group expanded listener
        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
//                Toast.makeText(getApplicationContext(),
//                        listDataHeader.get(groupPosition) + " Expanded",
//                        Toast.LENGTH_SHORT).show();

                expRet = true;
            }
        });

        // Listview Group collasped listener
        expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

            @Override
            public void onGroupCollapse(int groupPosition) {
//                Toast.makeText(getApplicationContext(),
//                        listDataHeader.get(groupPosition) + " Collapsed",
//                        Toast.LENGTH_SHORT).show();

                expRet = false;

            }
        });

        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
//                Toast.makeText(
//                        getApplicationContext(),
//                        listDataHeader.get(groupPosition)
//                                + " : "
//                                + listDataChild.get(
//                                listDataHeader.get(groupPosition)).get(
//                                childPosition), Toast.LENGTH_SHORT)
//                        .show();
                return false;
            }
        });

        //Use handler object so the scan thread communicates with the main thread
        mHandler = new Handler();

        //Initialize the bluetooth adapter variable
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }



    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_beacon_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_scan_beacons:
                Toast.makeText(getApplicationContext(), "Scanning", Toast.LENGTH_SHORT).show();
                scanLeDevice(true);
                break;
            case R.id.action_test:


                Intent i = new Intent(getApplicationContext(), testDevice.class);

                if (firstTime) {
                    i.putExtra("first time", true);
                    firstTime = false;
                } else {
                    i.putExtra("first time", false);
                    i.putExtra("check", collectIsEnabled);
                    i.putExtra("dist", distance);
                    i.putExtra("path", path);
                    i.putExtra("set", sset);
                }

                startActivityForResult(i, 42);
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
                filters = new ArrayList<ScanFilter>();
            }
            //scanLeDevice(true);


        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
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

        if (requestCode == 42) {
            if (resultCode == Activity.RESULT_OK) {

                boolean check = data.getBooleanExtra("set", false);
                String dist = data.getStringExtra("dist");
                String pat = data.getStringExtra("path");
                String set = data.getStringExtra("sets");

                collectIsEnabled = check;
                distance = dist;
                path = pat;
                sset = set;

                System.out.println("Checkbox: " + collectIsEnabled + " | Distance: " + distance + " | Path: " + path + " | Sets: " + sset);

            }
        }

        if(requestCode == REQUEST_CHECK_SETTINGS){
            if(resultCode == Activity.RESULT_OK){
                Log.i("Location", "Location enabled");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    } else {
                        Toast.makeText(getApplicationContext(), "Scan finished", Toast.LENGTH_SHORT).show();
                        mLEScanner.stopScan(mScanCallback);
//                        Log.d("RecordedRSSI", rawRSSIValues.toString());
//                        Log.d("RecordedFilteredRSSI", filteredRSSIValues.toString());
//                        Log.d("RecordedDistances", distances.toString());

                        Log.d("First Level", firstLevel.toString());
                        Log.d("Second Level", secondLevel.toString());
                        Log.d("Third Level", thirdLevel.toString());
                        Log.d("Fourth Level", fourthLevel.toString());
                        Log.d("Fifth Level", fifthLevel.toString());
                        Log.d("Sixth Level", sixthLevel.toString());
                        Log.d("Seventh Level", seventhLevel.toString());

                        filteredRSSIValues.clear();
                        rawRSSIValues.clear();
                        distances.clear();
                    }
                }
            }, SCAN_PERIOD);
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
            case -56:
                rssiAt1m = -56;
                break;
            default:
                rssiAt1m = -1;
                break;

        }

        return rssiAt1m;
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {


            //Log.i("callbackType", String.valueOf(callbackType));
            //Log.i("result", result.toString());


            BluetoothDevice btDevice = result.getDevice();

            //if(result.getScanRecord().getManufacturerSpecificData().keyAt(0) == 89){

            //0x02 refers to beacon
            if (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[0] == 2) {

                if (!bDevices.contains(btDevice.getAddress())) {
                    bDevices.add(btDevice.getAddress());
                    listDataHeader.add(btDevice.getAddress());
                }


                int startByte = 2;
                byte[] uuidBytes = new byte[16];
                System.arraycopy(result.getScanRecord().getManufacturerSpecificData().valueAt(0), startByte, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //Here is your UUID
                String uuid = hexString.substring(0, 8) + "-" +
                        hexString.substring(8, 12) + "-" +
                        hexString.substring(12, 16) + "-" +
                        hexString.substring(16, 20) + "-" +
                        hexString.substring(20, 32);

                maj = String.valueOf((result.getScanRecord().getManufacturerSpecificData().valueAt(0)[18] & 0xff) * 0x100 + (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[19] & 0xff));
                min = String.valueOf((result.getScanRecord().getManufacturerSpecificData().valueAt(0)[20] & 0xff) * 0x100 + (result.getScanRecord().getManufacturerSpecificData().valueAt(0)[21] & 0xff));
                phoneRSSI = Integer.toString(result.getRssi());
                beaconRSSI = Byte.toString(result.getScanRecord().getManufacturerSpecificData().valueAt(0)[22]);

                switch(beaconRSSI){
                    case "4":
                        firstLevel.add(Double.valueOf(phoneRSSI));
                        break;
                    case "0":
                        secondLevel.add(Double.valueOf(phoneRSSI));
                        break;
                    case "-4":
                        thirdLevel.add(Double.valueOf(phoneRSSI));
                        break;
                    case "-8":
                        fourthLevel.add(Double.valueOf(phoneRSSI));
                        break;
                    case "-12":
                        fifthLevel.add(Double.valueOf(phoneRSSI));
                        break;
                    case "-16":
                        sixthLevel.add(Double.valueOf(phoneRSSI));
                        break;
                    case "-20":
                        seventhLevel.add(Double.valueOf(phoneRSSI));
                        break;
                }

                //dist = Math.exp((Double.parseDouble(phoneRSSI) - getReferenceRSSI(Integer.parseInt(beaconRSSI))) / (-20));
//                kGain = errorEstimate / (errorEstimate + errorMeasurement);
//                valueEstimate = valueEstimate + kGain*(Double.valueOf(phoneRSSI) - valueEstimate);
//                errorEstimate = (1 - kGain) * errorEstimate;

                Map<String, Double> power_levels = new HashMap<>();
                if(firstScan){

                    power_levels.put("4", 0.0);
                    power_levels.put("0", 0.0);
                    power_levels.put("-4", 0.0);
                    power_levels.put("-8", 0.0);
                    power_levels.put("-12", 0.0);
                    power_levels.put("-16", 0.0);
                    power_levels.put("-20", 0.0);


                    //armaMeasurement = Double.valueOf(phoneRSSI);
                    power_levels.remove(beaconRSSI);
                    power_levels.put(beaconRSSI, Double.valueOf(phoneRSSI));
                    firstScan = false;
                }


                double armaPowerLevel = power_levels.get(beaconRSSI);

                valueEstimate = armaFilter(0.1, Double.parseDouble(phoneRSSI));

                filteredRSSIValues.add(valueEstimate);
                rawRSSIValues.add(Double.valueOf(phoneRSSI));

                dist = Math.exp((valueEstimate - getReferenceRSSI(Integer.parseInt(beaconRSSI))) / (-10 * 1.3));
                //dist = (dist * 960) / 4.6;

                distances.add(dist);


                for (int i = 0; i < listDataHeader.size(); i++) {
                    if (btDevice.getAddress().contains(listDataHeader.get(i))) {

                        List<String> child = new ArrayList<String>();


                        child.add("UUID: " + uuid);
                        child.add("Major: " + maj);
                        child.add("Minor: " + min);
                        child.add("Phone RSSI: " + phoneRSSI);
                        child.add("Beacon RSSI: " + beaconRSSI);
                        child.add("Filtered RSSI: " + valueEstimate);
                        child.add("Distance: " + String.valueOf(dist));

                        listDataChild.put(listDataHeader.get(i), child);

                    }
                }


            }

            //For testing purposes
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


            listAdapter.notifyDataSetChanged();


            if (expRet == false && !listDataChild.isEmpty()) {
                expListView.collapseGroup(0);
            } else if (expRet && !listDataChild.isEmpty()) {
                expListView.expandGroup(0);
            }

            if (collectIsEnabled && (value < nrOfSets)) {

                if (scanBegin) {
                    String rD = "------ Distance: " + distance + " | " + "Path: " + path + " ------ \n";
                    scanBegin = false;
                } else {
                    String beaconReceivedData = "BeaconRSSI: " + beaconRSSI + " | " + "PhoneRSSI: " + phoneRSSI + " \n";
                    writeBeaconData(beaconReceivedData);
                    value++;
                }


            } else if (collectIsEnabled && (value == nrOfSets)) {
                mLEScanner.stopScan(mScanCallback);
                value = 0;
                scanBegin = true;
            }


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

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     final byte[] scanRecord) {
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


    private void prepareListData(List<String> bDevice, List<String> bDat) {


        //Adding header & child data

        int start = 0;
        int end = bDevice.size();
        int dEnd = bDevice.size() * 5;

        for (int i = 0; i < dEnd; i = i + 5) {

            if (start == end) {
                break;
            }

            listDataHeader.add(bDevice.get(start));


            List<String> child = new ArrayList<String>();
            child.add("UUID: " + bDat.get(i));
            child.add("Major: " + bDat.get(i + 1));
            child.add("Minor: " + bDat.get(i + 2));
            child.add("Phone RSSI: " + bDat.get(i + 3));
            child.add("Beacon RSSI: " + bDat.get(i + 4));


            listDataChild.put(listDataHeader.get(start), child);

            start++;


        }

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


    public double armaFilter(double smoothingFactor, double measurement){
        armaMeasurement = armaMeasurement - smoothingFactor * (armaMeasurement - measurement);
        return armaMeasurement;
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
                return;
            }
        }
    }


}
