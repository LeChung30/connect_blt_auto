package com.example.connect_blt_auto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
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
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_PERMISSION_BT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetoothAndScan();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void enableBluetoothAndScan() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
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
        strongestDevice = null;
        strongestRssi = Integer.MIN_VALUE;

        addPairedDevices();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, "Đang quét thiết bị Bluetooth...", Toast.LENGTH_SHORT).show();

        listView.postDelayed(() -> {
            bluetoothAdapter.cancelDiscovery();
            if (strongestDevice != null) {
                connectToDevice(strongestDevice);
            } else {
                Toast.makeText(this, "Không tìm thấy thiết bị mạnh nhất", Toast.LENGTH_SHORT).show();
            }
        }, 5000);
    }

    private void connectToDevice(BluetoothDevice device) {
        if (device == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        new Thread(() -> {
            try {
                // Initiate pairing
                boolean pairingInitiated = device.createBond();
                if (!pairingInitiated) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Không thể bắt đầu ghép đôi", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Wait for pairing to complete
                synchronized (this) {
                    wait(5000); // Wait for 5 seconds (adjust as needed)
                }

                // Connect to the device
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                bluetoothAdapter.cancelDiscovery();
                socket.connect();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Kết nối thành công!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                System.out.println("Lỗi kết nối: " + e.getMessage());
            }
        }).start();
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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                if (device != null && hasBluetoothPermissions()) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    String deviceName = (device.getName() != null) ? device.getName() : "Thiết bị không tên";
                    String deviceInfo = deviceName + " - " + device.getAddress() + " (RSSI: " + rssi + " dBm)";

                    if (!deviceList.contains(deviceInfo)) {
                        deviceList.add(deviceInfo);
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    }

                    if (rssi > strongestRssi) {
                        strongestRssi = rssi;
                        strongestDevice = device;
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quyền bị từ chối")
                .setMessage("Ứng dụng cần quyền Bluetooth để hoạt động. Vui lòng cấp quyền trong cài đặt.")
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
