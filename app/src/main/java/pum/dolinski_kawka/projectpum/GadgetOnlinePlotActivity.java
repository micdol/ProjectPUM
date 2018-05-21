package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class GadgetOnlinePlotActivity extends Activity implements GadgetListener {

    public static final String DEBUG_TAG = "--DEBUG--";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_online_plot);
    }

    @Override
    public void onOnlineRejestrationSampleReceived(OnlineSample sample) {
        Log.i(DEBUG_TAG, sample.toString());
    }

    @Override
    public void onIDReceived(String name, int id) {

    }

    @Override
    public void onBatteryReceived(int state) {

    }


}
