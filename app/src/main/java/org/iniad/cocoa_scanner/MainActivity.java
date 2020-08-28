package org.iniad.cocoa_scanner;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final ParcelUuid UUID_COCOA = new ParcelUuid(UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB"));

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private RecyclerView recyclerView;
    private TextView tvSummary;
    private List<NeighbourDevice> devices = new ArrayList<>();
    private RecyclerView.Adapter adapter = new DeviceListAdapter(devices);

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSIONS);
        } else {
            initBLE();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanner.stopScan(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBLE();
            } else {
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                finish();
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initUI() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        tvSummary = (TextView) findViewById(R.id.tv_summary);

        handler = new Handler();
        handler.post(runnable);
    }

    private void initBLE() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            finish();
        }

        scan();
    }

    private void scan() {
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanFilter.Builder fBuilder = new ScanFilter.Builder();
        fBuilder.setServiceUuid(UUID_COCOA);
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(fBuilder.build());

        ScanSettings.Builder sBuilder = new ScanSettings.Builder();

        scanner.startScan(filters, sBuilder.build(), callback);
    }

    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            int rssi = result.getRssi();
            byte[] data = result.getScanRecord().getServiceData(UUID_COCOA);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02X", data[i]));
            }

            boolean found = false;
            String id = sb.toString();
            long now = System.currentTimeMillis();
            for (NeighbourDevice device : devices) {
                if(device.getId().equals(id)) {
                    device.update(rssi, now);
                    found = true;
                } else if(now - device.getRecevedAt() > 1000 * 30) {
                    device.invalidate();
                }
            }
            if(! found) {
                devices.add(new NeighbourDevice(id, rssi, now));
            }
            Log.i("BLE", String.format("Received:%s (%d)", id, rssi));
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            final Predicate<NeighbourDevice> isValid = new Predicate<NeighbourDevice>() {
                @Override
                public boolean test(NeighbourDevice item) {
                    return !item.isValid();
                }
            };

            final Comparator<NeighbourDevice> compareRssi = new Comparator<NeighbourDevice>() {
                @Override
                public int compare(NeighbourDevice n1, NeighbourDevice n2) {
                    return n2.getRssi() - n1.getRssi();
                }
            };

            devices.removeIf(isValid);

            if(devices.size() != 0) {
                devices.sort(compareRssi);
                adapter.notifyDataSetChanged();
                showStats();
            }

            handler.postDelayed(this, 1000);
        }
    };

    private void showStats() {
        double sum = 0;
        double sum2 = 0;
        int min = 0;
        int max = -127;
        for(NeighbourDevice device : devices) {
            int rssi = device.getRssi();
            sum += rssi;
            sum2 += rssi * rssi;
            if(rssi < min) {
                min = rssi;
            }
            if(rssi > max) {
                max = rssi;
            }
        }

        double average = sum / devices.size();
        double std = Math.sqrt(sum2 / devices.size() - average * average);

        String str = String.format("Count: %d\n - Average: %f\n - Standard Deviation: %f\n - Max: %d, Min:%d", devices.size(), average, std, max, min);

        tvSummary.setText(str);
    }
}
