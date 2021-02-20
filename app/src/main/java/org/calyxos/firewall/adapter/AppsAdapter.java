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
import android.net.NetworkPolicyManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.firewall.R;

import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {

    private static final String TAG = AppsAdapter.class.getSimpleName();
    private Context mContext;
    private NetworkPolicyManager mPolicyManager;
    private PackageManager mPackageManager;
    private List<ApplicationInfo> mSystemApps;

    public AppsAdapter(Context context, PackageManager packageManager, List<ApplicationInfo> systemApps) {
        mSystemApps = systemApps;
        mContext = context;
        mPackageManager = packageManager;
        mPolicyManager = NetworkPolicyManager.from(context);

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_app_setting_accordion, parent, false);
        return new ViewHolder(view, mContext, mPackageManager);
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
        private LinearLayout linearLayout;
        private SwitchCompat mMainToggle, mBackgroundToggle, mWifiToggle, mMobileToggle, mVpnToggle;
        private TextView appName, settingStatus;
        private ImageView appIcon, accordionIcon;

        private ApplicationInfo app;

        public ViewHolder(@NonNull View itemView, Context context, PackageManager packageManager) {
            super(itemView);

            mContext = context;
            mPackageManager = packageManager;

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
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(app.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            appIcon.setImageDrawable(app.loadIcon(mPackageManager));
            appName.setText(app.loadLabel(mPackageManager));

            //initialize toggle states

            //initialize settings status
        }

        @Override
        public void onClick(View v) {
            if (linearLayout.getVisibility() == View.VISIBLE) {
                linearLayout.setVisibility(View.GONE);
                accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_down));
            } else {
                linearLayout.setVisibility(View.VISIBLE);
                accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_up));
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()) {
                case R.id.main_toggle:

                    break;

                case R.id.app_allow_background_toggle:

                    break;

                case R.id.app_allow_wifi_toggle:

                    break;

                case R.id.app_allow_mobile_toggle:

                    break;

                case R.id.app_allow_vpn_toggle:

                    break;
            }
        }
    }
}
