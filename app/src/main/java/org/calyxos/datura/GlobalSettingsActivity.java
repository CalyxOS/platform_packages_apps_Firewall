package org.calyxos.datura;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import org.calyxos.datura.settings.SettingsManager;
import org.calyxos.datura.util.Util;

import java.util.ArrayList;
import java.util.List;

public class GlobalSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = GlobalSettingsActivity.class.getSimpleName();
    private SwitchCompat mBackgroundDataToggle, mWIFIDataToggle, mMobileDataToggle, mVPNDataToggle, mAllNetworkToggle;
    private SettingsManager mSettingsManager;
    private List<ApplicationInfo> allApps = new ArrayList<>();

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

        mSettingsManager = new SettingsManager(this);
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
        switch (v.getId()) {

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
}
