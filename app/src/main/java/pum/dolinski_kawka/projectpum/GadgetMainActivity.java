package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GadgetMainActivity extends Activity implements GadgetListener {

    private static final String DEBUG_TAG = "--DEBUG--";

    private Gadget gadget;
    private Button btnOnline;
    private TextView tvID;
    private ProgressBar pbBattery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_main);

        initControls();

        Intent intent = getIntent();
        BluetoothDevice btDevice = intent.getParcelableExtra(BTDevicesActivity.EXTRA_BLUETOOTH_DEVICE);

        gadget = new Gadget(btDevice);
        gadget.addListener(this);

        gadget.getID();
        gadget.getBattery();

    }

    private void initControls() {
        btnOnline = findViewById(R.id.btn_online);
        btnOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent initGadget = new Intent(GadgetMainActivity.this, GadgetOnlineSettingsActivity.class);
                initGadget.putExtra(BTDevicesActivity.EXTRA_BLUETOOTH_DEVICE, gadget.getBtDevice());
                startActivity(initGadget);
            }
        });

        tvID = findViewById(R.id.tv_id_value);
        pbBattery = findViewById(R.id.pb_battery);
    }

    @Override
    public void onIDReceived(final String name, final int id) {
        tvID.post(new Runnable() {
            @Override
            public void run() {
                tvID.setText(name + "(" + id + ")");
            }
        });
    }

    @Override
    public void onBatteryReceived(final int state) {
        pbBattery.post(new Runnable() {
            @Override
            public void run() {
                pbBattery.setProgress(state);
            }
        });
    }

    // Not used... default interface impl from api 24
    @Override
    public void onOnlineRejestrationSampleReceived(OnlineSample sample) {

    }
}
