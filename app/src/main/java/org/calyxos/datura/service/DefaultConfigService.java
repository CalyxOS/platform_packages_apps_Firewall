package org.calyxos.datura.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.calyxos.datura.MainActivity;
import org.calyxos.datura.NewPackageInstallReceiver;
import org.calyxos.datura.R;
import org.calyxos.datura.util.Constants;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

public class DefaultConfigService extends Service {

    private static final String TAG = DefaultConfigService.class.getSimpleName();
    private NewPackageInstallReceiver newPackageInstallReceiver;
    private StopActionReceiver stopActionReceiver;
    private static DefaultConfigService instance;

    public class ServiceBinder extends Binder {
        public DefaultConfigService getService() {
            return DefaultConfigService.this;
        }
    }

    private final ServiceBinder binder = new ServiceBinder();

    public Notification getNotification() {
        Log.d(TAG, "Notification creation started");
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.default_config_notification_title))
                .setContentText(getString(R.string.default_config_notification_desc))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.default_config_notification_desc)))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(getPendingIntent())
                .addAction(R.drawable.ic_close_24, getString(R.string.stop), getStopActionPendingIntent());

        Notification notification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(Constants.DEFAULT_CONFIG_NOTIFICATION_ID, notification);



        Log.d(TAG, "Notification finished and showing");

        return notification;
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.default_config_notification_channel_name);
        String description = getString(R.string.default_config_notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    private PendingIntent getStopActionPendingIntent() {
        Log.d(TAG, "Stop pending intent creation started");
        Intent stopIntent = new Intent(this, StopActionReceiver.class);
        stopIntent.setAction(Constants.ACTION_STOP);
        stopIntent.putExtra(EXTRA_NOTIFICATION_ID, Constants.DEFAULT_CONFIG_NOTIFICATION_ID);
        stopIntent.setClass(this, StopActionReceiver.class);
        return PendingIntent.getBroadcast(this, 12, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void stopService() {
        Log.d(TAG, "Stop service called");
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");
        newPackageInstallReceiver = new NewPackageInstallReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(newPackageInstallReceiver, intentFilter);

        stopActionReceiver = new StopActionReceiver();
        IntentFilter stopIntentFilter = new IntentFilter();
        stopIntentFilter.addAction(Constants.ACTION_STOP);
        registerReceiver(stopActionReceiver, stopIntentFilter);

        instance = this;
    }

    public static DefaultConfigService getInstance() {
        return instance;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (newPackageInstallReceiver != null)
            unregisterReceiver(newPackageInstallReceiver);

        if (stopActionReceiver != null)
            unregisterReceiver(stopActionReceiver);

        stopForeground(true);
    }

    public static class StopActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Stop action receiver started");
            String action = intent.getAction();
            Log.d(TAG, "Action: " + action);
            if (action.equals(Constants.ACTION_STOP)) {
                int notificationId = intent.getExtras().getInt(EXTRA_NOTIFICATION_ID);
                Log.d(TAG, "Extra Noti Id: " + notificationId);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(null, notificationId);

                MainActivity mainActivity = MainActivity.getInstance();
                DefaultConfigService defaultConfigService = DefaultConfigService.getInstance();
                defaultConfigService.stopForeground(true);

                //update the main activity adapter to show default config service has been killed
                if (mainActivity != null) {
                    mainActivity.serverServiceConnection();
                }

                defaultConfigService.stopService();

                Log.d(TAG, "Stop action receiver ended");
            }
        }
    }
}
