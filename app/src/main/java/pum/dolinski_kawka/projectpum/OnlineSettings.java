package pum.dolinski_kawka.projectpum;

import java.util.ArrayList;
import java.util.List;

public class OnlineSettings {
    int channelCnt, freq, acc, gyro;
    boolean[] channelConfig;
    boolean toggleByte, startByte;

    public OnlineSettings() {
        this(100, 6, 250, true, true, true, true, true, true);
    }

    public OnlineSettings(int freq, int acc, int gyro, boolean... channelConfig) {
        this.freq = freq;
        this.acc = acc;
        this.gyro = gyro;
        this.channelCnt = 0;
        for (boolean b : channelConfig) if (b) this.channelCnt++;
        this.channelConfig = channelConfig;
    }

    public boolean areValid() {
        boolean result = Gadget.AVAILABLE_FREQUENCIES.contains(freq);
        result &= Gadget.AVAILABLE_ACCEL_SENS.contains(acc);
        result &= Gadget.AVAILABLE_GYRO_SENS.contains(gyro);
        result &= Gadget.CHANNEL_PER_FREQUENCY.get(freq) >= channelCnt;
        result &= !(channelCnt <= 6 && channelCnt >= 1);
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
        if (!Gadget.AVAILABLE_FREQUENCIES.contains(freq)) {
            errors.add("Unsupported frequency: " + freq);
        }
        if (!Gadget.AVAILABLE_ACCEL_SENS.contains(acc)) {
            errors.add("Unsupported accelerometer sens: " + acc);
        }
        if (!Gadget.AVAILABLE_GYRO_SENS.contains(gyro)) {
            errors.add("Unsupported gyroscope sens: " + gyro);
        }
        if (Gadget.CHANNEL_PER_FREQUENCY.get(freq) < channelCnt) {
            errors.add("Unsupported channel configuration for this freq: max." + Gadget.CHANNEL_PER_FREQUENCY.get(freq) + " for " + freq + " Hz");
        }
        if (channelCnt <= 6 && channelCnt >= 1) {
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

    public byte[] toByteArray() {
        int[] settings = Gadget.Util.onlineSettings(freq, acc, gyro, channelConfig);
        byte[] result = new byte[(toggleByte ? 1 : 0) + (startByte ? 1 : 0) + settings.length];
        if (toggleByte) result[0] = 0x05;
        for (int srcIdx = 0, dstIdx = toggleByte ? 1 : 0; srcIdx < settings.length; srcIdx++, dstIdx++) {
            result[dstIdx] = (byte) (settings[srcIdx] & 0x000000ff);
        }
        if (startByte) result[result.length - 1] = (byte) 0xda;
        return result;
    }
}
