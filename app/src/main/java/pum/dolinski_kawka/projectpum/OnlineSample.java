package pum.dolinski_kawka.projectpum;

import java.util.Arrays;

// Class holding single sample data received while online registration is running.
// This class does not provide any information about configuration/settings applied only
// holds channel values.
// Provides a way to determine if sample has correct data.
public class OnlineSample {
    // end of sample signature [0xC3, 0x3C]
    public static final byte[] END_OF_SAMPLE = new byte[]{ (byte) 0x3C, (byte) 0xC3 };

    private static int ID = -1;
    public final boolean isValid;
    public final float[] channelValues;

    // data - bytes read from device
    // settings - for conversion purposes (eg. what was the gyroscope sensitivity etc.)
    public OnlineSample(byte[] data, final OnlineSettings settings) {
        ID++;

        // each chunk contains
        // - 2 bytes per channel (first is HIGH, second is LOW)
        // - 2 bytes as a end-of-sample indicator
        int channelCnt = (data.length - 2) / 2;

        // make array with enough space
        channelValues = new float[channelCnt];

        // parse data
        for (int i = 0; i < channelCnt; i++) {
            int H = data[2 * i];
            int L = data[2 * i + 1];
            int V = ((H << 8) & 0x0000ff00) | (L & 0x000000ff);

            // TODO - scaling of values according to channel (so far all scaled from 0 to 1)
            channelValues[i] = 1.0f * V / 65535.0f;
        }

        // check if valid
        // last two bytes has to be equal to end-of-sample signature
        int from = data.length - 2;
        int to = data.length;
        isValid = Arrays.equals(END_OF_SAMPLE, Arrays.copyOfRange(data, from, to));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(ID + " - OnlineSample [");
        for (float f : channelValues) {
            sb.append(f + " ");
        }
        sb.append("] - valid: ");
        sb.append(isValid);
        return sb.toString();
    }

}
