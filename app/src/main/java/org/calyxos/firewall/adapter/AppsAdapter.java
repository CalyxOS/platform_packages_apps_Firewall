package org.calyxos.firewall.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        List<String> s = new ArrayList<>();

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
            mMainToggle.setOnCheckedChangeListener(this);
            mBackgroundToggle.setOnCheckedChangeListener(this);
            mWifiToggle.setOnCheckedChangeListener(this);
            mMobileToggle.setOnCheckedChangeListener(this);
            mVpnToggle.setOnCheckedChangeListener(this);
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
            //get background data status
            mBackgroundToggle.setChecked(mSettingsManager.isBlacklisted(app.uid));

            //get wifi status
            mWifiToggle.setChecked(mSettingsManager.getAppRestrictWifi(app.uid));

            //get mobile status
            mMobileToggle.setChecked(mSettingsManager.getAppRestrictCellular(app.uid));

            //get vpn status
            mVpnToggle.setChecked(mSettingsManager.getAppRestrictVpn(app.uid));

            //initialize main toggle
            if (mBackgroundToggle.isChecked() || mWifiToggle.isChecked() || mMobileToggle.isChecked() || mVpnToggle.isChecked())
                mMainToggle.setChecked(true);
            else mMainToggle.setChecked(false);

            //initialize settings status

            if (!mBackgroundToggle.isChecked())
                s.add("background data");
            if (!mWifiToggle.isChecked())
                s.add("wifi");
            if (!mMobileToggle.isChecked())
                s.add("mobile data");
            if (!mVpnToggle.isChecked())
                s.add("VPN");

            settingStatus.setText(generateStatus(s));
        }

        @Override
        public void onClick(View v) {
            if (linearLayout.getVisibility() == View.VISIBLE) {
                linearLayout.setVisibility(View.GONE);
                accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_down, null));
            } else {
                linearLayout.setVisibility(View.VISIBLE);
                accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_up, null));
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()) {
                case R.id.main_toggle:
                    if (compoundButton.isChecked()) {
                        //reset to default
                        mBackgroundToggle.setChecked(true);
                        mSettingsManager.setIsBlacklisted(app.uid, app.packageName, false);
                        mWifiToggle.setChecked(true);
                        mSettingsManager.setAppRestrictWifi(app.uid, false);
                        mMobileToggle.setChecked(true);
                        mSettingsManager.setAppRestrictCellular(app.uid, false);
                        mVpnToggle.setChecked(true);
                        mSettingsManager.setAppRestrictVpn(app.uid, false);

                        s.clear();
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

                        s.clear();
                        s.add("background data");
                        s.add("wifi");
                        s.add("mobile data");
                        s.add("VPN");
                    }
                    settingStatus.setText(generateStatus(s));
                    break;

                case R.id.app_allow_background_toggle:
                    if (compoundButton.isChecked()) {
                        mSettingsManager.setIsBlacklisted(app.uid, app.packageName, false);
                        s.remove("background data");
                    } else {
                        mSettingsManager.setIsBlacklisted(app.uid, app.packageName, true);
                        if (!s.contains("background data"))
                            s.add("background data");
                    }
                    settingStatus.setText(generateStatus(s));
                    break;

                case R.id.app_allow_wifi_toggle:
                    if (compoundButton.isChecked()) {
                        mSettingsManager.setAppRestrictWifi(app.uid, false);
                        s.remove("wifi");
                    } else {
                        mSettingsManager.setAppRestrictWifi(app.uid, true);
                        if (!s.contains("wifi"))
                            s.add("wifi");
                    }
                    settingStatus.setText(generateStatus(s));
                    break;

                case R.id.app_allow_mobile_toggle:
                    if (compoundButton.isChecked()) {
                        mSettingsManager.setAppRestrictCellular(app.uid, false);
                        s.remove("mobile data");
                    } else {
                        mSettingsManager.setAppRestrictCellular(app.uid, true);
                        if (!s.contains("mobile data"))
                            s.add("mobile data");
                    }
                    settingStatus.setText(generateStatus(s));
                    break;

                case R.id.app_allow_vpn_toggle:
                    if (compoundButton.isChecked()) {
                        mSettingsManager.setAppRestrictVpn(app.uid, false);
                        s.remove("VPN");
                    } else {
                        mSettingsManager.setAppRestrictVpn(app.uid, true);
                        if (!s.contains("VPN"))
                            s.add("VPN");
                    }
                    settingStatus.setText(generateStatus(s));
                    break;
            }
        }

        private String generateStatus(List<String> s) {
            if (!s.isEmpty()) {
                StringBuilder statusText = new StringBuilder("Block ");
                for (String str : s) {
                    if (s.indexOf(str) == s.size() - 1 && s.size() != 1) {
                        statusText.append(" and, ").append(str);
                    } else if (s.indexOf(str) == 0) {
                        statusText.append(str);
                    } else {
                        statusText.append(", ").append(str);
                    }
                }

                return statusText.toString();
            } else return mContext.getString(R.string.default_settings);
        }
    }
}
