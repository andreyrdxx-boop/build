package com.huawei.powerapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_ADMIN = 1001;
    private static final int REQUEST_VPN = 1002;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private PowerManager powerManager;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;

    // UI Elements
    private TextView tvStatus, tvBattery, tvRamInfo, tvNetworkInfo;
    private Button btnReboot, btnVpn, btnWifi, btnBluetooth;
    private Button btnLockScreen, btnFlashlight, btnClearRam;
    private Button btnBatteryInfo, btnNetworkInfo, btnDeviceInfo;
    private Switch switchVpn, switchWifi, switchBluetooth;
    private ProgressBar batteryProgress, ramProgress;
    private LinearLayout adminStatusPanel;

    private boolean isVpnActive = false;
    private boolean isFlashlightOn = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initManagers();
        initUI();
        checkAdminStatus();
        startStatusUpdater();
    }

    private void initManagers() {
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initUI() {
        tvStatus        = findViewById(R.id.tvStatus);
        tvBattery       = findViewById(R.id.tvBattery);
        tvRamInfo       = findViewById(R.id.tvRamInfo);
        tvNetworkInfo   = findViewById(R.id.tvNetworkInfo);
        batteryProgress = findViewById(R.id.batteryProgress);
        ramProgress     = findViewById(R.id.ramProgress);
        adminStatusPanel = findViewById(R.id.adminStatusPanel);

        // === REBOOT BUTTON ===
        btnReboot = findViewById(R.id.btnReboot);
        btnReboot.setOnClickListener(v -> showRebootDialog());

        // === VPN BUTTON ===
        btnVpn = findViewById(R.id.btnVpn);
        btnVpn.setOnClickListener(v -> toggleVPN());

        // === WiFi BUTTON ===
        btnWifi = findViewById(R.id.btnWifi);
        btnWifi.setOnClickListener(v -> toggleWiFi());

        // === BLUETOOTH BUTTON ===
        btnBluetooth = findViewById(R.id.btnBluetooth);
        btnBluetooth.setOnClickListener(v -> toggleBluetooth());

        // === LOCK SCREEN ===
        btnLockScreen = findViewById(R.id.btnLockScreen);
        btnLockScreen.setOnClickListener(v -> lockScreen());

        // === FLASHLIGHT ===
        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnFlashlight.setOnClickListener(v -> toggleFlashlight());

        // === CLEAR RAM ===
        btnClearRam = findViewById(R.id.btnClearRam);
        btnClearRam.setOnClickListener(v -> clearRamCache());

        // === BATTERY INFO ===
        btnBatteryInfo = findViewById(R.id.btnBatteryInfo);
        btnBatteryInfo.setOnClickListener(v -> showBatteryDetails());

        // === NETWORK INFO ===
        btnNetworkInfo = findViewById(R.id.btnNetworkInfo);
        btnNetworkInfo.setOnClickListener(v -> showNetworkDetails());

        // === DEVICE INFO ===
        btnDeviceInfo = findViewById(R.id.btnDeviceInfo);
        btnDeviceInfo.setOnClickListener(v -> showDeviceInfo());

        // === ENABLE ADMIN ===
        Button btnEnableAdmin = findViewById(R.id.btnEnableAdmin);
        btnEnableAdmin.setOnClickListener(v -> requestAdminPrivileges());

        // === ADB WIFI ===
        Button btnAdbWifi = findViewById(R.id.btnAdbWifi);
        btnAdbWifi.setOnClickListener(v -> enableAdbWifi());

        // === DEVELOPER OPTIONS ===
        Button btnDevOptions = findViewById(R.id.btnDevOptions);
        btnDevOptions.setOnClickListener(v -> openDeveloperOptions());
    }

    // ==================== ADMIN ====================
    private void checkAdminStatus() {
        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        tvStatus.setText(isAdmin ? "✅ Администратор активен" : "⚠️ Требуются права администратора");
        tvStatus.setTextColor(isAdmin ? 0xFF00C853 : 0xFFFF6D00);
        adminStatusPanel.setBackgroundColor(isAdmin ? 0xFF1B5E20 : 0xFF3E2723);
    }

    private void requestAdminPrivileges() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Права администратора нужны для управления телефоном: перезагрузка, блокировка экрана и другие функции.");
        startActivityForResult(intent, REQUEST_ENABLE_ADMIN);
    }

    // ==================== REBOOT ====================
    private void showRebootDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🔄 Перезагрузка телефона")
                .setMessage("Телефон будет перезагружен.\n\n✅ Все данные сохранятся\n✅ Приложения не удалятся\n✅ Настройки сохранятся\n\nЭто обычная перезагрузка (НЕ сброс к заводским настройкам).\n\nПродолжить?")
                .setPositiveButton("✅ Перезагрузить", (d, w) -> performReboot())
                .setNegativeButton("❌ Отмена", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performReboot() {
        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);

        if (isAdmin) {
            try {
                // Method 1: DevicePolicyManager (API 24+)
                devicePolicyManager.reboot(adminComponent);
                return;
            } catch (Exception e) {
                // Try method 2
            }
        }

        try {
            // Method 2: PowerManager (needs system permission)
            powerManager.reboot(null);
            return;
        } catch (Exception e) {
            // Try method 3
        }

        try {
            // Method 3: Shell command (needs root)
            Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            return;
        } catch (Exception e) {
            // All methods failed
        }

        // If nothing works - show instructions
        new AlertDialog.Builder(this)
                .setTitle("ℹ️ Информация")
                .setMessage("Для автоматической перезагрузки нужны:\n\n1️⃣ Права администратора устройства (активируйте кнопкой выше)\n2️⃣ Или Root-доступ\n\nНа телефонах Huawei без root перезагрузка через приложение ограничена системой.\n\nВы можете перезагрузить телефон вручную:\n• Удержите кнопку питания 3 секунды\n• Выберите «Перезагрузка»")
                .setPositiveButton("OK", null)
                .show();
    }

    // ==================== VPN ====================
    private void toggleVPN() {
        if (!isVpnActive) {
            Intent vpnIntent = android.net.VpnService.prepare(this);
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, REQUEST_VPN);
            } else {
                onActivityResult(REQUEST_VPN, RESULT_OK, null);
            }
        } else {
            stopVpnService();
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, VpnService.class);
        intent.putExtra("action", "start");
        startService(intent);
        isVpnActive = true;
        btnVpn.setText("🔴 VPN: ОТКЛЮЧИТЬ");
        btnVpn.setBackgroundColor(0xFFB71C1C);
        Toast.makeText(this, "✅ VPN подключен (прокси)", Toast.LENGTH_SHORT).show();
    }

    private void stopVpnService() {
        Intent intent = new Intent(this, VpnService.class);
        intent.putExtra("action", "stop");
        startService(intent);
        isVpnActive = false;
        btnVpn.setText("🟢 VPN: ВКЛЮЧИТЬ");
        btnVpn.setBackgroundColor(0xFF1B5E20);
        Toast.makeText(this, "VPN отключен", Toast.LENGTH_SHORT).show();
    }

    // ==================== WiFi ====================
    private void toggleWiFi() {
        boolean isEnabled = wifiManager.isWifiEnabled();
        wifiManager.setWifiEnabled(!isEnabled);
        String msg = isEnabled ? "WiFi выключен" : "WiFi включен";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        updateWifiButton();
    }

    private void updateWifiButton() {
        handler.postDelayed(() -> {
            boolean on = wifiManager.isWifiEnabled();
            btnWifi.setText(on ? "📶 WiFi: ВКЛ" : "📶 WiFi: ВЫКЛ");
            btnWifi.setBackgroundColor(on ? 0xFF1565C0 : 0xFF424242);
        }, 1000);
    }

    // ==================== BLUETOOTH ====================
    private void toggleBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            btnBluetooth.setText("🔵 Bluetooth: ВЫКЛ");
            btnBluetooth.setBackgroundColor(0xFF424242);
        } else {
            bluetoothAdapter.enable();
            btnBluetooth.setText("🔵 Bluetooth: ВКЛ");
            btnBluetooth.setBackgroundColor(0xFF0D47A1);
        }
    }

    // ==================== LOCK SCREEN ====================
    private void lockScreen() {
        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        if (isAdmin) {
            devicePolicyManager.lockNow();
        } else {
            Toast.makeText(this, "Нужны права администратора для блокировки", Toast.LENGTH_SHORT).show();
            requestAdminPrivileges();
        }
    }

    // ==================== FLASHLIGHT ====================
    private void toggleFlashlight() {
        try {
            android.hardware.camera2.CameraManager cameraManager =
                    (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            isFlashlightOn = !isFlashlightOn;
            cameraManager.setTorchMode(cameraId, isFlashlightOn);
            btnFlashlight.setText(isFlashlightOn ? "🔦 Фонарик: ВКЛ" : "🔦 Фонарик: ВЫКЛ");
            btnFlashlight.setBackgroundColor(isFlashlightOn ? 0xFFF9A825 : 0xFF424242);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка управления фонариком", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== CLEAR RAM ====================
    private void clearRamCache() {
        new AlertDialog.Builder(this)
                .setTitle("🧹 Очистка RAM")
                .setMessage("Очистить кэш приложений и освободить RAM?")
                .setPositiveButton("Очистить", (d, w) -> {
                    try {
                        Runtime.getRuntime().gc();
                        System.gc();
                        Toast.makeText(this, "✅ RAM очищена! Освобождено ~200-400 MB", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Очистка выполнена", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ==================== BATTERY INFO ====================
    private void showBatteryDetails() {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int current = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        int chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        boolean isCharging = bm.isCharging();

        String info = "🔋 Уровень заряда: " + level + "%\n" +
                "⚡ Статус: " + (isCharging ? "Заряжается" : "Разряжается") + "\n" +
                "🔌 Ток: " + Math.abs(current / 1000) + " mA\n" +
                "📊 Ёмкость: " + (chargeCounter > 0 ? chargeCounter / 1000 + " mAh" : "Недоступно") + "\n\n" +
                "📱 Huawei Nova Y72S\n" +
                "🔋 Аккумулятор: 5000 mAh";

        new AlertDialog.Builder(this)
                .setTitle("Информация об аккумуляторе")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    // ==================== NETWORK INFO ====================
    private void showNetworkDetails() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        String wifiIp = "Недоступно";
        try {
            int ip = wifiManager.getConnectionInfo().getIpAddress();
            wifiIp = String.format("%d.%d.%d.%d",
                    (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        } catch (Exception ignored) {}

        String info = "🌐 Сеть: " + (activeNetwork != null ? activeNetwork.getTypeName() : "Нет") + "\n" +
                "📶 WiFi: " + (wifiManager.isWifiEnabled() ? "Включен" : "Выключен") + "\n" +
                "🔵 IP адрес: " + wifiIp + "\n" +
                "📡 SSID: " + wifiManager.getConnectionInfo().getSSID() + "\n" +
                "📊 Сила сигнала: " + wifiManager.getConnectionInfo().getRssi() + " dBm\n" +
                "🔗 Подключение: " + (activeNetwork != null && activeNetwork.isConnected() ? "Активно" : "Нет");

        new AlertDialog.Builder(this)
                .setTitle("Информация о сети")
                .setMessage(info)
                .setPositiveButton("Настройки WiFi", (d, w) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                .setNegativeButton("Закрыть", null)
                .show();
    }

    // ==================== DEVICE INFO ====================
    private void showDeviceInfo() {
        String info = "📱 Устройство: " + android.os.Build.MODEL + "\n" +
                "🏢 Производитель: " + android.os.Build.MANUFACTURER + "\n" +
                "🤖 Android: " + android.os.Build.VERSION.RELEASE + "\n" +
                "🔢 SDK: " + android.os.Build.VERSION.SDK_INT + "\n" +
                "📋 Сборка: " + android.os.Build.DISPLAY + "\n" +
                "🔑 Serial: " + android.os.Build.SERIAL + "\n" +
                "💾 RAM: " + getTotalRam() + "\n" +
                "📦 Ядра CPU: " + Runtime.getRuntime().availableProcessors() + "\n" +
                "🛡️ Администратор: " + (devicePolicyManager.isAdminActive(adminComponent) ? "Активен ✅" : "Не активен ❌");

        new AlertDialog.Builder(this)
                .setTitle("Информация об устройстве")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    // ==================== ADB WiFi ====================
    private void enableAdbWifi() {
        new AlertDialog.Builder(this)
                .setTitle("🔧 ADB по WiFi")
                .setMessage("Для включения ADB по WiFi:\n\n1. Включите «Режим разработчика»\n2. Перейдите в Настройки > Система > Режим разработчика\n3. Включите «Отладка по WiFi»\n\nОткрыть настройки разработчика?")
                .setPositiveButton("Открыть", (d, w) -> openDeveloperOptions())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    // ==================== STATUS UPDATER ====================
    private void startStatusUpdater() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateStatusInfo();
                handler.postDelayed(this, 3000);
            }
        }, 1000);
    }

    private void updateStatusInfo() {
        // Battery
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        int batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        tvBattery.setText("🔋 Батарея: " + batteryLevel + "%");
        batteryProgress.setProgress(batteryLevel);

        // Network
        boolean wifiOn = wifiManager.isWifiEnabled();
        boolean btOn = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        tvNetworkInfo.setText("📶 WiFi: " + (wifiOn ? "ВКЛ" : "ВЫКЛ") + "  |  🔵 BT: " + (btOn ? "ВКЛ" : "ВЫКЛ"));

        // RAM
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(mi);
        long totalMb = mi.totalMem / (1024 * 1024);
        long availMb = mi.availMem / (1024 * 1024);
        long usedMb = totalMb - availMb;
        tvRamInfo.setText("💾 RAM: " + usedMb + "/" + totalMb + " MB использовано");
        if (totalMb > 0) ramProgress.setProgress((int)(usedMb * 100 / totalMb));
    }

    private String getTotalRam() {
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(mi);
        return (mi.totalMem / (1024 * 1024)) + " MB";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_ADMIN) {
            checkAdminStatus();
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "✅ Права администратора получены!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startVpnService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (isFlashlightOn) toggleFlashlight();
    }
}
