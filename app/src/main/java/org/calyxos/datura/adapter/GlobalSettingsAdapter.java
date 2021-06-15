package org.calyxos.datura.adapter;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.datura.MainActivity;
import org.calyxos.datura.R;
import org.calyxos.datura.service.DefaultConfigService;
import org.calyxos.datura.settings.SettingsManager;
import org.calyxos.datura.util.Constants;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class GlobalSettingsAdapter extends RecyclerView.Adapter<GlobalSettingsAdapter.ViewHolder> {

    private static final String TAG = GlobalSettingsAdapter.class.getSimpleName();
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final SettingsManager mSettingsManager;

    public GlobalSettingsAdapter(Context context, PackageManager packageManager) {
        mContext = context;
        mPackageManager = packageManager;
        mSettingsManager = new SettingsManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_global_settings, parent, false);
        return new ViewHolder(view, mContext, mPackageManager, mSettingsManager);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Context mContext;
        private PackageManager mPackageManager;
        private SettingsManager mSettingsManager;
        private SwitchCompat mGlobalClrTextToggle, mDefaultConfigToggle, mNetworkAccessToggle, mBackgroundToggle, mWifiToggle, mMobileToggle, mVpnToggle,
                mAppClrTextToggle;
        private ImageView mAccordionIcon;
        private TextView mAccordionTitle;
        private ConstraintLayout mAccordion;
        private SharedPreferences sharedPreferences;

        public ViewHolder(@NonNull View itemView, Context context, PackageManager packageManager, SettingsManager settingsManager) {
            super(itemView);

            mContext = context;
            mPackageManager = packageManager;
            mSettingsManager = settingsManager;

            sharedPreferences = context.getSharedPreferences(Constants.DEFAULT_CONFIG, MODE_PRIVATE);

            mAccordion = itemView.findViewById(R.id.default_config_accordion);

            mGlobalClrTextToggle = itemView.findViewById(R.id.global_cleartext_toggle);
            mDefaultConfigToggle = itemView.findViewById(R.id.default_config_toggle);
            mNetworkAccessToggle = itemView.findViewById(R.id.all_network_toggle);
            mBackgroundToggle = itemView.findViewById(R.id.app_allow_background_toggle);
            mWifiToggle = itemView.findViewById(R.id.app_allow_wifi_toggle);
            mMobileToggle = itemView.findViewById(R.id.app_allow_mobile_toggle);
            mVpnToggle = itemView.findViewById(R.id.app_allow_vpn_toggle);
            mAppClrTextToggle = itemView.findViewById(R.id.app_allow_cleartext_toggle);

            mAccordionIcon = itemView.findViewById(R.id.default_config_accordion_icon);
            mAccordionTitle = itemView.findViewById(R.id.apply_to_new_text);


            //check if Private DNS is enabled
            mGlobalClrTextToggle.setEnabled(mSettingsManager.isPrivateDNSEnabled());
            //initialize cleartext toggle state
            mGlobalClrTextToggle.setChecked(mSettingsManager.isCleartextBlocked());

            //initialize default config toggles
            mDefaultConfigToggle.setChecked(isDefaultConfigServiceRunning());
            mNetworkAccessToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_NETWORK_ACCESS, true));
            mBackgroundToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_BACKGROUND_DATA, true));
            mWifiToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_WIFI_DATA, true));
            mMobileToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_MOBILE_DATA, true));
            mVpnToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_VPN_DATA, true));
            mAppClrTextToggle.setChecked(sharedPreferences.getBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, true));

            //set on click listeners instead of checked change for actual settings API calls because known issues
            //that comes with that
            mGlobalClrTextToggle.setOnClickListener(this);

            mAccordionIcon.setOnClickListener(this);
            mAccordionTitle.setOnClickListener(this);
            //set on click listeners instead of checked change for actual settings API calls because known issues
            //that comes with that
            mDefaultConfigToggle.setOnClickListener(this);
            mNetworkAccessToggle.setOnClickListener(this);
            mBackgroundToggle.setOnClickListener(this);
            mWifiToggle.setOnClickListener(this);
            mMobileToggle.setOnClickListener(this);
            mVpnToggle.setOnClickListener(this);
            mAppClrTextToggle.setOnClickListener(this);
        }

        public void bind(ApplicationInfo app) {

        }

        @Override
        public void onClick(View v) {
            if (v.equals(mGlobalClrTextToggle)) {
                mSettingsManager.blockCleartextTraffic(mGlobalClrTextToggle.isChecked());
                //call a main activity function that refreshes the list of apps
                MainActivity.getInstance().notifyDataSetChanged();
            } else if (v.getId() == R.id.default_config_accordion_icon || v.getId() == R.id.apply_to_new_text) {
                if (mAccordion.getVisibility() == View.VISIBLE) {
                    mAccordion.setVisibility(View.GONE);
                    mAccordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_down, null));
                } else {
                    mAccordion.setVisibility(View.VISIBLE);
                    mAccordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_up, null));
                }
            } else {
                Log.d(TAG, "Click event started");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                switch (v.getId()) {
                    case R.id.default_config_toggle: {
                        if (mDefaultConfigToggle.isChecked()) {
                            Log.d(TAG, "Toggle not checked");
                            editor.putBoolean(Constants.ALLOW_NETWORK_ACCESS, mNetworkAccessToggle.isChecked())
                                    .putBoolean(Constants.ALLOW_BACKGROUND_DATA, mBackgroundToggle.isChecked())
                                    .putBoolean(Constants.ALLOW_WIFI_DATA, mWifiToggle.isChecked())
                                    .putBoolean(Constants.ALLOW_MOBILE_DATA, mMobileToggle.isChecked())
                                    .putBoolean(Constants.ALLOW_VPN_DATA, mVpnToggle.isChecked())
                                    .putBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, mAppClrTextToggle.isChecked()).apply();

                            MainActivity.getInstance().startDefaultConfigService();
                        } else {
                            editor.clear().apply();

                            MainActivity.getInstance().stopDefaultConfigService();
                        }
                        break;
                    }

                    case R.id.all_network_toggle: {
                        editor.putBoolean(Constants.ALLOW_NETWORK_ACCESS, mNetworkAccessToggle.isChecked()).apply();
                        break;
                    }

                    case R.id.app_allow_background_toggle: {
                        editor.putBoolean(Constants.ALLOW_BACKGROUND_DATA, mBackgroundToggle.isChecked()).apply();
                        break;
                    }

                    case R.id.app_allow_wifi_toggle: {
                        editor.putBoolean(Constants.ALLOW_WIFI_DATA, mWifiToggle.isChecked()).apply();
                        break;
                    }

                    case R.id.app_allow_mobile_toggle: {
                        editor.putBoolean(Constants.ALLOW_MOBILE_DATA, mMobileToggle.isChecked()).apply();
                        break;
                    }

                    case R.id.app_allow_vpn_toggle: {
                        editor.putBoolean(Constants.ALLOW_VPN_DATA, mVpnToggle.isChecked()).apply();
                        break;
                    }

                    case R.id.app_allow_cleartext_toggle: {
                        editor.putBoolean(Constants.ALLOW_CLEARTEXT_TRAFFIC, mAppClrTextToggle.isChecked()).apply();
                        break;
                    }
                }
            }
        }

        private boolean isDefaultConfigServiceRunning() {
            final ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

            for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
                if (runningServiceInfo.service.getClassName().equals(DefaultConfigService.class.getName())){
                    return true;
                }
            }
            return false;
        }
    }
}
