package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class BTDevicesActivity extends Activity {

    private static final String DEBUG_TAG = "--DEBUG--";

    public static final String EXTRA_BLUETOOTH_DEVICE = "BLUETOOTH_DEVICE";

    Button btnBTScan;
    ToggleButton tglBTToggle;
    ListView lvBTDevices;
    TextView tvHeader;
    BluetoothAdapter btAdapter;
    BTDevicesArrayAdapter lvAdapter;
    BroadcastReceiver broadcastReceiver;
    IntentFilter broadcastFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btdevices);

        // Setup BT adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // no BT adapter - exit (gently)
        if (btAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Device is not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .show();
        }

        // Setup controls
        initControls();

        // Setup receiver for Bluetooth Events
        initReceivers();

        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ex) {
            Log.i(DEBUG_TAG, "Weird.. Receiver is not registered...");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, broadcastFilter);
    }

    /**
     * Assigns UI elements to members
     * Adds their listeners
     */
    private void initControls() {
        // Setup toggle Bluetooth On/Off button
        tglBTToggle = findViewById(R.id.tgl_bt_toggle);
        tglBTToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btAdapter.isEnabled() && btAdapter.disable()) {
                    Log.i(DEBUG_TAG, "tglBTToggle - bluetooth disabled");
                } else if (!btAdapter.isEnabled() && btAdapter.enable()) {
                    Log.i(DEBUG_TAG, "tglBTToggle - bluetooth enabled");
                }
            }
        });

        // Setup scan button
        btnBTScan = findViewById(R.id.btn_bt_devices_scan);
        btnBTScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // (Re)start discovery (which will last +-12 seconds) and clears currently visible list
                Log.i(DEBUG_TAG, "btnBTScan - starting discovery");
                if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
                btAdapter.startDiscovery();
                lvAdapter.clear();
            }
        });

        // Setup adapter which delivers data to the ListView
        lvAdapter = new BTDevicesArrayAdapter(this);

        // Setup ListView of currently visible BT devices
        lvBTDevices = findViewById(R.id.lv_bt_devices);
        lvBTDevices.setAdapter(lvAdapter);
        lvBTDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO - except for canceling discovery this should initiate gadget connection as well
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }

                BluetoothDevice btDevice = (BluetoothDevice) parent.getAdapter().getItem(position);
                Intent initGadget = new Intent(BTDevicesActivity.this, GadgetMainActivity.class);
                initGadget.putExtra(EXTRA_BLUETOOTH_DEVICE, btDevice);
                startActivity(initGadget);
            }
        });

        // Text on top
        tvHeader = findViewById(R.id.tv_bt_devices_header);
    }

    /**
     *
     */
    private void initReceivers() {
        // Create if null only
        if (broadcastFilter == null) {
            // Register receivers for some actions, this way we can for instance capture BT changes
            // which originate not only from this app requests but as well from user actions in OS
            broadcastFilter = new IntentFilter();
            broadcastFilter.addAction(BluetoothDevice.ACTION_FOUND);
            broadcastFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            broadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            broadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        }

        // Create if null only
        if (broadcastReceiver == null) {
            // Manage received events, saved in var so that in onPause/Resume we can un/re-register
            // it again
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    switch (action) {
                        case BluetoothDevice.ACTION_FOUND:
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            Log.i(DEBUG_TAG, "found device: " + device.getName() + " " + device.getAddress());
                            lvAdapter.add(device);
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                            Log.i(DEBUG_TAG, "Starting discovery...");
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                            Log.i(DEBUG_TAG, "Discovery finished");
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Scan finished",
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                    updateUI();
                }
            };
        }

        registerReceiver(broadcastReceiver, broadcastFilter);
    }

    private void updateUI() {
        btnBTScan.setEnabled(btAdapter.isEnabled() && !btAdapter.isDiscovering());

        if (btAdapter.isDiscovering()) btnBTScan.setText(R.string.btn_bt_devices_scan_running_text);
        else btnBTScan.setText(R.string.btn_bt_devices_scan_text);
        tglBTToggle.setChecked(btAdapter.isEnabled());

        // remove all found (if any) devices when BT is disabled
        if (!btAdapter.isEnabled()) lvAdapter.clear();

        if (lvAdapter.getCount() > 0)
            tvHeader.setText(R.string.tv_bt_devices_header_text_devices_found);
        else tvHeader.setText(R.string.tv_bt_devices_header_text_no_devices);

    }
}
