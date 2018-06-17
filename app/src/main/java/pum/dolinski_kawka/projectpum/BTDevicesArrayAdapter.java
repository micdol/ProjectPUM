package pum.dolinski_kawka.projectpum;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collection;
import java.util.HashSet;

// Adapter for ListView used in BTDevicesActivity
// Holds info about bluetooth device, displays only name and whether device is already paired
public class BTDevicesArrayAdapter extends BaseAdapter {
    private Activity                    context;
    private Collection<BluetoothDevice> btDevices;

    public BTDevicesArrayAdapter(Activity context) {
        super();
        this.context = context;
        btDevices = new HashSet<>();
    }

    public void add(BluetoothDevice btDevice) {
        ParcelUuid[] uuids = btDevice.getUuids();
        btDevices.add(btDevice);
        notifyDataSetChanged();
    }

    public void addAll(Collection<? extends BluetoothDevice> btDevices) {
        this.btDevices.addAll(btDevices);
        notifyDataSetChanged();
    }

    public void clear() {
        btDevices.clear();
        notifyDataSetChanged();
    }

    // region BASE_ADAPTER_IMPL
    @Override
    public int getCount() {
        return btDevices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        if (position < 0 || position > btDevices.size()) {
            return null;
        }
        int i = 0;
        for (BluetoothDevice bt : btDevices) {
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

        final BluetoothDevice btd = getItem(position);
        String name = btd.getName();
        if(name == null || name == "" || name.isEmpty()) {
            name = "UNKNOWN";
        }
        boolean isPaired = BluetoothAdapter.getDefaultAdapter().getBondedDevices().contains(btd);

        final TextView tvDeviceName = rowView.findViewById(R.id.tv_bt_device_name);
        tvDeviceName.setText(name);

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
    // endregion BASE_ADAPTER_IMPL
}
