package pum.dolinski_kawka.projectpum;

import java.util.HashMap;
import java.util.Map;

public class OnlineSample {

    public Map<String, Float> values;
    public int id;

    private static int SAMPLE_COUNT = 0;

    public OnlineSample(byte[] data, OnlineSettings settings) {
        id = SAMPLE_COUNT++;

        values = new HashMap<>();

        int[] raw_values = new int[settings.channelCnt];

        for (int i = 0, chnl = 0; i < data.length-2; i += 2, chnl++) {
            while (chnl < 6 && !settings.channelConfig[chnl]) chnl++;

            int H = data[i];
            int L = data[i + 1];

            int value = H << 8 | L;
            String channel = Gadget.CHANNELS[chnl];
            values.put(channel, value / 65535.f);
        }
    }

    @Override
    public String toString() {
        String result = "" + id + " - ";
        for (Map.Entry<String, Float> e : values.entrySet()) {
            result += "[" + e.getKey() + " -> " + e.getValue() + "]";
        }
        return result;
    }

}
