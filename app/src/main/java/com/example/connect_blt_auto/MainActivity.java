package com.example.connect_blt_auto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_BT = 2;

    private BluetoothAdapter bluetoothAdapter;
    private ListView listView;
    private ArrayList<String> deviceList;
    private ArrayAdapter<String> adapter;
    private Button btnScan;

    private BluetoothDevice strongestDevice = null;
    private int strongestRssi = Integer.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list_view_devices);
        btnScan = findViewById(R.id.btn_scan);
        deviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        listView.setAdapter(adapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth không được hỗ trợ", Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            enableBluetoothAndScan();
        }

        btnScan.setOnClickListener(v -> {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions();
            } else {
                startDiscovery();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDeviceInfo = deviceList.get(position);
            String deviceAddress = selectedDeviceInfo.split(" - ")[1].split(" ")[0]; // Lấy địa chỉ MAC
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            connectToDevice(device);
        });
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_PERMISSION_BT);
    }

    private void enableBluetoothAndScan() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startDiscovery();
        }
    }

    private void startDiscovery() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth chưa được bật", Toast.LENGTH_SHORT).show();
            return;
        }

        deviceList.clear();
        adapter.notifyDataSetChanged();
        scannedDevices.clear();

        addPairedDevices();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, "Đang quét thiết bị Bluetooth...", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            bluetoothAdapter.cancelDiscovery();
            if (!scannedDevices.isEmpty()) {
                BluetoothDevice weakestDevice = scannedDevices.get(0).first;
                connectToDevice(weakestDevice);
            }
        }, 10000); // Đợi 10 giây để quét xong rồi kết nối
    }


    private void addPairedDevices() {
        if (hasBluetoothPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = (device.getName() != null) ? device.getName() : "Unknown Device";
                String deviceInfo = deviceName + " - " + device.getAddress();
                if (!deviceList.contains(deviceInfo)) {
                    deviceList.add(deviceInfo);
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    private final ArrayList<Pair<BluetoothDevice, Integer>> scannedDevices = new ArrayList<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                if (device != null && hasBluetoothPermissions()) {
                    String deviceName = (device.getName() != null) ? device.getName() : "Thiết bị không tên";
                    String deviceInfo = deviceName + " - " + device.getAddress() + " (RSSI: " + rssi + " dBm)";

                    // Kiểm tra nếu thiết bị chưa có trong danh sách
                    boolean exists = false;
                    for (Pair<BluetoothDevice, Integer> entry : scannedDevices) {
                        if (entry.first.getAddress().equals(device.getAddress())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        scannedDevices.add(new Pair<>(device, rssi));
                    }

                    runOnUiThread(() -> {
                        deviceList.clear();
                        // Sắp xếp danh sách theo RSSI giảm dần (mạnh nhất trước)
                        scannedDevices.sort((p1, p2) -> Integer.compare(p2.second, p1.second));

                        for (Pair<BluetoothDevice, Integer> entry : scannedDevices) {
                            deviceList.add(entry.first.getName() + " - " + entry.first.getAddress() + " (RSSI: " + entry.second + " dBm)");
                        }
                        adapter.notifyDataSetChanged();
                    });


                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        if (device == null) return;

        new Thread(() -> {
            try {
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    boolean pairingInitiated = device.createBond();
                    if (!pairingInitiated) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Không thể bắt đầu ghép đôi", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    synchronized (this) {
                        wait(5000);
                    }
                }

                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                bluetoothAdapter.cancelDiscovery();
                socket.connect();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Kết nối thành công!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}