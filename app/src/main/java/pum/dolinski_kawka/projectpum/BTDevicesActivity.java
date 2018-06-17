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

import java.lang.reflect.Method;

// Enables discovering bluetooth devices
// Enables to pick the desired device to connect to
public class BTDevicesActivity extends Activity {

    public static final String EXTRA_BLUETOOTH_DEVICE = "BLUETOOTH_DEVICE";

    // Request to next activity (GadgetMain) to check if device is supported
    private static final int REQUEST_CHECK_SUPPORT = 1;

    Button                btnBTScan;
    ToggleButton          tglBTToggle;
    ListView              lvBTDevices;
    TextView              tvHeader;
    BluetoothAdapter      btAdapter;
    BTDevicesArrayAdapter lvAdapter;


    // region UI_LISTENERS
    final View.OnClickListener            tglBTListener       = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(DEBUG.TAG, "BTDevicesActivity - toggle bluetooth button clicked");
            boolean isBTOn = btAdapter.isEnabled();
            // Turn BT OFF
            if (isBTOn && btAdapter.disable()) {
                Toast.makeText(getApplicationContext(), "Bluetooth disabled", Toast.LENGTH_SHORT).show();
            }
            // Turn BT ON
            else if (!isBTOn && btAdapter.enable()) {
                Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            }
        }
    };
    final View.OnClickListener            btnScanListener     = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(DEBUG.TAG, "BTDevicesActivity - scan button clicked");
            // If scan is running - stop
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
                Toast.makeText(getApplicationContext(), "Scan stopped", Toast.LENGTH_SHORT).show();
            }
            // Scan not running - start
            else {
                boolean isStarted = btAdapter.startDiscovery();
                // Started successfully - clear old devices
                if (isStarted) {
                    Toast.makeText(getApplicationContext(), "Scan started...", Toast.LENGTH_SHORT).show();
                    lvAdapter.clear();
                }
            }
        }
    };
    final AdapterView.OnItemClickListener lvBTDevicesListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.i(DEBUG.TAG, "BTDevicesActivity - list view item clicked");

            // stop discovery if running already
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }

            // get clicked item a.k.a btDevice
            BluetoothDevice btDevice = null;
            try {
                btDevice = (BluetoothDevice) parent.getAdapter().getItem(position);
            } catch (RuntimeException ex) {
                Log.e(DEBUG.TAG, "Error while getting list view item: " + ex);
                return;
            }

            // check if not paired - if so init pairing
            if (!btAdapter.getBondedDevices().contains(btDevice)) {
                initPairBluetoothDevice(btDevice);
                Toast.makeText(getApplicationContext(), "Device need to be paired, pairing...", Toast.LENGTH_LONG).show();
                return;
            }

            // device is paired - start next activity
            startGadgetMainAct(btDevice);
        }
    };
    // endregion UI_LISTENERS

    // region BT_BROADCAST_LISTENERS

    final IntentFilter btBroadcastFilter;

    {
        btBroadcastFilter = new IntentFilter();
        btBroadcastFilter.addAction(BluetoothDevice.ACTION_FOUND);
        btBroadcastFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btBroadcastFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btBroadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    }

    final BroadcastReceiver btBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.i(DEBUG.TAG, "BTDevicesActivity - broadcast received - " + action);

            // processed actions:
            // - new dev found
            // - pairing
            // - bt switched off/on
            switch (action) {
                // new bt device found - add it to list view
                case BluetoothDevice.ACTION_FOUND:
                    final BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    lvAdapter.add(btDevice);
                    break;
                // device pair result
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    // pairing successful
                    if (state == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(getApplicationContext(), "Device paired", Toast.LENGTH_SHORT).show();
                    }
                    // pairing failed
                    else if (state == BluetoothDevice.BOND_NONE) {
                        Toast.makeText(getApplicationContext(), "Device failed to pair", Toast.LENGTH_SHORT).show();
                    }
                    // update list view to reflect current state
                    lvAdapter.notifyDataSetChanged();
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    btAdapter.cancelDiscovery();
                    if (!btAdapter.isEnabled())
                        lvAdapter.clear();
                    break;
            }

            // update UI afterwards
            updateUI();
        }
    };
    // endregion BT_BROADCAST_LISTENERS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        for (int freq : Gadget.FREQUENCIES) {
            for (int cc = 1; cc < 7; cc++) {
                boolean channels[] = new boolean[]{ false, false, false, false, false, false };
                for (int c = 0; c < cc; c++)
                    channels[c] = true;

                Log.e(DEBUG.TAG, new OnlineSettings(freq, 6, 250, channels).toString());
            }
        }
        */

        Log.i(DEBUG.TAG, "BTDevicesActivity - onCreate - start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btdevices);

        // Setup BT adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // no BT adapter - exit (gently)
        if (btAdapter == null) {
            Log.i(DEBUG.TAG, "BTDevicesActivity - onCreate - bluetooth not supported");
            new AlertDialog.Builder(this)
                    .setTitle("Device is not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    }).show();
        }

        // Setup controls
        initControls();

        // Updates list & labels
        updateUI();

        Log.i(DEBUG.TAG, "BTDevicesActivity - onCreate - finish");
    }

    @Override
    protected void onPause() {
        Log.i(DEBUG.TAG, "BTDevicesActivity - onPause - start");

        super.onPause();
        unregisterReceiver(btBroadcastReceiver);
        btAdapter.cancelDiscovery();

        Log.i(DEBUG.TAG, "BTDevicesActivity - onPause - finish");

    }

    @Override
    protected void onResume() {
        Log.i(DEBUG.TAG, "BTDevicesActivity - onResume - start");

        super.onResume();
        // Setup receiver for Bluetooth Events
        registerReceiver(btBroadcastReceiver, btBroadcastFilter);

        Log.i(DEBUG.TAG, "BTDevicesActivity - onResume - finish");
    }

    // Assigns UI elements to members, adds their listeners
    private void initControls() {
        Log.i(DEBUG.TAG, "BTDevicesActivity - initControls - start");

        // Setup adapter which delivers data to the ListView
        lvAdapter = new BTDevicesArrayAdapter(this);

        // Setup toggle Bluetooth On/Off button
        tglBTToggle = findViewById(R.id.tgl_bt_toggle);
        tglBTToggle.setOnClickListener(tglBTListener);

        // Setup scan button
        btnBTScan = findViewById(R.id.btn_bt_devices_scan);
        btnBTScan.setOnClickListener(btnScanListener);

        // Setup ListView of currently visible BT devices
        lvBTDevices = findViewById(R.id.lv_bt_devices);
        lvBTDevices.setAdapter(lvAdapter);
        lvBTDevices.setOnItemClickListener(lvBTDevicesListener);

        // Text on top
        tvHeader = findViewById(R.id.tv_bt_devices_header);

        Log.i(DEBUG.TAG, "BTDevicesActivity - initControls - finish");
    }

    // Updates list & labels
    private void updateUI() {
        Log.i(DEBUG.TAG, "BTDevicesActivity - updateUI - start");

        btnBTScan.setEnabled(btAdapter.isEnabled() && !btAdapter.isDiscovering());

        if (btAdapter.isDiscovering())
            btnBTScan.setText(R.string.btn_bt_devices_scan_running_text);
        else
            btnBTScan.setText(R.string.btn_bt_devices_scan_text);
        tglBTToggle.setChecked(btAdapter.isEnabled());

        // remove all found (if any) devices when BT is disabled
        if (!btAdapter.isEnabled())
            lvAdapter.clear();

        if (lvAdapter.getCount() > 0)
            tvHeader.setText(R.string.tv_bt_devices_header_text_devices_found);
        else
            tvHeader.setText(R.string.tv_bt_devices_header_text_no_devices);

        Log.i(DEBUG.TAG, "BTDevicesActivity - updateUI - finish");
    }

    // Starts next activity
    private void startGadgetMainAct(BluetoothDevice btDevice) {
        Log.i(DEBUG.TAG, "BTDevicesActivity - startGadgetMainAct - start");

        btAdapter.cancelDiscovery();
        Intent startGadgetMainAct = new Intent(BTDevicesActivity.this, GadgetMainActivity.class);
        startGadgetMainAct.putExtra(EXTRA_BLUETOOTH_DEVICE, btDevice);
        startActivityForResult(startGadgetMainAct, BTDevicesActivity.REQUEST_CHECK_SUPPORT);

        Log.i(DEBUG.TAG, "BTDevicesActivity - startGadgetMainAct - finish");
    }

    // Tries to pair given btdevice
    private void initPairBluetoothDevice(BluetoothDevice btDevice) {
        Log.i(DEBUG.TAG, "BTDevicesActivity - initPairBluetoothDevice - start");

        try {
            Method method = btDevice.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(btDevice, (Object[]) null);
        } catch (Exception e) {
        }

        Log.i(DEBUG.TAG, "BTDevicesActivity - initPairBluetoothDevice - start");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(DEBUG.TAG, "BTDevicesActivity - onActivityResult - start");

        if (requestCode == REQUEST_CHECK_SUPPORT && resultCode == GadgetMainActivity.DEVICE_UNSUPPORTED) {
            Toast.makeText(getApplicationContext(), "Unsupported device", Toast.LENGTH_SHORT).show();
        }

        Log.i(DEBUG.TAG, "BTDevicesActivity - onActivityResult - finish");
    }
}
