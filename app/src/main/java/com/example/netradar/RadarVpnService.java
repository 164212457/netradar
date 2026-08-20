package com.example.netradar;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
public class RadarVpnService extends VpnService {
    private Thread mThread;
    private ParcelFileDescriptor mInterface;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        mThread = new Thread(this::runVpn);
        mThread.start();
        return START_STICKY;
    }
    private void runVpn() {
        try {
            Builder builder = new Builder();
            builder.setAddress("10.0.0.1", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.setMtu(1500);
            builder.setSession("Radar VPN");
            mInterface = builder.establish();
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
            ByteBuffer packet = ByteBuffer.allocate(65535);
            while (true) {
                int len = in.read(packet.array());
                if (len > 0) {
                    packet.limit(len);
                    parsePacket(packet);
                    out.write(packet.array(), 0, len);
                    packet.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }
    private void parsePacket(ByteBuffer packet) {
        if (packet.remaining() < 0x20) return;
        float x = packet.getFloat(0x10);
        float y = packet.getFloat(0x14);
        float z = packet.getFloat(0x18);
        if (x > -10000 && x < 10000 && y > -10000 && y < 10000) {
            updateOverlay(x, y, z);
        }
    }
    private void updateOverlay(float x, float y, float z) {
        Intent intent = new Intent("UPDATE_OVERLAY");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("z", z);
        sendBroadcast(intent);
    }
    private void createNotification() {
        String channelId = "radar_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "雷达服务", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("坐标雷达")
                .setContentText("正在拦截网络包...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi
cat > ~/app/app/src/main/java/com/example/netradar/RadarVpnService.java << 'EOF'
package com.example.netradar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class RadarVpnService extends VpnService {
    private Thread mThread;
    private ParcelFileDescriptor mInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        mThread = new Thread(this::runVpn);
        mThread.start();
        return START_STICKY;
    }

    private void runVpn() {
        try {
            Builder builder = new Builder();
            builder.setAddress("10.0.0.1", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.setMtu(1500);
            builder.setSession("Radar VPN");
            mInterface = builder.establish();

            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

            ByteBuffer packet = ByteBuffer.allocate(65535);
            while (true) {
                int len = in.read(packet.array());
                if (len > 0) {
                    packet.limit(len);
                    parsePacket(packet);
                    out.write(packet.array(), 0, len);
                    packet.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void parsePacket(ByteBuffer packet) {
        if (packet.remaining() < 0x20) return;
        float x = packet.getFloat(0x10);
        float y = packet.getFloat(0x14);
        float z = packet.getFloat(0x18);
        if (x > -10000 && x < 10000 && y > -10000 && y < 10000) {
            updateOverlay(x, y, z);
        }
    }

    private void updateOverlay(float x, float y, float z) {
        Intent intent = new Intent("UPDATE_OVERLAY");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("z", z);
        sendBroadcast(intent);
    }

    private void createNotification() {
        String channelId = "radar_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "雷达服务", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("坐标雷达")
                .setContentText("正在拦截网络包...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mThread != null) mThread.interrupt();
        if (mInterface != null) {
            try { mInterface.close(); } catch (Exception e) {}
        }
    }
}
