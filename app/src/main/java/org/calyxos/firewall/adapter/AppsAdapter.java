package org.calyxos.firewall.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.firewall.R;
import org.calyxos.firewall.settings.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {

    private static final String TAG = AppsAdapter.class.getSimpleName();
    private Context mContext;
    private PackageManager mPackageManager;
    private SettingsManager mSettingsManager;
    private List<ApplicationInfo> mSystemApps;

    public AppsAdapter(Context context, PackageManager packageManager, List<ApplicationInfo> systemApps) {
        mSystemApps = systemApps;
        mContext = context;
        mPackageManager = packageManager;
        mSettingsManager = new SettingsManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_app_setting_accordion, parent, false);
        return new ViewHolder(view, mContext, mPackageManager, mSettingsManager);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo apps = mSystemApps.get(position);
        holder.bind(apps);
    }

    @Override
    public int getItemCount() {
        if(mSystemApps != null)
            return mSystemApps.size();
        else return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        private Context mContext;
        private PackageManager mPackageManager;
        private SettingsManager mSettingsManager;
        private LinearLayout linearLayout;
        private SwitchCompat mMainToggle, mBackgroundToggle, mWifiToggle, mMobileToggle, mVpnToggle;
        private TextView appName, settingStatus;
        private ImageView appIcon, accordionIcon;

        private ApplicationInfo app;

        List<String> stringList = new ArrayList<>();
        String backgroundDataTxt, wifiTxt, mobileDataTxt, vpn, blockTxt, andTxt, commaTxt;

        public ViewHolder(@NonNull View itemView, Context context, PackageManager packageManager, SettingsManager settingsManager) {
            super(itemView);

            mContext = context;
            mPackageManager = packageManager;
            mSettingsManager = settingsManager;

            mMainToggle = itemView.findViewById(R.id.main_toggle);
            mBackgroundToggle = itemView.findViewById(R.id.app_allow_background_toggle);
            mWifiToggle = itemView.findViewById(R.id.app_allow_wifi_toggle);
            mMobileToggle = itemView.findViewById(R.id.app_allow_mobile_toggle);
            mVpnToggle = itemView.findViewById(R.id.app_allow_vpn_toggle);

            appName = itemView.findViewById(R.id.app_name);
            settingStatus = itemView.findViewById(R.id.setting_status);
            appIcon = itemView.findViewById(R.id.app_icon);
            accordionIcon = itemView.findViewById(R.id.accordion_icon);

            linearLayout = itemView.findViewById(R.id.accordion_contents);

            itemView.setOnClickListener(this);
            accordionIcon.setOnClickListener(this);
            //set on click listeners instead of checked change for actual settings API calls because known issues
            //that comes with that
            mMainToggle.setOnClickListener(this);
            mBackgroundToggle.setOnClickListener(this);
            mWifiToggle.setOnClickListener(this);
            mMobileToggle.setOnClickListener(this);
            mVpnToggle.setOnClickListener(this);

            //set check changed here for status text updates
            mMainToggle.setOnCheckedChangeListener(this);
            mBackgroundToggle.setOnCheckedChangeListener(this);
            mWifiToggle.setOnCheckedChangeListener(this);
            mMobileToggle.setOnCheckedChangeListener(this);
            mVpnToggle.setOnCheckedChangeListener(this);

            backgroundDataTxt = mContext.getString(R.string.background_data);
            wifiTxt = mContext.getString(R.string.wifi);
            mobileDataTxt = mContext.getString(R.string.mobile_data);
            vpn = mContext.getString(R.string.vpn);
            blockTxt = mContext.getString(R.string.block_space);
            andTxt = mContext.getString(R.string.space_and_comma_space);
            commaTxt = mContext.getString(R.string.comma_space);
        }

        public void bind(ApplicationInfo app) {
            this.app = app;
            try {
                //here just in case
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(app.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            appIcon.setImageDrawable(app.loadIcon(mPackageManager));
            appName.setText(app.loadLabel(mPackageManager));

            //initialize toggle states
            //check if it's an app
            //if (UserHandle.isApp(app.uid)) {
                //get background data status
                mBackgroundToggle.setChecked(!mSettingsManager.isBlacklisted(app.uid));

                //get wifi status
                mWifiToggle.setChecked(!mSettingsManager.getAppRestrictWifi(app.uid));

                //get mobile status
                mMobileToggle.setChecked(!mSettingsManager.getAppRestrictCellular(app.uid));

                //get vpn status
                mVpnToggle.setChecked(!mSettingsManager.getAppRestrictVpn(app.uid));

                //initialize main toggle
                checkMainToggle();

                //initialize settings status
                if (!mBackgroundToggle.isChecked()) {
                    if (!stringList.contains(backgroundDataTxt))
                        stringList.add(backgroundDataTxt);
                }
                if (!mWifiToggle.isChecked()) {
                    if (!stringList.contains(wifiTxt))
                        stringList.add(wifiTxt);
                }
                if (!mMobileToggle.isChecked()) {
                    if (!stringList.contains(mobileDataTxt))
                        stringList.add(mobileDataTxt);
                }
                if (!mVpnToggle.isChecked()) {
                    if (!stringList.contains(vpn))
                        stringList.add(vpn);
                }

            /*} else {
                mMainToggle.setEnabled(false);
                mBackgroundToggle.setEnabled(false);
                mWifiToggle.setEnabled(false);
                mMobileToggle.setEnabled(false);
                mVpnToggle.setEnabled(false);
            }*/

            settingStatus.setText(generateStatus(stringList));
        }

        @Override
        public void onClick(View v) {
            if (v.equals(itemView) || v.getId() == R.id.accordion_icon) {
                if (linearLayout.getVisibility() == View.VISIBLE) {
                    linearLayout.setVisibility(View.GONE);
                    accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_down, null));
                } else {
                    linearLayout.setVisibility(View.VISIBLE);
                    accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_up, null));
                }
            } else {
                try {
                    switch (v.getId()) {
                        case R.id.main_toggle:
                            if (mMainToggle.isChecked()) {
                                //reset to default
                                mBackgroundToggle.setChecked(true);
                                mSettingsManager.setIsBlacklisted(app.uid, app.packageName, false);
                                mWifiToggle.setChecked(true);
                                mSettingsManager.setAppRestrictWifi(app.uid, false);
                                mMobileToggle.setChecked(true);
                                mSettingsManager.setAppRestrictCellular(app.uid, false);
                                mVpnToggle.setChecked(true);
                                mSettingsManager.setAppRestrictVpn(app.uid, false);
                            } else {
                                //block everything
                                mBackgroundToggle.setChecked(false);
                                mSettingsManager.setIsBlacklisted(app.uid, app.packageName, true);
                                mWifiToggle.setChecked(false);
                                mSettingsManager.setAppRestrictWifi(app.uid, true);
                                mMobileToggle.setChecked(false);
                                mSettingsManager.setAppRestrictCellular(app.uid, true);
                                mVpnToggle.setChecked(false);
                                mSettingsManager.setAppRestrictVpn(app.uid, true);
                            }
                            break;

                        case R.id.app_allow_background_toggle:
                            if (mBackgroundToggle.isChecked())
                                mSettingsManager.setIsBlacklisted(app.uid, app.packageName, false);
                            else
                                mSettingsManager.setIsBlacklisted(app.uid, app.packageName, true);

                            checkMainToggle();
                            break;

                        case R.id.app_allow_wifi_toggle:
                            if (mWifiToggle.isChecked())
                                mSettingsManager.setAppRestrictWifi(app.uid, false);
                            else
                                mSettingsManager.setAppRestrictWifi(app.uid, true);

                            checkMainToggle();
                            break;

                        case R.id.app_allow_mobile_toggle:
                            if (mMobileToggle.isChecked())
                                mSettingsManager.setAppRestrictCellular(app.uid, false);
                            else
                                mSettingsManager.setAppRestrictCellular(app.uid, true);

                            checkMainToggle();
                            break;

                        case R.id.app_allow_vpn_toggle:
                            if (mVpnToggle.isChecked())
                                mSettingsManager.setAppRestrictVpn(app.uid, false);
                            else
                                mSettingsManager.setAppRestrictVpn(app.uid, true);

                            checkMainToggle();
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());

                    settingStatus.setText(generateStatus(stringList));
                    //disable that app's toggles
                    mMainToggle.setEnabled(false);
                    mBackgroundToggle.setEnabled(false);
                    mWifiToggle.setEnabled(false);
                    mMobileToggle.setEnabled(false);
                    mVpnToggle.setEnabled(false);

                    Toast.makeText(mContext, mContext.getString(R.string.error_setting_preference, appName.getText()), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.main_toggle:
                    if (buttonView.isChecked())
                        stringList.clear();
                    else {
                        stringList.clear();
                        stringList.add(backgroundDataTxt);
                        stringList.add(wifiTxt);
                        stringList.add(mobileDataTxt);
                        stringList.add(vpn);
                    }
                    settingStatus.setText(generateStatus(stringList));
                    break;

                case R.id.app_allow_background_toggle:
                    if (buttonView.isChecked())
                        stringList.remove(backgroundDataTxt);
                    else {
                        if (!stringList.contains(backgroundDataTxt))
                            stringList.add(backgroundDataTxt);
                    }
                    settingStatus.setText(generateStatus(stringList));
                    break;

                case R.id.app_allow_wifi_toggle:
                    if (mWifiToggle.isChecked())
                        stringList.remove(wifiTxt);
                    else {
                        if (!stringList.contains(wifiTxt))
                            stringList.add(wifiTxt);
                    }
                    settingStatus.setText(generateStatus(stringList));
                    break;

                case R.id.app_allow_mobile_toggle:
                    if (mMobileToggle.isChecked())
                        stringList.remove(mobileDataTxt);
                    else {
                        if (!stringList.contains(mobileDataTxt))
                            stringList.add(mobileDataTxt);
                    }
                    settingStatus.setText(generateStatus(stringList));
                    break;

                case R.id.app_allow_vpn_toggle:
                    if (mVpnToggle.isChecked())
                        stringList.remove(vpn);
                    else {
                        if (!stringList.contains(vpn))
                            stringList.add(vpn);
                    }
                    settingStatus.setText(generateStatus(stringList));
                    break;
            }
        }

        private String generateStatus(List<String> s) {
            if (!s.isEmpty()) {
                StringBuilder statusText = new StringBuilder(blockTxt);
                for (String str : s) {
                    if (s.indexOf(str) == s.size() - 1 && s.size() != 1) {
                        statusText.append(andTxt).append(str);
                    } else if (s.indexOf(str) == 0) {
                        statusText.append(str);
                    } else {
                        statusText.append(commaTxt).append(str);
                    }
                }

                return statusText.toString();
            } else return mContext.getString(R.string.default_settings);
        }

        private void checkMainToggle() {
            if (mBackgroundToggle.isChecked() && mWifiToggle.isChecked() && mMobileToggle.isChecked() && mVpnToggle.isChecked())
                mMainToggle.setChecked(true);
            else if (!mBackgroundToggle.isChecked() && !mWifiToggle.isChecked() && !mMobileToggle.isChecked() && !mVpnToggle.isChecked())
                mMainToggle.setChecked(false);
        }
    }
}
