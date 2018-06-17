package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class GadgetOnlinePlotActivity extends Activity implements GadgetListener {

    private Button btnOnlineStop;

    // region UI_LISTENERS
    View.OnClickListener btnOnlineStopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Gadget.getInstance().stopOnlineRegistration();
        }
    };
    // endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_online_plot);

        initControls();

        Gadget.getInstance().addListener(this);
    }

    private void initControls() {
        btnOnlineStop = findViewById(R.id.btn_online_stop);
        btnOnlineStop.setOnClickListener(btnOnlineStopListener);
    }

    // region GADGET_LISTENER_IMPL

    @Override
    public void onOnlineRegistrationSampleReceived(OnlineSample sample) {
        Log.i(DEBUG.TAG, sample.toString());
    }

    // endregion GADGET_LISTENER_IMPL

}
