package pum.dolinski_kawka.projectpum;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class GadgetUtil {

    private final static DateFormat format = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);

    public static int parseFrequency(String str) {
        str = str.replaceAll("\\s*[Hh][Zz]", "");
        return Integer.parseInt(str);
    }

    public static int parseDelay(String str) {
        Calendar date = Calendar.getInstance();
        try {
            date.setTime(format.parse(str));
        } catch (ParseException ex) {
            return -1;
        }
        return
                date.get(Calendar.MINUTE) * 60 +
                        date.get(Calendar.SECOND);
    }

    public static int parseLength(String str) {
        Calendar date = Calendar.getInstance();
        try {
            date.setTime(format.parse(str));
        } catch (ParseException ex) {
            return -1;
        }
        return
                date.get(Calendar.HOUR_OF_DAY) * 3600 +
                        date.get(Calendar.MINUTE) * 60 +
                        date.get(Calendar.SECOND);
    }

    public static int delayToByte(int delay) {
        return delay & 0xff;
    }

    public static int delayFromByte(int delay) {
        return delay & 0x00ff;
    }

    public static int[] lengthToByte(int length) {
        int lo = length & 0x00ff;
        int hi = ((length & 0xff00) >> 16) & 0x00ff;
        return new int[]{ lo, hi };
    }

    public static int lengthFromByte(byte[] length) {
        int lo = length[0];
        int hi = length[1];
        return ((hi << 16) & 0xff00) | (lo & 0x00ff);
    }

    public static int lengthFromByte(int[] length) {
        int lo = length[0];
        int hi = length[1];
        return ((hi << 16) & 0xff00) | (lo & 0x00ff);
    }

    public static int freqToByte(int freq) {
        final int SHIFT = 2;
        switch (freq) {
            case 20:
                return 0;
            case 50:
                return 1 << SHIFT;
            case 100:
                return 2 << SHIFT;
            case 200:
                return 3 << SHIFT;
            case 400:
                return 4 << SHIFT;
            case 500:
                return 5 << SHIFT;
            case 700:
                return 6 << SHIFT;
            case 1000:
                return 7 << SHIFT;
            default:
                return 2 << SHIFT;
        }
    }

    public static int freqFromByte(int freq) {
        final int SHIFT = 2;
        final int MASK = 0x1C;
        freq = (freq & MASK) >>> SHIFT;
        switch (freq) {
            case 0:
                return 20;
            case 1:
                return 50;
            case 2:
                return 100;
            case 3:
                return 200;
            case 4:
                return 400;
            case 5:
                return 500;
            case 6:
                return 700;
            case 7:
                return 1000;
            default:
                return -1;
        }
    }

    public static int channelCount(boolean... channels) {
        int cnt = 0;
        for (int i = 0; i < channels.length; i++) {
            if (channels[i])
                cnt++;
        }
        return cnt;
    }

    public static int channelCountToByte(boolean... channels) {
        return channelCountToByte(channelCount(channels));
    }

    public static int channelCountToByte(int cc) {
        final int SHIFT = 5;
        final int MASK = 0xe0;
        cc = cc - 1;
        return ((cc << SHIFT) & MASK);
    }

    public static int channelCountFromByte(int cc) {
        final int SHIFT = 5;
        final int MASK = 0xe0;
        cc = (cc & MASK) >>> SHIFT;
        return cc + 1;
    }

    public static int[] channelConfigurationOffline(boolean... channels) {
        int[] config = new int[6];
        for (int i = 0; i < 6; i++) {
            if (channels[i])
                config[i] = i & 0xff;
            else
                config[i] = 0xff;
        }
        return config;
    }

    public static int[] channelConfigurationOnline(boolean... channels) {
        int count = 0;
        for (int i = 0; i < channels.length; i++)
            if (channels[i])
                count++;
        int[] config = new int[count + 2];
        config[0] = 0x05;
        for (int i = 0, j = 1; i < channels.length; i++) {
            if (channels[i])
                config[j++] = (i & 0xff) | 0x80;
        }
        return config;

    }

    public static int accToByte(int acc) {
        switch (acc) {
            case 6:
                return 0;
            case 12:
                return 1;
            case 24:
                return 2;
            default:
                return -1;
        }
    }

    public static int accFromByte(int acc) {
        final int MASK = 0x03;
        acc = acc & MASK;
        switch (acc) {
            case 0:
                return 6;
            case 1:
                return 12;
            case 2:
                return 24;
            default:
                return -1;
        }
    }

    public static int gyroToByte(int gyro) {
        final int SHIFT = 2;
        switch (gyro) {
            case 250:
                return 0;
            case 500:
                return 1 << SHIFT;
            case 2500:
                return 2 << SHIFT;
            default:
                return -1;
        }
    }

    public static int gyroFromByte(int gyro) {
        final int MASK = 0x0c;
        final int SHIFT = 2;
        gyro = (gyro & MASK) >>> SHIFT;
        switch (gyro) {
            case 0:
                return 250;
            case 1:
                return 500;
            case 2:
                return 2500;
            default:
                return -1;
        }
    }

    public static int[] offlineSettings(int delay, int len, int freq, int acc, int gyro, boolean... channels) {
        int[] settings = new int[13];
        settings[0] = delayToByte(delay);
        settings[1] = lengthToByte(delay)[0];
        settings[2] = lengthToByte(delay)[1];
        settings[3] = channelCountToByte(channels) | freqToByte(freq);
        for (int i = 4, j = 0; i < 10; i++, j++) {
            settings[i] = channelConfigurationOffline(channels)[j];
        }
        settings[11] = 0xff;
        settings[12] = 0xff;
        settings[13] = accToByte(acc) | gyroToByte(gyro);
        return settings;
    }

    public static int[] onlineSettings(int freq, int acc, int gyro, int cc, boolean... channels) {
        int[] settings = new int[2 + cc];
        settings[0] = 0x02 | freqToByte(freq) | channelCountToByte(cc);
        int i = 1;
        for (int j = 0; i < 1 + cc; i++, j++) {
            settings[i] = 0x80 | channelConfigurationOnline(channels)[j];
        }
        settings[i] = accToByte(acc) | gyroToByte(gyro);
        return settings;
    }

    public static int batteryFromByte(int[] bytes) {
        int battery = -1;

        if (bytes.length != 2)
            return battery;

        // TODO map this value correctly (now its only "na oko")
        int state = (bytes[1] << 8) | bytes[0];

        return -60 + state / 25;
    }

    // java is retarded, and byte range is [-128,127] not [0,255]
    static int b2i(byte b) {
        return b & 0xff;
    }
}