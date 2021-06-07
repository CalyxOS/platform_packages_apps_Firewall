package org.calyxos.datura;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import org.calyxos.datura.settings.SettingsManager;
import org.calyxos.datura.util.Constants;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class NewPackageInstallReceiver extends BroadcastReceiver {

    private static final String TAG = NewPackageInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        //check if default config is set
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.DEFAULT_CONFIG, MODE_PRIVATE);
        if (sharedPreferences.contains(Constants.ALLOW_BACKGROUND_DATA)) {
            String action = intent.getAction();

            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                Uri packageUri = intent.getData();
                if (packageUri == null) {
                    return;
                }

                String packageName = packageUri.getSchemeSpecificPart();
                if (packageName == null) {
                    Log.e(TAG, "No package name");
                    return;
                }

                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    Log.i(TAG, "Not new app, skip it: " + packageName);
                    return;
                }

                //then apply config if so
                final PackageManager pm = context.getPackageManager();
                //TODO: might need to handle work profiles here.
                try {
                    ApplicationInfo app = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

                    SettingsManager settingsManager = new SettingsManager(context);

                    List<ApplicationInfo> apps = new ArrayList<>();
                    apps.add(app);

                    settingsManager.allowAppsNetworkAccess(apps, sharedPreferences.getBoolean(Constants.ALLOW_NETWORK_ACCESS, true));
                    settingsManager.allowAppsBackgroundData(apps, sharedPreferences.getBoolean(Constants.ALLOW_BACKGROUND_DATA, true));
                    settingsManager.allowAppsMobileData(apps, sharedPreferences.getBoolean(Constants.ALLOW_MOBILE_DATA, true));
                    settingsManager.allowAppsVPNData(apps, sharedPreferences.getBoolean(Constants.ALLOW_VPN_DATA, true));
                    settingsManager.allowAppsWIFIData(apps, sharedPreferences.getBoolean(Constants.ALLOW_WIFI_DATA, true));

                    if(settingsManager.isPrivateDNSEnabled() && settingsManager.isCleartextTrafficPermitted(app.packageName)) {
                        settingsManager.allowAppCleartext(app.uid, sharedPreferences.getBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, true));
                    }

                    //maybe refresh app list if Datura is in the foreground
                    MainActivity mainActivity = MainActivity.getInstance();
                    if (mainActivity != null)
                        mainActivity.notifyDataSetChanged();
                    
                } catch (PackageManager.NameNotFoundException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
