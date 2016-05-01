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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

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


@TargetApi(23)
public class MainActivity extends Activity {
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;


    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    private List<String> bDevices = new ArrayList<String>();
    private List<String> bData = new ArrayList<String>();

    private boolean expRet = false;

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

    String[] storagePermissions = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_READ_WRITE_STORAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.listExp);

        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();


        listAdapter = new ExpandableListAdapter(getApplicationContext(), listDataHeader, listDataChild);

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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }

            if ((this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs storage access");
                builder.setMessage("Please grant storage access so this app can log beacon data.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(storagePermissions, PERMISSION_REQUEST_READ_WRITE_STORAGE);
                    }
                });
                builder.show();
            }
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
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        } else if (requestCode == 42) {
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


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {


            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());


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


                for (int i = 0; i < listDataHeader.size(); i++) {
                    if (btDevice.getAddress().contains(listDataHeader.get(i))) {

                        List<String> child = new ArrayList<String>();


                        child.add("UUID: " + uuid);
                        child.add("Major: " + maj);
                        child.add("Minor: " + min);
                        child.add("Phone RSSI: " + phoneRSSI);
                        child.add("Beacon RSSI: " + beaconRSSI);

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




    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Ceva", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
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
