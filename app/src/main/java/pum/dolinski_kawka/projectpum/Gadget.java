package pum.dolinski_kawka.projectpum;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

// GADGET HAS TO BE PAIR IN ORDER FOR IT TO WORK
public class Gadget {

    private final String DEBUG_TAG = "--GADGET--";

    // number of attempts to connect to device, used in connect()
    private final int RECONNECTS = 5;

    // general timeout in ms, generally indicates a wait period for device to process/expose data
    private final int TIMEOUT = 33;

    // default size of the buffer which is used for reading
    private final int READ_BUFF_SIZE = 256;

    //
    private final int READ_ATTEMPTS = 5;

    // list of available commands
    private interface CMD {
        int SET_MEAS_PARAMS = 1;
        int GET_ID = 2;
        int SET_ID = 3;
        int SET_ONLINE_ON = 5;
        int GET_MEASURMENT = 128;
        int RESET = 160;
        int GET_MEAS_PARAMS = 254;
    }

    private final BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private DataInputStream in;
    private DataOutputStream out;

    public Gadget(BluetoothDevice btDevice) {
        this.btDevice = btDevice;
        btSocket = null;
        in = null;
        out = null;
    }

    /**
     * Tries to connect to device, if device is already connected attempt to disconnect is performed
     * first. After function succeeds:
     * - socket is valid, opened & connected
     * - I/O streams are valid & opened
     *
     * @return was device connected and were I/O is established successfully
     */
    private synchronized boolean connect() {
        // Already connected and disconnect fails -> false
        if (isConnected() && !disconnect()) return false;

        try {
            // Get device UUID
            UUID uuid = btDevice.getUuids()[0].getUuid();

            // Create socket (old is discarded in disconnect)
            btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);

            // Attempt to connect
            int retry = 0;
            while (true) {
                try {
                    btSocket.connect();
                    break;
                } catch (IOException ex) {
                    if (retry++ < RECONNECTS) {
                        Log.i(DEBUG_TAG, "Failed to connect. Retrying (" + retry + "): " + ex.toString());
                        continue;
                    }
                    Log.i(DEBUG_TAG, "Failed to connect: " + ex.toString());
                    // Perform cleanup - invalidate sockets & I/O streams
                    disconnect();
                    return false;
                }
            }
        } catch (IOException | ArrayIndexOutOfBoundsException ex) {
            Log.i(DEBUG_TAG, "Error on connecting: " + ex.toString());
            // Perform cleanup - invalidate sockets & I/O streams
            disconnect();
            return false;
        }

        // Obtain I/O streams
        try {
            in = new DataInputStream(new BufferedInputStream(btSocket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(btSocket.getOutputStream()));
        } catch (IOException ex) {
            Log.i(DEBUG_TAG, "Failed to obtain I/O stream: " + ex.toString());
            // Perform cleanup - invalidate sockets & I/O streams
            disconnect();
            return false;
        }
        Log.i(DEBUG_TAG, "GADGET connected.");

        return true;
    }

    /**
     * Disconnects from device
     * - closes and invalidates BT socket
     * - closes and invalidates I/O streams
     * This method is thread-safe
     *
     * @return
     */
    private synchronized boolean disconnect() {
        // Socket and streams are already invalid
        if (btSocket == null && in == null && out == null) return true;

        try {
            // Close and invalidate socket
            if (btSocket != null) btSocket.close();
            btSocket = null;

            // Close and invalidate IN stream
            if (in != null) in.close();
            in = null;

            // Close and invalidate OUT stream
            if (out != null) out.close();
            out = null;
        } catch (IOException ex) {
            // Exception can only occur on close() so its pretty safe to ignore it
            Log.i(DEBUG_TAG, "Error on disconnecting: " + ex.toString());
        }

        return btSocket == null && in == null && out == null;
    }

    /**
     * Checks if all params for connections are A-OK
     * - socket is connected
     * - I/O streams are valid
     *
     * @return T/F
     */
    public synchronized boolean isConnected() {
        return btSocket != null && btSocket.isConnected() && in != null && out != null;
    }

    public synchronized String getIDString() throws IOException {
        if (!isConnected() && !connect()) throw new IOException("Gadget is not connected");

        write(CMD.GET_ID);
        byte[] output = read();

        // last byte is integer ID
        String id = new String(Arrays.copyOfRange(output, 0, output.length - 1));

        return null;
    }

    public synchronized int getIDNumber() throws IOException {
        if (!isConnected() && !connect()) throw new IOException("Gadget is not connected");

        write(CMD.GET_ID);
        byte[] output = read();

        int id = output[output.length - 1];
        return id;
    }

    // For writing commands which do not send any additional details (like GET_ID)
    private boolean write(int command) {
        try {
            out.write(command);
            out.flush();
            // Gadget needs some time to process command
            Thread.sleep(TIMEOUT);
        } catch (IOException | InterruptedException ex) {
            Log.i(DEBUG_TAG, "Error on sending command: " + command + ", " + ex.toString());
            return false;
        }
        return true;
    }

    private boolean write(int command, byte[] params) throws IOException {
        try {
            byte[] toSend = new byte[1 + params.length];

            toSend[0] = (byte) (command & 0x000000ff);
            System.arraycopy(params, 0, toSend, 1, params.length);

            out.write(toSend);
            out.flush();
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException ex) {
            Log.i(DEBUG_TAG, "Error on sending command: " + command + ", " + ex.toString());
            return false;
        }
        return true;
    }

    private byte[] read() throws IOException {
        byte[] buffer = new byte[READ_BUFF_SIZE];

        int numBytesRead = 0;

        // wait for something to read
        try {
            int retries = 0;
            while (in.available() == 0) {
                if (retries++ > READ_ATTEMPTS) {
                    throw new IOException("No data was available for reading");
                }
                Thread.sleep(TIMEOUT);
            }
            while (in.available() > 0) {
                numBytesRead += in.read(buffer, numBytesRead, in.available());
                Thread.sleep(TIMEOUT);
            }
        } catch (InterruptedException ex) {
            Log.i(DEBUG_TAG, "Error on reading: " + ex.toString());
        }

        return Arrays.copyOfRange(buffer, 0, numBytesRead);
    }
    /*

                Log.i(DEBUG_TAG, "pos: " + position);
                Log.i(DEBUG_TAG, "id: " + id);
                BluetoothDevice btDevice = (BluetoothDevice) parent.getAdapter().getItem(position);
                UUID uuid = btDevice.getUuids()[0].getUuid();
                Log.i(DEBUG_TAG, "BTDevice: " + btDevice.getName() + " " + btDevice.getAddress() + " " + uuid.toString());

                BluetoothSocket btSocket;
                try {
                    btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException ex) {
                    Log.i(DEBUG_TAG, "Failed to create RfComm socket: " + ex.toString());
                    return;
                }
                Log.i(DEBUG_TAG, "Created a bluetooth socket.");

                for (int i = 0; ; i++) {
                    try {
                        btSocket.connect();
                    } catch (IOException ex) {
                        if (i < 5) {
                            Log.i(DEBUG_TAG, "Failed to connect. Retrying: " + ex.toString());
                            continue;
                        }

                        Log.i(DEBUG_TAG, "Failed to connect: " + ex.toString());
                        return;
                    }
                    break;
                }

                Log.i(DEBUG_TAG, "Connected to the bluetooth socket.");
                try {
                    OutputStream writer = new DataOutputStream(new BufferedOutputStream(btSocket.getOutputStream()));
                    writer.write(2);
                    writer.flush();
                } catch (IOException ex) {
                    Log.i(DEBUG_TAG, "Failed to write a command: " + ex.toString());
                    return;
                }
                Log.i(DEBUG_TAG, "Command is sent: " + 2);

                byte[] output = new byte[256];
                String rejID = "";
                try {
                    DataInputStream reader = new DataInputStream(new BufferedInputStream(btSocket.getInputStream()));
                    int bytesRead = 0;
                    while(reader.available() < 1);
                    while(reader.available() > 0) {
                        bytesRead += reader.read(output, bytesRead, reader.available());
                        Thread.sleep(100);
                    }
                    Log.i(DEBUG_TAG, "Read #" + bytesRead + " bytes");
                    if(bytesRead > 0) {
                        rejID = new String(output);
                    }
                } catch (IOException|InterruptedException ex) {
                    Log.i(DEBUG_TAG, "Failed to write a command: " + ex.toString());
                    return;
                }
                Log.i(DEBUG_TAG, "Result: " + rejID);

                try {
                    btSocket.close();
                } catch (IOException ex) {
                    Log.i(DEBUG_TAG, "Failed to close the bluetooth socket.");
                    return;
                }
                Log.i(DEBUG_TAG, "Closed the bluetooth socket.");
 */
}

