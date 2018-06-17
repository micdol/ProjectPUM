package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import static pum.dolinski_kawka.projectpum.GadgetOnlineSettingsActivity.EXTRA_SETTINGS;

public class GadgetOnlinePlotActivity extends Activity implements GadgetListener {

    private Button    btnOnlineStop;
    private GraphView graph;
    private              int    colorId         = -1;
    private              double graphLastXValue = -1d;
    private              int    frequency       = 1;
    private              int    numOfPoints     = 1000;
    private static final int    DEFAULT_X_RANGE = 10 /*seconds*/;
    public final static  int[]  colorPalette    = new int[]{
            Color.BLUE, Color.GREEN, Color.RED, Color.BLACK, Color.YELLOW, Color.MAGENTA };


    // TODO Add saving data to list
    // private ArrayList<ArrayList<Float>> channelsData;
    private ArrayList<LineGraphSeries<DataPoint>> Series = new ArrayList<>();

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
        graphLastXValue = -1d;
        initControls();

        Gadget.getInstance().addListener(this);

    }



    private void initControls() {
        graph = findViewById(R.id.graph);

        btnOnlineStop = findViewById(R.id.btn_online_stop);
        btnOnlineStop.setOnClickListener(btnOnlineStopListener);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.i(DEBUG.TAG, "No extras sent to Intent");
            return;
        }

        OnlineSettings settings = extras.getParcelable(EXTRA_SETTINGS);
        if (settings != null) {
            initGraph(settings);
            frequency = settings.freq;
            numOfPoints = DEFAULT_X_RANGE * frequency;
        } else {
            Log.i(DEBUG.TAG, "Invalid settings object");
        }
    }

    private void initGraph(OnlineSettings settings) {
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(DEFAULT_X_RANGE);
        graph.getViewport().setMaxY(1);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setScalable(true);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Value");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time [s]");
        for (int channel = 0; channel < 6; channel++) {
            if (settings.channelConfig[channel]) {
                colorId++;
                LineGraphSeries<DataPoint> mSeries = new LineGraphSeries<>();
                mSeries.setTitle(Gadget.CHANNELS[channel]);
                mSeries.setColor(colorPalette[colorId]);
                mSeries.setDrawDataPoints(true);
                mSeries.setDataPointsRadius(5);
                mSeries.setThickness(3);
                Series.add(mSeries);
                graph.addSeries(Series.get(colorId));
            }
        }
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graph.getLegendRenderer().setMargin(100);
    }

    // region GADGET_LISTENER_IMPL

    @Override
    public void onOnlineRegistrationSampleReceived(OnlineSample sample) {

        graph.post(new Runnable() {
            @Override
            public void run() {
                if (sample.isValid) {
                    graphLastXValue += 1d;

                    for (int channel = 0; channel < Series.size(); channel++) {
                        LineGraphSeries lgs = Series.get(channel);
                        if (lgs != null) {
                            double x = graphLastXValue / frequency;
                            double y = sample.channelValues[channel];
                            DataPoint dp = new DataPoint(x, y);
                            lgs.appendData(dp, true, numOfPoints, false);
                        }
                    }
                }
                graph.getViewport().setMaxY(1);
                graph.getViewport().setMinY(-1);
            }
        });
    }
    // endregion GADGET_LISTENER_IMPL

}