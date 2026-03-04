package com.winlator.mt5;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TradingForegroundService extends Service {
    private static final String TAG             = "MT5-NeverSleep";
    private static final String CHANNEL_ID      = "mt5_trading_channel";
    private static final String CHANNEL_NAME    = "MT5 Trading Session";
    private static final int    NOTIFICATION_ID = 8888;
    private static final String ACTION_START    = "com.winlator.mt5.ACTION_START";
    private static final String ACTION_STOP     = "com.winlator.mt5.ACTION_STOP";

    private WakeLock cpuWakeLock;
    private final IBinder binder = new LocalBinder();

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, TradingForegroundService.class);
        i.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    public static void stop(Context ctx) {
        Intent i = new Intent(ctx, TradingForegroundService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            shutdown(); return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID,
            buildNotification("🟢 MT5 Running — Never Sleep active"));
        return START_STICKY;
    }

    @Override public void onDestroy() { releaseWakeLock(); super.onDestroy(); }

    @Nullable @Override public IBinder onBind(Intent intent) { return binder; }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return;
        cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            "Winlator:MT5TradingLock");
        cpuWakeLock.setReferenceCounted(false);
        cpuWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (cpuWakeLock != null && cpuWakeLock.isHeld()) {
            cpuWakeLock.release(); cpuWakeLock = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent piOpen = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stopI = new Intent(this, TradingForegroundService.class);
        stopI.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, 1, stopI,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Winlator MT5 — Trading Mode")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setColor(Color.parseColor("#1B5E20"))
            .setOngoing(true)
            .setContentIntent(piOpen)
            .addAction(android.R.drawable.ic_delete, "Stop", piStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    public void updateStatus(String text) {
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void shutdown() { releaseWakeLock(); stopForeground(true); stopSelf(); }

    public class LocalBinder extends Binder {
        public TradingForegroundService getService() { return TradingForegroundService.this; }
    }
  }
