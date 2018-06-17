package pum.dolinski_kawka.projectpum;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Gadget {

    // region SINGLETON
    private static Gadget instance;

    private Gadget() {
        listeners = new HashSet<>();
        taskQueue = new LinkedBlockingQueue<>();
        isOnlineRegistrationRunning = false;
        tasksDispatcher.submit(taskDispatcherThread);
    }

    public static Gadget getInstance() {
        if (instance == null)
            instance = new Gadget();
        return instance;
    }
    // endregion

    // region DEVICE_PARAMETERS
    // publicly available params restrictions
    public final static List<Integer>         FREQUENCIES;
    public final static List<Integer>         GYROSCOPE_SENSITIVITIES;
    public final static List<Integer>         ACCELEROMETER_SENSITIVITIES;
    public final static Map<Integer, Integer> CHANNEL_PER_FREQUENCY;
    public final static String[]              CHANNELS;

    // static initalizer block (init static public constants)
    static {
        FREQUENCIES = Arrays.asList(20, 50, 100, 200, 400, 500, 700, 1000);
        GYROSCOPE_SENSITIVITIES = Arrays.asList(250, 500, 2500);
        ACCELEROMETER_SENSITIVITIES = Arrays.asList(6, 12, 24);
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
        CHANNELS = new String[]{ "AX", "AY", "AZ", "GR", "GP", "GY" };
    }
    // endregion DEVICE_PARAMETERS

    // region SIMPLE_TASK_IMPL
    class SimpleTask implements Runnable {
        int             command;
        int[]           additionalData;
        Consumer<int[]> callback;

        public SimpleTask(int command, Consumer<int[]> callback) {
            this(command, null, callback);
        }

        public SimpleTask(int command, int[] additionalData, Consumer<int[]> callback) {
            this.command = command;
            if (additionalData != null && additionalData.length > 0)
                this.additionalData = Arrays.copyOf(additionalData, additionalData.length);
            else
                additionalData = null;

            this.callback = callback;
        }

        @Override
        public void run() {
            if (bluetoothDevice == null) {
                Log.e(DEBUG.TAG, "Bluetooth device is null");
                if (callback != null)
                    callback.accept(null);
                return;
            }

            ParcelUuid puuids[] = bluetoothDevice.getUuids();
            if (puuids == null || puuids.length == 0) {
                Log.e(DEBUG.TAG, "Bluetooth device has no valid UUID");
                if (callback != null)
                    callback.accept(null);
                return;
            }

            List<UUID> uuidCandidates = new ArrayList<>();
            for (ParcelUuid puuid : puuids)
                uuidCandidates.add(puuid.getUuid());

            boolean secure = true;
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothConnector connector = new BluetoothConnector(bluetoothDevice, secure, bluetoothAdapter, uuidCandidates);

            try {
                BluetoothConnector.BluetoothSocketWrapper bluetoothSocket = connector.connect();

                try (DataInputStream in = new DataInputStream(new BufferedInputStream(bluetoothSocket.getInputStream()));
                     DataOutputStream out = new DataOutputStream(new BufferedOutputStream(bluetoothSocket.getOutputStream()))) {

                    int[] output = new int[1 + (additionalData != null ? additionalData.length : 0)];
                    output[0] = command;
                    if (additionalData != null && additionalData.length > 0)
                        System.arraycopy(additionalData, 0, output, 1, additionalData.length);


                    Log.i(DEBUG.TAG, "Writing: " + Arrays.toString(output));
                    for (int b : output)
                        out.write(b);
                    out.flush();
                    Log.i(DEBUG.TAG, "Written");

                    Thread.sleep(333);

                    Log.i(DEBUG.TAG, "Reading...");
                    byte[] buffer = new byte[256];
                    int bytesRead = 0;
                    while (in.available() > 0) {
                        bytesRead += in.read(buffer, bytesRead, Math.min(in.available(), buffer.length - bytesRead));
                        Log.i(DEBUG.TAG, "Read bytes: " + bytesRead);
                        Thread.sleep(333);
                    }

                    int[] input = null;
                    if (bytesRead > 0) {
                        input = new int[bytesRead];
                        for (int i = 0; i < bytesRead; i++)
                            input[i] = buffer[i] & 0xff;
                    }
                    Log.i(DEBUG.TAG, "Read finished: " + bytesRead + " " + Arrays.toString(input));

                    if (callback != null)
                        callback.accept(input);
                } catch (IOException | InterruptedException ex) {
                    Log.e(DEBUG.TAG, "Error on communication: " + Log.getStackTraceString(ex));
                    if (callback != null)
                        callback.accept(null);
                }

                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(DEBUG.TAG, "Error on communication: " + Log.getStackTraceString(e));
            }

        }
    }
    // endregion SIMPLE_TASK_IMPL

    // region COMMAND_LIST
    public interface CMD {
        int STOP_ONLINE_REGISTRATION = 0;
        int SET_OFFLINE_SETTINGS     = 1;
        int GET_ID                   = 2;
        int SET_ID                   = 3;
        //...
        int GET_BATTERY              = 176;
    }
    // endregion COMMAND_LIST

    private          BluetoothDevice         bluetoothDevice;
    private volatile boolean                 isOnlineRegistrationRunning;
    private          BlockingQueue<Runnable> taskQueue;
    private          Set<GadgetListener>     listeners;
    private final ExecutorService tasksDispatcher      = Executors.newSingleThreadExecutor();
    private final Runnable        taskDispatcherThread = new Runnable() {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future task = null;

        @Override
        public void run() {
            while (true) {
                // task finished and wasn't cancelled - remove it
                if (task != null && task.isDone() && !task.isCancelled())
                    task = null;

                // either queue is empty or task was not finished yet
                if (taskQueue.isEmpty() || (task != null && !task.isDone() && !task.isCancelled()))
                    continue;

                // awaiting task ready
                try {
                    task = executorService.submit(taskQueue.take());
                } catch (InterruptedException e) {
                    Log.e(DEBUG.TAG, Log.getStackTraceString(e));
                }
            }
        }
    };

    public void addListener(GadgetListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GadgetListener listener) {
        listeners.remove(listener);
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public void test() {
        taskQueue.add(new SimpleTask(CMD.GET_ID, null, new Consumer<int[]>() {
            @Override
            public void accept(int[] bytes) {
                for (GadgetListener listener : listeners)
                    listener.onTestReceived(bytes != null && bytes.length > 0);
            }
        }));
    }

    public void getID() {
        taskQueue.add(new SimpleTask(CMD.GET_ID, null, new Consumer<int[]>() {
            @Override
            public void accept(int[] bytes) {
                int len = bytes.length;
                // last byte is ID (number), rest is human-readable name
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len - 2; i++) {
                    sb.append((char) bytes[i]);
                }
                String name = sb.toString();
                int id = bytes[len - 1];

                for (GadgetListener listener : listeners)
                    listener.onIDReceived(name, id);
            }
        }));
    }

    public void setID(int id) {
        taskQueue.add(new SimpleTask(CMD.SET_ID, new int[]{ id }, null));
    }

    public void getBattery() {
        taskQueue.add(new SimpleTask(CMD.GET_BATTERY, null, new Consumer<int[]>() {
            @Override
            public void accept(int[] bytes) {
                int battery = GadgetUtil.batteryFromByte(bytes);
                for (GadgetListener listener : listeners)
                    listener.onBatteryReceived(battery);
            }
        }));
    }


    public void startOnlineRegistration(final OnlineSettings settings) {
        taskQueue.add(new Runnable() {
            @Override
            public void run() {
                if (!settings.areValid()) {
                    Log.e(DEBUG.TAG, "Invalid settings");
                    for (GadgetListener listener : listeners)
                        listener.onOnlineRegistrationStopped();
                    return;
                }

                if (bluetoothDevice == null) {
                    Log.e(DEBUG.TAG, "Bluetooth device is null");
                    for (GadgetListener listener : listeners)
                        listener.onOnlineRegistrationStopped();
                    return;
                }

                ParcelUuid puuids[] = bluetoothDevice.getUuids();
                if (puuids == null || puuids.length == 0) {
                    Log.e(DEBUG.TAG, "Bluetooth device has no valid UUID");
                    for (GadgetListener listener : listeners)
                        listener.onOnlineRegistrationStopped();
                    return;
                }

                List<UUID> uuidCandidates = new ArrayList<>();
                for (ParcelUuid puuid : puuids)
                    uuidCandidates.add(puuid.getUuid());

                boolean secure = true;
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothConnector connector = new BluetoothConnector(bluetoothDevice, secure, bluetoothAdapter, uuidCandidates);

                try {
                    BluetoothConnector.BluetoothSocketWrapper bluetoothSocket = connector.connect();

                    try (DataInputStream in = new DataInputStream(new BufferedInputStream(bluetoothSocket.getInputStream()));
                         DataOutputStream out = new DataOutputStream(new BufferedOutputStream(bluetoothSocket.getOutputStream()))) {

                        settings.setStartByte(true);
                        settings.setToggleByte(true);

                        Log.i(DEBUG.TAG, "Starting online registration....");
                        for (int i : settings.toByteArray()) {
                            out.write(i);
                            out.flush();
                            Thread.sleep(33);
                        }


                        Log.i(DEBUG.TAG, "Started!");
                        isOnlineRegistrationRunning = true;

                        final int sampleSize = settings.channelCnt * 2 + 2;

                        byte[] buffer = new byte[128];
                        int idx = 0;
                        int LP_READ = 0;
                        int totalNumBytesRead = 0;
                        while (isOnlineRegistrationRunning) {
                            if (in.available() == 0)
                                continue;

                            int bytesRead = in.read(buffer, idx, Math.min(in.available(), buffer.length - idx));
                            idx += bytesRead;
                            totalNumBytesRead += bytesRead;
                            LP_READ++;

                            int i = 0;
                            if (idx > sampleSize) {
                                for (i = 0; i < idx; i += sampleSize) {
                                    byte[] sampleData = Arrays.copyOfRange(buffer, i, i + sampleSize);
                                    Log.i(DEBUG.TAG, "LP_READ: " + LP_READ + " " + Arrays.toString(sampleData));
                                    for (GadgetListener listener : listeners)
                                        listener.onOnlineRegistrationSampleReceived(new OnlineSample(sampleData, settings));
                                }

                                for (int j = 0; i < idx; i++, j++) {
                                    buffer[j] = buffer[i];
                                }
                                idx = 0;
                            }
                            Log.i(DEBUG.TAG, "Idx: " + idx + ", i: " + i + "LP_READ: " + LP_READ + " " + Arrays.toString(buffer));

                        }
                        Log.i(DEBUG.TAG, "Online registration finished! Total num bytes read: " + totalNumBytesRead);

                        out.write(CMD.STOP_ONLINE_REGISTRATION);
                        out.flush();

                        Thread.sleep(33);

                    } catch (IOException | InterruptedException ex) {
                        Log.e(DEBUG.TAG, "Error on communication: " + Log.getStackTraceString(ex));
                    }

                    bluetoothSocket.close();
                } catch (IOException e) {
                    Log.e(DEBUG.TAG, "Error on communication: " + Log.getStackTraceString(e));
                }
            }
        });
    }

    public void stopOnlineRegistration() {
        isOnlineRegistrationRunning = false;
    }

}
