package org.calyxos.datura;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import org.calyxos.datura.service.DefaultConfigService;
import org.calyxos.datura.settings.SettingsManager;
import org.calyxos.datura.util.Constants;
import org.calyxos.datura.util.Util;

import java.util.ArrayList;
import java.util.List;

public class GlobalSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = GlobalSettingsActivity.class.getSimpleName();
    private SwitchCompat mBackgroundDataToggle, mWIFIDataToggle, mMobileDataToggle, mVPNDataToggle, mAllNetworkToggle, mDefaultConfigToggle,
            mDefConfNetworkAccessToggle, mDefConfBackgroundToggle, mDefConfWifiToggle, mDefConfMobileToggle, mDefConfVpnToggle,
            mDefConfAppClrTextToggle;
    private ImageView mAccordionIcon;
    private TextView mAccordionTitle;
    private ConstraintLayout mAccordion;
    private SharedPreferences sharedPreferences;
    private SettingsManager mSettingsManager;
    private ServiceConnection serviceConnection;
    private DefaultConfigService configService;
    private List<ApplicationInfo> allApps = new ArrayList<>();

    private static GlobalSettingsActivity instance;

    public void startDefaultConfigService() {
        Log.d(TAG, "Service about to be started");
        Intent serviceIntent = new Intent(GlobalSettingsActivity.this, DefaultConfigService.class);
        //Service connection to bind the service to this context because of startForegroundService issues
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Service connected");
                DefaultConfigService.ServiceBinder binder = (DefaultConfigService.ServiceBinder) service;
                configService = binder.getService();
                startForegroundService(serviceIntent);
                configService.startForeground(Constants.DEFAULT_CONFIG_NOTIFICATION_ID, configService.getNotification());
            }

            @Override
            public void onBindingDied(ComponentName name) {
                Log.w(TAG, "Binding has dead.");
            }

            @Override
            public void onNullBinding(ComponentName name) {
                Log.w(TAG, "Bind was null.");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w(TAG, "Service is disconnected..");
            }
        };

        try {
            Log.d(TAG, "Service bound");
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (RuntimeException ignored) {
            Log.d(TAG, "Runtime exception");
            //Use the normal way and accept it will fail sometimes
            startForegroundService(serviceIntent);
        }
    }

    public void stopDefaultConfigService() {
        unbindService(serviceConnection);
        if (configService != null)
            configService.stopForeground(true);
        stopService(new Intent(this, DefaultConfigService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.advanced_settings);

        mBackgroundDataToggle = findViewById(R.id.app_allow_background_toggle);
        mBackgroundDataToggle.setOnClickListener(this);
        mWIFIDataToggle = findViewById(R.id.app_allow_wifi_toggle);
        mWIFIDataToggle.setOnClickListener(this);
        mMobileDataToggle = findViewById(R.id.app_allow_mobile_toggle);
        mMobileDataToggle.setOnClickListener(this);
        mVPNDataToggle = findViewById(R.id.app_allow_vpn_toggle);
        mVPNDataToggle.setOnClickListener(this);
        mAllNetworkToggle = findViewById(R.id.all_network_toggle);
        mAllNetworkToggle.setOnClickListener(this);

        sharedPreferences = getSharedPreferences(Constants.DEFAULT_CONFIG, MODE_PRIVATE);

        mAccordion = findViewById(R.id.default_config_accordion);

        mAccordionIcon = findViewById(R.id.default_config_accordion_icon);
        mAccordionTitle = findViewById(R.id.apply_to_new_text);

        mDefaultConfigToggle = findViewById(R.id.default_config_toggle);
        mDefConfNetworkAccessToggle = findViewById(R.id.def_conf_all_network_toggle);
        mDefConfBackgroundToggle = findViewById(R.id.def_conf_app_allow_background_toggle);
        mDefConfWifiToggle = findViewById(R.id.def_conf_app_allow_wifi_toggle);
        mDefConfMobileToggle = findViewById(R.id.def_conf_app_allow_mobile_toggle);
        mDefConfVpnToggle = findViewById(R.id.def_conf_app_allow_vpn_toggle);
        mDefConfAppClrTextToggle = findViewById(R.id.app_allow_cleartext_toggle);

        mAccordionIcon.setOnClickListener(this);
        mAccordionTitle.setOnClickListener(this);

        //set on click listeners instead of checked change for actual settings API calls because known issues
        //that comes with that
        mDefaultConfigToggle.setOnClickListener(this);
        mDefConfNetworkAccessToggle.setOnClickListener(this);
        mDefConfBackgroundToggle.setOnClickListener(this);
        mDefConfWifiToggle.setOnClickListener(this);
        mDefConfMobileToggle.setOnClickListener(this);
        mDefConfVpnToggle.setOnClickListener(this);
        mDefConfAppClrTextToggle.setOnClickListener(this);

        mSettingsManager = new SettingsManager(this);

        instance = this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        final PackageManager pm = getPackageManager();

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        //filter system and installed apps
        List<ApplicationInfo> sysApps = new ArrayList<>();
        List<ApplicationInfo> instApps = new ArrayList<>();

        for (ApplicationInfo ai : packages) {
            // Skip anything that isn't an "app" since we can't set policies for those, as
            // the framework code which handles setting the policies has a similar check.
            if (Util.isApp(ai.uid)) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    sysApps.add(ai);
                } else {
                    instApps.add(ai);
                }
            }
        }

        allApps.addAll(instApps);
        allApps.addAll(sysApps);

        //initialize default config toggles
        mDefaultConfigToggle.setChecked(isDefaultConfigServiceRunning());
        mDefConfNetworkAccessToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_NETWORK_ACCESS, true));
        mDefConfBackgroundToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_BACKGROUND_DATA, true));
        mDefConfWifiToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_WIFI_DATA, true));
        mDefConfMobileToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_MOBILE_DATA, true));
        mDefConfVpnToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_VPN_DATA, true));
        mDefConfAppClrTextToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, true));

        //initialize global toggles states

        //if all apps have their background data blocked then the global toggle should be unchecked as this
        //means background data is blocked for all apps
        mBackgroundDataToggle.setChecked(!mSettingsManager.isAppsBackgroundDataBlocked(allApps));
        //this applies to the rest of the toggles
        mWIFIDataToggle.setChecked(!mSettingsManager.isAppsWIFIDataBlocked(allApps));
        mMobileDataToggle.setChecked(!mSettingsManager.isAppsMobileDataBlocked(allApps));
        mVPNDataToggle.setChecked(!mSettingsManager.isAppsVPNDataBlocked(allApps));

        mAllNetworkToggle.setChecked(!mBackgroundDataToggle.isChecked() || mWIFIDataToggle.isChecked() || mMobileDataToggle.isChecked()
                || mVPNDataToggle.isChecked());
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Click event started");
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (v.getId()) {

            case R.id.default_config_accordion_icon:
            case R.id.apply_to_new_text: {
                if (mAccordion.getVisibility() == View.VISIBLE) {
                    mAccordion.setVisibility(View.GONE);
                    mAccordionIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_accordion_down, null));
                } else {
                    mAccordion.setVisibility(View.VISIBLE);
                    mAccordionIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_accordion_up, null));
                }
            }

            case R.id.default_config_toggle: {
                if (mDefaultConfigToggle.isChecked()) {
                    Log.d(TAG, "Toggle not checked");
                    editor.putBoolean(Constants.ALLOW_NETWORK_ACCESS, mDefConfNetworkAccessToggle.isChecked())
                            .putBoolean(Constants.ALLOW_BACKGROUND_DATA, mDefConfBackgroundToggle.isChecked())
                            .putBoolean(Constants.ALLOW_WIFI_DATA, mDefConfWifiToggle.isChecked())
                            .putBoolean(Constants.ALLOW_MOBILE_DATA, mDefConfMobileToggle.isChecked())
                            .putBoolean(Constants.ALLOW_VPN_DATA, mDefConfVpnToggle.isChecked())
                            .putBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, mDefConfAppClrTextToggle.isChecked()).apply();

                    startDefaultConfigService();
                } else {
                    editor.clear().apply();

                    stopDefaultConfigService();
                }
                break;
            }

            case R.id.def_conf_all_network_toggle: {
                editor.putBoolean(Constants.ALLOW_NETWORK_ACCESS, mDefConfNetworkAccessToggle.isChecked()).apply();
                break;
            }

            case R.id.def_conf_app_allow_background_toggle: {
                editor.putBoolean(Constants.ALLOW_BACKGROUND_DATA, mDefConfBackgroundToggle.isChecked()).apply();
                break;
            }

            case R.id.def_conf_app_allow_wifi_toggle: {
                editor.putBoolean(Constants.ALLOW_WIFI_DATA, mDefConfWifiToggle.isChecked()).apply();
                break;
            }

            case R.id.def_conf_app_allow_mobile_toggle: {
                editor.putBoolean(Constants.ALLOW_MOBILE_DATA, mDefConfMobileToggle.isChecked()).apply();
                break;
            }

            case R.id.def_conf_app_allow_vpn_toggle: {
                editor.putBoolean(Constants.ALLOW_VPN_DATA, mDefConfVpnToggle.isChecked()).apply();
                break;
            }

            case R.id.app_allow_cleartext_toggle: {
                editor.putBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, mDefConfAppClrTextToggle.isChecked()).apply();
                break;
            }

            //-------------------------------------

            case R.id.app_allow_background_toggle: {
                mSettingsManager.allowAppsBackgroundData(allApps, mBackgroundDataToggle.isChecked());
                //if all network access toggle is off when this is switched on, that toggle should be set on again
                if (!mAllNetworkToggle.isChecked() && mBackgroundDataToggle.isChecked())
                    mAllNetworkToggle.setChecked(true);
                break;
            }

            case R.id.app_allow_wifi_toggle: {
                mSettingsManager.allowAppsWIFIData(allApps, mWIFIDataToggle.isChecked());
                //if all network access toggle is off when this is switched on, that toggle should be set on again
                if (!mAllNetworkToggle.isChecked() && mWIFIDataToggle.isChecked())
                    mAllNetworkToggle.setChecked(true);
                break;
            }

            case R.id.app_allow_mobile_toggle: {
                mSettingsManager.allowAppsMobileData(allApps, mMobileDataToggle.isChecked());
                //if all network access toggle is off when this is switched on, that toggle should be set on again
                if (!mAllNetworkToggle.isChecked() && mMobileDataToggle.isChecked())
                    mAllNetworkToggle.setChecked(true);
                break;
            }

            case R.id.app_allow_vpn_toggle: {
                mSettingsManager.allowAppsVPNData(allApps, mVPNDataToggle.isChecked());
                //if all network access toggle is off when this is switched on, that toggle should be set on again
                if (!mAllNetworkToggle.isChecked() && mVPNDataToggle.isChecked())
                    mAllNetworkToggle.setChecked(true);
                break;
            }

            case R.id.all_network_toggle: {
                mSettingsManager.allowAppsNetworkAccess(allApps, mAllNetworkToggle.isChecked());

                //mAppAdapter.allowAllBackgroundData(mAllNetworkToggle.isChecked());
                mBackgroundDataToggle.setChecked(mAllNetworkToggle.isChecked());

                //mAppAdapter.allowAllWIFIData(mAllNetworkToggle.isChecked());
                mWIFIDataToggle.setChecked(mAllNetworkToggle.isChecked());

                //mAppAdapter.allowAllMobileData(mAllNetworkToggle.isChecked());
                mMobileDataToggle.setChecked(mAllNetworkToggle.isChecked());

                //mAppAdapter.allowAllVPNData(mAllNetworkToggle.isChecked());
                mVPNDataToggle.setChecked(mAllNetworkToggle.isChecked());
                break;
            }
        }
    }

    private boolean isDefaultConfigServiceRunning() {
        final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(DefaultConfigService.class.getName())){
                return true;
            }
        }
        return false;
    }

    public void updateToggle() {
        mDefaultConfigToggle.setChecked(false);
    }

    public static GlobalSettingsActivity getInstance() {
        return instance;
    }

    public void serverServiceConnection() {
        unbindService(serviceConnection);
    }
}
