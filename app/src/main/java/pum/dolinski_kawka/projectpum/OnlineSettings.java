package pum.dolinski_kawka.projectpum;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Class holding settings for online registration, that is:
// - channel count
// - frequency (Hz)
// - accelerometer sensitivity (g/s)
// - gyroscope sensitivity (deg/s)
// - channel configuration (6 channels)
// Human readable settings can be converted to writable bytes but NOT the other way around (as there
// is no such use-case). Provides a way to validate given settings and get errors
// Implements Parcelable so can be easily passed between activates (putExtraParcelable)
public class OnlineSettings implements Parcelable {
    int channelCnt, freq, acc, gyro;
    boolean[] channelConfig;
    boolean   toggleByte, startByte;

    public OnlineSettings() {
        this(100, 6, 250, true, true, true, true, true, true);
    }

    public OnlineSettings(int freq, int acc, int gyro, boolean... channelConfig) {
        this.freq = freq;
        this.acc = acc;
        this.gyro = gyro;
        this.channelCnt = 0;
        for (boolean b : channelConfig)
            if (b)
                this.channelCnt++;
        this.channelConfig = channelConfig;
    }

    public boolean areValid() {
        boolean result = Gadget.FREQUENCIES.contains(freq);
        result &= Gadget.ACCELEROMETER_SENSITIVITIES.contains(acc);
        result &= Gadget.GYROSCOPE_SENSITIVITIES.contains(gyro);
        result &= Gadget.CHANNEL_PER_FREQUENCY.get(freq) >= channelCnt;
        result &= !(channelCnt > 6 || channelCnt < 1);
        return result;
    }

    // zero-indexed channel
    public boolean isChannelSet(int idx) {
        try {
            return channelConfig[idx];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return false;
        }
    }

    public List<String> getErrors() {
        List<String> errors = new ArrayList<>();
        if (!Gadget.FREQUENCIES.contains(freq)) {
            errors.add("Unsupported frequency: " + freq);
        }
        if (!Gadget.ACCELEROMETER_SENSITIVITIES.contains(acc)) {
            errors.add("Unsupported accelerometer sens: " + acc);
        }
        if (!Gadget.GYROSCOPE_SENSITIVITIES.contains(gyro)) {
            errors.add("Unsupported gyroscope sens: " + gyro);
        }
        if (Gadget.CHANNEL_PER_FREQUENCY.get(freq) < channelCnt) {
            errors.add("Unsupported channel configuration for this freq: max." + Gadget.CHANNEL_PER_FREQUENCY.get(freq) + " for " + freq + " Hz");
        }
        if (channelCnt > 6 || channelCnt < 1) {
            errors.add("Unsupported channel configuration, channel count: " + channelCnt);
        }
        return errors;
    }

    public void setToggleByte(boolean isSet) {
        toggleByte = isSet;
    }

    public void setStartByte(boolean isSet) {
        startByte = isSet;
    }

    public int[] toByteArray() {
        final int BYTE_COUNT = 0
                + (toggleByte ? 1 : 0)
                + 1 // one byte to encode frequency and channel count
                + channelCnt // one byte for each channel
                + 1 // one byte to encode accelerometer and gyroscope
                + (startByte ? 1 : 0);

        int[] result = new int[BYTE_COUNT];

        int currentIdx = 0;
        if (toggleByte)
            result[currentIdx++] = 0x05;

        result[currentIdx++] = 0x02 | GadgetUtil.freqToByte(freq) | GadgetUtil.channelCountToByte(channelCnt);

        for (int channel = 0; channel < 6; channel++) {
            if (channelConfig[channel]) {
                result[currentIdx++] = 0x80 | channel;
            }
        }

        result[currentIdx++] = GadgetUtil.accToByte(acc) | GadgetUtil.gyroToByte(gyro);

        if (startByte)
            result[currentIdx++] = 0xda;

        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Online settings: \n");
        sb.append("Frequency: " + freq + "\n");
        sb.append("Channel count: " + channelCnt + "\n");
        sb.append("Channel config: " + Arrays.toString(channelConfig) + "\n");
        sb.append("Accelerometer sens: " + acc + "\n");
        sb.append("Gyroscope sens: " + gyro + "\n");
        sb.append("Byte settings: " + Arrays.toString(toByteArray()));
        return sb.toString();
    }

    // region PARCELABLE_IMPL
    public static final Creator<OnlineSettings> CREATOR = new Creator<OnlineSettings>() {
        @Override
        public OnlineSettings createFromParcel(Parcel in) {
            return new OnlineSettings(in);
        }

        @Override
        public OnlineSettings[] newArray(int size) {
            return new OnlineSettings[size];
        }
    };

    protected OnlineSettings(Parcel in) {
        freq = in.readInt();
        channelCnt = in.readInt();
        acc = in.readInt();
        gyro = in.readInt();
        channelConfig = in.createBooleanArray();
        toggleByte = in.readByte() != 0;
        startByte = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(freq);
        dest.writeInt(channelCnt);
        dest.writeInt(acc);
        dest.writeInt(gyro);
        dest.writeBooleanArray(channelConfig);
        dest.writeByte((byte) (toggleByte ? 1 : 0));
        dest.writeByte((byte) (startByte ? 1 : 0));
    }
    // endregion PARCELABLE_IMPL
}
