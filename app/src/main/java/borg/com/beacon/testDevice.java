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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;


/**
 * Created by Borg on 8/26/2015.
 */
public class testDevice extends Activity {

    private CheckBox chkBox;
    private Spinner distSpin;
    private Spinner pathSpin;
    private Spinner setsSpin;
    private Button okBtn;
    private Button cnlBtn;

    private boolean data;
    private String dist;
    private String path;
    private String set;

    private boolean isFirstTimeOpened;
    private String receivedPath;
    private String receivedDistance;
    private String receivedSets;
    private boolean check;

    String[] paths = {"None", "Concrete", "Water", "Direct"};
    String[] distance = {"None", "1m", "3m", "4m"};
    String[] sets = {"None", "10", "20", "30", "40", "50"};



    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_device);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.x = 0;
        params.height = 700;
        params.width = 500;
        params.y = 40;
        this.getWindow().setAttributes(params);

        Intent i = getIntent();
        isFirstTimeOpened = i.getBooleanExtra("first time", true);

        if (!isFirstTimeOpened) {
            check = i.getBooleanExtra("check", true);
            receivedDistance = i.getStringExtra("dist");
            receivedPath = i.getStringExtra("path");
            receivedSets = i.getStringExtra("set");
        }


        setupCheckbox();
        setupOKBtn();
        setupCnlBtn();
        setupPathSpinner();
        setupDistSpinner();
        setupSetsSpinner();


    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setupCheckbox() {
        chkBox = (CheckBox) findViewById(R.id.checkBox);

        if (isFirstTimeOpened) {
            chkBox.setChecked(false);
            data = chkBox.isChecked();
        } else {
            chkBox.setChecked(check);
            data = chkBox.isChecked();
        }

        chkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    data = isChecked;
                    pathSpin.setEnabled(true);
                    distSpin.setEnabled(true);
                    setsSpin.setEnabled(true);

                } else if (!isChecked) {
                    data = isChecked;
                    pathSpin.setEnabled(false);
                    distSpin.setEnabled(false);
                    setsSpin.setEnabled(false);
                }
            }
        });
    }

    private void setupOKBtn() {
        okBtn = (Button) findViewById(R.id.setBtn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                if (data && isFirstTimeOpened) {
                    Toast.makeText(getApplicationContext(), "Data will be saved in the 'Download' directory", Toast.LENGTH_LONG).show();
                }

                Intent intent = new Intent();
                intent.putExtra("set", data);
                intent.putExtra("path", path);
                intent.putExtra("dist", dist);
                intent.putExtra("sets", set);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    private void setupCnlBtn() {
        cnlBtn = (Button) findViewById(R.id.cnlBtn);
        cnlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
            }
        });
    }

    private void setupPathSpinner() {
        ArrayAdapter<String> pathAdapt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, paths);
        pathSpin = (Spinner) findViewById(R.id.pathSpin);

        if (chkBox.isChecked()) {
            pathSpin.setEnabled(true);
        } else {
            pathSpin.setEnabled(false);
        }

        pathSpin.setAdapter(pathAdapt);

        if (isFirstTimeOpened) {
            pathSpin.setSelection(0);
        } else {
            for (int i = 0; i < paths.length; i++) {
                if (paths[i].equals(receivedPath)) {
                    pathSpin.setSelection(i);
                    break;
                }
            }
        }

        pathSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        path = paths[0];
                        break;
                    case 1:
                        path = paths[1];
                        break;
                    case 2:
                        path = paths[2];
                        break;
                    case 3:
                        path = paths[3];
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getApplicationContext(), "Nothing selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDistSpinner() {
        ArrayAdapter<String> distAdapt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, distance);
        distSpin = (Spinner) findViewById(R.id.distSpin);

        if (chkBox.isChecked()) {
            distSpin.setEnabled(true);
        } else {
            distSpin.setEnabled(false);
        }

        distSpin.setAdapter(distAdapt);

        if (isFirstTimeOpened) {
            distSpin.setSelection(0);
        } else {
            for (int i = 0; i < distance.length; i++) {
                if (distance[i].equals(receivedDistance)) {
                    distSpin.setSelection(i);
                    break;
                }
            }
        }


        distSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        dist = distance[0];
                        break;
                    case 1:
                        dist = distance[1];
                        break;
                    case 2:
                        dist = distance[2];
                        break;
                    case 3:
                        dist = distance[3];
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupSetsSpinner() {
        ArrayAdapter<String> setsAdapt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sets);
        setsSpin = (Spinner) findViewById(R.id.setsSpin);

        if (chkBox.isChecked()) {
            setsSpin.setEnabled(true);
        } else {
            setsSpin.setEnabled(false);
        }

        setsSpin.setAdapter(setsAdapt);

        if (isFirstTimeOpened) {
            setsSpin.setSelection(0);
        } else {
            for (int i = 0; i < sets.length; i++) {
                if (sets[i].equals(receivedSets)) {
                    setsSpin.setSelection(i);
                    break;
                }
            }
        }


        setsSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        set = sets[0];
                        break;
                    case 1:
                        set = sets[1];
                        break;
                    case 2:
                        set = sets[2];
                        break;
                    case 3:
                        set = sets[3];
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

}
