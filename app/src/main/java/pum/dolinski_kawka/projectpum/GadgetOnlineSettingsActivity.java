package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Arrays;

public class GadgetOnlineSettingsActivity extends Activity {

    public static final String EXTRA_SETTINGS = "EXTRA_SETTINGS";

    private Spinner  spnrFrequency;
    private CheckBox cbAX, cbAY, cbAZ, cbGR, cbGP, cbGY;
    private RadioButton rbAcc6, rbAcc12, rbAcc24, rbGyro250, rbGyro500, rbGyro2500;
    private Button btnStartOnline;

    // region UI_LISTENERS
    // starts online rejestration and next activity
    View.OnClickListener btnStartListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(DEBUG.TAG, "Start clicked");

            // parse frequency
            String freqStr = (String) spnrFrequency.getSelectedItem();
            int freq = GadgetUtil.parseFrequency(freqStr);

            // parse accelerometer sensitivity
            int accSens = -1;
            if (rbAcc6.isChecked())
                accSens = 6;
            else if (rbAcc12.isChecked())
                accSens = 12;
            else if (rbAcc24.isChecked())
                accSens = 24;

            // parse gyroscope sensitivity
            int gyroSens = -1;
            if (rbGyro250.isChecked())
                gyroSens = 250;
            else if (rbGyro500.isChecked())
                gyroSens = 500;
            else if (rbGyro2500.isChecked())
                gyroSens = 2500;

            // parset channel configuration
            boolean channelConfig[] = new boolean[]{
                    cbAX.isChecked(),
                    cbAY.isChecked(),
                    cbAZ.isChecked(),
                    cbGR.isChecked(),
                    cbGP.isChecked(),
                    cbGY.isChecked()
            };

            Log.i(DEBUG.TAG, "Settings from UI");
            Log.i(DEBUG.TAG, "Freq:  " + freq);
            Log.i(DEBUG.TAG, "Accel: " + accSens);
            Log.i(DEBUG.TAG, "Gyro:  " + gyroSens);
            Log.i(DEBUG.TAG, "Chnls: " + Arrays.toString(channelConfig));

            // construct settings
            OnlineSettings settings = new OnlineSettings(freq, accSens, gyroSens, channelConfig);

            // validate
            boolean settingsValid = settings.areValid();

            // not valid - show what the errors are
            if (!settingsValid) {
                // Concat all errors, each in new line
                StringBuilder sb = new StringBuilder("Invalid settings: \n");
                for (String error : settings.getErrors()) {
                    sb.append("- ");
                    sb.append(error);
                    sb.append(" \n");
                }
                String message = sb.toString();
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                // break here - user has to fix the settings
                return;
            }

            Log.i(DEBUG.TAG, settings.toString());

            // settings valid - start registration and start plot activity (pass settings further)
            Intent plotActivity = new Intent(GadgetOnlineSettingsActivity.this, GadgetOnlinePlotActivity.class);
            plotActivity.putExtra(EXTRA_SETTINGS, settings);
            Gadget.getInstance().startOnlineRegistration(settings);
            startActivity(plotActivity);
        }
    };
    // endregion UI_LISTENERS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_online_settings);
        initControls();
    }

    void initControls() {
        // map xml to objects
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

        // listeners
        btnStartOnline.setOnClickListener(btnStartListener);
    }
}
