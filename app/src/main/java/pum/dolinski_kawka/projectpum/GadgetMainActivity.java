package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GadgetMainActivity extends Activity implements GadgetListener {

    // Result this activity returns if Bluetooth Device passed is not supported
    public static final int DEVICE_UNSUPPORTED  = -1;
    public static final int DEVICE_DISCONNECTED = -2;

    private Gadget      gadget;
    private Button      btnOnline;
    private TextView    tvID;
    private ProgressBar pbBattery;
    private boolean isDeviceOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onCreate - start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_main);

        initControls();

        Intent intent = getIntent();
        BluetoothDevice btDevice = intent.getParcelableExtra(BTDevicesActivity.EXTRA_BLUETOOTH_DEVICE);

        gadget = Gadget.getInstance();
        gadget.setBluetoothDevice(btDevice);
        gadget.addListener(this);
        gadget.test();



        Log.i(DEBUG.TAG, "GadgetMainActivity - onCreate - finish");
    }

    @Override
    protected void onStart() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onStart - " + this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onResume - " + this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onPause - " + this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onStop - " + this);
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onRestart - " + this);
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onDestroy - " + this);
        super.onDestroy();
        gadget.removeListener(this);
    }

    private void initControls() {
        Log.i(DEBUG.TAG, "GadgetMainActivity - initControls - start");

        btnOnline = findViewById(R.id.btn_online);
        btnOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(DEBUG.TAG, "GadgetMainActivity - button online - clicked");

                Intent initGadget = new Intent(GadgetMainActivity.this, GadgetOnlineSettingsActivity.class);
                startActivity(initGadget);
            }
        });

        tvID = findViewById(R.id.tv_id_value);
        pbBattery = findViewById(R.id.pb_battery);

        Log.i(DEBUG.TAG, "GadgetMainActivity - initControls - finish");
    }

    @Override
    public void onTestReceived(boolean isDevOk) {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onTestReceived - " + isDevOk);

        // device is indeed supported - get battery and id
        if (isDevOk && !isDeviceOk) {
            isDeviceOk = true;
            gadget.getID();
            gadget.getBattery();
            // enable online rejestartion after some time so id and battery can be safely retrieved
            btnOnline.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnOnline.setEnabled(true);
                }
            }, 3000);
        }
        // unsupported device - exit with proper code
        else {
            setResult(GadgetMainActivity.DEVICE_UNSUPPORTED);
            finish();
        }
    }

    // region GADGET_LISTENER_IMPL
    @Override
    public void onIDReceived(final String name, final int id) {
        Log.i(DEBUG.TAG, "GadgetMainActivity " + this + " - onIDReceived - " + name + " " + id);

        tvID.post(new Runnable() {
            @Override
            public void run() {
                tvID.setText(name + "(" + id + ")");
            }
        });
    }

    @Override
    public void onBatteryReceived(final int state) {
        Log.i(DEBUG.TAG, "GadgetMainActivity - onBatteryReceived - " + state);

        pbBattery.post(new Runnable() {
            @Override
            public void run() {
                pbBattery.setProgress(state);
            }
        });
    }

    // endregion GADGET_LISTENER_IMPL

}
