package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

public class GadgetOnlineSettingsActivity extends Activity {

    Gadget gadget;
    Spinner spnrFrequency;
    CheckBox cbAX, cbAY, cbAZ, cbGR, cbGP, cbGY;
    RadioButton rbAcc6, rbAcc12, rbAcc24, rbGyro250, rbGyro500, rbGyro2500;
    Button btnStartOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_online_settings);

        initControls();

        Intent intent = getIntent();
        BluetoothDevice btDevice = intent.getParcelableExtra(BTDevicesActivity.EXTRA_BLUETOOTH_DEVICE);

        gadget = new Gadget(btDevice);

    }

    void initControls() {
        spnrFrequency = findViewById(R.id.spnr_frequency);
        cbAX = findViewById(R.id.cb_channel_config_ax);
        cbAY = findViewById(R.id.cb_channel_config_ay);
        cbAZ = findViewById(R.id.cb_channel_config_az);
        cbGR = findViewById(R.id.cb_channel_config_gr);
        cbGY = findViewById(R.id.cb_channel_config_gy);
        cbGP = findViewById(R.id.cb_channel_config_gp);
        rbAcc6 = findViewById(R.id.rb_accel_sens_6);
        rbAcc12 = findViewById(R.id.rb_accel_sens_12);
        rbAcc24 = findViewById(R.id.rb_accel_sens_24);
        rbGyro250 = findViewById(R.id.rb_gyro_sens_250);
        rbGyro500 = findViewById(R.id.rb_gyro_sens_500);
        rbGyro2500 = findViewById(R.id.rb_gyro_sens_2500);
        btnStartOnline = findViewById(R.id.btn_online_start);

        btnStartOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String freqStr = (String) spnrFrequency.getSelectedItem();
                int freq = Gadget.Util.parseFrequency(freqStr);

                int acc_sens = 6;
                if (rbAcc12.isChecked()) acc_sens = 12;
                else if (rbAcc24.isChecked()) acc_sens = 24;

                int gyro_sens = 250;
                if (rbGyro500.isChecked()) gyro_sens = 500;
                else if (rbGyro2500.isChecked()) gyro_sens = 2500;

                OnlineSettings settings = new OnlineSettings(freq, acc_sens, gyro_sens, cbAX.isChecked(), cbAY.isChecked(), cbAZ.isChecked(), cbGR.isChecked(), cbGP.isChecked(), cbGY.isChecked());
                if (!settings.areValid()) {
                    String msg = "Errors:\n";
                    for (String error : settings.getErrors()) {
                        msg += error + "\n";
                    }
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                } else {
                    gadget.startOnlineRejestration(settings);
                    Intent initGadget = new Intent(GadgetOnlineSettingsActivity.this, GadgetOnlinePlotActivity.class);
                    initGadget.putExtra(BTDevicesActivity.EXTRA_BLUETOOTH_DEVICE, gadget.getBtDevice());
                    //startActivity(initGadget);
                }
            }

        });
    }
}
