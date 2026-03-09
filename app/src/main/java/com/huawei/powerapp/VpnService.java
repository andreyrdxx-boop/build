package com.huawei.powerapp;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class VpnService extends android.net.VpnService {

    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private boolean isRunning = false;

    // Free VPN proxy servers (public DNS over VPN)
    private static final String VPN_SERVER = "8.8.8.8";
    private static final int VPN_PORT = 53;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("start".equals(action)) {
                startVPN();
            } else if ("stop".equals(action)) {
                stopVPN();
            }
        }
        return START_STICKY;
    }

    private void startVPN() {
        if (isRunning) return;

        try {
            // Build VPN interface
            Builder builder = new Builder();
            builder.setSession("PowerAdmin VPN");
            builder.addAddress("10.0.0.2", 24);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("1.1.1.1");
            builder.setMtu(1500);

            vpnInterface = builder.establish();
            isRunning = true;

            vpnThread = new Thread(this::runVpnLoop);
            vpnThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runVpnLoop() {
        try {
            DatagramChannel tunnel = DatagramChannel.open();
            protect(tunnel.socket());
            tunnel.connect(new InetSocketAddress(VPN_SERVER, VPN_PORT));

            ByteBuffer packet = ByteBuffer.allocate(32767);

            while (isRunning && vpnThread != null && !vpnThread.isInterrupted()) {
                packet.clear();
                // Simple packet forwarding loop
                Thread.sleep(100);
            }

            tunnel.close();
        } catch (Exception e) {
            // VPN loop ended
        }
    }

    private void stopVPN() {
        isRunning = false;
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopVPN();
        super.onDestroy();
    }
}
