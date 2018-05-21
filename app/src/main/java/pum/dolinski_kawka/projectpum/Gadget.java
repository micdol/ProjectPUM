package pum.dolinski_kawka.projectpum;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// GADGET HAS TO BE PAIRED IN ORDER FOR IT TO WORK
public class Gadget {
    private final static String DEBUG_TAG = "--DEBUG--";

    // publicly available arams restrictions
    public final static List<Integer> AVAILABLE_FREQUENCIES;
    public final static List<Integer> AVAILABLE_GYRO_SENS;
    public final static List<Integer> AVAILABLE_ACCEL_SENS;
    public final static Map<Integer, Integer> CHANNEL_PER_FREQUENCY;
    public final static String[] CHANNELS;

    // static initalizer block (init static public constants)
    static {
        AVAILABLE_FREQUENCIES = Arrays.asList(20, 50, 100, 200, 400, 500, 700, 1000);
        AVAILABLE_GYRO_SENS = Arrays.asList(250, 500, 2500);
        AVAILABLE_ACCEL_SENS = Arrays.asList(6, 12, 24);
        CHANNEL_PER_FREQUENCY = new Supplier<Map<Integer, Integer>>() {
            @Override
            public Map<Integer, Integer> get() {
                Map<Integer, Integer> m = new LinkedHashMap<>();
                m.put(20, 6);
                m.put(50, 6);
                m.put(100, 6);
                m.put(200, 6);
                m.put(400, 6);
                m.put(500, 6);
                m.put(700, 4);
                m.put(1000, 2);
                return m;
            }
        }.get();
        CHANNELS = new String[]{"AX", "AY", "AZ", "GR", "GP", "GY"};
    }

    private final Runnable tasksThread = new Runnable() {
        final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        Future subtask = null;

        @Override
        public void run() {
            Log.i(DEBUG_TAG, "Starting ioTask...");
            while (true) {
                // Subtask was scheduled but finished
                if (subtask != null && subtask.isDone()) {
                    Log.i(DEBUG_TAG, "Subtask finished");
                    subtask = null;
                    continue;
                }

                // No commands to process OR task is being processed - wait...
                if (taskQueue.isEmpty() || (subtask != null && !subtask.isDone())) continue;

                // Take first subtask
                Runnable task = null;
                try {
                    task = taskQueue.take();
                } catch (InterruptedException ex) {
                    Log.e(DEBUG_TAG, "Error while taking next task to run: " + ex);
                    continue;
                }

                subtask = taskExecutor.submit(task);
                Log.i(DEBUG_TAG, "New subtask submitted");
            }
        }
    };

    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private BlockingQueue<Runnable> taskQueue;
    private List<GadgetListener> listeners;
    private ExecutorService tasksExecutor;
    private Integer stopOnlineHash;
    private OnlineSettings lastKnownSettings;

    public Gadget(BluetoothDevice btDevice) {
        this.btDevice = btDevice;
        btSocket = null;
        stopOnlineHash = null;
        taskQueue = new LinkedBlockingDeque<>();
        listeners = new ArrayList<>();
        tasksExecutor = Executors.newSingleThreadExecutor();
        tasksExecutor.submit(tasksThread);
    }

    public void addListener(GadgetListener l) {
        listeners.add(l);
    }

    public void removeListener(GadgetListener l) {
        listeners.remove(l);
    }

    public void getID() {
        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                Log.i(DEBUG_TAG, "Starting getID task");
                byte[] result = null;
                try {
                    connect();
                    write(2);
                    result = read();
                    int len = result.length;
                    String name = new String(Arrays.copyOfRange(result, 0, len - 1));
                    int id = result[len - 1];
                    for (GadgetListener gl : listeners) gl.onIDReceived(name, id);
                    disconnect();
                } catch (IOException ex) {
                    Log.e(DEBUG_TAG, "getID failed: " + ex);
                }
                Log.i(DEBUG_TAG, "getID task finished");
            }
        });
    }

    public void setID(final int id) {
        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                try {
                    connect();
                    byte[] toWrite = new byte[]{(byte) 3, (byte) id};
                    write(toWrite);
                    disconnect();
                } catch (IOException ex) {
                    Log.e(DEBUG_TAG, "setID failed: " + ex);
                }
            }
        });
    }

    public void startOnlineRejestration(OnlineSettings settings) {
        settings.setToggleByte(true);
        settings.setStartByte(true);
        lastKnownSettings = settings;
        final byte[] toWrite = settings.toByteArray();
        final int SAMPLE_SIZE = settings.channelCnt * 2 + 2;
        taskQueue.add(new Runnable() {
            boolean firstSampleRead = false;

            @Override
            public void run() {
                try {
                    connect();
                    write(toWrite);
                    while (true) {
                        if (!taskQueue.isEmpty()) {
                            try {
                                Runnable stop = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                                if (stopOnlineHash == null || (stop != null && stop.hashCode() == stopOnlineHash)) {
                                    taskQueue.take();
                                    write(0);
                                    stopOnlineHash = null;
                                    break;
                                } else {
                                    taskQueue.clear();
                                }
                            } catch (InterruptedException e) {
                            }
                        }

                        // Check for first sample
                        while (!firstSampleRead) {
                            byte[] data = read(1);
                            if (data[0] == (byte) 0xc3) {
                                data = read(1);
                                firstSampleRead = data[0] == (byte) 0x3c;
                            }
                        }

                        // TODO
                        byte[] data = read(SAMPLE_SIZE);
                        OnlineSample sample = new OnlineSample(data, lastKnownSettings);
                        Log.i(DEBUG_TAG, sample.toString());
                        for (GadgetListener gl : listeners)
                            gl.onOnlineRejestrationSampleReceived(sample);
                    }

                    disconnect();
                } catch (IOException ex) {
                    Log.e(DEBUG_TAG, "startOnlineRejestration failed: " + ex);
                }
            }
        });
    }

    public void stopOnlineRejestration() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };
        stopOnlineHash = r.hashCode();
        taskQueue.add(r);
        lastKnownSettings = null;
    }

    public void getBattery() {
        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                Log.i(DEBUG_TAG, "Starting getBattery task");
                byte[] result = null;
                try {
                    connect();
                    write(176);
                    result = read();
                    int state = (((int) result[0]) << 8) | (int) result[1];
                    Log.i(DEBUG_TAG, "ACDC: " + state);
                    for (GadgetListener gl : listeners) gl.onBatteryReceived(state);
                    disconnect();
                } catch (IOException ex) {
                    Log.e(DEBUG_TAG, "getBattery failed: " + ex);
                }
                Log.i(DEBUG_TAG, "getBattery task finished");
            }
        });
    }

    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    private synchronized boolean isConnected() {
        return btSocket != null && btSocket.isConnected() && in != null && out != null;
    }

    private synchronized void connect() throws IOException {
        if (isConnected()) disconnect();

        UUID uuid = btDevice.getUuids()[0].getUuid();

        btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);

        int retry = 0;
        while (true) {
            try {
                btSocket.connect();
                break;
            } catch (IOException ex) {
                if (retry++ < 5) continue;
            }
        }

        in = new DataInputStream(new BufferedInputStream(btSocket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(btSocket.getOutputStream()));
    }

    private synchronized void disconnect() throws IOException {
        if (in != null) in.close();
        in = null;

        if (out != null) out.close();
        out = null;

        if (btSocket != null) btSocket.close();
        btSocket = null;
    }

    private synchronized void write(int cmd) throws IOException {
        out.write(cmd);
        out.flush();
    }

    private synchronized void write(byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    // reads at most numbytes bytes
    private synchronized byte[] read(int numBytes) throws IOException {
        byte[] buffer = new byte[numBytes];
        int numBytesRead = 0;

        int retries = 0;
        try {
            // wait for first byte to be available
            while (in.available() == 0) {
                if (retries++ > 5) throw new IOException("No data was available for reading");
                Thread.sleep(33);
            }
            // read bytes
            while (in.available() > 0) {
                numBytesRead += in.read(buffer, numBytesRead, Math.min(in.available(), numBytes - numBytesRead));
                Thread.sleep(33);
            }
        } catch (InterruptedException ex) {
            throw new IOException("Reading data was interrupted");
        }

        return Arrays.copyOfRange(buffer, 0, numBytesRead);
    }

    private synchronized byte[] read() throws IOException {
        return read(256);
    }

    // INNER SUPPORT CLASSES

    // Utilities for converting between UI and Gadget param values and types
    // Since streams in Java has writeByte(int) function all "byte" values are encoded as integers
    public static final class Util {

        private final static DateFormat format = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);

        public static int parseFrequency(String str) {
            str = str.replace("\\s*[Hh][Zz]", "");
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
            return new int[]{lo, hi};
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
                if (channels[i]) cnt++;
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
                if (channels[i]) config[i] = i & 0xff;
                else config[i] = 0xff;
            }
            return config;
        }

        public static int[] channelConfigurationOnline(boolean... channels) {
            int count = 0;
            for (int i = 0; i < channels.length; i++) if (channels[i]) count++;
            int[] config = new int[count + 2];
            config[0] = 0x05;
            for (int i = 0, j = 1; i < channels.length; i++) {
                if (channels[i]) config[j++] = (i & 0xff) | 0x80;
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

        public static int[] onlineSettings(int freq, int acc, int gyro, boolean... channels) {
            int cc = channelCount(channels);
            int[] settings = new int[2 + cc];
            settings[0] = 0x02 | freqToByte(freq) | channelCountToByte(channels);
            int i = 1;
            for (int j = 0; i < 1 + cc; i++, j++) {
                settings[i] = 0x80 | channelConfigurationOnline(channels)[j];
            }
            settings[i] = accToByte(acc) | gyroToByte(gyro);
            return settings;
        }


    }


}

