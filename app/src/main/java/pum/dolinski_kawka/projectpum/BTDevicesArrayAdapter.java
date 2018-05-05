package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collection;
import java.util.HashSet;

public class BTDevicesArrayAdapter extends BaseAdapter {
    private Activity context;
    private Collection<BluetoothDevice> devices;

    public BTDevicesArrayAdapter(Activity context) {
        super();
        this.context = context;
        devices = new HashSet<>();
    }

    public void add(BluetoothDevice device) {
        ParcelUuid[] uuids = device.getUuids();
        devices.add(device);
        notifyDataSetChanged();
    }

    public void addAll(Collection<? extends BluetoothDevice> devices) {
        this.devices.addAll(devices);
        notifyDataSetChanged();
    }

    public void clear() {
        devices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        if (position < 0 || position > devices.size()) {
            return null;
        }
        int i = 0;
        for (BluetoothDevice bt : devices) {
            if (i++ == position) {
                return bt;
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = context.getLayoutInflater().inflate(R.layout.lv_bluetooth_devices_item, null, true);

        BluetoothDevice btd = getItem(position);
        String name = btd.getName();
        boolean isPaired = BluetoothAdapter.getDefaultAdapter().getBondedDevices().contains(btd);

        TextView tvDeviceName = rowView.findViewById(R.id.tv_bt_device_name);
        tvDeviceName.setText(btd.getName());

        TextView tvIsPaired = rowView.findViewById(R.id.tv_bt_device_is_paired);
        if (isPaired) {
            tvIsPaired.setText(R.string.tv_is_paired);
            tvIsPaired.setBackgroundResource(R.color.color_tv_paired_background);
        } else {
            tvIsPaired.setText(R.string.tv_is_not_paired);
            tvIsPaired.setBackgroundResource(R.color.color_tv_not_paired_background);
        }

        return rowView;
    }
}
