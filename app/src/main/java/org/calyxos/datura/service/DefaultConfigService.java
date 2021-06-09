package org.calyxos.datura.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.calyxos.datura.NewPackageInstallReceiver;

public class DefaultConfigService extends Service {

    NewPackageInstallReceiver newPackageInstallReceiver;

    public class ServiceBinder extends Binder {
        public DefaultConfigService getService() {
            return DefaultConfigService.this;
        }
    }

    private final ServiceBinder binder = new ServiceBinder();

    private Notification getNotification() {
        return null; //TODO build a notification
    }

    public void stopService() {
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        newPackageInstallReceiver = new NewPackageInstallReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(newPackageInstallReceiver, intentFilter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (newPackageInstallReceiver != null)
            unregisterReceiver(newPackageInstallReceiver);
        stopForeground(true);
    }
}
